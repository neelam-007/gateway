package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.ConfigurationCommand;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 9:47:19 AM
 * To change this template use File | Settings | File Templates.
 */
public abstract class ConfigWizardStepPanel extends WizardStepPanel {
    protected OSSpecificFunctions osFunctions;
    protected ConfigurationWizard parentWizard;
    protected String stepLabel;

    protected ConfigurationBean configBean;
    protected ConfigurationCommand configCommand;

    public ConfigWizardStepPanel(WizardStepPanel next) {
        super(next);
        osFunctions = OSDetector.getOSSpecificFunctions();
    }

    public void setParentWizard(ConfigurationWizard parent) {
        parentWizard = parent;
    }

    public ConfigurationWizard getParentWizard() {
        return (ConfigurationWizard)getOwner();
    }

    public String getStepLabel() {
        return stepLabel;
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        updateView();
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        updateModel();
        getParentWizard().storeCommand(configCommand);
    }

    public boolean canFinish() {
        return false;
    }

    protected void setupManualSteps() {
        if (configBean != null) {
            updateModel();
            List<String> steps = configBean.getManualSteps();
            if (steps == null || steps.isEmpty()) {
                return;
            }

            getParentWizard().addManualSteps(this.getClass().getName(), steps);
        }
    }

    public boolean onNextButton() {
        boolean isValid = isValidated();
        if (isValid) setupManualSteps();
        return isValid;
    }

    protected boolean isValidated() {
        return true;
    }

    protected abstract void updateModel();

    protected abstract void updateView();
}
