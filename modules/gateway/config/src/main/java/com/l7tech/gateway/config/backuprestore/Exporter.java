package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.*;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfig;
import com.l7tech.gateway.common.transport.ftp.FtpClientConfigImpl;
import com.l7tech.gateway.common.transport.ftp.FtpUtils;
import com.jscape.inet.ftp.FtpException;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.sql.SQLException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import org.xml.sax.SAXException;


/**
 * <p>
 * Exporter manages the creation of a complete SSG backup. Calling createBackupImage() will create a zip file
 * containing all components which constitute a complete backup.
 * See http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_Backup_Restore_Func_Spec and
 * http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_Backup_Restore_Design
 * </p>
 *
 * <p>Instances of Exporter are not safe to be used by multiple threads</p>
 * 
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public final class Exporter{

    private static final Logger logger = Logger.getLogger(Exporter.class.getName());

    // exporter options
    public static final CommandLineOption IMAGE_PATH = new CommandLineOption("-image",
                                                                             "location of image file to export",
                                                                             true,
                                                                             false);
    public static final CommandLineOption AUDIT = new CommandLineOption("-ia",
                                                                        "to include audit tables",
                                                                        false,
                                                                        true);
    public static final CommandLineOption MAPPING_PATH = new CommandLineOption("-it",
                                                                               "path of the output mapping template file",
                                                                               true, false);

    public static final CommandLineOption VERBOSE = new CommandLineOption("-v",
            "verbose output, without this ssgbackup.sh is silent. Consult log file ssgbackup%g.log for logging messages",
                                                                               false, true);
    public static final CommandLineOption HALT_ON_FIRST_FAILURE = new CommandLineOption("-halt",
            "halt on first failure. Default behaviour is to try each component independently",
                                                                               false, true);

    private static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, AUDIT, MAPPING_PATH, VERBOSE, HALT_ON_FIRST_FAILURE};

    public static final CommandLineOption FTP_HOST =
            new CommandLineOption("-ftp_host",
                                    "[Optional] host to ftp backup image to: "+
                                    "host.domain.com:port",
                                     false, false);

    public static final CommandLineOption FTP_USER = new CommandLineOption("-ftp_user",
                                                                               "[Optional] ftp username",
                                                                               false, false);

    public static final CommandLineOption FTP_PASS = new CommandLineOption("-ftp_pass",
                                                                                   "[Optional] ftp password",
                                                                                   false, false);

    private static final CommandLineOption[] ALL_FTP_OPTIONS = {FTP_HOST, FTP_USER, FTP_PASS};

    private static final CommandLineOption[] ALL_IGNORED_OPTIONS = {
            new CommandLineOption("-p", "Ignored parameter for partition", true, false) };

    public static final String SRCPARTNMFILENAME = "sourcepartitionname";
    public static final String NODE_CONF_DIR = "node/default/etc/conf/";
    public static final String CA_JAR_DIR = "runtime/modules/lib";
    public static final String MA_AAR_DIR = "runtime/modules/assertions";

    public static final String POST_FIVE_O_DEFAULT_BACKUP_FOLDER = "config/backup/images";
    /**
     * my.cnf makes up part of a databsae backup. This is the current known path to this file
     */
    private static final String PATH_TO_MY_CNF = "/etc/my.cnf";
    private static final String UNIQUE_TIMESTAMP = "yyyyMMddHHmmss";
    private static final String FTP_PROTOCOL = "ftp://";

    /** Home directory of the SSG installation. This will always be /opt/SecureSpan/Gateway however maintaining
     * the ability for this to be theoritically installed into other directories*/
    private final File ssgHome;

    /** Stream for verbose output; <code>null</code> for no verbose output. */
    private PrintStream stdout;

    /**
     * confDir is a file created from combining ssg_home with NODE_CONF_DIR. This is the directory for the SSGs
     * config files
     */
    private final File confDir;

    private String applianceHome;
    public static final String NO_UNIQUE_IMAGE_SYSTEM_PROP =
            "com.l7tech.gateway.config.backuprestore.nouniqueimagename";

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

    private static interface BackupComponent<E extends Exception>{

        public void doBackup() throws E;

        public ImportExportUtilities.ComponentType getComponentType();
    }

    public static class BackupResult{

        private final String backUpImageName;
        private final Status status;
        private final List<String> failedComponents;
        private final Exception exception;

        public enum Status{
            SUCCESS(),
            FAILURE(),
            PARTIAL_SUCCESS()
        }

        private BackupResult(String backUpImageName, Status status, List<String> failedComponents, Exception exception){
            this.backUpImageName = backUpImageName;
            this.status = status;
            this.failedComponents = failedComponents;
            this.exception = exception;
        }

        private BackupResult(String backUpImageName, Status status){
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
     * @param stdout        stream for verbose output; <code>null</code> for no verbose output
     * @param applianceHome the standard installation directory of the SSG appliance. If this folder exists then
     * OS files will be backed up via backUpComponentOS(). Cannot be null or the empty string
     * @throws NullPointerException if ssgHome or ssgAppliance are null
     * @throws IllegalArgumentException if ssgHome does not exsit or is not a directory, or if applianceHome is the
     * empty string
     */
    public Exporter(final File ssgHome, final PrintStream stdout, String applianceHome) {
        if(ssgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!ssgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!ssgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");
        if(applianceHome == null) throw new NullPointerException("applianceHome cannot be null");
        if(applianceHome.equals("")) throw new IllegalArgumentException("applianceHome cannot be null");
        
        this.ssgHome = ssgHome;
        //this class is not usable without an installed SSG > 5.0
        int [] versionInfo = throwIfLessThanFiveO(new File(ssgHome, "runtime/Gateway.jar"));
        isPostFiveO = versionInfo[2] > 0;

        this.stdout = stdout;

        confDir = new File(ssgHome, NODE_CONF_DIR);
        this.applianceHome = applianceHome;
    }

    /**
     * This constructor is only used with tests.
     * This constructor will not check the for an SSG installation
     * @param ssgHome   home directory where the SSG is installed. For tests this will be a tmp directory. Cannot be null
     * @param stdout    stream for verbose output; <code>null</code> for no verbose output
     * @param applianceHome the standard installation directory of the SSG appliance. If this folder exists then
     * OS files will be backed up via backUpComponentOS(). Cannot be null or the empty string. For testing this should
     * just be any folder which exists, so that the OS backup can be tested
     * @param flagNotUsed just used to make the constructor signature unique
     * @throws IllegalStateException if constructor is used outside of a test environment
     */
    Exporter(final File ssgHome, final PrintStream stdout, String applianceHome, boolean flagNotUsed) {
        File f = new File(ssgHome, "runtime/Gateway.jar");
        if(f.exists()) throw new IllegalStateException("This constructor is only for testing. Not for use in production");
        
        if(ssgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!ssgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!ssgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");
        if(applianceHome == null) throw new NullPointerException("applianceHome cannot be null");
        if(applianceHome.equals("")) throw new IllegalArgumentException("applianceHome cannot be null");

        this.ssgHome = ssgHome;
        isPostFiveO = false;//we won't have the /opt/SecureSpan/Gateway/Backup folder

        this.stdout = stdout;
        confDir = new File(ssgHome, NODE_CONF_DIR);
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
    public BackupResult createBackupImage(final String [] args)
            throws InvalidProgramArgumentException, IOException, BackupRestoreLauncher.FatalException {

        final List<CommandLineOption> validArgList = new ArrayList<CommandLineOption>();
        validArgList.addAll(Arrays.asList(ALLOPTIONS));
        validArgList.addAll(Arrays.asList(ALL_FTP_OPTIONS));
        validArgList.addAll(Arrays.asList(ImportExportUtilities.ALL_COMPONENTS));
        programFlagsAndValues =
                ImportExportUtilities.getAndValidateCommandLineOptions(args,
                        validArgList, Arrays.asList(ALL_IGNORED_OPTIONS));

        validateProgramParameters(programFlagsAndValues);
        final boolean usingFtp = checkAndValidateFtpParams(programFlagsAndValues);

        //overwrite the supplied image name with a unique name based on it
        String pathToUniqueImageFile = getUniqueImageFileName(programFlagsAndValues.get(IMAGE_PATH.name));
        if (stdout != null && isVerbose) stdout.println("Creating image: " + pathToUniqueImageFile);
        programFlagsAndValues.put(IMAGE_PATH.name, pathToUniqueImageFile);
        //We only want to validate the image file when we are not using ftp
        validateFiles(programFlagsAndValues, !usingFtp);

        if(!usingFtp){
            // check that we can write output at location asked for
            pathToUniqueImageFile = ImportExportUtilities.getAbsolutePath(pathToUniqueImageFile);
            validateImageFile(pathToUniqueImageFile);
        }

        //check that node.properties exists
        final File nodePropsFile = new File(confDir, ImportExportUtilities.NODE_PROPERTIES);
        if ( !nodePropsFile.exists() ) {
            throw new IllegalStateException("node.properties must exist in " + nodePropsFile.getAbsolutePath());
        }

        //check whether mapping option was used
        //were doing this here as if it's requested, then we need to be able to create it
        if(programFlagsAndValues.get(MAPPING_PATH.name) != null) {
            //fail if file exists
            ImportExportUtilities.throwIfFileExists(programFlagsAndValues.get(MAPPING_PATH.name));
        }

        String tmpDirectory = null;
        try {
            tmpDirectory = ImportExportUtilities.createTmpDirectory();

            final String mappingFile = programFlagsAndValues.get(MAPPING_PATH.name);
            final FtpClientConfig ftpConfig = getFtpConfig(programFlagsAndValues);
            performBackupSteps(mappingFile, pathToUniqueImageFile, tmpDirectory, ftpConfig);
        } catch(Exception e){
            return new BackupResult(null, BackupResult.Status.FAILURE, null, e);
        } finally {
          if(tmpDirectory != null){
                logger.info("cleaning up temp files at " + tmpDirectory);
                if (stdout != null && isVerbose) stdout.println("Cleaning temporary files at " + tmpDirectory);
                FileUtils.deleteDir(new File(tmpDirectory));
            }
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

        String ignoreProp = System.getProperty(NO_UNIQUE_IMAGE_SYSTEM_PROP);
        if(ignoreProp != null){
            if(Boolean.valueOf(ignoreProp)) return imagePathAndName;
        }
        final SimpleDateFormat dateFormat = new SimpleDateFormat(UNIQUE_TIMESTAMP);
        final Calendar cal = Calendar.getInstance();
        final String uniqueStart = dateFormat.format(cal.getTime());

        String dir = ImportExportUtilities.getDirPart(imagePathAndName);
        String file = ImportExportUtilities.getFilePart(imagePathAndName);
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
    private String getPostFiveOAbsImagePath(String pathToImageFile){
        if(!isPostFiveO) return pathToImageFile;

        String dirPart = ImportExportUtilities.getDirPart(pathToImageFile);
        if(dirPart != null) return pathToImageFile;//path info has been supplied

        File imageFolder = new File(ssgHome, POST_FIVE_O_DEFAULT_BACKUP_FOLDER);
        File imageFile = new File(imageFolder, pathToImageFile);
        return imageFile.getAbsolutePath();
    }
    /**
     * Extract ftp parameters from the programParams and create and return an FtpClientConfig. If no -ftp_* parameters
     * were passed into createBackupImage, then this will return null
     * @param programParams The parameters passed into createBackupImage
     * @return a FtpClientConfig object if ftp params were supplied, null otherwise
     * @throws InvalidProgramArgumentException if any required ftp parameter is missing
     */
    public FtpClientConfig getFtpConfig(final Map<String, String> programParams) throws InvalidProgramArgumentException {
        String ftpHost = programParams.get(FTP_HOST.name);
        if(ftpHost == null) return null;
        if(!ftpHost.startsWith(FTP_PROTOCOL)) ftpHost = FTP_PROTOCOL+ftpHost;
        //as ftp host was supplied, validate all required ftp params exist
        checkAndValidateFtpParams(programParams);

        final String ftpUser = programParams.get(FTP_USER.name);
        final String ftpPass = programParams.get(FTP_PASS.name);
        if(ftpUser == null || ftpPass == null) throw new NullPointerException("ftp_user and ftp_pass must be non null");

        URL url;
        try {
            url = new URL(ftpHost);
        } catch (MalformedURLException e) {
            //won't happen due to above check
            throw new InvalidProgramArgumentException(e.getMessage());
        }

        final FtpClientConfig ftpConfig = FtpClientConfigImpl.newFtpConfig(url.getHost());
        ftpConfig.setPort(url.getPort());
        ftpConfig.setUser(ftpUser);
        ftpConfig.setPass(ftpPass);

        final String imageName = programParams.get(IMAGE_PATH.name);
        final String dirPart = ImportExportUtilities.getDirPart(imageName);
        if(dirPart != null){
            ftpConfig.setDirectory(dirPart);
        }
        return ftpConfig;
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
     * @param pathToImageZip String representing the path (optional) and name of the image zip file to create. If
     * the image is going to be ftp'd then any path information is relative to the ftp server
     * @param tmpOutputDirectory String the temporary directory created to host the back up of each component before
     * the image zip file is created
     * @param ftpConfig FtpClientConfig if ftp is required, Pass <code>null</code> when ftp is not required
     * @throws Exception for any Exception when backing up the components or when creating the zip file
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.FatalException if ftp is requested and
     * its not possible to ftp the newly created image
     */
    private void performBackupSteps(final String mappingFile, final String pathToImageZip,
                                    final String tmpOutputDirectory, final FtpClientConfig ftpConfig)
            throws Exception {

        List<BackupComponent<? extends Exception>> compsToBackup =
                getComponentsForBackup(mappingFile, tmpOutputDirectory);

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

        //when we are using ftp, we need to store the image file somewhere locally
        //not using the same temp directory as the image data as it causes recursive problems when zipping
        if(ftpConfig != null){
            String newTmpDir = null;
            try{
                newTmpDir = ImportExportUtilities.createTmpDirectory();
                //What is just the file name? We will use just the file name, and create the zip in the tmp directory
                String zipFileName = ImportExportUtilities.getFilePart(pathToImageZip);
                zipFileName = newTmpDir+File.separator+zipFileName;                
                createImageZip(zipFileName, tmpOutputDirectory);
                ftpImage(zipFileName, pathToImageZip, ftpConfig);
            }finally{
                if(newTmpDir != null){
                    logger.info("cleaning up temp files at " + newTmpDir);
                    if (stdout != null && isVerbose) stdout.println("Cleaning temporary files at " + newTmpDir);
                    FileUtils.deleteDir(new File(newTmpDir));
                }
            }
        }else{
            createImageZip(pathToImageZip, tmpOutputDirectory);
        }
    }

    /**
     * <p>
     * All of the backUp* methods are public and can be used as an API for Exporter. However when createBackupImage()
     * is used, it promises to do a non fail fast backup. This means that we will try and back up each applicable
     * component individually and independently of others. As each backUp* method can throw Exceptions, this is a
     * convenience method to wrap each component to be backed up in a Function.NullaryVoidThrows(Exception) and to
     * return them in a Map, which can then simply be iterated over with a very simple and clean try / catch structure.
     * </p>
     *
     * <p>
     * The returned data structure is a Map, as it's nice to be able to report in the logs at a higher level, which
     * component failed to back up. If any other information is required, or if the exception needs to change from
     * IOException, then this generic use of Functions.NullaryVoidThrows can be promoted to using a new interface of
     * its own, for this task of wrapping calls to these functions.
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
     * @param tmpOutputDirectory where the components should write their backup to
     * @return A ordered Map of a Function.NullaryVoidThrows to a string description. Clients can iterate over this map
     * and call call() to back up the component its wrapping
     * @throws IOException if any exception ocurs reading node.properties to get database information. This is always
     * done to determine if the database is local or remote
     */
    private List<BackupComponent<? extends Exception>> getComponentsForBackup(
            final String mappingFile,
            final String tmpOutputDirectory)
            throws IOException {

        List<BackupComponent<? extends Exception>> componentList = new ArrayList<BackupComponent<? extends Exception>>();

        BackupComponent<IOException> versionComp = new BackupComponent<IOException>() {
            public void doBackup() throws IOException {
                // record version of this image
                backUpVersion(tmpOutputDirectory);
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

            BackupComponent<IOException> dbComp = new BackupComponent<IOException>() {
                public void doBackup() throws IOException {
                    //this will also create the mapping file if it was requested
                    backUpComponentMainDb(mappingFile, tmpOutputDirectory, config);
                }

                public ImportExportUtilities.ComponentType getComponentType() {
                    return ImportExportUtilities.ComponentType.MAINDB;
                }
            };
            componentList.add(dbComp);
            // check whether or not we are expected to include audit in export
            if (includeAudits) {
                BackupComponent<Exception> auditComp = new BackupComponent<Exception>() {
                    public void doBackup() throws IOException {
                        backUpComponentAudits(tmpOutputDirectory, config);
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

        BackupComponent<Exception> configComp = new BackupComponent<Exception>() {
            public void doBackup() throws IOException {
                backUpComponentConfig(tmpOutputDirectory);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CONFIG;
            }
        };
        componentList.add(configComp);

        BackupComponent<Exception> osComp = new BackupComponent<Exception>() {
            public void doBackup() throws IOException {
                //restore OS files if this is an appliance
                backUpComponentOS(tmpOutputDirectory);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.OS;
            }
        };
        componentList.add(osComp);

        BackupComponent<Exception> caComp = new BackupComponent<Exception>() {
            public void doBackup() throws IOException {
                backUpComponentCA(tmpOutputDirectory);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.CA;
            }
        };

        componentList.add(caComp);

        BackupComponent<Exception> maComp = new BackupComponent<Exception>() {
            public void doBackup() throws IOException {
                backUpComponentMA(tmpOutputDirectory);
            }

            public ImportExportUtilities.ComponentType getComponentType() {
                return ImportExportUtilities.ComponentType.MA;
            }
        };
        componentList.add(maComp);

        return filterComponents(componentList);
    }

    private List<BackupComponent<? extends Exception>> filterComponents(List<BackupComponent<? extends Exception>> allComponents){

        if(!isSelectiveBackup){
            return allComponents;
        }

        if(stdout != null && isVerbose) stdout.println("Performing a selective backup");
        logger.log(Level.INFO, "Performing a selective backup");

        List <BackupComponent<? extends Exception>>
                returnList = new ArrayList<BackupComponent<? extends Exception>>();

        for(BackupComponent<? extends Exception> comp: allComponents){
            if(comp.getComponentType() == ImportExportUtilities.ComponentType.VERSION){
                //We always include the version component
                returnList.add(comp);
                continue;
            }

            if(programFlagsAndValues.containsKey("-"+comp.getComponentType().getComponentName())){
                returnList.add(comp);
            } 
        }

        return returnList;
    }

    /**
     * Ftp a local image zip file to a ftp server
     * @param localZipFile The local image zip file. This String includes the path and the file name. Cannot be null
     * @param destPathAndFileName The file name including path info if required, of where the file should be uploaded
     * to on the ftp server. The filename will have a timestamp in the format "yyyyMMddHHmmss_" prepended to the
     * front of the file name 
     * @param ftpConfig the configuration for the ftp server to upload the localZipFile to
     * @throws BackupRestoreLauncher.FatalException if any ftp exception occurs
     * @throws FileNotFoundException if the localZipFile cannot be found
     * @throws NullPointerException if any parameter is null. All are required
     * @throws IllegalArgumentException if any String param is the emtpy string
     */
    public void ftpImage(final String localZipFile, final String destPathAndFileName, final FtpClientConfig ftpConfig)
            throws BackupRestoreLauncher.FatalException, FileNotFoundException {
        if(localZipFile == null) throw new NullPointerException("localZipFile cannot be null");
        if(localZipFile.equals("")) throw new IllegalArgumentException("localZipFile cannot equal the empty string");
        if(destPathAndFileName == null) throw new NullPointerException("destPathAndFileName cannot be null");
        if(destPathAndFileName.equals("")) throw new IllegalArgumentException("destPathAndFileName cannot equal the empty string");
        if(ftpConfig == null) throw new NullPointerException("ftpConfig cannot be null");

        InputStream is = null;
        try {
            is = new FileInputStream(new File(localZipFile));
            if (stdout != null && isVerbose)
                stdout.println("Ftp file '" + localZipFile+"' to host '" + ftpConfig.getHost()+"' into directory '"
                        + destPathAndFileName+"'");

            final String filePart = ImportExportUtilities.getFilePart(destPathAndFileName);
            FtpUtils.upload(ftpConfig, is, filePart, true);
        } catch (FtpException e) {
            throw new BackupRestoreLauncher.FatalException(e.getMessage());
        } finally{
            ResourceUtils.closeQuietly(is);
        }
    }

    /**
     * This method will back up database audits. Audits are written directly from memory into a gzip file to
     * conserve space. The file created is called "audits.gz" and is contained inside the "audits" folder in
     * the tmpOutputDirectory
     * //todo [Donal] implement heuristic for determine backup space for audits here
     * //todo [Donal] make audits interruptable - error depending on -halt status
     *
     * Note: The sql created in the file "audits.gz" will NOT contain create and drop statements. The drop and
     * create statments are created by addDatabaseToBackupFolder. When restoring or migrating the audits tables must
     * always be dropped and recreated. As a result the "audits.gz" file cannot be loaded into a database until
     * the audit tables have been recreated.
     *
     * Audits can only be backed up if the database is local. If it's not and this method is called an
     * IllegalStateException will be thrown. Call isHostLocal() to see if the database is local or not
     * @param tmpOutputDirectory The name of the directory to back the audits file up into. They will be in the
     * folder representing this component "audits"
     * @param config DatabaseConfig object used for connecting to the database
     * @throws IOException if any exception occurs writing the database back up files
     */
    public void backUpComponentAudits(final String tmpOutputDirectory, final DatabaseConfig config) throws IOException {
        if(!ImportExportUtilities.isHostLocal(config.getHost())){
            logger.log(Level.WARNING, "Cannot backup database as it is not local");
            throw new IllegalStateException("Cannot back up database as it is not local");
        }
        try {
            //Create the database folder
            final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.AUDITS.getComponentName());
            //never include audits with the main db dump
            DBDumpUtil.auditDump(ssgHome, config, dir.getAbsolutePath(), stdout, isVerbose);
        } catch (SQLException e) {
            logger.log(Level.INFO, "exception dumping database, possible that the database is not running or credentials " +
                    "are not correct", ExceptionUtils.getDebugException(e));
            throw new IOException("cannot dump the database, please ensure the database is running and the credentials are correct");
        }
    }

    /**
     * This method will backup the main db excluding any audit tables. If mappingFile is not null then a mapping file
     * will be created providing a template to specify the mapping of cluster property values and ip address values
     * from the system being backed up to the system being restored. This method will also back up my.cnf
     *
     * Note: When using the backup image to import, if -migrate is not used, this template file is ignored.
     *
     * If the database is not local and this method is called, then an IllegalState exception will be thrown. To
     * determine if a database is local nor not, call isHostLocal()
     *
     * @param mappingFile name of the mapping file. Can include path information. Can be null when it is not required
     * @param tmpOutputDirectory The name of the directory to back the database file up into. They will be in the
     * folder representing this component "maindb" for the db
     * @param config DatabaseConfig object used for connecting to the database
     * @throws IOException if any exception occurs writing the database back up files
     */
    public void backUpComponentMainDb(final String mappingFile, final String tmpOutputDirectory,
                                      final DatabaseConfig config) throws IOException {
        final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.MAINDB.getComponentName());
        addDatabaseToBackupFolder(dir, config);
        // produce template mapping if necessary
        if(mappingFile != null){
            final String mappingFileName = ImportExportUtilities.getAbsolutePath(mappingFile);
            createMappingFile(mappingFileName, config);
        }

        //add my.cnf
        final File file = new File(PATH_TO_MY_CNF);
        if ( file.exists() && !file.isDirectory()) {
            FileUtils.copyFile(file, new File(dir.getAbsolutePath() + File.separator + file.getName()));
        }else{
            logger.log(Level.WARNING, "Cannot backup my.cnf");
        }
    }

    /**
     * Create a file called 'version' in the folder tmpOutputDirectory. This file lists the current version of the
     * SSG software installed where this program is ran
     * @param tmpOutputDirectory folder to create version file in
     * @throws IOException if tmpOutputDirectory is null, doesn't exist or is not a directory
     */
    public void backUpVersion(final String tmpOutputDirectory) throws IOException {
        ImportExportUtilities.verifyDirExistence(tmpOutputDirectory);

        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(tmpOutputDirectory + File.separator + ImportExportUtilities.VERSION);
            fos.write( BuildInfo.getProductVersion().getBytes());
        } finally{
            ResourceUtils.closeQuietly(fos);            
        }
    }

    /**
     * This method backs up the SSG config files node.properties, ssglog.properties, system.properties and omp.dat
     * from the node/default/etc/conf folder into the "config" directory under tmpOutputDirectory
     * @param tmpOutputDirectory The name of the directory to back the SSG config files into. They will be in the
     * folder representing this component "config"
     * @throws IOException if any exception occurs writing the SSG config files to the backup folder
     */
    public void backUpComponentConfig(final String tmpOutputDirectory) throws IOException {
        final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.CONFIG.getComponentName());

        ImportExportUtilities.copyFiles(confDir, dir, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                for(String ssgFile: ImportExportUtilities.CONFIG_FILES){
                    if(ssgFile.equals(name)) return true;
                }
                return false;
            }
        });
    }

    /**
     * This method backs up OS files listed in the file backup_manifest, in the config/backup/cfg folder, into the
     * "os" directory under tmpOutputDirectory. The file backup_manifest only exists when the appliance is installed.
     * In addition when the appliance is not installed this method will do nothing.
     *
     * @param tmpOutputDirectory The name of the directory to back the OS files into. They will be in the
     * folder representing this component "os"
     * @throws IOException if any exception occurs writing the OS files to the backup folder
     */
    public void backUpComponentOS(final String tmpOutputDirectory) throws IOException {
        if (new File(applianceHome).exists()) {
            // copy system config files
            final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.OS.getComponentName());
            OSConfigManager.saveOSConfigFiles(dir.getAbsolutePath(), ssgHome);
        }
        //there is no else as backing up os files is not a user option. It just happens when were on an appliance
    }

    /**
     * This method backs up custom assertions and their associated property files to the ca folder under
     * tmpOutputDirectory
     *
     * Custom assertion jars are stored in runtime/modules/lib and their property files are kept in node/default/etc/conf
     *
     * @param tmpOutputDirectory The name of the directory to back the custom assertions and property files into.
     * They will be in the folder representing this component "ca"
     * @throws IOException if any exception occurs writing the custom assertions and property files to the backup folder
     */
    public void backUpComponentCA(final String tmpOutputDirectory) throws IOException {
        final File dir =
                createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.CA.getComponentName());

        //backup all .property files in the conf folder which are not in CONFIG_FILES
        ImportExportUtilities.copyFiles(confDir, dir, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                if(!name.endsWith(".properties")) return false;

                for(String ssgFile: ImportExportUtilities.CONFIG_FILES){
                    if(ssgFile.equals(name)) return false;
                }
                return true;
            }
        });

        //back up all jar files in /opt/SecureSpan/Gateway/runtime/modules/lib
        ImportExportUtilities.copyFiles(new File(ssgHome, CA_JAR_DIR), dir, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                return name.endsWith(".jar");
            }
        });
    }

    /**
     * This method backs up modular assertions to the ma folder under tmpOutputDirectory
     *
     * Custom assertion jars are stored in runtime/modules/assertions
     *
     * @param tmpOutputDirectory The name of the directory to back the modular assertions into.
     * They will be in the folder representing this component "ma"
     * @throws IOException if any exception occurs writing the modular assertions to the backup folder
     */
    public void backUpComponentMA(final String tmpOutputDirectory) throws IOException {
        final File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ComponentType.MA.getComponentName());

        //back up all jar files in /opt/SecureSpan/Gateway/runtime/modules/assertions
        ImportExportUtilities.copyFiles(new File(ssgHome, MA_AAR_DIR), dir, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                return name.endsWith(".aar");
            }
        });
    }

    /**
     * Create an image zip archive from all the files and folders in tmpOutputDirectory.
     * @param zipFileName The name of the zip file to create
     * @param tmpOutputDirectory The folder to zip
     * @throws IOException if any exception occurs while creating the zip
     */
    public void createImageZip(final String zipFileName, final String tmpOutputDirectory) throws IOException {
        logger.info("compressing image into " + zipFileName);
        final File dirObj = new File(tmpOutputDirectory);
        if (!dirObj.isDirectory()) {
            throw new IOException(tmpOutputDirectory + " is not a directory");
        }
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        if (stdout != null && isVerbose) stdout.println("Compressing SecureSpan Gateway image into " + zipFileName);
        final StringBuilder sb = new StringBuilder();
        addDir(dirObj, out, tmpOutputDirectory, sb);

        //Add the manifest listing all files in the archive to the archive
        final File manifest = new File(tmpOutputDirectory + File.separator + ImportExportUtilities.MANIFEST_LOG);
        manifest.createNewFile();
        final FileOutputStream fos = new FileOutputStream(manifest);
        fos.write(sb.toString().getBytes());
        fos.close();

        addZipFileToArchive(out, tmpOutputDirectory, manifest, sb);

        out.close();
    }

    /**
     * Classloader designed to ONLY to load BuildInfo from Gateway jar from the standard install directory of
     * /opt/SecureSpan/Gateway/runtime/Gateway.jar
     */
    public static class GatewayJarClassLoader extends ClassLoader{
        private ClassLoader loader;

        public GatewayJarClassLoader(File gatewayJarFile) throws MalformedURLException {
            URL gatewayJar = gatewayJarFile.toURI().toURL();
            loader = new URLClassLoader(new URL[]{gatewayJar}, null);
        }

        public Class<?> loadClass(String name) throws ClassNotFoundException {
            if(name.equals("com.l7tech.util.BuildInfo")){
                return loader.loadClass(name);
            }
            throw new ClassNotFoundException(GatewayJarClassLoader.class.getName()+" only supports class BuildInfo");
        }
    }

    /**
     * Are we running on a pre 5.0 system? If so a BackupRestoreLauncher.FatalException is thrown. This method will
     * return the version of the SSG installed
     * @param gatewayJarFile File representing Gateway.jar, from the local SSG installation
     * @return an int arrary with the major, minor and subversion values for the installed SSG as indexs 0, 1 and 2
     * @throws RuntimeException if the SSG installation cannot be found, based on the existence of Gateway.jar. Also
     * thrown if it's not possible to determine the SSG version from the SSG installation
     */
    int [] throwIfLessThanFiveO(File gatewayJarFile){
        try {
            if(!gatewayJarFile.exists()) throw new RuntimeException("Cannot find SSG installation");
            
            GatewayJarClassLoader gatewayJarClassLoader = new GatewayJarClassLoader(gatewayJarFile);
            Class clazz = gatewayJarClassLoader.loadClass("com.l7tech.util.BuildInfo");

            Method method = clazz.getMethod("getProductVersionMajor");
            Object test = method.invoke(clazz);
            int majorVersion = Integer.parseInt(test.toString());

            method = clazz.getMethod("getProductVersionMinor");
            test = method.invoke(clazz);
            int minorVersion = Integer.parseInt(test.toString());

            method = clazz.getMethod("getProductVersionSubMinor");
            test = method.invoke(clazz);
            int subMinorVersion = Integer.parseInt(test.toString());

            if(majorVersion < 5) throw new UnsupportedOperationException("Pre 5.0 SSG installations are not supported");

            return new int[]{majorVersion, minorVersion, subMinorVersion};

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (NoSuchMethodException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Cannot determine SSG version: " + e.getMessage() );
            throw new RuntimeException("Cannot determine SSG version: " + e.getMessage());
        }
    }
    
    /**
     * Add any arbitrary file to the zip archive being created. This is used to add the manifest.log file to the archive
     * @param out The zip archive to add the file to. It must be open
     * @param tmpOutputDirectory The directory the zip archieve is archiving. The fileToAdd should be in this directory
     * @param fileToAdd the file to add to the zip. This file should be in tmpOutputDirectory
     * @param sb the StringBuilder to append to, so that a record of each file archived is made.
     * @throws IOException if any exception occurs while writing to the zip archive
     * @throws IllegalStateException if the fileToAdd does not reside in tmpOutputDirectory
     */
    private void addZipFileToArchive(final ZipOutputStream out, final String tmpOutputDirectory, final File fileToAdd,
                                     final StringBuilder sb)
            throws IOException {
        final byte[] tmpBuf = new byte[1024];
        final FileInputStream in = new FileInputStream(fileToAdd.getAbsolutePath());
        if (stdout != null && isVerbose) stdout.println("\t- " + fileToAdd.getAbsolutePath());
        String zipEntryName = fileToAdd.getAbsolutePath();
        if (zipEntryName.startsWith(tmpOutputDirectory)) {
            zipEntryName = zipEntryName.substring(tmpOutputDirectory.length() + 1);
        }else{
            throw new IllegalStateException("File '"+fileToAdd.getAbsoluteFile()+"' does not exist in directory '"
                    +tmpOutputDirectory+"'");
        }
        out.putNextEntry(new ZipEntry(zipEntryName));
        if(sb != null) sb.append(zipEntryName).append("\n");
        // Transfer from the file to the ZIP fileToAdd
        int len;
        while ((len = in.read(tmpBuf)) > 0) {
            out.write(tmpBuf, 0, len);
        }
        // Complete the entry
        out.closeEntry();
        in.close();
    }

    /**
     * This method is intentionally private. This method represents one step in the backing up of the database component,
     * and as a result it's private. The public method to back up the database component is addDbBackupToBackupFolder()
     *
     * @param dbTmpOutputDirectory The directory to back up the database to. This will create the file main_backup.sql
     * in the folder "maindb"
     * @param config DatabaseConfig object used for connecting to the database
     * @throws IOException if any exception occurs writing the database back up files
     * @throws IllegalThreadStateException if the database is not local
     */
    private void addDatabaseToBackupFolder(final File dbTmpOutputDirectory, final DatabaseConfig config)
            throws IOException {
        if(!ImportExportUtilities.isHostLocal(config.getHost())){
            logger.log(Level.WARNING, "Cannot backup database as it is not local");
            throw new IllegalStateException("Cannot back up database as it is not local");
        }
        try {
            DBDumpUtil.dump(config, dbTmpOutputDirectory.getAbsolutePath(), stdout, isVerbose);
        } catch (SQLException e) {
            logger.log(Level.INFO, "exception dumping database, possible that the database is not running or credentials " +
                    "are not correct", ExceptionUtils.getDebugException(e));
            throw new IOException("cannot dump the database, please ensure the database is running and the credentials are correct");
        }
    }

    /**
     * Create the directory with the name componentName in the directory tmpOutputDirectory. This will delete any
     * existing directory if it exists and recreate it, otherwise it just creates the directory
     * @param tmpOutputDirectory String The directory where the componentName should be created
     * @param componentName String The name of the new directory
     * @return File representing the created directory
     * @throws IOException if any exception occurs while creating the directory
     */
    private File createComponentDir(final String tmpOutputDirectory, final String componentName) throws IOException {
        final File dir = new File(tmpOutputDirectory, componentName);
        if(dir.exists()){
            dir.delete();
        }
        dir.mkdir();
        return dir;
    }

    /**
     * Create a mapping file which can be used when migrating tables.
     * @param mappingFileName The path(optional) and name of the mapping file to create
     * @param config The DatabaseConfig used to connect to the database
     * @throws IOException if any exception occurs when creating the mapping file
     */
    private void createMappingFile(final String mappingFileName, final DatabaseConfig config) throws IOException {
        if(!ImportExportUtilities.isHostLocal(config.getHost())){
            logger.log(Level.WARNING, "Cannot create maping file as database is not local");
            throw new IllegalStateException("Cannot create maping file as database is not local");
        }

        if (mappingFileName != null) {
            if (!testCanWriteSilently(mappingFileName)) {
                throw new IllegalArgumentException("cannot write to the mapping template path provided: " + mappingFileName);
            }
            // read policy files from this dump, collect all potential mapping in order to produce mapping template file
            try {
                MappingUtil.produceTemplateMappingFileFromDB(config, mappingFileName);
            } catch (SQLException e) {
                // should not happen
                logger.log(Level.WARNING, "unexpected problem producing template mapping file ", e);
                throw new RuntimeException("problem producing template mapping file", e);
            } catch (SAXException e) {
                // should not happen
                logger.log(Level.WARNING, "unexpected problem producing template mapping file ", e);
                throw new RuntimeException("problem producing template mapping file", e);
            }
        }
    }

    /**
     * Validate that the image file exists and that we can write to it
     * @param pathToImageFile String representing the relative or absolute path to the image file. Cannot be nul
     * @throws IOException if we cannot write to the pathToImageFile file
     */
    private void validateImageFile(final String pathToImageFile) throws IOException {
        if (pathToImageFile == null) {
            logger.info("No target image path specified");
            throw new NullPointerException("pathToImageFile cannot be null");
        } else {
            //fail if file exists
            ImportExportUtilities.throwIfFileExists(pathToImageFile);
        }

        if (!testCanWriteSilently(pathToImageFile)) {
            throw new IOException("Cannot write image to " + pathToImageFile);
        }
    }

    /**
     * Quietly test if the given path have permissions to write.  This method is similar to
     * testCanWrite except that it will not output any error messages, instead, they will be logged.
     *
     * @param path  Path to test
     * @return  TRUE if can write on the given path, otherwise FALSE.
     */
    private boolean testCanWriteSilently(final String path) {
        try {
            final FileOutputStream fos = new FileOutputStream(path);
            fos.close();
            (new File(path)).delete();
            logger.log(Level.INFO, "Successfully tested write permission for " + path);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Cannot write to " + path + ". ");
            return false;
        }
        return true;
    }

    /**
     * Add a directory to a zip archive. The zip archive should be currently open
     * @param dirObj The directory to add to the archive
     * @param out The currently open ZipOutputStream
     * @param tmpOutputDirectory The root directory being archived
     * @param sb The StringBuilder used to record each file archived
     * @throws IOException if any exception occurs when writing to the zip archive
     */
    private void addDir(final File dirObj, final ZipOutputStream out, final String tmpOutputDirectory,
                        final StringBuilder sb) throws IOException {
        final File[] files = dirObj.listFiles();

        for (final File file : files) {
            if (file.isDirectory()) {
                addDir(file, out, tmpOutputDirectory, sb);
                continue;
            }
            addZipFileToArchive(out, tmpOutputDirectory, file, sb);
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
        if (!args.containsKey(IMAGE_PATH.name)) {
            throw new InvalidProgramArgumentException("missing option " + IMAGE_PATH.name + ", required for exporting image");
        } 

        //check if ftp requested
        checkAndValidateFtpParams(args);

        //check if node.properties file exists
        final File configDir = new File(ssgHome, NODE_CONF_DIR);
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
        if(args.containsKey(VERBOSE.name)) isVerbose = true;

        if(args.containsKey(HALT_ON_FIRST_FAILURE.name)) isHaltOnFirstFailure = true;

        //determine if we are doing a selective backup
        isSelectiveBackup = isSelectiveBackup(args);

        //the decision to back up audits can come from the -ia flag or from the audits flag
        //-ia means backup audits with a full backup
        //-audits means only audits, or if other components are selected then it meas 'also audits'
        String auditVal = programFlagsAndValues.get(AUDIT.name);
        if (auditVal != null && !auditVal.toLowerCase().equals("no") && !auditVal.toLowerCase().equals("false")) {
            includeAudits = true;
        }else {
            includeAudits =  programFlagsAndValues.containsKey("-" + ImportExportUtilities.ComponentType.AUDITS.getComponentName());
        }
    }

    /**
     * If any of the options from ImportExportUtilities.ALL_COMPONENTS) is included in the args, return true
     * as we are doing a selective backup
     * @param args program parameters converted to a map of string to their values
     * @return true if a selective backup should be done, false otherwise
     */
    private boolean isSelectiveBackup(final Map<String, String> args){
        for(CommandLineOption option: ImportExportUtilities.ALL_COMPONENTS){
            if(args.containsKey(option.name)) return true;
        }
        return false;
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
            ImportExportUtilities.throwIfFileExists(args.get(IMAGE_PATH.name));   //test that the file is a new file
            ImportExportUtilities.verifyCanWriteFile(args.get(IMAGE_PATH.name));  //test if we can create the file
        }

        //check condition for mapping file
        if (args.containsKey(MAPPING_PATH.name)) {
            ImportExportUtilities.throwIfFileExists(args.get(MAPPING_PATH.name)); //test that the file is a new file
            ImportExportUtilities.verifyCanWriteFile(args.get(MAPPING_PATH.name));    //test if we can create the file
        }
    }
    /**
     * Validae the ftp program parameters. If any ftp param is supplied, then they all must. This method enforces that
     * constraint. If all ftp params are supplied then true is returned, otherwise false
     * @param allParams map of program parameters
     * @return true if ftp parameters were supplied and they all exist. False if NO ftp params were supplied
     * @throws InvalidProgramArgumentException if an incomplete set of ftp parameters were supplied, or if the ftp
     * host name is invalid
     */
    public boolean checkAndValidateFtpParams(final Map<String, String> allParams) throws InvalidProgramArgumentException {
        //check if ftp requested
        for(Map.Entry<String, String> entry: allParams.entrySet()){
            if(entry.getKey().startsWith("-ftp")){
                //make sure they are all there
                for(final CommandLineOption clo: ALL_FTP_OPTIONS){
                    if(!allParams.containsKey(clo.name)) throw new InvalidProgramArgumentException("Missing argument: " + clo.name);
                    if(clo == FTP_HOST){
                        String hostName = allParams.get(FTP_HOST.name);
                        try {
                            if(!hostName.startsWith(FTP_PROTOCOL)) hostName = FTP_PROTOCOL +hostName;
                            final URL url = new URL(hostName);
                            if(url.getPort() == -1)
                                throw new InvalidProgramArgumentException("-ftp_host value requires a port number");
                        } catch (MalformedURLException e) {
                            throw new InvalidProgramArgumentException(e.getMessage());
                        }
                    }
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Get the usage for the Exporter utility. Usage information is written to the StringBuilder output
     * @param output StringBuilder to write the usage information to
     */
    public static void getExporterUsage(final StringBuilder output) {

        final List<CommandLineOption> allOptList = new ArrayList<CommandLineOption>();
        allOptList.addAll(Arrays.asList(ALLOPTIONS));
        allOptList.addAll(Arrays.asList(ALL_FTP_OPTIONS));
        allOptList.addAll(Arrays.asList(ImportExportUtilities.ALL_COMPONENTS));

        int largestNameStringSize;
        largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(allOptList);
        for (final CommandLineOption option : allOptList) {
            output.append("\t")
                    .append(option.name)
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.name.length() + 1))
                    .append(option.description)
                    .append(BackupRestoreLauncher.EOL_CHAR);
        }

        output.append("FTP options are optional. If FTP is requested, then all ftp parameters must be supplied");
    }
}