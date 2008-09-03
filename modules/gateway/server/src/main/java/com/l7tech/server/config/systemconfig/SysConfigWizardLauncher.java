package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.wizard.ConfigWizardConsoleStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleSummaryStep;
import com.l7tech.server.config.wizard.ConsoleWizardUtils;
import com.l7tech.server.config.wizard.ConfigWizardConsoleResultsStep;

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
    private static final String ARG_PRINT_CONFIG = "-printConfig";

    public static void main(String[] args) {
        launch(args);
    }

    private static void launch(String[] args) {
        InputStream wizardInput = System.in;
        PrintStream wizardOutput = System.out;

        SystemConfigurationWizard sysWizard = new SystemConfigurationWizard(wizardInput, wizardOutput);

        if (args.length != 0) {
            String launchType = args[0];
            if (launchType.equalsIgnoreCase(ARG_PRINT_CONFIG)) {
                sysWizard.printConfigOnly();
            }
        } else {
            sysWizard.setSteps(getSteps(sysWizard));
            sysWizard.startWizard();
        }
    }

    private static List<ConfigWizardConsoleStep> getSteps(SystemConfigurationWizard sysWizard) {
        List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();
        stepsList.add(new SystemConfigWizardNetworkingStep(sysWizard));
        stepsList.add(new SystemConfigWizardNtpStep(sysWizard));
        stepsList.add(new ConfigWizardConsoleSummaryStep(sysWizard, "System Configuration Summary"));

        ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(sysWizard, "Networking Configuration Results");
        resultsStep.setSuccessMessage("The configuration was successfully applied." + ConsoleWizardUtils.EOL_CHAR + "You must restart the SSG Appliance in order for the configuration to take effect." + ConsoleWizardUtils.EOL_CHAR);
        resultsStep.setManualStepsFileName("ssg_networkingconfig_manualsteps.txt");
        resultsStep.setLogFilename("ssg_networkingconfig_log.txt");
        stepsList.add(resultsStep);
        return stepsList;
    }
}
