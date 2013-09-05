package com.l7tech.skunkworks;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.*;
import org.xml.sax.SAXException;

import java.beans.ExceptionListener;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Tool that scans a database for XMLDecoder documents that can't be processed with the default ClassFilterBuilder.
 */
public class ScanForXmlDecoder {
    public static final String DB_HOST = "localhost";
    public static final String DB_USER = "root";
    public static final String DB_PASS = "7layer";
    public static final int DB_PORT = 3306;
    public static final String DB_NAME = "ssg711d";

    static DBActions dbActions = new DBActions();

    static DatabaseConfig dbConfig;

    public static void main(String[] args) throws Exception {
        dbConfig = new DatabaseConfig(DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASS);
        Connection conn = dbActions.getConnection(dbConfig, false);

        List<String> tables = getTableNames(conn);

        for (String table : tables) {
            scanTable(conn, table);
        }
    }

    private static void scanTable(Connection conn, String table) throws SQLException, IOException {
        try (PreparedStatement ps = conn.prepareStatement("select * from " + table)) {
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int columns = meta.getColumnCount();
                while (rs.next()) {
                    for (int i = 1; i <= columns; ++i) {
                        String s;
                        switch (meta.getColumnType(i)) {
                            case Types.BLOB:
                                Blob blob = rs.getBlob(i);
                                s = new String(IOUtils.slurpStream(blob.getBinaryStream()));
                                break;
                            case Types.CLOB:
                                Clob clob = rs.getClob(i);
                                long clobLength = clob.length();
                                if (clobLength > Integer.MAX_VALUE)
                                    throw new IllegalStateException("clob too big");
                                s = clob.getSubString(1, (int) clobLength);
                                break;
                            default:
                                s = rs.getString(i);
                        }
                        scanString(table, meta, i, s);
                    }
                }
            }
        }
    }

    private static void scanString(String table, ResultSetMetaData meta, int col, String data) throws SQLException {
        if (data == null || !data.contains("XMLDecoder"))
            return;

        String label = table + "." + meta.getColumnName(col);

        System.out.print("Found: " + label);

        boolean failed = false;

        try {
            // Ensure well-formed XML
            XmlUtil.stringToDocument(data);

            ExceptionListener fatalListener = SafeXMLDecoderBuilder.getFatalExceptionListener();
            SafeXMLDecoder decoder = new SafeXMLDecoderBuilder(new ByteArrayInputStream(data.getBytes(Charsets.UTF8))).setExceptionListener(fatalListener).build();
            while (true) {
                Object obj;
                try {
                    obj = decoder.readObject();
                } catch (ArrayIndexOutOfBoundsException e) {
                    // read last object OK
                    break;
                }
                if (obj == null) {
                    System.out.print(" nullObj");
                } else {
                    System.out.print(" " + ClassUtils.getJavaTypeName(obj.getClass()));
                }
            }
            System.out.print(" OK");

        } catch (SAXException e) {
            System.out.print(" bad xml: " + ExceptionUtils.getMessage(e));
        } catch (Exception e) {
            System.out.print(" FAIL: " + ExceptionUtils.getMessage(e));
            failed = true;
        } finally {
            if (failed) {
                System.out.println("\n****START DOC**** " + label + "\n");
                System.out.println(data);
                System.out.print("\n****END DOC****");
            }

            System.out.println();
        }
    }

    private static List<String> getTableNames(Connection conn) throws SQLException {
        List<String> tableNames = new ArrayList<>();

        DatabaseMetaData meta = conn.getMetaData();
        ResultSet rs = meta.getTables(conn.getCatalog(), null, null, new String[] { "TABLE" } );
        while (rs.next()) {
            tableNames.add(rs.getString("TABLE_NAME"));
        }

        return tableNames;
    }
}