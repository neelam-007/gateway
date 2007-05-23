package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.server.security.keystore.SsgKeyEntry;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.text.MessageFormat;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Window for managing private key entries (certificate chains with private keys) in the Gateway.
 */
public class PrivateKeyManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(PrivateKeyManagerWindow.class.getName());

    private JPanel mainPanel;
    private JScrollPane keyTableScrollPane;
    private JButton propertiesButton;
    private JButton closeButton;
    private JButton createButton;
    private JButton importButton;
    private JButton removeButton;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private PermissionFlags flags;
    private TrustedCertAdmin.KeystoreInfo mutableKeystore;
    private KeyTable keyTable = null;


    public PrivateKeyManagerWindow(JDialog owner) throws RemoteException {
        super(owner, resources.getString("keydialog.title"), true);
        initialize();
    }

    public PrivateKeyManagerWindow(Frame owner) throws RemoteException {
        super(owner, resources.getString("keydialog.title"), true);
        initialize();
    }

    private void initialize() throws RemoteException {
        flags = PermissionFlags.get(EntityType.SSG_KEY_ENTRY);

        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        keyTable = new KeyTable();
        keyTableScrollPane.setViewportView(keyTable);
        keyTableScrollPane.getViewport().setBackground(Color.white);

        keyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                doProperties();
            }
        });

        createButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doNewPrivateKey();
            }
        });

        importButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doImport();
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                doRemove();
            }
        });


        pack();
        enableOrDisableButtons();

        loadPrivateKeys();
        mutableKeystore = findMutableKeystore();
        if (mutableKeystore == null) {
            createButton.setEnabled(false);
            importButton.setEnabled(false);
            removeButton.setEnabled(false);
        }
    }

    private void doRemove() {
        final KeyTableRow certHolder = getSelectedObject();
        if (certHolder == null)
            return;

        final TrustedCertAdmin.KeystoreInfo keystore = certHolder.getKeystore();
        if (keystore.readonly) {
            JOptionPane.showMessageDialog(this, "This keystore is read-only.", "Unable to Remove Key", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String cancel = "Cancel";
        DialogDisplayer.showOptionDialog(
                this,
                "Really delete private key " + certHolder.getAlias() + " (" + certHolder.getKeyEntry().getSubjectDN() + ")?\n\n" +
                "This will irrevocably destory this key, and cannot be undone.",
                "Confirm deletion",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                new Object[] { "Destroy Private Key", cancel},
                cancel,
                new DialogDisplayer.OptionListener() {
                    public void reportResult(int option) {
                        if (option != 0)
                            return;

                        String alias = certHolder.getAlias();
                        try {
                            getTrustedCertAdmin().deleteKey(keystore.id, alias);
                        } catch (IOException e) {
                            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
                        } catch (CertificateException e) {
                            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
                        } catch (DeleteException e) {
                            showErrorMessage("Deletion Failed", "Unable to delete key: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
        );
    }

    private void showErrorMessage(String title, String msg, Throwable e) {
        logger.log(Level.WARNING, msg, e);
        DialogDisplayer.showMessageDialog(this, msg, title, JOptionPane.ERROR_MESSAGE, null);
    }

    private void doImport() {
        // TODO import from PKCS#12 file
        JOptionPane.showMessageDialog(this, "Import from local PKCS#12 file goes here");
    }

    private TrustedCert asTrustedCert(KeyTableRow row) {
        TrustedCert certHolder = new TrustedCert();
        certHolder.setName(row.getAlias());
        certHolder.setSubjectDn(row.getSubjectDN());
        try {
            certHolder.setCertificate(row.getCertificate());
        } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
        }
        return certHolder;
    }

    private void doProperties() {
        TrustedCert certHolder = asTrustedCert(getSelectedObject());
        // TODO refactor cert properties window to show key info while reusing much code
        CertPropertiesWindow cpw = new CertPropertiesWindow(this, certHolder, false, false);
        DialogDisplayer.display(cpw);
    }

    /** @return the currently selected row or null */
    private KeyTableRow getSelectedObject() {
        int sr = keyTable.getSelectedRow();
        if (sr < 0)
            return null;
        return keyTable.getRowAt(sr);
    }

    private void doNewPrivateKey() {
        final NewPrivateKeyDialog dlg = new NewPrivateKeyDialog(this, mutableKeystore);
        dlg.setModal(true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isConfirmed()) {
                    try {
                        loadPrivateKeys();
                    } catch (RemoteException e1) {
                        showErrorMessage("Refresh Failed", "Unable to reload private keys: " + ExceptionUtils.getMessage(e1), e1);
                    }
                }
            }
        });
    }

    /*
     * Load the certs from the SSG
     */
    private void loadPrivateKeys() throws RemoteException {
        try {
            java.util.List<KeyTableRow> keyList = new ArrayList<KeyTableRow>();
            for (TrustedCertAdmin.KeystoreInfo keystore : getTrustedCertAdmin().findAllKeystores())
                for (SsgKeyEntry entry : getTrustedCertAdmin().findAllKeys(keystore.id))
                    keyList.add(new KeyTableRow(keystore, entry));

            keyTable.setData(keyList);

        } catch (Exception e) {
            String msg = resources.getString("cert.find.error");
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(PrivateKeyManagerWindow.this, msg,
                                          resources.getString("load.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /** @return the first keystore on this Gateway that isn't read-only, or null */
    private TrustedCertAdmin.KeystoreInfo findMutableKeystore() {
        TrustedCertAdmin.KeystoreInfo keystore = null;

        // TODO for now, always create in the first mutable keystore in the list
        // Someday, when there can be more than one mutable keystore, we may support choosing the keystore to create in
        final List<TrustedCertAdmin.KeystoreInfo> keystores;
        try {
            keystores = getTrustedCertAdmin().findAllKeystores();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        } catch (KeyStoreException e) {
            throw new RuntimeException(e);
        }
        for (TrustedCertAdmin.KeystoreInfo k : keystores) {
            if (!k.readonly) {
                keystore = k;
                break;
            }
        }

        return keystore;
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        KeyTableRow row = getSelectedObject();
        propertiesButton.setEnabled(row != null);
        removeButton.setEnabled(row != null && !row.getKeystore().readonly);
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        return Registry.getDefault().getTrustedCertManager();
    }

    /** Represents a row in the Manage Private Keys table. */
    private static class KeyTableRow {
        private final TrustedCertAdmin.KeystoreInfo keystoreInfo;
        private final SsgKeyEntry keyEntry;
        private String keyType = null;
        private String expiry = null;

        public KeyTableRow(TrustedCertAdmin.KeystoreInfo keystoreInfo, SsgKeyEntry keyEntry) {
            this.keystoreInfo = keystoreInfo;
            this.keyEntry = keyEntry;
        }

        public TrustedCertAdmin.KeystoreInfo getKeystore() {
            return keystoreInfo;
        }

        public SsgKeyEntry getKeyEntry() {
            return keyEntry;
        }

        public String getAlias() {
            return keyEntry.getAlias();
        }

        public String getSubjectDN() {
            return keyEntry.getSubjectDN();
        }

        public X509Certificate getCertificate() {
            return keyEntry.getCertificate();
        }

        public Object getKeyType() {
            if (keyType == null) {
                PublicKey publicKey = getCertificate().getPublicKey();
                String alg = publicKey.getAlgorithm();
                if (alg == null) alg = "N/A"; // can't happen
                if (publicKey instanceof RSAPublicKey) {
                    RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
                    String modulus = rsaKey.getModulus().toString(16);
                    alg = MessageFormat.format("{0} {1} bits", alg, modulus.length() * 4);
                }
                keyType = alg;
            }
            return keyType;
        }

        public Object getExpiry() {
            if (expiry == null)
                expiry = DateFormat.getDateInstance().format(getCertificate().getNotAfter());
            return expiry;
        }
    }

    private static class KeyTable extends JTable {
        private final KeyTableModel model = new KeyTableModel();

        public KeyTable() {
            setModel(model);
            getTableHeader().setReorderingAllowed(false);
            getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            TableColumnModel cols = getColumnModel();
            int numCols = model.getColumnCount();
            for (int i = 0; i < numCols; ++i) {
                final TableColumn col = cols.getColumn(i);
                col.setMinWidth(model.getColumnMinWidth(i));
                col.setPreferredWidth(model.getColumnPrefWidth(i));
                col.setMaxWidth(model.getColumnMaxWidth(i));
            }
        }

        public KeyTableRow getRowAt(int row) {
            return model.getRowAt(row);
        }

        public void setData(List<KeyTableRow> rows) {
            model.setData(rows);
        }

        private static class KeyTableModel extends AbstractTableModel {
            private static abstract class Col {
                final String name;
                final int minWidth;
                final int prefWidth;
                final int maxWidth;

                protected Col(String name, int minWidth, int prefWidth, int maxWidth) {
                    this.name = name;
                    this.minWidth = minWidth;
                    this.prefWidth = prefWidth;
                    this.maxWidth = maxWidth;
                }
                abstract Object getValueForRow(KeyTableRow row);
            }

            public static final Col[] columns = new Col[] {
                    new Col("Location", 60, 90, 90) {
                        Object getValueForRow(KeyTableRow row) {
                            return row.getKeystore().name;
                        }
                    },

                    new Col("Alias", 60, 90, 300) {
                        Object getValueForRow(KeyTableRow row) {
                            return row.getAlias();
                        }
                    },

                    new Col("Subject", 3, 100, 999999) {
                        Object getValueForRow(KeyTableRow row) {
                            return row.getSubjectDN();
                        }
                    },

                    new Col("Key Type", 3, 88, 88) {
                        Object getValueForRow(KeyTableRow row) {
                            return row.getKeyType();
                        }
                    },

                    new Col("Expiry", 3, 85, 85) {
                        Object getValueForRow(KeyTableRow row) {
                            return row.getExpiry();
                        }
                    }
            };

            private final List<KeyTableRow> rows = new ArrayList<KeyTableRow>();

            public KeyTableModel() {
            }

            public int getColumnMinWidth(int column) {
                return columns[column].minWidth;
            }

            public int getColumnPrefWidth(int column) {
                return columns[column].prefWidth;
            }

            public int getColumnMaxWidth(int column) {
                return columns[column].maxWidth;
            }

            public String getColumnName(int column) {
                return columns[column].name;
            }

            public void setData(List<KeyTableRow> rows) {
                this.rows.clear();
                this.rows.addAll(rows);
                fireTableDataChanged();
            }

            public int getRowCount() {
                return rows.size();
            }

            public int getColumnCount() {
                return columns.length;
            }

            public Object getValueAt(int rowIndex, int columnIndex) {
                return columns[columnIndex].getValueForRow(rows.get(rowIndex));
            }

            public KeyTableRow getRowAt(int rowIndex) {
                return rows.get(rowIndex);
            }
        }
    }
}
