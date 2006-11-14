package com.l7tech.server.flasher;

import com.l7tech.server.config.db.DBActions;

import java.sql.*;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Methods for dumping
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>ma
 */
public class DBDumpUtil {

    /**
     * outputs a database dump
     * @param databaseURL database host
     * @param databaseUser database user
     * @param databasePasswd database password
     * @param includeAudit whether or not audit tables should be included
     * @param outputPath the path where the dump should go to
     */
    public static void dump(String databaseURL, String databaseUser, String databasePasswd,
                            boolean includeAudit, String outputPath) throws SQLException, IOException {
        Connection c = DBActions.getConnection(databaseURL, databaseUser, databasePasswd);
        if (c == null) {
            throw new SQLException("could not connect using url: " + databaseURL + ". with username " + databaseUser + ", and password: " + databasePasswd);
        }

        Statement showTablesStmt = c.createStatement();
        ResultSet tablesList = showTablesStmt.executeQuery("show tables");
        FileOutputStream fos = new FileOutputStream(outputPath);
        System.out.print("Dumping database to " + outputPath + " ..");
        while (tablesList.next()) {
            String tableName = tablesList.getString(1);
            if (tableName.equals("service_metrics")) continue;
            if (!includeAudit) {
                if (tableName.startsWith("audit_")) continue;
            }
            Statement tdata = c.createStatement();
            ResultSet tdataList = tdata.executeQuery("select * from " + tableName);
            while (tdataList.next()) {
                if (tableName.equals("cluster_properties")) { // dont include license in image
                    String tmp = tdataList.getString(3);
                    if (tmp != null && tmp.equals("license")) continue;
                }

                StringBuffer insertStatementToRecord = new StringBuffer("INSERT INTO " + tableName + " VALUES (");
                ResultSetMetaData rowInfo = tdataList.getMetaData();
                for (int i = 1; i <= rowInfo.getColumnCount(); i++) {
                    int colType = rowInfo.getColumnType(i);
                    switch (colType) {
                        case Types.BIGINT:
                            insertStatementToRecord.append(tdataList.getLong(i));
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;

                        case Types.INTEGER:
                        case Types.BIT:
                            insertStatementToRecord.append(tdataList.getInt(i));
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;

                        case Types.DOUBLE:
                            insertStatementToRecord.append(tdataList.getDouble(i));
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;

                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.LONGVARCHAR: // medium text
                        case Types.LONGVARBINARY: // medium blob
                            String tmp = tdataList.getString(i);
                            if (tmp != null) {
                                tmp = tmp.replace("\"", "\\\"");
                                tmp = tmp.replace("\n", "\\n");
                                insertStatementToRecord.append("'");
                                insertStatementToRecord.append(tmp);
                                insertStatementToRecord.append("'");
                            } else {
                                insertStatementToRecord.append("NULL");
                            }
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;

                        default:
                            throw new RuntimeException("unhandled column type:" + colType);
                    }
                }
                insertStatementToRecord.append(");\n");
                fos.write(insertStatementToRecord.toString().getBytes());
            }
            tdataList.close();
        }
        tablesList.close();
        showTablesStmt.close();
        c.close();
        System.out.println(". Done");

    }

    public static void main(String[] args) throws Exception {
        dump("jdbc:mysql://localhost/ssg?failOverReadOnly=false&autoReconnect=false&socketTimeout=120000&useNewIO=true&characterEncoding=UTF8&characterSetResults=UTF8",
             "gateway", "7layer", false, "/home/flascell/tmp/dumped.sql");
    }
}
