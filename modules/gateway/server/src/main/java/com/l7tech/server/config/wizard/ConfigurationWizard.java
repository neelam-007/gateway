package com.l7tech.server.config.wizard;

import com.l7tech.util.JdkLoggerConfigurator;
import com.l7tech.server.config.*;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import java.util.*;
import java.util.logging.Logger;
import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

/**
 * User: megery
 * Date: Feb 22, 2006
 * Time: 3:42:52 PM
 */
public class ConfigurationWizard {

    // - PUBLIC

    public static final String COMMONS_LOGGING_PROP = "org.apache.commons.logging.Log";
    public static final String COMMONS_LOGGING_JDK14_LOGGER = "org.apache.commons.logging.impl.Jdk14Logger";
    public static final String L7TECH_CLASSNAME = "com.l7tech";
    public static final String LOGCONFIG_NAME = "configlogging.properties";

    public ConfigurationWizard() {
        initLogging();
        osFunctions = OSSpecificFunctions.getOSSpecificFunctions();
        commands = new LinkedHashSet<ConfigurationCommand>();
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

    // - PROTECTED

    protected OSSpecificFunctions osFunctions;
    protected List<ConfigWizardConsoleStep> steps = new ArrayList<ConfigWizardConsoleStep>();
    protected Set<ConfigurationCommand> additionalCommands;
    protected Collection<ConfigurationCommand> commands;
    protected boolean hadFailures;

    protected void applyConfiguration() {
        hadFailures = false;

        if (additionalCommands != null)
            commands.addAll(additionalCommands);

        Iterator<ConfigurationCommand> iterator = commands.iterator();
        ConsoleWizardUtils.printText("Please wait while the configuration is applied ..." + EOL);

        while (iterator.hasNext()) {
            ConfigurationCommand command = iterator.next();
            boolean successful= command.execute();
            if (!successful) {
                hadFailures = true;
            }
        }
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(ConfigurationWizard.class.getName());

    private void initLogging() {
        System.setProperty(COMMONS_LOGGING_PROP, COMMONS_LOGGING_JDK14_LOGGER);
        JdkLoggerConfigurator.configure(L7TECH_CLASSNAME, "com/l7tech/server/config/resources/logging.properties", LOGCONFIG_NAME, false, true);
    }

    private void addSteps(List<ConfigWizardConsoleStep> steps) {
        if (steps != null) {
            for (ConfigWizardConsoleStep step : steps) {
                addStep(step);
            }
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

            if (step.isShowQuitMessage())
                ConsoleWizardUtils.printText(ConsoleWizardUtils.QUIT_HEADER + EOL);

            if (step.isShowNavigation())
                ConsoleWizardUtils.printText(ConsoleWizardUtils.NAV_HEADER + EOL);

            ConsoleWizardUtils.printText(EOL);

            try {
                step.showStep(true);
                if (step.shouldApplyConfiguration()) applyConfiguration();
            } catch (WizardNavigationException e) {
                String message = e.getMessage();
                if (WizardNavigationException.NAVIGATE_NEXT.equals(message)) {
                } else if (WizardNavigationException.NAVIGATE_PREV.equals(message)) {
                    //since the iterator has already advanced with next(), we need to make two calls to previous().
                    stepsIterator.previous();
                    stepsIterator.previous();
                } else {}
            }
        }

        if (hadFailures) System.exit(1);
        else {
            logger.info("Configuration Completed.");
            System.exit(0);
        }
    }
}
