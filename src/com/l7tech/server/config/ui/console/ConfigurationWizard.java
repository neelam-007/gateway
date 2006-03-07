package com.l7tech.server.config.ui.console;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.LinkedList;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:42:52 PM
 */
public class ConfigurationWizard {
    public static final int SILENT_INDEX = 0;
    public static final int FILENAME_INDEX = 1;

//    ConfigWizardConsoleStep firstStep = null;
//    ConfigWizardConsoleStep currentStep = null;

    LinkedList steps = new LinkedList();

    boolean isSilent = false;

    public ConfigurationWizard(InputStream in, PrintWriter out) {
        addWizardStep(new ConfigWizardConsoleStartStep(this, in, out));
        addWizardStep(new ConfigWizardConsoleClusteringStep(this, in, out));
        addWizardStep(new ConfigWizardConsoleDatabaseStep(this, in, out));
        addWizardStep(new ConfigWizardConsoleKeystoreStep(this, in, out));
        addWizardStep(new ConfigWizardConsoleSummaryStep(this, in, out));
        addWizardStep(new ConfigWizardConsoleResultsStep(this, in, out));
    }

    private void addWizardStep(ConfigWizardConsoleStep wizardStep) {
        steps.add(wizardStep);
//        //if this is the first step to be added, mark it as such.
//        if (firstStep == null) {
//            firstStep = wizardStep;
//        }
//
//        if (currentStep != null) {
//            currentStep.setNextStep(wizardStep);
//        }
//
//        currentStep = wizardStep;
//
//        wizardStep.setPreviousStep(currentStep);
//        wizardStep.setNextStep(null);
    }

    public static void startWizard(String[] args) {
        ConfigurationWizard consoleWizard;
        InputStream is = null;
        PrintWriter pw = null;

        //args will be [-console | -graphical], -silent, -filename
        if (args != null && args.length > 0) {
            if ("-silent".equals(args[SILENT_INDEX])) {
                if (args.length < 2 || args[FILENAME_INDEX] == null) {
                    System.out.println("A filename must be specified when operating in silent mode");
                    printUsage();
                    System.exit(1);
                }

                String silentFileName = args[SILENT_INDEX];
                try {
                    is = new FileInputStream(silentFileName);
                    pw = new PrintWriter(System.out);
                } catch (FileNotFoundException e) {
                    System.out.println("Could not find file: " + silentFileName);
                    System.out.println("A valid filename must be specified when operating in silent mode");
                    printUsage();
                    System.exit(1);
                }
            }
        }
        else {
            is = System.in;
            pw = new PrintWriter(System.out);
        }
        consoleWizard = new ConfigurationWizard(is, pw);
        consoleWizard.start();
    }

    private static void printUsage() {
        System.out.println("Usage:");
    }

    private void start() {
        ConfigWizardConsoleStep step = (ConfigWizardConsoleStep) steps.getFirst();
        step.collectInput();
    }

    public void next(BaseConsoleStep baseConsoleStep) {
        System.out.println("moving to the next panel");
    }

    public void previous(BaseConsoleStep baseConsoleStep) {
    }
}
