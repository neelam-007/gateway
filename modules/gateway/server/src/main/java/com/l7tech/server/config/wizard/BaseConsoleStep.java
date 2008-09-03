package com.l7tech.server.config.wizard;

import com.l7tech.server.config.WizardInputValidator;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * User: megery
 * Date: Feb 21 19, 2006
 * Time: 4:50:05 PM
 */
public abstract class BaseConsoleStep<CBT extends ConfigurationBean, CCT extends ConfigurationCommand> implements ConfigWizardConsoleStep {

    protected ConsoleWizardUtils consoleWizardUtils;
    protected ConfigurationWizard parent;

    protected CBT configBean = null;

    protected CCT configCommand;

    protected boolean showNavigation = true;
    protected boolean showQuitMessage = true;

    private static final String eolChar = System.getProperty("line.separator");

    public static String getEolChar() {
        return eolChar;
    }

    public BaseConsoleStep(ConfigurationWizard parent_) {
        this.parent = parent_;
        consoleWizardUtils = parent.getWizardUtils();
    }

    public void showStep(boolean validated) throws WizardNavigationException {
        doUserInterview(validated);
        boolean isValid = validateStep();
        if (!isValid)
            showStep(false);
    }


    public CCT getConfigurationCommand() {
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
                        if (!StringUtils.isEmpty(secondPrompt)) //only complain if the caller actually prompted for a second password
                            errorMessages.add("**** The passwords do not match ****" + eolChar);
                    }

                    if (errorMessages.size() > 0) {
                        return errorMessages.toArray(new String[errorMessages.size()]);
                    }
                    return null;
                }
            },
            true);

        return (String) passwords.get(firstPrompt);
    }

    protected void handleInput(String input) throws WizardNavigationException {
        consoleWizardUtils.handleInput(input, isShowNavigation());
    }

    protected String getSecretData(String[] promptLines, String defaultValue, Pattern allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getSecretData(promptLines, defaultValue, isShowNavigation(), allowedEntries, errorMessage);
    }

    protected String getData(String[] promptLines, String defaultValue, String[] allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), allowedEntries, errorMessage);
    }

    protected String getData(String[] promptLines, String defaultValue, Pattern allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        return consoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), allowedEntries, errorMessage);
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

    protected boolean isYes(String answer) {
        return consoleWizardUtils.isYes(answer);
    }

    protected String getVersionString() {
        return "(Version " + ConfigurationWizard.getCurrentVersion() + ")";
    }

    protected boolean getConfirmationFromUser(String message, String defaultValue) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                message + " : [" + defaultValue +"]",
        };

        String input = getData(prompts, defaultValue, ConsoleWizardUtils.YES_NO_VALUES, "*** That is not a valid selection. Please answer Y or N. ***");
        return input != null && (isYes(input));
    }
}