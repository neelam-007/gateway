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
    private JTabbedPane tabs;
    private JPanel informattionPanel;
    private JPanel warningPanel;
    private JPanel errorPanel;
    private JTextArea errorMessages;
    private JTextArea warningMessages;
    private JTextArea informationMesssages;
    //private JLabel mainLabel;
    private JButton saveButton;
    private JButton button2;
    private JScrollPane errorsScroller;
    private JScrollPane warningsScroller;
    private JScrollPane infoScroller;
    private JTextArea messageText;

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

        tabs.removeAll();

        errorMessages.setBackground(mainPanel.getBackground());
        warningMessages.setBackground(mainPanel.getBackground());
        informationMesssages.setBackground(mainPanel.getBackground());
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

                ps.println("--- ERROR MESSAGES ---");
                ps.print(errorMessages.getText());

                ps.println("--- WARNING MESSAGES ---");
                ps.print(warningMessages.getText());

                ps.println("--- INFORMATIONAL MESSAGES ---");
                ps.print(informationMesssages.getText());
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
        //To change body of implemented methods use File | Settings | File Templates.
    }
    protected void updateView(HashMap settings) {
        ArrayList errors = ListHandler.getSevereLogList();
        ArrayList warnings = ListHandler.getWarningLogList();
        ArrayList infos = ListHandler.getInfoLogList();
        tabs.removeAll();
        ConfigurationWizard wizard = getParentWizard();
        if (wizard != null) {
            wizard.setCancelEnabled(false);
            wizard.setEnableBackButton(false);
        }
        if (errors.size() > 0) {
            Iterator errorIter = errors.iterator();
            StringBuffer errorBuffer = new StringBuffer();
            while (errorIter.hasNext()) {
                errorBuffer.append((String)errorIter.next()).append("\n");
            }
            tabs.add(errorPanel);
            tabs.setTitleAt(tabs.getTabCount() - 1, "Errors");
            errorMessages.setText(errorBuffer.toString());
//            tabs.setEnabledAt(0, true);

        } else {
//            tabs.setEnabledAt(0, false);
        }


        if (warnings.size() > 0) {
            Iterator warningIter = warnings.iterator();
            StringBuffer warningBuffer = new StringBuffer();
            while (warningIter.hasNext()) {
                warningBuffer.append((String)warningIter.next()).append("\n");
            }
            tabs.add(warningPanel);
            tabs.setTitleAt(tabs.getTabCount() - 1, "Warnings");
            warningMessages.setText(warningBuffer.toString());
//            tabs.setEnabledAt(1, true);
        } else {
//            tabs.setEnabledAt(1, false);
        }

        if (infos.size() > 0) {
            Iterator infoIter = infos.iterator();
            StringBuffer infoBuffer = new StringBuffer();
            while (infoIter.hasNext()) {
                infoBuffer.append((String)infoIter.next()).append("\n");
            }
            tabs.add(informattionPanel);
            tabs.setTitleAt(tabs.getTabCount() - 1, "Information");
            informationMesssages.setText(infoBuffer.toString());
//            tabs.setEnabledAt(2, true);
        } else {
//            tabs.setEnabledAt(2, false);
        }

        if (tabs.getTabCount() > 0)
            tabs.setSelectedIndex(0);

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
