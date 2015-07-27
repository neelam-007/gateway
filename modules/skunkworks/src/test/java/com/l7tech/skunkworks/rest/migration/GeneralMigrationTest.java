package com.l7tech.skunkworks.rest.migration;

import com.l7tech.skunkworks.rest.migration.tests.GeneralMigration;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * This is here so that the General Migration Test is run on the default (per checkin) build
 */
public class GeneralMigrationTest extends GeneralMigration {
    @BeforeClass
    public static void beforeClass() throws Exception {
        MigrationTestBase.createMigrationEnvironments();
    }

    @AfterClass
    public static void afterClass() {
        MigrationTestBase.destroyMigrationEnvironments();
    }
}
