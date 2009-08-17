package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import static com.l7tech.gateway.config.backuprestore.ImportExportUtilities.UtilityResult.Status.FAILURE;
import static com.l7tech.gateway.config.backuprestore.ImportExportUtilities.UtilityResult.Status.PARTIAL_SUCCESS;
import static com.l7tech.gateway.config.backuprestore.ImportExportUtilities.UtilityResult.Status.SUCCESS;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.util.*;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * </p>
 *
 *
 * <p>
 * The utility that restores or migrates an SSG image
 * Importer is responsible for accecpting parameters, validating them, then calling the appropriate methods of
 * the Restore interface
 * </p>
 *
 * @see BackupRestoreFactory
 * @see com.l7tech.gateway.config.backuprestore.Restore
 */
public final class Importer{

    private static final Logger logger = Logger.getLogger(Importer.class.getName());
    // importer options
    static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image",
            "location of image file locally to restore, or on ftp host if -ftp* options are used", true);
    static final CommandLineOption IMAGE_PATH_MIGRATE = new CommandLineOption("-image",
            "location of image file to migrate", true);
    static final CommandLineOption MAPPING_PATH = new CommandLineOption("-mapping",
            "location of the mapping template file", true);
    static final CommandLineOption DB_HOST_NAME = new CommandLineOption("-dbh", "database host name", true);
    static final CommandLineOption DB_NAME = new CommandLineOption("-db", "database name", true);
    static final CommandLineOption DB_ADMIN_PASSWD = new CommandLineOption("-dbp", "database admin password", true);
    static final CommandLineOption DB_ADMIN_USER = new CommandLineOption("-dbu", "database admin username", true);
    static final CommandLineOption OS_OVERWRITE =
            new CommandLineOption("-"+ ImportExportUtilities.ComponentType.OS.getComponentName(),
            "overwrite os level config files", false);
    static final CommandLineOption CREATE_NEW_DB = new CommandLineOption("-newdb"
            ,"create a new database when migrating", true);
    static final CommandLineOption MIGRATE = new CommandLineOption("-migrate",
            "apply migrate behavior instead of restore", false);

    static final CommandLineOption CONFIG_ONLY =
            new CommandLineOption("-"+ ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
                    "only migrate configuration files, no database migrate", false);
    static final CommandLineOption CLUSTER_PASSPHRASE =
            new CommandLineOption("-cp", "the cluster passphrase for the (resulting) database", true);
    static final CommandLineOption GATEWAY_DB_USERNAME =
            new CommandLineOption("-gdbu", "gateway database username", true);
    static final CommandLineOption GATEWAY_DB_PASSWORD =
            new CommandLineOption("-gdbp", "gateway database password", true);

    /**
     * Note this doesn't list MIGRATE. for ssgmigrate.sh, MIGRATE is an ignored option
     */
    static final List<CommandLineOption> ALL_MIGRATE_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            IMAGE_PATH_MIGRATE, MAPPING_PATH, DB_ADMIN_USER, DB_ADMIN_PASSWD,DB_HOST_NAME, DB_NAME, CONFIG_ONLY,
            OS_OVERWRITE, CLUSTER_PASSPHRASE, GATEWAY_DB_USERNAME, GATEWAY_DB_PASSWORD, CREATE_NEW_DB));

    static final List<CommandLineOption> ALL_RESTORE_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            IMAGE_PATH, DB_ADMIN_USER, DB_ADMIN_PASSWD, CommonCommandLineOptions.HALT_ON_FIRST_FAILURE,
            CommonCommandLineOptions.VERBOSE));

    static final List<CommandLineOption> MIGRATE_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            MIGRATE, MAPPING_PATH, CREATE_NEW_DB));

    static final List<CommandLineOption> COMMAND_LINE_DB_ARGS = Collections.unmodifiableList(Arrays.asList(
            DB_HOST_NAME, DB_NAME, CLUSTER_PASSPHRASE, GATEWAY_DB_USERNAME, GATEWAY_DB_PASSWORD));

    static final List<CommandLineOption> ALL_IGNORED_OPTIONS = Collections.unmodifiableList(Arrays.asList(
            new CommandLineOption("-p", "Ignored parameter for partition", false),
            new CommandLineOption("-mode", "Ignored parameter for mode type", true)));

    private String suppliedClusterPassphrase;

    private final File ssgHome;

    /**
     * Base folder of all Securespan products
     */
    private final File secureSpanHome;

    /**
     * If -v is supplied, where to print verbose output to
     */
    private final PrintStream printStream;

    private Map<String, String> programFlagsAndValues;
    private boolean isMigrate;

    /**
     * if true, send verbose output to the printStream, if it's not null
     */
    private boolean isVerbose;

    private boolean isSelectiveRestore;

    /**
     * When true, the restore is fail fast
     */
    private boolean isHaltOnFirstFailure;

    private BackupImage backupImage;

    /**
     * If a default restore is being done, or a selective restore with db components, this represents our db
     * configuration to use when restoring db components
     */
    private DatabaseConfig databaseConfig;

    /**
     * Backup is not fail fast. This stores the names of the components which failed
     */
    private final List<String> failedComponents = new ArrayList<String>();

    /**
     * When -migrate is supplied and the -newdb value is supplied, this will be true and allows migrate to create a new
     * db instead of only working with an existing db
     */
    private boolean canCreateNewDb;

    /**
     * If this is not null, it means that we need to update the target's node.properties. This may employ the
     * use of ompDatToMatchNodePropertyConfig
     */
    private PropertiesConfiguration nodePropertyConfig;

    /**
     * If we need to overwrite node.properties on the target, then this is the omp.dat which will be used to
     * overwrite omp.dat on the host. If node.properties is updated, then so must omp.dat in case it's changed
     * from the default obfuscated master password
     */
    private File ompDatToMatchNodePropertyConfig;

    /**
     * If true it means we might be restoring db components, so we need to validate db parameters and that we can
     * correctly configure databaseConfig
     */
    private boolean isDbComponent;

    /**
     * @param secureSpanHome The installation of the SSG e.g. /opt/SecureSpan/Gateway. Must be non null and exist
     * @param printStream where any verbose output will be sent. Can be null
     */
    public Importer(final File secureSpanHome, final PrintStream printStream) {
        if(secureSpanHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!secureSpanHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!secureSpanHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");

        final File testSsgHome = new File(secureSpanHome, ImportExportUtilities.GATEWAY);
        if(!testSsgHome.exists()) throw new IllegalArgumentException("Gateway installation not found");
        if(!testSsgHome.isDirectory()) throw new IllegalArgumentException("Gateway incorrectly installed");

        ssgHome = testSsgHome;
        this.secureSpanHome = secureSpanHome;

        //this class is not usable without an installed SSG >= 5.0
        final boolean checkVersion =
                SyspropUtil.getBoolean("com.l7tech.gateway.config.backuprestore.checkversion", true);
        if (checkVersion) ImportExportUtilities.throwIfLessThanFiveO(new File(ssgHome, "runtime/Gateway.jar"));

        this.printStream = printStream;
    }

    /**
     * Restore or migrate a backup image. See Importer.getRestoreUsage for expected parameters
     *
     * @param args program parameters
     * @return RestoreMigrateResult which contains the success / failure, any exceptions thrown and the list of failed
     *         components if applicable
     * @throws InvalidProgramArgumentException
     *          if any of the supplied parameters is invalid or is missing a required
     *          value
     */
    public ImportExportUtilities.UtilityResult restoreOrMigrateBackupImage(final String[] args)
            throws InvalidProgramArgumentException {

        //determine what we are doing - restore or migrate?
        //do this by validating the args with all possible options
        //once the mode is determined, then we can validate with more restricted arguments

        final List<CommandLineOption> validArgList = getRestoreOptionsWithDb();
        validArgList.addAll(getAllMigrateOptions());
        final Map<String, String> initialValidArgs =
                ImportExportUtilities.getAndValidateCommandLineOptions(
                        args, validArgList, ALL_IGNORED_OPTIONS, true, printStream);

        //are we doing a restore with migrate behaviour?
        isMigrate = initialValidArgs.get(MIGRATE.getName()) != null;

        if (isMigrate) {
            return performMigrate(args);
        } else {
            return performRestore(args);
        }
    }

    private ImportExportUtilities.UtilityResult performMigrate(final String [] args)
            throws InvalidProgramArgumentException {
        //All restore options are valid for migrate, apart from ftp options.
        //Remember that ssgmigrate.sh gets translated into ssgrestore.sh
        //with a migrate capability. Therefore here we are actually validating ssgrestore.sh parameters
        final List<CommandLineOption> validArgList = getRestoreOptionsWithDb();

        final String imageValue = ImportExportUtilities.getAndValidateSingleArgument(
                args, IMAGE_PATH, validArgList, ALL_IGNORED_OPTIONS);
        final File imageFile = getAndValidateImageExists(imageValue);

        programFlagsAndValues = ImportExportUtilities.getAndValidateCommandLineOptions(
                args, validArgList, ALL_IGNORED_OPTIONS, false, printStream);

        preBackupImageInitialization(programFlagsAndValues);

        isVerbose = programFlagsAndValues.containsKey(CommonCommandLineOptions.VERBOSE.getName());

        //migrate always requires all the db params (apart from the root db user and password)
        wereCompleteDbParamsSupplied(true);

        //what ever happens we need to delete any unzipped directory no matter what the outcome
        try {
            backupImage = new BackupImage(imageFile.getAbsolutePath(), printStream, isVerbose);

            //backup image is required before initialize is called, in case node.properties is required
            initialize(programFlagsAndValues);

            final boolean isDbComponent = programFlagsAndValues.containsKey(CommonCommandLineOptions.MAINDB_OPTION.getName())
                    || programFlagsAndValues.containsKey(CommonCommandLineOptions.AUDITS_OPTION.getName());

            if(!isDbComponent){
                //if the db component is not requested, then we must update node.properties
                //not writing to node.properties here, but creating the instnace variable nodePropertyConfig
                //this is a corner case specific to the existing 5.0 behaviour of migrate
                final String msg = "Reading migrate host's node.properties";
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                initializeNodePropsFromTarget(programFlagsAndValues);
            }

            //build list of components and filter appropriately
            performRestoreSteps();

        } catch (Restore.RestoreException e) {
            return new ImportExportUtilities.UtilityResult(FAILURE, null, e);
        } catch(Exception e){
            throw new RuntimeException(e);                        
        } finally {
            if (backupImage != null) backupImage.removeTempDirectory();
        }

        if(!failedComponents.isEmpty()){
            return new ImportExportUtilities.UtilityResult(PARTIAL_SUCCESS, failedComponents, null);
        }

        return new ImportExportUtilities.UtilityResult(SUCCESS);
    }

    private ImportExportUtilities.UtilityResult performRestore(final String [] args)
            throws InvalidProgramArgumentException {
        final List<CommandLineOption> validArgList = getRestoreOptionsWithDb();
        //make sure no unexpected arguments were supplied
        ImportExportUtilities.softArgumentValidation(args, validArgList, ALL_IGNORED_OPTIONS);

        //Cannot validate the command line arguments until the state of the backup image is known,
        //this is because of the database parameters.
        //We do not want to embark on a restore with the possibility that it will throw an exception due to an invalid
        //program parameter. Users would appreciate being told before anything is restored, that there is a problem
        //with the parameters and values they supplied

        //Get the image param
        final String imageValue = ImportExportUtilities.getAndValidateSingleArgument(
                args, IMAGE_PATH, validArgList, ALL_IGNORED_OPTIONS);

        //are we using ftp?
        //Can use any ftp param to validate if ftp is requested, this is further validated below, but first
        //we need to know if ftp has been specified
        final boolean ftpCheck = ImportExportUtilities.checkArgumentExistence(
                args, CommonCommandLineOptions.FTP_HOST.getName(), validArgList, ALL_IGNORED_OPTIONS);

        isVerbose = ImportExportUtilities.checkArgumentExistence(
                args, CommonCommandLineOptions.VERBOSE.getName(), validArgList, ALL_IGNORED_OPTIONS);

        programFlagsAndValues = ImportExportUtilities.getAndValidateCommandLineOptions(
                args, validArgList, ALL_IGNORED_OPTIONS, false, printStream);

        preBackupImageInitialization(programFlagsAndValues);
        //what ever happens we need to delete any unzipped directory no matter what the outcome
        try {
            validArgList.clear();
            if(ftpCheck){
                validArgList.addAll(getRestoreOptionsWithDb());
                //get the ftp config
                final FtpClientConfig ftpConfig = ImportExportUtilities.getFtpConfig(
                        args, validArgList, ALL_IGNORED_OPTIONS, imageValue);

                backupImage = new BackupImage(ftpConfig, imageValue, printStream, isVerbose);
            }else{
                final File imageFile = getAndValidateImageExists(imageValue);
                backupImage = new BackupImage(imageFile.getAbsolutePath(), printStream, isVerbose);
                validArgList.addAll(getRestoreOptionsWithDb());
            }

            initialize(programFlagsAndValues);

            //build list of components and filter appropriately
            performRestoreSteps();

        } catch (Restore.RestoreException e) {
            return new ImportExportUtilities.UtilityResult(FAILURE, null, e);
        } catch (Exception e){
            throw new RuntimeException(e);  
        } finally {
            if(backupImage != null) backupImage.removeTempDirectory();
        }

        if(!failedComponents.isEmpty()){
            return new ImportExportUtilities.UtilityResult(PARTIAL_SUCCESS, failedComponents, null);
        }

        return new ImportExportUtilities.UtilityResult(SUCCESS);
    }

    /**
     * Carry out all restore / migrate steps. Manages the -halt option
     * @throws Exception
     */
    private void performRestoreSteps() throws Restore.RestoreException {
        final String msg = "Performing " + ((isMigrate) ? "migrate" : "restore") + " ...";
        ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
        
        final String mappingFile = programFlagsAndValues.get(MAPPING_PATH.getName());
        final List<RestoreComponent> allComponents = getComponentsForRestore(mappingFile);
        for (RestoreComponent component : allComponents) {
            try {
                final ComponentResult result = component.doRestore();
                if(isSelectiveRestore && result.getResult() == ComponentResult.Result.NOT_APPLICABLE){
                    throw new Restore.RestoreException(result.getNotApplicableMessage());
                }else if (result.getResult() == ComponentResult.Result.NOT_APPLICABLE) {
                    final String msg1 = "Not applicable for this backup image";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, isVerbose, printStream);
                }
            } catch (Restore.RestoreException e) {
                final String msg1 =  "Could not restore component '" +
                        component.getComponentType().getComponentName() + "': " + e.getMessage();

                if (isHaltOnFirstFailure) {
                    ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg1, isVerbose, printStream);
                    final String haltMsg = "Halting restore as -halt option was supplied";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, haltMsg, isVerbose, printStream);
                    throw e;
                }
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg1, isVerbose, printStream);
                failedComponents.add(component.getComponentType().getComponentName());
            }
        }
    }

    private File getAndValidateImageExists(final String imagePath) throws InvalidProgramArgumentException {
        if(imagePath == null) throw new NullPointerException("imagePath cannot be null");
        final File imageFile = new File(imagePath);
        if(!imageFile.exists())
            throw new InvalidProgramArgumentException("'"+imageFile.getAbsolutePath()+"' could not be found");
        if(imageFile.isDirectory())
            throw new InvalidProgramArgumentException("'"+imageFile.getAbsolutePath()+"' is a directory");

        return imageFile;
    }

    /**
     * Call before the BackupImage is downloaded / unzipped. This ensures that required db params have been supplied
     * and the time spend downloading / unzipping is not wasted.
     * 
     * Sets isSelective and isDbComponent
     * Validates that if any db component is requested, that the user supplied the -dbu param
     * @param args map of command ilne args
     * @throws InvalidProgramArgumentException if any required db param is missing or invalid
     */
    private void preBackupImageInitialization(final Map<String, String> args) throws InvalidProgramArgumentException{
        isSelectiveRestore = ImportExportUtilities.isSelective(args);
        //ensure that we are not being asked to restore audits without the maindb
        final boolean auditsOnly = !args.containsKey(CommonCommandLineOptions.MAINDB_OPTION.getName())
                && args.containsKey(CommonCommandLineOptions.AUDITS_OPTION.getName());
        if(auditsOnly){
            throw new InvalidProgramArgumentException("Cannot restore -audits without -maindb");
        }

        isDbComponent = !isSelectiveRestore
                || args.containsKey(CommonCommandLineOptions.MAINDB_OPTION.getName())
                || args.containsKey(CommonCommandLineOptions.AUDITS_OPTION.getName());

        if(isDbComponent){
            final String adminDBUsername = programFlagsAndValues.get(DB_ADMIN_USER.getName());
            if (adminDBUsername == null) {
                throw new InvalidProgramArgumentException("Cannot restore the main database without" +
                        " the admin database user name. Please provide options: " + DB_ADMIN_USER.getName() +
                        " and optionally " + DB_ADMIN_PASSWD.getName());
            }
        }
    }

    private void initialize(final Map<String, String> args) throws InvalidProgramArgumentException, IOException,
            ConfigurationException {
        if(isDbComponent){
            initializeDatabaseConfiguration(args);
        }

        if(args.containsKey(CommonCommandLineOptions.HALT_ON_FIRST_FAILURE.getName())) isHaltOnFirstFailure = true;

        //do a quick check to see if the -esm was requrested
        final boolean esmRequested = args.containsKey("-"
                + ImportExportUtilities.ComponentType.ESM.getComponentName());
        //if the esm is requested, then it's required
        //check if it's running, lets not start any restore if the esm is running
        if(esmRequested){
            ImportExportUtilities.throwifEsmIsRunning();
            //validate the esm looks ok - just a basic check
            ImportExportUtilities.throwIfEsmNotPresent(
                    new File(secureSpanHome, ImportExportUtilities.ENTERPRISE_SERVICE_MANAGER));
        }

    }

    /**
     * Initialize all required database instance variables
     * After this method, the following are guaranteed to be set
     * canCreateNewDb - true or false
     * databaseConfig - configured correctly
     * suppliedClusterPassphrase - not null with a valid value
     * nodePropertyConfig - if not null, values which should be written to disk
     * ompDatToMatchNodePropertyConfig - set to the omp.dat file to use, might be needed when nodePropertyConfig
     * is not null, null when the local omp.dat does not need to be updated
     * @param args
     * @throws InvalidProgramArgumentException
     * @throws IOException
     */
    private void initializeDatabaseConfiguration(final Map<String, String> args)
            throws InvalidProgramArgumentException, IOException, ConfigurationException {
        final String msg1 = "Intitializing database connection properties...";
        ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg1, isVerbose, printStream);
        
        final String adminDBUsername = programFlagsAndValues.get(DB_ADMIN_USER.getName());
        if(adminDBUsername == null)
            throw new IllegalStateException("No admin database user supplied with -dbu option");
        
        final String tempAdminPassword = programFlagsAndValues.get(DB_ADMIN_PASSWD.getName());
        // its ok for password to be empty string
        final String adminDBPasswd = (tempAdminPassword != null)? tempAdminPassword: "";

        //check if the -newdb option was supplied
        canCreateNewDb = args.containsKey(CREATE_NEW_DB.getName());

        final File configFolder = backupImage.getConfigFolder();//may not exist or be applicable to image

        final boolean postFiveOImage = backupImage.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O;
        //See bug http://sarek.l7tech.com/bugzilla/show_bug.cgi?id=7469 for more info 
        //for rules regarding how and when we use node.properties and omp.dat from the backup image

        final boolean configCanBeUsed = !isSelectiveRestore ||
                args.containsKey(CommonCommandLineOptions.CONFIG_OPTION.getName());

        //does the image contain node.prop and omp.dat?
        final boolean nodePropExists = postFiveOImage && configFolder != null && !isMigrate
                && (new File(configFolder, ImportExportUtilities.NODE_PROPERTIES)).exists()
                && (new File(configFolder, ImportExportUtilities.NODE_PROPERTIES)).isFile()
                && configCanBeUsed;

        final boolean ompFileExists = postFiveOImage && configFolder!= null && !isMigrate
                && (new File(configFolder, ImportExportUtilities.OMP_DAT)).exists()
                && (new File(configFolder, ImportExportUtilities.OMP_DAT)).isFile()
                && configCanBeUsed;

        //this will throw if were doing a migrate and not all db params were supplied
        final boolean completeDb = wereCompleteDbParamsSupplied(isMigrate);

        if (!nodePropExists && !completeDb) {
            //this is a restore using node.properties and omp.dat from the host - never a migrate
            //no need to update node.properties as were not restoring it and no db command line params
            //were supplied
            if(isMigrate) throw new IllegalStateException("isMigrate is true");
            //we need to get our databse config and cluster passphrase from the node.properties on the target
            final File targetNodeConfDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);

            final File targetNodeProp = new File(targetNodeConfDir, ImportExportUtilities.NODE_PROPERTIES);
            final File targetOmpDat = new File(targetNodeConfDir, ImportExportUtilities.OMP_DAT);

            final String msg = "Database configuration retrieved from restore host's node.properties and omp.dat files";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

            //if node.properties and omp doesn't exist, then it will thrown an exception. We can't continue as
            //we dont have any db information from anywhere
            final Pair<DatabaseConfig, String> dbConfigPhrasePair =
                    getDatabaseConfig(targetNodeProp, targetOmpDat);
            databaseConfig = dbConfigPhrasePair.left;
            suppliedClusterPassphrase = dbConfigPhrasePair.right;
        } else if (nodePropExists && !completeDb) {
            //this is a restore using node.properties from the image - never a migrate
            if(isMigrate) throw new IllegalStateException("isMigrate is true");
            //the image contains node.properties and no overriding command line db params supplied
            final File ompFileToUse;
            if (!ompFileExists) {
                final String msg = "No omp.dat supplied in image, using default";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                final File targetNodeConfDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
                ompFileToUse = new File(targetNodeConfDir, ImportExportUtilities.OMP_DAT);
                if (!ompFileToUse.exists() || !ompFileToUse.isFile())
                    throw new IllegalStateException("omp.dat not found in ssg installation");
            } else {
                ompFileToUse = new File(configFolder, ImportExportUtilities.OMP_DAT);
            }
            final String msg = "Database configuration retrieved from backup image's config/node.properties file." +
                    " omp.dat retrieved from " + ((ompFileExists)?"backup image": "restore host");
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

            final File nodePropertiesFromImage = new File(configFolder, ImportExportUtilities.NODE_PROPERTIES);
            final Pair<DatabaseConfig, String> dbConfigPhrasePair =
                    getDatabaseConfig(nodePropertiesFromImage, ompFileToUse);
            databaseConfig = dbConfigPhrasePair.left;
            suppliedClusterPassphrase = dbConfigPhrasePair.right;
            //if were using omp.dat from the target, we won't want to overwrite it with itself later
            ompDatToMatchNodePropertyConfig = (ompFileExists)? ompFileToUse: null;
            nodePropertyConfig = getPropertyConfig(nodePropertiesFromImage);
        } else if(nodePropExists && completeDb){
            //we are going to update the node.properties from the image with the command line arguments
            if(isMigrate) throw new IllegalStateException("isMigrate is true");

            final File ompFileToUse;
            if (!ompFileExists) {
                final String msg = "No omp.dat supplied in image, using default";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                final File targetNodeConfDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
                ompFileToUse = new File(targetNodeConfDir, ImportExportUtilities.OMP_DAT);
                if (!ompFileToUse.exists() || !ompFileToUse.isFile())
                    throw new IllegalStateException("omp.dat not found in ssg installation");
            } else {
                ompFileToUse = new File(configFolder, ImportExportUtilities.OMP_DAT);
            }

            final String msg = "Database configuration retrieved from backup image's config/node.properties file." +
                    " omp.dat retrieved from " + ((ompFileExists)?"backup image": "restore host");
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);

            final File nodePropertiesFromImage = new File(configFolder, ImportExportUtilities.NODE_PROPERTIES);
            nodePropertyConfig = getPropertyConfig(nodePropertiesFromImage);
            //we need to merge in the command line arguments
            mergeCommandLineIntoProperties(args, nodePropertyConfig, ompFileToUse);

            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    "Merged command line db parameters into node.properties from backup image", isVerbose, printStream);

            databaseConfig = getDatabaseConfig(nodePropertyConfig, getMasterPasswordManagerForOmp(ompFileToUse));
            suppliedClusterPassphrase = programFlagsAndValues.get(CLUSTER_PASSPHRASE.getName());
            //if were using omp.dat from the target, we won't want to overwrite it with itself later
            ompDatToMatchNodePropertyConfig = (ompFileExists)? ompFileToUse: null;
        } else {
            //this is a migrate or a restore and no node.properties is included (could be a 5.0 image) but we've got all
            //command line args this will require the node.properties and omp.dat from the target but will override
            //certain db parameters

            final String taskVerb = (isMigrate) ? "migrate" : "restore";
            final String msg = "Database configuration being retrieved from " + taskVerb + " host's local " +
                    "node.properties and omp.dat files";
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
            initializeNodePropsFromTarget(args);
            
            databaseConfig = getDatabaseConfig(nodePropertyConfig, getMasterPasswordManagerForOmp(getOmpDatFromTarget()));
        }

        databaseConfig.setDatabaseAdminUsername(adminDBUsername);
        databaseConfig.setDatabaseAdminPassword(adminDBPasswd);
    }

    /**
     * This will initialize the following:-
     * suppliedClusterPassphrase
     * nodePropertyConfig
     *
     * Call this method when isMigrate is true, or a 5.0 image is being used and we are going to retrieve all the
     * required db parameters from the command line and will update / create the targets node.properties and will
     * use the targets omp.dat
     * @param args program parameters and values
     * @throws InvalidProgramArgumentException any required program param is missing
     */
    private void initializeNodePropsFromTarget(final Map<String, String> args)
            throws InvalidProgramArgumentException {
        suppliedClusterPassphrase = programFlagsAndValues.get(CLUSTER_PASSPHRASE.getName());
        nodePropertyConfig = getNodePropertiesFromTarget(args, suppliedClusterPassphrase);
        ompDatToMatchNodePropertyConfig = null;
        mergeCommandLineIntoProperties(args, nodePropertyConfig, getOmpDatFromTarget());

        final boolean performingAMerge = nodePropertiesExistsOnRestoreHost();
        //we may have merged with node.properties from the host or we may not have, find out if node.properties exists
        final String taskVerb = (isMigrate) ? "migrating" : "restoring";
        if(performingAMerge) {
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    "Merged command line db parameters into node.properties from " + taskVerb + " host", isVerbose,
                    printStream);
        }else{
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                    "Created a new node.properties with command line db parameters and the new node.id", isVerbose,
                    printStream);
        }
    }

    private boolean nodePropertiesExistsOnRestoreHost(){
        final File nodePropsFile = new File(ssgHome,
                ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.NODE_PROPERTIES);

        return nodePropsFile.exists() && nodePropsFile.isFile();
    }
    /**
     * Check that the complete set of database options were found
     * @param throwIfNotFound if true, throw an exception if not found or is the empty string
     * @return true if all required db params, apart from root db user, were supplied
     * @throws InvalidProgramArgumentException if a required param is missing and throwIfNotFound is true
     */
    private boolean wereCompleteDbParamsSupplied(final boolean throwIfNotFound) throws InvalidProgramArgumentException {

        final String taskVerb = (isMigrate) ? "migrating" : "restoring";

        final String dbHost = programFlagsAndValues.get(DB_HOST_NAME.getName());
        if (dbHost == null || dbHost.trim().isEmpty())
            if (throwIfNotFound) {
                throw new InvalidProgramArgumentException("missing option "
                        + DB_HOST_NAME.getName()
                        + ", required for " + taskVerb + " this image");
            } else {
                return false;
            }

        final String dbName = programFlagsAndValues.get(DB_NAME.getName());
        if (dbName == null || dbName.trim().isEmpty()) {
            //the dbname can come from -newdb instead
            final String newDb = programFlagsAndValues.get(CREATE_NEW_DB.getName());
            if (newDb == null || newDb.trim().isEmpty()) {
                if (throwIfNotFound) {
                    throw new InvalidProgramArgumentException("missing option "
                            + DB_NAME.getName()
                            + " or " + CREATE_NEW_DB.getName() + ", required for " + taskVerb + " this image");
                } else {
                    return false;
                }
            }
        }

        final String dbUser = programFlagsAndValues.get(GATEWAY_DB_USERNAME.getName());
        if (dbUser == null || dbUser.trim().isEmpty()) {
            if (throwIfNotFound) {
                throw new InvalidProgramArgumentException("missing option "
                        + GATEWAY_DB_USERNAME.getName()
                        + ", required for " + taskVerb + " this image");
            } else {
                return false;
            }
        }

        final String dbPass = programFlagsAndValues.get(GATEWAY_DB_PASSWORD.getName());
        if (dbPass == null || dbPass.trim().isEmpty())
            if (throwIfNotFound) {
                throw new InvalidProgramArgumentException("missing option "
                        + GATEWAY_DB_PASSWORD.getName()
                        + ", required for " + taskVerb + " this image");
            } else {
                return false;
            }

        final String clusterPassphrase = programFlagsAndValues.get(CLUSTER_PASSPHRASE.getName());
        if (clusterPassphrase == null || clusterPassphrase.trim().isEmpty())
            if (throwIfNotFound) {
                throw new InvalidProgramArgumentException("missing option "
                        + CLUSTER_PASSPHRASE.getName()
                        + ", required for " + taskVerb + " this image");
            } else {
                return false;
            }

        return true;
    }

    private PropertiesConfiguration getPropertyConfig(final File nodeProps){
        try {
            PropertiesConfiguration returnConfig = new PropertiesConfiguration();
            returnConfig.setAutoSave(false);
            returnConfig.setListDelimiter((char) 0);
            returnConfig.load(nodeProps);
            return returnConfig;
        } catch (ConfigurationException e) {
            throw new IllegalStateException("Cannot load file '" + nodeProps.getAbsolutePath()+"': " + e.getMessage());
        }
    }

    /**
     * canCreateNewDb should have been decided apon before this method is called
     * @return
     * @throws InvalidProgramArgumentException
     */
    private PropertiesConfiguration getNodePropertiesFromTarget(final Map<String, String> args,
                                                                final String clusterPassphrase)
            throws InvalidProgramArgumentException {
        //sanity check - invarient - we must have all db params if we are using node.properties from the host
        wereCompleteDbParamsSupplied(true);

        final MasterPasswordManager mpm = getMasterPasswordManagerForOmp(getOmpDatFromTarget());

        final File nodePropsFile = new File(ssgHome,
                ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.NODE_PROPERTIES);

        final String gwUser = args.get(GATEWAY_DB_USERNAME.getName());
        //password is from the command line, cannot be encrypted, if it is that's an input error
        final String gwPass = args.get(GATEWAY_DB_PASSWORD.getName());

        final Pair<String, String> hostPortPair =
                ImportExportUtilities.getDbHostAndPortPair(args.get(DB_HOST_NAME.getName()));
        final String dbHost = hostPortPair.left;
        final String dbPort = hostPortPair.right;

        final String dbName = (canCreateNewDb)? args.get(CREATE_NEW_DB.getName()): args.get(DB_NAME.getName());

        final PropertiesConfiguration returnConfig = new PropertiesConfiguration();
        returnConfig.setAutoSave(false);
        returnConfig.setListDelimiter((char) 0);

        final boolean nodePropExists = nodePropsFile.exists() && nodePropsFile.isFile();
        //to maintain the behaviour from 5.0, if node.properties doesn't exist on the host, we will create it
        
        if(nodePropExists){
            //if were creating a new db, then we can still use an existing node.id
            try {
                returnConfig.load(nodePropsFile);
                //note: we don't need the value from node.properties, but if it's there well check they match
                if (isMigrate && !canCreateNewDb){
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                            "node.properties file was found. Checking database name match", isVerbose, printStream);
                    final String nodePropDbName = returnConfig.getString("node.db.config.main.name");
                    if(nodePropDbName != null && !nodePropDbName.trim().isEmpty()){
                        if(!dbName.equals(nodePropDbName)){
                            throw new InvalidProgramArgumentException("Provided database name does not match with database " +
                                    "name in node.properties file. If you wish to create a new database, use the " +
                                    CREATE_NEW_DB.getName() + " option");
                        }
                    }
                }
                //valildity check on node.properties
                final String nodeId = returnConfig.getString("node.id");
                if(nodeId == null || nodeId.trim().isEmpty()){
                    throw new IllegalStateException("node.id does not exist in SSG node.properties. SSG is not" +
                            " correctly configured");
                }
            } catch (ConfigurationException e) {
                throw new IllegalStateException("Cannot read from node.properties");
            }

            final String nodeId = returnConfig.getString("node.id");
            if(nodeId == null || nodeId.trim().isEmpty()){
                throw new IllegalStateException("node.id does not exist in SSG node.properties. SSG is not" +
                        " correctly configured");
            }
        }else{
            //we are creating a new node.properties so generate a new node.id
            try {
                final String nodeId = NodeConfigurationManager.loadOrCreateNodeIdentifier("default",
                        new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, gwUser, gwPass), true);
                returnConfig.setProperty("node.id", nodeId );
                final String msg = ((isMigrate)?"Migrate":"Restore") + " host does not contain node.properties. " +
                        "Created a new node.id";
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
            } catch (IOException e) {
                throw new IllegalStateException("Could not generate a node.id: " + e.getMessage());
            }

        }

        returnConfig.setProperty("node.db.config.main.host", dbHost);
        returnConfig.setProperty("node.db.config.main.port", dbPort);
        returnConfig.setProperty("node.db.config.main.name", dbName);
        returnConfig.setProperty("node.db.config.main.user", gwUser);
        returnConfig.setProperty("node.db.config.main.pass", mpm.encryptPassword(gwPass.toCharArray()));
        returnConfig.setProperty("node.cluster.pass",
                mpm.encryptPassword(clusterPassphrase.toCharArray()));

        return returnConfig;
    }

    private MasterPasswordManager getMasterPasswordManagerForOmp(final File ompFile) throws InvalidProgramArgumentException {
        return new MasterPasswordManager( new DefaultMasterPasswordFinder(ompFile));
    }

    /**
     * Merge all db params applicable to node.properties into properties, using the ompFile for encryption
     * @param args command line arguments
     * @param properties the PropertiesConfiguration which will be written to the restore host
     * @throws InvalidProgramArgumentException, if any required db params are missing from args
     */
    private void mergeCommandLineIntoProperties(final Map<String, String> args,
                                                final PropertiesConfiguration properties,
                                                final File ompFile)
            throws InvalidProgramArgumentException {
        //sanity check - invarient - we must have all db params if we are using node.properties from the host
        wereCompleteDbParamsSupplied(true);

        final String gwUser = args.get(GATEWAY_DB_USERNAME.getName());
        //password is from the command line, cannot be encrypted, if it is that's an input error
        final String gwPass = args.get(GATEWAY_DB_PASSWORD.getName());

        final Pair<String, String> hostPortPair =
                ImportExportUtilities.getDbHostAndPortPair(args.get(DB_HOST_NAME.getName()));
        final String dbHost = hostPortPair.left;
        final String dbPort = hostPortPair.right;

        final String dbName = (canCreateNewDb)? args.get(CREATE_NEW_DB.getName()): args.get(DB_NAME.getName());

        final MasterPasswordManager mpm = new MasterPasswordManager( new DefaultMasterPasswordFinder(ompFile));
        final String clusterPassPhrase = args.get(CLUSTER_PASSPHRASE.getName());

        properties.setProperty("node.db.config.main.host", dbHost);
        properties.setProperty("node.db.config.main.port", dbPort);
        properties.setProperty("node.db.config.main.name", dbName);
        properties.setProperty("node.db.config.main.user", gwUser);
        properties.setProperty("node.db.config.main.pass", mpm.encryptPassword(gwPass.toCharArray()));
        properties.setProperty("node.cluster.pass",
                mpm.encryptPassword(clusterPassPhrase.toCharArray()));
    }

    /**
     * Get the omp.dat file from the restore host
     *
     * @return the omp.dat file from the host. Never null. Always exists. Exception if this cannot be achieved
     * @throws InvalidProgramArgumentException
     *
     */
    private File getOmpDatFromTarget() throws InvalidProgramArgumentException {
        //sanity check, we must have all db params if we are using node.properties from the host
        wereCompleteDbParamsSupplied(true);

        final File confDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
        final File ompFile = new File(confDir, ImportExportUtilities.OMP_DAT);
        if (!ompFile.exists() || !ompFile.isFile())
            throw new IllegalStateException("File '" + ompFile.getAbsolutePath() + "' not found");

        return ompFile;
    }

    private DatabaseConfig getDatabaseConfig(final PropertiesConfiguration propConfig,
                                             final MasterPasswordManager mpm) {

        final String dbHost = propConfig.getString("node.db.config.main.host");
        final String dbPort = propConfig.getString("node.db.config.main.port");
        final String dbName = propConfig.getString("node.db.config.main.name");
        final String gwUser = propConfig.getString("node.db.config.main.user");
        final String gwPass =
                new String(mpm.decryptPasswordIfEncrypted(propConfig.getString("node.db.config.main.pass")));

        return rewriteHostName(new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, gwUser, gwPass));
    }

    /**
     * Get a correctly configured DatabaseConfig from the files node.properties and omp.dat
     *
     * @param nodeProperties node.properties file. Must exist and be readable
     * @param ompFile        omp.dat. Must exist and be readable
     * @return a Pair of a correctly configured DatabaseConfig based on the supplied confiuration files with its
     *         associated cluster passphrase
     * @throws java.io.IOException if problem reading the files
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException
     *                             if the suplied files contain invalid data / settings
     */
    private Pair<DatabaseConfig, String> getDatabaseConfig(final File nodeProperties, final File ompFile)
            throws IOException, InvalidProgramArgumentException {
        if (nodeProperties == null) throw new NullPointerException("nodeProperties cannot be null");
        if (!nodeProperties.exists())
            throw new InvalidProgramArgumentException("File '" + nodeProperties.getAbsolutePath() + "' does not exist");
        if (nodeProperties.isDirectory())
            throw new InvalidProgramArgumentException("File '" + nodeProperties.getAbsolutePath() + "' is not a regular file");

        if (ompFile == null) throw new NullPointerException("ompFile cannot be null");
        if (!ompFile.exists())
            throw new InvalidProgramArgumentException("File '" + ompFile.getAbsolutePath() + "' does not exist");
        if (ompFile.isDirectory())
            throw new InvalidProgramArgumentException("File '" + ompFile.getAbsolutePath() + "' is not a regular file");

        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodeProperties, true);
        final DatabaseConfig config = nodeConfig.getDatabase(DatabaseType.NODE_ALL,
                NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER);
        if (config == null) {
            throw new InvalidProgramArgumentException("DatabaseConfig could not be configured with node.properties and omp.dat");
        }

        final MasterPasswordManager decryptor =
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());

        config.setNodePassword(new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())));

        String clusterPassPhrase = new String(decryptor.decryptPasswordIfEncrypted(nodeConfig.getClusterPassphrase()));

        return new Pair<DatabaseConfig, String>(rewriteHostName(config), clusterPassPhrase);
    }

    /**
     * Check if the hostname contained in the DatabaseConfig config represents 'localhost'. If it does update the
     * host name string to be 'localhost'
     * This is to allow root connections to the mysql instance, which will reject them even if they are from localhost
     * if the connection string does not specify localhost for the hostname
     * @param config DatabaseConfig to potentially update
     * @return config updated
     */
    private DatabaseConfig rewriteHostName(final DatabaseConfig config){

        final String hostName = config.getHost();
        if(ImportExportUtilities.isHostLocal(hostName)){
            config.setHost("localhost");
        }

        return config;
    }

    /**
     * All components configured are candidates to be returned. If the user supplied any explicit components, then
     * the returned list is filtered to only include components the user requested. Some components are not selective
     * : the node identity task, which ensures that the restore hosts node.properties and omp.dat file are correctly
     * updated.
     *
     * The list of RestoreComponent s returned may not all be applicable. For example, the database component may
     * return NOT_APPLICABLE as it's Result, if the db is remote, or the OS may do the same if the Appliance is
     * not installed. This method is not concerned with ensuring that all the components returned are applicable.
     *
     * There is no additional filtering of what components are returned when isMigrate is true. Migrate is a capability
     * added to the restore of certain components - db, os and config files.
     * ssgmigrate.sh ensures that the Importer class is provided with the correct command line arguments, and it
     * should always produce a selective restore, in which cases components like ca, ma won't be included, but they
     * can be part of a selective restore with migrate capabilities if desired
     *
     * @param mappingFile can be null 
     * @return list of applicable RestoreComponent's. Filtered for selective restore if applicable
     * @throws RestoreImpl.RestoreException
     */
    private List<RestoreComponent> getComponentsForRestore(final String mappingFile)
            throws Restore.RestoreException {

        final Restore restore = BackupRestoreFactory.getRestoreInstance(this.secureSpanHome,
                this.backupImage,
                databaseConfig,
                suppliedClusterPassphrase,
                this.isVerbose,
                this.printStream);


        final List<RestoreComponent> componentList = new ArrayList<RestoreComponent>();

        final String taskVerb = (isMigrate) ? "Migrating" : "Restoring";

        componentList.add(new RestoreComponent(){
            public ComponentResult doRestore() throws Restore.RestoreException {
                final String msg = taskVerb + " component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                //if no db component is being restored, then we need to allow the config component to restore
                //the node identity if found - node.properties and omp.dat
                //if the db is part of the restore, then the restoreNodeIdentity task will ensure that node.properties
                //and omp.dat are correctly written / updated on the restore host
                return restore.restoreComponentConfig(isMigrate, isDbComponent);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CONFIG;
            }
        });

        componentList.add(new RestoreComponent(){
            public ComponentResult doRestore() throws Restore.RestoreException {
                final String msg = taskVerb + " component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                return restore.restoreComponentMainDb(isMigrate, canCreateNewDb, mappingFile);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MAINDB;
            }
        });

        componentList.add(new RestoreComponent(){
            public ComponentResult doRestore() throws Restore.RestoreException {
                final String msg = taskVerb + " component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                return restore.restoreComponentAudits(isMigrate);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.AUDITS;
            }
        });

        componentList.add(new RestoreComponent(){
            public ComponentResult doRestore() throws Restore.RestoreException {
                final String msg = taskVerb + " component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                return restore.restoreComponentOS();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.OS;
            }
        });

        componentList.add(new RestoreComponent(){
            public ComponentResult doRestore() throws Restore.RestoreException {
                final String msg = taskVerb + " component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                return restore.restoreComponentCA();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CA;
            }
        });

        componentList.add(new RestoreComponent(){
            public ComponentResult doRestore() throws Restore.RestoreException {
                final String msg = taskVerb + " component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                return restore.restoreComponentMA();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MA;
            }
        });

        //Check for -esm, we don't add this by default, only when it's explicitly asked for
        if(programFlagsAndValues.containsKey(CommonCommandLineOptions.ESM_OPTION.getName())){
            componentList.add(new RestoreComponent(){
                public ComponentResult doRestore() throws Restore.RestoreException {
                    final String msg = taskVerb + " component " + getComponentType().getComponentName();
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    return restore.restoreComponentESM();
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.ESM;
                }
            });
        }

        final List<RestoreComponent> returnList;
        //feedback to user
        if(isSelectiveRestore){
            if(!isMigrate){
                //don't tell the user this if they are doing a migrate
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, "Performing a selective restore",
                        isVerbose, printStream);
            }
            returnList =
                    ImportExportUtilities.filterComponents(componentList, programFlagsAndValues);
        }else{
            returnList = componentList;
        }

        //any task added after here will not be filtered
        //restore the node identity when we know it has been ignored by config restore
        if(isDbComponent){
            returnList.add(new RestoreComponent(){
                public ComponentResult doRestore() throws Restore.RestoreException {
                    final String msg = taskVerb + " component " + getComponentType().getComponentName();
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                    //both of these can be null or ompDatToMatchNodePropertyConfig can be null
                    return restore.restoreNodeIdentity(isMigrate, nodePropertyConfig, ompDatToMatchNodePropertyConfig);
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.NODE_IDENTITY;
                }
            });
        }

        return returnList;
    }

    public String getDatabaseConfigCommandLineOptions(){
        final List<CommandLineOption> cmdDbOptions = Arrays.asList(DB_HOST_NAME, DB_NAME, CLUSTER_PASSPHRASE,
                GATEWAY_DB_USERNAME, GATEWAY_DB_PASSWORD);
        final StringBuilder output = new StringBuilder();
        final int largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(cmdDbOptions);
        for (CommandLineOption option : cmdDbOptions) {
         output.append("\t")
                 .append(option.getName())
                 .append(ImportExportUtilities.createSpace(largestNameStringSize-option.getName().length() + 1))
                 .append(option.getDescription())
                 .append(BackupRestoreLauncher.EOL_CHAR);
        }
        output.append(BackupRestoreLauncher.EOL_CHAR);

        return output.toString();
    }

    /**
     * Get the usage for the Restore. Usage information is written to the StringBuilder output
     * @param output StringBuilder to write the usage information to
     */
    static void getRestoreUsage(final StringBuilder output) {
        final List<CommandLineOption> restoreArgList = getStandardRestoreOptions();
        final int largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(restoreArgList);
        final List<CommandLineOption> prependOptions = new ArrayList<CommandLineOption>();
        prependOptions.addAll(CommonCommandLineOptions.ALL_COMPONENTS);
        prependOptions.add(CommonCommandLineOptions.ESM_OPTION);
        for (CommandLineOption option : restoreArgList) {
            final String description = (prependOptions.contains(option))?
                    "restore " + option.getDescription(): option.getDescription();
            output.append("\t")
                    .append(option.getName())
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.getName().length() + 1))
                    .append(description )
                    .append(BackupRestoreLauncher.EOL_CHAR);
        }
        output.append(BackupRestoreLauncher.EOL_CHAR);
    }

    /**
     * Get the usage for the traditional migrate. Usage information is written to the StringBuilder output
     *
     * This is used to show what the valid arguments to ssgmigrate.sh are
     * @param output StringBuilder to write the usage information to
     */
    static void getMigrateUsage(final StringBuilder output) {
        final List<CommandLineOption> migrateArgList = getAllMigrateOptions();
        final int largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(migrateArgList);
        for (CommandLineOption option : migrateArgList) {
            output.append("\t")
                    .append(option.getName())
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.getName().length() + 1))
                    .append(option.getDescription())
                    .append(BackupRestoreLauncher.EOL_CHAR);
        }
        output.append(BackupRestoreLauncher.EOL_CHAR);
    }

    private static List<CommandLineOption> getStandardRestoreOptions(){
        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(ALL_RESTORE_OPTIONS);
        validArgList.addAll(CommonCommandLineOptions.ALL_FTP_OPTIONS);
        validArgList.addAll(CommonCommandLineOptions.ALL_COMPONENTS);
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ESM_OPTION));
        return validArgList;
    }

    private static List<CommandLineOption> getRestoreOptionsWithDb(){
        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(ALL_RESTORE_OPTIONS);
        validArgList.addAll(CommonCommandLineOptions.ALL_FTP_OPTIONS);
        validArgList.addAll(COMMAND_LINE_DB_ARGS);
        validArgList.addAll(CommonCommandLineOptions.ALL_COMPONENTS);
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ESM_OPTION));
        validArgList.addAll(MIGRATE_OPTIONS);
        return validArgList;
    }

    /**
     * This is only for the getMigrateUsage() method. Remember, when we are validating what's been passed into
     * the Importer, we are validating ssgrestore.sh's parameters. ssgmigrate.sh's parameters will have been
     * translated by BackupRestoreLauncher
     * @return
     */
    private static List<CommandLineOption> getAllMigrateOptions(){
        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(ALL_MIGRATE_OPTIONS);
        return validArgList;
    }
}