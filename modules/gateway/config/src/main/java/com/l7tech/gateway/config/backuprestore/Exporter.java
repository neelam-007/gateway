package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.*;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
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
            "name of image file to create locally or on ftp host if -ftp* options are used", true);

    static final CommandLineOption IA_AUDIT_FLAG = new CommandLineOption("-ia", "include audit data with a default backup", false);

    static final CommandLineOption MAPPING_PATH = new CommandLineOption("-it",
            "populate supplied file with template mapping information", true);

//    static final CommandLineOption ESM =
    private static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, IA_AUDIT_FLAG, MAPPING_PATH,
            CommonCommandLineOptions.VERBOSE, CommonCommandLineOptions.HALT_ON_FIRST_FAILURE};

    private static final CommandLineOption[] ALL_IGNORED_OPTIONS = {
            new CommandLineOption("-p", "ignored parameter for backup", false) };

    /** Home directory of the SSG installation. This will always be /opt/SecureSpan/Gateway however maintaining
     * the ability for this to be theoritically installed into other directories*/
    private final File ssgHome;

    /**
     * Base folder of all Securespan products
     */
    private final File secureSpanHome;

    /** Stream for verbose output; <code>null</code> for no verbose output. */
    private PrintStream printStream;

    /**
     * confDir is a file created from combining ssg_home with NODE_CONF_DIR. This is the directory for the SSGs
     * config files
     */
    private final File confDir;

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

    static final class BackupResult extends ImportExportUtilities.UtilityResult{

        private final String backUpImageName;

        private BackupResult(final String backUpImageName,
                             final Status status,
                             final List<String> failedComponents,
                             final Exception exception){
            super(status, failedComponents, exception);
            this.backUpImageName = backUpImageName;
        }

        private BackupResult(final String backUpImageName, final Status status){
            super(status);
            this.backUpImageName = backUpImageName;
        }

        public String getBackUpImageName() {
            return backUpImageName;
        }
    }

    /**
     * @param secureSpanHome home directory where the SSG is installed. Should equal /opt/SecureSpan/Gateway. Cannot be null
     * @param printStream    stream for verbose output. Can be null
     * @throws NullPointerException     if ssgHome or ssgAppliance are null
     * @throws IllegalArgumentException if ssgHome does not exsit or is not a directory, or if applianceHome is the
     *                                  empty string
     */
    Exporter(final File secureSpanHome, final PrintStream printStream) throws Backup.BackupException {
        if (secureSpanHome == null) throw new NullPointerException("ssgHome cannot be null");
        if (!secureSpanHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if (!secureSpanHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");

        final File testSsgHome = new File(secureSpanHome, ImportExportUtilities.GATEWAY);
        if (!testSsgHome.exists()) throw new IllegalArgumentException("Gateway installation not found");
        if (!testSsgHome.isDirectory()) throw new IllegalArgumentException("Gateway incorrectly installed");

        ssgHome = testSsgHome;
        this.secureSpanHome = secureSpanHome;

        //this class is not usable without an installed SSG >= 5.0
        final boolean checkVersion =
                SyspropUtil.getBoolean("com.l7tech.gateway.config.backuprestore.checkversion", true);
        if (checkVersion) {
            int[] versionInfo = ImportExportUtilities.throwIfLessThanFiveO(new File(ssgHome, "runtime/Gateway.jar"));
            isPostFiveO = versionInfo[1] > 0;
        } else {
            //we won't have the /opt/SecureSpan/Gateway/Backup folder, set false by default, system prop is for testing
            isPostFiveO = SyspropUtil.getBoolean("com.l7tech.gateway.config.backuprestore.setpostfiveo", false);
        }

        this.printStream = printStream;

        confDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
    }

    /**
     * <p>
     * Create the backup image zip file.
     * The returned BackupResult will contain BackupResult.Status.SUCCESS when all components applicable were backed up
     * and no failures occured. BackupResult.Status.PARTIAL_SUCCESS is returned when -halt was used and 1 or more
     * components failed to back up succesfully, BackupResult.Status.FAILURE is returned when the back up failed,
     * and no image zip file was produced
     * </p>
     * @param args array of all command line arguments
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException if any of the required program parameters are not supplied
     * @return BackupResult This contains the name of the image file created. This will be based on the value supplied with
     * the -image parameter. A timestamp will have been added to the file name. It also contains the status. The status
     * can be SUCCESS, FAILURE or PARTIAL_SUCCESS.
     */
    BackupResult createBackupImage(final String [] args) throws InvalidProgramArgumentException {

        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(Arrays.asList(ALLOPTIONS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_FTP_OPTIONS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_COMPONENTS));
        validArgList.addAll(Arrays.asList(CommonCommandLineOptions.ESM_OPTION));

        programFlagsAndValues =
                ImportExportUtilities.getAndValidateCommandLineOptions(args,
                        validArgList, Arrays.asList(ALL_IGNORED_OPTIONS), true, printStream);

        validateProgramParameters();
        final boolean usingFtp = ImportExportUtilities.checkAndValidateFtpParams(programFlagsAndValues);

        Backup backup = null;
        try {
            //overwrite the supplied image name with a unique name based on it
            String pathToUniqueImageFile = getUniqueImageFileName(programFlagsAndValues.get(IMAGE_PATH.getName()), usingFtp);

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

            final FtpClientConfig ftpConfig = (usingFtp)?ImportExportUtilities.getFtpConfig(programFlagsAndValues): null;

            backup = BackupRestoreFactory.getBackupInstance(secureSpanHome, ftpConfig, pathToUniqueImageFile,
                    isPostFiveO, isVerbose, printStream);

            final String mappingFile = programFlagsAndValues.get(MAPPING_PATH.getName());
            performBackupSteps(mappingFile, backup);

            if(!failedComponents.isEmpty()){
                return new BackupResult(pathToUniqueImageFile, BackupResult.Status.PARTIAL_SUCCESS, failedComponents, null);
            }
            return new BackupResult(pathToUniqueImageFile, BackupResult.Status.SUCCESS);
            
        } catch(Backup.BackupException e){
            return new BackupResult(null, BackupResult.Status.FAILURE, null, e);
        } catch(Exception e){
            throw new RuntimeException(e);
        } finally {
            if(backup != null){
                if(!backup.deleteTemporaryDirectory()){
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO,
                            "Could not delete backup image temporary folder", isVerbose, printStream);
                }
            }
        }
    }

    /**
     * Given the image file path (optional) and name, return a unique file name which is equal to
     * getDirPart(pathToImageFile) + "yyyymmddhhnnss_" + getFilePart(pathToImageFile)
     *
     * If the system property com.l7tech.gateway.config.backuprestore.nomodifyimagename.nouniqueimagename is set
     * to 'true', then this will return the pathToImageFile unmodified 
     * @param pathToImageFile path and file name to make unique
     * @param usingFtp if true, then if pathToImageFile has no path part, it won't add the default images folder
     * @return a unique file name
     */
    String getUniqueImageFileName(final String pathToImageFile, final boolean usingFtp) {

        final String imagePathAndName = (isPostFiveO && !usingFtp)?getPostFiveOAbsImagePath(pathToImageFile)
                :pathToImageFile;

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
     * The behaviour of this method is that each back up is performed individually and independently of others. If any
     * component fails to back up, this is logged, and the execution continues onto the next component. This behaviour
     * can be modified via the -halt parameter
     * </p>
     *
     * @param mappingFile path (optional) and name of the mapping file to be created. Can be null
     * @param backup the Backup instance to use for backup
     * @throws Backup.BackupException for any Exception when backing up the components
     * @throws IOException if any problems when creating the zip file or its not possible to ftp the newly created image
     */
    private void performBackupSteps(final String mappingFile, final Backup backup)
            throws Backup.BackupException, IOException {

        final List<BackupComponent> compsToBackup =
                getComponentsForBackup(mappingFile, backup);

        for(BackupComponent component: compsToBackup){
            try{
                final ComponentResult result = component.doBackup();
                if(isSelectiveBackup && result.getResult() == ComponentResult.Result.NOT_APPLICABLE) {
                    throw new Backup.BackupException(result.getNotApplicableMessage());
                }else if (result.getResult() == ComponentResult.Result.NOT_APPLICABLE) {
                    final String msg1 = "Not applicable for this host";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.INFO, msg1, isVerbose, printStream);
                }
            }catch (Backup.BackupException e) {
                final String msg = "Component '" + component.getComponentType().getComponentName()+ "' could not be " +
                        "backed up: " + e.getMessage();
                if (isHaltOnFirstFailure) {
                    ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg, isVerbose, printStream);
                    final String msg1 = "Halting backup as -halt option was supplied";
                    ImportExportUtilities.logAndPrintMessage(logger, Level.SEVERE, msg1, isVerbose, printStream);
                    throw e;
                }
                ImportExportUtilities.logAndPrintMessage(logger, Level.WARNING, msg, isVerbose, printStream);
                failedComponents.add(component.getComponentType().getComponentName());
            }
        }

        backup.createBackupImage();
    }

    /**
     * <p>
     * createBackupImage() does a non fail fast backup by default, unless -halt is supplied.
     * This means that we will try and back up each applicable
     * component individually and independently of others.
     * </p>
     *
     * <p>
     * The returned data structure is a List containing BackupComponent. A BackupComponent can tell you what component
     * it represents
     * </p>
     *
     * <p>
     * If any selective components were requested via the program parameters, then the returned Map only
     * contains the requested components 
     * </p>
     * @param mappingFile should the db compoment backup create a mapping file? Can be null if not required
     * @param backup the Backup instance to use for backup
     * @throws IOException if any exception ocurs reading node.properties to get database information. This is always
     * @return A List of BackupComponent. Clients can iterate over this list and call doBackup() to back up the component
     * its wrapping
     */
    private List<BackupComponent> getComponentsForBackup(
            final String mappingFile, final Backup backup) throws IOException {

        final List<BackupComponent> componentList = new ArrayList<BackupComponent>();

        final BackupComponent versionComp = new BackupComponent() {
            public ComponentResult doBackup() throws Backup.BackupException {
                final String msg = "Backing up component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);
                // record version of this image
                return backup.backUpVersion();
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

        final BackupComponent dbComp = new BackupComponent() {
            public ComponentResult doBackup() throws Backup.BackupException {
                final String msg = "Backing up component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                //this will also create the mapping file if it was requested
                return backup.backUpComponentMainDb(mappingFile, config);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MAINDB;
            }
        };
        componentList.add(dbComp);
        // check whether or not we are expected to include audit in export
        //other components are just added, and they decide themselves if they are applicable or not
        //audits are different as the user has to always explicitly say they want audits
        if (includeAudits) {
            final BackupComponent auditComp = new BackupComponent() {
                public ComponentResult doBackup() throws Backup.BackupException {
                    final String msg = "Backing up component " + getComponentType().getComponentName();
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                    return backup.backUpComponentAudits(config);
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.AUDITS;
                }
            };
            componentList.add(auditComp);
        }

        final BackupComponent configComp = new BackupComponent() {
            public ComponentResult doBackup() throws Backup.BackupException {
                final String msg = "Backing up component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                return backup.backUpComponentConfig();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CONFIG;
            }
        };
        componentList.add(configComp);

        final BackupComponent osComp = new BackupComponent() {
            public ComponentResult doBackup() throws Backup.BackupException {
                final String msg = "Backing up component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                //restore OS files if this is an appliance
                return backup.backUpComponentOS();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.OS;
            }
        };
        componentList.add(osComp);

        final BackupComponent caComp = new BackupComponent() {
            public ComponentResult doBackup() throws Backup.BackupException {
                final String msg = "Backing up component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                return backup.backUpComponentCA();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CA;
            }
        };

        componentList.add(caComp);

        final BackupComponent maComp = new BackupComponent() {
            public ComponentResult doBackup() throws Backup.BackupException {
                final String msg = "Backing up component " + getComponentType().getComponentName();
                ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                return backup.backUpComponentMA();
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MA;
            }
        };
        componentList.add(maComp);

        //Check for -esm, we don't add this by default, only when it's explicitly asked for
        if (programFlagsAndValues.containsKey(CommonCommandLineOptions.ESM_OPTION.getName())) {
            final BackupComponent emComp = new BackupComponent() {
                public ComponentResult doBackup() throws Backup.BackupException {
                    final String msg = "Backing up component " + getComponentType().getComponentName();
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, isVerbose, printStream);

                    return backup.backUpComponentESM();
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.ESM;
                }
            };
            componentList.add(emComp);
        }

        if(isSelectiveBackup){
            ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, "Performing a selective backup",
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
     * @throws IOException for arguments which are files, they are checked to see if the exist, which may cause an IOException
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException if any program params are invalid
     */
    private void validateProgramParameters() throws BackupRestoreLauncher.InvalidProgramArgumentException {
        //image option must be specified
        if (!programFlagsAndValues.containsKey(IMAGE_PATH.getName())) {
            throw new InvalidProgramArgumentException("missing option " + IMAGE_PATH.getName() + ", required for exporting image");
        } 

        //check if ftp requested
        ImportExportUtilities.checkAndValidateFtpParams(programFlagsAndValues);

        //check if node.properties file exists
        final File configDir = new File(ssgHome, ImportExportUtilities.NODE_CONF_DIR);
        final File nodePropsFile = new File(configDir, ImportExportUtilities.NODE_PROPERTIES);
        final NodeConfig nodeConfig;
        try {
            nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        final DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( config == null ) {
            throw new RuntimeException("Database configuration could not be loaded");
        }

        final File ompFile = new File(configDir, ImportExportUtilities.OMP_DAT);
        if(!ompFile.exists() || !ompFile.isFile()){
            throw new IllegalStateException("omp.dat was not found at '"+ ompFile.getAbsolutePath()+"'");
        }

        final MasterPasswordManager decryptor =
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword());
        config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );

        //check if we can connect to the database
        //we only need to do this if the db is local, as otherwise a db connection is not required to perform the backup
        if(ImportExportUtilities.isDatabaseAvailableForBackupRestore(config.getHost())){
            try {
                ImportExportUtilities.verifyDatabaseConnection(config, false);
            } catch (SQLException e) {
                throw new InvalidProgramArgumentException(e.getMessage());
            }
        }

        //will we use isVerbose output?
        if(programFlagsAndValues.containsKey(CommonCommandLineOptions.VERBOSE.getName())) isVerbose = true;

        if(programFlagsAndValues.containsKey(CommonCommandLineOptions.HALT_ON_FIRST_FAILURE.getName())) isHaltOnFirstFailure = true;

        //determine if we are doing a selective backup
        isSelectiveBackup = ImportExportUtilities.isSelective(programFlagsAndValues);

        if(isSelectiveBackup){
            //ensure that we are not being asked to backup audits without the maindb
            final boolean auditsOnly = !programFlagsAndValues.containsKey(CommonCommandLineOptions.MAINDB_OPTION.getName())
                    && programFlagsAndValues.containsKey(CommonCommandLineOptions.AUDITS_OPTION.getName());
            if(auditsOnly){
                throw new InvalidProgramArgumentException("Cannot backup -audits without -maindb");
            }
        }

        //the decision to back up audits can come from the -ia flag or from the audits flag
        //-ia means backup audits with a full backup
        //this code is only concerned about whether we are including audits by any means
        final String auditVal = programFlagsAndValues.get(IA_AUDIT_FLAG.getName());
        if (auditVal != null && !auditVal.toLowerCase().equals("no") && !auditVal.toLowerCase().equals("false")) {
            includeAudits = true;
        }else {
            includeAudits =  programFlagsAndValues.containsKey("-"
                    + ImportExportUtilities.ComponentType.AUDITS.getComponentName());
        }

        //do a quick check to see if the -esm was requrested
        final boolean esmRequested = programFlagsAndValues.containsKey("-"
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
        allOptList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_FTP_OPTIONS));
        allOptList.addAll(Arrays.asList(CommonCommandLineOptions.ALL_COMPONENTS));
        allOptList.addAll(Arrays.asList(CommonCommandLineOptions.ESM_OPTION));

        final List<CommandLineOption> prependOptions = new ArrayList<CommandLineOption>();
                prependOptions.addAll(Arrays.asList(CommonCommandLineOptions.ALL_COMPONENTS));
                prependOptions.add(CommonCommandLineOptions.ESM_OPTION);

        int largestNameStringSize;
        largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(allOptList);
        for (final CommandLineOption option : allOptList) {
            final String description = (prependOptions.contains(option))?
                    "backup " + option.getDescription(): option.getDescription();

            output.append("\t")
                    .append(option.getName())
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.getName().length() + 1))
                    .append(description)
                    .append(BackupRestoreLauncher.EOL_CHAR);
        }
    }
}