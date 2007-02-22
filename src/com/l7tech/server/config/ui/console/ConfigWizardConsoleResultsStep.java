package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.*;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep{

    private String manualStepsFileName = "ssg_config_manual_steps.txt";
    private String title = "Configuration Results";
    private List<String> manualSteps;
    private String logFilename = "ssgconfig0.log";
    private File manualStepsFile;

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

        if (wizard.isHadFailures()) printText("There were errors during configuration, see below for details" + getEolChar());
        else {
            printText("The configuration was successfully applied." + getEolChar());
            printText("You must restart the SSG in order for the configuration to take effect." + getEolChar());
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
        String convertedSteps = null;
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
}
