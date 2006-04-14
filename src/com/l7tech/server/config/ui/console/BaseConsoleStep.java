package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.PasswordValidator;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * User: megery
 * Date: Feb 21 19, 2006
 * Time: 4:50:05 PM
 */
public abstract class BaseConsoleStep implements ConfigWizardConsoleStep {

    private ConsoleWizardUtils consoleWizardUtils;
    ConfigurationWizard parent;

    protected ConfigurationBean configBean = null;

    protected OSSpecificFunctions osFunctions = null;
    protected BufferedReader reader;
    protected ConfigurationCommand configCommand;

    protected boolean readyToAdvance;

    protected boolean showNavigation = true;

    public BaseConsoleStep(ConfigurationWizard parent_, OSSpecificFunctions osFunctions) {
        this.parent = parent_;
        this.osFunctions = osFunctions;
        reader = new BufferedReader(new InputStreamReader(parent.getInputSteam()));
        consoleWizardUtils = ConsoleWizardUtils.getInstance(parent.getInputSteam(), parent.getWriter());
        readyToAdvance = false;
    }

    public void showStep(boolean validated) throws WizardNavigationException {
        doUserInterview(validated);
        if (!validateStep()) showStep(false);
    }

    public ConfigurationCommand getConfigurationCommand() {
        return configCommand;
    }

    public boolean shouldApplyConfiguration() {
        return false;
    }

    public void setReadyToAdvance(boolean readyToAdvance) {
        this.readyToAdvance = readyToAdvance;
    }

    public boolean isShowNavigation() {
        return showNavigation;
    }

    protected ConfigurationWizard getParent() {
        return parent;
    }

    public void showTitle() {
        String title = getTitle();
        String banner = StringUtils.repeat("-", title.length());

        consoleWizardUtils.printText(new String[] {
                "\n",
                banner + "\n",
                title + "\n",
                banner + "\n",
        });
    }

    protected String getMatchingPasswords(String prompt1, String prompt2, PasswordValidator validator) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getMatchingDataWithConfirm(prompt1, prompt2, -1, validator);
    }

    protected void handleInput(String input) throws WizardNavigationException {
        consoleWizardUtils.handleInput(input);
    }

    protected String getData(String[] promptLines, String defaultValue, boolean isNavAware) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getData(promptLines, defaultValue, isNavAware);
    }

    protected void printText(String[] textToPrint) {
        consoleWizardUtils.printText(textToPrint);
    }

    protected void printText(String textToPrint) {
        consoleWizardUtils.printText(textToPrint);
    }

    protected void storeInput() {
        if (configCommand != null) {
            getParent().storeCommand(configCommand);
        }
    }

    abstract boolean validateStep();

    abstract void doUserInterview(boolean validated) throws WizardNavigationException;

    abstract String getTitle();
}