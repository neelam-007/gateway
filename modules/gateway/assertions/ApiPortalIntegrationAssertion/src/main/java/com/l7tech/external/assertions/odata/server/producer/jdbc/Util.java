package com.l7tech.external.assertions.odata.server.producer.jdbc;

import org.odata4j.core.Throwables;
import org.odata4j.edm.EdmEntitySet;
import org.odata4j.edm.EdmProperty;
import org.odata4j.expression.EntitySimpleProperty;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rraquepo, 8/28/13
 */
public class Util {
    /**
     * Converts a string to a Pascal Case string
     */
    public static String constantToPascalCase(String constantCase, boolean makePlural) {
        String[] tokens = constantCase.split("_");
        StringBuilder sb = new StringBuilder();
        for (String token : tokens) {
            if (token.isEmpty())
                continue;
            sb.append(Character.toUpperCase(token.charAt(0)));
            if (token.length() > 1)
                sb.append(token.substring(1).toLowerCase());
        }
        if (makePlural) {
            if (!sb.toString().endsWith("s")) {
                sb.append("s");
            }
        }
        return sb.toString();
    }

    /**
     * Return database name from the given connection. <b>Note:</b>This method will not close the database connection.
     */
    public static String getDatabaseTypeName(Connection conn) {
        String databaseName = null;
        try {
            databaseName = (conn.getMetaData().getDriverName() + "," + conn.getMetaData().getDatabaseProductName()).toLowerCase();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
        return databaseName;
    }

    public static String getColumnName(JdbcMetadataMapping mapping, EdmEntitySet entitySet, EntitySimpleProperty entitySimpleProperty) {
        return getColumnName(mapping, entitySet, entitySimpleProperty.getPropertyName());
    }

    public static String getColumnName(JdbcMetadataMapping mapping, EdmEntitySet entitySet, String propertyName) {
        EdmProperty edmProperty = entitySet.getType().findDeclaredProperty(propertyName);
        if (edmProperty != null) {
            JdbcModel.JdbcColumn column = mapping.getMappedColumn(edmProperty);
            return column.columnName;
        }
        return null;
    }

    public static JdbcModel.JdbcTable getTable(JdbcMetadataMapping mapping, EdmEntitySet entitySet) {
        return mapping.getMappedTable(entitySet);
    }

    /**
     * Since the custom query is user entered values it  might contain some formatting characters that we don't need. This method tries
     * to normalize the query to not include any of those characters
     */
    public static String normalizeQuery(final String query) {
        String normalizeQuey = new String(query);
        normalizeQuey = normalizeQuey.replaceAll("\\n", " ");
        normalizeQuey = normalizeQuey.replaceAll("\\r", " ");
        normalizeQuey = normalizeQuey.replaceAll("\\t", " ");
        normalizeQuey = normalizeQuey.replaceAll("  ", " ");//double space to single space
        while (true) {
            if (normalizeQuey.indexOf("  ") >= 0) {
                normalizeQuey = normalizeQuey.replaceAll("  ", " ");//double space to single space
            } else {
                break;
            }
        }
        return normalizeQuey.trim();
    }

    public static String unPascalize(String str) {
        StringBuffer unpascalize = new StringBuffer();
        for (int i = 0, n = str.length(); i < n; i++) {
            char c = str.charAt(i);
            if (i == 0) {
                unpascalize.append(c);
                continue;
            }
            if (Character.isUpperCase(c)) {
                unpascalize.append("_");
                unpascalize.append(c);
            } else {
                unpascalize.append(c);
            }
        }
        return unpascalize.toString();
    }

    /**
     * Returns the
     */
    public static List<String> parseFieldsFromQuery(final String query) {
        String query2 = query.toUpperCase();
        String[] tokenFields = query.substring(query2.indexOf("SELECT ") + "SELECT ".length(), query2.indexOf(" FROM ")).split(",");
        List<String> fields = new ArrayList<>();
        String tmpStr = null;
        for (String str : tokenFields) {
            //try to determine if it's a comma with a function?
            if (str.indexOf("(") > 0 || tmpStr != null) {
                if (tmpStr == null) {
                    tmpStr = str;
                } else {
                    tmpStr = tmpStr + "," + str;
                }
                //check if the function is fully complete
                if (tmpStr.split("\\(").length == tmpStr.split("\\)").length) {
                    fields.add(tmpStr.trim());
                    tmpStr = null;
                }
            } else {
                tmpStr = null;
                fields.add(str.trim());

            }
        }
        return fields;
    }


}
