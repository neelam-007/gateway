package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.OSSpecificFunctions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.PrintStream;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 30, 2005
 * Time: 2:56:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardResultsPanel extends ConfigWizardStepPanel {
    private JPanel mainPanel;

    private JButton saveButton;
    private JTextArea messageText;
    private JTextArea logsView;
    private JPanel logsPanel;

    public ConfigWizardResultsPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        configBean = null;
        configCommand = null;
        stepLabel = "Configuration Results";
        saveButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                doSaveLogs();
            }
        });

        logsView.setBackground(mainPanel.getBackground());
        messageText.setBackground(mainPanel.getBackground());

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void doSaveLogs() {
        JFileChooser fc = new JFileChooser(".");
        int retval = fc.showOpenDialog(this);
        File selectedFile = null;
        if (retval == JFileChooser.CANCEL_OPTION) {
        } else if (retval == JFileChooser.APPROVE_OPTION) {
            selectedFile = fc.getSelectedFile();
        }
        if (selectedFile != null) {
            PrintStream ps = null;
            try {
                ps = new PrintStream(new FileOutputStream(selectedFile));

                ps.print(logsView.getText());

                ps.close();
                ps = null;
            } catch (FileNotFoundException e) {
            } finally{
                if (ps != null) {
                    ps.close();
                }
            }
        }
    }

    protected void updateModel(HashMap settings) {
    }

    protected void updateView(HashMap settings) {
        ArrayList logs = ListHandler.getLogList();
        ConfigurationWizard wizard = getParentWizard();
        if (wizard != null) {
            wizard.setCancelEnabled(false);
            wizard.setEnableBackButton(false);
        }
        if (logs.size() > 0) {
            Iterator logIter = logs.iterator();
            StringBuffer logBuffer = new StringBuffer();
            while (logIter.hasNext()) {
                logBuffer.append((String)logIter.next()).append("\n");
            }
            logsView.setText(logBuffer.toString());
        }
        boolean hadFailures = getParentWizard().isHadFailures();
        if (hadFailures) {
            messageText.setText("There were errors during configuration, see below for details");
        } else {
            messageText.setText("The configuration was successfully applied\n" +
                    "You must restart the SSG in order for the configuration to take effect.");
        }
    }

    public boolean canFinish() {
        return true;
    }
}
