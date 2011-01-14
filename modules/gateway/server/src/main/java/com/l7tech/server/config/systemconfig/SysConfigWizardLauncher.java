package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.beans.BaseConfigurationBean;
import com.l7tech.server.config.wizard.ConfigWizardConsoleStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleSummaryStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleResultsStep;
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
        SystemConfigurationWizard sysWizard = new SystemConfigurationWizard();

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
        stepsList.add(new SystemConfigWizardKeyboardStep(sysWizard));
        stepsList.add(new SystemConfigWizardNetworkingStep(sysWizard));
        stepsList.add(new SystemConfigWizardNtpStep(sysWizard));
        stepsList.add(new ConfigWizardConsoleSummaryStep(sysWizard, "System Configuration Summary"));

        ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(sysWizard, "Networking Configuration Results");
        resultsStep.setSuccessMessage("The configuration was successfully applied." + BaseConfigurationBean.EOL + "You must restart the SSG Appliance in order for the configuration to take effect." + BaseConfigurationBean.EOL);
        stepsList.add(resultsStep);
        return stepsList;
    }
}
