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
package org.sonatype.nexus.repository.db.datastore;

import java.sql.Connection;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.sql.DataSource;

import org.sonatype.nexus.common.app.FeatureFlags;
import org.sonatype.nexus.common.db.DatabaseCheck;
import org.sonatype.nexus.common.app.FeatureFlag;
import org.sonatype.nexus.common.app.ManagedLifecycle;
import org.sonatype.nexus.common.stateguard.Guarded;
import org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport;
import org.sonatype.nexus.common.upgrade.AvailabilityVersion;
import org.sonatype.nexus.datastore.api.DataStoreManager;

import com.google.common.annotations.VisibleForTesting;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationVersion;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;
import static org.sonatype.nexus.common.app.FeatureFlags.DATASTORE_ENABLED;
import static org.sonatype.nexus.common.app.ManagedLifecycle.Phase.STORAGE;
import static org.sonatype.nexus.common.stateguard.StateGuardLifecycleSupport.State.STARTED;
import static org.sonatype.nexus.datastore.api.DataStoreManager.DEFAULT_DATASTORE_NAME;

@Named
@Singleton
@FeatureFlag(name = DATASTORE_ENABLED)
@ManagedLifecycle(phase = STORAGE)
public class DatabaseCheckImpl
    extends StateGuardLifecycleSupport
    implements DatabaseCheck
{
  private final DataStoreManager dataStoreManager;

  private final boolean datastoreClustered;

  private DataSource dataSource;

  private MigrationVersion currentSchemaVersion;

  private boolean postgresql = false;

  @Inject
  public DatabaseCheckImpl(
      final DataStoreManager dataStoreManager,
      @Named(FeatureFlags.DATASTORE_CLUSTERED_ENABLED_NAMED) final boolean datastoreClustered)
  {
    this.dataStoreManager = checkNotNull(dataStoreManager);
    this.datastoreClustered = datastoreClustered;
  }

  @Override
  protected void doStart() throws Exception {
    dataSource = dataStoreManager.get(DEFAULT_DATASTORE_NAME)
        .orElseThrow(() -> new IllegalStateException("Missing DataStore named: " + DEFAULT_DATASTORE_NAME))
        .getDataSource();

    try (Connection con = dataSource.getConnection()) {
      postgresql = POSTGRE_SQL.equalsIgnoreCase(con.getMetaData().getDatabaseProductName());
    }
  }

  @Guarded(by = STARTED)
  @Override
  public boolean isPostgresql() {
    return postgresql;
  }

  @Override
  public boolean isAllowedByVersion(final Class<?> annotatedClass) {
    if (!datastoreClustered) {
      return true;
    }

    AvailabilityVersion availabilityVersion = annotatedClass.getAnnotation(AvailabilityVersion.class);
    if (availabilityVersion != null && isAllowed(availabilityVersion.from())) {
      return true;
    }
    else if (availabilityVersion == null) {
      log.error("Missing database version specified for {}", annotatedClass);
    }

    log.debug("The database schema version is lower than the minimum required to enable {}", annotatedClass);
    return false;
  }

  private boolean isAllowed(String requiredVersion) {
    if (currentSchemaVersion == null) {
      currentSchemaVersion = getMigrationVersion(dataSource);
      if (currentSchemaVersion == null) {
        return true;
      }
    }

    return currentSchemaVersion.isAtLeast(requiredVersion);
  }

  @VisibleForTesting
  MigrationVersion getMigrationVersion(final DataSource dataSource) {
    if (dataSource == null) {
      log.warn("datasource has not been initialised");
      return null;
    }

    Flyway flyway = Flyway.configure()
        .dataSource(dataSource).load();

    MigrationInfo current = flyway.info().current();
    if (current != null) {
      return current.getVersion();
    }

    log.error("Could not determine database schema version");
    return null;
  }
}
