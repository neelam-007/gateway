package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.ConfigurationBean;
import com.l7tech.server.config.commands.ConfigurationCommand;

import java.util.HashMap;

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

    public ConfigWizardStepPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next);
        osFunctions = functions;
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
        HashMap settingsMap = (HashMap) settings;
        updateView(settingsMap);
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        HashMap settingsMap = (HashMap) settings;
        updateModel(settingsMap);
        if (configCommand != null) {
            settingsMap.put(configCommand.getClass().getName(), configCommand);
        }
    }

    public boolean canFinish() {
        return false;
    }



    protected abstract void updateModel(HashMap settings);

    protected abstract void updateView(HashMap settings);
}
