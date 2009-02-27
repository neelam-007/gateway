package com.l7tech.gateway.config.flasher;

import com.l7tech.util.MasterPasswordManager;
import com.l7tech.util.DefaultMasterPasswordFinder;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.FileUtils;
import com.l7tech.util.CausedIOException;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.sql.SQLException;

import org.xml.sax.SAXException;

/**
 * The utility that exports an SSG image file.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class Exporter extends ImportExportUtility {

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

    private static final String[] CONFIG_FILES = new String[]{
        "ssglog.properties",
        "system.properties",
    };

    private boolean includeAudit = false;
    private String tmpDirectory;
    private boolean mappingEnabled = false;

    /** Home directory of the Flasher; <code>null</code> if the JVM was launched from there already. */
    private File flasherHome;

    /** Stream for verbose output; <code>null</code> for no verbose output. */
    private PrintStream stdout;

    /** Stream for error output; <code>null</code> for no error output. */
    private PrintStream stderr;

    /**
     * @param flasherHome   home directory of the Flasher; use <code>null</code>
     *                      if the JVM was launched from there already
     * @param stdout        stream for verbose output; <code>null</code> for no verbose output
     * @param stderr        stream for error output; <code>null</code> for no error output
     */
    public Exporter(final File flasherHome, final PrintStream stdout, final PrintStream stderr) {
        this.flasherHome = flasherHome;
        this.stdout = stdout;
        this.stderr = stderr;
    }

    /**
     * @param includeAudit      whether to include audit
     * @param mappingPath       can be null; relative path is assumed to be relative to the directory specified by the system property {@link FlashUtilityLauncher#BASE_DIR_PROPERTY}
     * @param outputPath        must not be null; relative path is assumed to be relative to the directory specified by the system property {@link FlashUtilityLauncher#BASE_DIR_PROPERTY}
     */
    public void doIt(final boolean includeAudit,
                     final String mappingPath,
                     final String outputPath)
            throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        if (outputPath == null) throw new FlashUtilityLauncher.InvalidArgumentException("outputPath cannot be null");

        final Map<String, String> args = new HashMap<String, String>();
        args.put(AUDIT.name, Boolean.toString(includeAudit));
        if (mappingPath != null) args.put(MAPPING_PATH.name, mappingPath);
        args.put(IMAGE_PATH.name, outputPath);

        doIt(args);
    }

    // do the export
    public void doIt(Map<String, String> arguments) throws FlashUtilityLauncher.InvalidArgumentException, IOException {
        // check that we can write output at located asked for
        String outputpathval = FlashUtilityLauncher.getAbsolutePath(arguments.get(IMAGE_PATH.name));
        if (outputpathval == null) {
            logger.info("no target image path specified");
            throw new FlashUtilityLauncher.InvalidArgumentException("missing option " + IMAGE_PATH.name + ". i dont know where to output the image to.");
        }
        if (!testCanWriteSilently(outputpathval)) {            
            throw new IOException("cannot write image to " + outputpathval);
        }

        // check whether or not we are expected to include audit in export
        String auditval = arguments.get(AUDIT.name);
        if (auditval != null && !auditval.toLowerCase().equals("no") && !auditval.toLowerCase().equals("false")) {
            includeAudit = true;
        }

        //check whether mapping option was used
        if(arguments.get(MAPPING_PATH.name) != null) mappingEnabled = true;

        File confDir = new File(flasherHome, "../../node/default/etc/conf");
        tmpDirectory = createTmpDirectory();

        try {
            logger.info("created temporary directory at " + tmpDirectory);

            // Read database connection settings for the partition at hand
            File ompFile = new File(confDir, "omp.dat");
            final MasterPasswordManager decryptor = ompFile.exists() ?
                    new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) :
                    null;

            File nodePropsFile = new File(confDir, "node.properties");
            if ( nodePropsFile.exists() ) {
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

                // dump the database
                try {
                    DBDumpUtil.dump(config, includeAudit, mappingEnabled, tmpDirectory, stdout);
                } catch (SQLException e) {
                    logger.log(Level.INFO, "exception dumping database", e);
                    throw new IOException("cannot dump the database " + e.getMessage());
                }

                // produce template mapping if necessary
                String mappingFileName = FlashUtilityLauncher.getAbsolutePath(arguments.get(MAPPING_PATH.name));
                if (mappingFileName != null) {
                    if (!testCanWriteSilently(mappingFileName)) {
                        throw new FlashUtilityLauncher.InvalidArgumentException("cannot write to the mapping template path provided: " + mappingFileName);
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

            // record version of this image
            FileOutputStream fos = new FileOutputStream(tmpDirectory + File.separator + VERSIONFILENAME);
            fos.write( BuildInfo.getProductVersion().getBytes());
            fos.close();

            // copy all config files we want into this temp directory
            for ( String filename : CONFIG_FILES ) {
                File file = new File(confDir, filename);
                if ( file.exists() ) {
                    FileUtils.copyFile(file, new File(tmpDirectory + File.separator + file.getName()));
                }
            }

            //restore OS files if this is an appliance
            if (new File("/opt/SecureSpan/Appliance").exists()) {
                // copy system config files
                OSConfigManager.saveOSConfigFiles(tmpDirectory, flasherHome);
            } else {
                logger.info("OS configuration files can only be restored on an appliance.  This option will be ignored.");
            }
    
            // zip the temp directory into the requested image file (outputpathval)
            logger.info("compressing image into " + outputpathval);
            zipDir(outputpathval, tmpDirectory);
        } finally {
            logger.info("cleaning up temp files at " + tmpDirectory);
            if (stdout != null) stdout.println("Cleaning temporary files at " + tmpDirectory);
            FileUtils.deleteDir(new File(tmpDirectory));
        }
    }

    @Deprecated
    private boolean testCanWrite(String path) {
        try {
            FileOutputStream fos = new FileOutputStream(path);
            fos.close();
        } catch (FileNotFoundException e) {
            if (stderr != null) stderr.println("cannot write to " + path + ". " + e.getMessage());
            return false;
        } catch (IOException e) {
            if (stderr != null) stderr.println("cannot write to " + path + ". " + e.getMessage());
            return false;
        }
        (new File(path)).delete();
        logger.info("successfully tested write permission for " + path);
        return true;
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

    public static String createTmpDirectory() throws IOException {
        File tmp = File.createTempFile("ssgflash", "tmp");
        tmp.delete();
        tmp.mkdir();
        return tmp.getPath();
    }

    private void zipDir(String zipFileName, String dir) throws IOException {
        File dirObj = new File(dir);
        if (!dirObj.isDirectory()) {
            throw new IOException(dir + " is not a directory");
        }
        ZipOutputStream out = new ZipOutputStream(new FileOutputStream(zipFileName));
        if (stdout != null) stdout.println("Compressing SecureSpan Gateway image into " + zipFileName);
        addDir(dirObj, out);
        out.close();
    }

    private void addDir(File dirObj, ZipOutputStream out) throws IOException {
        File[] files = dirObj.listFiles();
        byte[] tmpBuf = new byte[1024];

        for (File file : files) {
            if (file.isDirectory()) {
                addDir(file, out);
                continue;
            }
            FileInputStream in = new FileInputStream(file.getAbsolutePath());
            if (stdout != null) stdout.println("\t- " + file.getAbsolutePath());
            String zipEntryName = file.getAbsolutePath();
            if (zipEntryName.startsWith(tmpDirectory)) {
                zipEntryName = zipEntryName.substring(tmpDirectory.length() + 1);
            }
            out.putNextEntry(new ZipEntry(zipEntryName));
            // Transfer from the file to the ZIP file
            int len;
            while ((len = in.read(tmpBuf)) > 0) {
                out.write(tmpBuf, 0, len);
            }
            // Complete the entry
            out.closeEntry();
            in.close();
        }
    }

    @Override
    public List<CommandLineOption> getIgnoredOptions() {
        return Arrays.asList(ALL_IGNORED_OPTIONS);
    }

    @Override
    public List<CommandLineOption> getValidOptions() {
        return Arrays.asList(ALLOPTIONS);
    }

    @Override
    public String getUtilityType() {
        return "export";
    }
}
