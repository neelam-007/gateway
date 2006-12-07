package com.l7tech.server.flasher;

import com.l7tech.server.config.OSDetector;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.IOException;
import java.io.File;
import java.io.InputStream;

/**
 * Entrypoint for the ssg flashing utility.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Nov 8, 2006<br/>
 */
public class FlashUtilityLauncher {
    public static final String EOL_CHAR = System.getProperty("line.separator");
    private static final String LOGCONFIG_NAME = "migrationlogging.properties";
    private static final Logger logger = Logger.getLogger(FlashUtilityLauncher.class.getName());
    private static ArrayList<CommandLineOption> allRuntimeOptions = null;
    private static int argparseri = 0;

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            printusage();
            return;
        }

        // set logging
        initializeLogging();

        try {
            HashMap<String, String> passedArgs = parseArguments(args);
            if (args[0].toLowerCase().equals("import")) {
                Importer importer = new Importer();
                importer.doIt(passedArgs);
                System.out.println("\nImport completed with no errors.");
            } else if (args[0].toLowerCase().equals("export")) {
                Exporter exporter = new Exporter();
                exporter.doIt(passedArgs);
                System.out.println("\nExport of SecureSpan Gateway image completed with no errors.");
            } else if (args[0].toLowerCase().equals("cfgdeamon")) {
                OSConfigManager.restoreOSConfigFilesForReal();
            } else if (args[0] != null) {
                String issue = "unsupported option " + args[0];
                logger.warning(issue);
                System.out.println(issue);
                printusage();
                return;
            }
            logger.info("Operation complete without errors");
        } catch (InvalidArgumentException e) {
            System.out.println(e.getMessage());
            logger.log(Level.INFO, "User error. Bad arguments.", e);
            printusage();
        } catch (IOException e) {
            System.out.println("Error, consult log file.");
            logger.log(Level.WARNING, "Could not perform operation", e);
            System.out.println(e.getMessage());
        }
    }

    private static HashMap<String, String> parseArguments(String[] args) throws InvalidArgumentException {
        HashMap<String, String> output = new HashMap<String, String>();
        for (argparseri = 1; argparseri < args.length; argparseri++) {
            String arg = args[argparseri];
            boolean isPath = isOptionPath(arg);
            boolean hasNoValue = hasOptionNoValue(arg);
            if (arg != null && arg.length() > 0) {
                if (!arg.startsWith("-")) {
                    throw new InvalidArgumentException("Invalid argument name: " + arg);
                }
                if (hasNoValue) {
                    output.put(arg, "");
                } else {
                    if ((argparseri+1) >= args.length) {
                        throw new InvalidArgumentException("A value must be specified for option: " + arg);
                    }
                    argparseri++;
                    String val = getFullValue(args, isPath);
                    logger.info(arg + " with value _" + val + "_");
                    output.put(arg, val);
                }
            }
        }
        return output;
    }

    private static String getFullValue(String[] in, boolean ispath) {
        if (OSDetector.isWindows() || !ispath) {
            return in[argparseri];
        } else {
            String tmp = in[argparseri];
            while ((argparseri+1) < in.length) {
                if (tmp.endsWith("\\")) tmp = tmp.substring(0, tmp.length() - 1);
                if (!isOption(in[argparseri+1])) {
                    tmp = tmp + " " + in[argparseri+1];
                    argparseri++;
                } else break;
            }
            logger.info("reconstructing option value " + tmp);
            return tmp;
        }
    }

    private static void initializeLogging() {
        final LogManager logManager = LogManager.getLogManager();
        final File file = new File(LOGCONFIG_NAME);
        if (file.exists()) {
            InputStream in = null;
            try {
                in = file.toURI().toURL().openStream();
                if (in != null) {
                    logManager.readConfiguration(in);
                }
            } catch (IOException e) {
                System.err.println("Cannot initialize logging " + e.getMessage());
            } finally {
                try {
                    if (in != null) in.close();
                } catch (IOException e) { // should not happen
                    System.err.println("cannot close logging properties input stream " + e.getMessage());
                }
            }
        } else {
            System.err.println("Cannot initialize logging");
        }
    }

    private static void printusage() {
        StringBuffer output = new StringBuffer();
        if (OSDetector.isWindows()) {
            output.append("usage: ssgmigration.cmd [import | export] [OPTIONS]").append(EOL_CHAR);
        } else {
            output.append("usage: ssgmigration.sh [import | export] [OPTIONS]").append(EOL_CHAR);
        }
        output.append("\tIMPORT OPTIONS:").append(EOL_CHAR);
        for (CommandLineOption option : Importer.ALLOPTIONS) {
            output.append("\t").append(option.name).append("\t\t").append(option.description).append(EOL_CHAR);
        }
        output.append(EOL_CHAR);
        output.append("\tEXPORT OPTIONS:").append(EOL_CHAR);
        for (CommandLineOption option : Exporter.ALLOPTIONS) {
            output.append("\t").append(option.name).append("\t\t").append(option.description).append(EOL_CHAR);
        }
        System.out.println(output);
    }

    public static class InvalidArgumentException extends Exception {
        public InvalidArgumentException(String reason) {super(reason);}
    }

    public static boolean isOptionPath(String optionname) throws InvalidArgumentException {
        getOptions();
        for (CommandLineOption commandLineOption : allRuntimeOptions) {
            if (commandLineOption.name.equals(optionname)) {
                return commandLineOption.isValuePath;
            }
        }
        throw new InvalidArgumentException("option " + optionname + " is invalid");
    }

    private static ArrayList<CommandLineOption> getOptions() {
        if (allRuntimeOptions == null) {
            allRuntimeOptions = new ArrayList<CommandLineOption>();
            for (CommandLineOption aALLOPTIONS : Importer.ALLOPTIONS) {
                allRuntimeOptions.add(aALLOPTIONS);
            }
            for (CommandLineOption aALLOPTIONS1 : Exporter.ALLOPTIONS) {
                allRuntimeOptions.add(aALLOPTIONS1);
            }
        }
        return allRuntimeOptions;
    }

    public static boolean hasOptionNoValue(String optionname) throws InvalidArgumentException {
        getOptions();
        for (CommandLineOption commandLineOption : allRuntimeOptions) {
            if (commandLineOption.name.equals(optionname)) {
                return commandLineOption.hasNoValue;
            }
        }
        throw new InvalidArgumentException("option " + optionname + " is invalid");
    }

    private static boolean isOption(String optionname) {
        getOptions();
        for (CommandLineOption commandLineOption : allRuntimeOptions) {
            if (commandLineOption.name.equals(optionname)) {
                return true;
            }
        }
        return false;
    }
}
