package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.beans.ClusteringConfigBean;
import com.l7tech.server.config.exceptions.WizardNavigationException;

import java.io.*;
import java.util.*;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 10:00:57 AM
 */
public class ConfigWizardConsoleResultsStep extends BaseConsoleStep{
    private static final String MANUAL_STEPS_FILENAME = "ssg_config_manual_steps.txt";

    public ConfigWizardConsoleResultsStep(ConfigurationWizard parentWiz) {
        super(parentWiz);
        showNavigation = false;
    }

    public void doUserInterview(boolean validated) throws WizardNavigationException {
        ConfigurationWizard wizard = getParentWizard();

        if (wizard.isHadFailures()) printText("There were errors during configuration, see below for details" + getEolChar());
        else {
            printText("The configuration was successfully applied." + getEolChar());
            printText("You must restart the SSG in order for the configuration to take effect." + getEolChar());
        }

        if (needsManualSteps(wizard.getClusteringType(), wizard.getKeystoreType())) {
            printText(getEolChar() + "**** Some manual steps are required to complete the configuration of the SSG ****" + getEolChar());
            printText("\tThese manual steps have been saved to a the file: " + MANUAL_STEPS_FILENAME + getEolChar());
            saveManualSteps();
        }

        printText(getEolChar() + "The following is a summary of the actions taken by the wizard" + getEolChar());
        printText("\tThese logs have been saved to the file: ssgconfig0.log" + getEolChar());

        printText(getEolChar());

        List<String> logs = ListHandler.getLogList();
        if (logs != null) {
            for (String log : logs) {
                if (log != null) printText(log + getEolChar());
            }
        }

        printText("Press <Enter> to finish the wizard" + getEolChar());

        try {
            handleInput(readLine());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean needsManualSteps(int clusteringType, String keystoreType) {
        return (clusteringType != ClusteringConfigBean.CLUSTER_NONE || keystoreType.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME));
    }

    private boolean saveManualSteps() {
        boolean success = true;

        StringBuilder stepsBuffer = new StringBuilder();
        StringBuilder allSteps = new StringBuilder();

        Map<String, List<String>> manualSteps = getParentWizard().getManualSteps();

        boolean hasManualSteps = manualSteps != null && !manualSteps.isEmpty();
        if (hasManualSteps) {
            Set<String> keys = manualSteps.keySet();
            for (String key : keys) {
                List<String> steps = manualSteps.get(key);
                for (String step : steps) {
                    allSteps.append(step);
                }
            }

            stepsBuffer.append("The following manual steps are required to complete the configuration of the SSG");
            stepsBuffer.append(getEolChar()).append(getEolChar());
            stepsBuffer.append(allSteps).append(getEolChar());
            stepsBuffer.append(getEolChar());
            stepsBuffer.append(getEolChar());

            PrintWriter saveWriter = null;
            try {
                saveWriter = new PrintWriter(MANUAL_STEPS_FILENAME);
                saveWriter.print(convertStepsForConsole(stepsBuffer.toString()));
            } catch (FileNotFoundException e) {
                printText("Could not create file: " + MANUAL_STEPS_FILENAME + getEolChar());
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

    public String getTitle() {
        return "SSG Configuration Results";
    }

    public boolean validateStep() {
        return true;
    }
}
