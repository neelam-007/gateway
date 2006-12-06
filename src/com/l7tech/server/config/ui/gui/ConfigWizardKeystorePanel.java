package com.l7tech.server.config.ui.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.KeystoreType;
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
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Aug 17, 2005
 * Time: 9:58:50 AM
 * To change this template use File | Settings | File Templates.
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
        stepLabel = "Set Up SSG Keystore";
    }

    protected void updateModel() {
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        ksBean.setClusterType(getParentWizard().getClusteringType());
        if (dontDoKsConfig.isSelected()) {
            ksBean.doKeystoreConfig(false);
            getParentWizard().setKeystoreType(KeystoreType.NO_KEYSTORE);
        } else {
            ksBean.doKeystoreConfig(true);
            ksBean.setHostname(getParentWizard().getHostname());

            KeystoreType ksType = (KeystoreType) keystoreType.getSelectedItem();
            ksBean.setKeyStoreType(ksType);

            if (ksType == KeystoreType.DEFAULT_KEYSTORE_NAME) {
                ksBean.setKsPassword(((DefaultKeystorePanel)whichKeystorePanel).getKsPassword());
                ksBean.setDoBothKeys(((DefaultKeystorePanel)whichKeystorePanel).doBothKeys());
            } else {
                ksBean.setLunaInstallationPath(((LunaKeystorePanel)whichKeystorePanel).getLunaInstallPath());
                ksBean.setLunaJspPath(((LunaKeystorePanel)whichKeystorePanel).getLunaJSPPath());
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
        for (KeystoreType type : KeystoreType.values()) {
            if (type != KeystoreType.NO_KEYSTORE)
                kstypes.add(type);
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
            keystoresList = osFunctions.getKeystoreTypes();
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

        if (selectedItem == KeystoreType.DEFAULT_KEYSTORE_NAME) {
            whichKeystorePanel = new DefaultKeystorePanel();
        } else if (selectedItem == KeystoreType.LUNA_KEYSTORE_NAME) {
            whichKeystorePanel = new LunaKeystorePanel();
            ((LunaKeystorePanel)whichKeystorePanel).setDefaultLunaInstallPath(osFunctions.getLunaInstallDir());
            ((LunaKeystorePanel)whichKeystorePanel).setDefaultLunaJSPPath(osFunctions.getLunaJSPDir());
        } else {
        }

        ksDataPanel.removeAll();
        ksDataPanel.setLayout(borderLayout);
        ksDataPanel.add(whichKeystorePanel, BorderLayout.CENTER);
        ksDataPanel.revalidate();
    }

    public boolean isValidated() {
        if (!dontDoKsConfig.isSelected()) {
            KeystorePanel ksPanel = (KeystorePanel) whichKeystorePanel;
            return ksPanel.validateInput();
        } else {
            PartitionInformation pinfo = getParentWizard().getActivePartition();

            if (pinfo != null) {
                if (PartitionManager.getInstance().getActivePartition().isNewPartition()) {
                    JOptionPane.showMessageDialog(
                        this,
                        "Warning: You are configuring a new partition without a keystore. \nThis partition will not be able to start without a keystore.",
                        "New Partition With No Keystore",
                        JOptionPane.WARNING_MESSAGE);
                }
                pinfo.setShouldDisable(true);
            }
        }
        return true;
    }


}
