package com.l7tech.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DbUpgradeUtilTest {
    @Mock
    private Connection connection;
    @Mock
    private Statement statement;

    @Test
    public void checkVersionFromDatabaseVersionConnectionSqlException() throws Exception {
        when(connection.createStatement()).thenThrow(new SQLException("mocking exception"));
        assertNull(DbUpgradeUtil.checkVersionFromDatabaseVersion(connection));
    }

    @Test
    public void checkVersionFromDatabaseVersionStatementSqlException() throws Exception {
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("mocking exception"));
        assertNull(DbUpgradeUtil.checkVersionFromDatabaseVersion(connection));
    }

    @Test(expected = IOException.class)
    public void getStatementsFromFileDoesNotExist() throws Exception {
        DbUpgradeUtil.getStatementsFromFile("doesnotexist");
    }

    @Test
    public void getStatementsFromFile() throws Exception {
        final URL resource = DbUpgradeUtilTest.class.getClassLoader().getResource("com/l7tech/util/db/upgrade_x-y.sql");
        final String[] statements = DbUpgradeUtil.getStatementsFromFile(resource.getPath());
        assertEquals(2, statements.length);
        final List<String> statementsAsList = Arrays.asList(statements);
        assertTrue(statementsAsList.contains("update some_table set some_column = 'some_value'"));
        assertTrue(statementsAsList.contains("update ssg_version set current_version = 'y'"));
    }

    /**
     * Looks in classpath resource directory com.l7tech.util.db for upgrade scripts.
     */
    @Test
    public void buildUpgradeMap() throws Exception {
        final URL upgradeDirectory = DbUpgradeUtilTest.class.getClassLoader().getResource("com/l7tech/util/db");
        final Map<String, String[]> upgradeMap = DbUpgradeUtil.buildUpgradeMap(new File(upgradeDirectory.toURI()));
        assertEquals(3, upgradeMap.size());
        final String[] xToY = upgradeMap.get("x");
        assertEquals("y", xToY[0]);
        assertTrue(xToY[1].contains("upgrade_x-y.sql"));
        final String[] yToZ = upgradeMap.get("y");
        assertEquals("z", yToZ[0]);
        assertTrue(yToZ[1].contains("upgrade_y-z.sql"));
        final String[] zToAA = upgradeMap.get("z");
        assertEquals("aa", zToAA[0]);
        assertTrue(zToAA[1].contains("upgrade_z-aa"));
        assertNotNull(zToAA[2]);
    }

    @Test
    public void isUpgradeScript() throws Exception {
        // valid
        final Triple<String, String, String> upgradeInfo = DbUpgradeUtil.isUpgradeScript("upgrade_x-y.sql");
        assertNotNull(upgradeInfo);
        assertEquals("x", upgradeInfo.left);
        assertEquals("y", upgradeInfo.middle);

        final Triple<String, String, String> upgradeInfo1 = DbUpgradeUtil.isUpgradeScript("upgrade_x-y_mayFail.sql");
        assertNotNull(upgradeInfo1);
        assertEquals("x", upgradeInfo1.left);
        assertEquals("y", upgradeInfo1.middle);
        assertNotNull(upgradeInfo1.right);

        // invalid
        assertNull(DbUpgradeUtil.isUpgradeScript(""));
        assertNull(DbUpgradeUtil.isUpgradeScript("upgrade_x-y"));
        assertNull(DbUpgradeUtil.isUpgradeScript("upgrade_x-y_something.sql"));
        assertNull(DbUpgradeUtil.isUpgradeScript("upgrade_x.sql"));
        assertNull(DbUpgradeUtil.isUpgradeScript("notupgrade"));
        assertNull(DbUpgradeUtil.isUpgradeScript("upgrade_x-y.txt"));
    }
}
