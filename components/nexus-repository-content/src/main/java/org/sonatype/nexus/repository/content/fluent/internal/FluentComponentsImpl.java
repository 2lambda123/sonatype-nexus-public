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
package org.sonatype.nexus.repository.content.fluent.internal;

import java.util.Collection;

import org.sonatype.nexus.common.entity.Continuation;
import org.sonatype.nexus.datastore.mybatis.ContinuationArrayList;
import org.sonatype.nexus.repository.content.Component;
import org.sonatype.nexus.repository.content.facet.ContentFacetSupport;
import org.sonatype.nexus.repository.content.fluent.FluentComponent;
import org.sonatype.nexus.repository.content.fluent.FluentComponentBuilder;
import org.sonatype.nexus.repository.content.fluent.FluentComponents;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * {@link FluentComponents} implementation.
 *
 * @since 3.next
 */
public class FluentComponentsImpl
    implements FluentComponents
{
  private final ContentFacetSupport facet;

  public FluentComponentsImpl(final ContentFacetSupport facet) {
    this.facet = checkNotNull(facet);
  }

  @Override
  public FluentComponentBuilder name(final String name) {
    return new FluentComponentBuilderImpl(facet, name);
  }

  @Override
  public FluentComponent with(final Component component) {
    return new FluentComponentImpl(facet, component);
  }

  @Override
  public Continuation<FluentComponent> browse(final int limit, final String continuationToken) {
    return new ContinuationArrayList(
        /* facet.componentStore().browseComponents(limit, limit, continuationToken) */);
  }

  @Override
  public Collection<String> namespaces() {
    return facet.componentStore().browseNamespaces(facet.contentRepositoryId());
  }

  @Override
  public Collection<String> names(final String namespace) {
    return facet.componentStore().browseNames(facet.contentRepositoryId(), namespace);
  }

  @Override
  public Collection<String> versions(final String namespace, final String name) {
    return facet.componentStore().browseVersions(facet.contentRepositoryId(), namespace, name);
  }
}
