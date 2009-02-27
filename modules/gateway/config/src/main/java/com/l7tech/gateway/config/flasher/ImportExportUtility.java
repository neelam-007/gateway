package com.l7tech.gateway.config.flasher;

import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;

import com.l7tech.gateway.config.flasher.FlashUtilityLauncher.InvalidArgumentException;

/**
 * Base class for the import / export utility.
 *
 * User: dlee
 * Date: Feb 26, 2009
 */
public abstract class ImportExportUtility {

    private static enum ARGUMENT_TYPE {VALID_OPTION, IGNORED_OPTION, INVALID_OPTION, VALUE}
    private static final Logger logger = Logger.getLogger(ImportExportUtility.class.getName());

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
}
