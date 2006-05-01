package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.common.BuildInfo;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSDetector;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.commands.LoggingConfigCommand;
import com.l7tech.server.config.commands.RmiConfigCommand;
import com.l7tech.server.config.commands.AppServerConfigCommand;
import com.incors.plaf.kunststoff.KunststoffLookAndFeel;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The SSG Configuration Wizard. Extends the functionality of the Wizard framework specifically for the ConfigurationWizard.
 *
 * A sequence of ConfigWizardStepPanel panels are presented to the user, and a list of ConfigurationCommand objects is created.
 * Each ConfigurationCommand encapsulates the configuration activities for a specific area of SSG configuration.
 *
 * This Wizard configures the following:
 * <ul>
 * <li><ul>Connection to a database
 *  <li>connection to an existing database</li>
 *  <li>creation of a new database and subsequent connection to it</li>
 *  <li>update the hibernate.properties file with the connection information</li>
 *
 *
 * </ul></li>
 *
 *
 * </ul>
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
    private String keystoreType;
    private String currentVersion;

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

    /**
     * sets up the wizard, and it's event handlers
     * @param panel
     */
    public void init(WizardStepPanel panel) {
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
        setTitle("SSG Configuration Wizard for " + osFunctions.getOSName());
        setShowDescription(false);
        setEscKeyStrokeDisposes(this);
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
        getButtonHelp().setVisible(false);
        //  Disable for now
        //
        //        getButtonHelp().addActionListener(new ActionListener() {
        //            public void actionPerformed(ActionEvent e) {
        //                Actions.invokeHelp(ConfigurationWizard.this);
        //            }
        //        });
        pack();
    }

    /**
     * Iterates over the list of ConfigurationCommand objects created in response to user input.
     * Calls ConfigurationCommand.execute() on each of the commands to perform the relevant actions.
     *
     * Sets a failure flag if any of the commands returned an error.
     *
     * NOTE: This method can be called from any Thread, not just the Swing thread, since it does
     * absolutely no GUI manipulation
     */
    public void applyConfiguration() {
        log.info("Applying the configuration changes");
        HashMap commands = (HashMap) wizardInput;

        //make sure that the server.xml gets appropriately upgraded to include the new ConnectionId Management stuff
        AppServerConfigCommand appserverCommand = new AppServerConfigCommand(osFunctions);
        commands.put(appserverCommand.getClass().getName(), appserverCommand);

        //we need to add this to make sure that non clustering/db/etc. specific actions occur
        LoggingConfigCommand loggingCommand = new LoggingConfigCommand(null, osFunctions);
        commands.put(loggingCommand.getClass().getName(), loggingCommand);

        RmiConfigCommand rmiCommand = new RmiConfigCommand(null, osFunctions);
        commands.put(rmiCommand.getClass().getName(), rmiCommand);

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

    /**
     * builds the chain of ConfigWizardStepPanel objects that will comprise the wizard.
     * @return the first panel in the list, linked to the next ... and so on
     */
    private static ConfigWizardStepPanel getStartPanel() {
        ConfigWizardResultsPanel lastPanel = new ConfigWizardResultsPanel(null, osFunctions);
        ConfigWizardSummaryPanel summaryPanel = new ConfigWizardSummaryPanel(lastPanel, osFunctions);
        ConfigWizardKeystorePanel keystorePanel = new ConfigWizardKeystorePanel(summaryPanel, osFunctions);
        ConfigWizardNewDBPanel configWizardDatabasePanelPanel = new ConfigWizardNewDBPanel(keystorePanel, osFunctions);
        ConfigWizardClusteringPanel clusteringPanel = new ConfigWizardClusteringPanel(configWizardDatabasePanelPanel, osFunctions);

        ConfigWizardStepPanel startPanel;
        startPanel = new ConfigWizardStartPanel(clusteringPanel,osFunctions);

        return startPanel;
    }

    /**
     * returns the Singleton instance of this wizard, with a Frame as the parent
     * @param parent - the parent of this wizard
     * @return the Wizard instance
     */
    public static ConfigurationWizard getInstance(Frame parent) {
        ConfigWizardStepPanel startPanel = getStartPanel();
        return new ConfigurationWizard(parent, startPanel);
    }

    /**
     * returns the Singleton instance of this wizard, with a DIalog as the parent
     * @param parent - the parent of this wizard
     * @return the Wizard instance
     */
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

    /**
     * exposes the back button from the underlying Wizard so that it can be enabled/disabled etc.
     * @return the back button
     */
    public JButton getBackButton() {
        return getButtonBack();
    }

    public JButton getNextButton() {
        return getButtonNext();
    }

    public JButton getFinishButton() {
        return getButtonFinish();
    }

    public JButton getCancelButton() {
        return getButtonCancel();
    }

    public static void main(String[] args) {
        startWizard(args);
    }

    public static void startWizard(String[] args) {
        initLogging();
        log.info("Starting SSG Configuration Wizard");

        try {
            UIManager.setLookAndFeel(new KunststoffLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        JFrame mainFrame = new JFrame();
        Image icon = ImageCache.getInstance().getIcon(RESOURCE_PATH + "/layer7_logo_small_32x32.png");
        ImageIcon imageIcon = new ImageIcon(icon);
        mainFrame.setIconImage(imageIcon.getImage());
        try {
            osFunctions = OSDetector.getOSSpecificActions();
            ConfigurationWizard wizard = ConfigurationWizard.getInstance(mainFrame);
            wizard.setSize(780, 560);
            Utilities.centerOnScreen(wizard);
            wizard.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            wizard.setVisible(true);
        }
        catch(OSSpecificFunctions.MissingPropertyException mpe) {
            System.out.println(mpe.getMessage());
        }
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

    public String getKeystoreType() {
        return keystoreType;
    }

    public void setKeystoreType(String type) {
        keystoreType = type;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }


}