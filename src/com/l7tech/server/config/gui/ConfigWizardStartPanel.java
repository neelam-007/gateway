package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 8, 2005
 * Time: 5:03:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardStartPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;
    private JLabel infoLabel;
    private JTextArea textArea1;


    /**
     * Creates new form WizardPanel
     */

    public ConfigWizardStartPanel(OSSpecificFunctions functions) {
        super(null, functions);
        init();
    }

    public ConfigWizardStartPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        stepLabel = "Introduction";
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView(HashMap settingsMap) {
    }

    protected void updateModel(HashMap settingsMap) {
    }
}
