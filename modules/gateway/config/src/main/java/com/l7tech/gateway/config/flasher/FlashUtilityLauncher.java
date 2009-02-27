package com.l7tech.gateway.config.flasher;

import com.l7tech.util.SyspropUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

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
    /** System property used as base directory for all relative paths. */
    private static final String BASE_DIR_PROPERTY = "com.l7tech.server.flasher.basedir";
    private static final String LOGCONFIG_NAME = "backuputilitylogging.properties";
    private static final String SSGBACKUP_SH = "ssgbackup.sh";
    private static final String SSGRESTORE_SH = "ssgrestore.sh";
    private static final String IMPORT_TYPE = "import";
    private static final String EXPORT_TYPE = "export";
    private static final Logger logger = Logger.getLogger(FlashUtilityLauncher.class.getName());
    private static ArrayList<CommandLineOption> allRuntimeOptions = null;
    private static int argparseri = 0;

    public static void main(String[] args) {
        if (args == null || args.length < 1) {
            System.out.println(getUsage(null));
            return;
        }

        // set logging
        initializeLogging();

        try {
            Map<String, String> passedArgs;
            if (args[0].toLowerCase().equals("import")) {
                Importer importer = new Importer();
                passedArgs = importer.getParameters(args);
                importer.doIt(passedArgs);
                System.out.println("\nImport completed with no errors.");
            } else if (args[0].toLowerCase().equals("export")) {
                Exporter exporter = new Exporter(null, System.out, System.err);
                passedArgs = exporter.getParameters(args);
                exporter.doIt(passedArgs);
                System.out.println("\nExport of SecureSpan Gateway image completed with no errors.");
            } else if (args[0].toLowerCase().equals("cfgdeamon")) {
                OSConfigManager.restoreOSConfigFilesForReal();
            } else if (args[0] != null) {
                String issue = "unsupported option " + args[0];
                logger.warning(issue);
                System.out.println(issue);
                System.out.println(getUsage(args[0]));
                return;
            }
            logger.info("Operation complete without errors");
        } catch (InvalidArgumentException e) {
            String message = "Invalid argument due to '" + e.getMessage() + "'.";
            System.out.println(message);
            logger.log(Level.INFO, message);            
            System.out.println(getUsage(args[0]));
            System.exit(1);
        } catch (IOException e) {
            String message = "Error occurred due to '" + e.getMessage() + "'.";
            System.out.println(message);
            logger.log(Level.WARNING, message);
            System.exit(1);
        } catch (FatalException e) {
            String message = "Import failed due to '" + e.getMessage() + "'.";
            System.out.println(message);
            logger.log(Level.WARNING, message);
            System.exit(1);
        }
    }

    private static void initializeLogging() {
        final LogManager logManager = LogManager.getLogManager();
        final File file = new File(SyspropUtil.getString("java.util.logging.config.file", LOGCONFIG_NAME));
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

    /**
     * Prints the usages for the given utility type (import/export).
     *
     * @param utilityType   import or export
     * @return  The string containing the usages
     */
    private static String getUsage(final String utilityType) {
        StringBuffer output = new StringBuffer();

        //determine what is the utility type
        if (utilityType == null || "".equals(utilityType)) {
            //from legacy code (ssgmigration.sh) will perform based on import/export.  So for now, we'll
            //just output that if you do not specify import/export, it is required as a parameter
            //otherwise the ssgbackup.sh will always use export and ssgrestore.sh will always use import
            output.append("usage: [import | export] [OPTIONS]").append(EOL_CHAR);
            output.append("\tIMPORT OPTIONS:").append(EOL_CHAR);
            int largestNameStringSize = getLargestNameStringSize(Arrays.asList(Importer.ALLOPTIONS));
            for (CommandLineOption option : Importer.ALLOPTIONS) {
                output.append("\t")
                        .append(option.name)
                        .append(createSpace(largestNameStringSize-option.name.length() + 1))
                        .append(option.description)
                        .append(EOL_CHAR);
            }
            output.append(EOL_CHAR);
            output.append("\tEXPORT OPTIONS:").append(EOL_CHAR);
            largestNameStringSize = getLargestNameStringSize(Arrays.asList(Exporter.ALLOPTIONS));
            for (CommandLineOption option : Exporter.ALLOPTIONS) {
                output.append("\t")
                        .append(option.name)
                        .append(createSpace(largestNameStringSize-option.name.length() + 1))
                        .append(option.description)
                        .append(EOL_CHAR);
            }
        } else if (utilityType.equals(IMPORT_TYPE)) {
            output.append("usage: " + SSGRESTORE_SH + " [OPTIONS]").append(EOL_CHAR);
            output.append("\tOPTIONS:").append(EOL_CHAR);
            int largestNameStringSize = getLargestNameStringSize(Arrays.asList(Importer.ALLOPTIONS));
            for (CommandLineOption option : Importer.ALLOPTIONS) {
                output.append("\t")
                        .append(option.name)
                        .append(createSpace(largestNameStringSize-option.name.length() + 1))
                        .append(option.description)
                        .append(EOL_CHAR);
            }
        } else if (utilityType.equals(EXPORT_TYPE)) {
            output.append("usage: " + SSGBACKUP_SH + " [OPTIONS]").append(EOL_CHAR);
            output.append("\tOPTIONS:").append(EOL_CHAR);
            int largestNameStringSize = getLargestNameStringSize(Arrays.asList(Importer.ALLOPTIONS));
            for (CommandLineOption option : Exporter.ALLOPTIONS) {
                output.append("\t")
                        .append(option.name)
                        .append(createSpace(largestNameStringSize-option.name.length() + 1))
                        .append(option.description)
                        .append(EOL_CHAR);
            }
        }
        return output.toString();
    }

    /**
     * Traverses the list of options and determine the length of the longest option name.
     *
     * @param options   The list of options
     * @return          The length of the longest option name
     */
    private static int getLargestNameStringSize(final List<CommandLineOption> options) {
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
    private static String createSpace(int space) {
        StringBuffer spaces = new StringBuffer();
        for (int i=0; i <= space; i++) {
            spaces.append(" ");
        }
        return spaces.toString();
    }

    public static class InvalidArgumentException extends Exception {
        public InvalidArgumentException(String reason) {super(reason);}
    }

    public static class FatalException extends Exception {
        public FatalException(String reason) {super(reason);}
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
}
