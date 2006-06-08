package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.WizardInputValidator;
import com.l7tech.server.config.OSDetector;
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
    protected ConfigurationWizard parent;

    protected ConfigurationBean configBean = null;

    protected OSSpecificFunctions osFunctions = null;

    protected ConfigurationCommand configCommand;

    protected boolean showNavigation = true;
    protected boolean showQuitMessage = true;

    private static final String eolChar = System.getProperty("line.separator");

    public static String getEolChar() {
        return eolChar;
    }



    public BaseConsoleStep(ConfigurationWizard parent_) {
        this.parent = parent_;
        osFunctions = OSDetector.getOSSpecificFunctions();
        consoleWizardUtils = parent.getWizardUtils();
    }

    public void showStep(boolean validated) throws WizardNavigationException {
        doUserInterview(validated);
        boolean isValid = validateStep();
        if (isValid) setupManualSteps();
        else showStep(false);
    }

    protected void setupManualSteps() {
        if (configBean != null) {
            List<String> steps = configBean.getManualSteps();
            if (steps == null || steps.isEmpty()) {
                return;
            }

            getParentWizard().addManualSteps(this.getClass().getName(), steps);
        }
    }

    public ConfigurationCommand getConfigurationCommand() {
        return configCommand;
    }

    public boolean shouldApplyConfiguration() {
        return false;
    }

    public boolean isShowNavigation() {
        return showNavigation;
    }

    public boolean isShowQuitMessage() {
        return showQuitMessage;
    }

    protected ConfigurationWizard getParentWizard() {
        return parent;
    }

    public void showTitle() {
        String title = getTitle();
        String banner = StringUtils.repeat("-", title.length());

        consoleWizardUtils.printText(new String[] {
                eolChar,
                banner + eolChar,
                title + eolChar,
                banner + eolChar,
        });
    }

    public String readLine() throws IOException {
        return consoleWizardUtils.readLine();
    }


    protected String getMatchingPasswords(final String firstPrompt, final String secondPrompt, final int minPasswordLength) throws IOException, WizardNavigationException {
        Map passwords = consoleWizardUtils.getValidatedDataWithConfirm(
            new String[]{firstPrompt, secondPrompt},
            null,
            -1,
            isShowNavigation(),
            new WizardInputValidator() {
                public String[] validate(Map inputs) {
                    int passwordLength = minPasswordLength;
                    if (minPasswordLength < 0) {
                        passwordLength = 0;
                    }
                    List<String> errorMessages = new ArrayList<String>();

                    String password1 = (String) inputs.get(firstPrompt);
                    String password2 = (String) inputs.get(secondPrompt);

                    if (StringUtils.isEmpty(password1)) {
                        errorMessages.add("**** The password cannot be empty ****" + eolChar);
                    } else if (password1.length() < passwordLength) {
                        errorMessages.add("**** The password must be at least " + passwordLength +" characters long. Please try again ****" + eolChar);
                    } else if (!StringUtils.equals(password1, password2)) {
                        errorMessages.add("**** The passwords do not match ****" + eolChar);
                    }

                    if (errorMessages.size() > 0) {
                        return errorMessages.toArray(new String[errorMessages.size()]);
                    }
                    return null;
                }
            });

        return (String) passwords.get(firstPrompt);
    }

    protected void handleInput(String input) throws WizardNavigationException {
        consoleWizardUtils.handleInput(input, isShowNavigation());
    }

    protected String getData(List<String> promptLines, String defaultValue, String[] allowedEntries) throws IOException, WizardNavigationException {
        if (promptLines == null) return "";
        return consoleWizardUtils.getData(promptLines.toArray(new String[]{}), defaultValue, isShowNavigation(), allowedEntries);
    }

    protected String getData(List<String> promptLines, String defaultValue) throws IOException, WizardNavigationException {
        if (promptLines == null) return "";
        return consoleWizardUtils.getData(promptLines.toArray(new String[]{}), defaultValue, isShowNavigation(), null);
    }

    protected String getData(String[] promptLines, String defaultValue, String[] allowedEntries) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), allowedEntries);
    }

    protected String getData(String[] promptLines, String defaultValue) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), null);
    }

    protected void printText(List<String> textToPrint) {
        if (textToPrint != null) consoleWizardUtils.printText(textToPrint.toArray(new String[textToPrint.size()]));
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

    //each step must implement these.
    public abstract boolean validateStep();

    public abstract void doUserInterview(boolean validated) throws WizardNavigationException;

    public abstract String getTitle();
}