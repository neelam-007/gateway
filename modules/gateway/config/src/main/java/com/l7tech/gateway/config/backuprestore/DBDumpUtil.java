package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;

import java.io.*;
import java.sql.*;
import java.util.logging.Logger;
import java.util.ArrayList;

/**
 * Methods for dumping
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
class DBDumpUtil {

    private static final Logger logger = Logger.getLogger(DBDumpUtil.class.getName());
    //public static final String DBDUMPFILENAME_CLONE = "dbdump_restore.sql";
    public static final String MAIN_BACKUP_FILENAME = "main_backup.sql";
    public static final String AUDIT_BACKUP_FILENAME = "audit_backup.sql";
    public static final String LICENCEORIGINALID = "originallicenseobjectid.txt";
    //private static final String[] TABLE_NEVER_EXPORT = {"cluster_info", "message_id", "service_metrics", "service_metrics_details", "service_usage"};

    //configuration files
    private static final String AUDIT_TABLES_CONFIG = "./cfg/backup_tables_audit";

    /**
     * outputs database dump files
     *
     * @param config          database configuration
     * @param includeAudit    whether or not audit tables should be included
     * @param mappingEnabled  whether or not mapping option "-it" was used
     * @param outputDirectory the directory path where the dump files should go to
     * @param stdout          stream for verbose output; <code>null</code> for no verbose output
     * @throws java.sql.SQLException problem getting data out of db
     * @throws java.io.IOException   problem with dump files
     */
    public static void dump(final DatabaseConfig config,
                            final boolean includeAudit,
                            final boolean mappingEnabled,
                            final String outputDirectory,
                            final PrintStream stdout) throws SQLException, IOException {
        DBActions dba = new DBActions();
        Connection c = dba.getConnection(config, false);
        DatabaseMetaData metadata = c.getMetaData();

        //read all configuration file data in to arrays
        String[] auditTables = parseConfigFile(AUDIT_TABLES_CONFIG);

        String[] tableTypes = {
                "TABLE"
        };
        ResultSet tableNames = metadata.getTables(null, "%", "%", tableTypes);

        FileOutputStream mainOutput = new FileOutputStream(outputDirectory + File.separator + MAIN_BACKUP_FILENAME);
        mainOutput.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());

        if (stdout != null) stdout.print("Dumping database to " + outputDirectory + " ..");
        while (tableNames.next()) {
            String tableName = tableNames.getString("TABLE_NAME");

            // drop and recreate table
            mainOutput.write(("DROP TABLE IF EXISTS " + tableName + ";\n").getBytes());
            Statement getCreateTablesStmt = c.createStatement();
            ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
            while (createTables.next()) {
                String s = createTables.getString(2);
                s = s.replace("\r", " ");
                s = s.replace("\n", " ");
                s = s.replace("`", "");
                mainOutput.write((s + ";\n").getBytes());
            }

            if (!includeAudit) {
                if (tableInList(tableName, auditTables)/*tableName.startsWith("audit_")*/) {
                    continue;
                }
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
                        case Types.BIGINT: {
                            final long value = tdataList.getLong(i);
                            insertStatementToRecord.append(tdataList.wasNull() ? "NULL" : value);
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        }
                        case Types.INTEGER:
                        case Types.BIT:
                        case Types.TINYINT: {
                            final int value = tdataList.getInt(i);
                            insertStatementToRecord.append(tdataList.wasNull() ? "NULL" : value);
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        }
                        case Types.DOUBLE: {
                            final double value = tdataList.getDouble(i);
                            insertStatementToRecord.append(tdataList.wasNull() ? "NULL" : value);
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        }
                        case Types.VARCHAR:
                        case Types.CHAR:
                        case Types.LONGVARCHAR: // medium text
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
                        case Types.LONGVARBINARY: // medium blob
                            final byte[] tmpBytes = tdataList.getBytes(i);
                            if (tmpBytes != null) {
                                insertStatementToRecord.append("0x");
                                insertStatementToRecord.append(HexUtils.hexDump(tmpBytes));
                            } else {
                                insertStatementToRecord.append("NULL");
                            }
                            if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                            break;
                        default:
                            logger.severe("unexpected java.sql.Type value " + colType);
                            throw new RuntimeException("unhandled column type: " + colType);
                    }
                }
                insertStatementToRecord.append(");\n");
                mainOutput.write(insertStatementToRecord.toString().getBytes());
            }
            tdataList.close();
        }
        c.close();
        mainOutput.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
        ResourceUtils.closeQuietly(mainOutput);

        if (stdout != null) stdout.println(". Done");
    }

    private static String escapeForSQLInsert(String in) {
        String output = in.replace("\"", "\\\"");
        output = output.replace("\'", "\\\'");
        output = output.replace("\n", "\\n");
        output = output.replace("\r", "\\r");
        return output;
    }

    private static boolean tableInList(String tableName, String[] tableList) {
        for (String s : tableList) {
            if (s.equals(tableName)) return true;
        }
        return false;
    }

    private static String[] parseConfigFile(String filename) throws IOException {
        ArrayList<String> parsedElements = new ArrayList<String>();
        File configFile = new File(filename);
        if (configFile.isFile()) {
            FileReader fr = new FileReader(configFile);
            BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {//ignore comments
                    String tableName = line.trim();
                    if (!parsedElements.contains(tableName)) {//no duplicates
                        parsedElements.add(tableName);
                    }
                }
            }
        }
        return parsedElements.toArray(new String[parsedElements.size()]);
    }
}
