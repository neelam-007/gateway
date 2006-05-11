package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.WizardInputValidator;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * User: megery
 * Date: Feb 21 19, 2006
 * Time: 4:50:05 PM
 */
public abstract class BaseConsoleStep implements ConfigWizardConsoleStep {

    protected ConsoleWizardUtils consoleWizardUtils;
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

    protected ConfigurationWizard getParentWizard() {
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

    protected String getMatchingPasswords(final String firstPrompt, final String secondPrompt, final int minPasswordLength) throws IOException, WizardNavigationException {
        Map passwords = consoleWizardUtils.getValidatedDataWithConfirm(
            new String[]{firstPrompt, secondPrompt},
            null,
            -1,
            new WizardInputValidator() {
                public String[] validate(Map inputs) {
                    int passwordLength = minPasswordLength;
                    if (minPasswordLength < 0) {
                        passwordLength = 0;
                    }
                    List errorMessages = new ArrayList();

                    String password1 = (String) inputs.get(firstPrompt);
                    String password2 = (String) inputs.get(secondPrompt);

                    if (StringUtils.isEmpty(password1)) {
                        errorMessages.add("**** The password cannot be empty ****\n");
                    } else if (password1.length() < passwordLength) {
                        errorMessages.add("**** The password must be at least " + passwordLength +" characters long. Please try again ****\n");
                    } else if (!StringUtils.equals(password1, password2)) {
                        errorMessages.add("**** The passwords do not match ****\n");
                    }

                    if (errorMessages.size() > 0) {
                        return (String[]) errorMessages.toArray(new String[errorMessages.size()]);
                    }
                    return null;
                }
            });

        return (String) passwords.get(firstPrompt);
    }

    protected void handleInput(String input) throws WizardNavigationException {
        consoleWizardUtils.handleInput(input, isShowNavigation());
    }

    protected String getData(String[] promptLines, String defaultValue) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation());
    }

    protected void printText(String[] textToPrint) {
        consoleWizardUtils.printText(textToPrint);
    }

    protected void printText(String textToPrint) {
        consoleWizardUtils.printText(textToPrint);
    }

    protected void storeInput() {
        if (configCommand != null) {
            getParentWizard().storeCommand(configCommand);
        }
    }

    abstract boolean validateStep();

    abstract void doUserInterview(boolean validated) throws WizardNavigationException;

    abstract String getTitle();
}