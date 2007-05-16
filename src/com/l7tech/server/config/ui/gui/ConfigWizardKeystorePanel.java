package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.KeystoreType;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.partition.PartitionManager;
import com.l7tech.server.partition.PartitionInformation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

/**
 * User: megery
 * The main panel for the Config Wizard (GUI) keystore step. At runtime, subsititutes a
 * KeystorePanel for whatever keystore type we are configuring.
 */
public class ConfigWizardKeystorePanel extends ConfigWizardStepPanel {
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

    public ConfigWizardKeystorePanel(WizardStepPanel next) {
        super(next);
        stepLabel = "Set Up the SSG Keystore";
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

        boolean isLunaOk = true;
        String[] keystores = getKeystores(isLunaOk);
        KeystoreConfigBean ksConfigBean = (KeystoreConfigBean) configBean;

        KeystoreType ksType = ksConfigBean.getKeyStoreType();
        keystoreType.setSelectedItem(ksType == null?keystores[0]:ksType);
    }

    private String[] getKeystores(boolean lunaOk) {
        if (lunaOk) {
            return getKeystores();
        }
        else {
            String[] fullList = getKeystores();
            ArrayList<String> newKeystoreList = new ArrayList<String>();
            for (int i = 0; i < fullList.length; i++) {
                String s = new String(fullList[i]);
                if (!s.equalsIgnoreCase(KeystoreType.LUNA_KEYSTORE_NAME.toString())) {
                    newKeystoreList.add(s);
                }
            }
            return newKeystoreList.toArray(new String[newKeystoreList.size()]);
        }

    }

    private void init() {
        osFunctions = getParentWizard().getOsFunctions();

        setShowDescriptionPanel(false);
        java.util.List<KeystoreType> kstypes = new ArrayList<KeystoreType>();

        for (OSSpecificFunctions.KeystoreInfo ksInfo : osFunctions.getAvailableKeystores()) {
            KeystoreType kstype = ksInfo.getType();
            if (kstype != KeystoreType.NO_KEYSTORE && kstype != KeystoreType.UNDEFINED)
                kstypes.add(kstype);
        }
        keystoreType.setModel(new DefaultComboBoxModel(kstypes.toArray(new KeystoreType[0])));
        configBean = new KeystoreConfigBean();
        configCommand = new KeystoreConfigCommand(configBean);

        ButtonGroup doKsChoices = new ButtonGroup();
        doKsChoices.add(doKsConfig);
        doKsChoices.add(dontDoKsConfig);
        dontDoKsConfig.setSelected(true);
        enableKsConfigPanel();

        doKsConfig.addActionListener(doKsConfigActionListener);
        dontDoKsConfig.addActionListener(doKsConfigActionListener);

        String[] keystores = getKeystores();
        if (keystores == null) {
            setSkipped(true);
        } else {
            updateKeystoreList(keystores, keystores[0]);
        }

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

    private String[] getKeystores() {
        if (keystoresList == null) {
            OSSpecificFunctions.KeystoreInfo[] keystoreInfos = osFunctions.getAvailableKeystores();
            if (keystoreInfos == null) {
                keystoresList = new String[0];
            } else {
                keystoresList = new String[keystoreInfos.length];
                for (int i = 0; i < keystoreInfos.length; i++) {
                    KeystoreType type = keystoreInfos[i].getType();
                    keystoresList[i] = type.getName();
                }
            }
        }
        return keystoresList;
    }

    private void updateKeystoreList(String[] keystores, String selected) {
        if (keystoreType.getItemCount() == 0) {
            for (int i = 0; i < keystores.length; i++) {
                String keystore = keystores[i];
                keystoreType.addItem(keystore);
            }
        }
        keystoreType.setSelectedItem(selected);
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

        boolean isValid = true;
        boolean shouldDisable = true;
        if (!dontDoKsConfig.isSelected()) {
            shouldDisable = false;
            KeystorePanel ksPanel = (KeystorePanel) whichKeystorePanel;
            isValid = ksPanel.validateInput();
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
            }
        }
        pinfo.setShouldDisable(shouldDisable);
        return isValid;
    }


}