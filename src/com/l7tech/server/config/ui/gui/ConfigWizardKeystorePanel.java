package com.l7tech.server.config.ui.gui;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.KeystoreActions;
import com.l7tech.server.config.KeystoreActionsListener;
import com.l7tech.server.config.KeystoreType;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.partition.PartitionInformation;
import com.l7tech.server.partition.PartitionManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * User: megery
 * The main panel for the Config Wizard (GUI) keystore step. At runtime, subsititutes a
 * KeystorePanel for whatever keystore type we are configuring.
 */
public class ConfigWizardKeystorePanel extends ConfigWizardStepPanel implements KeystoreActionsListener {
    private static final Logger logger = Logger.getLogger(ConfigWizardKeystorePanel.class.getName());

    private JPanel mainPanel;
    private JComboBox keystoreType;
    private JPanel ksDataPanel;
    private KeystorePanel whichKeystorePanel;

    BorderLayout borderLayout = new BorderLayout();

    String[] keystoresList = null;

    private ActionListener ksTypeChangeActionListener = new ActionListener() {

        public void actionPerformed(ActionEvent e) {
            updateKeystorePanel();
        }
    };

    private ActionListener doKsConfigActionListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
                enableKsConfigPanel();
            }
    };

    private JPanel ksConfigPanel;
    private JRadioButton doKsConfig;
    private JRadioButton dontDoKsConfig;
    private JTextPane errorMessage;

    public ConfigWizardKeystorePanel(WizardStepPanel next) {
        super(next);
        stepLabel = "Set Up the SSG Keystore";
        errorMessage.setForeground(Color.RED);
        errorMessage.setVisible(false);
    }

    protected void updateModel() {
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        if (dontDoKsConfig.isSelected()) {
            ksBean.doKeystoreConfig(false);
            getParentWizard().setKeystoreType(KeystoreType.NO_KEYSTORE);
        } else {
            ksBean.doKeystoreConfig(true);
            ksBean.setHostname(getParentWizard().getHostname());

            KeystoreType ksType = (KeystoreType) keystoreType.getSelectedItem();
            ksBean.setKeyStoreType(ksType);

            switch (ksType) {
                case DEFAULT_KEYSTORE_NAME:
                    ksBean.setKsPassword(((DefaultKeystorePanel)whichKeystorePanel).getKsPassword());
                    ksBean.setDoBothKeys(((DefaultKeystorePanel)whichKeystorePanel).doBothKeys());
                    break;
                case LUNA_KEYSTORE_NAME:
                    ksBean.setLunaInstallationPath(((LunaKeystorePanel)whichKeystorePanel).getLunaInstallPath());
                    ksBean.setLunaJspPath(((LunaKeystorePanel)whichKeystorePanel).getLunaJSPPath());
                    break;
                case SCA6000_KEYSTORE_NAME:
                    ksBean.setKsPassword(((Sca6000KeystorePanel)whichKeystorePanel).getPassword());
                    ksBean.setShouldBackupMasterKey(((Sca6000KeystorePanel)whichKeystorePanel).isShouldBackupMasterKey());
                    ksBean.setInitializeHSM(((Sca6000KeystorePanel)whichKeystorePanel).isInitializeHSM());
                    break;
                default:
                    break;
            }

            getParentWizard().setKeystoreType(ksType);
        }
    }

    protected void updateView() {
        if (osFunctions == null) init();

        keystoreType.setModel(new DefaultComboBoxModel(getKeystoreTypes()));
        KeystoreConfigBean ksConfigBean = (KeystoreConfigBean) configBean;

        KeystoreType ksType = ksConfigBean.getKeyStoreType();
         if (ksType == null) {
            //set the HSM one
            keystoreType.setSelectedItem(OSSpecificFunctions.KeystoreInfo.isHSMEnabled()?KeystoreType.SCA6000_KEYSTORE_NAME:KeystoreType.DEFAULT_KEYSTORE_NAME);
        } else {
            keystoreType.setSelectedItem(ksType);
            switch (ksType) {
                case DEFAULT_KEYSTORE_NAME:
                    DefaultKeystorePanel defaultpanel = (DefaultKeystorePanel) whichKeystorePanel;
                    defaultpanel.setKsPassword(ksConfigBean.getKsPassword());
                    break;
                case LUNA_KEYSTORE_NAME:
                    break;
                case SCA6000_KEYSTORE_NAME:
                    Sca6000KeystorePanel scapanel = (Sca6000KeystorePanel) whichKeystorePanel;
                    scapanel.setKsPassword(ksConfigBean.getKsPassword());
                    break;
                default:
                    break;
            }
        }
    }

    private void init() {
        osFunctions = getParentWizard().getOsFunctions();

        setShowDescriptionPanel(false);

        keystoreType.setModel(new DefaultComboBoxModel(getKeystoreTypes()));
        configBean = new KeystoreConfigBean();
        configCommand = new KeystoreConfigCommand(configBean);

        ButtonGroup doKsChoices = new ButtonGroup();
        doKsChoices.add(doKsConfig);
        doKsChoices.add(dontDoKsConfig);
        dontDoKsConfig.setSelected(true);
        enableKsConfigPanel();

        doKsConfig.addActionListener(doKsConfigActionListener);
        dontDoKsConfig.addActionListener(doKsConfigActionListener);

        updateKeystorePanel();

        keystoreType.addActionListener(ksTypeChangeActionListener);

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void enableKsConfigPanel() {
        if (dontDoKsConfig.isSelected()) {
            ksConfigPanel.setVisible(false);
            ksDataPanel.setVisible(false);
        } else {
            ksConfigPanel.setVisible(true);
            ksDataPanel.setVisible(true);
        }
    }

    private KeystoreType[] getKeystoreTypes() {
        java.util.List<KeystoreType> kstypes = new ArrayList<KeystoreType>();
        String activePartitionName = PartitionManager.getInstance().getActivePartition().getPartitionId();
        for (OSSpecificFunctions.KeystoreInfo ksInfo : osFunctions.getAvailableKeystores()) {
            KeystoreType kstype = ksInfo.getType();
            if (kstype != KeystoreType.NO_KEYSTORE && kstype != KeystoreType.UNDEFINED) {
                if (kstype == KeystoreType.SCA6000_KEYSTORE_NAME && !activePartitionName.equals(PartitionInformation.DEFAULT_PARTITION_NAME))
                    continue;
                kstypes.add(kstype);
            }
        }
        return kstypes.toArray(new KeystoreType[0]);
    }

    private void updateKeystorePanel() {
        KeystoreType selectedItem = (KeystoreType) keystoreType.getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        switch (selectedItem) {
            case DEFAULT_KEYSTORE_NAME:
                whichKeystorePanel = new DefaultKeystorePanel();
                break;
            case LUNA_KEYSTORE_NAME:
                whichKeystorePanel = new LunaKeystorePanel();
                LunaKeystorePanel lkp = (LunaKeystorePanel) whichKeystorePanel;
                OSSpecificFunctions.KeystoreInfo ksInfo = osFunctions.getKeystore(KeystoreType.LUNA_KEYSTORE_NAME);
                lkp.setDefaultLunaInstallPath(ksInfo.getMetaInfo("INSTALL_DIR"));
                lkp.setDefaultLunaJSPPath(ksInfo.getMetaInfo("JSP_DIR"));
                break;
            case SCA6000_KEYSTORE_NAME:
                whichKeystorePanel = new Sca6000KeystorePanel();
                break;
        }

        ksDataPanel.removeAll();
        ksDataPanel.setLayout(borderLayout);
        ksDataPanel.add(whichKeystorePanel, BorderLayout.CENTER);
        ksDataPanel.revalidate();
    }

    public boolean isValidated() {
        PartitionInformation pinfo = getParentWizard().getActivePartition();

        boolean shouldDisable = true;
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        if (!dontDoKsConfig.isSelected()) {
            pinfo.setShouldDisable(false);
            KeystorePanel ksPanel = (KeystorePanel) whichKeystorePanel;
            if (ksPanel.validateInput(ksBean)) {
                KeystoreActions ka = new KeystoreActions(osFunctions);
                try {
                    byte[] existingSharedKey = ka.getSharedKey(this);
                    if (existingSharedKey != null) {
                        ksBean.setSharedKeyBytes(existingSharedKey);
                    }
                    shouldDisable = false;
                } catch (KeystoreActions.KeystoreActionsException e) {
                    shouldDisable = true;
                    showErrorMessage("Error while updating the cluster shared key\n" + e.getMessage());
                }
            }
        } else {
            if (pinfo != null) {
                if (PartitionManager.getInstance().getActivePartition().isNewPartition()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Warning: You are configuring a new partition without a keystore. \nThis partition will not be able to start without a keystore.",
                        "New Partition With No Keystore",
                        JOptionPane.WARNING_MESSAGE);
                    shouldDisable = true;
                } else {
                    shouldDisable = false;
                }
                pinfo.setShouldDisable(shouldDisable);
            }
        }
        return !shouldDisable;
    }

    private void showErrorMessage(String s) {
        errorMessage.setText(s);
        errorMessage.setVisible(true);
    }

    private void hideErrorMessage() {
        errorMessage.setVisible(false);
    }


    public java.util.List<String> promptForKeystoreTypeAndPassword() {
        java.util.List<String> answers = new ArrayList<String>();
        String title = "Existing keystore information";
        String passwordMessage = "Please provide the password for the existing keystore";
        String typeMessage = "Please provide the type for the existing keystore";
        String[] allowedTypes = new String[] {
            KeystoreType.DEFAULT_KEYSTORE_NAME.shortTypeName(),
            KeystoreType.SCA6000_KEYSTORE_NAME.shortTypeName(),
            KeystoreType.LUNA_KEYSTORE_NAME.shortTypeName(),
        };

        KeystoreInformationDialog kiDialog = new KeystoreInformationDialog(this.getParentWizard(), title, passwordMessage, typeMessage, allowedTypes);
        Utilities.centerOnParentWindow(kiDialog);
        kiDialog.setVisible(true);

        char[] password = kiDialog.getPassword();
        String ksType = kiDialog.getKeystoreType();
        answers.add(new String(password));
        answers.add(ksType);
        return answers;
    }

    public void printKeystoreInfoMessage(String msg) {
    }
}