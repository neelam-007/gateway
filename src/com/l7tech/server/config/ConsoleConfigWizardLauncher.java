package com.l7tech.server.config;

import com.l7tech.server.config.ui.console.*;

import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: May 9, 2006
 * Time: 4:39:11 PM
 */
public class ConsoleConfigWizardLauncher {
    public static void launch(String[] args) {
        launchWithConsole(args);
    }

    private static void launchWithConsole(String[] args) {
        System.out.println("Starting Configuration Wizard in Console Mode");

        //args will be either empty or -silent -filename
        boolean isSilent = false;
        if (args != null && args.length > 0) {
            if ("-silent".equals(args[ConfigurationWizard.SILENT_INDEX])) {
                isSilent = true;
            }
        }
        else {
            isSilent = false;
        }


        OSSpecificFunctions osFunctions = OSDetector.getOSSpecificActions();
        ConfigurationWizard consoleWizard;

        consoleWizard = new ConfigurationWizard();
        String[] errorMessages = consoleWizard.initialize(isSilent, args);
        if (errorMessages != null) {
            for (int i = 0; i < errorMessages.length; i++) {
                String errorMessage = errorMessages[i];
                System.out.println(errorMessage);
            }
            System.exit(1);
        }

        List stepsList = new ArrayList();

        stepsList.add(new ConfigWizardConsoleStartStep(consoleWizard, osFunctions));
        stepsList.add(new ConfigWizardConsoleClusteringStep(consoleWizard, osFunctions));
        stepsList.add(new ConfigWizardConsoleDatabaseStep(consoleWizard, osFunctions));
        stepsList.add(new ConfigWizardConsoleKeystoreStep(consoleWizard, osFunctions));
        stepsList.add(new ConfigWizardConsoleSummaryStep(consoleWizard, osFunctions));
        stepsList.add(new ConfigWizardConsoleResultsStep(consoleWizard, osFunctions));

        consoleWizard.setSteps(stepsList);

        consoleWizard.startWizard();
    }
}
