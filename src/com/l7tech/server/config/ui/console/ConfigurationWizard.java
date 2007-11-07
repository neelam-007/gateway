package com.l7tech.server.config.ui.console;

import com.l7tech.common.BuildInfo;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.server.config.*;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.db.DBInformation;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import java.io.InputStream;
import java.io.PrintStream;
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

    private List<ConfigWizardConsoleStep> steps = new ArrayList<ConfigWizardConsoleStep>();
    private Set<ConfigurationCommand> commands;
    private Set<ConfigurationCommand> additionalCommands;
    private boolean hadFailures;
    static String currentVersion = null;

    private static final String COMMONS_LOGGING_PROP = "org.apache.commons.logging.Log";
    private static final String COMMONS_LOGGING_JDK14_LOGGER = "org.apache.commons.logging.impl.Jdk14Logger";
    private static final String L7TECH_CLASSNAME = "com.l7tech";
    private static final String LOGCONFIG_NAME = "configlogging.properties";

    ConsoleWizardUtils wizardUtils = null;

    private ManualStepsManager manualStepsManager;
    private SharedWizardInfo sharedWizardInfo;

    static {
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
        String subMinor = BuildInfo.getProductVersionSubMinor();
        if (subMinor != null && !subMinor.equals("")) currentVersion += "." + subMinor;
    }

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
        initLogging();
        osFunctions = OSDetector.getOSSpecificFunctions();
        wizardUtils = ConsoleWizardUtils.getInstance(in, out);
        commands = new LinkedHashSet<ConfigurationCommand>();
        manualStepsManager = new ManualStepsManager();
        sharedWizardInfo = SharedWizardInfo.getInstance();
    }

    private void initLogging() {
        System.setProperty(COMMONS_LOGGING_PROP, COMMONS_LOGGING_JDK14_LOGGER);
        JdkLoggerConfigurator.configure(L7TECH_CLASSNAME, LOGCONFIG_NAME);
    }

    private void addSteps(List<ConfigWizardConsoleStep> steps) {
        if (steps != null) {
            for (ConfigWizardConsoleStep step : steps) {
                addStep(step);
            }
        }
    }

    public void setAdditionalCommands(Set<ConfigurationCommand> moreCommands) {
        additionalCommands = moreCommands;
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
        if (additionalCommands != null)
            commands.addAll(additionalCommands);

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

    public static String getCurrentVersion() {
        return currentVersion;
    }

    public void setKeystoreType(KeystoreType ksType) {
        sharedWizardInfo.setKeystoreType(ksType);
    }

    public void setClusteringType(ClusteringType clusteringType) {
        sharedWizardInfo.setClusterType(clusteringType);
    }

    public void setHostname(String hostname) {
        sharedWizardInfo.setHostname(hostname);
    }

    public String getHostname() {
        return sharedWizardInfo.getHostname();
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

    public ConsoleWizardUtils getWizardUtils() {
        return wizardUtils;
    }

    public List<String> getManualSteps() {
        return manualStepsManager.getManualSteps();
    }

    public OSSpecificFunctions getOsFunctions() {
        PartitionInformation pi = PartitionManager.getInstance().getActivePartition();
        return (pi == null?OSDetector.getOSSpecificFunctions(""):pi.getOSSpecificFunctions());
    }

    public void setOsFunctions(OSSpecificFunctions osFunctions) {
        this.osFunctions = osFunctions;
    }

    public void setPartitionName(PartitionInformation partition) {
        PartitionManager.getInstance().setActivePartition(partition);
    }

    public void setDbInfo(DBInformation dbInfo) {
        sharedWizardInfo.setDbinfo(dbInfo);
    }
}
