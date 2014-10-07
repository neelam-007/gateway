/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 6:58:37 PM
 */
package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.config.manager.ClusterPassphraseManager;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang.StringUtils;

import java.io.*;
import java.sql.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 *
 * Responsible for all restore database interactions. Understands migrate's requirements
 * A migrate is a restore with the ability to filter which tables are restored. When a migrate happens, the
 * caller should be aware if the cluster_properties table was recreated, this affects the license. See callers
 * of restoreDb()
 *
 * This class is immutable
 */
class DatabaseRestorer {
    private static final Logger logger = Logger.getLogger(DatabaseRestorer.class.getName());
    
    private final DatabaseConfig dbConfig;
    private final boolean verbose;
    private final PrintStream printStream;
    private final String clusterPassphrase;
    private final File ssgHome;
    private final DBActions dbAction = new DBActions();
    private final BackupImage image;
    private static final String EXCLUDE_TABLES_PATH = "config/backup/cfg/exclude_tables";

    /**
     *
     * @param ssgHome
     * @param clusterPassphrase should not be encrypted
     * @param dbConfig
     * @param image
     * @param printStream
     * @param verbose
     */
    DatabaseRestorer(final File ssgHome,
                    final String clusterPassphrase,
                    final DatabaseConfig dbConfig,
                    final BackupImage image,
                    final PrintStream printStream,
                    final boolean verbose) {
        if(ssgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!ssgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!ssgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");

        if(clusterPassphrase == null) throw new NullPointerException("clusterPassphrase cannot be null");
        if(clusterPassphrase.trim().isEmpty()) throw new IllegalArgumentException("clusterPassphrase cannot be empty");
        if(dbConfig == null) throw new NullPointerException("dbConfig cannot be null");

        if(image == null) throw new NullPointerException("image cannot be null");

        this.ssgHome = ssgHome;
        this.clusterPassphrase = clusterPassphrase;
        this.dbConfig = dbConfig;
        this.printStream = printStream;
        this.verbose = verbose;
        this.image = image;
    }

    boolean doesDatabaseExist(){
        final DBActions.DBActionsResult result = dbAction.checkExistingDb(dbConfig);
        if(result.getStatus() != DBActions.StatusType.SUCCESS){
            String msg = "Database: '" + dbConfig.getName()+"' does not exist";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, verbose, printStream);
        }
        return result.getStatus() == DBActions.StatusType.SUCCESS;
    }

    /**
     * Create a new db or throw an exception
     * @param overwriteDb if true, this will overwrite the database if it exists. if false and the db exists an
     * exception will be thrown
     * @return true if the db was created, never false
     * @throws RestoreImpl.RestoreException
     */
    boolean createNewDatabaseOrThrow(boolean overwriteDb) throws RestoreImpl.RestoreException {
        final DBActions.DBActionsResult res;
        try {
            res = dbAction.createDb(dbConfig,
                    null,
                    new File(ssgHome, ImportExportUtilities.SSG_DB_SCHEMA_FOLDER).getAbsolutePath(),
                    overwriteDb);
            switch(res.getStatus()){
                case ALREADY_EXISTS:
                    throw new RestoreImpl.RestoreException("Database '" + dbConfig.getName()+"' already exists");
                case SUCCESS:
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                            "Created new database", verbose, printStream);
                    break;
                default:
                    throw new RestoreImpl.RestoreException("Cannot create database: " +
                            ((res.getErrorMessage() != null) ? res.getErrorMessage() : ""));    
            }
        } catch (IOException e) {
            throw new RestoreImpl.RestoreException("Could not read ssg.xml: " + e.getMessage() );
        }
        return true;
    }

    DatabaseConfig getDbConfig(){
        return new DatabaseConfig(dbConfig);
    }

    /**
     * The retuned cluster passphrase is never encrypted
     * @return
     */
    String getClusterPassphrase(){
        return clusterPassphrase;        
    }

    /**
     * Only restore the audit data. Caller should know that audit data exists to be restored. If there is none,
     * an exception will be thrown
     * The database must exist when this method is called. If the database exists, then the audit tables are empty
     */
    void restoreAudits() throws DatabaseRestorerException, IOException, SQLException {
        final File auditFile = image.getAuditsBackupFile();

        if(!auditFile.exists()) throw new IllegalStateException("File '"+auditFile.getAbsolutePath()+" does not exist");
        if(auditFile.isDirectory())
            throw new IllegalStateException("File '"+auditFile.getAbsolutePath()+" is not a regular file");

        final BufferedReader mainBackupReader;
        final InputStreamReader inReader;
        if(image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O){
            //we need to deal with a gzipped file
            final InputStream auditInputStream = new BufferedInputStream(new FileInputStream(auditFile));
            final InputStream zippedStream = new GZIPInputStream(auditInputStream);
            inReader = new InputStreamReader(zippedStream);
            mainBackupReader = new BufferedReader(inReader);
        }else{
            inReader = new FileReader(auditFile);
            mainBackupReader = new BufferedReader(inReader);
        }

        final DBActions dba = new DBActions();
        final Connection c = dba.getConnection(dbConfig, true, false);
        String tmp;
        try {
            c.setAutoCommit(false);
            final Statement stmt = c.createStatement();
            try {
                while ((tmp = mainBackupReader.readLine()) != null) {
                    if (tmp.endsWith(";")) {
                        if(image.getImageVersion() == BackupImage.ImageVersion.FIVE_O){
                            //make sure it's an audit statement, as the main_backup.sql contains everything
                            if(!doesAffectsTables(tmp, Arrays.asList("audit")) && !tmp.startsWith("SET")) continue;
                        }
                        stmt.executeUpdate(fixSql(tmp.substring(0, tmp.length() - 1)));
                    }else {
                        throw new SQLException("unexpected statement " + tmp);
                    }
                }

                // commit at the end if everything updated correctly
                final String msg1 = "Database audits loaded succesfully, committing...";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, verbose, printStream, false);
                c.commit();
                //don't want a tab
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, " Done", verbose, printStream);
            } catch (SQLException e) {
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                        "Error loading database audits. Rolling back now. " + e.getMessage(), verbose, printStream);
                c.rollback();
                throw e;
            } catch (IOException e) {
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                        "Error loading database audits. Rolling back now. " + e.getMessage(), verbose, printStream);
                c.rollback();
                throw e;
            } finally {
                stmt.close();
                c.setAutoCommit(true);
            }
        } finally {
            inReader.close();
            mainBackupReader.close();
        }
    }

    public static class DatabaseRestorerException extends Exception{
        public DatabaseRestorerException(String message) {
            super(message);
        }
    }
    /**
     *
     * @param isNewDatabase
     * @param isMigrate
     * @return true if the cluster_properties table was updated. This lets us know if an existing license may
     * have been deleted
     * @throws IOException
     * @throws SQLException
     * @throws DatabaseRestorerException
     */
    boolean restoreDb(final boolean isNewDatabase,
                          final boolean isMigrate)
            throws IOException, SQLException, DatabaseRestorerException {

        // create temporary database copy to test the import
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                "\tCreating copy of target database for testing import...", verbose, printStream, false);
        final String testdbname = "TstDB_" + System.currentTimeMillis();

        final DatabaseConfig targetConfig = new DatabaseConfig(dbConfig);
        targetConfig.setName(testdbname);

        final DBActions dba = new DBActions();
        dba.copyDatabase(dbConfig, targetConfig, true, null);
        ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, " Done", verbose, printStream);

        try {
            // load that image on the temp database
            final String msg = "Loading image on temporary database";
            final Connection c = dba.getConnection(targetConfig, true, false);
            try {
                loadMainDbOnly(c, msg, isMigrate);

                //if not a new DB, verify the supplied cluster passphrase is correct
                if (!isNewDatabase) {
                    //Cluster passphrase manager will ask for a non admin connection
                    //the gateway user in our targetConfig will not have access to the temporary database
                    //created, so we will create a new DatabaseConfig and set the node username / password to
                    //that of the admin user
                    final DatabaseConfig adminDbConfig = new DatabaseConfig(targetConfig);
                    adminDbConfig.setNodeUsername(adminDbConfig.getDatabaseAdminUsername());
                    adminDbConfig.setNodePassword(adminDbConfig.getDatabaseAdminPassword());

                    final ClusterPassphraseManager cpm = new ClusterPassphraseManager(adminDbConfig);
                    if (cpm.getDecryptedSharedKey(clusterPassphrase) == null) {
                        throw new DatabaseRestorerException("Incorrect cluster passphrase.");
                    }
                }
            } finally {
                ResourceUtils.closeQuietly( c );
            }
        } finally {
            // delete the temporary database
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    "\tDeleting temporary database...", verbose, printStream, false);
            final Connection c = dba.getConnection(targetConfig, true);
            try {
                final Statement stmt = c.createStatement();
                boolean success = false;
                try {
                    stmt.executeUpdate("drop database " + testdbname + ";");
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO,
                            " Done", verbose, printStream);
                    success = true;
                } finally {
                    //purly for formatting
                    if(!success){
                        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "", verbose, printStream, true);
                    }
                    ResourceUtils.closeQuietly( stmt );
                }
            } finally {
                ResourceUtils.closeQuietly( c );
            }
        }

        // importing on the real target database
        final String msg = "Loading image on target database";
        final Connection c = dba.getConnection(dbConfig, true, false);
        boolean didUpdateClusterProperties = false;
        try {
            didUpdateClusterProperties = loadMainDbOnly(c, msg, isMigrate);
            final String msg1 = "Successfully "+((isMigrate)?"migrated":"restored") 
                    + " to host '"+dbConfig.getHost()+"' database '"+dbConfig.getName()+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    msg1, verbose, printStream);

        } finally {
            ResourceUtils.closeQuietly( c );
        }
        return didUpdateClusterProperties;
    }

    /**
     * Get any existing license
     * @return
     * @throws SQLException
     */
    String getExistingLicense() throws SQLException {
        final Connection c = (new DBActions()).getConnection(dbConfig, false);
        try {
            final Statement selectlicense = c.createStatement();
            final ResultSet maybeLicenseRS =
                    selectlicense.executeQuery("select propvalue from cluster_properties where propkey=\'license\'");
            try {
                while (maybeLicenseRS.next()) {
                    final String maybeLicense = maybeLicenseRS.getString(1);
                    if (StringUtils.isNotEmpty(maybeLicense)) {
                        return maybeLicense;
                    }
                }
            } finally {
                maybeLicenseRS.close();
                selectlicense.close();
            }
        } finally {
            c.close();
        }
        return null;
    }

    /**
     * <p>
     * Restore a license. This only applies when a migrate is ran and an existing database was updated.
     * If this is the case, we need to record the license before the migrate, as the migrate will remove the license,
     * then after the restore we need to reinstatate the license with the oid from the original system.
     * This is fine so long as the cluster_property table was part of the migrate.
     * <b>Therefore this method should not be called if it's known that the cluster_properties table
     * was not part of the migrate</b>
     * </p>
     *
     * @param originalLicense the license from the target system before the migrate. Cannot be null
     * @throws IOException
     * @throws SQLException
     */
    void reloadLicense(final String originalLicense) throws IOException, SQLException {
        if(originalLicense == null) throw new NullPointerException("originalLicense cannot be null");
        if(originalLicense.trim().isEmpty()) throw new NullPointerException("originalLicense cannot be empty");
        
        final File originalLicenseIdFile =
                new File(image.getRootFolder() + File.separator + BackupImage.ORIGINAL_LICENSE_ID_FILENAME);
        if(!originalLicenseIdFile.exists() || !originalLicenseIdFile.isFile())
            throw new IllegalStateException("File '"+originalLicenseIdFile.getAbsolutePath()+"' " +
                    "does not exist or is not a regular file");

        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "\tRestoring license from image..",
                verbose, printStream, false);
        // get the id to use
        final byte[] buf = new byte[128];
        final FileInputStream fis = new FileInputStream(originalLicenseIdFile);
        fis.read(buf);
        fis.close();
        final Goid licenseObjectId = new Goid(buf);
        final Connection c = (new DBActions()).getConnection(dbConfig, false);
        try {
            final PreparedStatement ps =
                    c.prepareStatement("insert into cluster_properties (goid,version,propkey,propvalue,properties) values (?, 1, \'license\', ?, null)");
            ps.setBytes(1, licenseObjectId.getBytes());
            ps.setString(2, originalLicense);
            ps.executeUpdate();
            ps.close();
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Done", verbose, printStream);
        } finally {
            c.close();
        }
    }

    /**
     * This NEVER load audits
     * @param c
     * @param msg
     * @return true if the cluster_properties table was updated. This lets us know if an existing license may
     * have been deleted
     * @throws IOException
     * @throws SQLException
     */
    private boolean loadMainDbOnly(final Connection c, final String msg, final boolean isMigrate)
            throws IOException, SQLException {
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg + " [please wait]...", verbose, printStream);

        final File dbFolder = image.getMainDbBackupFolder();
        final File mainDbDumpFile = new File(dbFolder, BackupImage.MAINDB_BACKUP_FILENAME);

        if(!mainDbDumpFile.exists()) throw new IllegalStateException("File '"+mainDbDumpFile.getAbsolutePath()+" does not exist");
        if(mainDbDumpFile.isDirectory())
            throw new IllegalStateException("File '"+mainDbDumpFile.getAbsolutePath()+" is not a regular file");

        final List<String> omitTablesList;
        if (isMigrate) {
            omitTablesList = getExcludedTableNames(ssgHome);
        }else {omitTablesList = null;}

        final FileReader mainFileReader = new FileReader(mainDbDumpFile);
        final BufferedReader mainBackupReader = new BufferedReader(mainFileReader);
        String tmp;
        boolean didUpdateClusterProperties = false;
        try {
            c.setAutoCommit(false);
            final Statement stmt = c.createStatement();
            try {
                //always test importing the main backup
                while ((tmp = mainBackupReader.readLine()) != null) {
                    if (tmp.endsWith(";")) {
                        //a 5.0 image may contain audit data, buzzcut won't, but it will contain drop and create
                        //audit table statements

                        //is it an audit table insert? if so skip it
                        //for 5.0 also, audits are always restored separately
                        if(isAuditInsertStatement(tmp)) continue;

                        if (isMigrate && doesAffectsTables(tmp, omitTablesList)) {
                            logger.finest("SQL statement: '" + tmp + "' was not executed.");
                        } else {
                            stmt.executeUpdate(fixSql(tmp.substring(0, tmp.length() - 1)));
                            //if its a migrate, we need to know if we modified cluster properties table
                            if(isMigrate && !didUpdateClusterProperties){
                                didUpdateClusterProperties = isClusterPropertyStatement(tmp);
                            }
                        }
                    } else {
                        throw new SQLException("unexpected statement " + tmp);
                    }
                }

                // commit at the end if everything updated correctly
                final String msg1 = "\tDatabase dump import loaded succesfully, committing... ";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, verbose, printStream, false);
                c.commit();
            } catch (SQLException e) {
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING,
                        "Error loading database dump due to error: " + e.getMessage(), verbose, printStream, false);

                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING,
                        "Rolling back database changes... ", verbose, printStream, false);
                
                c.rollback();
                throw e;
            } catch (IOException e) {
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING,
                        "Error loading database dump due to error: " + e.getMessage(), verbose, printStream, false);

                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING,
                        "Rolling back database changes... ", verbose, printStream, false);
                c.rollback();
                throw e;
            } finally {
                stmt.close();
                c.setAutoCommit(true);
            }
        } finally {
            mainBackupReader.close();
            mainFileReader.close();
        }
        ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Done", verbose, printStream);
        return didUpdateClusterProperties;
    }

    private static String fixSql( final String sql ) {
        return fixUnescapedBackslash( fixEmptyHex( sql ) );
    }

    /**
     * Fix exports broken due to bug 8675. 
     */
    private static String fixEmptyHex( final String sql ) {
        String fixed = sql;

        if ( sql.contains( "0x," )) {
            fixed = fixed.replace( "0x,", "x''," );
        }

        return fixed;
    }

    /** bug 8183:
     * Pre 5.2 DBDumpUtil escaped newlines (0x0a and 0x0d) and simple and double quotes, but not literal backslash characters;
     * in 5.2 and later backslashes are also escaped.
     * Backslashes not escaping the above known characters must be assumed to be literals, therefore need to be escaped before being passed to mysql.
     */
    static String fixUnescapedBackslash(String s) {
        if (s == null || s.length() < 1 ) return s;
        
        if (s.length() == 1) return "\\".equals(s) ? "\\\\" : s;

        StringBuilder result = new StringBuilder();
        boolean escape = false;
        char prev, current;
        for(int i=0; i<s.length(); i++) {
            current = s.charAt(i);
            if( escape && current != 'n' && current != 'r' && current != '\'' && current != '\"' && current != '\\') {
                result.append('\\');
            }
            result.append(current);
            prev = current;
            escape = ! escape && '\\' == prev;
        }
        if (escape)
            result.append("\\");
        return result.toString();
    }

    private boolean isClusterPropertyStatement(final String sqlStatement) {
        if (sqlStatement == null) return false;

        final String createTable = "CREATE TABLE ";
        final String dropTable = "DROP TABLE IF EXISTS ";
        final String insertTable = "INSERT INTO ";

        final String tableName = "cluster_properties";

        if (sqlStatement.startsWith(createTable + tableName)
                || sqlStatement.startsWith(dropTable + tableName)
                || sqlStatement.startsWith(insertTable + tableName)) {
            return true;
        }
        return false;
    }


    private boolean isAuditInsertStatement(final String sqlStatement){
        final String insertTable = "INSERT INTO ";

        final String audit = "audit";
        if (sqlStatement.startsWith(insertTable + audit)) {
            return true;
        }
        return false;
    }

    /**
     * Compares the SQL statement and see if it will CREATE/DROP/INSERT any of the tables contained
     * in the provided omit table list.
     *
     * @param sqlStatement  SQL statement to compare
     * @param listOfTables    list of tables to see if sql statement affects
     * @return true if sql statement will affect a listed table, false otherwise
     */
    private boolean doesAffectsTables(final String sqlStatement, final List<String> listOfTables) {
        if (sqlStatement == null) return false;
        if (listOfTables == null) return false;
        if (listOfTables.isEmpty()) return false;

        final String createTable = "CREATE TABLE ";
        final String dropTable = "DROP TABLE IF EXISTS ";
        final String insertTable = "INSERT INTO ";

        for (String tableName : listOfTables) {
            if (sqlStatement.startsWith(createTable + tableName)
                    || sqlStatement.startsWith(dropTable + tableName)
                    || sqlStatement.startsWith(insertTable + tableName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reads the list of table names to be excluded. Read from the default within the file.
     *
     * @param ssgHome
     * @return  The list of table names found in the file.
     * @throws java.io.IOException
     */
    private List<String> getExcludedTableNames(final File ssgHome) throws IOException {
        final File excludeTableFile = new File(ssgHome, EXCLUDE_TABLES_PATH);
        if(!excludeTableFile.exists() || !excludeTableFile.isFile()){
            final String msg = "File '" + excludeTableFile.getAbsolutePath()+"' was not found. No tables will" +
                    " be excluded";
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, verbose, printStream);
            return Collections.emptyList();
        }

        final List<String> returnList = ImportExportUtilities.processFile(new File(ssgHome, EXCLUDE_TABLES_PATH));
        if(!returnList.isEmpty()){
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "\tThe following tables will not be modified: ",
                    verbose, printStream, false);

            for(String s: returnList){
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, s+" ",
                        verbose, printStream, false);
            }
            //just for pretty formatting
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "", verbose, printStream);
        }else{
            //this is a warning, as with migrate, you would expect that some tables will be excluded
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, "No tables will be excluded",
                    verbose, printStream);
        }

        return returnList;
    }

}
