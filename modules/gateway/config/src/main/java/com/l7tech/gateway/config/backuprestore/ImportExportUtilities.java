package com.l7tech.gateway.config.backuprestore;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.net.NetworkInterface;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.SocketException;

import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.manager.NodeConfigurationManager;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.config.node.NodeConfig;
import com.l7tech.server.management.config.node.DatabaseType;
import com.l7tech.util.*;

/**
 * Utility class for Exporting and Importing backup image zip files
 *
 */
public class ImportExportUtilities {
    /** System property used as base directory for all relative paths. */
    public static final String BASE_DIR_PROPERTY = "com.l7tech.server.backuprestore.basedir";
    public static final String OMP_DAT = "omp.dat";
    public static final String NODE_PROPERTIES = "node.properties";
    public static final String SSGLOG_PROPERTIES = "ssglog.properties";
    public static final String SYSTEM_PROPERTIES = "system.properties";
    /**
     * These configuration files are expected in ssg_home/node/default/etc/conf/ and constitute
     * the complete backing up of SSG configuration files
     */
    public static final String[] CONFIG_FILES = new String[]{
            SSGLOG_PROPERTIES,
            SYSTEM_PROPERTIES,
            NODE_PROPERTIES,
            OMP_DAT
    };
    public static final String VERSION = "version";
    public static final String MANIFEST_LOG = "manifest.log";
    /**
     * Default installation of the ssg appliance
     */
    public static final String OPT_SECURE_SPAN_APPLIANCE = "/opt/SecureSpan/Appliance";

    private static enum ARGUMENT_TYPE {VALID_OPTION, IGNORED_OPTION, INVALID_OPTION, SKIP_OPTION, VALUE}
    private static final Logger logger = Logger.getLogger(ImportExportUtilities.class.getName());

    //todo [Donal] what is skip processing actually needed for? Program args must always be valid. Remove
    public static final CommandLineOption SKIP_PRE_PROCESS = new CommandLineOption("-skipPreProcess", "skips pre-processing", false, true);
    public static final CommandLineOption[] SKIP_OPTIONS = {SKIP_PRE_PROCESS};

    /**
     * ImageDirectories lists all the possible folders which can exist in a Backup image
     */
    protected enum ImageDirectories {
        OS("os", "Folder for OS only files"),
        CONFIG("config", "Folder for SSG only configuration files"),
        MAINDB("maindb", "Folder for database backup sql file and my.cnf information. No audits ever"),
        AUDITS("audits", "Folder containing audits zipped file"),
        CA("ca", "Folder containing Custom Assertion jars and associated property files"),
        MA("ma", "Folder containing Modular Assertion jars");

        private ImageDirectories(String dirName, String description) {
            this.dirName = dirName;
            this.description = description;
        }

        String getDirName() {
            return dirName;
        }

        public String getDescription() {
            return description;
        }

        private final String dirName;
        private final String description;
    }

    /**
     * @param path  either a relative path or an absolute path
     * @return if <code>path</code> is absolute, it is simply returned;
     *         if <code>path</code> is relative, it is resolved by prefixing it with the directory specified by the system property {@link #BASE_DIR_PROPERTY};
     *         if <code>path</code> is null, returns null
     * @throws RuntimeException if the system property {@link #BASE_DIR_PROPERTY} is not set when it is needed
     */
    public static String getAbsolutePath(final String path) {
        if (path == null) return null;

        if (new File(path).isAbsolute()) {
            return path;
        }

        final String baseDir = System.getProperty(BASE_DIR_PROPERTY);
        if (baseDir == null) {
            throw new RuntimeException("System property \"" + BASE_DIR_PROPERTY + "\" not set.");
        }
        return baseDir + File.separator + path;
    }

    /**
     * Determine if the given host is local. Any exceptions are logged and false returned.
     * @param host String name of the host to check if it is local or not
     * @return true if host is local. False otherwise. False if any exception occurs when looking up the host.
     */
    public static boolean isHostLocal(final String host) {
        try{
            final NetworkInterface networkInterface = NetworkInterface.getByInetAddress( InetAddress.getByName(host) );
            if ( networkInterface != null ) return true;
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING,  "Could not look up database host: " + e.getMessage());
        } catch (SocketException e) {
            logger.log(Level.WARNING,  "Socket exception looking up database host: " + e.getMessage());
        }
        return false;
    }

    /**
     * Retrieve a DatabaseConfig object using the supplied node.properties file and omp.dat file. This provides all
     * information required to connect to the database represented in node.properties
     * @param nodePropsFile node.properties
     * @param ompFile omp.dat
     * @return DatabaseConfig representing the db in node.properties
     * @throws java.io.IOException if any exception occurs while reading the supplied files
     */
    public static DatabaseConfig getNodeConfig(final File nodePropsFile, final File ompFile) throws IOException {
        final MasterPasswordManager decryptor = ompFile.exists() ?
                new MasterPasswordManager(new DefaultMasterPasswordFinder(ompFile).findMasterPassword()) :
                null;

        final NodeConfig nodeConfig = NodeConfigurationManager.loadNodeConfig("default", nodePropsFile, true);
        final DatabaseConfig config = nodeConfig.getDatabase( DatabaseType.NODE_ALL, NodeConfig.ClusterType.STANDALONE, NodeConfig.ClusterType.REPL_MASTER );
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
     * Convert command line parameters into a map, where any command line value which requires a value, is mapped to
     * it's value in the returned map.
     * validOptions lists which arguments to expect, each CommandLineOption within it, specifies whether or not it
     * must be followed by a parameter on the command line.
     * ignoredOptions is provided for backwards comapability where an old script may supply a parameter which has been
     * removed, but for which we will not error out because of //todo [Donal] look at removing this
     * @param args  The command line arguments. Cannot be null
     * @param validOptions list of valid command line argument. Canot be null
     * @param ignoredOptions list of arguments which are allowed, but will be ignored. Cannot be null
     * @return  A map of the command line argumetents to their values, if any
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException
     * If an unexpected arguments is found, or an argument is missing it's value, or the value of an argument represents
     * a file, and that file does not exist or cannot be written to
     */
    public static Map<String, String> getAndValidateCommandLineOptions(final String[] args,
                                                                       List<CommandLineOption> validOptions,
                                                List<CommandLineOption> ignoredOptions)
            throws BackupRestoreLauncher.InvalidProgramArgumentException {
        if(args == null) throw new NullPointerException("args cannot be null");
        if(validOptions == null) throw new NullPointerException("validOptions cannot be null");
        if(ignoredOptions == null) throw new NullPointerException("ignoredOptions cannot be null");
        
        Map<String, String> arguments = new HashMap<String, String>();
        int index = 1;  //skip the first argument, because it's already used to determine the utility type (export/import)

        while (args != null && index < args.length) {
            final String argument = args[index];

            switch (determineArgumentType(argument, validOptions, ignoredOptions)) {
                case SKIP_OPTION:   //todo [Donal] look at removing this
                    arguments.put(argument, "");    //skip options does not have values!
                    index++;
                    break;
                case VALID_OPTION:
                    if (optionHasNoValue(argument, validOptions)) {
                        arguments.put(argument, "");
                        index++;
                    } else if (!optionIsValuePath(argument, validOptions)) {
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions))) {
                            arguments.put(argument, args[index+1]);
                            index = index + 2;  //already processed one, so we'll need to advance to the next after the processed
                        } else {
                            //expecting a value for this parameter but there was none
                            throw new InvalidProgramArgumentException("option " + argument + " expects value");
                        }
                    } else {
                        int nextOptionIndex = findIndexOfNextOption(index, args, validOptions, ignoredOptions);
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions)) && index+1 < nextOptionIndex) {
                            String fullValue = getFullValue(args, index+1, nextOptionIndex);
                            arguments.put(argument, fullValue);
                            index = nextOptionIndex;
                        } else {
                            throw new InvalidProgramArgumentException("option " + argument + " expects value");
                        }
                    }
                    break;
                case IGNORED_OPTION:
                    if (optionHasNoValue(argument, ignoredOptions)) {
                        logger.info("Option '" + argument + "' is ignored.");
                        index++;
                    } else if (!optionIsValuePath(argument, ignoredOptions)) {
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions))) {
                            index = index + 2;
                        } else {
                            //we dont care, just continue to process
                            index++;
                        }
                        logger.info("Option '" + argument + " " + args[index] + "' is ignored.");
                    } else {
                        int nextOptionIndex = findIndexOfNextOption(index, args, validOptions, ignoredOptions);
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1], validOptions, ignoredOptions)) && index+1 < nextOptionIndex) {
                            String fullValue = getFullValue(args, index+1, nextOptionIndex);
                            index = nextOptionIndex;
                            logger.info("Option '" + argument + " " + fullValue + "' is ignored.");
                        } else {
                            //we dont care, just continue to process
                            index++;
                            logger.info("Option '" + argument + "' is ignored.");
                        }
                    }
                    break;
                default:
                    throw new BackupRestoreLauncher.InvalidProgramArgumentException("option " + argument + " is invalid");
            }
        }

        return arguments;
    }

    /**
     * Verify that the given database configuration can be used to connect to a database
     *
     * @param config    Database configuration informaiton. Cannot be null
     * @param isRootAccount Flag to use as root account or gateway account
     * @throws NullPointerException if config is null
     * @throws java.sql.SQLException if any database access error occurs when trying to obtain a connection. This will
     * wrap the underlying SQLException thrown by the database driver
     */
    public static void verifyDatabaseConnection(DatabaseConfig config, boolean isRootAccount) throws SQLException {
        if (config == null) throw new NullPointerException("config cannot be null");

        Connection connection = null;
        try {
            connection = (new DBActions()).getConnection(config, isRootAccount);
        } catch (SQLException sqle) {
            throw new SQLException("cannot connect to database host '" + config.getHost()
                    + "' in database '" + config.getName() + "' with user '"
                    + (isRootAccount ? config.getDatabaseAdminUsername() : config.getNodeUsername()) + "'", sqle);
        } finally{
            ResourceUtils.closeQuietly(connection);
        }
    }

    /**
     * Confirm that the file fileName exists
     * @param fileName the file name to confirm exists
     * @throws IllegalArgumentException if the file fileName does not exist
     */
    public static void throwIfFileExists(String fileName){
        if (fileName == null) throw new NullPointerException("fileName cannot be null");
        File file = new File(fileName);
        if(file.exists()) throw new IllegalArgumentException("file '" + fileName + "' should not exist");
    }

    /**
     * Confirm that the file fileName, does not exists
     * @param fileName the file name to confirm does not exists
     * @throws IllegalArgumentException if the file fileName does exist
     */
    public static void throwIfFileDoesNotExist(String fileName){
        if (fileName == null) throw new NullPointerException("fileName cannot be null");
        File file = new File(fileName);
        if(!file.exists()) throw new IllegalArgumentException("file '" + fileName + "' should exist");
    }

    /**
     * Test if can write and create the file.
     *
     * @param fileName  The file to be created
     * @throws IOException
     * @throws NullPointerException if fileName is null
     * @throws IllegalArgumentException if fileName alredy exists
     */
    public static void verifyCanWriteFile(String fileName) throws IOException {
        if(fileName == null) throw new NullPointerException("fileName cannot be null");
        throwIfFileExists(fileName);
        
        File file = new File(fileName);
        FileOutputStream fos = null;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write("I can write to this file".getBytes());
            //file exists
        } finally {
            ResourceUtils.closeQuietly(fos);
            file.delete();
        }
    }

    /**
     * When migrating and a pre existing database is being used, then the version of the database must match
     * the version of the installed SSG.
     *
     * @param version   The database version to be compared to the installed SSG version
     * @throws UnsupportedOperationException if the version do not match
     */
    public static void throwIfDbVersionDoesNotMatchSSG(String version){
        String ssgVersion = BuildInfo.getProductVersion();
        if (!ssgVersion.equals(version)) {
             throw new UnsupportedOperationException("Database version '"+version+"' does not match the SSG version " +
                     "'"+ssgVersion+"'. Values must match to Import using -migrate");
        }
    }

    /**
     * Verify if the database exists.
     *
     * @param host  The database host
     * @param dbName    The database name
     * @param port  The port
     * @param username  The username to be used for login
     * @param password  The password for the specified username
     * @return
     */
    public static boolean verifyDatabaseExists(String host, String dbName, int port, String username, String password) {
        Connection c = null;
        Statement s = null;
        ResultSet rs = null;
        try {
            DatabaseConfig config = new DatabaseConfig(host, port, dbName, username, password);
            c = new DBActions().getConnection(config, false);
            s = c.createStatement();
            rs = s.executeQuery("select * from hibernate_unique_key");
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            return false;
        } finally {
            ResourceUtils.closeQuietly(rs);
            ResourceUtils.closeQuietly(s);
            ResourceUtils.closeQuietly(c);
        }
        return false;
    }

    /**
     * Create a directory in the system temp folder. The returned string represents a directory that exists. Caller
     * should delete the directory when finished. The created directory will be prefixed with "ssg_backup_restore"
     * @return String path of the created directory
     * @throws IOException if the temp directory cannot be created
     */
    public static String createTmpDirectory() throws IOException {
        File tmp = File.createTempFile("ssg_backup_restore", "tmp");
        tmp.delete();
        tmp.mkdir();
        logger.info("created temporary directory at " + tmp.getPath());
        return tmp.getPath();
    }

    /**
     * Traverses the list of options and determine the length of the longest option name.
     *
     * @param options   The list of options
     * @return          The length of the longest option name
     */
    public static int getLargestNameStringSize(final List<CommandLineOption> options) {
        int largestStringSize = 0;
        for (CommandLineOption option : options) {
            if (option.name != null && option.name.length() > largestStringSize) {
                largestStringSize = option.name.length();
            }
        }
        return largestStringSize;
    }

    /**
     * Creates the padding spaces string
     *
     * @param space The number of spaces to create
     * @return  The space string
     */
    public static String createSpace(int space) {
        StringBuffer spaces = new StringBuffer();
        for (int i=0; i <= space; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    /**
     * Verify that a directory exists
     * @param directoryName name of the directory to check if it exists or not. Cannot be null
     * @throws IllegalArgumentException if the directoryName is null, directoryName does not exist or does not represent a directory
     * @throws NullPointerException if directory name is null
     */
    public static void verifyDirExistence(String directoryName){
        if (directoryName == null) throw new NullPointerException("directoryName cannot be null");

        // non empty check allows the utility to work with backup servlet temporary files
        File file = new File(directoryName);
        if(!file.exists()){
            throw new IllegalArgumentException("Directory '" + directoryName + "' does not exist");
        }

        if(!file.isDirectory()){
            throw new IllegalArgumentException("File '" + directoryName + "' is not a directory");
        }
    }

    /**
     * Copy the files from destinationDir to sourceDir. Does not copy directories
     * @param destinationDir The directory containing the files to copy
     * @param sourceDir The directory to copy the files to
     * @param fileFilter FilenameFilter can be null. Use to filter the files in sourceDir
     * @throws IOException if any exception occurs while copying the files
     */
    public static void copyFiles(final File sourceDir, final File destinationDir, final FilenameFilter fileFilter)
            throws IOException{
        final String [] filesToCopy = sourceDir.list(fileFilter);

        for ( final String filename : filesToCopy ) {
            final File file = new File(sourceDir, filename);
            if ( file.exists() && !file.isDirectory()) {
                FileUtils.copyFile(file, new File(destinationDir.getAbsolutePath() + File.separator + file.getName()));
            }
        }
    }

    public static void copyDir(final File sourceDir, final File destDir) throws IOException {
        if(!destDir.exists()) destDir.mkdir();
        else if (!destDir.isDirectory()) throw new RuntimeException("desDir '"+destDir.getName()+"' is not a directory");

        final File [] allFiles = sourceDir.listFiles();
        for(final  File f: allFiles){
            if(f.isDirectory()) {
                copyDir(f, new File(destDir, f.getName()));
            } else {
                FileUtils.copyFile(f, new File(destDir.getAbsolutePath() + File.separator + f.getName()));
            }
        }
    }

    /**
     * Get the directory part of a path and filename string e.g. /home/layer7/temp/image1.zip
     * The path information is optional
     *
     * This method supports both the unix / and the windows \ as the user may supply a unix or windows path name
     * when the export / import is used in conjection with ftp
     * @param imageName The String name to extract the path information from
     * @return The path information not including the final / or \, null if no path information in the imageName.
     * @throws NullPointerException if imageName is null
     * @throws IllegalArgumentException if imageName is empty
     */
    public static String getDirPart(final String imageName){
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.isEmpty()) throw new IllegalArgumentException("imageName cannot be empty");

        if(imageName.equals("/")) return imageName;
        if(imageName.equals("\\")) throw new IllegalArgumentException("\\ is an invalid imageName");
        
        int lastIndex = imageName.lastIndexOf("/");
        if(lastIndex == -1) lastIndex = imageName.lastIndexOf("\\");
        if(lastIndex != -1){
            return imageName.substring(0, lastIndex);
        }
        return null;
    }

    /**
     * Get the file part of a path and filename string e.g. /home/layer7/temp/image1.zip
     * The path information is optional
     * If the file name only contains a path ending in either / or \, then an empty string will be returned
     *
     * This method supports both the unix / and the windows \ as the user may supply a unix or windows path name
     * when the export / import is used in conjection with ftp
     * @param imageName The String name to extract the file name information from
     * @return The file name, never null
     * @throws NullPointerException if imageName is null
     * @throws IllegalArgumentException if imageName is empty or is equal to / or \
     */
    public static String getFilePart(final String imageName){
        if(imageName == null) throw new NullPointerException("imageName cannot be null");
        if(imageName.isEmpty()) throw new IllegalArgumentException("imageName cannot be empty");

        if(imageName.equals("/")) throw new IllegalArgumentException("/ is an invalid imageName");
        if(imageName.equals("\\")) throw new IllegalArgumentException("\\ is an invalid imageName");

        int lastIndex = imageName.lastIndexOf("/");
        if(lastIndex == -1) lastIndex = imageName.lastIndexOf("\\");
        if(lastIndex == -1){
            return imageName;
        }
        return imageName.substring(lastIndex + 1, imageName.length());
    }

    /**
     * Determines if the provided option has path as an option
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name has path as option, otherwise FALSE.
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException     The option name is not found in the available option list
     */
    private static boolean optionIsValuePath(String optionName, final List<CommandLineOption> options) throws BackupRestoreLauncher.InvalidProgramArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.name.equals(optionName)) {
                return commandLineOption.isValuePath;
            }
        }
        //this is a programming error
        throw new IllegalArgumentException("option " + optionName + " does not exist in options");
    }

    /**
     * Determines if the provided option expects a value
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name expects a value, otherwise FALSE.
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException     The option name is not found in the available option list
     */
    private static boolean optionHasNoValue(String optionName, final List<CommandLineOption> options) throws InvalidProgramArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.name.equals(optionName)) {
                return commandLineOption.hasNoValue;
            }
        }
        //this is a programming error
        throw new IllegalArgumentException("option " + optionName + " does not exist in options");
    }

    /**
     * Determines if the provided option name is an option
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name is an option, otherwise FALSE.
     */
    private static boolean isOption(String optionName, final List<CommandLineOption> options) {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.name.equals(optionName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine the argument type as either an option or a value to a particular option
     *
     * @param argument  argument for comparison
     * @return  The ARGUMENT_TYPE of the parse argument.
     */
    private static ARGUMENT_TYPE determineArgumentType(String argument, List<CommandLineOption> validOptions,
                                                List<CommandLineOption> ignoredOptions) {
        if (argument != null && argument.startsWith("-")) {
            if (isOption(argument, validOptions))
                return ARGUMENT_TYPE.VALID_OPTION;
            else if (isOption(argument, ignoredOptions))
                return ARGUMENT_TYPE.IGNORED_OPTION;
            else if (isOption(argument, Arrays.asList(SKIP_OPTIONS)))
                return ARGUMENT_TYPE.SKIP_OPTION;
            else
                return ARGUMENT_TYPE.INVALID_OPTION;
        } else {
            return ARGUMENT_TYPE.VALUE;
        }
    }

    /**
     * Finds the index location of the next option if available
     *
     * @param startIndex    The starting index of the location to start searching.  DO NOT start at the index of the
     *                      previous option index location.
     *                      eg. if parameter were "-p1 val1 -p2 val2 -p3 ...   If p1 was the previous option, then the
     *                      start index should be at val1
     * @param args          The list of arguments
     * @return              The index location of the next available option found, if reached to end of argument list, then
     *                      it'll return the size of the argument list
     */
    private static int findIndexOfNextOption(final int startIndex, final String[] args, List<CommandLineOption> validOptions,
                                                List<CommandLineOption> ignoredOptions) {
        int nextOptionIndex = startIndex;
        while (args != null && nextOptionIndex+1 < args.length) {
            if (!ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[++nextOptionIndex], validOptions, ignoredOptions))) {
                return nextOptionIndex;
            }
        }
        return args != null ? args.length : nextOptionIndex+1;
    }

    /**
     * Parses the values given by the provided boundaries.
     *
     * @param args  The list of arguments
     * @param startIndex    The starting index
     * @param endIndex      The ending index, does not include this index
     * @return      The parsed value withing the boundaries.
     */
    private static String getFullValue(final String[] args, int startIndex, int endIndex) {
        StringBuffer buffer = new StringBuffer();
        for (int i=startIndex; i < args.length && i < endIndex; i++) {
            buffer.append(args[i] + " ");
        }
        return buffer.toString() != null ? buffer.toString().trim() : buffer.toString();
    }

}