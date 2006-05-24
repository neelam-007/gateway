package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 8, 2005
 * Time: 5:03:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardStartPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;

    public ConfigWizardStartPanel(WizardStepPanel next) {
        super(next);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        stepLabel = "Introduction";
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView(Set settings) {
    }

    protected void updateModel(Set settings) {
    }
}
