package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.PartitionActions;
import com.l7tech.server.config.PartitionActionListener;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep implements PartitionActionListener {
    private static final Logger logger = Logger.getLogger(ConfigWizardConsoleResultsStep.class.getName());
        
    private String manualStepsFileName = "ssg_config_manual_steps.txt";
    private String title = "Configuration Results";
    private List<String> manualSteps;
    private String logFilename = "ssgconfig0.log";
    private File manualStepsFile;
    private static final String CONFIG_ERRORS_TEXT = getEolChar() + "*** Configuration problems detected: There were warnings and/or errors during configuration, see the logs above for details. ***" + getEolChar();
    private String successMsg = "The configuration was successfully applied." + getEolChar() + "You must restart the SSG in order for the configuration to take effect." + getEolChar();

    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        showNavigation = false;
    }

    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz, String title) {
        this(parentWiz);
        this.title = title;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        ConfigurationWizard wizard = getParentWizard();

        if (wizard.isHadFailures())
            printText("There were errors during configuration, see below for details" + getEolChar());
        else {
            printText(successMsg);
        }

        if (needsManualSteps()) {
            if (saveManualSteps() ) {
                printText(getEolChar() + "**** Some manual steps are required to complete the configuration of the SSG ****" + getEolChar());
                printText("\tThese manual steps have been saved to a the file: " + manualStepsFile.getAbsolutePath() + getEolChar());
            }
        }

        printText(getEolChar() + "The following is a summary of the actions taken by the wizard" + getEolChar());
        printText("\tThese logs have been saved to the file: "+ logFilename + getEolChar());

        printText(getEolChar());

        List<String> logs = ListHandler.getLogList();
        if (logs != null) {
            for (String log : logs) {
                if (log != null) printText(log + getEolChar());
            }
        }

        if (osFunctions.isWindows() && !getParentWizard().isHadFailures()) {
            try {
                boolean shouldStart;
                shouldStart = getConfirmationFromUser(getEolChar() + "Would you like to start the partition that was just configured?", "n");
                if (shouldStart) {
                    PartitionInformation pi = PartitionManager.getInstance().getActivePartition();
                    if (!PartitionActions.startService(pi, this)) {
                        printText(getEolChar() + "Couldn't start the service. Please start the service manually");
                    }
                }
            } catch (IOException e) {
                logger.severe(e.getMessage());
                printText(getEolChar() + "Couldn't start the service [" + e.getMessage() + "]" + getEolChar());
            }
        }
        if (wizard.isHadFailures()) {
            printText(CONFIG_ERRORS_TEXT);
            printText(getEolChar() + "The wizard will now exit." + getEolChar());
        }
        else
            printText(getEolChar() + "Configuration complete. The wizard will now exit." + getEolChar());
    }

    private boolean needsManualSteps() {
        if (manualSteps == null) manualSteps = getParentWizard().getManualSteps();
        return (manualSteps != null && !manualSteps.isEmpty());
    }

    private boolean saveManualSteps() {
        boolean success = true;

        StringBuilder stepsBuffer = new StringBuilder();
        StringBuilder allSteps = new StringBuilder();

        if (needsManualSteps()) {
            for (String manualStep : manualSteps) {
                allSteps.append(manualStep);
            }

            stepsBuffer.append("The following manual steps are required to complete the configuration of the SSG");
            stepsBuffer.append(getEolChar()).append(getEolChar());
            stepsBuffer.append(allSteps).append(getEolChar());
            stepsBuffer.append(getEolChar());
            stepsBuffer.append(getEolChar());

            PrintWriter saveWriter = null;
            try {
                manualStepsFile = new File(manualStepsFileName);
                saveWriter = new PrintWriter(manualStepsFileName);
                saveWriter.print(convertStepsForConsole(stepsBuffer.toString()));
            } catch (FileNotFoundException e) {
                printText("Could not create file: " + manualStepsFile.getAbsolutePath() + getEolChar());
                printText(e.getMessage() + getEolChar());
                success = false;
            } finally {
                if (saveWriter != null) saveWriter.close();
            }
        }
        return success;
    }

    private String convertStepsForConsole(String originalSteps) {
        String convertedSteps;
        //convert UL and LI to an equivalent form
        convertedSteps = originalSteps.replaceAll("<[Bb][Rr]>", getEolChar());
        convertedSteps = convertedSteps.replaceAll("<[Uu][Ll]>|<[Dd][Ll]>|</.*>", "");
        convertedSteps = convertedSteps.replaceAll("<[Pp]>", getEolChar() + "\t ");
        convertedSteps = convertedSteps.replaceAll("<[Ll][Ii]>", "\t* ");
        convertedSteps = convertedSteps.replaceAll("<[Dd][Tt]>", "\t\t- ");
        convertedSteps = convertedSteps.replaceAll("<[Dd][Dd]>", "\t\t\t- ");
        //strip out any html tags other than those that have been converted
        return convertedSteps;
    }

    public String getManualStepsFileName() {
        return manualStepsFileName;
    }

    public void setManualStepsFileName(String fileName) {
        this.manualStepsFileName = fileName;
    }

    public String getLogFilename() {
        return logFilename;
    }

    public void setLogFilename(String logFilename) {
        this.logFilename = logFilename;
    }

    public String getTitle() {
        return title;
    }

    public boolean validateStep() {
        return true;
    }

    public boolean isShowQuitMessage() {
        return false;
    }

    public boolean getPartitionActionsConfirmation(String message) {
        boolean ok = false;
        try {
            getConfirmationFromUser(message, "n");
        } catch (IOException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        } catch (WizardNavigationException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        }
        return ok;
    }

    public void showPartitionActionErrorMessage(String message) {
        List<String> messages = new ArrayList<String>();
        messages.add("**** " + message + " ****");
        messages.add("Press Enter to continue");
        try {
            getData(messages.toArray(new String[0]),"", (String[]) null,null);
        } catch (IOException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        } catch (WizardNavigationException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        }
    }

    public void showPartitionActionMessage(String message) {
        List<String> messages = new ArrayList<String>();
        messages.add(message);
        messages.add("Press Enter to continue");
        try {
            getData(messages.toArray(new String[0]),"", (String[]) null,null);
        } catch (IOException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        } catch (WizardNavigationException e) {
            logger.severe("Error while getting input. [" + e.getMessage() + "]");
        }
    }

    public void setSuccessMessage(String msg) {
        successMsg = msg;
    }
}
