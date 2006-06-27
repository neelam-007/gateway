package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.ui.console.ConfigWizardConsoleResultsStep;
import com.l7tech.server.config.ui.console.ConfigWizardConsoleStep;
import com.l7tech.server.config.ui.console.ConfigWizardConsoleSummaryStep;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: May 16, 2006
 * Time: 11:04:51 AM
 */
public class SysConfigWizardLauncher {

    public static void main(String[] args) {
        launch(args);
    }

    private static void launch(String[] args) {
        InputStream wizardInput = System.in;
        PrintStream wizardOutput = System.out;

        SystemConfigurationWizard sysWizard = new SystemConfigurationWizard(wizardInput, wizardOutput);

        sysWizard.setSteps(getSteps(sysWizard));

        sysWizard.startWizard();
    }

    private static List<ConfigWizardConsoleStep> getSteps(SystemConfigurationWizard sysWizard) {
        List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();
        stepsList.add(new SystemConfigWizardNetworkingStep(sysWizard));
        stepsList.add(new SystemConfigWizardNtpStep(sysWizard));
        stepsList.add(new ConfigWizardConsoleSummaryStep(sysWizard, "Networking Configuration Summary"));

        ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(sysWizard, "Networking Configuration Results");
        resultsStep.setManualStepsFileName("ssg_networkingconfig_manualsteps.txt");
        resultsStep.setLogFilename("ssg_networkingconfig_log.txt");
        stepsList.add(resultsStep);
        return stepsList;
    }
}
