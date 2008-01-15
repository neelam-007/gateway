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
import java.util.logging.Logger;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:42:52 PM
 */
public class ConfigurationWizard {
    private static final Logger logger = Logger.getLogger(ConfigurationWizard.class.getName());

    public static final int SILENT_INDEX = 0;
    public static final int FILENAME_INDEX = 1;

    public static final int MOVING_PREV = 2;

    protected SilentConfigData silentConfigData;
    protected String configDataPassphrase;

    protected OSSpecificFunctions osFunctions;
    protected List<ConfigWizardConsoleStep> steps = new ArrayList<ConfigWizardConsoleStep>();
    protected Set<ConfigurationCommand> additionalCommands;
    protected Collection<ConfigurationCommand> commands;
    protected boolean hadFailures;
    protected SharedWizardInfo sharedWizardInfo;

    static String currentVersion = null;
    public static final String COMMONS_LOGGING_PROP = "org.apache.commons.logging.Log";
    public static final String COMMONS_LOGGING_JDK14_LOGGER = "org.apache.commons.logging.impl.Jdk14Logger";
    public static final String L7TECH_CLASSNAME = "com.l7tech";

    public static final String LOGCONFIG_NAME = "configlogging.properties";

    protected ConsoleWizardUtils wizardUtils = null;
    private ManualStepsManager manualStepsManager;

    boolean jumpToApply = false;

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
            if (isJumpToApply()) {
                if (!step.shouldApplyConfiguration()) continue;
            }
            step.showTitle();


            if (step.isShowQuitMessage()) wizardUtils.printText(ConsoleWizardUtils.QUIT_HEADER + ConsoleWizardUtils.EOL_CHAR);
            if (step.isShowNavigation()) wizardUtils.printText(ConsoleWizardUtils.NAV_HEADER + ConsoleWizardUtils.EOL_CHAR);


            wizardUtils.printText(ConsoleWizardUtils.EOL_CHAR);

            try {
                step.showStep(true);
                if (step.shouldApplyConfiguration()) applyConfiguration();
            } catch (WizardNavigationException e) {
                String message = e.getMessage();
                if (WizardNavigationException.NAVIGATE_NEXT.equals(message)) {
                } else if (WizardNavigationException.NAVIGATE_PREV.equals(message)) {
                    //since the iterator has already advanced with next(), we need to make two calls to previous().
                    stepsIterator.previous();
                    step = stepsIterator.previous();
                } else {}
            }
        }

        if (hadFailures) System.exit(1);
        else {
            logger.info("Configuration Completed.");
            System.exit(0);
        }
    }

    protected void applyConfiguration() {
        hadFailures = false;

        if (additionalCommands != null)
            commands.addAll(additionalCommands);

        Iterator<ConfigurationCommand> iterator = commands.iterator();


        wizardUtils.printText("Please wait while the configuration is applied ..." + ConsoleWizardUtils.EOL_CHAR);

        while (iterator.hasNext()) {
            ConfigurationCommand command = iterator.next();
            boolean successful= command.execute();
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

    public void setRealHostname(String realHostname) {
        sharedWizardInfo.setRealHostname(realHostname);
    }

    public String getHostname() {
        return sharedWizardInfo.getHostname();
    }

    public String getRealHostname() {
        return sharedWizardInfo.getRealHostname();
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

    public boolean isJumpToApply() {
        return jumpToApply;
    }

    public void setJumpToApply(boolean jumpToApply) {
        this.jumpToApply = jumpToApply;
    }

    public void setSilentConfigData(SilentConfigData configData) {
        this.silentConfigData = configData;
    }

    public SilentConfigData getSilentConfigData() {
        return silentConfigData;
    }

    public void setConfigDataPassword(String passphrase) {
        this.configDataPassphrase = passphrase;
    }

    public String getConfigDataPassphrase() {
        return configDataPassphrase;
    }

    public boolean shouldSaveConfigData() {
        return  SharedWizardInfo.getInstance().getConfigType() == ConfigurationType.CONFIG_CLUSTER &&
                SharedWizardInfo.getInstance().getClusterType() == ClusteringType.CLUSTER_MASTER;
    }
}
