package com.l7tech.server.config.ui.console;

import com.l7tech.server.config.commands.ConfigurationCommand;

import java.io.*;

/**
 * User: megery
 * Date: Feb 21 19, 2006
 * Time: 4:50:05 PM
 */
public abstract class BaseConsoleStep implements ConfigWizardConsoleStep {

    ConfigurationWizard parent;
    protected final PrintWriter out;
    protected final InputStream in;

    ConfigWizardConsoleStep previousStep;
    ConfigWizardConsoleStep nextStep;

    protected BaseConsoleStep(ConfigurationWizard parent_, InputStream is, PrintWriter pw) {
        this.parent = parent_;
        this.in = is;
        this.out = pw;
    }

    public ConfigurationCommand getConfigurationCommand() {
        return makeConfigCommand();
    }

    protected ConfigurationWizard getParent() {
        return parent;
    }

    public void collectInput() {
        showTitle();
        doInputCollection();
    }

    public void showTitle() {
        out.println("----------------------------------------------");
        out.println(getTitle());
        out.println("----------------------------------------------");
    }

    protected abstract void doInputCollection();

    public abstract String getTitle();

    protected abstract ConfigurationCommand makeConfigCommand();


    public ConfigWizardConsoleStep getPreviousStep() {
        return previousStep;
    }

    public void setPreviousStep(ConfigWizardConsoleStep previousStep) {
        this.previousStep = previousStep;
    }

    public ConfigWizardConsoleStep getNextStep() {
        return nextStep;
    }

    public void setNextStep(ConfigWizardConsoleStep nextStep) {
        this.nextStep = nextStep;
    }

    public void goForward() {
        getParent().next(this);
    }

    public void goBack() {
        getParent().previous(this);
    }
}