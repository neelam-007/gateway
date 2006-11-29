package com.l7tech.server.config.ui.gui;

import com.incors.plaf.kunststoff.KunststoffLookAndFeel;
import com.l7tech.common.BuildInfo;
import com.l7tech.common.gui.util.ImageCache;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.JdkLoggerConfigurator;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.panels.Wizard;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.*;
import com.l7tech.server.config.commands.AppServerConfigCommand;
import com.l7tech.server.config.commands.ConfigurationCommand;
import com.l7tech.server.config.commands.LoggingConfigCommand;
import com.l7tech.server.config.commands.RmiConfigCommand;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.server.partition.PartitionInformation;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
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
//    private OSSpecificFunctions osFunctions;
    private String hostname;

    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    boolean hadFailures = false;
    private static String currentVersion;

    private Set<ConfigurationCommand> additionalCommands;
    private ManualStepsManager manualSteps;
    private ClusteringType clusteringType;
//    private String partitionName;

    static {
        currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
        String subMinor = BuildInfo.getProductVersionSubMinor();
        if (subMinor != null && !subMinor.equals("")) currentVersion += "." + subMinor;
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

    /**
     * sets up the wizard, and it's event handlers
     * @param panel
     */
    public void init(WizardStepPanel panel) {
        setTitle("SSG Configuration Wizard for " + OSDetector.getOSSpecificFunctions().getOSName() + " (Version " + getCurrentVersion() + ")");
        setShowDescription(false);
        setEscKeyStrokeDisposes(this);
        wizardInput = new HashSet<ConfigurationCommand>();
        manualSteps = new ManualStepsManager();

        setupAdditionalCommands();

        addWizardListener(new WizardAdapter() {
            public void wizardSelectionChanged(WizardEvent e) {
                // dont care
            }
            public void wizardFinished(WizardEvent e) {
                System.exit(0);
            }
            public void wizardCanceled(WizardEvent e) {
                System.out.println("Wizard Cancelled");
                System.exit(1);
            }
        });
        getButtonHelp().setVisible(false);
        pack();
    }

    private void setupAdditionalCommands() {
        additionalCommands = new HashSet<ConfigurationCommand>();
        //make sure that the server.xml gets appropriately upgraded to include the new ConnectionId Management stuff
        additionalCommands.add(new AppServerConfigCommand());

        //we need to add these to make sure that non clustering/db/etc. specific actions occur
        additionalCommands.add(new LoggingConfigCommand(null));
        additionalCommands.add(new RmiConfigCommand(null));
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
        Set<ConfigurationCommand>commands = (HashSet<ConfigurationCommand>) wizardInput;

        commands.addAll(additionalCommands);

        java.util.Iterator iterator = commands.iterator();

            hadFailures = false;
            while (iterator.hasNext()) {
                boolean successful = true;
                ConfigurationCommand cmd = (ConfigurationCommand) iterator.next();
                successful = cmd.execute();
                if (!successful) {
                    hadFailures = true;
                }
            }
    }

    public void storeCommand(ConfigurationCommand configCommand) {
        if (configCommand != null) {
            Set<ConfigurationCommand> commands = (Set<ConfigurationCommand>) wizardInput;
            if (commands.contains(configCommand)) {
                commands.remove(configCommand);
            }
            commands.add(configCommand);
        }
    }

    /**
     * builds the chain of ConfigWizardStepPanel objects that will comprise the wizard.
     * @return the first panel in the list, linked to the next ... and so on
     */
    private static ConfigWizardStepPanel getStartPanel() {
        ConfigWizardResultsPanel lastPanel = new ConfigWizardResultsPanel(null);
        ConfigWizardSummaryPanel summaryPanel = new ConfigWizardSummaryPanel(lastPanel);
        ConfigWizardKeystorePanel keystorePanel = new ConfigWizardKeystorePanel(summaryPanel);
        ConfigWizardNewDBPanel configWizardDatabasePanelPanel = new ConfigWizardNewDBPanel(keystorePanel);
        ConfigWizardClusteringPanel clusteringPanel = new ConfigWizardClusteringPanel(configWizardDatabasePanelPanel);
        ConfigWizardPartitioningPanel partitionPanel = new ConfigWizardPartitioningPanel(clusteringPanel);

        ConfigWizardStepPanel startPanel;
        startPanel = new ConfigWizardStartPanel(partitionPanel);

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
//            osFunctions = OSDetector.getOSSpecificFunctions();
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

    public ClusteringType getClusteringType() {
        return clusteringType;
    }

    public void setClusteringType(ClusteringType clusteringType) {
        this.clusteringType = clusteringType;
        manualSteps.setClusteringType(clusteringType);
    }

    public void setKeystoreType(KeystoreType ksType) {
        manualSteps.setKeystoreType(ksType);
    }

    public java.util.List<String> getManualSteps() {
        return manualSteps.getManualSteps();
    }

    public static String getCurrentVersion() {
        return currentVersion;
    }

    public String getPartitionName() {
        return PartitionManager.getInstance().getActivePartition().getPartitionId();
    }

    public void setPartitionName(PartitionInformation partition) {
        PartitionManager.getInstance().setActivePartition(partition);
    }

    public OSSpecificFunctions getOsFunctions() {
        PartitionInformation pi = PartitionManager.getInstance().getActivePartition();
        return (pi == null?OSDetector.getOSSpecificFunctions(""):pi.getOSSpecificFunctions());
    }
}