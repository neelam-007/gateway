package com.l7tech.server.util;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.transaction.PlatformTransactionManager;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Integration test for EmbeddedDbSchemaUpdater which uses an in-memory derby database.
 */
public class EmbeddedDbSchemaUpdaterTest {
    private TestableEmbeddedDbSchemaUpdater updater;
    private PlatformTransactionManager transactionManager;
    private EmbeddedDatabase database;
    private String softwareVersion;

    @Before
    public void setup() throws IOException {
        database = new EmbeddedDatabaseBuilder().setName("testDerbyDb").setType(EmbeddedDatabaseType.DERBY)
                .addScript("classpath:com/l7tech/server/util/db/derby/test_ssg_embedded.sql").build();
        transactionManager = new DataSourceTransactionManager(database);
        updater = new TestableEmbeddedDbSchemaUpdater(transactionManager, "com/l7tech/server/util/db/derby");
        updater.setDataSource(database);
        softwareVersion = "3.2.0";
    }

    @After
    public void teardown() {
        database.shutdown();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorUpgradeScriptDirectoryDoesNotExist() throws IOException {
        updater = new TestableEmbeddedDbSchemaUpdater(transactionManager, "does/not/exist");
    }

    @Test
    public void ensureCurrentSchema() {
        assertEquals("1.0.0", getDbVersion());
        updater.ensureCurrentSchema();
        assertEquals("3.2.0", getDbVersion());
    }

    @Test
    public void ensureCurrentSchemaUpgradeNotRequired() {
        softwareVersion = "1.0.0";
        assertEquals("1.0.0", getDbVersion());
        updater.ensureCurrentSchema();
        // should not have been updated
        assertEquals("1.0.0", getDbVersion());
    }

    @Test(expected = SchemaUpdater.SchemaException.class)
    public void ensureCurrentSchemaNoUpgradePath() {
        // there are no upgrade scripts for version 5
        softwareVersion = "5.0.0";
        assertEquals("1.0.0", getDbVersion());

        try {
            updater.ensureCurrentSchema();
        } catch (final SchemaUpdater.SchemaException e) {
            // should not have been updated
            assertEquals("1.0.0", getDbVersion());
            throw e;
        }

        fail("expected SchemaException");
    }

    @Test(expected = SchemaUpdater.SchemaException.class)
    public void ensureCurrentSchemaFailure() {
        // cause the update to fail due to table already exists error
        updater.getJdbcTemplate().execute("create table ugprade_v2_to_v3 (\n" +
                "    objectid bigint not null,\n" +
                "    primary key (objectid)\n" +
                ")");

        try {
            updater.ensureCurrentSchema();
        } catch (final SchemaUpdater.SchemaException e) {
            // should be rolled back
            assertEquals("1.0.0", getDbVersion());
            throw e;
        }

        fail("expected SchemaException");
    }

    @Test(expected = SchemaUpdater.SchemaException.class)
    public void ensureCurrentSchemaCannotReadDbVersion() {
        updater.getJdbcTemplate().execute("drop table ssg_version");
        updater.ensureCurrentSchema();
    }

    @Test(expected = SchemaUpdater.SchemaException.class)
    public void ensureCurrentSchemaNullProductVersion() {
        softwareVersion = null;
        updater.ensureCurrentSchema();
    }

    private String getDbVersion() {
        return updater.getJdbcTemplate().queryForObject("select current_version from ssg_version", String.class);
    }

    private class TestableEmbeddedDbSchemaUpdater extends EmbeddedDbSchemaUpdater {

        public TestableEmbeddedDbSchemaUpdater(@NotNull final PlatformTransactionManager transactionManager, @NotNull final String upgradeScriptDirectory) throws IOException {
            super(transactionManager, upgradeScriptDirectory);
        }

        @Override
        String getProductVersion() {
            return softwareVersion;
        }
    }
}
