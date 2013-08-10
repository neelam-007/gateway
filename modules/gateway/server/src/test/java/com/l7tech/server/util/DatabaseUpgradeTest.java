package com.l7tech.server.util;

import com.l7tech.util.DbUpgradeUtil;
import com.l7tech.util.FileUtils;
import org.apache.derby.jdbc.EmbeddedDataSource40;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * This test will compare a freshly created database to one that has been created using the upgrade scripts from an old
 * version.
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class DatabaseUpgradeTest {
    private static final String DB_FOLDER = "_dbtest";
    private static final String NEW_DATABASE_NAME = DB_FOLDER + "/ssg_new_db_test";
    private static final String UPGRADED_DATABASE_NAME = DB_FOLDER + "/ssg_upgraded_db_test";
    //This will need to be upgraded for every
    private String softwareVersion = "";

    private EmbeddedDataSource40 newDBDataSource;
    private EmbeddedDataSource40 upgradeDBDataSource;

    @Mock
    private SessionFactory sessionFactory;

    @Before
    public void before() throws IOException, SQLException {
        File fileDbFolder = new File(DB_FOLDER);
        FileUtils.deleteDir(fileDbFolder);

        newDBDataSource = new EmbeddedDataSource40();
        newDBDataSource.setDatabaseName(NEW_DATABASE_NAME);
        newDBDataSource.setCreateDatabase("create");

        //The new freshly created database
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("com/l7tech/server/resources/ssg_embedded.sql");
        DerbyDbHelper.runScripts(newDBDataSource.getConnection(), resources, false);

        // Gets the version of the new database;
        Connection newDBConnection = newDBDataSource.getConnection();
        softwareVersion = DbUpgradeUtil.checkVersionFromDatabaseVersion(newDBConnection);
        newDBConnection.close();

        upgradeDBDataSource = new EmbeddedDataSource40();
        upgradeDBDataSource.setDatabaseName(UPGRADED_DATABASE_NAME);
        upgradeDBDataSource.setCreateDatabase("create");

        //create a database from the 7.1.0 sql script
        resources = new PathMatchingResourcePatternResolver().getResources("com/l7tech/server/resources/ssg_embedded_7.1.0.sql");
        DerbyDbHelper.runScripts(upgradeDBDataSource.getConnection(), resources, false);

        final DataSourceTransactionManager transactionManager = new DataSourceTransactionManager(upgradeDBDataSource);
        EmbeddedDbSchemaUpdater dbUpdater = new EmbeddedDbSchemaUpdater(transactionManager, "com/l7tech/server/resources/derby") {
            @Override
            String getProductVersion() {
                return softwareVersion;
            }
        };
        dbUpdater.setDataSource(upgradeDBDataSource);

        //upgrade the 7.1.0 database to the latest version
        dbUpdater.ensureCurrentSchema();
    }

    @Test
    public void compareNewToUpgradedDatabase() throws SQLException {
        DatabaseMetaData newDatabaseMetadata = newDBDataSource.getConnection().getMetaData();
        DatabaseMetaData upgradedDatabaseMetadata = upgradeDBDataSource.getConnection().getMetaData();

        // ******************************* Compare tables ********************************************* //
        Map<String, Map<String, String>> newTablesInfo = getResultSetInfo(newDatabaseMetadata.getTables(null, "APP", null, null), 3);
        Map<String, Map<String, String>> upgradedTablesInfo = getResultSetInfo(upgradedDatabaseMetadata.getTables(null, "APP", null, null), 3);

        // remove the goid_upgrade_map table as it will never be in a newly created schema.
        upgradedTablesInfo.remove("goid_upgrade_map".toUpperCase());


        compareResultInfo(newTablesInfo, upgradedTablesInfo,
                "The table sizes need to be equal.",
                "The upgraded database is missing table: %1$s",
                "The new table and upgrade table have different values for a property. Table: %1$s property: %2$s");

        // ******************************* Compare table Columns ********************************************* //
        for (String tableName : newTablesInfo.keySet()) {
            Map<String, Map<String, String>> newTableColumnsInfo = getResultSetInfo(newDatabaseMetadata.getColumns(null, "APP", tableName, null), 4);
            Map<String, Map<String, String>> upgradedTableColumnsInfo = getResultSetInfo(upgradedDatabaseMetadata.getColumns(null, "APP", tableName, null), 4);

//            compareResultInfo(newTableColumnsInfo, upgradedTableColumnsInfo,
//                    "Table " + tableName + " has a different number of columns",
//                    "The upgraded database is missing column: %1$s for table " + tableName,
//                    "For table " + tableName + " the new column and upgrade column have different values for a property. Column: %1$s property: %2$s", CollectionUtils.set("ORDINAL_POSITION"));
        }
    }

    private void compareResultInfo(Map<String, Map<String, String>> newInfo, Map<String, Map<String, String>> upgradedInfo, String differentSizesErrorMessage, String missingRowErrorMessage, String propertyValueMismatchErrorMessage) {
        compareResultInfo(newInfo, upgradedInfo, differentSizesErrorMessage, missingRowErrorMessage, propertyValueMismatchErrorMessage, Collections.<String>emptySet());
    }

    private void compareResultInfo(Map<String, Map<String, String>> newInfo, Map<String, Map<String, String>> upgradedInfo, String differentSizesErrorMessage, String missingRowErrorMessage, String propertyValueMismatchErrorMessage, Set<String> ignoreProperties) {
        Assert.assertEquals(differentSizesErrorMessage, newInfo.size(), upgradedInfo.size());

        for (String tableName : newInfo.keySet()) {
            Map<String, String> newTableInfo = newInfo.get(tableName);
            Map<String, String> upgradedTableInfo = upgradedInfo.get(tableName);

            Assert.assertNotNull(String.format(missingRowErrorMessage, tableName), upgradedTableInfo);

            for (String tableProperty : newTableInfo.keySet()) {
                if (ignoreProperties.contains(tableName)) continue;
                String newTablePropertyValue = newTableInfo.get(tableProperty);
                String upgradedTablePropertyValue = upgradedTableInfo.get(tableProperty);

                Assert.assertEquals(String.format(propertyValueMismatchErrorMessage, tableName, tableProperty), newTablePropertyValue, upgradedTablePropertyValue);
            }
        }
    }

    private Map<String, Map<String, String>> getResultSetInfo(ResultSet resultSet, int idColumn) throws SQLException {
        HashMap<String, Map<String, String>> resultSetInfoMap = new HashMap<>();
        ArrayList<String> columnList = new ArrayList<>();
        for (int i = 1; i < resultSet.getMetaData().getColumnCount() + 1; i++) {
            columnList.add(resultSet.getMetaData().getColumnName(i));
        }
        while (resultSet.next()) {
            HashMap<String, String> rowMap = new HashMap<>();
            for (int i = 1; i < resultSet.getMetaData().getColumnCount() + 1; i++) {
                rowMap.put(columnList.get(i - 1), resultSet.getString(i));
            }
            resultSetInfoMap.put(resultSet.getString(idColumn), rowMap);
        }
        return resultSetInfoMap;
    }
}
