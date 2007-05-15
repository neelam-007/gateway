package com.l7tech.console.panels;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.util.Registry;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.server.security.keystore.SsgKeyEntry;

import javax.swing.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.rmi.RemoteException;

/**
 * Window for managing private key entries (certificate chains with private keys) in the Gateway.
 */
public class PrivateKeyManagerWindow extends JDialog {
    protected static final Logger logger = Logger.getLogger(PrivateKeyManagerWindow.class.getName());

    private JPanel mainPanel;
    private JScrollPane certTableScrollPane;
    private JButton propertiesButton;
    private JButton closeButton;
    private JButton createButton;
    private JButton importButton;
    private JButton removeButton;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private final PermissionFlags flags;
    private TrustedCertsTable certTable = null;


    public PrivateKeyManagerWindow(Frame owner) throws RemoteException {
        super(owner, resources.getString("keydialog.title"), true);

        flags = PermissionFlags.get(EntityType.SSG_KEY_ENTRY);

        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        initialize();
        loadCerts();
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        if (certTable == null) {
            certTable = new TrustedCertsTable();
        }
        certTableScrollPane.setViewportView(certTable);
        certTableScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        certTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_ISSUER_NAME_COLUMN_INDEX);
        certTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);

        certTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableGatewayButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int sr = certTable.getSelectedRow();
                TrustedCert certHolder = (TrustedCert) certTable.getTableSorter().getData(sr);
                CertPropertiesWindow cpw = new CertPropertiesWindow(PrivateKeyManagerWindow.this, certHolder, false, false);
                DialogDisplayer.display(cpw);
            }
        });

        // TODO other buttons
        createButton.setEnabled(false);
        importButton.setEnabled(false);
        removeButton.setEnabled(false);


        pack();
        enableOrDisableGatewayButtons();
    }

    /**
     * Load the certs from the SSG
     */
    private void loadCerts() throws RemoteException {
        java.util.List<TrustedCert> certList = new ArrayList<TrustedCert>();
        try {
            List<TrustedCertAdmin.KeystoreInfo> keystores = getTrustedCertAdmin().findAllKeystores();

            for (TrustedCertAdmin.KeystoreInfo keystore : keystores) {
                List<SsgKeyEntry> entries = getTrustedCertAdmin().findAllKeys(keystore.id);
                for (SsgKeyEntry entry : entries) {
                    // TODO create new table model and sorter with fields appropriate for private keys
                    TrustedCert holder = new TrustedCert();
                    holder.setName(entry.getAlias());
                    holder.setCertificate(entry.getCertificateChain()[0]);
                    certList.add(holder);
                }
            }

            certTable.getTableSorter().setData(certList);
            certTable.getTableSorter().getRealModel().setRowCount(certList.size());
            certTable.getTableSorter().fireTableDataChanged();
        } catch (Exception e) {
            String msg = resources.getString("cert.find.error");
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(PrivateKeyManagerWindow.this, msg,
                                          resources.getString("load.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableGatewayButtons() {
        boolean propsEnabled = false;
        int row = certTable.getSelectedRow();
        if (row >= 0) {
            propsEnabled = true;
        }
        propertiesButton.setEnabled(propsEnabled);
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
}
