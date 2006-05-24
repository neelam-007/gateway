package com.l7tech.server.config;

import com.l7tech.server.config.ui.console.*;
import com.l7tech.server.config.commands.AppServerConfigCommand;
import com.l7tech.server.config.commands.LoggingConfigCommand;
import com.l7tech.server.config.commands.RmiConfigCommand;
import com.l7tech.server.config.commands.ConfigurationCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.io.*;

/**
 * User: megery
 * Date: May 9, 2006
 * Time: 4:39:11 PM
 */
public class ConsoleConfigWizardLauncher {
    private static final int SILENT_INDEX = 0;
    private static final String SILENT_FILENAME_MSG = "A valid filename must be specified when operating in silent mode";

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


        ConfigurationWizard consoleWizard;
        InputStream wizardInput = null;
        PrintStream wizardOutput = null;

        if (!isSilent) {
            wizardInput = System.in;
            wizardOutput = System.out;
        } else {
            String silentFileName = args[SILENT_INDEX];
            try {
                wizardInput = new FileInputStream(silentFileName);
                /**
                 * TODO: make this a PrintWriter around a file
                 */
                wizardOutput = System.out;
            } catch (FileNotFoundException e) {
                System.out.println("Could not find file: " + silentFileName);
                System.out.println(SILENT_FILENAME_MSG);
                System.exit(1);
            }
        }

        consoleWizard = new ConfigurationWizard(wizardInput, wizardOutput);
        consoleWizard.setSteps(getSteps(consoleWizard));
        consoleWizard.addAdditionalCommands(getAdditionalCommands());

        consoleWizard.startWizard();
    }

    private static Set<ConfigurationCommand> getAdditionalCommands() {
        Set<ConfigurationCommand> additionalCommands = new HashSet<ConfigurationCommand>();

        //make sure that the server.xml gets appropriately upgraded to include the new ConnectionId Management stuff
        additionalCommands.add(new AppServerConfigCommand());
        additionalCommands.add(new LoggingConfigCommand(null));
        additionalCommands.add(new RmiConfigCommand(null));
        return additionalCommands;
    }

    private static List<ConfigWizardConsoleStep> getSteps(ConfigurationWizard consoleWizard) {
        List<ConfigWizardConsoleStep> stepsList = new ArrayList<ConfigWizardConsoleStep>();

        stepsList.add(new ConfigWizardConsoleStartStep(consoleWizard));
        stepsList.add(new ConfigWizardConsoleClusteringStep(consoleWizard));
        stepsList.add(new ConfigWizardConsoleDatabaseStep(consoleWizard));
        stepsList.add(new ConfigWizardConsoleKeystoreStep(consoleWizard));
        stepsList.add(new ConfigWizardConsoleSummaryStep(consoleWizard));
        stepsList.add(new ConfigWizardConsoleResultsStep(consoleWizard));
        return stepsList;
    }
}
