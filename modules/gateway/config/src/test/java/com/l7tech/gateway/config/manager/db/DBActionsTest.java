package com.l7tech.gateway.config.manager.db;

import com.l7tech.gateway.config.manager.AccountReset;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.SyspropUtil;
import org.junit.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

/**
 Tests the DBActions class. For use only when working on the DBActions class.
 Specifically, this tests the version check and upgrade methods

 NOTE: DO NOT INCLUDE THIS IS THE NIGHTLY BUILD !!!!

 since it requires the ssg.sql file for each of the versions below in
 the classpath, named as follows and will fail otherwise:
        ssg[version].sql, where version is the version number with dots. Ex ssg3.4.sql
 */
@Ignore("This test modifies the database on localhost")
public class DBActionsTest {
    private String currentVersion = null;
    String[] versions = new String[] {"6.2"};

    private String hostname = "localhost";
    private int port = 3306;
    private String dbName = "ssg";
    private String privUsername = "root";
    private String privPassword = "7layer";
    private String username = "gateway";
    private String password = "7layer";

    DBActions dbActions = null;
    private boolean DBalreadyCreated;

    public DBActionsTest() {
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
    }

    @Before
    public void setUp() throws Exception {
        if (!DBalreadyCreated) {
            SyspropUtil.setProperty( "com.l7tech.server.home", "/ssg" );
            dbActions = new DBActions();
            DBActions.StatusType result = createTestDatabases(dbActions);
            Assert.assertEquals(DBActions.StatusType.SUCCESS, result);
            DBalreadyCreated = true;
        }
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.server.home"
        );
    }

    @Test
    public void testCheckDbVersion() {
        System.out.println("------- Testing DB Versions -------");

        for (String realVersion : versions) {
            String versionName = realVersion.replaceAll("\\.", "");

            String dbVersion = dbActions.checkDbVersion(getDatabaseConfig(dbName + versionName));
            Assert.assertEquals(dbName + versionName + " has a problem with the version. Wanted " + realVersion + " but detected " + dbVersion,
                    true, dbVersion != null && dbVersion.equals(realVersion));
        }
    }

    @Test
    public void testUpgrade() {
        System.out.println("------- Testing DB Upgrade Process -------");
        String dbVersion;
        for (String realVersion : versions) {
            String versionName = realVersion.replaceAll("\\.", "");
            dbVersion = dbActions.checkDbVersion(getDatabaseConfig(dbName + versionName));

            if (currentVersion.equals(dbVersion)) {
                continue;
            }
            System.out.println("Upgrading " + dbName + versionName + " from " + dbVersion + " to " + currentVersion);
            DBActions.DBActionsResult upgradeStatus;
            try {
                upgradeStatus = dbActions.upgradeDbSchema(getDatabaseConfig(dbName + versionName), false, dbVersion, currentVersion, getSchemaPath(), null);
                Assert.assertEquals("Failed upgrade procedure. upgradeStatus != success [" + upgradeStatus.getErrorMessage() + "]", 
                                    DBActions.StatusType.SUCCESS, upgradeStatus.getStatus());
                dbVersion = dbActions.checkDbVersion(getDatabaseConfig(dbName + versionName));
                Assert.assertEquals("The version of the upgraded DB is incorrect", currentVersion, dbVersion);
            } catch (IOException e) {
                Assert.fail(e.getMessage());
            }
        }
    }

    @Test
    public void testDBVersionSorter() {
        //expect them sorted in reverse order
        String[] goldVersions = new String[] {
            "4.0",
            "3.5",
            "3.4",
            "3.3",
            "3.2",
        };

        //out of order, this should be sorted by the next bit
        DbVersionChecker[] testCheckers = new DbVersionChecker[] {
            new DbVersion35Checker(),
            new DbVersion3132Checker(),
            new DbVersion34Checker(),
            new DbVersion36Checker(),
            new DbVersion33Checker()
        };

        Arrays.sort(testCheckers, Collections.reverseOrder());
        String[] testVersions = new String[testCheckers.length];

        for(int i = 0; i < testCheckers.length; ++i) {
            testVersions[i] = testCheckers[i].getVersion();
        }

        Assert.assertEquals("The sorted versions do not match the expected order (expected " + Arrays.asList(goldVersions) + " but got " + Arrays.asList(testVersions) + ")", true, Arrays.equals(goldVersions, testVersions));
    }

    @Ignore("This test modifies the database on localhost")
    @Test public void testAccountReset() throws Exception{
        AccountReset.resetAccount(getDatabaseConfig("blah"),"newAdmin","layer7");
    }

    private DatabaseConfig getDatabaseConfig( String dbName ) {
        DatabaseConfig config = new DatabaseConfig(hostname, port, dbName, username, password);
        config.setDatabaseAdminUsername(privUsername);
        config.setDatabaseAdminPassword(privPassword);
        return config;
    }

    private String getSchemaPath() {
        return "etc/db/mysql/ssg.sql";    
    }

    private DBActions.StatusType createTestDatabases(DBActions dbActions) throws IOException {
        System.out.println("------- Creating test databases -------");
        DBActions.DBActionsResult result = null;
        for (String realVersion : versions) {
            String versionName = realVersion.replaceAll("\\.", "");
            result = dbActions.createDb(getDatabaseConfig(dbName+versionName),
                    null,
                    "ssg" + realVersion + ".sql", true);
            if (result.getStatus() != DBActions.StatusType.SUCCESS) {
                System.out.println("Could not create database realVersion: " + realVersion +
                        "\n----------------------------------------\n");
                return DBActions.StatusType.UNKNOWN_FAILURE;
            }
        }
        return DBActions.StatusType.SUCCESS;
    }
}
