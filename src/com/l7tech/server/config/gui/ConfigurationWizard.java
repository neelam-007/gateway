package com.l7tech.server.config.gui;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.JdkLoggerConfigurator;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 8, 2005
 * Time: 3:29:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class ConfigurationWizard extends Wizard {
    static Logger log = Logger.getLogger(ConfigurationWizard.class.getName());

    private boolean isNewInstall;
    private static OSSpecificFunctions osFunctions;
    private String hostname;

    public static final String RESOURCE_PATH = "com/l7tech/console/resources";
    public static final String LOG_PROPERTIES_NAME = "configuration-logging.properties";

    boolean hadFailures = false;
    private int clusteringType;

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
        setTitle("SSG Configuration Wizard for " + osFunctions.getOSName());
        setShowDescription(false);
        Actions.setEscKeyStrokeDisposes(this);
        wizardInput = new HashMap();


        addWizardListener(new WizardAdapter() {
            public void wizardSelectionChanged(WizardEvent e) {
                // dont care
            }
            public void wizardFinished(WizardEvent e) {
                //applyConfiguration();
                System.exit(0);
            }
            public void wizardCanceled(WizardEvent e) {
                System.out.println("Wizard Cancelled");
                System.exit(1);
            }
        });

        getButtonHelp().addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(ConfigurationWizard.this);
            }
        });
        pack();
    }

    public void applyConfiguration() {
        log.info("Applying the configuration changes");
        HashMap commands = (HashMap) wizardInput;

        //we need to add this to make sure that non clustering/db/etc. specific actions occur
        LoggingConfigCommand loggingCommand = new LoggingConfigCommand(null, osFunctions);
        commands.put(loggingCommand.getClass().getName(), loggingCommand);

        Set keys = commands.keySet();
        java.util.Iterator iterator = keys.iterator();

            hadFailures = false;
            while (iterator.hasNext()) {
                boolean successful = true;
                String key = (String) iterator.next();
                ConfigurationCommand cmd = (ConfigurationCommand) commands.get(key);
                successful = cmd.execute();
                if (!successful) {
                    hadFailures = true;
                }
            }
    }

    private static ConfigWizardStepPanel getStartPanel() {
        ConfigWizardResultsPanel lastPanel = new ConfigWizardResultsPanel(null, osFunctions);
        ConfigWizardSummaryPanel summaryPanel = new ConfigWizardSummaryPanel(lastPanel, osFunctions);
        ConfigWizardKeystorePanel keystorePanel = new ConfigWizardKeystorePanel(summaryPanel, osFunctions);
        ConfigWizardNewDBPanel configWizardDatabasePanelPanel = new ConfigWizardNewDBPanel(keystorePanel, osFunctions);
        //ConfigWizardDatabasePanel configWizardDatabasePanelPanel = new ConfigWizardDatabasePanel(keystorePanel, osFunctions);
        ConfigWizardClusteringPanel clusteringPanel = new ConfigWizardClusteringPanel(configWizardDatabasePanelPanel, osFunctions);

        ConfigWizardStepPanel startPanel;
        startPanel = new ConfigWizardStartPanel(clusteringPanel,osFunctions);

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

    public boolean isHadFailures() {
        return hadFailures;
    }

    public void setCancelEnabled(boolean enabled) {
        getButtonCancel().setEnabled(enabled);
    }

    public JButton getBackButton() {
        return getButtonBack();
    }

    public static void main(String[] args) {

        initLogging();
        log.info("Starting SSG Configuration Wizard");

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InstantiationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IllegalAccessException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        JFrame mainFrame = new JFrame();
        Image icon = ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png");
        ImageIcon imageIcon = new ImageIcon(icon);
        mainFrame.setIconImage(imageIcon.getImage());
        ConfigurationWizard wizard = ConfigurationWizard.getInstance(mainFrame);

        wizard.setSize(780, 560);
        Utilities.centerOnScreen(wizard);
        wizard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        wizard.setVisible(true);
    }

    private static void initLogging() {
        // apache logging layer to use the jdk logger
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.Jdk14Logger");
        JdkLoggerConfigurator.configure("com.l7tech", "configlogging.properties");
    }

    public int getClusteringType() {
        return clusteringType;
    }

    public void setClusteringType(int clusteringConfigured) {
        this.clusteringType = clusteringConfigured;
    }
}
