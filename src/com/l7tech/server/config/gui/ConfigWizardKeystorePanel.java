package com.l7tech.server.config.gui;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.server.config.OSSpecificFunctions;
import com.l7tech.server.config.KeyStoreConstants;
import com.l7tech.server.config.commands.KeystoreConfigCommand;
import com.l7tech.server.config.beans.KeystoreConfigBean;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.HashMap;
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

    public ConfigWizardKeystorePanel(WizardStepPanel next, OSSpecificFunctions functions) {
        super(next, functions);
        init();
    }

    protected void updateModel(HashMap settings) {
        KeystoreConfigBean ksBean = (KeystoreConfigBean) configBean;
        ksBean.setHostname(getParentWizard().getHostname());

        String ksType = (String) keystoreType.getSelectedItem();
        ksBean.setKeyStoreType(ksType);

        if (ksType == KeyStoreConstants.DEFAULT_KEYSTORE_NAME) {
            ksBean.setKsPassword(defaultPanel.getKsPassword());
            ksBean.setDoBothKeys(defaultPanel.doBothKeys());
        } else {
            ksBean.overwriteLunaCerts(lunaPanel.isOverwriteExisting());
            ksBean.setLunaInstallationPath(lunaPanel.getLunaInstallPath());
            ksBean.setLunaJspPath(lunaPanel.getLunaJSPPath());
        }
    }

    protected void updateView(HashMap settings) {
        String[] keystores = getKeystores();
        KeystoreConfigBean ksConfigBean = (KeystoreConfigBean) configBean;

        String ksType = ksConfigBean.getKeyStoreType();
        keystoreType.setSelectedItem(ksType == null?keystores[0]:ksType);
    }

    private void init() {
        setShowDescriptionPanel(false);
        stepLabel = "Setup SSG Keystore";
        configBean = new KeystoreConfigBean(osFunctions);
        configCommand = new KeystoreConfigCommand(configBean);

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
        KeystorePanel ksPanel = (KeystorePanel) whichKeystorePanel;
        return ksPanel.validateInput();
    }


}
