package com.l7tech.gateway.config.backuprestore;

import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.Charsets;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.FileUtils;
import com.l7tech.util.IOUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Implemenation of the Restore public api
 *
 * This class is immutable
 */
final class RestoreImpl implements Restore{

    private static final Logger logger = Logger.getLogger(RestoreImpl.class.getName());

    private final PrintStream printStream;
    private final boolean isVerbose;
    private final File applianceHome;
    private final File esmHome;
    private final BackupImage image;
    private final DatabaseRestorer dbRestorer;
    private final File ssgConfigDir;
    private final OSConfigManager osConfigManager;
    private final File ssgHome;
    private final boolean isMigrate;

    /**
     * @param verbose boolean, if true print messages to the supplied printStream
     * @param printStream PrintStream if not null and verbose is true, this is where messages will be written to
     * @param isMigrate          if true, the file /config/backup/cfg/exclude_files will be consulted and any files
     *                           listed will be ignored during the restore. If true, then node.properties and omp.dat
     *                           will never be restored
     */
    RestoreImpl(final File secureSpanHome,
                final BackupImage image,
                final DatabaseConfig dbConfig,
                final String clusterPassphrase,
                final boolean verbose,
                final PrintStream printStream,
                final boolean isMigrate)  {
        if (secureSpanHome == null) throw new NullPointerException("secureSpanHome cannot be null");
        if (!secureSpanHome.exists()) throw new IllegalArgumentException("secureSpanHome directory does not exist");
        if (!secureSpanHome.isDirectory()) throw new IllegalArgumentException("secureSpanHome must be a directory");

        //check that the gateway exists
        final File testSsgHome = new File(secureSpanHome, ImportExportUtilities.GATEWAY);

        throwIfSsgInstallationInvalid(testSsgHome);
        this.ssgHome = testSsgHome;
        ssgConfigDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);

        if(image == null) throw new NullPointerException("image cannot be null");

        if(dbConfig != null){
            if(clusterPassphrase == null) throw new NullPointerException("clusterPassphrase cannot be null");
            if(clusterPassphrase.trim().isEmpty()) throw new IllegalArgumentException("clusterPassphrase cannot be empty");
            
            //validate the dbConfig
            final DatabaseConfig testConfig = new DatabaseConfig(dbConfig);
            boolean localDb = ImportExportUtilities.isDatabaseAvailableForBackupRestore(dbConfig.getHost());
            this.dbRestorer = (localDb)? new DatabaseRestorer(ssgHome,
                    clusterPassphrase, testConfig, image, printStream, verbose): null;
        }else{
            this.dbRestorer = null;
        }

        this.image = image;
        isVerbose = verbose;

        applianceHome = new File(secureSpanHome, ImportExportUtilities.APPLIANCE);//may not exist, that's ok
        esmHome = new File(secureSpanHome, ImportExportUtilities.ENTERPRISE_SERVICE_MANAGER);//may not exist, that's ok

        this.printStream = printStream;
        if(OSConfigManager.isAppliance(ssgHome)){
            osConfigManager = new OSConfigManager(ssgHome, false, isVerbose, printStream);
        }else{
            osConfigManager = null;
        }

        this.isMigrate = isMigrate;
    }

    /**
     * Validates that configuration, modular and custom assertion directories exist.
     * @param testSsgHome
     */
    private void throwIfSsgInstallationInvalid(final File testSsgHome){
        if(testSsgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!testSsgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!testSsgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");

        //Make sure ssg configuration folder exists
        final File testSsgConfigDir = new File(testSsgHome, ImportExportUtilities.NODE_CONF_DIR);
        if(!testSsgConfigDir.exists() || !testSsgConfigDir.isDirectory())
            throw new IllegalStateException("Gateway configuration directory '"+testSsgConfigDir.getAbsolutePath()+"'is missing");


        final File testCADir = new File(testSsgHome, ImportExportUtilities.CA_JAR_DIR);
        if(!testCADir.exists() || !testCADir.isDirectory())
            throw new IllegalStateException("Custom assertion directory '"+testCADir.getAbsolutePath()+"' is missing");

        final File testMADir = new File(testSsgHome, ImportExportUtilities.MA_AAR_DIR);
        if(!testMADir.exists() || !testMADir.isDirectory())
            throw new IllegalStateException("Modular assertion directory '"+testMADir.getAbsolutePath()+"' is missing");

    }

    @Override
    public ComponentResult restoreComponentCA() throws RestoreException {

        final File imageCADir = image.getCAFolder();
        if(imageCADir == null){
            final String msg = "No ca folder found. No custom assertions or property files can be restored";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        try {
            //restore all .property files found in the ca folder to the node/default/etc/conf folder
            final boolean propFilesCopied = ImportExportUtilities.copyFiles(
                    imageCADir, ssgConfigDir, new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".properties");
                }
            }, isVerbose, printStream);

            if(!propFilesCopied){
                final String msg = "No Custom Assertion property files were restored";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

            final File ssgCaFolder = new File(ssgHome, ImportExportUtilities.CA_JAR_DIR);
            //restore all jar files found to /opt/SecureSpan/Gateway/runtime/modules/lib
            final boolean jarFilesCopied = ImportExportUtilities.copyFiles(
                    imageCADir, ssgCaFolder, new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".jar");
                }
            }, isVerbose, printStream);

            if(!jarFilesCopied){
                final String msg = "No Custom Assertion jar files were restored";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

        } catch (IOException e) {
            throw new RestoreException("Problem restoring custom assertion component: " + e.getMessage());
        }

        return new ComponentResult(ComponentResult.Result.SUCCESS);
    }

    @Override
    public ComponentResult restoreComponentMA() throws RestoreException {
        final File imageMADir = image.getMAFolder();
        if(imageMADir == null){
            final String msg = "No ma folder found. No modular assertions can be restored";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        try {
            //We want to restore only modular assertions which do not exist on the target system.
            //As back up backs up each assertion found, we need to check to see if a modular assertion
            //exists on the target, and if so, don't copy it

            //first build up the list of modular assertions on the target system
            final File ssgMaFolder = new File(ssgHome, ImportExportUtilities.MA_AAR_DIR);
            if(!ssgMaFolder.exists() || !ssgMaFolder.isDirectory())
                throw new IllegalStateException("Modular assertion folder not found");

            final String [] foundAssertions = ssgMaFolder.list(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".aar");
                }
            });

            final Set<String> uniqueAssertionNames = new HashSet<String>();
            for(String s: foundAssertions){
                //extract the name part of the found assertion
                final String namePart = ImportExportUtilities.getFilePart(s);
                //EchoRoutingAssertion-5.0.aar - remove everything after -
                final String assertionName;
                if(namePart.indexOf("-") == -1){
                    assertionName = namePart;                    
                }else{
                    assertionName = namePart.substring(0, namePart.lastIndexOf("-"));
                }

                uniqueAssertionNames.add(assertionName);
            }

            //restore all aar files found to /opt/SecureSpan/Gateway/runtime/modules/assertions
            final boolean aarFilesCopied = ImportExportUtilities.copyFiles(imageMADir, ssgMaFolder, new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    //if it's not .aar, ignore it completely
                    if(!name.endsWith(".aar")){
                        final String msg = "Ignoring non modular assertion file found: '" + name+"' ";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                        return false;
                    }

                    //check if this modular assertion already exists on the host
                    //extract the name part of the found assertion
                    final String namePart = ImportExportUtilities.getFilePart(name);
                    //EchoRoutingAssertion-5.0.aar - remove everhthing after -
                    final String assertionName;
                    if(namePart.indexOf("-") == -1){
                        assertionName = namePart;
                    }else{
                        assertionName = namePart.substring(0, namePart.lastIndexOf("-"));
                    }

                    if(uniqueAssertionNames.contains(assertionName)){
                        final String msg = "Modular assertion '" + name+"' is being skipped as it already exists in "
                                + ssgMaFolder.getAbsolutePath();
                        ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                        return false;
                    }

                    return true;
                }
            }, isVerbose, printStream);

            if(!aarFilesCopied){
                final String msg = "No modular assertion aar files were restored";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

        } catch (IOException e) {
            throw new RestoreException("Problem restoring modular assertion component: " + e.getMessage());
        }

        return new ComponentResult(ComponentResult.Result.SUCCESS);
    }

    @Override
    public ComponentResult restoreComponentEXT() throws RestoreException {
        final File imageEXTDir = image.getEXTFolder();
        if(imageEXTDir == null){
            final String msg = "No ext folder found. No lib/ext files can be restored.";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        final File ssgExtFolder = new File(ssgHome, ImportExportUtilities.LIB_EXT_DIR);
        if(!ssgExtFolder.exists() || !ssgExtFolder.isDirectory())
            throw new IllegalStateException("/Gateway/runtime/lib/ext folder not found");

        //restore all files found to /opt/SecureSpan/Gateway/runtime/lib/ext
        try {
            final boolean extFilesCopied = ImportExportUtilities.copyFiles(imageEXTDir, ssgExtFolder, null, isVerbose, printStream);
            if(!extFilesCopied){
                final String msg = "No lib/ext files were restored";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            }

        } catch (IOException e) {
            throw new RestoreException("Problem restoring lib/ext component: " + e.getMessage());
        }

        return new ComponentResult(ComponentResult.Result.SUCCESS);
    }

    @Override
    public ComponentResult restoreComponentConfig(final boolean ignoreNodeIdentity) throws RestoreException {
        final File imageConfigDir = image.getConfigFolder();
        if(imageConfigDir == null){
            final String msg = "No config folder found. No ssg configuration can be restored";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        try {
            final List<String> ssgConfigFilesToExclude;
            if(isMigrate){
                ssgConfigFilesToExclude = ImportExportUtilities.getExcludedFiles(ssgHome, EXCLUDE_FILES_PATH);
                if(ssgConfigFilesToExclude.isEmpty()){
                    String msg = "";
                    if(!new File(ssgHome, EXCLUDE_FILES_PATH).exists()){
                        msg = "File '" + new File(ssgHome, EXCLUDE_FILES_PATH).getAbsolutePath()
                                +"' was not found. ";
                    }
                    msg = msg + "No ssg configuration files will be excluded.";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                }
            }else{
                ssgConfigFilesToExclude = Collections.emptyList();
            }

            //copy all ssg config property files from the config folder to the ssg config folder
            ImportExportUtilities.copyFiles(imageConfigDir, ssgConfigDir, new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {

                    //has this file being excluded during a migrate?
                    if (isMigrate) {
                        if (ssgConfigFilesToExclude.contains(name)) {
                            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                                    "\tThe following ssg configuration files will not be overwritten: " + name, isVerbose,
                                    printStream, false);
                            return false;
                        }
                    }

                    //this is not necessary, however it a safeguard against copying config files accidentally
                    //we are explicitly only copying files we know about
                    for (String ssgFile : ImportExportUtilities.CONFIG_FILES) {
                        if ((ignoreNodeIdentity || isMigrate) && ssgFile.equals(ImportExportUtilities.NODE_PROPERTIES))
                            continue;
                        if ((ignoreNodeIdentity || isMigrate) && ssgFile.equals(ImportExportUtilities.OMP_DAT))
                            continue;
                        if (ssgFile.equals(name)) return true;
                    }
                    return false;
                }
            }, isVerbose, printStream);

        } catch (IOException e) {
            throw new RestoreException("Problem restoring config component: " + e.getMessage());
        }
        
        return new ComponentResult(ComponentResult.Result.SUCCESS);
    }

    @Override
    public ComponentResult restoreComponentOS() throws RestoreException {

        if (applianceHome.exists()) {
            //need to use the exclude file
            final File osFolder = image.getOSFolder();
            if(osConfigManager == null){
                final String msg = "Operating System restore is not applicable for this host";
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
            }else if(osFolder == null){
                //no os backup in image
                final String msg = "No Operating System backup found in image";
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
            }

            final List<String> excludedFiles;
            if(isMigrate){
                excludedFiles = ImportExportUtilities.getExcludedFiles(ssgHome, Restore.EXCLUDE_FILES_PATH);
                if(excludedFiles.isEmpty()){
                    String msg = "";
                    if(!new File(ssgHome, EXCLUDE_FILES_PATH).exists()){
                        msg = "File '" + new File(ssgHome, EXCLUDE_FILES_PATH).getAbsolutePath()
                                +"' was not found. ";
                    }
                    msg = msg + "No OS files will be excluded.";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                }
            } else {
                excludedFiles = Collections.emptyList();
            }

            final boolean restartIsRequired = osConfigManager.copyFilesToInternalFolderPriorToReboot(osFolder, excludedFiles);
            return new ComponentResult(ComponentResult.Result.SUCCESS, restartIsRequired);
        }else{
            final String msg = "Operating System restore is not applicable for this host";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }
    }

    @Override
    public ComponentResult restoreComponentESM() throws RestoreException {
        try {
            //validate the esm looks ok - just a basic check
            ImportExportUtilities.throwIfEsmNotPresent(esmHome);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage());
        }

        try {
            //this is a RestoreException as opposed to an Illegal state. The above throw if esm not present
            //is an illegal state as this component shouldn't be called if the esm is not installed
            ImportExportUtilities.throwifEsmIsRunning();
        } catch (IllegalStateException e) {
            throw new RestoreException(e.getMessage());
        }

        final File esmFolder = image.getESMFolder();
        if(esmFolder == null){
            final String msg = "No ESM backup found in image";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        try {
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Restoring ESM component", isVerbose,
                    printStream);

            //copyDir will always delete the destination folder if it exists
            //copy the etc folder - contains omp.dat
            ImportExportUtilities.copyDir(new File(esmFolder, "etc"),
                    new File(esmHome, "etc"), null, isVerbose, printStream);

            final String varDbFolder = "var" + File.separator + "db";

            ImportExportUtilities.copyDir(new File(esmFolder, varDbFolder), new File(esmHome, varDbFolder), null, isVerbose, printStream);

            final String emConfigProp = "emconfig.properties";

            final String varEmConfig = "var" + File.separator + emConfigProp;
            final File emConfigTargetFile = new File(esmHome, varEmConfig);
            if(emConfigTargetFile.exists()){
                if(!emConfigTargetFile.delete()){
                    throw new RestoreException("Cannot delete file '" + emConfigTargetFile.getAbsolutePath()
                            +"' before restoring it");
                }
            }

            final File emConfigSourceFile = new File(esmFolder, varEmConfig);

            FileUtils.copyFile(emConfigSourceFile, emConfigTargetFile);

            final String msg = "File '"+ emConfigSourceFile.getAbsolutePath()+"' copied to '"
                    + emConfigTargetFile.getAbsolutePath()+"'";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

        } catch (IOException e) {
            throw new RestoreException("Could not restore the ESM component: " + e.getMessage());
        }
        
        return new ComponentResult(ComponentResult.Result.SUCCESS);
    }

    @Override
    public ComponentResult restoreComponentAudits()
            throws RestoreException {
        final File auditsFile = image.getAuditsBackupFile();

        //case when no db is configured
        if(this.dbRestorer== null) {
            //We have been asked to restore the audit database component. We cannot as no db is configured
            String msg;
            if (auditsFile != null && image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O &&
                    auditsFile.exists() && auditsFile.isFile()) {
                //after 5.0 we can tell if the image contained audit data or not
                msg = "Ignoring audits backup as either the target database is remote " +
                        "or the configuration is invalid";
            } else {
                //5.0 or post 5.0 and audit data does not exist
                msg = "Cannot restore audits as no database is configured";
                if (image.getImageVersion() == BackupImage.ImageVersion.FIVE_O) {
                    msg = msg + ". Image is 5.0 and may contain audit data, which has been ignored";
                }
            }
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
        }

        if (auditsFile == null || !auditsFile.exists() || !auditsFile.isFile()) {
            if(isMigrate){
                //if it's a migrate, then it's ok that we don't have an audits file file
                //this is a success case, as migrate will always specify -audits, but it may not be applicable
                final String msg = "No audit data found in image";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                return new ComponentResult(ComponentResult.Result.SUCCESS);
            } else if (image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O) {
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, "No audit backup found in image");
            } else {
                //if this is 5.0 and there is no audit file, then there is also no db file!
                //5.0 does not allow this
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, "No database backup found in the image");
            }
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
            final String startOfFile = new String(bytes, Charsets.UTF8);
            final int index = startOfFile.indexOf("INSERT INTO audit_");
            if(index == -1){
                //see http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_Backup_Restore_Func_Spec#ssgmigrate.sh
                //to understand this requirement - when isMigrate is true, missing audit data is ok
                final String msg = "No audit data found in image";
                if (isMigrate) {
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    return new ComponentResult(ComponentResult.Result.SUCCESS, msg);
                } else {
                    return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg);
                }
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
            String msg = "\tRestoring audits... ";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, this.printStream, false);
            dbRestorer.restoreAudits();
        } catch (DatabaseRestorer.DatabaseRestorerException e) {
            throw new RestoreException("Could not restore database audits: " + e.getMessage());
        } catch (IOException e) {
            throw new RestoreException("Could not restore database audits: " + e.getMessage());
        } catch (SQLException e) {
            throw new RestoreException("Could not restore database audits: " + e.getMessage());
        }

        return new ComponentResult(ComponentResult.Result.SUCCESS);
    }

    @Override
    public ComponentResult restoreComponentMainDb(final boolean newDatabaseIsRequired,
                                                  final String pathToMappingFile) throws RestoreException {
        //can restore my.cnf without the database contents restore
        boolean restartMaybeRequired = false;
        //See if my.cnf needs to be copied
        if (image.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O) {
            final File myCnf = image.getDatabaseConfiguration();
            if (!myCnf.exists() || !myCnf.isFile()) {
                final String msg = "my.cnf is not contained in the backup image";
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            } else {
                //copy file
                //FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
                final String etcFolder = ConfigFactory.getProperty( "com.l7tech.config.backuprestore.mycnfdir", "/etc" );
                final File etcDir = new File(etcFolder);
                boolean dontCopy = false;
                if(isMigrate){
                    final List<String> excludedFiles =
                            ImportExportUtilities.getExcludedFiles(ssgHome, Restore.EXCLUDE_FILES_PATH);
                    final File testMyCnf = new File(etcDir, BackupImage.MY_CNF);
                    if(excludedFiles.contains(testMyCnf.getAbsolutePath())){
                        final String msg = "Not copying file '" + testMyCnf +"' as it has been excluded.";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                        dontCopy = true;
                    }
                }

                if(!dontCopy){
                    if (!etcDir.exists() && !etcDir.isDirectory()) {
                        final String msg = "Cannot copy my.cnf as '" + etcFolder + "' folder not found";
                        ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                    } else {
                        try {
                            if (osConfigManager == null) {
                                final String msg = "my.cnf will not be restored as the Appliance is not installed";
                                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                            } else {
                                osConfigManager.copyFileToInternalFolder(myCnf, new File(etcDir, BackupImage.MY_CNF));
                                restartMaybeRequired = true;
                            }
                        } catch (IOException e) {
                            final String msg = "Cannot copy my.cnf: " + e.getMessage();
                            ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                        }
                    }
                }
            }
        }

        if (this.dbRestorer == null) {
            //We have been asked to restore the main database component. We cannot as it is not local
            //or the system property to bypass this has not been set
            final File backupFile = new File(image.getMainDbBackupFolder(), BackupImage.MAINDB_BACKUP_FILENAME);
            if (backupFile.exists() && !backupFile.isDirectory()) {
                final String msg = "Ignoring main database backup as either the target database is remote " +
                        "or the configuration is invalid";
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg, restartMaybeRequired);
            } else {
                String msg = "No database backup found in image";
                return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg, restartMaybeRequired);
            }
        }

        final File dbFolder = image.getMainDbBackupFolder();
        if (dbFolder == null || !dbFolder.exists() || !dbFolder.isDirectory()) {
            final String msg = "No database backup found in image";
            return new ComponentResult(ComponentResult.Result.NOT_APPLICABLE, msg, restartMaybeRequired);
        }

        final File dbSql = new File(dbFolder, BackupImage.MAINDB_BACKUP_FILENAME);
        if (!dbSql.exists()) throw new IllegalStateException("'" + dbSql.getAbsolutePath() + "' could not be found");
        if (dbSql.isDirectory()) throw new IllegalStateException("'" + dbSql.getAbsolutePath() + "' is a directory");

        //Only need to check if db exists when newDatabaseIsRequired is false
        final boolean wasNewDbCreated;
        if (isMigrate && newDatabaseIsRequired) {
            //now check for db existence, we know we want to create a new database
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

        //if it's a migrate, we may need to update the oid of the license on the target system
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
                            "Loading mapping file " + mappingPath, isVerbose, this.printStream);

                    MappingUtil.CorrespondanceMap mapping = MappingUtil.loadMapping(mappingPath);
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Mapping file loaded",
                            isVerbose, this.printStream);

                    MappingUtil.applyMappingChangesToDB(dbRestorer.getDbConfig(), mapping, isVerbose, printStream);

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

        return new ComponentResult(ComponentResult.Result.SUCCESS, restartMaybeRequired);
    }

    @Override
    public ComponentResult restoreNodeIdentity(final PropertiesConfiguration propertiesConfiguration,
                                               final File ompDatFile) throws RestoreException {

        if (ompDatFile != null && propertiesConfiguration == null)
            throw new IllegalArgumentException("ompDatFile cannot be null when propertiesConfiguration is not also null");

        final String taskVerb = (isMigrate) ? "migrating" : "restoring";
        try {
            if (propertiesConfiguration != null) {
                propertiesConfiguration.save(new File(ssgConfigDir, ImportExportUtilities.NODE_PROPERTIES));

                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Updated " + taskVerb + " host's node.properties file",
                        isVerbose, printStream);
            }

            if (ompDatFile != null) {
                final File ompOnTarget = new File(ssgConfigDir, ImportExportUtilities.OMP_DAT);
                if (ompOnTarget.exists() && !ompOnTarget.delete())
                    throw new RestoreException("Could not delete file from target'" + ompOnTarget.getAbsolutePath() + "'");
                try {
                    FileUtils.copyFile(ompDatFile, ompOnTarget);
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                            "Updated " + taskVerb + " host's omp.dat file", isVerbose, printStream);
                } catch (IOException e) {
                    throw new RestoreException("Could not copy omp.dat to host: " + e.getMessage());
                }
            }

            return new ComponentResult(ComponentResult.Result.SUCCESS);
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
            logger.log(Level.WARNING, "Cannot verify if Gateway is running: " + e.getMessage());
            throw new RestoreException("Cannot verify if Gateway is running: " + e.getMessage());
        }

        if (!runningSsg.isEmpty()) {
            final StringBuffer runningGateways = new StringBuffer();
            for (int i=0; i < runningSsg.size()-1; i++) {
                runningGateways.append(runningSsg.get(i) + ", ");
            }
            runningGateways.append(runningSsg.get(runningSsg.size()-1));

            throw new RestoreException("Possible Gateway(s) may be running and connected to the database." +
                    "  Please shutdown the following gateway(s): " + runningGateways.toString());
        }
    }
}
