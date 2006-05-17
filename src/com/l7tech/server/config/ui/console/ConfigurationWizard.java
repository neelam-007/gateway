package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.BuildInfo;

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

    protected OSSpecificFunctions osFunctions;

    private List steps = new ArrayList();
    private Set commands;
    private boolean hadFailures;
    private PrintWriter pw;
    private InputStream in;
    String currentVersion;
    private String keystoreType;
    private int clusteringType;
    private String hostname;
    private Set additionalCommands;

    public ConfigurationWizard() {
        init();
    }

    public ConfigurationWizard(InputStream in, PrintWriter out, List stepsList) {
        this.in = in;
        this.pw = out;
        addSteps(stepsList);
        init();
    }

    public PrintWriter getWriter() {
        return pw;
    }

    public InputStream getInputSteam() {
        return in;
    }

    public void startWizard() {
        doWizard();
    }

    public void storeCommand(ConfigurationCommand command) {
        commands.add(command);
    }

    public boolean isHadFailures() {
        return hadFailures;
    }

    private void init() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        JdkLoggerConfigurator.configure("com.l7tech", "configlogging.properties");
        osFunctions = OSDetector.getOSSpecificActions();
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
    }

    private void addSteps(List steps) {
        if (steps != null) {
            for (Iterator iterator = steps.iterator(); iterator.hasNext();) {
                ConfigWizardConsoleStep step = (ConfigWizardConsoleStep) iterator.next();
                addStep(step);
            }
        }
    }

    public void setAdditionalCommands(Set moreCommands) {
        if (moreCommands != null) {
            additionalCommands = moreCommands;
            if (commands == null) commands = new HashSet();
            commands.addAll(additionalCommands);
        }
    }

    private void addStep(ConfigWizardConsoleStep step) {
        steps.add(step);
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
        Iterator iterator = commands.iterator();
        hadFailures = false;

        pw.println("Please wait while the configuration is applied ...");
        pw.flush();

        while (iterator.hasNext()) {
            ConfigurationCommand command = (ConfigurationCommand) iterator.next();
            boolean successful = command.execute();
            if (!successful) {
                hadFailures = true;
            }
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

    public int getClusteringType() {
        return clusteringType;
    }

    public void setClusteringType(int clusteringType) {
        this.clusteringType = clusteringType;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getHostname() {
        return hostname;
    }

    public List getCommandDescription() {
        ArrayList list = new ArrayList();
        Iterator iter = commands.iterator();
        while(iter.hasNext()) {
            ConfigurationCommand command = (ConfigurationCommand) iter.next();
            String[] summary = command.getActionSummary();
            if (summary != null) {
                list.add(summary);
            }
        }
        return list;
    }

    private static ConfigurationWizard getWizard(InputStream is, PrintWriter pw, List stepsList) {
        initLogging();
        return new ConfigurationWizard(is, pw, stepsList);
    }

    private static void initLogging() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        JdkLoggerConfigurator.configure("com.l7tech", "configlogging.properties");
    }

    public void setSteps(List stepsList) {
        addSteps(stepsList);
    }

    public String[] initialize(boolean isSilent, String[] args) {
//        if (isSilent) {
//            return initializeSilentMode(args);
//
//        } else {
            return initializeInteractiveMode();
//        }
    }

    public String[] initializeInteractiveMode() {
        in = System.in;
        pw = new PrintWriter(System.out);

        return null;
    }

//    public String[] initializeSilentMode(String[] args) {
//        checkSilentMode(args);
//
//        String silentFileName = args[SILENT_INDEX];
//        try {
//            in = new FileInputStream(silentFileName);
//            pw = new PrintWriter(System.out);
//        } catch (FileNotFoundException e) {
//            return new String[]{
//                    "Could not find file: " + silentFileName,
//                    "A valid filename must be specified when operating in silent mode"
//            };
//        }
//
//        return null;
//    }

    private static void checkSilentMode(String[] args) {
        if (args.length < 2 || args[FILENAME_INDEX] == null) {
            System.err.println("A filename must be specified when operating in silent mode");
            System.exit(1);
        }
    }
}
