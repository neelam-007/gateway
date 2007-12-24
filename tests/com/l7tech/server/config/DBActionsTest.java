package com.l7tech.server.config;

import com.l7tech.common.BuildInfo;
import com.l7tech.server.config.db.*;
import com.l7tech.server.config.exceptions.UnsupportedOsException;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;

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
public class DBActionsTest extends TestCase {
    private String currentVersion = null;
    String[] versions = new String[] {"3.1", "3.2", "3.3", "3.4", "3.5", "4.0"};

    private String hostname = "localhost";
    private String dbName = "ssg";
    private String privUsername = "root";
    private String privPassword = "7layer";
    private String username = "gateway";
    private String password = "7layer";

    OSSpecificFunctions osFunctions = null;
    DBActions dbActions = null;
    private boolean DBalreadyCreated;

    public DBActionsTest(String string) {
        super(string);
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
    }

    protected void setUp() throws Exception {

        if (!DBalreadyCreated) {
            super.setUp();
            try {
                System.setProperty("com.l7tech.server.home", "/ssg");
                osFunctions = OSDetector.getOSSpecificFunctions();
                dbActions = new DBActions();
                boolean isWindows = osFunctions.isWindows();
                DBActions.DBActionsResult result = createTestDatabases(dbActions, isWindows);
                assertEquals(DBActions.DB_SUCCESS, result.getStatus());
                DBalreadyCreated = true;

            } catch (UnsupportedOsException e) {
                fail(e.getMessage());
            }
        }
    }

    public void testCheckDbVersion() {
        System.out.println("------- Testing DB Versions -------");

        for (int i = 0; i < versions.length; i++) {
            String realVersion = versions[i];
            String versionName = realVersion.replaceAll("\\.", "");

            String dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);
            assertEquals(dbName + versionName + " has a problem with the version. Wanted " + realVersion + " but detected " + dbVersion,
                    true, dbVersion != null && dbVersion.equals(realVersion));
        }
    }

    public void testUpgrade() {
        System.out.println("------- Testing DB Upgrade Process -------");
        String dbVersion = null;
        for (int i = 0; i < versions.length; i++) {
            String realVersion = versions[i];
            String versionName = realVersion.replaceAll("\\.", "");
            dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);

            if (currentVersion.equals(dbVersion)) {
                continue;
            }
            System.out.println("Upgrading " + dbName+versionName + " from " + dbVersion + " to " + currentVersion);
            DBActions.DBActionsResult upgradeStatus = new DBActions.DBActionsResult();
            try {
                upgradeStatus = dbActions.upgradeDbSchema(hostname, privUsername, privPassword, dbName+versionName,
                        dbVersion, currentVersion);
                assertEquals("Failed upgrade procedure. upgradeStatus != success [" + upgradeStatus.getErrorMessage() + "]", DBActions.DB_SUCCESS, upgradeStatus.getStatus());
                dbVersion = dbActions.checkDbVersion(hostname, dbName+versionName, username, password);
                assertEquals("The version of the upgraded DB is incorrect", currentVersion, dbVersion);
            } catch (IOException e) {
                fail(e.getMessage());
            }
        }
    }

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

        assertEquals("The sorted versions do not match the expected order (expected " + Arrays.asList(goldVersions) + " but got " + Arrays.asList(testVersions) + ")", true, Arrays.equals(goldVersions, testVersions));
    }

    private DBActions.DBActionsResult createTestDatabases(DBActions dbActions, boolean windows) throws IOException {
        System.out.println("------- Creating test databases -------");
        DBActions.DBActionsResult result = null;
        for (int i = 0; i < versions.length; i++) {
            String realVersion = versions[i];
            String versionName = realVersion.replaceAll("\\.", "");
            result = dbActions.createDb(privUsername, privPassword, hostname, dbName+versionName, username, password,
                    "ssg"+realVersion +".sql", windows, true);
            if (result.getStatus() != DBActions.DB_SUCCESS) {
                System.out.println("Could not create database realVersion: " + realVersion +
                    "\n----------------------------------------\n");
                result.setStatus(DBActions.DB_UNKNOWN_FAILURE);
            }
        }
        return result;
    }

    public static Test suite() {
        return new TestSuite(DBActionsTest.class);
    }

    public static void main(String[] args) {
        TestRunner.run (DBActionsTest.class);
    }

}
