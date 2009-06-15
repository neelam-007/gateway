package com.l7tech.console.panels;

import com.l7tech.console.policy.exporter.PrivateKeyReference;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.TrustedCertAdmin;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class is similar with {@link AssertionKeyAliasEditor}
 *
 * User: ghuang
 */
public class ResolvePrivateKeyPanel extends WizardStepPanel {
    private final Logger logger = Logger.getLogger(ResolvePrivateKeyPanel.class.getName());

    private JPanel mainPanel;
    private JRadioButton useDefaultKeypairRadioButton;
    private JRadioButton useCustomKeyPairRadioButton;
    private JComboBox aliasCombo;
    private JButton manageCustomKeysButton;
    private JPanel customFrame;

    private PrivateKeyReference keyReference;

    public ResolvePrivateKeyPanel(WizardStepPanel next, PrivateKeyReference keyReference) {
        super(next);
        this.keyReference = keyReference;

        initialize();
    }

    public String getDescription() {
        return getStepLabel();
    }

    public boolean canFinish() {
        return !hasNextPanel();
    }

    public String getStepLabel() {
        return "Unresolved Private Key " + keyReference.getKeyAlias();
    }

    public boolean onNextButton() {
        if (useDefaultKeypairRadioButton.isSelected()) {
            keyReference.setLocalizeReplace(true, null, 0);
        } else {
            ComboEntry comboentry = (ComboEntry)aliasCombo.getSelectedItem();
            keyReference.setLocalizeReplace(false, comboentry.alias, comboentry.keystoreid);
            if (comboentry.alias == null) {
                return false;
            }
        }

        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        ActionListener modecheck = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableValueFieldAsAppropriate();
            }
        };
        useDefaultKeypairRadioButton.addActionListener(modecheck);
        useCustomKeyPairRadioButton.addActionListener(modecheck);

        manageCustomKeysButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                manageCustomKeys();
            }
        });

        populateCombobox();

        if (keyReference.isDefaultKey()) {
            useDefaultKeypairRadioButton.setSelected(true);
        } else {
            useCustomKeyPairRadioButton.setSelected(true);
        }

        enableValueFieldAsAppropriate();
    }

    private void manageCustomKeys() {
        PrivateKeyManagerWindow pkmw;
        pkmw = new PrivateKeyManagerWindow(owner);
        pkmw.pack();
        Utilities.centerOnScreen(pkmw);
        DialogDisplayer.display(pkmw, new Runnable() {
            public void run() {
                populateCombobox();
            }
        });
    }

    private void populateCombobox() {
        try {
            java.util.List<KeystoreFileEntityHeader> keystores = getTrustedCertAdmin().findAllKeystores(true);
            if (keystores != null) {
                java.util.List<ComboEntry> comboEntries = new ArrayList<ComboEntry>();
                ComboEntry toSelect = null;
                final long wantId = keyReference.getKeystoreOid();
                for (KeystoreFileEntityHeader kfeh : keystores) {
                    for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(kfeh.getOid())) {
                        ComboEntry comboEntry = new ComboEntry(kfeh.getOid(), kfeh.getName(), entry.getAlias());
                        comboEntries.add(comboEntry);
                        if ((wantId == 0 || wantId == -1 || wantId == kfeh.getOid()) && entry.getAlias().equalsIgnoreCase(keyReference.getKeyAlias()))
                            toSelect = comboEntry;
                    }
                }
                if (toSelect == null && !keyReference.isDefaultKey()) {
                    // Alias is configured, but it doesn't exist on this Gateway (Bug #4143)
                    toSelect = new ComboEntry(wantId, "UNRECOGNIZED", keyReference.getKeyAlias());
                    comboEntries.add(0, toSelect);
                }
                aliasCombo.setModel(new DefaultComboBoxModel(comboEntries.toArray()));
                if (toSelect != null) {
                    aliasCombo.setSelectedItem(toSelect);
                } else {
                    aliasCombo.setSelectedIndex(0);
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "problem populating keystore info", e);
        }
    }

    private TrustedCertAdmin getTrustedCertAdmin() {
        return Registry.getDefault().getTrustedCertManager();
    }

    private void enableValueFieldAsAppropriate() {
        if (useDefaultKeypairRadioButton.isSelected()) {
            aliasCombo.setEnabled(false);
            manageCustomKeysButton.setEnabled(false);
            customFrame.setEnabled(false);
        } else {
            aliasCombo.setEnabled(true);
            manageCustomKeysButton.setEnabled(true);
            customFrame.setEnabled(true);
        }
    }


    private class ComboEntry {
        public ComboEntry(long keystoreid, String keystorename, String alias) {
            this.keystoreid = keystoreid;
            this.keystorename = keystorename;
            this.alias = alias;
        }

        public long keystoreid;
        public String keystorename;
        public String alias;
        public String toString() {
            return "'" + alias + "'" + " in " + keystorename;
        }
    }
}
