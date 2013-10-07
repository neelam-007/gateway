package com.l7tech.gateway.config.backuprestore;

import com.l7tech.objectmodel.Goid;
import com.l7tech.util.*;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;

import java.io.*;
import java.sql.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.GZIPOutputStream;
import java.util.List;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 */
class DBDumpUtil {

    private static final Logger logger = Logger.getLogger(DBDumpUtil.class.getName());

    /**
     * Downloads all database info, apart from audits, to the file backupFileName
     * An audit table has the prefix 'audit' and will never be included in this file
     *
     * The caller should decide if a database backup is applicable for the host in dbConfig before calling this
     * method. This method does not care whether the database is local or remote
     *
     * Note: This will create drop statements for EVERY SINGLE DATABASE TABLE, even if it's not backing it up
     * @param dbConfig          database configuration
     * @param outputDirectory the directory path where the dump files should go to
     * @param backupFileName the name of the database backup file, will be created in outputDirectory
     * @param licenseStorageFileName the name of the file to store the license in, if it's found. Cannot be null
     * @param verbose if true, then verbose output will be written to stdout, if it's not null
     * @param stdout          stream for verbose output; <code>null</code> for no verbose output
     * @throws java.sql.SQLException problem getting data out of db
     * @throws java.io.IOException   problem with dump files
     */
    public static void dump(final DatabaseConfig dbConfig,
                            final String outputDirectory,
                            final String backupFileName,
                            final String licenseStorageFileName,
                            final boolean verbose,
                            final PrintStream stdout
    ) throws SQLException, IOException {
        if(dbConfig == null) throw new NullPointerException("dbConfig cannot be null");
        if(outputDirectory == null || outputDirectory.trim().isEmpty())
            throw new IllegalArgumentException("outputDirectory cannot be null and must not be the empty string");
        if(backupFileName == null || backupFileName.trim().isEmpty())
            throw new IllegalArgumentException("backupFileName cannot be null and must not be the empty string");
        if(licenseStorageFileName == null || licenseStorageFileName.trim().isEmpty())
            throw new IllegalArgumentException("licenseStorageFileName cannot be null and must not be the empty string");

        final DBActions dba = new DBActions();
        final Connection conn = dba.getConnection(dbConfig, false);
        final DatabaseMetaData metadata = conn.getMetaData();

        final String[] tableTypes = {
                "TABLE"
        };
        ResultSet tableNames = null;
        final File dbBackupFile = new File(outputDirectory, backupFileName);
        FileUtils.delete(dbBackupFile);//delete if it exists
        final boolean fileCreated = dbBackupFile.createNewFile();
        if(!fileCreated) throw new IOException("File already exists " + dbBackupFile.getAbsolutePath());
        
        final FileOutputStream mainOutput = new FileOutputStream(dbBackupFile);

        Functions.BinaryThrows<Boolean, String, ResultSet, Exception> checkForClusterPropTable =
                new Functions.BinaryThrows<Boolean, String, ResultSet, Exception>(){
            @Override
            public Boolean call(final String tableName, final ResultSet resultSet) throws Exception {
                if (tableName.equals("cluster_properties")) { // dont include license in image
                    final String tmp = resultSet.getString(3);
                    if (tmp != null && tmp.equals("license")) {
                        // we dont include the license, however, we will record the object id
                        // in order to be able to put back same id when we restore the original
                        // target license. this will avoid clashing object id
                        final Goid licenseobjectid = new Goid(resultSet.getBytes(1));
                        final File parentFile = (new File(outputDirectory)).getParentFile();
                        final FileOutputStream licenseFos =
                                new FileOutputStream(new File(parentFile, licenseStorageFileName));
                        licenseFos.write(licenseobjectid.getBytes());
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

            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    "\tDumping database to " + outputDirectory + " ..", verbose, stdout, false);
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
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, ". Done\n", verbose, stdout, false);
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
     * @param dbConfig DatabaseConfig to use for connecting to database. Cannot be null
     * @param outputDirectory The directory to write the audits.gz file to. Cannot be null or the emtpy string
     * @param auditTables the list of audit tables to backup. Cannot be null or empty
     * @param verbose if true, then verbose output will be written to stdout, if it's not null 
     * @param stdout PrintStream to write verbose info messages to. Ok to be null
     * @throws SQLException if any database exceptions occur
     * @throws IOException if any exceptions occur writing to audits.gz
     */
    public static void auditDump(final DatabaseConfig dbConfig,
                                 final String outputDirectory,
                                 final List<String> auditTables,
                                 final PrintStream stdout,
                                 final boolean verbose) throws SQLException, IOException {
        if(dbConfig == null) throw new NullPointerException("dbConfig cannot be null");
        if(outputDirectory == null || outputDirectory.trim().isEmpty())
            throw new IllegalArgumentException("outputDirectory cannot be null and must not be the empty string");

        if(auditTables == null) throw new NullPointerException("auditTables cannot be null");
        if(auditTables.isEmpty())
            throw new IllegalArgumentException("auditTables does not list any audit tables to backup");

        Connection conn = null;
        ResultSet tableNames = null;
        final OutputStream outputStream = new BufferedOutputStream(
                new FileOutputStream(outputDirectory + File.separator + BackupImage.AUDIT_BACKUP_FILENAME));
        //default buffer size is 512
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);

        try{
            final DBActions dba = new DBActions();
            conn = dba.getConnection(dbConfig, false);

            //audit heuristic - see http://sarek/bugzilla/show_bug.cgi?id=7476
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

            final String msg = "\tDumping database audit tables to " + outputDirectory + " ..";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, verbose, stdout, false);
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
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, ". Done\n", verbose, stdout, false);
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
                // The below replacement is needed because mysql will return a default byte value as the actual byte string.
                // In the create table statement we need it to be a hex string. This is only needed in the wsdm_subscription table.
                s = s.replace("DEFAULT '\\0\\0\\0\\0\\0\\0\\0\\0��������'", "DEFAULT X'0000000000000000FFFFFFFFFFFFFFFF'");
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
        //set explicity parameters on the statement, although these are the default values
        final Statement stmt = conn.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY,
                      java.sql.ResultSet.CONCUR_READ_ONLY);
        //force the result set to stream
        //if you print the fetch size with and without this statement you will get 0, but this actually has
        //the side affect of causing the result set to stream
        //see http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
        //go to the section ResultSet
        stmt.setFetchSize(Integer.MIN_VALUE);

        ResultSet resultSet = null;

        try{
            resultSet = stmt.executeQuery("select * from " + tableName);
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
                            //something unexpected happened
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
                case Types.VARBINARY:
                case Types.BINARY: //GOID
                    final byte[] tmpBytes = resultSet.getBytes(i);
                    if (tmpBytes == null) {
                        insertStatementToRecord.append("NULL");
                    } else if ( tmpBytes.length == 0 ) {
                        insertStatementToRecord.append("x''");
                    } else {
                        insertStatementToRecord.append("0x");
                        insertStatementToRecord.append(HexUtils.hexDump(tmpBytes));
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
        String output = in.replace("\\", "\\\\");
        output = output.replace("\"", "\\\"");
        output = output.replace("\'", "\\\'");
        output = output.replace("\n", "\\n");
        output = output.replace("\r", "\\r");
        return output;
    }

}