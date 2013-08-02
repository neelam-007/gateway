package com.l7tech.gateway.config.backuprestore;

import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.SyspropUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static com.l7tech.gateway.config.backuprestore.ImportExportUtilities.UtilityResult.Status;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * Run backup / restore / migrate 
 */
public class BackupRestoreLauncher {
    public static final String EOL_CHAR = SyspropUtil.getProperty( "line.separator" );
    private static final String BACKUP_LOGCONFIG_NAME = "backuputilitylogging.properties";
    private static final String RESTORE_LOGCONFIG_NAME = "restore_logging.properties";
    private static final String SSGBACKUP_SH = "ssgbackup.sh";
    private static final String SSGRESTORE_SH = "ssgrestore.sh";
    private static final String SSGMIGRATE_SH = "ssgmigrate.sh";
    private static final Logger logger = Logger.getLogger(BackupRestoreLauncher.class.getName());
    private static final String SECURE_SPAN_HOME = "C:\\Users\\vkazakov\\Workspaces\\SSG\\build\\deploy";

    public enum UTILITY_TYPE{
        BACKUP("Backup"),
        RESTORE("Restore"),
        MIGRATE("Migrate"),
        CFGDEAMON("cfgdeamon");

        private final String name;

        UTILITY_TYPE(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static UTILITY_TYPE getUtilityType(final String utilityType){

            for(UTILITY_TYPE ut: UTILITY_TYPE.values()){
                if(ut.getName().equalsIgnoreCase(utilityType)){
                    return ut;
                }
            }
            throw new IllegalArgumentException("Unsupported value '" + utilityType+"'");
        }
    }

    /**
     * If the Backup / Restore / Migrate fails, then the VM will exist with status 1. If there is a partially successfull
     * backup, then the VM will exist with status 2
     */
    public static void main(final String[] args) {
        if (args == null || args.length < 1) {
            System.out.println("usage: [restore | migrate | backup] [OPTIONS]");
            return;
        }

        final UTILITY_TYPE utilityType = UTILITY_TYPE.getUtilityType(args[0]);
        initializeLogging(utilityType);

        //when noConsoleOutput is true, no System.out.println calls should happen, regardless of error conditions
        //this is for when cfgdaemon target is ran on system reboot, errors should still be logged
        final boolean noConsoleOutput = (utilityType == UTILITY_TYPE.CFGDEAMON);

        try {

            final Functions.UnaryThrows<
                    ImportExportUtilities.UtilityResult,
                    String[],
                    ? extends Exception> utilityFunction = getUtilityFunction(utilityType);

            final ImportExportUtilities.UtilityResult result = utilityFunction.call(args);

            switch(result.getStatus()){
                case SUCCESS:
                    final String msg = "\n" + utilityType.getName() + " completed with no errors." +
                            ((result.isRebootMaybeRequired())?" Please reboot to complete the process.":"");
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.INFO, msg, true, System.out);
                    break;
                case FAILURE:
                    Exception e = result.getException();
                    final String msg1 = "\n" + utilityType.getName() + " failed" + ((e != null) ? ": " + e.getMessage() : "");
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.SEVERE, msg1, true, System.out);
                    System.exit(1);
                    break;
                case PARTIAL_SUCCESS:
                    final String partialSuccessMsg = "\n" + utilityType.getName() + " of Gateway image partially succeeded." +
                            ((result.isRebootMaybeRequired())?" Please reboot to complete the process.":"");
                    ImportExportUtilities.logAndPrintMajorMessage(logger, Level.SEVERE, partialSuccessMsg, true, System.out);
                    List<String> failedComponents = result.getFailedComponents();
                    for (String s : failedComponents) {
                        ImportExportUtilities.logAndPrintMajorMessage(logger, Level.SEVERE, "Failed component: " + s, true, System.out);
                    }
                    System.exit(2);
                    break;
                default:
                    throw new RuntimeException("Unexpected response from " + utilityType.getName().toLowerCase());
            }

        }catch (Exception e) {
            if(e instanceof InvalidProgramArgumentException
                    || ExceptionUtils.causedBy(e, InvalidProgramArgumentException.class)){
                final boolean wrappedInRuntime = ExceptionUtils.causedBy(e, InvalidProgramArgumentException.class);
                final String exceptionMsg = (wrappedInRuntime)?
                        ExceptionUtils.getCauseIfCausedBy(e, InvalidProgramArgumentException.class).getMessage()
                        : e.getMessage();

                String message = utilityType.getName() + " invalid argument: " + exceptionMsg;
                if (!noConsoleOutput) System.out.println(message);
                logger.log(Level.WARNING, message);
                if (!noConsoleOutput) System.out.println(getUsage(utilityType));
                System.exit(1);
            }else{
                String message = utilityType.getName()+ " failed: " + ExceptionUtils.getMessage(e, "Unknown error");
                if (!noConsoleOutput) System.out.println(message);
                logger.log(Level.WARNING, message);
                System.exit(1);
            }
        }
    }

    private static Functions.UnaryThrows<
            ImportExportUtilities.UtilityResult,
            String[],
            ? extends Exception> getUtilityFunction(final UTILITY_TYPE utilityType) {
        switch (utilityType) {
            case RESTORE:
                return new Functions.UnaryThrows<ImportExportUtilities.UtilityResult, String[], Exception>() {
                    @Override
                    public ImportExportUtilities.UtilityResult call(final String[] args)
                            throws Exception {
                        final Importer importer = new Importer(new File(SECURE_SPAN_HOME), System.out);
                        return importer.restoreOrMigrateBackupImage(args);
                    }
                };
            case MIGRATE:
                return new Functions.UnaryThrows<ImportExportUtilities.UtilityResult, String[], Exception>() {
                    @Override
                    public ImportExportUtilities.UtilityResult call(final String[] args)
                            throws Exception {
                        final Importer importer = new Importer(new File(SECURE_SPAN_HOME), System.out);
                        return importer.restoreOrMigrateBackupImage(MigrateToRestoreConvertor.getConvertedArguments(args, System.out));
                    }
                };
            case BACKUP:
                return new Functions.UnaryThrows<ImportExportUtilities.UtilityResult, String[], Exception>() {
                    @Override
                    public ImportExportUtilities.UtilityResult call(final String[] args)
                            throws Exception {
                        final Exporter exporter = new Exporter(new File(SECURE_SPAN_HOME), System.out);
                        return exporter.createBackupImage(args);
                    }
                };
            case CFGDEAMON:
                return new Functions.UnaryThrows<ImportExportUtilities.UtilityResult, String[], Exception>() {
                    @Override
                    public ImportExportUtilities.UtilityResult call(final String[] notUsed)
                            throws Exception {
                        final File ssgHome = new File(SECURE_SPAN_HOME, ImportExportUtilities.GATEWAY);
                        final OSConfigManager osConfigManager =
                                new OSConfigManager(ssgHome, true, false, null);
                        final boolean filesWereCopied = osConfigManager.finishRestoreOfFilesOnReboot();
                        //this is the only output from cfgdaemon
                        if (filesWereCopied) System.out.println("Files were restored by the cfgdaemon");
                        return new ImportExportUtilities.UtilityResult(Status.SUCCESS);
                    }
                };
        }

        throw new IllegalArgumentException("Unknown utility type: " + utilityType.getName());
    }

    private static void initializeLogging(final UTILITY_TYPE utilityType) {
        final LogManager logManager = LogManager.getLogManager();

        final String logFileToUseAsDefault;

        if(utilityType == UTILITY_TYPE.BACKUP){
            logFileToUseAsDefault = BACKUP_LOGCONFIG_NAME;
        }else{
            logFileToUseAsDefault = RESTORE_LOGCONFIG_NAME;
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
                    System.err.println("Cannot close logging properties input stream " + e.getMessage());
                }
            }
        } else {
            System.err.println("Cannot initialize logging");
        }
    }

    /**
     * Prints the usages for the given utility type (restore / migrate / backup).
     *
     * @param utilityType   restore / migrate / backup
     * @return  The string containing the correct usages for the supplied utilityType
     */
    private static String getUsage(final UTILITY_TYPE utilityType) {
        StringBuilder output = new StringBuilder();

        switch (utilityType) {
            case RESTORE:
                output.append("usage: " + SSGRESTORE_SH + " [OPTIONS]").append(EOL_CHAR);
                output.append("\tOPTIONS:").append(EOL_CHAR);
                Importer.getRestoreUsage(output);
                break;
            case MIGRATE:
                output.append("usage: " + SSGMIGRATE_SH + " [OPTIONS]").append(EOL_CHAR);
                output.append("\tOPTIONS:").append(EOL_CHAR);
                Importer.getMigrateUsage(output);
                break;
            case BACKUP:
                output.append("usage: " + SSGBACKUP_SH + " [OPTIONS]").append(EOL_CHAR);
                output.append("\tOPTIONS:").append(EOL_CHAR);
                Exporter.getExporterUsage(output);
                break;
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
