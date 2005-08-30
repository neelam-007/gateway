package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.beans.KeystoreConfigBean;
import com.l7tech.server.config.commands.KeystoreConfigCommand;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

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
    private JPanel whichKeystorePanel;

    private DefaultKeystorePanel defaultPanel = new DefaultKeystorePanel();
    private LunaKeystorePanel lunaPanel = new LunaKeystorePanel();

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


    public ConfigWizardKeystorePanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    protected void updateModel(HashMap settings) {
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        if (dontDoKsConfig.isSelected()) {
            ksBean.doKeystoreConfig(false);
        } else {
            ksBean.doKeystoreConfig(true);
            ksBean.setHostname(getParentWizard().getHostname());

            String ksType = (String) keystoreType.getSelectedItem();
            ksBean.setKeyStoreType(ksType);

            if (ksType == KeyStoreConstants.DEFAULT_KEYSTORE_NAME) {
                ksBean.setKsPassword(defaultPanel.getKsPassword());
                ksBean.setDoBothKeys(defaultPanel.doBothKeys());
            } else {
                //ksBean.overwriteLunaCerts(lunaPanel.isOverwriteExisting());
                ksBean.setLunaInstallationPath(lunaPanel.getLunaInstallPath());
                ksBean.setLunaJspPath(lunaPanel.getLunaJSPPath());
            }
        }
    }

    protected void updateView(HashMap settings) {
        boolean isLunaOk = true;
//        try {
//            isLunaOk = LunaProber.isPartitionLoggedIn();
//        } catch (ClassNotFoundException e) {
//            isLunaOk = false;
//        }
        String[] keystores = getKeystores(isLunaOk);
        KeystoreConfigBean ksConfigBean = (KeystoreConfigBean) configBean;

        String ksType = ksConfigBean.getKeyStoreType();
        keystoreType.setSelectedItem(ksType == null?keystores[0]:ksType);
    }

    private String[] getKeystores(boolean lunaOk) {
        if (lunaOk) {
            return getKeystores();
        }
        else {
            String[] fullList = getKeystores();
            ArrayList newKeystoreList = new ArrayList();
            for (int i = 0; i < fullList.length; i++) {
                String s = new String(fullList[i]);
                if (!s.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
                    newKeystoreList.add(s);
                }
            }
            return (String[]) newKeystoreList.toArray(new String[newKeystoreList.size()]);
        }

    }

    private void init() {
        setShowDescriptionPanel(false);
        stepLabel = "Setup SSG Keystore";
        configBean = new KeystoreConfigBean(osFunctions);
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

        lunaPanel.setDefaultLunaInstallPath(osFunctions.getLunaInstallDir());
        lunaPanel.setDefaultLunaJSPPath(osFunctions.getLunaJSPDir());
        
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
        String selectedItem = (String) keystoreType.getSelectedItem();
        if (selectedItem == null) {
            return;
        }

        if (selectedItem.equalsIgnoreCase(KeyStoreConstants.DEFAULT_KEYSTORE_NAME)) {
            whichKeystorePanel = defaultPanel;
        } else if (selectedItem.equalsIgnoreCase(KeyStoreConstants.LUNA_KEYSTORE_NAME)) {
            whichKeystorePanel = lunaPanel;
        } else {
        }

        ksDataPanel.removeAll();
        ksDataPanel.setLayout(borderLayout);
        ksDataPanel.add(whichKeystorePanel, BorderLayout.CENTER);
        ksDataPanel.validate();
    }

    public boolean onNextButton() {
        boolean allIsWell = true;
        if (!dontDoKsConfig.isSelected()) {
            KeystorePanel ksPanel = (KeystorePanel) whichKeystorePanel;
            allIsWell = ksPanel.validateInput();
        }
        return allIsWell;
    }


}
