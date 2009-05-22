package com.l7tech.gateway.config.backuprestore;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ResourceUtils;

/**
 * Utility class for Exporting and Importing backup image zip files
 *
 * User: dlee
 * Date: Feb 26, 2009
 */
public class ImportExportUtilities {
    /** System property used as base directory for all relative paths. */
    private static final String BASE_DIR_PROPERTY = "com.l7tech.server.backuprestore.basedir";

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


    private static enum ARGUMENT_TYPE {VALID_OPTION, IGNORED_OPTION, INVALID_OPTION, SKIP_OPTION, VALUE}
    private static final Logger logger = Logger.getLogger(ImportExportUtilities.class.getName());

    public static final CommandLineOption SKIP_PRE_PROCESS = new CommandLineOption("-skipPreProcess", "skips pre-processing", false, true);
    public static final CommandLineOption[] SKIP_OPTIONS = {SKIP_PRE_PROCESS};

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

    /**
     * Reads the parameter for the provided utility.
     *
     * @param args  The list of arguments to parse out.
     * @return  A map of the determined arguments
     * @throws com.l7tech.gateway.config.backuprestore.BackupRestoreLauncher.InvalidProgramArgumentException    If a particular argument is an invalid option.
     */
    public static Map<String, String> getParameters(final String[] args, List<CommandLineOption> validOptions,
                                                List<CommandLineOption> ignoredOptions) throws BackupRestoreLauncher.InvalidProgramArgumentException {
        Map<String, String> arguments = new HashMap<String, String>();
        int index = 1;  //skip the first argument, because it's already used to determine the utility type (export/import)

        while (args != null && index < args.length) {
            final String argument = args[index];

            switch (determineArgumentType(argument, validOptions, ignoredOptions)) {
                case SKIP_OPTION:
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
     * Verify that the given database configuration, the connection is good.
     *
     * @param config    Database configuration informaiton
     * @param isRootAccount Flag to use as root account or gateway account
     * @throws IOException
     */
    public static void verifyDatabaseConnection(DatabaseConfig config, boolean isRootAccount) throws IOException {
        if (config == null) throw new IOException("no database configuration defined");

        try {
            Connection connection = (new DBActions()).getConnection(config, isRootAccount);
            if (connection == null) {
                throw new SQLException();
            }
        } catch (SQLException sqle) {
            throw new IOException("cannot connect to database host '" + config.getHost()
                    + "' in database '" + config.getName() + "' with user '"
                    + (isRootAccount ? config.getDatabaseAdminUsername() : config.getNodeUsername()) + "'");
        }
    }

    /**
     * Verify file existence.  If the flag 'failIfExists' is true, then basically it'll fail if the file does exists.
     * If the flag 'failIfExists' is false, then it'll fail if the file does not exists.
     *
     * @param fileName  The file name to verify for existence
     * @param failIfExists  TRUE = throw if file exists, FALSE = throw if file doesnt not exists
     * @throws IOException
     */
    public static void verifyFileExistence(String fileName, boolean failIfExists) throws IOException {
        if (fileName == null) {
            throw new NullPointerException("fileName cannot be null");
        }

        // non empty check allows the utility to work with backup servlet temporary files
        File file = new File(fileName);
        if (failIfExists && file.exists() && file.length() > 0) {
            throw new IOException("file '" + fileName + "' already exists and is not empty");
        }

        if (!failIfExists && !file.exists()) {
            throw new IOException("file '" + fileName + "' does not exists");
        }
    }

    /**
     * Test if can write and create the file.
     *
     * @param fileName  The file to be created
     * @throws IOException
     */
    public static void verifyCanWriteFile(String fileName) throws IOException {
        boolean isCreated = false;
        try {
            FileOutputStream fos = new FileOutputStream(fileName);
            fos.close();
            isCreated = true;
        } catch (IOException ioe) {
            throw new IOException("cannot write to '" + fileName + "'");
        } finally {
            //should only delete the file if it was actually created
            if (isCreated) {
                (new File(fileName)).delete();
            }
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

    //todo [Donal] Should createTmpDirectory be moved into FileUtils?
    public static String createTmpDirectory() throws IOException {
        File tmp = File.createTempFile("ssgflash", "tmp");
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
    public static int getLargestNameStringSize(final CommandLineOption [] options) {
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
     * @throws IOException if the directoryName is null, directoryName does not exist or does not represent a directory
     */
    public static void verifyDirExistence(String directoryName) throws IOException{
        if (directoryName == null) {
            throw new IOException("directoryName cannot be null");
        }

        // non empty check allows the utility to work with backup servlet temporary files
        File file = new File(directoryName);
        if(!file.exists()){
            throw new IOException("Directory '" + directoryName + "' does not exist");
        }

        if(!file.isDirectory()){
            throw new IOException("File '" + directoryName + "' is not a directory");
        }
    }

}