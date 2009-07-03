package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.HexUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;

import java.io.*;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;
import java.net.NetworkInterface;
import java.net.InetAddress;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 */
class DBDumpUtil {

    private static final Logger logger = Logger.getLogger(DBDumpUtil.class.getName());
    public static final String AUDIT_BACKUP_FILENAME = "audit_backup.sql";   //todo [donal] DBDumpUtil should not have this knowledge
    public static final String LICENCEORIGINALID = "originallicenseobjectid.txt"; //todo [donal] DBDumpUtil should not have this knowledge
    //private static final String[] TABLE_NEVER_EXPORT = {"cluster_info", "message_id", "service_metrics", "service_metrics_details", "service_usage"};

    //configuration files
    private static final String AUDIT_TABLES_CONFIG = "config/backup/cfg/backup_tables_audit"; //this knowledge is ok as its ssg install specific

    /**
     * outputs database dump files. The outputted dump file will never contain audits.
     * An audit table has the prefix 'audit'
     *
     * Note: This will create drop statements for EVERY SINGLE DATABASE TABLE, even if it's not backing it up
     * @param config          database configuration
     * @param outputDirectory the directory path where the dump files should go to
     * @param stdout          stream for verbose output; <code>null</code> for no verbose output
     * @param verbose if true, then verbose output will be written to stdout, if it's not null
     * @throws java.sql.SQLException problem getting data out of db
     * @throws java.io.IOException   problem with dump files
     * @throws UnsupportedOperationException if the database is remote
     */
    public static void dump(final DatabaseConfig config,
                            final String outputDirectory,
                            final PrintStream stdout,
                            final boolean verbose,
                            final String backupFileName) throws SQLException, IOException {
        final NetworkInterface networkInterface =
                NetworkInterface.getByInetAddress( InetAddress.getByName(config.getHost()) );
        if ( networkInterface == null ) {
            throw new UnsupportedOperationException("Backup of a remote database is not supported");
        }

        final DBActions dba = new DBActions();
        final Connection conn = dba.getConnection(config, false);
        final DatabaseMetaData metadata = conn.getMetaData();

        final String[] tableTypes = {
                "TABLE"
        };
        ResultSet tableNames = null;
        final File dbBackupFile = new File(outputDirectory, backupFileName);
        if(dbBackupFile.exists()){
            dbBackupFile.delete();//is this needed?
        }
        dbBackupFile.createNewFile();
        final FileOutputStream mainOutput = new FileOutputStream(dbBackupFile);

        Functions.BinaryThrows<Boolean, String, ResultSet, Exception> checkForClusterPropTable =
                new Functions.BinaryThrows<Boolean, String, ResultSet, Exception>(){
            public Boolean call(final String tableName, final ResultSet resultSet) throws Exception {
                if (tableName.equals("cluster_properties")) { // dont include license in image
                    final String tmp = resultSet.getString(3);
                    if (tmp != null && tmp.equals("license")) {
                        // we dont include the license, however, we will record the object id
                        // in order to be able to put back same id when we restore the original
                        // target license. this will avoid clashing object id
                        final long licenseobjectid = resultSet.getLong(1);
                        final File parentFile = (new File(outputDirectory)).getParentFile();
                        final FileOutputStream licenseFos = new FileOutputStream(new File(parentFile, LICENCEORIGINALID));
                        licenseFos.write(Long.toString(licenseobjectid).getBytes());
                        licenseFos.close();
                        return true;
                    }
                    return false;
                }else{
                    return false;
                }
            }
        };

        try{
            tableNames = metadata.getTables(null, "%", "%", tableTypes);
            mainOutput.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());

            if (stdout != null && verbose) stdout.print("Dumping database to " + outputDirectory + " ..");
            while (tableNames.next()) {
                final String tableName = tableNames.getString("TABLE_NAME");

                // write drop and recreate table statement
                //Note this write drop and create statements for every table it finds
                writeDropCreateTableStmt(conn, mainOutput, tableName);

                if(tableName.startsWith("audit")){
                    continue;
                }

                backUpTable(tableName, conn, mainOutput, checkForClusterPropTable);
            }
            mainOutput.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
            if (stdout != null && verbose) stdout.println(". Done");
        }finally{
            ResourceUtils.closeQuietly(tableNames);
            ResourceUtils.closeQuietly(conn);
            ResourceUtils.closeQuietly(mainOutput);
        }
    }

    /**
     * Backs ups up the audit tables into the audit folder in the outputDirectory in the file audits.gz.
     *
     * As audit data can be large, the data is written from memory directly to the gzipped file using a GZIPOutputStream
     * The sql created does not contain drop and create statements as this happens in dump().
     * The sql created by this method will only be useful when the sql created from dump() is exected on the target
     * host first, so that the audit tables exist and that they are empty
     *
     * Note: The file config/backup/cfg/backup_tables_audit is read to determine what audit tables are to be backed up
     * In 5.0 this contained the tables message_context_mapping_*. These tables are now backed up by default, but
     * as this code is also used to backup existing 5.0 systems, any table found which does not begin with 'audit_'
     * is ignored
     * @param ssgHome The installation directory of the SSG, so that the backup_tables_audit file can be located
     * @param config DatabaseConfig to use for connecting to database
     * @param outputDirectory The directory to write the audits.gz file to
     * @param verbose if true, then verbose output will be written to stdout, if it's not null 
     * @param stdout PrintStream to write verbose info messages to. Ok to be null
     * @throws SQLException if any database exceptions occur
     * @throws IOException if any exceptions occur writing to audits.gz
     */
    public static void auditDump(final File ssgHome,
                                 final DatabaseConfig config,
                                 final String outputDirectory,
                                 final PrintStream stdout,
                                 final boolean verbose) throws SQLException, IOException {
        final NetworkInterface networkInterface =
                NetworkInterface.getByInetAddress( InetAddress.getByName(config.getHost()) );
        if ( networkInterface == null ) {
            throw new UnsupportedOperationException("Audit backup of a remote database is not supported");
        }

        //read all configuration file data in to arrays
        final String[] auditTables = parseConfigFile(ssgHome.getAbsolutePath() + File.separator+ AUDIT_TABLES_CONFIG);

        Connection conn = null;
        ResultSet tableNames = null;
        final OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputDirectory + File.separator + BackupImage.AUDIT_BACKUP_FILENAME));
        //default buffer size is 512
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

        try{
            final DBActions dba = new DBActions();
            conn = dba.getConnection(config, false);

            //todo [Donal] audit heuristic
//            final String sizeSql = "show table status like 'audit%'";
//            final Statement stmt = conn.createStatement();
//            final ResultSet rs = stmt.executeQuery(sizeSql);
//            int totalSize = 0;
//            while(rs.next()){
//                final String tableName = rs.getString("Name");
//                final int dataLength = rs.getInt("Data_Length");
//                final int indexLength = rs.getInt("Index_length");
//                System.out.println(tableName+" length: " + dataLength+" index length is: " + indexLength);
//                totalSize += (dataLength + indexLength);
//            }
//
//            System.out.println("Size of audits is in bytes is: " + totalSize);
//            System.out.println("Size of audits is: " + totalSize / 1024+" kb");


            gzipOutputStream.write("SET FOREIGN_KEY_CHECKS = 0;\n".getBytes());

            if (stdout != null && verbose) stdout.print("Dumping database audit tables to " + outputDirectory + " ..");
            for(final String tableName: auditTables){

                if(!tableName.startsWith("audit")){
                    //this is ok as a 5.0 system will have the message_context_mapping_* tables in the audit file
                    //so just ignore the table and log it
                    logger.log(Level.WARNING, "Table '"+tableName+"' ignored as it is not an audit table");
                    continue;
                }

                backUpTable(tableName, conn, gzipOutputStream, null);
            }
            gzipOutputStream.write("SET FOREIGN_KEY_CHECKS = 1;\n".getBytes());
            gzipOutputStream.close();
            if (stdout != null && verbose) stdout.println(". Done");
        }finally{
            ResourceUtils.closeQuietly(tableNames);
            ResourceUtils.closeQuietly(conn);
        }
    }

    private static void writeDropCreateTableStmt(final Connection conn, final FileOutputStream mainOutput,
                                                 final String tableName) throws IOException, SQLException {
        mainOutput.write(("DROP TABLE IF EXISTS " + tableName + ";\n").getBytes());
        Statement getCreateTablesStmt = null;
        try{
            getCreateTablesStmt = conn.createStatement();
            final ResultSet createTables = getCreateTablesStmt.executeQuery("show create table " + tableName);
            while (createTables.next()) {
                String s = createTables.getString(2);
                s = s.replace("\r", " ");
                s = s.replace("\n", " ");
                s = s.replace("`", "");
                mainOutput.write((s + ";\n").getBytes());
            }
        }finally{
            ResourceUtils.closeQuietly(getCreateTablesStmt);
        }
    }

    /**
     *
     * @param tableName The name of the table to backup
     * @param conn Active database connection
     * @param mainOutput The OutputStream to write the backup data to
     * @param rowFilter Can be null. If not null the call method will be executed for every row of data found in the
     * table tableName. If the result is true then the current row is not included in the backup. If the result is
     * false, then the row will be included
     * @throws SQLException Any database exception
     * @throws IOException Any exception writing to the backup output stream
     */
    private static void backUpTable(final String tableName, final Connection conn, final OutputStream mainOutput,
                                    final Functions.BinaryThrows<Boolean, String, ResultSet, Exception> rowFilter)
            throws SQLException, IOException {
        final Statement tdata = conn.createStatement();
        ResultSet resultSet = null;

        try{
            resultSet = tdata.executeQuery("select * from " + tableName);
            while (resultSet.next()) {
                if(rowFilter != null){
                    boolean isRowExcluded;
                    try {
                        isRowExcluded = rowFilter.call(tableName, resultSet);
                    } catch (Exception e) {
                        if(ExceptionUtils.causedBy(e, IOException.class)){
                            throw new IOException(e);
                        }else if(ExceptionUtils.causedBy(e, SQLException.class)){
                            throw new SQLException(e);
                        }else{
                            //log strange behaviour
                            throw new IllegalStateException("Unexpected exception occured", e);
                        }
                    }
                    if(isRowExcluded) continue;
                }
                final StringBuilder insertStatementToRecord = getInsertStmtForRow(tableName, resultSet);
                mainOutput.write(insertStatementToRecord.toString().getBytes());
            }
        }finally{
            ResourceUtils.closeQuietly(resultSet);
        }
    }

    private static StringBuilder getInsertStmtForRow(final String tableName, final ResultSet resultSet)
            throws SQLException {
        final StringBuilder insertStatementToRecord = new StringBuilder("INSERT INTO " + tableName + " VALUES (");
        final ResultSetMetaData rowInfo = resultSet.getMetaData();
        for (int i = 1; i <= rowInfo.getColumnCount(); i++) {
            final int colType = rowInfo.getColumnType(i);
            switch (colType) {
                case Types.BIGINT: {
                    final long value = resultSet.getLong(i);
                    insertStatementToRecord.append(resultSet.wasNull() ? "NULL" : value);
                    if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                    break;
                }
                case Types.INTEGER:
                case Types.BIT:
                case Types.TINYINT: {
                    final int value = resultSet.getInt(i);
                    insertStatementToRecord.append(resultSet.wasNull() ? "NULL" : value);
                    if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                    break;
                }
                case Types.DOUBLE: {
                    final double value = resultSet.getDouble(i);
                    insertStatementToRecord.append(resultSet.wasNull() ? "NULL" : value);
                    if (i < rowInfo.getColumnCount()) insertStatementToRecord.append(", ");
                    break;
                }
                case Types.VARCHAR:
                case Types.CHAR:
                case Types.LONGVARCHAR: // medium text
                    String tmp = resultSet.getString(i);
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
                    final byte[] tmpBytes = resultSet.getBytes(i);
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
        return insertStatementToRecord;
    }

    private static String escapeForSQLInsert(String in) {
        String output = in.replace("\"", "\\\"");
        output = output.replace("\'", "\\\'");
        output = output.replace("\n", "\\n");
        output = output.replace("\r", "\\r");
        return output;
    }

    private static String[] parseConfigFile(final String filename) throws IOException {
        final ArrayList<String> parsedElements = new ArrayList<String>();
        final File configFile = new File(filename);
        if (configFile.isFile()) {
            final FileReader fr = new FileReader(configFile);
            final BufferedReader br = new BufferedReader(fr);
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.startsWith("#")) {//ignore comments
                    final String tableName = line.trim();
                    if (!parsedElements.contains(tableName)) {//no duplicates
                        parsedElements.add(tableName);
                    }
                }
            }
        }
        return parsedElements.toArray(new String[parsedElements.size()]);
    }
}