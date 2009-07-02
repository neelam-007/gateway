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
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * Run backup / restore 
 */
public class BackupRestoreLauncher {
    public static final String EOL_CHAR = System.getProperty("line.separator");
    private static final String BACKUP_LOGCONFIG_NAME = "backuputilitylogging.properties";
    private static final String RESTORE_LOGCONFIG_NAME = "restore_logging.properties";
    private static final String SSGBACKUP_SH = "ssgbackup.sh";
    private static final String SSGRESTORE_SH = "ssgrestore.sh";
    private static final String SSGMIGRATE_SH = "ssgmigrate.sh";
    private static final String IMPORT_TYPE = "import";
    private static final String EXPORT_TYPE = "export";
    private static final String MIGRATE_TYPE = "migrate";
    private static final String CFGDAEMON_TYPE = "cfgdeamon";
    private static final Logger logger = Logger.getLogger(BackupRestoreLauncher.class.getName());
    private static final String SECURE_SPAN_HOME = "/opt/SecureSpan";

    /**
     * If the Backup / Import fails, then the VM will exist with status 1. If there is a partially successfull
     * backup, then the VM will exist with status 2
     * @param args
     */
    public static void main(final String[] args) {
        if (args == null || args.length < 1) {
            System.out.println(getUsage(null));
            return;
        }

        initializeLogging(args[0]);

        try {
            if (args[0].equalsIgnoreCase(IMPORT_TYPE) || args[0].equalsIgnoreCase(MIGRATE_TYPE)) {
                final String [] argsToUse;
                if(args[0].equalsIgnoreCase(MIGRATE_TYPE)){
                    argsToUse = MigrateToRestoreConvertor.getConvertedArguments(args);
                }else{
                    argsToUse = args;
                }

                final Importer importer = new Importer(new File(SECURE_SPAN_HOME), System.out);

                Importer.RestoreMigrateResult result = importer.restoreOrMigrateBackupImage(argsToUse);
                switch (result.getStatus()){
                    case SUCCESS:
                        if(result.isWasMigrate()){
                            System.out.println("\nMigrate of SecureSpan Gateway image completed with no errors");
                        }else{
                            System.out.println("\nRestore of SecureSpan Gateway image completed with no errors");
                        }
                        break;
                    case FAILURE:
                        if(result.isWasMigrate()){
                            System.out.println("\nMigrate of SecureSpan Gateway image failed");
                        }else{
                            System.out.println("\nRestore of SecureSpan Gateway image failed");
                        }
                        Exception e = result.getException();
                        if(e != null) System.out.println(e.getMessage());
                        System.exit(1);
                        break;
                    case PARTIAL_SUCCESS:
                        if(result.isWasMigrate()){
                            System.out.println("\nMigrate of SecureSpan Gateway image partially succeeded");
                        }else{
                            System.out.println("\nRestore of SecureSpan Gateway image partially succeeded");
                        }
                        List<String> failedComponents = result.getFailedComponents();
                        for(String s: failedComponents ){
                            System.out.println("Failed component: " + s);
                        }
                        System.exit(2);
                        break;
                    default:
                        throw new RuntimeException("Unexpected response from import");

                }
            } else if (args[0].equalsIgnoreCase(EXPORT_TYPE)) {
                final Exporter exporter = new Exporter(new File(SECURE_SPAN_HOME), System.out);
                Exporter.BackupResult result = exporter.createBackupImage(args);
                switch (result.getStatus()){
                    case SUCCESS:
                        System.out.println("\nExport of SecureSpan Gateway image completed with no errors.");
                        break;
                    case FAILURE:
                        System.out.println("\nExport of SecureSpan Gateway image failed.");
                        Exception e = result.getException();
                        if(e != null) System.out.println(e.getMessage());
                        System.exit(1);
                        break;
                    case PARTIAL_SUCCESS:
                        System.out.println("\nExport of SecureSpan Gateway image partially succeeded");
                        List<String> failedComponents = result.getFailedComponents();
                        for(String s: failedComponents ){
                            System.out.println("Failed component: " + s);
                        }
                        System.exit(2);
                        break;
                    default:
                        throw new RuntimeException("Unexpected response from export");

                }
            } else if (args[0].equalsIgnoreCase(CFGDAEMON_TYPE)) {
                final OSConfigManager osConfigManager =
                        new OSConfigManager(new File(SECURE_SPAN_HOME), true, false, null);
                try {
                    osConfigManager.finishRestoreOfFilesOnReboot();
                } catch (OSConfigManager.OSConfigManagerException e) {
                    logger.log(Level.SEVERE, "Exception running cfgdaemon target of ssgrestore: " + e.getMessage());
                    System.exit(1);
                }
            } else if (args[0] != null) {
                String issue = "Unsupported option " + args[0];
                logger.warning(issue);
                System.out.println(issue);
                System.out.println(getUsage(args[0]));
            }
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

    private static void initializeLogging(final String launcherTarget) {
        final LogManager logManager = LogManager.getLogManager();

        final String logFileToUseAsDefault;
        if(launcherTarget.equalsIgnoreCase(IMPORT_TYPE) || launcherTarget.equalsIgnoreCase(MIGRATE_TYPE)){
            logFileToUseAsDefault = RESTORE_LOGCONFIG_NAME;
        }else{
            logFileToUseAsDefault = BACKUP_LOGCONFIG_NAME;
        }

        final File file = new File(SyspropUtil.getString("java.util.logging.config.file", logFileToUseAsDefault));
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
        }else if (utilityType.equals(MIGRATE_TYPE)) {
            output.append("usage: " + SSGMIGRATE_SH + " [OPTIONS]").append(EOL_CHAR);
            output.append("\tOPTIONS:").append(EOL_CHAR);
            Importer.getMigrateUsage(output);
        }  else if (utilityType.equals(EXPORT_TYPE)) {
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

    public static class FatalException extends Exception {
        public FatalException(String reason) {super(reason);}
    }

}