package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.common.gui.widgets.PleaseWaitDialog;
import com.l7tech.common.gui.util.Utilities;

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
    private JPanel summaryPanel;

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

    protected void updateView(HashMap settings) {
        if (settings != null) {
            StringBuffer buffer = new StringBuffer();
            Set keys = settings.keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                String key = (String) iterator.next();
                ConfigurationCommand command = (ConfigurationCommand) settings.get(key);
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
//            summaryText.setText(summaryText.getText() + "\n this is a really long line that should cause the screen to wrap or scroll 01234567890abcdefghijklmnopqrstuvwxyz");
        }
    }

    protected void updateModel(HashMap settings) {
    }

    public boolean onNextButton() {
        final PleaseWaitDialog dlg =
                new PleaseWaitDialog(this.getParentWizard(),
                                     "Configuration is being applied");
        dlg.setModal(true);
        Utilities.centerOnScreen(dlg);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    getParentWizard().applyConfiguration();
                } finally {
                    dlg.setVisible(false);
                    dlg.dispose();
                }
            }
        });
        dlg.pack();
        dlg.validate();
        dlg.setVisible(true); // Will block here until work finishes
        return true;
    }
}
