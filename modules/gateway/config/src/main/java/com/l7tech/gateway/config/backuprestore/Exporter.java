package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.*;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.jscape.inet.ftp.FtpException;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.sql.SQLException;
import java.text.SimpleDateFormat;


/**
 * <p>
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * </p>
 *
 *
 * <p>
 * The utility that backups up an SSG image
 * Exporter is responsible for accecpting parameters, validating them then calling the appropriate methods of
 * the Backup interface
 * </p>
 *
 * @see BackupRestoreFactory
 * @see com.l7tech.gateway.config.backuprestore.Backup
 */
public final class Exporter{

    private static final Logger logger = Logger.getLogger(Exporter.class.getName());

    // exporter options
    static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image",
                                                                             "location of image file to export",
                                                                             true,
                                                                             false);
    static final CommandLineOption AUDIT = new CommandLineOption("-ia",
                                                                        "to include audit tables",
                                                                        false,
                                                                        true);
    static final CommandLineOption MAPPING_PATH = new CommandLineOption("-it",
                                                                               "path of the output mapping template file",
                                                                               true, false);

    private static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, AUDIT, MAPPING_PATH,
            ImportExportUtilities.VERBOSE, ImportExportUtilities.HALT_ON_FIRST_FAILURE};

    private static final CommandLineOption[] ALL_IGNORED_OPTIONS = {
            new CommandLineOption("-p", "Ignored parameter for partition", true, false) };

    /** Home directory of the SSG installation. This will always be /opt/SecureSpan/Gateway however maintaining
     * the ability for this to be theoritically installed into other directories*/
    private final File ssgHome;

    /** Stream for verbose output; <code>null</code> for no verbose output. */
    private PrintStream printStream;

    /**
     * confDir is a file created from combining ssg_home with NODE_CONF_DIR. This is the directory for the SSGs
     * config files
     */
    private final File confDir;

    private String applianceHome;
    static final String NO_UNIQUE_IMAGE_SYSTEM_PROP =
            "com.l7tech.gateway.config.backuprestore.nouniqueimagename";

    /**
     * When backing up 5.0 we won't have the config/backup/images folder. This is where we will write back ups
     * to by default. Post 5.0 if the image name is just a file name, then the image will be written into the
     * config/backup/images folder by default. This default behaivour does not happen in 5.0 
     */
    private final boolean isPostFiveO;

    /**
     * Include verbose output from back up methods
     */
    private boolean isVerbose;

    /**
     * When true, the createBackupImage is fail fast
     */
    private boolean isHaltOnFirstFailure;

    /**
     * When true, only the components actually requrested via the program parameters will be backed up
     */
    private boolean isSelectiveBackup;

    private boolean includeAudits;

    /**
     * Backup is not fail fast. As a result we will allow clients access to this list of error message so that they
     * can be printed to the screen, when in -v mode
     */
    private final List<String> failedComponents = new ArrayList<String>();

    private Map<String, String> programFlagsAndValues;

    private Backup backup;


    static final class BackupResult{

        private final String backUpImageName;
        private final Status status;
        private final List<String> failedComponents;
        private final Exception exception;

        public enum Status{
            SUCCESS(),
            FAILURE(),
            PARTIAL_SUCCESS()
        }

        private BackupResult(final String backUpImageName,
                             final Status status,
                             final List<String> failedComponents,
                             final Exception exception){
            this.backUpImageName = backUpImageName;
            this.status = status;
            this.failedComponents = failedComponents;
            this.exception = exception;
        }

        private BackupResult(final String backUpImageName, final Status status){
            this.backUpImageName = backUpImageName;
            this.status = status;
            this.failedComponents = null;
            this.exception = null;
        }

        public String getBackUpImageName() {
            return backUpImageName;
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
    }
    
    /**
     * @param ssgHome   home directory where the SSG is installed. Should equal /opt/SecureSpan/Gateway. Cannot be null
     * @param printStream        stream for verbose output; <code>null</code> for no verbose output
     * @param applianceHome the standard installation directory of the SSG appliance. If this folder exists then
     * OS files will be backed up via backUpComponentOS(). Cannot be null or the empty string
     * @throws NullPointerException if ssgHome or ssgAppliance are null
     * @throws IllegalArgumentException if ssgHome does not exsit or is not a directory, or if applianceHome is the
     * empty string
     */
    Exporter(final File ssgHome, final PrintStream printStream, final String applianceHome) throws Backup.BackupException {
        if(ssgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!ssgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!ssgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");
        if(applianceHome == null) throw new NullPointerException("applianceHome cannot be null");
        if(applianceHome.trim().isEmpty()) throw new IllegalArgumentException("applianceHome cannot be null");
        
        this.ssgHome = ssgHome;

        //this class is not usable without an installed SSG >= 5.0
        final boolean checkVersion =
                SyspropUtil.getBoolean("com.l7tech.gateway.config.backuprestore.checkversion", true);
        if (checkVersion){
            int [] versionInfo = ImportExportUtilities.throwIfLessThanFiveO(new File(ssgHome, "runtime/Gateway.jar"));
            isPostFiveO = versionInfo[2] > 0;
        }else{
            isPostFiveO = false;//we won't have the /opt/SecureSpan/Gateway/Backup folder
        }

        this.printStream = printStream;

        confDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
        this.applianceHome = applianceHome;
    }

    /**
     * <p>
     * Create the backup image zip file.
     * The following arguments are expected in the array args:
     * <pre>
     * -image    location of image file to export. Value required
	 * -ia       to include audit tables. No value reuired
	 * -it       path of the output mapping template file. Value required
     * -v        verbose output
     * -halt     if supplied, then we fail fast
     * </pre>
     * </p>
     *
     * <p>
     * The returned BackupResult will contain BackupResult.Status.SUCCESS when all components applicable were backed up
     * and no failures occured. BackupResult.Status.PARTIAL_SUCCESS is returned when -halt was used and 1 or more
     * components failed to back up succesfully, BackupResult.Status.FAILURE is returned when the back up failed,
     * and no image zip file was produced
     * </p>
     * @param args array of all command line arguments
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException If any of the required program parameters are not supplied
     * @throws IOException Any IOException which occurs while creating the image zip file
     * @throws IllegalStateException if node.properties is not found. This must exist as we only back up a
     * correctly configured SSG node
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.FatalException if ftp is requested and
     * its not possible to ftp the newly created image
     * @return BackupResult This contains the name of the image file created. This will be based on the value supplied with
     * the -image parameter. A timestamp will have been added to the file name. It also contains the status. The status
     * can be SUCCESS, FAILURE or PARTIAL_SUCCESS.
     */
    BackupResult createBackupImage(final String [] args)
            throws InvalidProgramArgumentException, IOException, BackupRestoreLauncher.FatalException, Backup.BackupException {

        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(Arrays.asList(ALLOPTIONS));
        validArgList.addAll(Arrays.asList(ImportExportUtilities.ALL_FTP_OPTIONS));
        validArgList.addAll(Arrays.asList(ImportExportUtilities.ALL_COMPONENTS));
        programFlagsAndValues =
                ImportExportUtilities.getAndValidateCommandLineOptions(args,
                        validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        validateProgramParameters(programFlagsAndValues);
        final boolean usingFtp = ImportExportUtilities.checkAndValidateFtpParams(programFlagsAndValues);

        //overwrite the supplied image name with a unique name based on it
        String pathToUniqueImageFile = getUniqueImageFileName(programFlagsAndValues.get(IMAGE_PATH.getName()));
        if (printStream != null && isVerbose) printStream.println("Creating image: " + pathToUniqueImageFile);
        programFlagsAndValues.put(IMAGE_PATH.getName(), pathToUniqueImageFile);
        //We only want to validate the image file when we are not using ftp
        validateFiles(programFlagsAndValues, !usingFtp);

        if(!usingFtp){
            // check that we can write output at location asked for
            pathToUniqueImageFile = ImportExportUtilities.getAbsolutePath(pathToUniqueImageFile);
            ImportExportUtilities.validateImageFile(pathToUniqueImageFile);
        }

        //check that node.properties exists
        final File nodePropsFile = new File(confDir, ImportExportUtilities.NODE_PROPERTIES);
        if ( !nodePropsFile.exists() ) {
            throw new IllegalStateException("node.properties must exist in " + nodePropsFile.getAbsolutePath());
        }

        //check whether mapping option was used
        //were doing this here as if it's requested, then we need to be able to create it
        if(programFlagsAndValues.get(MAPPING_PATH.getName()) != null) {
            //fail if file exists
            ImportExportUtilities.throwIfFileExists(programFlagsAndValues.get(MAPPING_PATH.getName()));
        }

        final FtpClientConfig ftpConfig = ImportExportUtilities.getFtpConfig(programFlagsAndValues);
        backup = BackupRestoreFactory.getBackupInstance(ssgHome, applianceHome, ftpConfig, pathToUniqueImageFile,
                isVerbose, printStream);        

        try {
            final String mappingFile = programFlagsAndValues.get(MAPPING_PATH.getName());
            performBackupSteps(mappingFile);
        } catch(Exception e){
            return new BackupResult(null, BackupResult.Status.FAILURE, null, e);
        } finally {
            backup.deleteTemporaryDirectory();
        }

        if(!failedComponents.isEmpty()){
            return new BackupResult(pathToUniqueImageFile, BackupResult.Status.PARTIAL_SUCCESS, failedComponents, null);            
        }
        return new BackupResult(pathToUniqueImageFile, BackupResult.Status.SUCCESS);
    }

    /**
     * Given the image file path (optional) and name, return a unique file name which is equal to
     * getDirPart(pathToImageFile) + "yyyymmddhhnnss_" + getFilePart(pathToImageFile)
     *
     * If the system property com.l7tech.gateway.config.backuprestore.nomodifyimagename.nouniqueimagename is set
     * to 'true', then this will return the pathToImageFile unmodified 
     * @param pathToImageFile path and file name to make unique
     * @return a unique file name
     */
    private String getUniqueImageFileName(final String pathToImageFile) {

        final String imagePathAndName = (isPostFiveO)?getPostFiveOAbsImagePath(pathToImageFile):pathToImageFile;

        final String ignoreProp = System.getProperty(NO_UNIQUE_IMAGE_SYSTEM_PROP);
        if(ignoreProp != null){
            if(Boolean.valueOf(ignoreProp)) return imagePathAndName;
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat(ImportExportUtilities.UNIQUE_TIMESTAMP);
        final Calendar cal = Calendar.getInstance();
        final String uniqueStart = dateFormat.format(cal.getTime());

        final String dir = ImportExportUtilities.getDirPart(imagePathAndName);
        final String file = ImportExportUtilities.getFilePart(imagePathAndName);
        return (dir != null) ? dir + File.separator + uniqueStart + "_" + file : uniqueStart + "_" + file;
    }

    /**
     * Get the path name to the supplied file in a post 5.0 environment
     * If pathToImageFile has path info, then it is returned unmodified
     * If the system is on 5.0, then pathToImageFile is returned unmodified
     * If pathToImageFile has no path info, and the environment is > 5.0, then the return string is
     * the absolute path to the default backup folder in a Buzzcut install
     * @param pathToImageFile user supplied image location
     * @return path info to where the image file should be located
     */
    private String getPostFiveOAbsImagePath(final String pathToImageFile){
        if(!isPostFiveO) return pathToImageFile;

        final String dirPart = ImportExportUtilities.getDirPart(pathToImageFile);
        if(dirPart != null) return pathToImageFile;//path info has been supplied

        final File imageFolder = new File(ssgHome, ImportExportUtilities.POST_FIVE_O_DEFAULT_BACKUP_FOLDER);
        final File imageFile = new File(imageFolder, pathToImageFile);
        return imageFile.getAbsolutePath();
    }

    /**
     * <p>
     * Backs up each required component and places it into the tmpOutputDirectory directory, in the correct folder.
     * This method orchestrates the calls to the various backUp*() methods
     * </p>
     *
     * <p> This method is private and depends on the state of instance variables which were set by the caller
     * </p>
     *
     * <p>
     * The bahviour of this method is that each back up is performed individually and independently of others. If any
     * component fails to back up, this is logged, and the execution continues onto the next backup. This behaviour
     * can be modified via the -halt parameter
     * </p>
     *
     * @param mappingFile path (optional) and name of the mapping file to be created. if not required pass <code>null</code>
     * @throws Exception for any Exception when backing up the components or when creating the zip file
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.FatalException if ftp is requested and
     * its not possible to ftp the newly created image
     */
    private void performBackupSteps(final String mappingFile)
            throws Exception {

        final List<BackupComponent<? extends Exception>> compsToBackup =
                getComponentsForBackup(mappingFile);

        for(BackupComponent<? extends Exception> component: compsToBackup){
            try{
                component.doBackup();
            }catch (Exception e) {
                if (isHaltOnFirstFailure) {
                    logger.log(Level.SEVERE, "Could not back up component " + component.getComponentType().getDescription());
                    logger.log(Level.SEVERE, "Halting backup as -halt option was supplied");
                    throw e;
                }
                logger.log(Level.WARNING, "Could not back up component " + component.getComponentType().getDescription());
                failedComponents.add(component.getComponentType().getDescription());
            }
        }

        //todo [Donal] make sure that when an exception is thrown it communicated that it's a big fail to the user
        //as not image was created / ftp'ed
        backup.createBackupImage();
    }

    /**
     * <p>
     * All of the backUp*() methods are public and can be used as an API for Exporter. However when createBackupImage()
     * is used, it promises to do a non fail fast backup. This means that we will try and back up each applicable
     * component individually and independently of others. As each backUp* method can throw Exceptions, this is a
     * convenience method to wrap each component to be backed up in a BackupComonent<? extends Exception> and to
     * return them in a List, which can then simply be iterated over with a very simple and clean try / catch structure.
     * </p>
     *
     * <p>
     * The returned data structure is a List containing BackupComponent. A BackupComponent can tell you what component
     * it represents, as it's nice to be able to report in the logs at a higher level, which
     * component failed to back up. Each component can define any Exception it needs to throw
     * </p>
     *
     * <p>
     * When this function is used from performBackupSteps we will not take action based on specific subclasses of
     * Exception. All we care about from providing a non fail fast backup is that we can catch any exception that happens
     * when backing up a component, and then be able to proceed onto the next.
     * </p>
     *
     * <p>
     * The components returned are either all applicable for a full backup (e.g. if appliance then os, if local db, then
     * db) however if any selective components were requrested via the program parameters, then the returned Map ONLY
     * contains the requested components 
     * </p>
     * @param mappingFile should the db compoment backup create a mapping file? Can be null if not required
     * @throws IOException if any exception ocurs reading node.properties to get database information. This is always
     * @return A List of BackupComponent. Clients can iterate over this list and call doBackup() to back up the component
     * its wrapping
     */
    private List<BackupComponent<? extends Exception>> getComponentsForBackup(
            final String mappingFile) throws IOException {

        final List<BackupComponent<? extends Exception>>
                componentList = new ArrayList<BackupComponent<? extends Exception>>();

        final BackupComponent<Backup.BackupException> versionComp = new BackupComponent<Backup.BackupException>() {
            public void doBackup() throws Backup.BackupException {
                // record version of this image
                backup.backUpVersion();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.VERSION;
            }
        };
        componentList.add(versionComp);

        final File nodePropsFile = new File(confDir, ImportExportUtilities.NODE_PROPERTIES);
        final File ompFile = new File(confDir, ImportExportUtilities.OMP_DAT);
        // Read database connection settings
        final DatabaseConfig config = ImportExportUtilities.getNodeConfig(nodePropsFile, ompFile);

        //Backup database info if the db is local
        if(ImportExportUtilities.isHostLocal(config.getHost())){

            final BackupComponent<Backup.BackupException> dbComp = new BackupComponent<Backup.BackupException>() {
                public void doBackup() throws Backup.BackupException {
                    //this will also create the mapping file if it was requested
                    backup.backUpComponentMainDb(mappingFile, config);
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.MAINDB;
                }
            };
            componentList.add(dbComp);
            // check whether or not we are expected to include audit in export
            if (includeAudits) {
                final BackupComponent<Backup.BackupException> auditComp = new BackupComponent<Backup.BackupException>() {
                    public void doBackup() throws Backup.BackupException {
                        backup.backUpComponentAudits(config);
                    }

                    public ImportExportUtilities.ComponentType getComponentType() {
                        return ImportExportUtilities.ComponentType.AUDITS;
                    }
                };
                componentList.add(auditComp);
            }
        }else{
            logger.log(Level.INFO,  "Database is not local so no backup of database being created");
        }

        final BackupComponent<Backup.BackupException> configComp = new BackupComponent<Backup.BackupException>() {
            public void doBackup() throws Backup.BackupException {
                backup.backUpComponentConfig();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CONFIG;
            }
        };
        componentList.add(configComp);

        final BackupComponent<Backup.BackupException> osComp = new BackupComponent<Backup.BackupException>() {
            public void doBackup() throws Backup.BackupException {
                //restore OS files if this is an appliance
                backup.backUpComponentOS();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.OS;
            }
        };
        componentList.add(osComp);

        final BackupComponent<Backup.BackupException> caComp = new BackupComponent<Backup.BackupException>() {
            public void doBackup() throws Backup.BackupException {
                backup.backUpComponentCA();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CA;
            }
        };

        componentList.add(caComp);

        final BackupComponent<Backup.BackupException> maComp = new BackupComponent<Backup.BackupException>() {
            public void doBackup() throws Backup.BackupException {
                backup.backUpComponentMA();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MA;
            }
        };
        componentList.add(maComp);

        if(isSelectiveBackup){
            ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, "Performing a selective backup",
                    isVerbose, printStream);
            return ImportExportUtilities.filterComponents(componentList, programFlagsAndValues);
        }else{
            return componentList;
        }
    }

    /**
     * Validate all program arguments. This method will validate all required params are met, and that any which expect
     * a value recieve it.
     * Checks for -v parameter, if found sets verbose = true
     * @param args The name value pair map of each argument to it's value, if a vaule exists
     * @throws IOException for arguments which are files, they are checked to see if the exist, which may cause an IOException
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException if any program params are invalid
     */
    private void validateProgramParameters(final Map<String, String> args)
            throws IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
        //image option must be specified
        if (!args.containsKey(IMAGE_PATH.getName())) {
            throw new InvalidProgramArgumentException("missing option " + IMAGE_PATH.getName() + ", required for exporting image");
        } 

        //check if ftp requested
        ImportExportUtilities.checkAndValidateFtpParams(args);

        //check if node.properties file exists
        final File configDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
        final File nodePropsFile = new File(configDir, ImportExportUtilities.NODE_PROPERTIES);
        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        final DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( config == null ) {
            throw new IOException("database configuration not found.");
        }

        final File ompFile = new File(configDir, ImportExportUtilities.OMP_DAT);
        final MasterPasswordManager decryptor =
                ompFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) : null;
        config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );

        //check if we can connect to the database
        //we only need to do this if the db is local, as otherwise a db connection is not required to perform the backup
        if(ImportExportUtilities.isHostLocal(config.getHost())){
            try {
                ImportExportUtilities.verifyDatabaseConnection(config, false);
            } catch (SQLException e) {
                throw new InvalidProgramArgumentException(e.getMessage());
            }
        }

        //will we use isVerbose output?
        if(args.containsKey(ImportExportUtilities.VERBOSE.getName())) isVerbose = true;

        if(args.containsKey(ImportExportUtilities.HALT_ON_FIRST_FAILURE.getName())) isHaltOnFirstFailure = true;

        //determine if we are doing a selective backup
        isSelectiveBackup = ImportExportUtilities.isSelectiveBackup(args);

        //the decision to back up audits can come from the -ia flag or from the audits flag
        //-ia means backup audits with a full backup
        //-audits means only audits, or if other components are selected then it meas 'also audits'
        final String auditVal = programFlagsAndValues.get(AUDIT.getName());
        if (auditVal != null && !auditVal.toLowerCase().equals("no") && !auditVal.toLowerCase().equals("false")) {
            includeAudits = true;
        }else {
            includeAudits =  programFlagsAndValues.containsKey("-" + ImportExportUtilities.ComponentType.AUDITS.getComponentName());
        }
    }

    /**
     * Validate any files which are specified by the program arguments. If validateImageExistence is true, then
     * it will validate that the image file does not exist and that it can be written to.
     *
     * If a mapping file was supplied, it will also have the same tests applied
     * @param args program arguments
     * @param validateImageExistence if true, check that the iamge file exists and that we can write to it
     * @throws IOException if any exception occurs when trying to write to the files
     */
    private void validateFiles(final Map<String, String> args, final boolean validateImageExistence) throws IOException {
        if(validateImageExistence){
            ImportExportUtilities.throwIfFileExists(args.get(IMAGE_PATH.getName()));   //test that the file is a new file
            ImportExportUtilities.verifyCanWriteFile(args.get(IMAGE_PATH.getName()));  //test if we can create the file
        }

        //check condition for mapping file
        if (args.containsKey(MAPPING_PATH.getName())) {
            ImportExportUtilities.throwIfFileExists(args.get(MAPPING_PATH.getName())); //test that the file is a new file
            ImportExportUtilities.verifyCanWriteFile(args.get(MAPPING_PATH.getName()));    //test if we can create the file
        }
    }

    /**
     * Get the usage for the Exporter utility. Usage information is written to the StringBuilder output
     * @param output StringBuilder to write the usage information to
     */
    public static void getExporterUsage(final StringBuilder output) {

        final List<CommandLineOption> allOptList = new ArrayList<CommandLineOption>();
        allOptList.addAll(Arrays.asList(ALLOPTIONS));
        allOptList.addAll(Arrays.asList(ImportExportUtilities.ALL_FTP_OPTIONS));
        allOptList.addAll(Arrays.asList(ImportExportUtilities.ALL_COMPONENTS));

        int largestNameStringSize;
        largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(allOptList);
        for (final CommandLineOption option : allOptList) {
            output.append("\t")
                    .append(option.getName())
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.getName().length() + 1))
                    .append(option.getDescription())
                    .append(BackupRestoreLauncher.EOL_CHAR);
        }

        output.append("FTP options are optional. If FTP is requested, then all ftp parameters must be supplied");
    }
}