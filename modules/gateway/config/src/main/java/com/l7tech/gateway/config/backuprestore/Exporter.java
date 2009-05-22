package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.*;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.sql.SQLException;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;
import org.xml.sax.SAXException;


/**
 * Exporter manages the creation of a complete SSG backup. Calling createBackupImage() will create a zip file
 * containing all components which constitute a complete backup.
 * See http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_Backup_Restore_Func_Spec and
 * http://sarek.l7tech.com/mediawiki/index.php?title=Buzzcut_Backup_Restore_Design
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class Exporter{

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
    public static final CommandLineOption[] ALLOPTIONS = {IMAGE_PATH, AUDIT, MAPPING_PATH};

    public static final CommandLineOption[] ALL_IGNORED_OPTIONS = {
            new CommandLineOption("-p", "Ignored parameter for partition", true, false) };

    public static final String VERSIONFILENAME = "version";
    public static final String SRCPARTNMFILENAME = "sourcepartitionname";
    public static final String OMP_DAT_FILE = "omp.dat";
    public static final String NODE_PROPERTIES_FILE = "node.properties";
    public static final String NODE_CONF_DIR = "node/default/etc/conf/";
    public static final String CA_JAR_DIR = "runtime/modules/lib";
    public static final String MA_AAR_DIR = "runtime/modules/assertions";

    /**
     * These configuration files are expected in ssg_home/node/default/etc/conf/ and constitute
     * the complete backing up of SSG configuration files
     */
    private static final String[] CONFIG_FILES = new String[]{
        "ssglog.properties",
        "system.properties",
        "node.properties",
        "omp.dat"
    };

    /** Home directory of the SSG installation. This will always be /opt/SecureSpan/Gateway however maintaining
     * the ability for this to be theoritically installed into other directories*/
    private File ssgHome;

    /** Stream for verbose output; <code>null</code> for no verbose output. */
    private PrintStream stdout;

    /**
     * confDir is a file created from combining ssg_home with NODE_CONF_DIR. This is the directory for the SSGs
     * config files
     */
    private final File confDir;

    /**
     * Used to test if the node has the appliance installed
     */
    private static final String OPT_SECURE_SPAN_APPLIANCE = "/opt/SecureSpan/Appliance";

    /**
     * my.cnf makes up part of a databsae backup. This is the current known path to this file
     */
    private static final String PATH_TO_MY_CNF = "/etc/my.cnf";

    /**
     * @param ssgHome   home directory where the SSG is installed. Should equal /opt/SecureSpan/Gateway. Cannot be null
     * @param stdout        stream for verbose output; <code>null</code> for no verbose output
     */
    public Exporter(final File ssgHome, final PrintStream stdout) {
        if(ssgHome == null) throw new NullPointerException("ssgHome cannot be null");
        if(!ssgHome.exists()) throw new IllegalArgumentException("ssgHome directory does not exist");
        if(!ssgHome.isDirectory()) throw new IllegalArgumentException("ssgHome must be a directory");

        this.ssgHome = ssgHome;
        this.stdout = stdout;
        confDir = new File(ssgHome, NODE_CONF_DIR);
    }

    /**
     * Create the backup image zip file.
     * The following arguments are expected in the array args:
     * <pre>
     * -image    location of image file to export. Value required
	 * -ia       to include audit tables. No value reuired
	 * -it       path of the output mapping template file. Value required
     * </pre>
     * @param args array of all command line arguments
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException If any of the required program parameters are not supplied
     * @throws IOException Any IOException which occurs while creating the image zip file
     * @throws IllegalStateException if node.properties is not found. This must exist as we only back up a
     * correctly configured SSG node
     */
    public void createBackupImage(String [] args)
            throws InvalidProgramArgumentException, IOException{

        Map<String, String> programFlagsAndValues = ImportExportUtilities.getParameters(args, Arrays.asList(ALLOPTIONS),
                Arrays.asList(ALL_IGNORED_OPTIONS));

        validateProgramParameters(programFlagsAndValues);
        // check that we can write output at location asked for
        String pathToImageFile = ImportExportUtilities.getAbsolutePath(programFlagsAndValues.get(IMAGE_PATH.name));
        String absolutePathToImageFile = validateImageFile(pathToImageFile);

        //check that node.properties exists
        File nodePropsFile = new File(confDir, NODE_PROPERTIES_FILE);
        if ( !nodePropsFile.exists() ) {
            throw new IllegalStateException("node.properties must exist in " + nodePropsFile.getAbsolutePath());
        }

        //check whether mapping option was used
        //were doing this here as if it's requested, then we need to be able to create it
        if(programFlagsAndValues.get(MAPPING_PATH.name) != null) {
            //fail if file exists
            ImportExportUtilities.verifyFileExistence(programFlagsAndValues.get(MAPPING_PATH.name), true);
        }

        String tmpDirectory = null;
        try {
            tmpDirectory = ImportExportUtilities.createTmpDirectory();
            String auditval = programFlagsAndValues.get(AUDIT.name);
            boolean includeAudits = false;
            if (auditval != null && !auditval.toLowerCase().equals("no") && !auditval.toLowerCase().equals("false")) {
                includeAudits = true;
            }

            String mappingFile = programFlagsAndValues.get(MAPPING_PATH.name);
            performBackupSteps(includeAudits, mappingFile, absolutePathToImageFile, tmpDirectory);
        } finally {
            if(tmpDirectory != null){
                logger.info("cleaning up temp files at " + tmpDirectory);
                if (stdout != null) stdout.println("Cleaning temporary files at " + tmpDirectory);
                FileUtils.deleteDir(new File(tmpDirectory));
            }
        }
    }

    /**
     * Backs up each required component and places it into the tmpOutputDirectory directory, in the correct folder.
     * This method orchestrates the calls to the various addXXXToBackupFolder methods
     * @param includeAudits boolean true if audits are to be backed up, false if not
     * @param mappingFile path (optional) and name of the mapping file to be created. if not required pass <code>null</code>
     * @param pathToImageZip String representing the path (optional) and name of the image zip file to create
     * @param tmpOutputDirectory String the temporary directory created to host the back up of each component before
     * the image zip file is created
     * @throws IOException for any IO Exception when backing up the components or when creating the zip file
     */
    private void performBackupSteps(boolean includeAudits, String mappingFile, String pathToImageZip, String tmpOutputDirectory)
            throws IOException{

        // record version of this image
        backUpVersion(tmpOutputDirectory);

        //Back up the database
        File nodePropsFile = new File(confDir, NODE_PROPERTIES_FILE);
        File ompFile = new File(confDir, OMP_DAT_FILE);
        // Read database connection settings
        DatabaseConfig config = getNodeConfig(nodePropsFile, ompFile);

        //Backup database info if the db is local
        if(isDbLocal(config.getHost())){
            //this will also create the mapping file if it was requested
            backUpComponentMainDb(mappingFile, tmpOutputDirectory, config);
            // check whether or not we are expected to include audit in export
            if (includeAudits) {
                backUpComponentAudits(tmpOutputDirectory, config);
            }
        }else{
            logger.log(Level.INFO,  "Database is not local so no backup of database being created");
        }

        backUpComponentConfig(tmpOutputDirectory);

        //restore OS files if this is an appliance
        backUpComponentOS(tmpOutputDirectory);

        backUpComponentCA(tmpOutputDirectory);

        backUpComponentMA(tmpOutputDirectory);

        // zip the temp directory into the requested image file (pathToImageZip)
        createImageZip(pathToImageZip, tmpOutputDirectory);
    }

    /**
     * Determine if the given host is local. Any exceptions are logged and false returned.
     * @param host String name of the host to check if it is local or not
     * @return true if host is local. False otherwise. False if any exception occurs when looking up the host.
     */
    public boolean isDbLocal(String host) {
        try{
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress( InetAddress.getByName(host) );
            if ( networkInterface != null ) return true;
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING,  "Could not look up database host: " + e.getMessage());
        } catch (SocketException e) {
            logger.log(Level.WARNING,  "Socket exception looking up database host: " + e.getMessage());
        }
        return false;
    }

    /**
     * This method will back up database audits. Audits are written directly from memory into a gzip file to
     * conserve space. The file created is called "audits.gz" and is contained inside the "audits" folder in
     * the tmpOutputDirectory
     * //todo [Donal] implement heuristic for determine backup space for audits here
     *
     * Note: The sql created in the file "audits.gz" will NOT contain create and drop statements. The drop and
     * create statments are created by addDatabaseToBackupFolder. When restoring or migrating the audits tables must
     * always be dropped and recreated. As a result the "audits.gz" file cannot be loaded into a database until
     * the audit tables have been recreated.
     *
     * Audits can only be backed up if the database is local. If it's not and this method is called an
     * IllegalStateException will be thrown. Call isDbLocal() to see if the database is local or not
     * @param tmpOutputDirectory The name of the directory to back the audits file up into. They will be in the
     * folder representing this component "audits"
     * @param config DatabaseConfig object used for connecting to the database
     * @throws IOException if any exception occurs writing the database back up files
     */
    public void backUpComponentAudits(String tmpOutputDirectory, DatabaseConfig config) throws IOException {
        if(!isDbLocal(config.getHost())){
            logger.log(Level.WARNING, "Cannot backup database as it is not local");
            throw new IllegalStateException("Cannot back up database as it is not local");
        }
        try {
            //Create the database folder
            File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ImageDirectories.AUDITS.getDirName());

            //never include audits with the main db dump
            DBDumpUtil.auditDump(ssgHome, config, dir.getAbsolutePath(), stdout);
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
     * determine if a database is local nor not, call isDbLocal()
     *
     * @param mappingFile name of the mapping file. Can include path information. Can be null when it is not required
     * @param tmpOutputDirectory The name of the directory to back the database file up into. They will be in the
     * folder representing this component "maindb" for the db
     * @param config DatabaseConfig object used for connecting to the database
     * @throws IOException if any exception occurs writing the database back up files
     */
    public void backUpComponentMainDb(String mappingFile, String tmpOutputDirectory, DatabaseConfig config) throws IOException {
        File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ImageDirectories.MAINDB.getDirName());
        addDatabaseToBackupFolder(dir, config);
        // produce template mapping if necessary
        if(mappingFile != null){
            String mappingFileName = ImportExportUtilities.getAbsolutePath(mappingFile);
            createMappingFile(mappingFileName, config);
        }

        //add my.cnf
        File file = new File(PATH_TO_MY_CNF);
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
    public void backUpVersion(String tmpOutputDirectory) throws IOException {
        ImportExportUtilities.verifyDirExistence(tmpOutputDirectory);

        FileOutputStream fos = new FileOutputStream(tmpOutputDirectory + File.separator + VERSIONFILENAME);
        fos.write( BuildInfo.getProductVersion().getBytes());
        fos.close();
    }

    /**
     * This method backs up the SSG config files node.properties, ssglog.properties, system.properties and omp.dat
     * from the node/default/etc/conf folder into the "config" directory under tmpOutputDirectory
     * @param tmpOutputDirectory The name of the directory to back the SSG config files into. They will be in the
     * folder representing this component "config"
     * @throws IOException if any exception occurs writing the SSG config files to the backup folder
     */
    public void backUpComponentConfig(String tmpOutputDirectory) throws IOException {
        File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ImageDirectories.CONFIG.getDirName());

        copyFiles(confDir, dir, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                for(String ssgFile: CONFIG_FILES){
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
    public void backUpComponentOS(String tmpOutputDirectory) throws IOException {
        if (new File(OPT_SECURE_SPAN_APPLIANCE).exists()) {
            // copy system config files
            File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ImageDirectories.OS.getDirName());
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
    public void backUpComponentCA(String tmpOutputDirectory) throws IOException {
        File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ImageDirectories.CA.getDirName());

        //backup all .property files in the conf folder which are not in CONFIG_FILES
        copyFiles(confDir, dir, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                if(!name.endsWith(".properties")) return false;

                for(String ssgFile: CONFIG_FILES){
                    if(ssgFile.equals(name)) return false;
                }
                return true;
            }
        });

        //back up all jar files in /opt/SecureSpan/Gateway/runtime/modules/lib
        copyFiles(new File(ssgHome, CA_JAR_DIR), dir, new FilenameFilter(){
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
    public void backUpComponentMA(String tmpOutputDirectory) throws IOException {
        File dir = createComponentDir(tmpOutputDirectory, ImportExportUtilities.ImageDirectories.MA.getDirName());

        //back up all jar files in /opt/SecureSpan/Gateway/runtime/modules/assertions
        copyFiles(new File(ssgHome, MA_AAR_DIR), dir, new FilenameFilter(){
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
    public void createImageZip(String zipFileName, String tmpOutputDirectory) throws IOException {
        logger.info("compressing image into " + zipFileName);
        File dirObj = new File(tmpOutputDirectory);
        if (!dirObj.isDirectory()) {
            throw new IOException(tmpOutputDirectory + " is not a directory");
        }
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        if (stdout != null) stdout.println("Compressing SecureSpan Gateway image into " + zipFileName);
        StringBuilder sb = new StringBuilder();
        addDir(dirObj, out, tmpOutputDirectory, sb);

        //Add the manifest listing all files in the archive to the archive
        File manifest = new File(tmpOutputDirectory + File.separator + "manifest.log");
        manifest.createNewFile();
        FileOutputStream fos = new FileOutputStream(manifest);
        fos.write(sb.toString().getBytes());
        fos.close();

        addZipFileToArchive(out, tmpOutputDirectory, manifest, sb);

        out.close();
    }

    /**
     * Retrieve a DatabaseConfig object using the supplied node.properties file and omp.dat file. This provides all
     * information required to connect to the database represented in node.properties
     * @param nodePropsFile node.properties
     * @param ompFile omp.dat
     * @return DatabaseConfig representing the db in node.properties
     * @throws IOException if any exception occurs while reading the supplied files
     */
    public DatabaseConfig getNodeConfig(File nodePropsFile, File ompFile) throws IOException {
        final MasterPasswordManager decryptor = ompFile.exists() ?
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) :
                null;

        NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( config == null ) {
            throw new CausedIOException("Database configuration not found.");
        }

        config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );

        logger.info("Using database host '" + config.getHost() + "'.");
        logger.info("Using database port '" + config.getPort() + "'.");
        logger.info("Using database name '" + config.getName() + "'.");
        logger.info("Using database user '" + config.getNodeUsername() + "'.");
        return config;
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
    private void addZipFileToArchive(ZipOutputStream out, String tmpOutputDirectory, File fileToAdd, StringBuilder sb)
            throws IOException {
        byte[] tmpBuf = new byte[1024];
        FileInputStream in = new FileInputStream(fileToAdd.getAbsolutePath());
        if (stdout != null) stdout.println("\t- " + fileToAdd.getAbsolutePath());
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
    private void addDatabaseToBackupFolder(File dbTmpOutputDirectory, DatabaseConfig config) throws IOException {
        if(!isDbLocal(config.getHost())){
            logger.log(Level.WARNING, "Cannot backup database as it is not local");
            throw new IllegalStateException("Cannot back up database as it is not local");
        }
        try {
            DBDumpUtil.dump(config, dbTmpOutputDirectory.getAbsolutePath(), stdout);
        } catch (SQLException e) {
            logger.log(Level.INFO, "exception dumping database, possible that the database is not running or credentials " +
                    "are not correct", ExceptionUtils.getDebugException(e));
            throw new IOException("cannot dump the database, please ensure the database is running and the credentials are correct");
        }
    }

    /**
     * Copy the files from destinationDir to sourceDir. Does not copy directories
     * @param destinationDir The directory containing the files to copy
     * @param sourceDir The directory to copy the files to
     * @param fileFilter FilenameFilter can be null. Use to filter the files in sourceDir
     * @throws IOException if any exception occurs while copying the files
     */
    private void copyFiles(File sourceDir, File destinationDir, FilenameFilter fileFilter) throws IOException{
        String [] filesToCopy = sourceDir.list(fileFilter);

        for ( String filename : filesToCopy ) {
            File file = new File(sourceDir, filename);
            if ( file.exists() && !file.isDirectory()) {
                FileUtils.copyFile(file, new File(destinationDir.getAbsolutePath() + File.separator + file.getName()));
            }
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
    private File createComponentDir(String tmpOutputDirectory, String componentName) throws IOException {
        File dir = new File(tmpOutputDirectory, componentName);
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
    private void createMappingFile(String mappingFileName, DatabaseConfig config) throws IOException {
        if(!isDbLocal(config.getHost())){
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
     * @return a string representing the absolute path to the image file
     * @throws IOException if we cannot write to the pathToImageFile file
     */
    private String validateImageFile(String pathToImageFile) throws IOException {
        if (pathToImageFile == null) {
            logger.info("No target image path specified");
            throw new NullPointerException("pathToImageFile cannot be null");
        } else {
            //fail if file exists
            ImportExportUtilities.verifyFileExistence(pathToImageFile, true);
        }

        if (!testCanWriteSilently(pathToImageFile)) {
            throw new IOException("Cannot write image to " + pathToImageFile);
        }
        return pathToImageFile;
    }

    /**
     * Quietly test if the given path have permissions to write.  This method is similar to
     * testCanWrite except that it will not output any error messages, instead, they will be logged.
     *
     * @param path  Path to test
     * @return  TRUE if can write on the given path, otherwise FALSE.
     */
    private boolean testCanWriteSilently(String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.close();
            (new File(path)).delete();
            logger.warning("Successfully tested write permission for " + path);
        } catch (Exception e) {
            logger.warning("Cannot write to " + path + ". ");
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
    private void addDir(File dirObj, ZipOutputStream out, String tmpOutputDirectory, StringBuilder sb) throws IOException {
        File[] files = dirObj.listFiles();

        for (File file : files) {
            if (file.isDirectory()) {
                addDir(file, out, tmpOutputDirectory, sb);
                continue;
            }
            addZipFileToArchive(out, tmpOutputDirectory, file, sb);
        }
    }

    /**
     * Validate all program arguments. This method will validate all required params are met, and that any which expect
     * a value recieve it
     * @param args The name value pair map of each argument to it's value, if a vaule exists
     * @throws IOException for arguments which are files, they are checked to see if the exist, which may cause an IOException
     * @throws BackupRestoreLauncher.InvalidProgramArgumentException
     */
    private void validateProgramParameters(Map<String, String> args) throws IOException, BackupRestoreLauncher.InvalidProgramArgumentException {
        //skip the whole pre-processing
        if (args.containsKey(ImportExportUtilities.SKIP_PRE_PROCESS.name)) {
            return;
        }

        //image option must be specified
        if (!args.containsKey(IMAGE_PATH.name)) {
            throw new InvalidProgramArgumentException("missing option " + IMAGE_PATH.name + ", required for exporting image");
        } else {
            ImportExportUtilities.verifyFileExistence(args.get(IMAGE_PATH.name), true);   //test that the file is a new file
            ImportExportUtilities.verifyCanWriteFile(args.get(IMAGE_PATH.name));  //test if we can create the file
        }

        //check condition for mapping file
        if (args.containsKey(MAPPING_PATH.name)) {
            ImportExportUtilities.verifyFileExistence(args.get(MAPPING_PATH.name), true); //test that the file is a new file
            ImportExportUtilities.verifyCanWriteFile(args.get(MAPPING_PATH.name));    //test if we can create the file
        }

        //check if node.properties file exists
        File configDir = new File(ssgHome, NODE_CONF_DIR);
        File nodePropsFile = new File(configDir, NODE_PROPERTIES_FILE);
        NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
        if ( config == null ) {
            throw new IOException("database configuration not found.");
        }

        File ompFile = new File(configDir, OMP_DAT_FILE);
        final MasterPasswordManager decryptor =
                ompFile.exists() ? new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) : null;
        config.setNodePassword( new String(decryptor.decryptPasswordIfEncrypted(config.getNodePassword())) );

        //check if we can connect to the database
        ImportExportUtilities.verifyDatabaseConnection(config, false);
    }

    /**
     * Get the usage for the Exporter utility. Usage information is written to the StringBuilder output
     * @param output StringBuilder to write the usage information to
     */
    public static void getExporterUsage(StringBuilder output) {
        int largestNameStringSize;
        largestNameStringSize = ImportExportUtilities.getLargestNameStringSize(ALLOPTIONS);
        for (CommandLineOption option : ALLOPTIONS) {
            output.append("\t")
                    .append(option.name)
                    .append(ImportExportUtilities.createSpace(largestNameStringSize-option.name.length() + 1))
                    .append(option.description)
                    .append(BackupRestoreLauncher.EOL_CHAR);
        }
    }
}