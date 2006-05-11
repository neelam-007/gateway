package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.ConfigurationCommand;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;
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

    public ConfigWizardSummaryPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        stepLabel = "SSG Configuration Summary";
        summaryText.setBackground(mainPanel.getBackground());
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateView(Set settings) {
        if (settings != null) {
            StringBuffer buffer = new StringBuffer();

            Iterator iterator = settings.iterator();
            while (iterator.hasNext()) {
                ConfigurationCommand command = (ConfigurationCommand) iterator.next();
                String[] affectedObjects = command.getActionSummary();
                if (affectedObjects != null) {
                    for (int i = 0; i < affectedObjects.length; i++) {
                        String affectedObject = affectedObjects[i];
                        buffer.append(affectedObject).append(newline);
                    }
                }
                buffer.append(newline);
            }
            summaryText.setText("");
            summaryText.setText(buffer.toString());
        }
    }

    protected void updateModel(Set settings) {
    }
}
