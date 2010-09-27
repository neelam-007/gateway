package com.l7tech.server.config.packageupdater;

import com.l7tech.server.config.wizard.ConfigWizardConsoleResultsStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleStep;
import com.l7tech.server.config.wizard.ConfigWizardConsoleSummaryStep;
import com.l7tech.server.config.wizard.ConfigurationWizard;
import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: Mar 20, 2007
 * Time: 2:10:28 PM
 */
public class PackageUpdateWizardLauncher {
    public static void main(String[] args) {
        launch();
    }

    private static void launch() {
        ConfigurationWizard packageWizard = new ConfigurationWizard();

        packageWizard.setSteps(getSteps(packageWizard));

        packageWizard.startWizard();
    }

    private static List<ConfigWizardConsoleStep> getSteps(ConfigurationWizard sysWizard) {
        List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();
        stepsList.add(new UpdateWizardIntroStep(sysWizard));
        stepsList.add(new UpdateWizardPackageQuestionsStep(sysWizard));
        stepsList.add(new ConfigWizardConsoleSummaryStep(sysWizard, "Package Update Installation Summary"));

        ConfigWizardConsoleResultsStep resultsStep = new ConfigWizardConsoleResultsStep(sysWizard, "Package Update Installation Results");
        stepsList.add(resultsStep);
        return stepsList;
    }
}
