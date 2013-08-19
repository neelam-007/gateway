package com.l7tech.console.panels;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.exporter.PrivateKeyReference;
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
import java.util.Collections;
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
    private JTextField aliasTextField;
    private JRadioButton useDefaultKeyPairRadioButton;
    private JRadioButton useCustomKeyPairRadioButton;
    private JRadioButton removeRadioButton;
    private JRadioButton ignoreRadioButton;
    private JComboBox aliasCombo;
    private JButton manageCustomKeysButton;

    private PrivateKeyReference keyReference;

    public ResolvePrivateKeyPanel(WizardStepPanel next, PrivateKeyReference keyReference) {
        super(next);
        this.keyReference = keyReference;

        initialize();
    }

    @Override
    public String getDescription() {
        return getStepLabel();
    }

    @Override
    public boolean canFinish() {
        return !hasNextPanel();
    }

    @Override
    public String getStepLabel() {
        return "Unresolved private key " + keyReference.getKeyAlias();
    }

    @Override
    public boolean onNextButton() {
        if ( useDefaultKeyPairRadioButton.isSelected() ) {
            keyReference.setLocalizeReplace(true, null, new Goid(0,0));
        } else if ( useCustomKeyPairRadioButton.isSelected() ) {
            ComboEntry comboentry = (ComboEntry)aliasCombo.getSelectedItem();
            if (comboentry.alias == null) {
                return false;
            }
            keyReference.setLocalizeReplace(false, comboentry.alias, comboentry.keystoreid);
        } else if (removeRadioButton.isSelected()) {
            keyReference.setLocalizeDelete();
        } else if (ignoreRadioButton.isSelected()) {
            keyReference.setLocalizeIgnore();
        }
        return true;
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(mainPanel);

        aliasTextField.setText( keyReference.getKeyAlias() );
        aliasTextField.setCaretPosition( 0 );

        ActionListener modecheck = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableValueFieldAsAppropriate();
            }
        };
        useDefaultKeyPairRadioButton.addActionListener(modecheck);
        useCustomKeyPairRadioButton.addActionListener(modecheck);
        removeRadioButton.addActionListener(modecheck);
        ignoreRadioButton.addActionListener(modecheck);

        manageCustomKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                manageCustomKeys();
            }
        });

        populateCombobox();
        enableValueFieldAsAppropriate();
    }

    @Override
    public void notifyActive() {
        populateCombobox(); // ensure up to date
    }

    private void manageCustomKeys() {
        final PrivateKeyManagerWindow privateKeyManagerWindow = new PrivateKeyManagerWindow(owner);
        privateKeyManagerWindow.pack();
        Utilities.centerOnParentWindow(privateKeyManagerWindow);
        DialogDisplayer.display(privateKeyManagerWindow, new Runnable() {
            @Override
            public void run() {
                populateCombobox();
            }
        });
    }

    private void populateCombobox() {
        try {
            final java.util.List<KeystoreFileEntityHeader> keystores = getTrustedCertAdmin().findAllKeystores(true);
            if ( keystores != null ) {
                final java.util.List<ComboEntry> comboEntries = new ArrayList<ComboEntry>();
                final ComboEntry previousSelection = (ComboEntry) aliasCombo.getSelectedItem();
                for ( final KeystoreFileEntityHeader header : keystores ) {
                    for ( final SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(header.getGoid(), true) ) {
                        final ComboEntry comboEntry = new ComboEntry(header.getGoid(), header.getName(), entry.getAlias());
                        comboEntries.add(comboEntry);
                    }
                }

                Collections.sort(comboEntries);

                aliasCombo.setModel(new DefaultComboBoxModel(comboEntries.toArray()));

                if ( previousSelection != null && comboEntries.contains(previousSelection) ) {
                    aliasCombo.setSelectedItem( previousSelection );
                } else {
                    aliasCombo.setSelectedIndex( 0 );
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
        final boolean enableSelection = useCustomKeyPairRadioButton.isSelected();
        aliasCombo.setEnabled( enableSelection );
    }

    private class ComboEntry implements Comparable<ComboEntry> {
        private final Goid keystoreid;
        private final String keystorename;
        private final String alias;

        private ComboEntry(Goid keystoreid, String keystorename, String alias) {
            this.keystoreid = keystoreid;
            this.keystorename = keystorename;
            this.alias = alias;
        }

        @Override
        public int compareTo( final ComboEntry o ) {
            return alias.compareTo( o.alias );
        }

        public String toString() {
            return "'" + alias + "'" + " in " + keystorename;
        }

        @SuppressWarnings({ "RedundantIfStatement" })
        @Override
        public boolean equals( final Object o ) {
            if ( this == o ) return true;
            if ( o == null || getClass() != o.getClass() ) return false;

            final ComboEntry that = (ComboEntry) o;

            if ( keystoreid != that.keystoreid ) return false;
            if ( alias != null ? !alias.equals( that.alias ) : that.alias != null ) return false;
            if ( keystorename != null ? !keystorename.equals( that.keystorename ) : that.keystorename != null )
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = keystoreid != null ? keystoreid.hashCode() : 0;
            result = 31 * result + (keystorename != null ? keystorename.hashCode() : 0);
            result = 31 * result + (alias != null ? alias.hashCode() : 0);
            return result;
        }
    }
}
