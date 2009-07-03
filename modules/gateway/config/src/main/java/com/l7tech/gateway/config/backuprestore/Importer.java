package com.l7tech.gateway.config.backuprestore;

import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.FatalException;
import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
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
 * Importer is responsible for accecpting parameters, validating them then calling the appropriate methods of
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
            "location of image file to import",
            true, false);
    static final CommandLineOption MAPPING_PATH = new CommandLineOption("-mapping",
            "location of the mapping template file",
            true, false);
    static final CommandLineOption DB_HOST_NAME = new CommandLineOption("-dbh", "database host name");
    static final CommandLineOption DB_NAME = new CommandLineOption("-db", "database name");
    static final CommandLineOption DB_ROOT_PASSWD = new CommandLineOption("-dbp", "database root password");
    static final CommandLineOption DB_ROOT_USER = new CommandLineOption("-dbu", "database root username");
    static final CommandLineOption OS_OVERWRITE =
            new CommandLineOption("-"+ ImportExportUtilities.ComponentType.OS.getComponentName(),
            "overwrite os level config files",
            false, true);
    static final CommandLineOption CREATE_NEW_DB = new CommandLineOption("-newdb" ,"create new database");
    static final CommandLineOption MIGRATE = new CommandLineOption("-migrate", "migrate from environment to environment using the file exclusion and table exclusion configuration files", false, true);

    static final CommandLineOption CONFIG_ONLY =
            new CommandLineOption("-"+ ImportExportUtilities.ComponentType.CONFIG.getComponentName(),
                    "only restore configuration files, no database restore", false, true);
    static final CommandLineOption CLUSTER_PASSPHRASE = new CommandLineOption("-cp", "the cluster passphrase for the (resulting) database");
    static final CommandLineOption GATEWAY_DB_USERNAME = new CommandLineOption("-gdbu", "gateway database username");
    static final CommandLineOption GATEWAY_DB_PASSWORD = new CommandLineOption("-gdbp", "gateway database password");

    static final CommandLineOption[] ALL_MIGRATE_OPTIONS = {IMAGE_PATH, MAPPING_PATH, DB_ROOT_USER, DB_ROOT_PASSWD,
            DB_HOST_NAME, DB_NAME, CONFIG_ONLY, OS_OVERWRITE, CLUSTER_PASSPHRASE, GATEWAY_DB_USERNAME, GATEWAY_DB_PASSWORD,
            CREATE_NEW_DB, MIGRATE};

    static final CommandLineOption[] ALL_RESTORE_OPTIONS = {IMAGE_PATH, DB_ROOT_USER, DB_ROOT_PASSWD,
            CommonCommandLineOptions.HALT_ON_FIRST_FAILURE, CommonCommandLineOptions.VERBOSE, MIGRATE};

    static final CommandLineOption [] COMMAND_LINE_DB_ARGS = {DB_HOST_NAME, DB_NAME, CLUSTER_PASSPHRASE,
                GATEWAY_DB_USERNAME, GATEWAY_DB_PASSWORD};

    static final CommandLineOption[] ALL_IGNORED_OPTIONS = {
            new CommandLineOption("-p", "Ignored parameter for partition", true, false),
            new CommandLineOption("-mode", "Ignored parameter for mode type", true, false) };

    private String adminDBUsername;
    private String adminDBPasswd;
    private String suppliedClusterPassphrase;

    private final File ssgHome;

    /**
     * Base folder of all Securespan products
     */
    private final File secureSpanHome;
    

    /**
     * Only to be used when code path starts at restoreOrMigrateBackupImage. Do not access from public methods
     */
    private final PrintStream printStream;

    private Map<String, String> programFlagsAndValues;
    private boolean isMigrate;
    /**
     * Cannot be used by any public method. Only to be used when code path originates in performMigrateOrRestore..
     * isVerbose only applies when command line arguments are passed in
     */
    private boolean isVerbose;

    private boolean isSelectiveRestore;

    /**
     * When true, the restore is fail fast
     */
    private boolean isHaltOnFirstFailure;

    private BackupImage backupImage;
    /**
     * This field should only be used by the performRestore method. All other methods should accept a DatabaseConfig
     * object and use it. The public restoreXXX() methods may accept a DatabseConfig, and when it does this should
     * be used and not the instance variable
     */
    private DatabaseConfig databaseConfig; 

    /**
     * Backup is not fail fast. As a result we will allow clients access to this list of error message so that they
     * can be printed to the screen, when in -v mode
     */
    private final List<String> failedComponents = new ArrayList<String>();

    /**
     * Maybe set to true when migrate is ran, if the -newdb option is supplied
     */
    private boolean canCreateNewDb;

    /**
     * Can be set to true by the validate methods, to indiciate that we received the database configuration
     * information on the command line. If this is the case, then this is our flag to indiciate to the Restore
     * instance, that node.properties should be written to disk with the new values after a successful
     * database restore
     */
    private boolean updateNodeProperties;

    private PropertiesConfiguration nodePropertyConfig;

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
     * Restore or migrate a backup image. See getImporterUsage() for expected parameters
     *
     * @param args
     * @return
     * @throws InvalidProgramArgumentException
     * @throws BackupRestoreLauncher.FatalException
     * @throws IOException
     * @throws BackupImage.InvalidBackupImage
     */
    public RestoreMigrateResult restoreOrMigrateBackupImage(final String [] args)
            throws InvalidProgramArgumentException,
            BackupRestoreLauncher.FatalException,
            IOException,
            BackupImage.InvalidBackupImage, BackupImage.BackupImageException {

        //determine what we are doing - restore or migrate?
        //do this by validating the args with all possible options
        //once the mode is determined, then we can validate with more restricted arguments

        final List<CommandLineOption> validArgList = getRestoreOptionsWithDb();
        validArgList.addAll(getAllMigrateOptions());
        final Map<String, String> initialValidArgs =
                ImportExportUtilities.getAndValidateCommandLineOptions(args,
                        validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        //is migrate being used?
        isMigrate = initialValidArgs.get(MIGRATE.getName()) != null;

        if(isMigrate){
            return performMigrate(args);
        }else{
            return performRestore(args);
        }
    }

    private RestoreMigrateResult performMigrate(final String [] args) throws InvalidProgramArgumentException,
            IOException, BackupImage.InvalidBackupImage {
        //All restore options are valid for migrate. Remember that ssgmigrate.sh gets tranlsated into ssgrestore.sh
        //with a migrate capability. Therefore here we are actually validating ssgrestore.sh parameters
        final List<CommandLineOption> validArgList = getRestoreOptionsWithDb();

        final String imageValue = ImportExportUtilities.getAndValidateSingleArgument(args,
                IMAGE_PATH, validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));
        final File imageFile = getAndValidateImageExists(imageValue);
        
        backupImage = new BackupImage(imageFile.getAbsolutePath(), printStream, isVerbose);

        programFlagsAndValues = ImportExportUtilities.getAndValidateCommandLineOptions(args,
                validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        isVerbose = programFlagsAndValues.containsKey(CommonCommandLineOptions.VERBOSE.getName());

        //backup image is required validateRestoreProgramParameters is called
        validateMigrateProgramParameters(programFlagsAndValues);

        //what ever happens we need to delete any unzipped directory no matter what the outcome
        try {
            //build list of components and filter appropriately
            performRestoreSteps();

        } catch (Exception e) {
            return new RestoreMigrateResult(false, RestoreMigrateResult.Status.FAILURE, null, e);
        } finally {
            backupImage.removeTempDirectory();
        }

        if(!failedComponents.isEmpty()){
            return new RestoreMigrateResult(false, RestoreMigrateResult.Status.PARTIAL_SUCCESS, failedComponents, null);
        }

        return new RestoreMigrateResult(false, RestoreMigrateResult.Status.SUCCESS);
    }

    private RestoreMigrateResult performRestore(final String [] args) throws InvalidProgramArgumentException,
            IOException, FatalException, BackupImage.InvalidBackupImage, BackupImage.BackupImageException {
        final List<CommandLineOption> validArgList = getRestoreOptionsWithDb();
        //make sure no unexpected arguments were supplied
        ImportExportUtilities.softArgumentValidation(args, validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        //Cannot valide the command line arguments until the state of the backup image is known,
        //this is mainly a database issue, but we want to provide an api to these methods, so after
        //the Importer is initialized, it should be possible to call any of the restore***() methods
        //as doing this as you could be half way through a backup and then get an invalid program argument
        //clients would not appreciate this, and would appreciate being told, before they perform any restore,
        //that there is a problem with their command line arguments
        //Get the image param

        final String imageValue = ImportExportUtilities.getAndValidateSingleArgument(args,
                IMAGE_PATH, validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        //are we using ftp?
        //Can use any ftp param to validate if ftp is requested, this is further validated below, but first
        //we need to know if ftp has been specified
        final boolean ftpCheck = ImportExportUtilities.checkArgumentExistence(args,
                CommonCommandLineOptions.FTP_HOST.getName(), validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        isVerbose = ImportExportUtilities.checkArgumentExistence(args,
                CommonCommandLineOptions.VERBOSE.getName(), validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        validArgList.clear();
        if(ftpCheck){
            validArgList.addAll(getStandardRestoreOptions());
            //get the ftp config
            final FtpClientConfig ftpConfig = ImportExportUtilities.getFtpConfig(args,
                    validArgList,
                    Arrays.asList(ALL_IGNORED_OPTIONS),
                    imageValue);
            
            backupImage = new BackupImage(ftpConfig, imageValue, printStream, isVerbose);
        }else{
            final File imageFile = getAndValidateImageExists(imageValue);
            backupImage = new BackupImage(imageFile.getAbsolutePath(), printStream, isVerbose);
            validArgList.addAll(getRestoreOptionsWithDb());
        }

        programFlagsAndValues = ImportExportUtilities.getAndValidateCommandLineOptions(args,
                validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        validateRestoreProgramParameters(programFlagsAndValues);

        //what ever happens we need to delete any unzipped directory no matter what the outcome
        try {
            //build list of components and filter appropriately
            performRestoreSteps();

        } catch (Exception e) {
            return new RestoreMigrateResult(false, RestoreMigrateResult.Status.FAILURE, null, e);
        } finally {
            backupImage.removeTempDirectory();
        }

        if(!failedComponents.isEmpty()){
            return new RestoreMigrateResult(false, RestoreMigrateResult.Status.PARTIAL_SUCCESS, failedComponents, null);            
        }

        return new RestoreMigrateResult(false, RestoreMigrateResult.Status.SUCCESS);
    }

    /**
     * Carry out all restore / migrate steps. Manages the -halt option
     * @throws Exception
     */
    private void performRestoreSteps() throws Exception {
        final String mappingFile = programFlagsAndValues.get(MAPPING_PATH.getName());
        final List<RestoreComponent<? extends Exception>> allComponents = getComponentsForRestore(mappingFile);
        for(RestoreComponent<? extends Exception> component: allComponents){
            try{
                component.doRestore();
            }catch (Exception e) {
                if (isHaltOnFirstFailure) {
                    logger.log(Level.SEVERE, "Could not restore component " +
                            component.getComponentType().getDescription() + " due to exception: " + e.getMessage());
                    logger.log(Level.SEVERE, "Halting restore as -halt option was supplied");
                    throw e;
                }
                logger.log(Level.WARNING, "Could not restore component " +
                        component.getComponentType().getDescription()+ " due to exception: " + e.getMessage());
                failedComponents.add(component.getComponentType().getDescription());
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

    private void validateCommonProgramParameters(final Map<String, String> args) throws InvalidProgramArgumentException {
        isSelectiveRestore = ImportExportUtilities.isSelective(args);
        
        final boolean isDbComponent = !isSelectiveRestore
                || args.containsKey(CommonCommandLineOptions.MAINDB_OPTION.getName())
                || args.containsKey(CommonCommandLineOptions.AUDITS_OPTION.getName());
        
        if(isDbComponent){
            adminDBUsername = programFlagsAndValues.get(DB_ROOT_USER.getName());
            adminDBPasswd = programFlagsAndValues.get(DB_ROOT_PASSWD.getName());
            if (adminDBUsername == null) {
                throw new BackupRestoreLauncher.InvalidProgramArgumentException("Cannot restore the main database without" +
                        " the root database user name and password. Please provide options: " + DB_ROOT_USER.getName() +
                        " and " + DB_ROOT_PASSWD.getName());
            }
            if (adminDBPasswd == null) adminDBPasswd = ""; // totally legit
        }

        if(args.containsKey(CommonCommandLineOptions.HALT_ON_FIRST_FAILURE.getName())) isHaltOnFirstFailure = true;

        //do a quick check to see if the -esm was requrested
        final boolean esmRequested = args.containsKey("-"
                + ImportExportUtilities.ComponentType.ESM.getComponentName());
        //if the esm is requested, then it's required
        //check if it's running, lets not start any backup if the esm is running
        if(esmRequested){
            ImportExportUtilities.throwifEsmIsRunning();
            //validate the esm looks ok - just a basic check
            ImportExportUtilities.throwIfEsmNotPresent(
                    new File(secureSpanHome, ImportExportUtilities.ENTERPRISE_SERVICE_MANAGER));
        }
        
    }

    /**
     * Check that the complete set of database options were found
     * @param throwIfNotFound if true, throw an exception if not found or is the empty string
     * @return
     * @throws InvalidProgramArgumentException
     */
    private boolean wereCompleteDbParamsSupplied(final boolean throwIfNotFound) throws InvalidProgramArgumentException {

        final String dbHost = programFlagsAndValues.get(DB_HOST_NAME.getName());
        if(dbHost == null || dbHost.isEmpty())
            if(throwIfNotFound){
                throw new InvalidProgramArgumentException("missing option "
                    + DB_HOST_NAME.getName()
                    + ", required for exporting this image");
            }else{ return false; }

        final String dbName = programFlagsAndValues.get(DB_NAME.getName());
        if(dbName == null || dbName.isEmpty()) {
            //the dbname can come from -newdb instead
            final String newDb = programFlagsAndValues.get(CREATE_NEW_DB.getName());
            if(newDb == null || newDb.isEmpty()){
                if(throwIfNotFound){
                    throw new InvalidProgramArgumentException("missing option "
                            + DB_NAME.getName()
                            + " or "+CREATE_NEW_DB.getName()+", required for exporting this image");
                }else {return false;}
            }
        }

        final String dbUser = programFlagsAndValues.get(GATEWAY_DB_USERNAME.getName());
        if(dbUser == null || dbUser.isEmpty()){
            if(throwIfNotFound){
                throw new InvalidProgramArgumentException("missing option "
                        + GATEWAY_DB_USERNAME.getName()
                        + ", required for exporting this image");
            }else{ return false;}
        }

        final String dbPass = programFlagsAndValues.get(GATEWAY_DB_PASSWORD.getName());
        if(dbPass == null || dbPass.isEmpty())
            if(throwIfNotFound){
                throw new InvalidProgramArgumentException("missing option "
                        + GATEWAY_DB_PASSWORD.getName()
                        + ", required for exporting this image");
            }else{ return false; }

        final String clusterPassphrase = programFlagsAndValues.get(CLUSTER_PASSPHRASE.getName());
        if(clusterPassphrase == null || clusterPassphrase.isEmpty())
            if(throwIfNotFound){
                throw new InvalidProgramArgumentException("missing option "
                        + CLUSTER_PASSPHRASE.getName()
                        + ", required for exporting this image");
            }else{ return false; }

        return true;
    }

    /**
     * Validate all program arguments. This method will validate all required params are met, and that any which expect
     * a value recieve it.
     * This also validtes the database settings and database connection. The rational for this is that we don't want
     * a restore to continue and then fail on the db due to incorrect values presented. If the database is requested
     * then we will ensure before any thing is restored, that everything we need to restore it is present
     *
     * Sets databaseConfig
     * 
     * @param args The name value pair map of each argument to it's value, if a vaule exists
     * @throws IOException for arguments which are files, they are checked to see if the exist, which may cause an IOException
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException if any program params are invalid
     */
    private void validateRestoreProgramParameters(final Map<String, String> args)
            throws InvalidProgramArgumentException, BackupImage.InvalidBackupImage, IOException {
        validateCommonProgramParameters(args);


        //do we need to do a database backup?
        boolean isDbRequested = true;//by default
        if(isSelectiveRestore){
            //is it included
            isDbRequested = args.containsKey(CommonCommandLineOptions.MAINDB_OPTION.getName());
        }

        if(isDbRequested){
            final File configFolder = backupImage.getConfigFolder();

            if(backupImage.getImageVersion() == BackupImage.ImageVersion.AFTER_FIVE_O && configFolder != null){

                //Policy is 1) Check for command line args - if found - use
                //2) Use node.properties - if not found error, cannot restore image - remember at this point
                //the user has defaulted to allowing a db restore or has specified it explicitly
                //the user can always do a selective restore and avoid the db if needed
                final boolean completeDb = wereCompleteDbParamsSupplied(false);

                //we need node.properties and omp.dat in the image - always
                final File nodeProperties = new File(configFolder, ImportExportUtilities.NODE_PROPERTIES);
                if(!nodeProperties.exists())
                    throw new BackupImage.InvalidBackupImage("File '"+
                            nodeProperties.getAbsolutePath()+"' does not exist");
                if(nodeProperties.isDirectory())
                    throw new BackupImage.InvalidBackupImage("File '"+
                            nodeProperties.getAbsolutePath()+"' is not a regular file");

                final File ompFile = new File(configFolder, ImportExportUtilities.OMP_DAT);
                if(!ompFile.exists())
                    throw new BackupImage.InvalidBackupImage("File '"+ompFile.getAbsolutePath()+"' does not exist");
                if(ompFile.isDirectory())
                    throw new BackupImage.InvalidBackupImage("File '"+
                            ompFile.getAbsolutePath()+"' is not a regular file");

                try {
                    nodePropertyConfig = new PropertiesConfiguration();
                    nodePropertyConfig.setAutoSave(false);
                    nodePropertyConfig.setListDelimiter((char) 0);
                    nodePropertyConfig.load(nodeProperties);
                } catch (ConfigurationException e) {
                    throw new IllegalStateException("Cannot load node.properties from backup image: " + e.getMessage());
                }
                
                if(!completeDb){
                    //need to validate the contents of node.properties and omp.dat
                    final Pair<DatabaseConfig, String> dbConfigPhrasePair = getDatabaseConfig(nodeProperties, ompFile);
                    databaseConfig = dbConfigPhrasePair.left;
                    suppliedClusterPassphrase = dbConfigPhrasePair.right;
                }else{
                    updateNodeProperties = true;
                    //we must have received all db params on the command line
                    final Pair<PropertiesConfiguration, MasterPasswordManager> pair = getNodePropertiesConfig(args);
                    nodePropertyConfig = pair.left;
                    databaseConfig = getDatabaseConfig(nodePropertyConfig, pair.right);
                }
            }else{
                //5.0 image - WE MUST HAVE A node.properties AND omp.dat, from the system where it is being
                //restored onto - this is identical to the case when migrate is being used
                updateNodeProperties = true;
                //we must have received all db params on the command line
                final Pair<PropertiesConfiguration, MasterPasswordManager> pair = getNodePropertiesConfig(args);
                nodePropertyConfig = pair.left;
                databaseConfig = getDatabaseConfig(nodePropertyConfig, pair.right);
            }
            setDbAdminInfo();
        }
    }


    /**
     * Only call when the use has supplied all required db parameters
     * If the host has node.properties then it will be read. Omp.dat must exist
     * This is when isMigrate is true, its a 5.0 image or no config folder was found
     * @return PropertiesConfiguration, in a state where it can be saved and be correct
     */
    private final Pair<PropertiesConfiguration, MasterPasswordManager> getNodePropertiesConfig(final Map<String, String> args)
            throws InvalidProgramArgumentException {
        wereCompleteDbParamsSupplied(true);

        final File confDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
        final File ompFile = new File(confDir, ImportExportUtilities.OMP_DAT);
        if(!ompFile.exists() || !ompFile.isFile())
            throw new IllegalStateException("File '"+ompFile.getAbsolutePath()+"' not found");

        final MasterPasswordManager mpm = new MasterPasswordManager(
                new DefaultMasterPasswordFinder(ompFile));

        final File nodePropsFile = new File(ssgHome,
                ImportExportUtilities.NODE_CONF_DIR + File.separator + ImportExportUtilities.NODE_PROPERTIES);

        final boolean nodePropExists = nodePropsFile.exists() && nodePropsFile.isFile();

        final String gwUser = args.get(GATEWAY_DB_USERNAME.getName());
        //password is from the command line, cannot be encrypted, if it is that's an input error
        final String gwPass = args.get(GATEWAY_DB_PASSWORD.getName());

        final Pair<String, String> hostPortPair =
                ImportExportUtilities.getDbHostAndPortPair(args.get(DB_HOST_NAME.getName()));
        final String dbHost = hostPortPair.left;
        final String dbPort = hostPortPair.right;

        //check if the -newdb option was supplied
        final boolean newDbRequested = args.containsKey(CREATE_NEW_DB.getName());
        //db name can come from either the newdb or the dbname.
        final String dbName = (newDbRequested)? args.get(CREATE_NEW_DB.getName()): args.get(DB_NAME.getName());

        final PropertiesConfiguration returnConfig = new PropertiesConfiguration();
        returnConfig.setAutoSave(false);
        returnConfig.setListDelimiter((char) 0);

        //if were creating a new db, then we can still use an existing node.id
        if(nodePropExists){
            try {
                returnConfig.load(nodePropsFile);
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO,
                        "node.properties file was found. Checking database name match", isVerbose, printStream);
                //note: we don't need the value from node.properties, but if it's there well check they match
                final String nodePropHostName = returnConfig.getString("node.db.config.main.name");
                if(nodePropHostName != null && !nodePropHostName.isEmpty()){
                    if(!dbName.equals(nodePropHostName)){
                        throw new InvalidProgramArgumentException("Provided database name does not match with database " +
                                "name in node.properties file.  If you wish to create a new database, use the " +
                                CREATE_NEW_DB.getName() + " option");
                    }
                }
                //valildity check on node.properties
                final String nodeId = returnConfig.getString("node.id");
                if(nodeId == null || nodeId.isEmpty()){
                    throw new IllegalStateException("node.id does not exist in SSG node.properties. SSG is not" +
                            " correctly configured");
                }


            } catch (ConfigurationException e) {
                throw new IllegalStateException("Cannot read from node.properties");
            }
            final String nodeId = returnConfig.getString("node.id");
            if(nodeId == null || nodeId.isEmpty()){
                throw new IllegalStateException("node.id does not exist in SSG node.properties. SSG is not" +
                        " correctly configured");
            }

        }else{
            try {
                final String nodeId = NodeConfigurationManager.loadOrCreateNodeIdentifier("default",
                        new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, gwUser, gwPass), true);
                returnConfig.setProperty("node.id", nodeId );
            } catch (IOException e) {
                throw new IllegalStateException("Could not generate a node.id: " + e.getMessage());
            }

        }

        returnConfig.setProperty("node.db.config.main.host", dbHost);
        returnConfig.setProperty("node.db.config.main.port", dbPort);
        returnConfig.setProperty("node.db.config.main.name", dbName);
        returnConfig.setProperty("node.db.config.main.user", gwUser);
        returnConfig.setProperty("node.db.config.main.pass", mpm.encryptPassword(gwPass.toCharArray()));
        //supplied cluster passphrase always comes from the command line
        suppliedClusterPassphrase = programFlagsAndValues.get(CLUSTER_PASSPHRASE.getName());
        returnConfig.setProperty("node.cluster.pass",
                mpm.encryptPassword(suppliedClusterPassphrase.toCharArray()));

        return new Pair<PropertiesConfiguration, MasterPasswordManager>(returnConfig, mpm);
    }

    /**
     * Sets databaseConfig
     * @param args
     * @throws InvalidProgramArgumentException
     * @throws ConfigurationException
     * @throws IOException
     */
    private void validateMigrateProgramParameters(final Map<String, String> args)
            throws InvalidProgramArgumentException, IOException {
        validateCommonProgramParameters(args);
        //do we need to do a database backup? If -config was specified, then we don't want to modify the database
        final boolean isDbRequested = !args.containsKey(CONFIG_ONLY.getName());
        if(isDbRequested){

            //check if the -newdb option was supplied
            canCreateNewDb = args.containsKey(CREATE_NEW_DB.getName());

            final Pair<PropertiesConfiguration, MasterPasswordManager> pair = getNodePropertiesConfig(args);
            nodePropertyConfig = pair.left;

            databaseConfig = getDatabaseConfig(nodePropertyConfig, pair.right);
            
            //set the admin username and password, previously collected in validateCommonProgramParameters
            setDbAdminInfo();
        }
        
    }

    private final DatabaseConfig getDatabaseConfig(final PropertiesConfiguration propConfig,
                                                   final MasterPasswordManager mpm){

        final String dbHost = propConfig.getString("node.db.config.main.host");
        final String dbPort = propConfig.getString("node.db.config.main.port");
        final String dbName = propConfig.getString("node.db.config.main.name");
        final String gwUser = propConfig.getString("node.db.config.main.user");
        final String gwPass =
                new String(mpm.decryptPasswordIfEncrypted(propConfig.getString("node.db.config.main.pass")));

        return new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, gwUser, gwPass);
    }




    private void configureDatabaseConfigFromArgs(final Map<String, String> args,
                                                 final PropertiesConfiguration nodePropertyConfig,
                                                 final MasterPasswordManager mpm)
            throws InvalidProgramArgumentException {
        final Pair<String, String> hostPortPair = ImportExportUtilities.getDbHostAndPortPair(args.get(DB_HOST_NAME.getName()));
        final String dbHost = hostPortPair.left;
        final String dbPort = hostPortPair.right;
        final String dbName = args.get(DB_NAME.getName());
        final String gwUser = args.get(GATEWAY_DB_USERNAME.getName());
        final String gwPass = args.get(GATEWAY_DB_PASSWORD.getName());

        databaseConfig = new DatabaseConfig(dbHost, Integer.parseInt(dbPort), dbName, gwUser, gwPass);

        nodePropertyConfig.setProperty("node.db.config.main.host", dbHost);
        nodePropertyConfig.setProperty("node.db.config.main.port", dbPort);
        nodePropertyConfig.setProperty("node.db.config.main.name", dbName);
        nodePropertyConfig.setProperty("node.db.config.main.user", gwUser);
        //don't encrypt the password - it was passed in plain text and no where downstream will it get
        //decrypted
        nodePropertyConfig.setProperty("node.db.config.main.pass", gwPass);
        //supplied cluster passphrase always comes from the command line
        nodePropertyConfig.setProperty("node.cluster.pass",
                mpm.encryptPassword(suppliedClusterPassphrase.toCharArray()));
    }

    private void setDbAdminInfo(){
        databaseConfig.setDatabaseAdminUsername(adminDBUsername);
        databaseConfig.setDatabaseAdminPassword(adminDBPasswd);
    }
    /**
     * Get a correctly configured DatabaseConfig from the files node.properties and omp.dat
     * @param nodeProperties node.properties file. Must exist and be readable
     * @param ompFile omp.dat. Must exist and be readable
     * @return a Pair of a correctly configured DatabaseConfig based on the supplied confiuration files with its
     * associated cluster passphrase
     * @throws java.io.IOException if problem reading the files
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException if the
     * suplied files contain invalid data / settings
     */
    private Pair<DatabaseConfig, String> getDatabaseConfig(final File nodeProperties, final File ompFile)
            throws IOException, InvalidProgramArgumentException {
        if(nodeProperties == null) throw new NullPointerException("nodeProperties cannot be null");
        if(!nodeProperties.exists())
            throw new InvalidProgramArgumentException("File '"+nodeProperties.getAbsolutePath()+"' does not exist");
        if(nodeProperties.isDirectory())
            throw new InvalidProgramArgumentException("File '"+nodeProperties.getAbsolutePath()+"' is not a regular file");

        if(ompFile == null) throw new NullPointerException("ompFile cannot be null");
        if(!ompFile.exists())
            throw new InvalidProgramArgumentException("File '"+ompFile.getAbsolutePath()+"' does not exist");
        if(ompFile.isDirectory())
            throw new InvalidProgramArgumentException("File '"+ompFile.getAbsolutePath()+"' is not a regular file");

        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodeProperties, true);
        final DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL,
                NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( config == null ) {
            throw new InvalidProgramArgumentException("DatabaseConfig could not be configured with node.properties and omp.dat");
        }

        final MasterPasswordManager decryptor =
                ompFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) : null;
        config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );

        String clusterPassPhrase = new String(decryptor.decryptPasswordIfEncrypted(nodeConfig.getClusterPassphrase()));

        return new Pair<DatabaseConfig, String>(config, clusterPassPhrase);
    }

    /**
     * Note: below there is no filtering of what components are added when isMigrate is true. Migrate is a capability
     * added to the restore of certain components - db, os and config files.
     * ssgmigrate.sh ensuqres that the Importer class is provided with the correct command line arguments, and it
     * should always produce a selective restote, in which cases components like ca, ma won't be included, but they
     * can be part of a selective restore with migrate capabilities if desired
     * 
     * @param mappingFile can be null
     * @return list of applicable RestoreComponent's. Filtered for selective restore if applicable
     * @throws RestoreImpl.RestoreException
     */
    private List<RestoreComponent<? extends Exception>> getComponentsForRestore(final String mappingFile)
            throws Restore.RestoreException {

        final Restore restore = BackupRestoreFactory.getRestoreInstance(this.secureSpanHome,
                this.backupImage,
                databaseConfig,/*I might be null and that's ok*/
                suppliedClusterPassphrase,
                this.isVerbose,
                this.printStream);


        final List<RestoreComponent<? extends Exception>>
                componentList = new ArrayList<RestoreComponent<? extends Exception>>();

        //isSelectiveRestore represents isRequired in all methods to Restore interface
        //at the end the list of components is filtered, so if isSelectiveRestore is true, then the components
        //are filtered, and the remaining components are 'required'

        //ssg config files - SSG CONFIG MUST ALWAYS COME FIRST
        componentList.add(new RestoreComponent<Exception>(){
            public void doRestore() throws Exception {
                final String msg = "Restoring component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                //if it's migrate, we don't want to use any found node.properties
                restore.restoreComponentConfig(isSelectiveRestore, isMigrate, (isMigrate || updateNodeProperties));
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CONFIG;
            }
        });

        //Allowing the actual restoreComponentMainDb method to decide whether to back up or not
        //by doing this we can easily log when a backup included a db, but we decided to leave it alone, because
        //it was not local for example
        componentList.add(new RestoreComponent<Exception>(){
            public void doRestore() throws Exception {
                final String msg = "Restoring component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                //when isMigrate is true, then we always want to write node.properties
                restore.restoreComponentMainDb(isSelectiveRestore, isMigrate, canCreateNewDb, mappingFile,
                        (isMigrate || updateNodeProperties), nodePropertyConfig);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MAINDB;
            }
        });

        componentList.add(new RestoreComponent<Exception>(){
            public void doRestore() throws Exception {
                final String msg = "Restoring component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                restore.restoreComponentAudits(isSelectiveRestore, isMigrate);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.AUDITS;
            }
        });

        //os files
        componentList.add(new RestoreComponent<Exception>(){
            public void doRestore() throws Exception {
                final String msg = "Restoring component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                restore.restoreComponentOS(isSelectiveRestore);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.OS;
            }
        });

        //custom assertion files and jars
        componentList.add(new RestoreComponent<Exception>(){
            public void doRestore() throws Exception {
                final String msg = "Restoring component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                restore.restoreComponentCA(isSelectiveRestore);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CA;
            }
        });

        //modular assertion aar files
        componentList.add(new RestoreComponent<Exception>(){
            public void doRestore() throws Exception {
                final String msg = "Restoring component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                restore.restoreComponentMA(isSelectiveRestore);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MA;
            }
        });

        //Check for -esm, we don't add this by default, only when it's explicitly asked for
        if(programFlagsAndValues.containsKey(CommonCommandLineOptions.ESM_OPTION.getName())){
            componentList.add(new RestoreComponent<Exception>(){
                public void doRestore() throws Exception {
                    final String msg = "Restoring component " + getComponentType().getComponentName();
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg, isVerbose, printStream);
                    restore.restoreComponentESM(isSelectiveRestore);
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.ESM;
                }
            });
        }

        //feedback to user
        if(isSelectiveRestore){
            if(!isMigrate){
                //don't tell the user this if they are doing a migrate
                ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Performing a selective restore",
                        isVerbose, printStream);
            }
            return ImportExportUtilities.filterComponents(componentList, programFlagsAndValues);
        }else{
            return componentList;
        }
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
     * Get the usage for the Importer utility. Usage information is written to the StringBuilder output
     * @param output StringBuilder to write the usage information to
     */
    static void getImporterUsage(final StringBuilder output) {
        final List<CommandLineOption> restoreArgList = getRestoreOptionsWithDb();
        final int largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(restoreArgList);
        for (CommandLineOption option : restoreArgList) {
            output.append("\t")
                    .append(option.getName())
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.getName().length() + 1))
                    .append(option.getDescription() )
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

    public static final class RestoreMigrateResult {
        private final Status status;
        private final List<String> failedComponents;
        private final Exception exception;
        private boolean wasMigrate;

        public enum Status{
            SUCCESS(),
            FAILURE(),
            PARTIAL_SUCCESS()
        }

        private RestoreMigrateResult(final boolean wasMigrate,
                                     final Status status,
                                     final List<String> failedComponents,
                                     final Exception exception){
            this.status = status;
            this.failedComponents = failedComponents;
            this.exception = exception;
            this.wasMigrate = wasMigrate;
        }

        private RestoreMigrateResult(final boolean wasMigrate, final Status status){
            this.status = status;
            this.failedComponents = null;
            this.exception = null;
            this.wasMigrate = wasMigrate;
        }

        public List<String> getFailedComponents() {
            return failedComponents;
        }

        public Status getStatus() {
            return status;
        }

        public Exception getException() {
            return exception;
        }

        public boolean isWasMigrate() {
            return wasMigrate;
        }
    }

    private static List<CommandLineOption> getStandardRestoreOptions(){
        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(Arrays.asList(ALL_RESTORE_OPTIONS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_FTP_OPTIONS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_COMPONENTS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ESM_OPTION));
        return validArgList;
    }

    private static List<CommandLineOption> getRestoreOptionsWithDb(){
        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(Arrays.asList(COMMAND_LINE_DB_ARGS));
        validArgList.addAll(Arrays.asList(ALL_RESTORE_OPTIONS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_FTP_OPTIONS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_COMPONENTS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ESM_OPTION));
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
        validArgList.addAll(Arrays.asList(ALL_MIGRATE_OPTIONS));
        return validArgList;
    }
}