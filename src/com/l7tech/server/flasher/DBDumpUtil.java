package com.l7tech.server.flasher;

import com.l7tech.server.config.db.DBActions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;

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
    private static DBActions dbActions;

    /**
     * outputs a database dump
     * @param databaseURL database host
     * @param databaseUser database user
     * @param databasePasswd database password
     * @param includeAudit whether or not audit tables should be included
     * @param outputPath the path where the dump should go to
     */
    public static void dump(String databaseURL, String databaseUser, String databasePasswd,
                            boolean includeAudit, String outputPath) throws SQLException, IOException, ClassNotFoundException {
        Connection c = getDBActions().getConnection(databaseURL, databaseUser, databasePasswd);
        if (c == null) {
            throw new SQLException("could not connect using url: " + databaseURL +
                                   ". with username " + databaseUser +
                                   ", and password: " + databasePasswd);
        }
        DatabaseMetaData metadata = c.getMetaData();
        String[] tableTypes = {
                "TABLE"
        };
        ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);
        FileOutputStream fos = new FileOutputStream(outputPath);
        // todo, produce parrallel dump for staging only which would include only staging content
        System.out.print("Dumping database to " + outputPath + " ..");
        fos.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());
        while (tableNames.next()) {
            String tableName = tableNames.getString("TABLE_NAME");
            // drop and recreate table
            fos.write(("DROP TABLE IF EXISTS \'" + tableName + "\';\n").getBytes());
            Statement getCreateTablesStmt = c.createStatement();
            ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
            while (createTables.next()) {
                String s = createTables.getString(2);
                fos.write((s + ";\n").getBytes());
            }
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
                            // todo, all necessary encoding
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
        c.close();
        fos.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
        fos.close();
        System.out.println(". Done");
    }

    private static DBActions getDBActions() throws ClassNotFoundException {
        if (dbActions == null)
            dbActions = new DBActions();
        return dbActions;
    }
}
