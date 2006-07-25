package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.BuildInfo;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.*;

import org.apache.commons.lang.StringUtils;

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

    private List<ConfigWizardConsoleStep> steps = new ArrayList<ConfigWizardConsoleStep>();
    private Set<ConfigurationCommand> commands;
    private boolean hadFailures;
    String currentVersion = null;
    private String keystoreType;
    private int clusteringType;
    private String hostname;
    private static final String COMMONS_LOGGING_PROP = "org.apache.commons.logging.Log";
    private static final String COMMONS_LOGGING_JDK14_LOGGER = "org.apache.commons.logging.impl.Jdk14Logger";
    private static final String L7TECH_CLASSNAME = "com.l7tech";
    private static final String LOGCONFIG_NAME = "configlogging.properties";

    ConsoleWizardUtils wizardUtils = null;
    private Map<String, List<String>> manualSteps;

    public ConfigurationWizard(InputStream in, PrintStream out) {
        init(in, out);
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

    private void init(InputStream in, PrintStream out) {
        System.setProperty(COMMONS_LOGGING_PROP, COMMONS_LOGGING_JDK14_LOGGER);
        JdkLoggerConfigurator.configure(L7TECH_CLASSNAME, LOGCONFIG_NAME);
        osFunctions = OSDetector.getOSSpecificFunctions();
        wizardUtils = ConsoleWizardUtils.getInstance(in, out);
        commands = new HashSet<ConfigurationCommand>();
        manualSteps = new HashMap<String, List<String>>();
    }

    private void addSteps(List<ConfigWizardConsoleStep> steps) {
        if (steps != null) {
            for (ConfigWizardConsoleStep step : steps) {
                addStep(step);
            }
        }
    }

    public void addAdditionalCommands(Set<ConfigurationCommand> moreCommands) {
        if (moreCommands != null) {
            if (commands == null) commands = new HashSet<ConfigurationCommand>();
            commands.addAll(moreCommands);
        }
    }

    private void addStep(ConfigWizardConsoleStep step) {
        steps.add(step);
    }

    private void doWizard() {
        ListIterator<ConfigWizardConsoleStep> stepsIterator = steps.listIterator();
        ConfigWizardConsoleStep step;
        while (stepsIterator.hasNext()) {
            step = stepsIterator.next();
            step.showTitle();


            if (step.isShowQuitMessage()) wizardUtils.printText(ConsoleWizardUtils.QUIT_HEADER + ConsoleWizardUtils.EOL_CHAR);
            if (step.isShowNavigation()) wizardUtils.printText(ConsoleWizardUtils.NAV_HEADER + ConsoleWizardUtils.EOL_CHAR);


            wizardUtils.printText(ConsoleWizardUtils.EOL_CHAR);

            try {
                step.showStep(true);
                if (step.shouldApplyConfiguration()) applyConfiguration();
            } catch (WizardNavigationException e) {
                if (e.getMessage().equals(WizardNavigationException.NAVIGATE_NEXT)) {
                } else if (e.getMessage().equals(WizardNavigationException.NAVIGATE_PREV)) {
                    //since the iterator has already advanced with next(), we need to make two calls to previous().
                    stepsIterator.previous();
                    step = stepsIterator.previous();
                } else {}
            }
        }

        if (hadFailures) System.exit(1);

        System.exit(0);
    }

    private void applyConfiguration() {
        Iterator<ConfigurationCommand> iterator = commands.iterator();
        hadFailures = false;

        wizardUtils.printText("Please wait while the configuration is applied ..." + ConsoleWizardUtils.EOL_CHAR);

        while (iterator.hasNext()) {
            ConfigurationCommand command = iterator.next();
            boolean successful = command.execute();
            if (!successful) {
                hadFailures = true;
            }
        }
    }

    public String getCurrentVersion() {
        if (currentVersion == null)
            currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
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

    public List<String[]> getCommandDescriptions() {
        ArrayList<String[]> list = new ArrayList<String[]>();
        for (ConfigurationCommand command : commands) {
            String[] actions = command.getActions();
            if (actions != null) {
                list.add(actions);
            }
        }
        return list;
    }

    public void setSteps(List<ConfigWizardConsoleStep> stepsList) {
        addSteps(stepsList);
    }


//    private static void checkSilentMode(String[] args) {
//        if (args.length < 2 || args[FILENAME_INDEX] == null) {
//            System.err.println("A filename must be specified when operating in silent mode");
//            System.exit(1);
//        }
//    }

    public ConsoleWizardUtils getWizardUtils() {
        return wizardUtils;
    }

    public void addManualSteps(String stepKey, java.util.List<String> steps) {
        if (StringUtils.isEmpty(stepKey)) throw new IllegalArgumentException("key cannot be empty or null");
        if (steps == null) throw new IllegalArgumentException("steps cannot be null");

        manualSteps.put(stepKey, steps);
    }

    public Map<String, List<String>> getManualSteps() {
        return manualSteps;
    }
}
