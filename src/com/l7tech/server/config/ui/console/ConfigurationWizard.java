package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.commands.LoggingConfigCommand;
import com.l7tech.server.config.commands.RmiConfigCommand;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.BuildInfo;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.*;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:42:52 PM
 */
public class ConfigurationWizard {
    public static final int SILENT_INDEX = 0;
    public static final int FILENAME_INDEX = 1;

    public static final int MOVING_PREV = 2;

//    private static final String PREV_COMMAND = "B";
//    private static final String GENERAL_NAV_HEADER = "At any time press " + PREV_COMMAND + " to go to the previous step";

    protected OSSpecificFunctions osFunctions;

    private List steps = new ArrayList();
    private ArrayList commands = new ArrayList();
    private boolean hadFailures;
    private PrintWriter pw;
    private InputStream in;
    String currentVersion;
    private String keystoreType;

    public ConfigurationWizard(InputStream in, PrintWriter out) {
        osFunctions = OSDetector.getOSSpecificActions();
        this.in = in;
        this.pw = out;

        init();
    }

    public PrintWriter getWriter() {
        return pw;
    }

    public InputStream getInputSteam() {
        return in;
    }

    public static void startWizard(String[] args) {
        ConfigurationWizard consoleWizard = null;

        //args will be either empty or -silent -filename
        if (args != null && args.length > 0) {
            if ("-silent".equals(args[SILENT_INDEX])) {
                consoleWizard = initializeSilentMode(args);
                consoleWizard.doWizard();
            }
        }
        else {
            consoleWizard = initializeInteractiveMode();
            consoleWizard.doWizard();
        }
    }

    private static ConfigurationWizard initializeInteractiveMode() {
        return ConfigurationWizard.getWizard(System.in, new PrintWriter(System.out));
    }

    private static ConfigurationWizard initializeSilentMode(String[] args) {

        checkSilentMode(args);

        InputStream is = null;
        PrintWriter pw = null;

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

        return ConfigurationWizard.getWizard(is, pw);
    }

    private static void checkSilentMode(String[] args) {
        if (args.length < 2 || args[FILENAME_INDEX] == null) {
            System.err.println("A filename must be specified when operating in silent mode");
            printUsage();
            System.exit(1);
        }
    }

    public void storeCommand(ConfigurationCommand command) {
        commands.add(command);
    }

    public boolean isHadFailures() {
        return hadFailures;
    }

    private void init() {
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();

        addStep(new ConfigWizardConsoleStartStep(this, osFunctions));
        addStep(new ConfigWizardConsoleClusteringStep(this, osFunctions));
        addStep(new ConfigWizardConsoleDatabaseStep(this, osFunctions));
        addStep(new ConfigWizardConsoleKeystoreStep(this, osFunctions));
        addStep(new ConfigWizardConsoleSummaryStep(this, osFunctions));
        addStep(new ConfigWizardConsoleResultsStep(this, osFunctions));
    }

    private void addStep(ConfigWizardConsoleStep step) {
        steps.add(step);
    }

    private static ConfigurationWizard getWizard(InputStream is, PrintWriter pw) {
        initLogging();
        return new ConfigurationWizard(is, pw);
    }

    private static void printUsage() {
        System.out.println("Usage:");
    }

    private static void initLogging() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        JdkLoggerConfigurator.configure("com.l7tech", "configlogging.properties");
    }

    private void doWizard() {
        ListIterator stepsIterator = steps.listIterator();
        ConfigWizardConsoleStep step;
        while (stepsIterator.hasNext()) {
            step = (ConfigWizardConsoleStep) stepsIterator.next();
            step.showTitle();
            if (step.isShowNavigation()) {
                pw.println(ConsoleWizardUtils.GENERAL_NAV_HEADER);
                pw.println();
            }
            try {
                step.showStep(true);
                if (step.shouldApplyConfiguration()) applyConfiguration();
            } catch (WizardNavigationException e) {
                if (e.getMessage().equals(WizardNavigationException.NAVIGATE_NEXT)) {
                } else if (e.getMessage().equals(WizardNavigationException.NAVIGATE_PREV)) {
                    //since the iterator has already advanced with next(), we need to make two calls to previous().
                    stepsIterator.previous();
                    step = (ConfigWizardConsoleStep) stepsIterator.previous();
                } else {}
            }
        }

        if (hadFailures) System.exit(1);

        System.exit(0);
    }

    private void applyConfiguration() {
        storeCommand(new LoggingConfigCommand(null, osFunctions));
        storeCommand(new RmiConfigCommand(null, osFunctions));

        Iterator iterator = commands.iterator();
        hadFailures = false;
        while (iterator.hasNext()) {
            ConfigurationCommand command = (ConfigurationCommand) iterator.next();
            String[] actions = command.getActionSummary();
            if (actions != null) {
                for (int i = 0; i < actions.length; i++) {
                    String s = actions[i];
                    pw.println(s);
                }
                pw.flush();
            }

//            boolean successful = command.execute();
//            if (!successful) {
//                hadFailures = true;
//            }
        }
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public void setKeystoreType(String ksType) {
        this.keystoreType = ksType;
    }

    public String getKeystoreType() {
        return keystoreType;
    }
}
