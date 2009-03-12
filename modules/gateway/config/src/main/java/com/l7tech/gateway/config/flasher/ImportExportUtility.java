package com.l7tech.gateway.config.flasher;

import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.net.ConnectException;

import com.l7tech.gateway.config.flasher.FlashUtilityLauncher.InvalidArgumentException;
import com.l7tech.gateway.config.flasher.FlashUtilityLauncher.FatalException;
import com.l7tech.gateway.config.manager.db.DBActions;
import com.l7tech.gateway.config.client.beans.NodeManagementApiFactory;
import com.l7tech.server.management.config.node.DatabaseConfig;
import com.l7tech.server.management.api.node.NodeManagementApi;
import com.l7tech.server.management.NodeStateType;
import com.l7tech.util.BuildInfo;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.objectmodel.FindException;

/**
 * Base class for the import / export utility.
 *
 * User: dlee
 * Date: Feb 26, 2009
 */
public abstract class ImportExportUtility {

    private static enum ARGUMENT_TYPE {VALID_OPTION, IGNORED_OPTION, INVALID_OPTION, SKIP_OPTION, VALUE}
    private static final Logger logger = Logger.getLogger(ImportExportUtility.class.getName());

    public static final CommandLineOption SKIP_PRE_PROCESS = new CommandLineOption("-skipPreProcess", "skips pre-processing", false, true);
    public static final CommandLineOption[] SKIP_OPTIONS = {SKIP_PRE_PROCESS};

    private static final String pcUrl = "https://127.0.0.1:8765/services/nodeManagementApi";

    /**
     * @return  The list of all possible options for the provided utility.
     */
    public abstract List<CommandLineOption> getValidOptions();

    /**
     * @return  The list of all possible ignored options for the provided utility
     */
    public abstract List<CommandLineOption> getIgnoredOptions();

    /**
     * @return  Returns the type of utility to be performed (eg. export / import)
     */
    public abstract String getUtilityType();

    /**
     * Does pre process before actually carrying out the execution.
     * 1) Check if all required options are met and specified
     * 2) Check any additional options are valid
     *
     * @param args      The list of arguments
     * @throws InvalidArgumentException
     * @throws IOException
     * @throws FatalException
     */
    public abstract void preProcess(Map<String, String> args) throws InvalidArgumentException, IOException, FatalException;

    /**
     * Determines if the provided option has path as an option
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name has path as option, otherwise FALSE.
     * @throws InvalidArgumentException     The option name is not found in the available option list
     */
    private boolean optionIsValuePath(String optionName, final List<CommandLineOption> options) throws InvalidArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.name.equals(optionName)) {
                return commandLineOption.isValuePath;
            }
        }
        throw new InvalidArgumentException("option " + optionName + " is invalid");
    }

    /**
     * Determines if the provided option expects a value
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name expects a value, otherwise FALSE.
     * @throws InvalidArgumentException     The option name is not found in the available option list
     */
    private boolean optionHasNoValue(String optionName, final List<CommandLineOption> options) throws InvalidArgumentException {
        for (CommandLineOption commandLineOption : options) {
            if (commandLineOption.name.equals(optionName)) {
                return commandLineOption.hasNoValue;
            }
        }
        throw new InvalidArgumentException("option " + optionName + " is invalid");
    }

    /**
     * Determines if the provided option name is an option
     *
     * @param optionName    The option name
     * @param options       The list of options used to find the option if in the list
     * @return  TRUE if the option name is an option, otherwise FALSE.
     */
    private boolean isOption(String optionName, final List<CommandLineOption> options) {
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
    private ARGUMENT_TYPE determineArgumentType(String argument) {
        if (argument != null && argument.startsWith("-")) {
            if (isOption(argument, getValidOptions()))
                return ARGUMENT_TYPE.VALID_OPTION;
            else if (isOption(argument, getIgnoredOptions()))
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
    private int findIndexOfNextOption(final int startIndex, final String[] args) {
        int nextOptionIndex = startIndex;
        while (args != null && nextOptionIndex+1 < args.length) {
            if (!ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[++nextOptionIndex]))) {
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
    private String getFullValue(final String[] args, int startIndex, int endIndex) {
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
     * @throws InvalidArgumentException    If a particular argument is an invalid option.
     */
    public Map<String, String> getParameters(final String[] args) throws InvalidArgumentException {
        Map<String, String> arguments = new HashMap<String, String>();
        int index = 1;  //skip the first argument, because it's already used to determine the utility type (export/import)

        while (args != null && index < args.length) {
            final String argument = args[index];

            switch (determineArgumentType(argument)) {
                case SKIP_OPTION:
                    arguments.put(argument, "");    //skip options does not have values!
                    index++;
                    break;
                case VALID_OPTION:
                    if (optionHasNoValue(argument, getValidOptions())) {
                        arguments.put(argument, "");
                        index++;
                    } else if (!optionIsValuePath(argument, getValidOptions())) {
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1]))) {
                            arguments.put(argument, args[index+1]);
                            index = index + 2;  //already processed one, so we'll need to advance to the next after the processed
                        } else {
                            //expecting a value for this parameter but there was none
                            throw new InvalidArgumentException("option " + argument + " expects value");
                        }
                    } else {
                        int nextOptionIndex = findIndexOfNextOption(index, args);
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1])) && index+1 < nextOptionIndex) {
                            String fullValue = getFullValue(args, index+1, nextOptionIndex);
                            arguments.put(argument, fullValue);
                            index = nextOptionIndex;
                        } else {
                            throw new InvalidArgumentException("option " + argument + " expects value");
                        }
                    }
                    break;
                case IGNORED_OPTION:
                    if (optionHasNoValue(argument, getIgnoredOptions())) {
                        logger.info("Option '" + argument + "' is ignored.");
                        index++;
                    } else if (!optionIsValuePath(argument, getIgnoredOptions())) {
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1]))) {                            
                            index = index + 2;
                        } else {
                            //we dont care, just continue to process
                            index++;
                        }
                        logger.info("Option '" + argument + " " + args[index] + "' is ignored.");
                    } else {
                        int nextOptionIndex = findIndexOfNextOption(index, args);
                        if (index+1 < args.length && ARGUMENT_TYPE.VALUE.equals(determineArgumentType(args[index+1])) && index+1 < nextOptionIndex) {
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
                    throw new InvalidArgumentException("option " + argument + " is invalid");
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
    public void verifyDatabaseConnection(DatabaseConfig config, boolean isRootAccount) throws IOException {
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
    public void verifyFileExistence(String fileName, boolean failIfExists) throws IOException {
        if (fileName == null) {
            throw new IOException("file is null");
        }

        File file = new File(fileName);
        if (failIfExists && file.exists()) {
            throw new IOException("file '" + fileName + "' already exists");
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
    public void verifyCanWriteFile(String fileName) throws IOException {
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
     * Verifies that the product version from build info matches to the given version.
     *
     * @param version   The version to be compared
     * @throws InvalidArgumentException
     */
    public void verifyDatabaseVersion(String version) throws InvalidArgumentException {
        if (!BuildInfo.getProductVersion().equals(version)) {
             throw new InvalidArgumentException("Invalid database version");
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
    public boolean verifyDatabaseExists(String host, String dbName, int port, String username, String password) {
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
     * Uses the process controller to determine if the local gateway is running.  If it fails to communicate with the
     * process controller it will assume that the gateway is not running.
     * <b>NOTE:</b> Software gateway version will NOT have process controller.  So, be sure to know when to use this method.
     *
     * @return  TRUE if gatway is running, otherwise FALSE.
     */
    public boolean isLocalNodeRunning() {
        boolean isRunning = false;
        System.setProperty("org.apache.cxf.nofastinfoset", "true");
        try {
            NodeManagementApiFactory nodeManagementApiFactory = new NodeManagementApiFactory( pcUrl );
            NodeManagementApi nodeManagementApi = nodeManagementApiFactory.getManagementService();

            Collection<NodeManagementApi.NodeHeader> nodes = nodeManagementApi.listNodes();
            if (nodes != null) {
                if (nodes.size() > 1) {
                    logger.info("More than one node on host, will need to determine status of all nodes in the local host.");
                }
                for (NodeManagementApi.NodeHeader node : nodes) {
                    if (!NodeStateType.STOPPED.equals(node.getState())) {
                        isRunning = true;
                        break;
                    }
                }
            } else {
                isRunning = false;
            }
        } catch (FindException fe) {
            //cannot find nodes
            isRunning = true;
        } catch (Exception e) {
            if (ExceptionUtils.causedBy(e, ConnectException.class)) {
                //failed to connect to PC, assume local node is not running
                isRunning = false;
            } else {
                isRunning = true;
            }
        }

        return isRunning;
    }
}
