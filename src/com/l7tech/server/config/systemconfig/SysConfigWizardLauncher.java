package com.l7tech.server.config.systemconfig;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.ui.console.ConfigWizardConsoleStep;
import com.l7tech.server.config.ui.console.ConfigWizardConsoleSummaryStep;

import java.util.List;
import java.util.ArrayList;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.PrintStream;

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
        stepsList.add(new ConfigWizardConsoleSummaryStep(sysWizard, "Networking Configuration Summary"));
        return stepsList;
    }
}
