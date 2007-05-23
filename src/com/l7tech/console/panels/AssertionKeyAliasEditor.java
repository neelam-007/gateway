package com.l7tech.console.panels;

import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.server.security.keystore.SsgKeyEntry;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

/**
 * Dialog allowing SSM administrator to set the keystore used by a particular assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 23, 2007<br/>
 */
public class AssertionKeyAliasEditor extends JDialog {
    private final Logger logger = Logger.getLogger(AssertionKeyAliasEditor.class.getName());
    private PrivateKeyable assertion;
    private JPanel mainPanel;
    private JButton helpButton;
    private JButton OKButton;
    private JButton cancelButton;
    private JRadioButton useDefaultKeypairRadioButton;
    private JRadioButton useCustomKeyPairRadioButton;
    private JComboBox aliasCombo;
    private JButton manageCustomKeysButton;
    private JPanel customFrame;
    private boolean wasOKed = false;

    public AssertionKeyAliasEditor(Frame owner, PrivateKeyable assertion) {
        super(owner, true);
        this.assertion = assertion;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Set Private Key Alias");
        ActionListener modecheck = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableValueFieldAsAppropriate();
            }
        };
        useDefaultKeypairRadioButton.addActionListener(modecheck);
        useCustomKeyPairRadioButton.addActionListener(modecheck);

        OKButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                ok();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                cancel();
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                help();
            }
        });

        manageCustomKeysButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent actionEvent) {
                manageCustomKeys();
            }
        });

        populateCombobox();

        if (assertion.isUsesDefaultKeyStore()) {
            useDefaultKeypairRadioButton.setSelected(true);
        } else {
            useCustomKeyPairRadioButton.setSelected(true);
        }

        enableValueFieldAsAppropriate();

        Utilities.setEnterAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                ok();
            }
        });
        Utilities.setEscAction(this, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                cancel();
            }
        });
    }

    private void manageCustomKeys() {
        PrivateKeyManagerWindow pkmw;
        try {
            pkmw = new PrivateKeyManagerWindow(this);
        } catch (RemoteException e) {
            logger.log(Level.WARNING, "problem loading key manager window", e);
            return;
        }
        pkmw.pack();
        Utilities.centerOnScreen(pkmw);
        DialogDisplayer.display(pkmw, new Runnable() {
            public void run() {
                populateCombobox();
            }
        });

    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    public boolean hasAssertionChanged() {
        return wasOKed;
    }

    class ComboEntry {
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

    private void populateCombobox() {
        //PrivateKeyManagerWindow
        try {
            java.util.List<TrustedCertAdmin.KeystoreInfo> keystores = getTrustedCertAdmin().findAllKeystores();
            if (keystores != null) {
                int size = 0;
                for (TrustedCertAdmin.KeystoreInfo ksi : keystores) {
                    size += getTrustedCertAdmin().findAllKeys(ksi.id).size();
                }
                ComboEntry[] existingAlias = new ComboEntry[size];
                int i = 0;
                int tosel = 0;
                for (TrustedCertAdmin.KeystoreInfo ksi : keystores) {
                    for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(ksi.id)) {
                        existingAlias[i] = new ComboEntry(ksi.id, ksi.name, entry.getAlias());
                        if (assertion.getNonDefaultKeystoreId() == ksi.id) {
                            if (entry.getId().equals(assertion.getKeyId())) {
                                tosel = i;
                            }
                        }
                        i++;
                    }
                }
                aliasCombo.setModel(new DefaultComboBoxModel(existingAlias));
                aliasCombo.setSelectedItem(existingAlias[tosel]);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "problem populating keystore info", e);
        }
    }

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        dispose();
    }

    private void ok() {
        if (useDefaultKeypairRadioButton.isSelected()) {
            assertion.setUsesDefaultKeystore(true);
        } else {
            assertion.setUsesDefaultKeystore(false);
            ComboEntry comboentry = (ComboEntry)aliasCombo.getSelectedItem();
            assertion.setKeyId(comboentry.alias);
            assertion.setNonDefaultKeystoreId(comboentry.keystoreid);
        }
        wasOKed = true;
        cancel();
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
}
