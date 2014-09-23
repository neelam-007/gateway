package com.l7tech.server.management.db;

import com.l7tech.util.*;
import org.junit.Assert;

import java.nio.ByteBuffer;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This was created: 8/12/13 as 1:26 PM
 *
 * @author Victor Kazakov
 */
public class DbCompareTestUtils {

    public static void compareNewToUpgradedDatabase(Connection databaseOneConnection, Connection databaseTwoConnection) throws SQLException {
        compareNewToUpgradedDatabase(databaseOneConnection, databaseTwoConnection, true);
    }

    public static void compareNewToUpgradedDatabase(Connection databaseOneConnection, Connection databaseTwoConnection, boolean checkFunctionsAndProcedures) throws SQLException {

        DatabaseMetaData newDatabaseMetadata = databaseOneConnection.getMetaData();
        DatabaseMetaData upgradedDatabaseMetadata = databaseTwoConnection.getMetaData();

        // ******************************* Compare tables ********************************************* //
        Map<String, Map<String, String>> newTablesInfo = getResultSetInfo(newDatabaseMetadata.getTables(null, "APP", null, null), 3);
        Map<String, Map<String, String>> upgradedTablesInfo = getResultSetInfo(upgradedDatabaseMetadata.getTables(null, "APP", null, null), 3);

        //remove the database change log tables for the comparisons.
        newTablesInfo.remove("databasechangelog");
        upgradedTablesInfo.remove("databasechangelog");
        newTablesInfo.remove("databasechangeloglock");
        upgradedTablesInfo.remove("databasechangeloglock");
        newTablesInfo.remove("DATABASECHANGELOG");
        upgradedTablesInfo.remove("DATABASECHANGELOG");
        newTablesInfo.remove("DATABASECHANGELOGLOCK");
        upgradedTablesInfo.remove("DATABASECHANGELOGLOCK");

        compareResultInfo(newTablesInfo, upgradedTablesInfo,
                "The table sizes need to be equal.",
                "The upgraded database is missing table: %1$s",
                "The new table and upgrade table have different values for a property. Table: %1$s property: %2$s", CollectionUtils.set("TABLE_CAT"));

        // ******************************* Compare table Columns ********************************************* //
        for (String tableName : newTablesInfo.keySet()) {
            Map<String, Map<String, String>> newTableColumnsInfo = getResultSetInfo(newDatabaseMetadata.getColumns(null, "APP", tableName, null), 4);
            Map<String, Map<String, String>> upgradedTableColumnsInfo = getResultSetInfo(upgradedDatabaseMetadata.getColumns(null, "APP", tableName, null), 4);

            // ignore Ordinal position for now. We will not enforce column ordering for derby.
            compareResultInfo(newTableColumnsInfo, upgradedTableColumnsInfo,
                    "Table " + tableName + " has a different number of columns",
                    "The upgraded database is missing column: %1$s for table " + tableName,
                    "For table " + tableName + " the new column and upgrade column have different values for a property. Column: %1$s property: %2$s", CollectionUtils.set("TABLE_CAT", "ORDINAL_POSITION"));
        }

        // ***************************** Check table constraints ******************************************//
        for (String tableName : newTablesInfo.keySet()) {
            Set<Map<String, String>> newTableColumnsInfo = getResultSetInfo(newDatabaseMetadata.getImportedKeys(null, "APP", tableName));
            Set<Map<String, String>> upgradedTableColumnsInfo = getResultSetInfo(upgradedDatabaseMetadata.getImportedKeys(null, "APP", tableName));


            // ignore Ordinal position for now. We will not enforce column ordering for derby.
            compareResultInfo(newTableColumnsInfo, upgradedTableColumnsInfo,
                    "Table " + tableName + " has a different number of foreign key references.",
                    "The upgraded database is missing foreign key reference on table " + tableName,
                    CollectionUtils.set("FKTABLE_CAT", "PKTABLE_CAT", "PK_NAME", "FK_NAME"));
        }

        // ***************************** Check table index's ******************************************//
        for (String tableName : newTablesInfo.keySet()) {
            Set<Map<String, String>> newTableColumnsInfo = getResultSetInfo(newDatabaseMetadata.getIndexInfo(null, "APP", tableName, false, true));
            Set<Map<String, String>> upgradedTableColumnsInfo = getResultSetInfo(upgradedDatabaseMetadata.getIndexInfo(null, "APP", tableName, false, true));


            // ignore Ordinal position for now. We will not enforce column ordering for derby.
            compareResultInfo(newTableColumnsInfo, upgradedTableColumnsInfo,
                    "Table " + tableName + " has a different number of indexes.",
                    "The upgraded database is missing index on table " + tableName,
                    CollectionUtils.set("TABLE_CAT", "INDEX_NAME", "CARDINALITY"));
        }

        // ***************************** Check table data in goid tables ******************************************//
        Set<String> oidTables = CollectionUtils.set("replication_status");
        Map<String, String> otherTables = CollectionUtils.MapBuilder.<String, String>builder()
                .put("ssg_version", "current_version")
                .map();
        Set<String> ignoreTables = new HashSet<>(CollectionUtils.set("cluster_master", "hibernate_unique_key", "goid_upgrade_map", "databasechangelog", "databasechangeloglock"));
        ignoreTables.addAll(oidTables);
        ignoreTables.addAll(otherTables.keySet());
        Set<String> ignoreProperties = CollectionUtils.set();
        for (String tableName : newTablesInfo.keySet()) {
            if (ignoreTables.contains(tableName.toLowerCase())) continue;
            Statement db1SelectAllStatement = databaseOneConnection.createStatement();
            Statement db2SelectAllStatement = databaseTwoConnection.createStatement();

            Map<String, Map<String, String>> db1TableData = getResultSetInfo(db1SelectAllStatement.executeQuery("select * from " + tableName), "goid");
            Map<String, Map<String, String>> db2TableData = getResultSetInfo(db2SelectAllStatement.executeQuery("select * from " + tableName), "goid");

            for (String key : db1TableData.keySet()) {
                Map<String, String> db1RowData = db1TableData.get(key);
                Map<String, String> db2RowData = db2TableData.get(key);

                Assert.assertNotNull("Missing data from 2nd database in table: "+tableName+". Key: " + key + "\nMissing Row: " + db1RowData.toString(), db2RowData);

                for (String tableData : db1RowData.keySet()) {
                    if (ignoreProperties.contains(tableData.toLowerCase())) continue;
                    String newTablePropertyValue = db1RowData.get(tableData);
                    String upgradedTablePropertyValue = db2RowData.get(tableData);

                    Assert.assertEquals("Table data does not match for table: "+tableName+" Property: " +tableData + " for key: " + key, newTablePropertyValue, upgradedTablePropertyValue);
                }
            }
        }

        // ***************************** Check table data objectid tables ******************************************//
        ignoreProperties = CollectionUtils.set();
        for (String tableName : newTablesInfo.keySet()) {
            if (!oidTables.contains(tableName.toLowerCase())) continue;
            Statement db1SelectAllStatement = databaseOneConnection.createStatement();
            Statement db2SelectAllStatement = databaseTwoConnection.createStatement();

            Map<String, Map<String, String>> db1TableData;
            try {
                db1TableData = getResultSetInfo(db1SelectAllStatement.executeQuery("select * from " + tableName), "objectid");
            } catch (SQLException e) {
                throw new SQLException("Error loading 1st database data for objectid table " + tableName + ": " + ExceptionUtils.getMessage(e), e);
            }

            Map<String, Map<String, String>> db2TableData;
            try {
                db2TableData = getResultSetInfo(db2SelectAllStatement.executeQuery("select * from " + tableName), "objectid");
            } catch (SQLException e) {
                throw new SQLException("Error loading 2nd database data for objectid table " + tableName + ": " + ExceptionUtils.getMessage(e), e);
            }

            for (String key : db1TableData.keySet()) {
                Map<String, String> db1RowData = db1TableData.get(key);
                Map<String, String> db2RowData = db2TableData.get(key);

                Assert.assertNotNull("Missing data from 2nd database in table: "+tableName+". Key: " + key + "\nMissing Row: " + db1RowData.toString(), db2RowData);

                for (String tableData : db1RowData.keySet()) {
                    if (ignoreProperties.contains(tableData.toLowerCase())) continue;
                    String newTablePropertyValue = db1RowData.get(tableData);
                    String upgradedTablePropertyValue = db2RowData.get(tableData);

                    Assert.assertEquals("Table data does not match for table: "+tableName+" Property: " +tableData + " for key: " + key + "\nRow: " + db1RowData.toString(), newTablePropertyValue, upgradedTablePropertyValue);
                }
            }
        }

        // ***************************** Check table data in other tables ******************************************//
        for (Map.Entry<String, String> tableName : otherTables.entrySet()) {
            Statement db1SelectAllStatement = databaseOneConnection.createStatement();
            Statement db2SelectAllStatement = databaseTwoConnection.createStatement();

            Map<String, Map<String, String>> db1TableData = getResultSetInfo(db1SelectAllStatement.executeQuery("select * from " + tableName.getKey()), tableName.getValue());
            Map<String, Map<String, String>> db2TableData = getResultSetInfo(db2SelectAllStatement.executeQuery("select * from " + tableName.getKey()), tableName.getValue());

            for (String key : db1TableData.keySet()) {
                Map<String, String> db1RowData = db1TableData.get(key);
                Map<String, String> db2RowData = db2TableData.get(key);

                Assert.assertNotNull("Missing data from 2nd database in table: "+tableName.getKey()+". Key: " + key + "\nMissing Row: " + db1RowData.toString() + "\n2nd Database Rows: " + db2TableData.toString(), db2RowData);

                for (String tableData : db1RowData.keySet()) {
                    if (ignoreProperties.contains(tableData.toLowerCase())) continue;
                    String newTablePropertyValue = db1RowData.get(tableData);
                    String upgradedTablePropertyValue = db2RowData.get(tableData);

                    Assert.assertEquals("Table data does not match for table: "+tableName.getKey()+" Property: " +tableData + " for key: " + key, newTablePropertyValue, upgradedTablePropertyValue);
                }
            }
        }

        if(checkFunctionsAndProcedures) {
            // ***************************** Check functions ******************************************//
            Map<String, Map<String, String>> newFunctionsInfo = getResultSetInfo(newDatabaseMetadata.getFunctions(null, "APP", null), 3);
            Map<String, Map<String, String>> upgradedFunctionsInfo = getResultSetInfo(upgradedDatabaseMetadata.getFunctions(null, "APP", null), 3);
            compareResultInfo(newFunctionsInfo, upgradedFunctionsInfo,
                    "The number of functions need to be equal.",
                    "The upgraded database is missing function: %1$s",
                    "The new function and upgrade function have different values for a property. Function: %1$s property: %2$s", CollectionUtils.set("FUNCTION_CAT", "SPECIFIC_NAME"));

            for(String functionName : newFunctionsInfo.keySet()){
                Map<String, Map<String, String>> newFunctionColumnsInfo = getResultSetInfo(newDatabaseMetadata.getFunctionColumns(null, "APP", functionName, null), 4);
                Map<String, Map<String, String>> upgradedFunctionColumnsInfo = getResultSetInfo(upgradedDatabaseMetadata.getFunctionColumns(null, "APP", functionName, null), 4);

                // ignore Ordinal position for now. We will not enforce column ordering for derby.
                compareResultInfo(newFunctionColumnsInfo, upgradedFunctionColumnsInfo,
                        "Function " + functionName + " has a different number of columns",
                        "The upgraded database is missing column: %1$s for function " + functionName,
                        "For function " + functionName + " the new column and upgrade column have different values for a property. Column: %1$s property: %2$s", CollectionUtils.set("FUNCTION_CAT", "SPECIFIC_NAME"));
            }

            // ***************************** Check procedures ******************************************//
            Map<String, Map<String, String>> newProcedureInfo = getResultSetInfo(newDatabaseMetadata.getProcedures(null, "APP", null), 3);
            Map<String, Map<String, String>> upgradedProcedureInfo = getResultSetInfo(upgradedDatabaseMetadata.getProcedures(null, "APP", null), 3);
            compareResultInfo(newProcedureInfo, upgradedProcedureInfo,
                    "The number of procedures need to be equal.",
                    "The upgraded database is missing procedure: %1$s",
                    "The new procedure and upgrade procedure have different values for a property. Procedure: %1$s property: %2$s", CollectionUtils.set("PROCEDURE_CAT", "SPECIFIC_NAME"));
            for(String procedureName : newProcedureInfo.keySet()){
                Map<String, Map<String, String>> newProcedureColumnsInfo = getResultSetInfo(newDatabaseMetadata.getProcedureColumns(null, "APP", procedureName, null), 4);
                Map<String, Map<String, String>> upgradedProcedureColumnsInfo = getResultSetInfo(upgradedDatabaseMetadata.getProcedureColumns(null, "APP", procedureName, null), 4);

                // ignore Ordinal position for now. We will not enforce column ordering for derby.
                compareResultInfo(newProcedureColumnsInfo, upgradedProcedureColumnsInfo,
                        "Procedure " + procedureName + " has a different number of columns",
                        "The upgraded database is missing column: %1$s for procedure " + procedureName,
                        "For procedure " + procedureName + " the new column and upgrade column have different values for a property. Column: %1$s property: %2$s", CollectionUtils.set("PROCEDURE_CAT", "SPECIFIC_NAME"));
            }
        }
    }

    private static void compareResultInfo(Map<String, Map<String, String>> newInfo, Map<String, Map<String, String>> upgradedInfo, String differentSizesErrorMessage, String missingRowErrorMessage, String propertyValueMismatchErrorMessage) {
        compareResultInfo(newInfo, upgradedInfo, differentSizesErrorMessage, missingRowErrorMessage, propertyValueMismatchErrorMessage, Collections.<String>emptySet());
    }

    private static void compareResultInfo(Map<String, Map<String, String>> newInfo, Map<String, Map<String, String>> upgradedInfo, String differentSizesErrorMessage, String missingRowErrorMessage, String propertyValueMismatchErrorMessage, Set<String> ignoreProperties) {
        Assert.assertEquals(differentSizesErrorMessage + "\nNew db items:      " + newInfo.keySet().toString() + "\nUpgraded db items: " + upgradedInfo.keySet().toString() + "\n", newInfo.size(), upgradedInfo.size());

        for (String rowName : newInfo.keySet()) {
            Map<String, String> newTableInfo = newInfo.get(rowName);
            Map<String, String> upgradedTableInfo = upgradedInfo.get(rowName);

            Assert.assertNotNull(String.format(missingRowErrorMessage, rowName) + "\nMissing Row: " + newTableInfo.toString(), upgradedTableInfo);

            for (String tableProperty : newTableInfo.keySet()) {
                if (ignoreProperties.contains(tableProperty)) continue;
                String newTablePropertyValue = newTableInfo.get(tableProperty);
                String upgradedTablePropertyValue = upgradedTableInfo.get(tableProperty);

                Assert.assertEquals(String.format(propertyValueMismatchErrorMessage, rowName, tableProperty), newTablePropertyValue, upgradedTablePropertyValue);
            }
        }
    }

    private static void compareResultInfo(Set<Map<String, String>> newInfo, Set<Map<String, String>> upgradedInfo, String differentSizesErrorMessage, String missingRowErrorMessage, final Set<String> ignoreProperties) {
        Assert.assertEquals(differentSizesErrorMessage + "\nNew db items:      " + newInfo.toString() + "\nUpgraded db items: " + upgradedInfo.toString() + "\n", newInfo.size(), upgradedInfo.size());

        for (final Map<String, String> newTableInfo : newInfo) {

            final AtomicReference<Pair<Integer, Map<String, String>>> closestMatch = new AtomicReference<>(new Pair<Integer, Map<String, String>>(-1, null));

            boolean containsRow = Functions.exists(upgradedInfo, new Functions.Unary<Boolean, Map<String, String>>() {
                @Override
                public Boolean call(Map<String, String> upgradedTableInfo) {
                    boolean matched = true;
                    int numMatches = 0;
                    for (String tableProperty : newTableInfo.keySet()) {
                        if (ignoreProperties.contains(tableProperty)) continue;
                        String newTablePropertyValue = newTableInfo.get(tableProperty);
                        String upgradedTablePropertyValue = upgradedTableInfo.get(tableProperty);

                        if ((newTablePropertyValue != null && !newTablePropertyValue.equals(upgradedTablePropertyValue)) || (newTablePropertyValue == null && upgradedTablePropertyValue != null)) {
                            matched = false;
                        } else {
                            numMatches++;
                        }
                    }
                    if (matched || closestMatch.get().getKey() < numMatches) {
                        closestMatch.set(new Pair<>(numMatches, upgradedTableInfo));
                    }
                    return matched;
                }
            });

            Assert.assertTrue(String.format(missingRowErrorMessage) + "\nMissing Row:   " + newTableInfo.toString() + "\nClosest Match: " + (closestMatch.get().getValue() != null ? closestMatch.get().getValue().toString() : "null") + "\nUpgraded DB rows: " + upgradedInfo.toString(), containsRow);
        }
    }

    private static Map<String, Map<String, String>> getResultSetInfo(ResultSet resultSet, String idColumn) throws SQLException {
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
            final Object key = resultSet.getObject(idColumn);
            Assert.assertNotNull("The row key cannot be null.", key);
            final String keyString;
            if (key instanceof byte[]) {
                ByteBuffer buffer = ByteBuffer.wrap((byte[]) key);
                keyString = HexUtils.hexDump((byte[]) key) + " (" + buffer.getLong() + "," + buffer.getLong() + ")";
            } else {
                keyString = key.toString();
            }
            resultSetInfoMap.put(keyString, rowMap);
        }
        return resultSetInfoMap;
    }

    public static Map<String, Map<String, String>> getResultSetInfo(ResultSet resultSet, int idColumn) throws SQLException {
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
            final String key = resultSet.getString(idColumn);
            Assert.assertNotNull("The row key cannot be null.", key);
            resultSetInfoMap.put(key, rowMap);
        }
        return resultSetInfoMap;
    }

    private static Set<Map<String, String>> getResultSetInfo(ResultSet resultSet) throws SQLException {
        HashSet<Map<String, String>> resultSetInfoSet = new HashSet<>();
        ArrayList<String> columnList = new ArrayList<>();
        for (int i = 1; i < resultSet.getMetaData().getColumnCount() + 1; i++) {
            columnList.add(resultSet.getMetaData().getColumnName(i));
        }
        while (resultSet.next()) {
            HashMap<String, String> rowMap = new HashMap<>();
            for (int i = 1; i < resultSet.getMetaData().getColumnCount() + 1; i++) {
                rowMap.put(columnList.get(i - 1), resultSet.getString(i));
            }
            resultSetInfoSet.add(rowMap);
        }
        return resultSetInfoSet;
    }
}
