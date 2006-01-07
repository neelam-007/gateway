package com.l7tech.server.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.server.config.exceptions.UnsupportedOsException;

import java.io.IOException;

/**
 Tests the DBActions class
 Specifically, this tests the version check and upgrade methods

 NOTE: requires the ssg.sql file for each of the versions below in the classpath, named as follows:
        ssg[version].sql, where version is the version number with dots. Ex ssg3.4.sql
 */
public class DBActionsTest extends TestCase {
    private final String currentVersion = "4.0";
    String[] versions = new String[] {"3.1", "3.2", "3.3", "3.4", "4.0"};

    private String hostname = "localhost";
    private String dbName = "ssg";
    private String privUsername = "root";
    private String privPassword = "7layer";
    private String username = "gateway";
    private String password = "7layer";

    //boolean doFullTest = false;

    OSSpecificFunctions osFunctions = null;
    DBActions dbActions = null;

    public DBActionsTest(String string) {
        super(string);
    }

    protected void setUp() throws Exception {
        super.setUp();
        try {
            System.setProperty("com.l7tech.server.home", "/ssg");
            osFunctions = OSDetector.getOSSpecificActions();
            dbActions = new DBActions();
            boolean isWindows = osFunctions.isWindows();
            int success = createTestDatabases(dbActions, isWindows);
            assertEquals(success, DBActions.DB_SUCCESS);

        } catch (UnsupportedOsException e) {
            fail(e.getMessage());
        }
    }

    public void testCheckDbVersion() {
        String dbVersion = null;
        for (int i = 0; i < versions.length; i++) {
            String realVersion = versions[i];
            String versionName = realVersion.replaceAll("\\.", "");

            dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);
            assertTrue(dbVersion != null);
            assertTrue(dbName+versionName + " has a problem with the version. Wanted " + realVersion + " but detected " + dbVersion, dbVersion.equals(realVersion));
        }
    }

    public void testUpgrade() {
        String dbVersion = null;
        for (int i = 0; i < versions.length; i++) {
            String realVersion = versions[i];
            String versionName = realVersion.replaceAll("\\.", "");
            dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);

            if (!currentVersion.equals(dbVersion)) {
                System.out.println("Upgrading " + dbName+versionName + " from " + dbVersion + " to " + currentVersion);
                int upgradeStatus = DBActions.DB_SUCCESS;
                try {
                    upgradeStatus = dbActions.upgradeDbSchema(hostname, privUsername, privPassword, dbName+versionName, dbVersion, currentVersion, osFunctions);
                    assertEquals("Failed upgrade procedure!!", upgradeStatus, DBActions.DB_SUCCESS);
                    System.out.println("Checking version again - no message beyond here means success");
                    dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);
                    assertEquals("The version of the upgraded DB is incorrect", dbVersion, currentVersion);
                } catch (IOException e) {
                    fail(e.getMessage());
                }
            }
        }
    }

    private int createTestDatabases(DBActions dbActions, boolean windows) throws IOException {
        int success = DBActions.DB_SUCCESS;
        for (int i = 0; i < versions.length; i++) {
            String realVersion = versions[i];
            String versionName = realVersion.replaceAll("\\.", "");
            System.out.println("Creating database - version " + realVersion);
            success = dbActions.createDb(privUsername, privPassword, hostname, dbName+versionName, username, password, "ssg"+realVersion +".sql", windows, true);
            if (success == DBActions.DB_SUCCESS) {
                System.out.println("Success creating database - version " + realVersion +
                        "\n----------------------------------------\n");
            }
            else {
                System.out.println("Could not create database realVersion: " + realVersion +
                    "\n----------------------------------------\n");
                success = DBActions.DB_UNKNOWN_FAILURE;
            }
        }
        return success;
    }

    public static Test suite() {
        return new TestSuite(DBActionsTest.class);
    }

    public static void main(String[] args) {
        junit.swingui.TestRunner.run (DBActionsTest.class);
    }

}
