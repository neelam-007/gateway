package com.l7tech.server.flasher;

import com.l7tech.server.config.OSDetector;

import java.util.HashMap;
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
    private static final String LOGCONFIG_NAME = "flasherlogging.properties";
    private static final Logger logger = Logger.getLogger(FlashUtilityLauncher.class.getName());

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
                System.out.println("\nImport completed with no errors. You may restart the target SecureSpan Gateway.");
            } else if (args[0].toLowerCase().equals("export")) {
                Exporter exporter = new Exporter();
                exporter.doIt(passedArgs);
                System.out.println("\nExport of SecureSpan Gateway image completed with no errors.");
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
        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            if (!arg.startsWith("-")) {
                throw new InvalidArgumentException("Invalid argument name: " + arg);
            }
            i++;
            if (i >= args.length) {
                throw new InvalidArgumentException("argument name: " + arg + " not provided a value");
            }
            String val = args[i];
            output.put(arg, val);
        }
        return output;
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
            output.append("usage: ssgflash.cmd [import | export] [OPTIONS]").append(EOL_CHAR);
        } else {
            output.append("usage: ssgflash.sh [import | export] [OPTIONS]").append(EOL_CHAR);
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
}
