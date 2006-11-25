package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.commands.ConfigurationCommand;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 15, 2005
 * Time: 5:00:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardSummaryPanel extends ConfigWizardStepPanel {
    private JTextArea summaryText;
    private JPanel mainPanel;

    private String newline= "\n";
    private JScrollPane summaryScroller;
    private boolean initialized = false;

    public ConfigWizardSummaryPanel(WizardStepPanel next) {
        super(next);
        stepLabel = "SSG Configuration Summary";
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
