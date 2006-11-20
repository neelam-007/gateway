package com.l7tech.server.flasher;

import com.l7tech.server.config.db.DBActions;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
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
    public static final String DBDUMPFILENAME_STAGING = "dbdump_staging.sql";
    public static final String DBDUMPFILENAME_CLONE = "dbdump_clone.sql";
    public static final String LICENCEORIGINALID = "originallicenseobjectid.txt";
    private static final String[] TABLE_NOT_IN_STAGING = {"client_cert"};
    private static final String[] TABLE_NEVER_EXPORT = {"cluster_info", "service_usage", "message_id", "service_metrics"};


    /**
     * outputs database dump files
     * @param databaseURL database host
     * @param databaseUser database user
     * @param databasePasswd database password
     * @param includeAudit whether or not audit tables should be included
     * @param outputDirectory the directory path where the dump files should go to
     */
    public static void dump(String databaseURL, String databaseUser, String databasePasswd,
                            boolean includeAudit, String outputDirectory) throws SQLException, IOException, ClassNotFoundException {
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
        FileOutputStream cloneoutput = new FileOutputStream(outputDirectory + File.separator + DBDUMPFILENAME_CLONE);
        FileOutputStream stageoutput = new FileOutputStream(outputDirectory + File.separator + DBDUMPFILENAME_STAGING);
        System.out.print("Dumping database to " + outputDirectory + " ..");
        cloneoutput.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());
        stageoutput.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());
        while (tableNames.next()) {
            String tableName = tableNames.getString("TABLE_NAME");
            // drop and recreate table
            cloneoutput.write(("DROP TABLE IF EXISTS " + tableName + ";\n").getBytes());
            stageoutput.write(("DROP TABLE IF EXISTS " + tableName + ";\n").getBytes());
            Statement getCreateTablesStmt = c.createStatement();
            ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
            while (createTables.next()) {
                String s = createTables.getString(2);
                s = s.replace("\r", " ");
                s = s.replace("\n", " ");
                s = s.replace("`", "");
                cloneoutput.write((s + ";\n").getBytes());
                stageoutput.write((s + ";\n").getBytes());
            }
            if (tableInList(tableName, TABLE_NEVER_EXPORT)) continue;
            if (!includeAudit) {
                if (tableName.startsWith("audit_")) continue;
            }
            Statement tdata = c.createStatement();
            ResultSet tdataList = tdata.executeQuery("select * from " + tableName);
            while (tdataList.next()) {
                if (tableName.equals("cluster_properties")) { // dont include license in image
                    String tmp = tdataList.getString(3);
                    if (tmp != null && tmp.equals("license")) {
                        // we dont include the license, however, we will record the object id
                        // in order to be able to put back same id when we restore the original
                        // target license. this will avoid clashing object id
                        long licenseobjectid = tdataList.getLong(1);
                        FileOutputStream licenseFos = new FileOutputStream(outputDirectory + File.separator + LICENCEORIGINALID);
                        licenseFos.write(Long.toString(licenseobjectid).getBytes());
                        licenseFos.close();
                        continue;
                    }
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
                                tmp = escapeForSQLInsert(tmp);
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
                cloneoutput.write(insertStatementToRecord.toString().getBytes());
                if (!tableInList(tableName, TABLE_NOT_IN_STAGING)) {
                    stageoutput.write(insertStatementToRecord.toString().getBytes());
                }
            }
            tdataList.close();
        }
        c.close();
        cloneoutput.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
        stageoutput.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
        cloneoutput.close();
        stageoutput.close();
        System.out.println(". Done");
    }

    private static String escapeForSQLInsert(String in) {
        String output = in.replace("\"", "\\\"");
        output = output.replace("\'", "\\\'");
        output = output.replace("\n", "\\n");
        return output;
    }

    private static boolean tableInList(String tableName, String[] tableList) {
        for (String s : tableList) {
            if (s.equals(tableName)) return true;
        }
        return false;
    }

    private static DBActions getDBActions() throws ClassNotFoundException {
        if (dbActions == null)
            dbActions = new DBActions();
        return dbActions;
    }
}
