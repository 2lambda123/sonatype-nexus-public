/*
 * Sonatype Nexus (TM) Open Source Version
 * Copyright (c) 2008-present Sonatype, Inc.
 * All rights reserved. Includes the third-party code listed at http://links.sonatype.com/products/nexus/oss/attributions.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License Version 1.0,
 * which accompanies this distribution and is available at http://www.eclipse.org/legal/epl-v10.html.
 *
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc. "Sonatype" and "Sonatype Nexus" are trademarks
 * of Sonatype, Inc. Apache Maven is a trademark of the Apache Software Foundation. M2eclipse is a trademark of the
 * Eclipse Foundation. All other trademarks are the property of their respective owners.
 */
package org.sonatype.nexus.repository.content.store;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sonatype.nexus.blobstore.api.BlobRef;
import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.api.DataSession;
import org.sonatype.nexus.repository.content.Asset;
import org.sonatype.nexus.repository.content.store.example.TestAssetBlobDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetDAO;
import org.sonatype.nexus.repository.content.store.example.TestAssetData;
import org.sonatype.nexus.repository.content.store.example.TestComponentDAO;
import org.sonatype.nexus.repository.content.store.example.TestContentRepositoryDAO;

import org.apache.ibatis.exceptions.PersistenceException;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test {@link AssetDAO}.
 */
public class AssetDAOTest
    extends ExampleContentTestSupport
{
  private ContentRepositoryData contentRepository;

  private int repositoryId;

  @Before
  public void setupContent() {
    contentRepository = randomContentRepository();

    try (DataSession<?> session = sessionRule.openSession("content")) {
      ContentRepositoryDAO dao = session.access(TestContentRepositoryDAO.class);
      dao.createContentRepository(contentRepository);
      session.getTransaction().commit();
    }

    repositoryId = contentRepository.repositoryId;

    generateRandomPaths(100);
  }

  @Test
  public void testCrudOperations() throws InterruptedException {

    AssetData asset1 = randomAsset(repositoryId);
    AssetData asset2 = randomAsset(repositoryId);
    asset2.setPath(asset1.path() + "/2"); // make sure paths are different

    String path1 = asset1.path();
    String path2 = asset2.path();

    Asset tempResult;

    // CREATE

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertThat(dao.browseAssets(repositoryId, 10, null), emptyIterable());

      dao.createAsset(asset1);

      assertThat(dao.browseAssets(repositoryId, 10, null), contains(allOf(samePath(asset1), sameAttributes(asset1))));

      dao.createAsset(asset2);

      assertThat(dao.browseAssets(repositoryId, 10, null),
          contains(allOf(samePath(asset1), sameAttributes(asset1)), allOf(samePath(asset2), sameAttributes(asset2))));

      session.getTransaction().commit();
    }

    // TRY CREATE AGAIN

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      AssetData duplicate = new AssetData();
      duplicate.repositoryId = asset1.repositoryId;
      duplicate.setPath(asset1.path());
      duplicate.setAttributes(newAttributes("duplicate"));
      dao.createAsset(duplicate);

      session.getTransaction().commit();
      fail("Cannot create the same component twice");
    }
    catch (PersistenceException e) {
      logger.debug("Got expected exception", e);
    }

    // READ

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertFalse(dao.readAsset(repositoryId, "test-path").isPresent());

      tempResult = dao.readAsset(repositoryId, path1).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameAttributes(asset1));

      tempResult = dao.readAsset(repositoryId, path2).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameAttributes(asset2));
    }

    // UPDATE

    Thread.sleep(2); // make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path1).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();

      asset1.attributes("custom-section-1").set("custom-key-1", "more-test-values-1");
      dao.updateAssetAttributes(asset1);

      tempResult = dao.readAsset(repositoryId, path1).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameAttributes(asset1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      tempResult = dao.readAsset(repositoryId, path2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      asset2.assetId = null; // check a 'detached' entity with no internal id can be updated
      asset2.attributes("custom-section-2").set("custom-key-2", "more-test-values-2");
      dao.updateAssetAttributes(asset2);

      tempResult = dao.readAsset(repositoryId, path2).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameAttributes(asset2));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes have changed

      session.getTransaction().commit();
    }

    // UPDATE AGAIN

    Thread.sleep(2); // make sure any new last updated times will be different

    // must use a new session as CURRENT_TIMESTAMP (used for last_updated) is fixed once used inside a session

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path1).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();

      asset1.attributes("custom-section-1").set("custom-key-1", "more-test-values-again");
      dao.updateAssetAttributes(asset1);

      tempResult = dao.readAsset(repositoryId, path1).get();
      assertThat(tempResult, samePath(asset1));
      assertThat(tempResult, sameAttributes(asset1));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated)); // should change as attributes changed again

      tempResult = dao.readAsset(repositoryId, path2).get();

      oldCreated = tempResult.created();
      oldLastUpdated = tempResult.lastUpdated();

      dao.updateAssetAttributes(asset2);

      tempResult = dao.readAsset(repositoryId, path2).get();
      assertThat(tempResult, samePath(asset2));
      assertThat(tempResult, sameAttributes(asset2));
      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated)); // won't have changed as attributes haven't changed

      session.getTransaction().commit();
    }

    // DELETE

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      assertTrue(dao.deleteAsset(asset1));

      assertThat(dao.browseAssets(repositoryId, 10, null), contains(allOf(samePath(asset2), sameAttributes(asset2))));

      assertTrue(dao.deleteAssets(repositoryId));

      assertThat(dao.browseAssets(repositoryId, 10, null), emptyIterable());

      assertFalse(dao.deletePath(repositoryId, "test-path"));
    }
  }

  @Test
  public void testLastDownloaded() throws InterruptedException {

    AssetData asset = randomAsset(repositoryId);
    String path = asset.path();
    Asset tempResult;

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);
      dao.createAsset(asset);
      session.getTransaction().commit();
    }

    // INITIAL DOWNLOAD

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      assertFalse(tempResult.lastDownloaded().isPresent());

      dao.markAsDownloaded(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertTrue(tempResult.lastDownloaded().isPresent());
      assertTrue(tempResult.lastDownloaded().get().isAfter(oldLastUpdated));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();
    }

    // SOME LATER DOWNLOAD

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      DateTime oldLastDownloaded = tempResult.lastDownloaded().get();

      dao.markAsDownloaded(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertTrue(tempResult.lastDownloaded().isPresent());
      assertTrue(tempResult.lastDownloaded().get().isAfter(oldLastDownloaded));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();
    }
  }

  @Test
  public void testAttachingBlobs() throws InterruptedException {

    AssetBlobData assetBlob1 = randomAssetBlob();
    AssetBlobData assetBlob2 = randomAssetBlob();
    AssetData asset = randomAsset(repositoryId);
    String path = asset.path();
    Asset tempResult;

    BlobRef blobRef1 = assetBlob1.blobRef();
    BlobRef blobRef2 = assetBlob2.blobRef();

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetBlobDAO dao = session.access(TestAssetBlobDAO.class);
      dao.createAssetBlob(assetBlob1);
      dao.createAssetBlob(assetBlob2);
      session.access(TestAssetDAO.class).createAsset(asset);
      session.getTransaction().commit();

      assertThat(dao.browseUnusedBlobs(), containsInAnyOrder(blobRef1, blobRef2));
    }

    // ATTACH BLOB

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      assertFalse(tempResult.blob().isPresent());

      asset.setAssetBlob(assetBlob1);
      dao.updateAssetBlobLink(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertTrue(tempResult.blob().isPresent());
      assertThat(tempResult.blob().get(), sameBlob(assetBlob1));
      assertThat(tempResult, sameBlob(asset));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedBlobs(), contains(blobRef2));
    }

    // REPLACE BLOB

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      assertThat(tempResult.blob().get(), sameBlob(assetBlob1));

      asset.setAssetBlob(assetBlob2);
      dao.updateAssetBlobLink(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertTrue(tempResult.blob().isPresent());
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));
      assertThat(tempResult, sameBlob(asset));
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedBlobs(), contains(blobRef1));
    }

    // REPLACING WITH SAME BLOB DOESN'T UPDATE

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));

      asset.setAssetBlob(assetBlob2);
      dao.updateAssetBlobLink(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertTrue(tempResult.blob().isPresent());
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));
      assertThat(tempResult, sameBlob(asset));
      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated));

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedBlobs(), contains(blobRef1));
    }

    // DETACH BLOB

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      assertThat(tempResult.blob().get(), sameBlob(assetBlob2));

      asset.setAssetBlob(null);
      dao.updateAssetBlobLink(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertFalse(tempResult.blob().isPresent());
      assertThat(tempResult.created(), is(oldCreated));
      assertTrue(tempResult.lastUpdated().isAfter(oldLastUpdated));

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedBlobs(), containsInAnyOrder(blobRef1, blobRef2));
    }

    // DETACHING BLOB AGAIN DOESN'T UPDATE

    Thread.sleep(2);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      tempResult = dao.readAsset(repositoryId, path).get();

      DateTime oldCreated = tempResult.created();
      DateTime oldLastUpdated = tempResult.lastUpdated();
      assertFalse(tempResult.blob().isPresent());

      asset.setAssetBlob(null);
      dao.updateAssetBlobLink(asset);

      tempResult = dao.readAsset(repositoryId, path).get();
      assertFalse(tempResult.blob().isPresent());
      assertThat(tempResult.created(), is(oldCreated));
      assertThat(tempResult.lastUpdated(), is(oldLastUpdated));

      session.getTransaction().commit();

      assertThat(session.access(TestAssetBlobDAO.class).browseUnusedBlobs(), containsInAnyOrder(blobRef1, blobRef2));
    }
  }

  @Test
  public void testBrowseComponentAssets() {

    generateRandomNamespaces(100);
    generateRandomNames(100);
    generateRandomVersions(100);

    // scatter components and assets
    generateRandomRepositories(10);
    generateRandomContent(10, 100);

    List<Asset> browsedAssets = new ArrayList<>();

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO assetDao = session.access(TestAssetDAO.class);
      ComponentDAO componentDAO = session.access(TestComponentDAO.class);

      // now gather them back by browsing
      generatedRepositories().forEach(r ->
          componentDAO.browseComponents(r.repositoryId, 10, null).stream()
              .map(ComponentData.class::cast)
              .map(assetDao::browseComponentAssets)
              .forEach(browsedAssets::addAll));
    }

    // we should have the same assets, but maybe in a different order
    // (use hamcrest class directly as javac picks the wrong static varargs method)
    assertThat(browsedAssets, new IsIterableContainingInAnyOrder<>(
        generatedAssets().stream()
            // ignore generated assets without components
            .filter(asset -> asset.component().isPresent())
            .map(ExampleContentTestSupport::samePath)
            .collect(toList())));

    // check assets under a 'detached' entity with no internal id can still be browsed
    ComponentData component = (ComponentData) generatedComponents().get(0);
    component.componentId = null;
    try (DataSession<?> session = sessionRule.openSession("content")) {
      assertTrue(session.access(TestAssetDAO.class).browseComponentAssets(component).stream()
          .map(Asset::component)
          .map(Optional::get)
          .allMatch(sameCoordinates(component)::matches));
    }
  }

  @Test
  public void testContinuationBrowsing() {

    generateRandomNamespaces(1);
    generateRandomNames(1);
    generateRandomVersions(1);
    generateRandomPaths(10000);
    generateRandomRepositories(1);
    generateRandomContent(1, 1000);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      AssetDAO dao = session.access(TestAssetDAO.class);

      int page = 0;

      Continuation<Asset> assets = dao.browseAssets(repositoryId, 10, null);
      while (!assets.isEmpty()) {

        // verify we got the expected slice
        assertThat(assets, new IsIterableContainingInOrder<>(
            generatedAssets()
                .subList(page * 10, (page + 1) * 10)
                .stream()
                .map(ExampleContentTestSupport::samePath)
                .collect(toList())));

        assets = dao.browseAssets(repositoryId, 10, assets.nextContinuationToken());
      }
    }
  }

  @Test
  public void testFlaggedBrowsing() {

    TestAssetData asset1 = randomAsset(repositoryId);
    TestAssetData asset2 = randomAsset(repositoryId);

    try (DataSession<?> session = sessionRule.openSession("content")) {
      TestAssetDAO dao = session.access(TestAssetDAO.class);

      dao.addTestSchema();

      dao.createAsset(asset1);
      dao.createAsset(asset2);

      assertThat(dao.browseFlaggedAssets(repositoryId, 10, null), emptyIterable());

      asset2.setTestFlag(true);
      dao.updateAssetFlag(asset2);

      assertThat(dao.browseFlaggedAssets(repositoryId, 10, null),
          contains(allOf(samePath(asset2), sameAttributes(asset2))));

      asset1.setTestFlag(true);
      dao.updateAssetFlag(asset1);

      asset2.setTestFlag(false);
      dao.updateAssetFlag(asset2);

      assertThat(dao.browseFlaggedAssets(repositoryId, 10, null),
          contains(allOf(samePath(asset1), sameAttributes(asset1))));
    }
  }
}
