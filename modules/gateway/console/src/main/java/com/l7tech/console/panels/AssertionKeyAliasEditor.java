package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.keystore.KeystoreFileEntityHeader;
import com.l7tech.console.action.Actions;
import com.l7tech.console.util.Registry;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Dialog allowing SSM administrator to set the keystore used by a particular assertion.
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 23, 2007<br/>
 *
 * @see PrivateKeysComboBox
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
    private boolean readonly;

    public AssertionKeyAliasEditor(Frame owner, PrivateKeyable assertion, boolean readonly) {
        super(owner, true);
        this.assertion = assertion;
        this.readonly = readonly;
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        setTitle("Private Key Alias");
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
        OKButton.setEnabled(!readonly);

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

        aliasCombo.setRenderer(new TextListCellRenderer<ComboEntry>(
            // For the cell renderer
            new Functions.Unary<String, ComboEntry>(){
                @Override
                public String call( final ComboEntry comboEntry) {
                    // If alias and keystorename are too long (exceeding 64 chars), trim them.
                    String trimedAlias = (comboEntry.alias != null && comboEntry.alias.length() > 64)? comboEntry.alias.substring(0, 64) + "..." : comboEntry.alias;
                    String trimedKeystoreName = (comboEntry.keystorename != null && comboEntry.keystorename.length() > 64)? comboEntry.keystorename.substring(0, 64) + "..." : comboEntry.keystorename;
                    return "'" + trimedAlias + "'" + " in " + trimedKeystoreName;
                }
            },
            // For the tooltips
            new Functions.Unary<String, ComboEntry>(){
                @Override
                public String call(final ComboEntry comboEntry) {
                    return comboEntry.toString();
                }
            },
            false
        ));

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
        pkmw = new PrivateKeyManagerWindow(this);
        pkmw.pack();
        Utilities.centerOnScreen(pkmw);
        DialogDisplayer.display(pkmw, new Runnable() {
            public void run() {
                populateCombobox();

                // Resize the dialog size if needed.
                JRootPane rootpane = mainPanel.getRootPane();
                Container rootparent = rootpane.getParent();
                if (rootparent instanceof JInternalFrame) {
                    JInternalFrame jif = (JInternalFrame)rootparent;
                    Dimension newSize = mainPanel.getPreferredSize();
                    Dimension fullSize = new Dimension(10 + (int)newSize.getWidth(), 32 + (int)newSize.getHeight());
                    jif.setSize(fullSize);
                } else {
                    pack();
                }
            }
        });
    }

    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    public boolean hasAssertionChanged() {
        return wasOKed;
    }

    class ComboEntry implements Comparable {
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

        @Override
        public int compareTo(Object o) {
            return alias.compareTo(((ComboEntry)o).alias);
        }
    }

    private void populateCombobox() {
        //PrivateKeyManagerWindow
        try {
            java.util.List<KeystoreFileEntityHeader> keystores = getTrustedCertAdmin().findAllKeystores(true);
            if (keystores != null) {
                java.util.List<ComboEntry> comboEntries = new ArrayList<ComboEntry>();
                ComboEntry toSelect = null;
                final long wantId = assertion.getNonDefaultKeystoreId();
                for (KeystoreFileEntityHeader kfeh : keystores) {
                    for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(kfeh.getOid())) {
                        ComboEntry comboEntry = new ComboEntry(kfeh.getOid(), kfeh.getName(), entry.getAlias());
                        comboEntries.add(comboEntry);
                        if ((wantId == 0 || wantId == -1 || wantId == kfeh.getOid()) && entry.getAlias().equalsIgnoreCase(assertion.getKeyAlias()))
                            toSelect = comboEntry;
                    }
                }
                if (toSelect == null && !assertion.isUsesDefaultKeyStore()) {
                    // Alias is configured, but it doesn't exist on this Gateway (Bug #4143)
                    toSelect = new ComboEntry(wantId, "UNRECOGNIZED", assertion.getKeyAlias());
                    comboEntries.add(0, toSelect);
                }
                Collections.sort(comboEntries);
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

    private void help() {
        Actions.invokeHelp(this);
    }

    private void cancel() {
        dispose();
    }

    private void ok() {
        if (useDefaultKeypairRadioButton.isSelected()) {
            assertion.setUsesDefaultKeyStore(true);
        } else {
            assertion.setUsesDefaultKeyStore(false);
            ComboEntry comboentry = (ComboEntry)aliasCombo.getSelectedItem();
            assertion.setKeyAlias(comboentry.alias);
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
