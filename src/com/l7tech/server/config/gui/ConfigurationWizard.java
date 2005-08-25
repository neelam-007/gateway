package com.l7tech.server.config.gui;

import com.l7tech.console.action.Actions;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.commands.LoggingConfigCommand;
import com.l7tech.server.config.exceptions.UnsupportedOsException;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 8, 2005
 * Time: 3:29:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationWizard extends Wizard {
    private boolean isNewInstall;
    private static OSSpecificFunctions osFunctions;
    private String hostname;

    static {
        try {
            osFunctions = OSDetector.getOSSpecificActions();
        } catch (UnsupportedOsException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Creates new wizard
     */
    protected ConfigurationWizard(Frame parent, WizardStepPanel panel) {
        super(parent, panel);
        init(panel);
    }

    protected ConfigurationWizard(Dialog parent, WizardStepPanel panel) {
        super(parent, panel);
        init(panel);
    }

    public void init(WizardStepPanel panel) {
        setResizable(true);
        setTitle("SSG Configuration Wizard");
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);
        wizardInput = new HashMap();


        addWizardListener(new WizardAdapter() {
            public void wizardSelectionChanged(WizardEvent e) {
                // dont care
            }
            public void wizardFinished(WizardEvent e) {
                applyConfiguration();
            }
            public void wizardCanceled(WizardEvent e) {
                // dont care
            }
        });

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ConfigurationWizard.this);
            }
        });
        pack();
    }

    private void applyConfiguration() {
        HashMap commands = (HashMap) wizardInput;

        //we need to add this to make sure that non clustering/db/etc. specific actions occur
        LoggingConfigCommand loggingCommand = new LoggingConfigCommand(null, osFunctions);
        commands.put(loggingCommand.getClass().getName(), loggingCommand);

        Set keys = commands.keySet();
        java.util.Iterator iterator = keys.iterator();

        while (iterator.hasNext()) {
            String key = (String) iterator.next();
            ConfigurationCommand cmd = (ConfigurationCommand) commands.get(key);
            cmd.execute();
        }
    }

    private static ConfigWizardStepPanel getStartPanel() {
        ConfigWizardSummaryPanel lastPanel = new ConfigWizardSummaryPanel(null, osFunctions);
        ConfigWizardKeystorePanel keystorePanel = new ConfigWizardKeystorePanel(lastPanel, osFunctions);
        ConfigWizardDatabasePanel configWizardDatabasePanelPanel = new ConfigWizardDatabasePanel(keystorePanel, osFunctions);
        ConfigWizardClusteringPanel clusteringPanel = new ConfigWizardClusteringPanel(configWizardDatabasePanelPanel, osFunctions);

        ConfigWizardStepPanel startPanel;
        if (osFunctions.isWindows())  {
            ConfigWizardStepPanel servicePanel = new ConfigWizardWinServicePanel(clusteringPanel, osFunctions);
            startPanel = new ConfigWizardStartPanel(servicePanel, osFunctions);
        } else {
            startPanel = new ConfigWizardStartPanel(clusteringPanel,osFunctions);
        }

        return startPanel;
    }

    public static ConfigurationWizard getInstance(Frame parent) {
        ConfigWizardStepPanel startPanel = getStartPanel();
        return new ConfigurationWizard(parent, startPanel);
    }

    public static ConfigurationWizard getInstance(Dialog parent) {
        ConfigWizardStepPanel startPanel = getStartPanel();
        return new ConfigurationWizard(parent, startPanel);
    }


    public void setIsNewInstall(boolean newInstall) {
        isNewInstall = newInstall;
    }

    public boolean isNewInstall() {
        return isNewInstall;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String newHostname) {
        hostname = newHostname;
    }

    public static void main(String[] args)
      throws Exception {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        ConfigurationWizard wizard = ConfigurationWizard.getInstance(new JFrame());

        wizard.setSize(780, 560);
        Utilities.centerOnScreen(wizard);
        wizard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        wizard.setVisible(true);
    }

}
