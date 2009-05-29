package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;

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
public class BackupRestoreLauncher {
    public static final String EOL_CHAR = System.getProperty("line.separator");
    private static final String LOGCONFIG_NAME = "backuputilitylogging.properties";
    private static final String SSGBACKUP_SH = "ssgbackup.sh";
    private static final String SSGRESTORE_SH = "ssgrestore.sh";
    private static final String IMPORT_TYPE = "import";
    private static final String EXPORT_TYPE = "export";
    private static final Logger logger = Logger.getLogger(BackupRestoreLauncher.class.getName());
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
                final Importer importer = new Importer();
                passedArgs = ImportExportUtilities.getAndValidateCommandLineOptions(
                        args, Arrays.asList(Importer.ALLOPTIONS), Importer.getIgnoredOptions());
                importer.preProcess(passedArgs);
                importer.doIt(passedArgs, null);
                System.out.println("\nImport completed with no errors.");
            } else if (args[0].toLowerCase().equals("export")) {
                final Exporter exporter = new Exporter(new File("/opt/SecureSpan/Gateway"),
                        System.out, ImportExportUtilities.OPT_SECURE_SPAN_APPLIANCE);
                exporter.createBackupImage(args);
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
        } catch (InvalidProgramArgumentException e) {
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
        } catch (Exception e) {
            //this catches all runtime Exceptions which can be thrown for Null pointers, Illegal States and Unsupported Ops
            String message = "Import / Export failed due to '" + ExceptionUtils.getMessage(e,"Unknown error") + "'.";
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
        StringBuilder output = new StringBuilder();

        //determine what is the utility type
        if (utilityType == null || "".equals(utilityType)) {
            //from legacy code (ssgmigration.sh) will perform based on import/export.  So for now, we'll
            //just output that if you do not specify import/export, it is required as a parameter
            //otherwise the ssgbackup.sh will always use export and ssgrestore.sh will always use import
            output.append("usage: [import | export] [OPTIONS]").append(EOL_CHAR);
            output.append("\tIMPORT OPTIONS:").append(EOL_CHAR);
            Importer.getImporterUsage(output);
            output.append("\tEXPORT OPTIONS:").append(EOL_CHAR);
            Exporter.getExporterUsage(output);
        } else if (utilityType.equals(IMPORT_TYPE)) {
            output.append("usage: " + SSGRESTORE_SH + " [OPTIONS]").append(EOL_CHAR);
            output.append("\tOPTIONS:").append(EOL_CHAR);
            Importer.getImporterUsage(output);
        } else if (utilityType.equals(EXPORT_TYPE)) {
            output.append("usage: " + SSGBACKUP_SH + " [OPTIONS]").append(EOL_CHAR);
            output.append("\tOPTIONS:").append(EOL_CHAR);
            Exporter.getExporterUsage(output);
        }
        return output.toString();
    }


    /**
     * This class represents ane exception when a required program parameter is missing or has an invalid value.
     * An invalid value includes when a program parameter value has a value representing a file, and the file
     * does not exist or cannot be written to
     */
    public static class InvalidProgramArgumentException extends Exception{
        public InvalidProgramArgumentException(String reason) {super(reason);}
    }

    //todo [Donal] are these exceptions needed?
    public static class FatalException extends Exception {
        public FatalException(String reason) {super(reason);}
    }

}