package com.l7tech.server;

import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertNotNull;

/**
 * Test that ensures the embedded database schema is consistent with the External database schema.
 */
public class EmbeddedDatabaseConsistencyTest {

    static Map<String, TableInfo> externalTables;
    static Map<String, TableInfo> derbyTables;

    // Tables where it is OK if they are present in External DB but not in Derby
    static Set<String> externalOnlyTables = new HashSet<String>(Arrays.asList(
        "hibernate_unique_key",
        "replication_status"
    ));

    static Set<Pair<String,String>> externalOnlyColumns = new HashSet<Pair<String,String>>(Arrays.asList(
        // The policy_xml column of published_service has not been used since version 4.2, when it was moved to the policy table
        new Pair<String, String>("published_service", "policy_xml")
    ));

    // Tables where it is OK if they are present in Derby but not in External DB
    static Set<String> derbyOnlyTables = new HashSet<String>(Arrays.asList(
        "<no derby-only tables yet; if one appears, add it here>"
    ));

    static Set<Pair<String,String>> derbyOnlyColumns = new HashSet<Pair<String,String>>(Arrays.asList(
        new Pair<String, String>("<tablename>", "<no derby-only columns yet; if one appears, add it here>")
    ));

    @BeforeClass
    public static void loadExternalTables() throws Exception {
        URL externalSchemaUrl = new File("etc/db/mysql/ssg.sql").toURI().toURL();
        externalTables = loadDbInfo(externalSchemaUrl);
    }

    @BeforeClass
    public static void loadDerbyTables() throws Exception {
        URL embeddedSchemaUrl = EmbeddedDatabaseConsistencyTest.class.getClassLoader().getResource("com/l7tech/server/resources/ssg_embedded.sql");
        derbyTables = loadDbInfo(embeddedSchemaUrl);
    }

    @Test
    public void testAllExternalColumnsArePresentInEmbeddedDb() throws Exception {
        assertAllLeftColumnsPresentInRight(externalTables, "ssg.sql", derbyTables, "ssg_embedded.sql", externalOnlyTables, externalOnlyColumns);
    }

    @Test
    public void testAllDerbyColumnsArePresentInExternalDb() throws Exception {
        assertAllLeftColumnsPresentInRight(derbyTables, "ssg_embedded.sql", externalTables, "ssg.sql", derbyOnlyTables, derbyOnlyColumns);
    }


    // Ensure all columns in leftTables have a corresponding column in rightTables except for those exceptions explicitly listed
    private static void assertAllLeftColumnsPresentInRight(Map<String, TableInfo> leftTables,
                                                           String leftLabel,
                                                           Map<String, TableInfo> rightTables,
                                                           String rightLabel,
                                                           Set<String> leftOnlyTables,
                                                           Set<Pair<String, String>> leftOnlyColumns)
    {
        for (TableInfo leftTable : leftTables.values()) {
            final String tableName = leftTable.name;
            if (leftOnlyTables.contains(tableName))
                continue;

            TableInfo rightTable = rightTables.get(tableName);
            assertNotNull("Table " + tableName + " is present in " + leftLabel + " but not in " + rightLabel, rightTable);

            for (ColumnInfo leftColumn : leftTable.columns.values()) {
                final String columnName = leftColumn.name;
                if (leftOnlyColumns.contains(new Pair<String, String>(tableName, columnName)))
                    continue;

                ColumnInfo rightColumn = rightTable.columns.get(columnName);
                assertNotNull("Field " + columnName + " of table " + tableName + " is present in " + leftLabel + " but not in " + rightLabel, rightColumn);

                // TODO check types for some kind of consistency maybe?
            }

            // TODO check indexes, constraints, foreign key references, delete cascades?
        }
    }


    static class ColumnInfo {
        final String name;
        final String type;
        final String typeSize;
        final boolean nullable;

        ColumnInfo(String name, String type, String typeSize, boolean nullable) {
            this.name = name;
            this.type = type;
            this.typeSize = typeSize;
            this.nullable = nullable;
        }
    }

    static class TableInfo {
        final String name;

        TableInfo(String name) {
            this.name = name;
        }

        final Map<String, ColumnInfo> columns = new HashMap<String, ColumnInfo>();
    }

    private static Map<String, TableInfo> loadDbInfo(URL schemaFile) throws Exception {
        Map<String, TableInfo> dbInfo = new LinkedHashMap<String, TableInfo>();

        String sql = new String(IOUtils.slurpUrl(schemaFile), Charsets.UTF8).toLowerCase();
        String[] lines = sql.split("(?m)^");
        List<String> lineList = Arrays.asList(lines);

        Pattern createTablePat = Pattern.compile("create table (\\w+)");
        Matcher m;

        Iterator<String> it = lineList.iterator();
        while (it.hasNext()) {
            String line = it.next().trim();
            if (line.startsWith("--"))
                continue;

            m = createTablePat.matcher(line);
            if (m.find()) {
                String tableName = m.group(1);
                TableInfo tableInfo = loadTableInfo(tableName, it);
                dbInfo.put(tableInfo.name, tableInfo);
            } else {
                // TODO handle other statements like INSERT
            }
        }
        return dbInfo;
    }

    private static String stripQuotes(String value) {
        return value.replaceAll("\"|`", "");
    }

    private static TableInfo loadTableInfo(String name, Iterator<String> lineIterator) throws Exception {
        TableInfo tableInfo = new TableInfo(name);
        while (lineIterator.hasNext()) {
            String line = lineIterator.next().trim().replaceAll(",$", "");
            if (line.startsWith("--"))
                continue;
            if (line.startsWith(")"))
                break;
            if (line.length() < 1)
                continue;
            if (line.startsWith("primary key ") || line.startsWith("primary key(")) // TODO handle primary keys
                continue;
            if (line.startsWith("index ") || line.startsWith("index(")) // TODO handle indexes
                continue;
            if (line.startsWith("key ") || line.startsWith("key(")) // TODO handle indexes
                continue;
            if (line.startsWith("unique ") || line.startsWith("unique(")) // TODO handle indexes
                continue;
            if (line.startsWith("constraint ")) // TODO handle foreign key constraints
                continue;
            if (line.startsWith("foreign key ") || line.startsWith("foreign key(")) // TODO handle foreign key constraints
                continue;
            String[] f = line.split("\\s+");
            String fieldName = f[0];
            fieldName = stripQuotes(fieldName);

            String fieldType = f.length > 1 ? f[1].trim() : null;
            String typeSize = null;
            Matcher typeSizeMatcher = fieldType == null ? null : Pattern.compile("^(.*)\\((\\d+)\\)$").matcher(fieldType);
            if (typeSizeMatcher != null && typeSizeMatcher.matches()) {
                fieldType = typeSizeMatcher.group(1);
                typeSize = typeSizeMatcher.group(2);
            }

            boolean nullable = !line.endsWith("not null");

            ColumnInfo col = new ColumnInfo(fieldName, fieldType, typeSize, nullable);
            tableInfo.columns.put(fieldName, col);
        }
        return tableInfo;
    }
}
