package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 8, 2005
 * Time: 5:03:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardStartPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;
    boolean initialized = false;

    public ConfigWizardStartPanel(WizardStepPanel next) {
        super(next);
        stepLabel = "Introduction";
        setShowDescriptionPanel(false);
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected boolean isValidated() {
        return true;
    }

    protected void updateView() {}

    protected void updateModel() {}
}
