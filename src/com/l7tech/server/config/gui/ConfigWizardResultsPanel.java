package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.ListHandler;
import com.l7tech.server.config.commands.ClusteringConfigCommand;
import com.l7tech.server.config.beans.ClusteringConfigBean;

import javax.swing.*;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.logging.Logger;
import java.util.logging.Handler;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 30, 2005
 * Time: 2:56:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigWizardResultsPanel extends ConfigWizardStepPanel {
    static Logger logger = Logger.getLogger(ConfigWizardResultsPanel.class.getName());
    private JPanel mainPanel;
    private JTabbedPane tabs;
    private JPanel informattionPanel;
    private JPanel warningPanel;
    private JPanel errorPanel;
    private JTextArea errorMessages;
    private JTextArea warningMessages;
    private JTextArea informationMesssages;
    private JLabel mainLabel;

    public ConfigWizardResultsPanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    private void init() {
        setShowDescriptionPanel(false);
        configBean = null;
        configCommand = null;
        stepLabel = "Configuration Results";

        tabs.removeAll();

        errorMessages.setBackground(mainPanel.getBackground());
        warningMessages.setBackground(mainPanel.getBackground());
        informationMesssages.setBackground(mainPanel.getBackground());

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    protected void updateModel(HashMap settings) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
    protected void updateView(HashMap settings) {
        ArrayList errors = ListHandler.getSevereLogList();
        ArrayList warnings = ListHandler.getWarningLogList();
        ArrayList infos = ListHandler.getInfoLogList();
        tabs.removeAll();

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

        tabs.setSelectedIndex(0);

        boolean hadFailures = getParentWizard().isHadFailures();
        if (hadFailures) {
                mainLabel.setText("There were errors during configuration, see below for details");
        } else {
            mainLabel.setText("The configuration was successfully applied");
        }
    }

    public boolean canFinish() {
        return true;
    }
}
