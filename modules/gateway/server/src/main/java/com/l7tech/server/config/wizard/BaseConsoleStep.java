package com.l7tech.server.config.wizard;

import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import static com.l7tech.server.config.beans.BaseConfigurationBean.EOL;

/**
 * User: megery
 * Date: Feb 21 19, 2006
 * Time: 4:50:05 PM
 */
public abstract class BaseConsoleStep<CBT extends ConfigurationBean, CCT extends ConfigurationCommand> implements ConfigWizardConsoleStep {

    // - PUBLIC

    public BaseConsoleStep(ConfigurationWizard parent_) {
        this.parent = parent_;
    }

    // each step must implement these
    public abstract String getTitle();

    public abstract void doUserInterview(boolean validated) throws WizardNavigationException;

    public abstract boolean validateStep();
    
    @Override
    public void showTitle() {
        String title = getTitle();
        String banner = StringUtils.repeat("-", title.length());

        ConsoleWizardUtils.printText(new String[] {
                EOL,
                banner + EOL,
                title + EOL,
                banner + EOL,
        });
    }

    @Override
    public void showStep(boolean validated) throws WizardNavigationException {
        doUserInterview(validated);
        boolean isValid = validateStep();
        if (!isValid)
            showStep(false);
    }

    @Override
    public boolean shouldApplyConfiguration() {
        return false;
    }

    @Override
    public boolean isShowNavigation() {
        return showNavigation;
    }

    @Override
    public boolean isShowQuitMessage() {
        return showQuitMessage;
    }

    // - PROTECTED

    protected ConfigurationWizard parent;

    protected CBT configBean = null;

    protected CCT configCommand;

    protected boolean showNavigation = true;
    protected boolean showQuitMessage = true;


    protected ConfigurationWizard getParentWizard() {
        return parent;
    }

    protected String readLine() throws IOException {
        return ConsoleWizardUtils.readLine();
    }

    protected void handleInput(String input) throws WizardNavigationException {
        ConsoleWizardUtils.handleInput(input, isShowNavigation());
    }


    protected String getData(String[] promptLines, String defaultValue, String[] allowedEntries, String errorMessage, boolean isPassword) throws IOException, WizardNavigationException {
        return ConsoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), allowedEntries, errorMessage, isPassword);
    }

    protected String getData(String[] promptLines, String defaultValue, String[] allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        return getData(promptLines, defaultValue, allowedEntries, errorMessage,false);
    }

    protected String getData(String[] promptLines, String defaultValue, Pattern allowedEntries, String errorMessage, boolean isPassword) throws IOException, WizardNavigationException {
        return ConsoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), allowedEntries, errorMessage,isPassword);
    }

    protected String getData(String[] promptLines, String defaultValue, Pattern allowedEntries, String errorMessage) throws IOException, WizardNavigationException {
        return getData(promptLines, defaultValue, allowedEntries, errorMessage,false);
    }

    protected String getData(String[] promptLines, String defaultValue,  Functions.UnaryVoidThrows<String, Exception> verifer, String errorMessage, boolean isPassword) throws IOException, WizardNavigationException {
        return ConsoleWizardUtils.getData(promptLines, defaultValue, isShowNavigation(), verifer, errorMessage, isPassword);
    }

    protected String getData(String[] promptLines, String defaultValue,  Functions.UnaryVoidThrows<String, Exception> verifer, String errorMessage) throws IOException, WizardNavigationException {
        return getData(promptLines, defaultValue, verifer, errorMessage, false);
    }


    protected void printText(List<String> textToPrint) {
        if (textToPrint != null) ConsoleWizardUtils.printText(textToPrint.toArray(new String[textToPrint.size()]));
    }

    protected void printText(String[] textToPrint) {
        ConsoleWizardUtils.printText(textToPrint);
    }

    protected void printText(String textToPrint) {
        ConsoleWizardUtils.printText(textToPrint);
    }

    protected void storeInput() {
        if (configCommand != null) {
            getParentWizard().storeCommand(configCommand);
        }
    }

    protected boolean isYes(String answer) {
        return ConsoleWizardUtils.isYes(answer);
    }

    protected boolean getConfirmationFromUser(String message, String defaultValue) throws IOException, WizardNavigationException {
        String[] prompts = new String[] {
                message + " : [" + defaultValue +"]",
        };

        String input = getData(prompts, defaultValue, ConsoleWizardUtils.YES_NO_VALUES, "*** That is not a valid selection. Please answer Y or N. ***");
        return input != null && (isYes(input));
    }
}
