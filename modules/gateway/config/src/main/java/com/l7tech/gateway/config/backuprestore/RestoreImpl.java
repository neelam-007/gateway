/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 22, 2009
 * Time: 3:14:09 PM
 */
package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.sql.SQLException;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.ConfigurationException;

/**
 * Implemenation of the Restore public api
 *
 * This class is immutable
 */
final class RestoreImpl implements Restore{

    private static final Logger logger = Logger.getLogger(RestoreImpl.class.getName());

    private final File ssgHome;
    private final PrintStream printStream;
    private final String applianceHome;
    private final boolean isVerbose;
    private final BackupImage image;
    private final DatabaseRestorer dbRestorer;
    private final File ompFile;
    private final File configDirectory;
    private final OSConfigManager osConfigManager;

    /**
     * @param verbose boolean, if true print messages to the supplied printStream
     * @param printStream PrintStream if not null and verbose is true, this is where messages will be written to
     */
    RestoreImpl(final String applianceHome,
                       final BackupImage image,
                       final DatabaseConfig dbConfig,
                       final String clusterPassphrase,
                       final boolean verbose,
                       final File ssgHome,
                       final PrintStream printStream) throws RestoreException {
        throwIfSsgInstallationInvalid(ssgHome);
        //we know this exists due to above validation
        configDirectory = getConfigDir(ssgHome);
        ompFile = new File(configDirectory, ImportExportUtilities.OMP_DAT);

        if(applianceHome == null) throw new NullPointerException("applianceHome cannot be null");
        if(applianceHome.equals("")) throw new IllegalArgumentException("applianceHome cannot be null");
        if(image == null) throw new NullPointerException("image cannot be null");

        if(dbConfig != null){
            if(clusterPassphrase == null) throw new NullPointerException("clusterPassphrase cannot be null");
            if(clusterPassphrase.isEmpty()) throw new IllegalArgumentException("clusterPassphrase cannot be empty");
            
            //validate the dbConfig
            final DatabaseConfig testConfig = new DatabaseConfig(dbConfig);
            boolean localDb = DatabaseRestorer.canDatabaseBeRestored(dbConfig.getHost());
            this.dbRestorer = (localDb)? new DatabaseRestorer(ssgHome,
                    clusterPassphrase, testConfig, image, printStream, verbose): null;
        }else{
            this.dbRestorer = null;
        }

        this.image = image;
        isVerbose = verbose;

        this.ssgHome = ssgHome;
        //this class is not usable without an installed SSG > 5.0
        this.printStream = printStream;
        this.applianceHome = applianceHome;
        if((new File(ssgHome, OSConfigManager.BACKUP_MANIFEST).exists())){
            osConfigManager = new OSConfigManager(ssgHome, false, isVerbose, printStream);
        }else{
            osConfigManager = null;
        }
    }

    /**
     * Validates that omp.dat exists in the SSG installation. It must exist if the SSG has been correctly
     * installed
     * @param testSsgHome
     */
    private void throwIfSsgInstallationInvalid(final File testSsgHome){
        if(testSsgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!testSsgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!testSsgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");

        //We have to have omp.dat
        final File ompFile = new File(getConfigDir(testSsgHome), ImportExportUtilities.NODE_PROPERTIES);
        if(!ompFile.exists() || !ompFile.isFile())
            throw new IllegalStateException("File '"+ ompFile.getAbsolutePath()+" not found");
    }

    /**
     * The returned File may not exist, this method it just for convenience
     * @param testSsgHome may not exist
     * @return
     */
    private File getConfigDir(final File testSsgHome){
        return new File(testSsgHome, ImportExportUtilities.NODE_CONF_DIR);
    }

    public Result restoreComponentOS(boolean isRequired) throws RestoreException {

        if (new File(applianceHome).exists()) {
            // copy system config files
            //OSConfigManager.saveOSConfigFiles(dir.getAbsolutePath(), ssgHome);
            try {
                //need to use the exclude file
                final File osFolder = image.getOSFolder();
                if(osConfigManager == null){
                    final String msg = "Operating System restore is not applicable for this host";
                    if(isRequired){
                        throw new RestoreException(msg);
                    }
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    return Result.NOT_APPLICABLE;

                }else if(!osFolder.exists() || !osFolder.isDirectory()){
                    //no os backup in image
                    final String msg = "No Operating System backup found in image";
                    if(isRequired){
                        throw new RestoreException(msg);
                    }
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    return Result.NOT_APPLICABLE;
                }
                osConfigManager.copyFilesToInternalFolderPriorToReboot(osFolder);
                return Result.SUCCESS;
            } catch (OSConfigManager.OSConfigManagerException e) {
                throw new RestoreException("Could not restore OS configuration files: " + e.getMessage());
            }
        }else{
            if(isRequired){
                throw new RestoreException("No Operation System backup found in image");
            }
            return Result.NOT_APPLICABLE;
        }
    }

    public Result restoreComponentAudits(final boolean isRequired, final boolean isMigate)
            throws RestoreException {
        final File auditsFile = image.getAuditsBackupFile();

        //case when no db is configured
        if(this.dbRestorer== null) {
            //We have been asked to restore the audit database component. We cannot as no db is configured
            if (auditsFile != null && image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O &&
                    auditsFile.exists() && auditsFile.isFile()) {
                //after 5.0 we can tell if the image contained audit data or not
                String msg = "Ignoring audits backup as either the target database is remote " +
                        "or the configuration is invalid";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, this.printStream);
            } else {
                //5.0 or post 5.0 and audit data does not exist
                String msg = "Cannot restore audits as no database is configured";
                if (image.getImageVersion() == BackupImage.ImageVersion.FIVE_O) {
                    msg = msg + ". Image is 5.0 and may contain audit data, which has been ignored";
                }
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
            }
            return Result.NOT_APPLICABLE;
        }

        if(auditsFile == null || !auditsFile.exists() || !auditsFile.isFile()){
            if(image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O){
                if(isRequired && !isMigate){
                    throw new RestoreException("No audit backup found in image");
                }
            }else{
                //if this is 5.0 and there is no audit file, then there is also no db file!
                //5.0 does not allow this
                throw new RestoreException("No database backup found in the image");
            }

            //so this case is -> after 5.0 and isRequired is false
            String msg = "No audit backup found in image";
            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream );
            //this is success, as we found no data and as it was not required, the component does not apply
            //to the back up image
            return Result.SUCCESS;
        }

        if(image.getImageVersion() == BackupImage.ImageVersion.FIVE_O){
            //if it's 5.0 we can't tell if there is audit data without parsing main_backup.sql
            //we have a main_backup.sql, does it contain any insert into audit... statements?
            //INSERT INTO audit_
            //audits come very early on in the main_backup.sql file

            final byte[] bytes = new byte[4096];
            try {
                IOUtils.slurpStream(new FileInputStream(auditsFile), bytes);
            } catch (IOException e) {
                throw new RestoreException("Could not check the 5.0 backup for audits due to: " + e.getMessage());
            }
            try {
                final String startOfFile = new String(bytes, "UTF-8");
                final int index = startOfFile.indexOf("INSERT INTO audit_");
                if(index == -1){
                    //see http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_Backup_Restore_Func_Spec#ssgmigrate.sh
                    //to understand this requirement
                    if(isRequired && !isMigate){
                        throw new RestoreException("No audit backup found in image");
                    }
                    final String msg = "No audit backup in image";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, this.printStream);
                    return Result.SUCCESS;
                }
            } catch (UnsupportedEncodingException e) {
                throw new RestoreException("Could not check the 5.0 backup for audits due to data problem: "
                        + e.getMessage());
            }
        }

        //The target database must always exist when restoring audits. If it doesn't exist, then its a client
        //calling error
        if(!dbRestorer.doesDatabaseExist()){
            final DatabaseConfig dbConfig = dbRestorer.getDbConfig();
            throw new RestoreException("Database '" + dbConfig.getName() + "' on host '"+ dbConfig.getHost()
                    +"' does not exist");
        }

        try {
            String msg = "Restoring audits...";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, this.printStream, false);
            dbRestorer.restoreAudits();
            msg = ". Done";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, this.printStream);
        } catch (DatabaseRestorer.DatabaseRestorerException e) {
            throw new RestoreException("Could not restore database audits: " + e.getMessage());
        } catch (IOException e) {
            throw new RestoreException("Could not restore database audits: " + e.getMessage());
        } catch (SQLException e) {
            throw new RestoreException("Could not restore database audits: " + e.getMessage());
        }

        return Result.SUCCESS;
    }

    /**
     * It's ok to call this method if the image has no database info and if the DatabaseConfig is null from
     * the constructor. When no backup is done, this is logged and Result.NOT_APPLICABLE is returned.
     * <p/>
     * Restore the database component. This includes the main database backup and my.cnf and not audits
     * Backup only happens if the BackupImage contains these elements. If they don't happen, nothing happens
     * The database restore will only happen if the dbConfig object represents a host local to this system.
     * <p/>
     * If the image contains db information but the host is not local, it will beignored. This will be logged
     * and printed if verbose is true was used
     * <p/>
     * Restores the main database - all schema and data apart from audit data. Will create the gateway user if the
     * database had to be created
     */
    public Result restoreComponentMainDb(final boolean isRequired,
                                         final boolean isMigrate,
                                         final boolean newDatabaseIsRequired,
                                         final String pathToMappingFile,
                                         final boolean updateNodeProperties) throws RestoreException {
        if (this.dbRestorer == null) {
            if (isRequired) {
                throw new RestoreException("No database backup found in image");
            }
            //We have been asked to restore the main database component. We cannot as it is not local
            //or the system property to bypass this has not been set
            final File backupFile = new File(image.getMainDbBackupFolder(), BackupImage.MAINDB_BACKUP_FILENAME);
            if (backupFile.exists() && !backupFile.isDirectory()) {
                final String msg = "Ignoring main database backup as either the target database is remote " +
                        "or the configuration is invalid";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
            } else {
                //this is a warning as this code should not have been called knowing the backup is not applicable
                String msg = "Image contains no audit information and this instance is not configured to restore " +
                        "any database components";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
            }
            return Result.NOT_APPLICABLE;
        }

        final File dbFolder = image.getMainDbBackupFolder();
        if (dbFolder == null || !dbFolder.exists() || !dbFolder.isDirectory()) {
            if (isRequired) {
                throw new RestoreException("No database backup found in image");
            } else {
                String msg = "No database backup found in image";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                //this is success, as we found no data and as it was not required, the component does not apply
                //to the back up image
                return Result.SUCCESS;
            }
        }

        final File dbSql = new File(dbFolder, BackupImage.MAINDB_BACKUP_FILENAME);
        if (!dbSql.exists()) throw new IllegalStateException("'" + dbSql.getAbsolutePath() + "' could not be found");
        if (dbSql.isDirectory()) throw new IllegalStateException("'" + dbSql.getAbsolutePath() + "' is a directory");

        //Only need to check if db exists when newDatabaseIsRequired is false
        final boolean wasNewDbCreated;
        if (isMigrate && newDatabaseIsRequired) {
            //no check for db existence, we know we want to create a new database
            wasNewDbCreated = dbRestorer.createNewDatabaseOrThrow(false);
        } else {
            //only create the database if it doesn't exist
            final boolean doesDatabaseExist = dbRestorer.doesDatabaseExist();
            if (isMigrate && !doesDatabaseExist) {
                //migrate is not allowed to just go ahead and create a new db, unless it has been requested
                throw new IllegalStateException("Specified database does not exist. Use the "
                        + Importer.CREATE_NEW_DB.getName() + " option to create a new database when migrating");
            }
            //here the database may not exist, but if it's a Migrate, then it does exist
            //=> if it's not a Migrate - create the database!, as a restore always wants a new database
            if (!isMigrate) {
                dbRestorer.createNewDatabaseOrThrow(true);
                wasNewDbCreated = true;
            } else {
                wasNewDbCreated = false;
            }

        }

        String preMigrateLicense = null;

        if (!wasNewDbCreated) {
            //check no ssg is running    //only need to test if db wasn't created, a new db can't have a ssg using it
            checkAndThrowIfSSGRunning(isVerbose, printStream);
            try {
                preMigrateLicense = dbRestorer.getExistingLicense();
            } catch (SQLException e) {
                String msg = "Could not check for existing license: " + e.getMessage();
                ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, this.printStream);
                throw new RestoreException(msg);
            }
        }

        //Now we know we got a database to work with. Restore the database, as no ssg is currently using it
        final boolean didUpdateClusterProperties;
        try {
            didUpdateClusterProperties = dbRestorer.restoreDb(wasNewDbCreated, isMigrate);
        } catch (Exception e) {
            String msg = "Could not restore database: " + e.getMessage();
            ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, this.printStream);
            throw new RestoreException(msg);
        }

        //if it's a migrate, we may need to udpate the oid of the license on the target system
        //we may also need to load mappings
        if (isMigrate) {
            if (!wasNewDbCreated) {
                try {
                    if (preMigrateLicense != null && didUpdateClusterProperties)
                        dbRestorer.reloadLicense(preMigrateLicense);
                } catch (Exception e) {
                    String msg = "Could not check / update existing license: " + e.getMessage();
                    ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, this.printStream);
                    throw new RestoreException(msg);
                }
            }

            final String mappingPath = ImportExportUtilities.getAbsolutePath(pathToMappingFile);

            if (mappingPath != null && !wasNewDbCreated) {
                try {
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                            "loading mapping file " + mappingPath, isVerbose, this.printStream);

                    MappingUtil.CorrespondanceMap mapping = MappingUtil.loadMapping(mappingPath);
                    MappingUtil.applyMappingChangesToDB(dbRestorer.getDbConfig(), mapping, isVerbose, printStream);

                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Mapping file loaded",
                            isVerbose, this.printStream);

                } catch (Exception e) {
                    String msg = "Problem loading / applying mappings: " + e.getMessage();
                    ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, this.printStream);
                    throw new RestoreException(msg);
                }
            } else if (mappingPath != null) {
                //we were given a mapping file but it does not apply as we just created the database
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                        "Mapping file '" + mappingPath + "' ignored as new database was created",
                        isVerbose, this.printStream);
            }
        }
        //migrate always requires that node.properties is updated
        if (isMigrate || updateNodeProperties) writeNewNodeProperties();

        //See if my.cnf needs to be copied

        if (image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O) {
            final File myCnf = image.getDatabaseConfiguration();
            if (!myCnf.exists() || !myCnf.isFile()) {
                final String msg = "my.cnf is not contained in the backup image";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
            } else {
                //copy file
                //FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
                String etcFolder = SyspropUtil.getString("com.l7tech.config.backuprestore.mycnfdir", "/etc");
                final File etcDir = new File(etcFolder);
                if (!etcDir.exists() && !etcDir.isDirectory()) {
                    final String msg = "Cannot copy my.cnf as '" + etcFolder + "' folder not found";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                } else {
                    try {
                        FileUtils.copyFile(myCnf, new File(etcDir, myCnf.getName()));
                    } catch (IOException e) {
                        final String msg = "Cannot copy my.cnf: " + e.getMessage();
                        ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                    }
                }
            }
        }

        return Restore.Result.SUCCESS;
    }

    private void writeNewNodeProperties() throws RestoreException {

        DatabaseConfig dbConfig = dbRestorer.getDbConfig();
        MasterPasswordManager mpm = new MasterPasswordManager(
                new DefaultMasterPasswordFinder(ompFile));

        final PropertiesConfiguration nodePropertyConfig = new PropertiesConfiguration();
        nodePropertyConfig.setAutoSave(false);
        nodePropertyConfig.setListDelimiter((char) 0);

        nodePropertyConfig.setProperty("node.db.config.main.host", dbConfig.getHost());
        nodePropertyConfig.setProperty("node.db.config.main.host", dbConfig.getHost());
        nodePropertyConfig.setProperty("node.db.config.main.port", dbConfig.getPort());
        nodePropertyConfig.setProperty("node.db.config.main.name", dbConfig.getName());
        nodePropertyConfig.setProperty("node.db.config.main.user", dbConfig.getNodeUsername());
        nodePropertyConfig.setProperty("node.db.config.main.pass",
                mpm.encryptPassword(dbConfig.getNodePassword().toCharArray()));
        nodePropertyConfig.setProperty("node.cluster.pass",
                mpm.encryptPassword(this.dbRestorer.getClusterPassphrase().toCharArray()));
        try {
            nodePropertyConfig.save(new File(configDirectory, ImportExportUtilities.NODE_PROPERTIES));
        } catch (ConfigurationException e) {
            throw new RestoreException("Could not save node.properties: " + e.getMessage());
        }
    }

    /**
     * Only needed to be checked if a new database was not created.
     * @param verbose
     * @param printStream
     * @throws RestoreException
     */
    private void checkAndThrowIfSSGRunning(boolean verbose, PrintStream printStream) throws RestoreException {
        final List<String> runningSsg;
        try {
            runningSsg = ImportExportUtilities.getRunningSSG(dbRestorer.getDbConfig(), 10000, verbose, printStream);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot verify if SSG is running: " + e.getMessage());
            throw new RestoreException("Cannot verify if SSG is running: " + e.getMessage());
        }

        if (!runningSsg.isEmpty()) {
            final StringBuffer runningGateways = new StringBuffer();
            for (int i=0; i < runningSsg.size()-1; i++) {
                runningGateways.append(runningSsg.get(i) + ", ");
            }
            runningGateways.append(runningSsg.get(runningSsg.size()-1));

            throw new RestoreException("Possible SecureSpan Gateway(s) may be running and connected to the database." +
                    "  Please shutdown the following gateway(s): " + runningGateways.toString());
        }
    }
}
