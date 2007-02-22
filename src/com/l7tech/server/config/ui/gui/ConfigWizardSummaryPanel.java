package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.commands.ConfigurationCommand;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * User: megery
 * Date: Aug 15, 2005
 */
public class ConfigWizardSummaryPanel extends ConfigWizardStepPanel {
    private JTextArea summaryText;
    private JPanel mainPanel;

    private String newline= "\n";
    private boolean initialized = false;

    public ConfigWizardSummaryPanel(WizardStepPanel next) {
        super(next);
        stepLabel = "Configuration Summary";
    }

    private void init() {
        setShowDescriptionPanel(false);
        summaryText.setBackground(mainPanel.getBackground());
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView() {
        if (!initialized) init();
        
        Object input = getParentWizard().getWizardInput();
        if (input instanceof Set) {
            Set<ConfigurationCommand> commands = (Set<ConfigurationCommand>) input;

            StringBuffer buffer = new StringBuffer();

            for (ConfigurationCommand command : commands) {
                String[] actions = command.getActions();
                if (actions != null) {
                    for (String action : actions) {
                        buffer.append(action).append(newline);
                    }
                }
                buffer.append(newline);
            }
            summaryText.setText("");
            summaryText.setText(buffer.toString());
        }
    }

    protected void updateModel() {
    }
}
