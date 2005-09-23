package com.l7tech.server.config;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;
import com.l7tech.server.config.exceptions.UnsupportedOsException;

import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Sep 23, 2005
 * Time: 12:40:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class DBActionsTest extends TestCase {
    private final String currentVersion = "3.4";
    String[] versions = new String[] {"3.1", "3.2", "3.3", "3.4"};

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
                    System.out.println("Checking version again");
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
                System.out.println("Success creating database - version " + realVersion);
            }
            else {
                System.out.println("Could not create database realVersion: " + realVersion);
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
