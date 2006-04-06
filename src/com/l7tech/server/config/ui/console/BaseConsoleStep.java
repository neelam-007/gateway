package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.exceptions.WizardNavigationException;
import com.l7tech.server.config.beans.ConfigurationBean;

import java.io.*;

import org.apache.commons.lang.StringUtils;

/**
 * User: megery
 * Date: Feb 21 19, 2006
 * Time: 4:50:05 PM
 */
public abstract class BaseConsoleStep implements ConfigWizardConsoleStep {

    ConfigurationWizard parent;
    protected final PrintWriter out;
    protected final InputStream in;

   protected ConfigurationCommand configCommand;

    protected OSSpecificFunctions osFunctions = null;
    protected ConfigurationBean configBean = null;
    protected BufferedReader reader;
    protected ConfigurationCommand command;

    protected boolean readyToAdvance;

    protected boolean showNavigation = true;

    public BaseConsoleStep(ConfigurationWizard parent_, OSSpecificFunctions osFunctions) {
        this.parent = parent_;
        this.in = parent.getInputSteam();
        this.out = parent.getWriter();
        this.osFunctions = osFunctions;
        reader = new BufferedReader(new InputStreamReader(in));
        readyToAdvance = false;
    }

    public void showStep(boolean validated) throws WizardNavigationException {
        doUserInterview(validated);
        if (!validateStep()) showStep(false);
    }

    public void showTitle() {
        String title = getTitle();
        String banner = StringUtils.repeat("-", title.length());

        out.println();
        out.println(banner);
        out.println(title);
        out.println(banner);
        out.flush();
    }

    public ConfigurationCommand getConfigurationCommand() {
        return command;
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

    protected void handleInput(String input) throws WizardNavigationException {
        parent.handleInput(input);
    }

    protected void storeInput() {
        if (command != null) {
            getParent().storeCommand(command);
        }
    }

    protected String getData(String[] promptLines, String defaultValue) throws IOException, WizardNavigationException {
        doPromptLines(promptLines);
        String input = reader.readLine();
        handleInput(input);
        if (StringUtils.isEmpty(input)) {
            input = defaultValue;
        }
        return input;
    }


    private void doPromptLines(String[] promptLines) {
        if (promptLines != null) {
            for (int i = 0; i < promptLines.length; i++) {
                String promptLine = promptLines[i];
                out.print(promptLine);
            }
            out.flush();
        }
    }


    abstract boolean validateStep();

    abstract void doUserInterview(boolean validated) throws WizardNavigationException;

    abstract String getTitle();
}