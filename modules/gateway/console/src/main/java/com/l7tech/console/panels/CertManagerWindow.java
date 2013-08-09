/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.objectmodel.EntityType.TRUSTED_CERT;

/**
 * This class is the main window of the trusted certificate manager
 */
public class CertManagerWindow extends JDialog {

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertManagerWindow.class.getName());

    private JPanel mainPanel;
    private JButton addButton;
    private JButton importButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton closeButton;
    private JScrollPane certTableScrollPane;
    private JButton certificateValidationButton;
    private JLabel expiredCertCautionLabel;

    private TrustedCertsTable trustedCertTable = null;
    private Collection<RevocationCheckPolicy> revocationCheckPolicies;
    private final PermissionFlags flags;
    private boolean foundExpiredCert;

    /**
     * Constructor
     *
     * @param owner The parent component
     */
    public CertManagerWindow( final Window owner ) {
        super(owner, resources.getString("dialog.title"), DEFAULT_MODALITY_TYPE);

        flags = PermissionFlags.get(TRUSTED_CERT);

        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        initialize();
        loadTrustedCerts();
    }


    /**
     * Initialization of the cert manager window
     */
    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certTableScrollPane.setViewportView(trustedCertTable);
        certTableScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_ISSUER_NAME_COLUMN_INDEX);

        trustedCertTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                dispose();
            }
        });

        // Initialize expiredCertCautionLabel
        expiredCertCautionLabel.setText("Caution! Some certificate(s) have expired.");
        expiredCertCautionLabel.setBackground(new Color(0xFF, 0xFF, 0xe1));
        expiredCertCautionLabel.setOpaque(true);
        expiredCertCautionLabel.setVisible(false);

        certificateValidationButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                showCertificateValidation();
            }
        });

        addButton.addActionListener( new NewTrustedCertificateAction(new CertListener(){
            @Override
            public void certSelected(CertEvent ce) {
                // reload all certs from server
                loadTrustedCerts();
            }
        }, "Add"));

        importButton.addActionListener( new ImportTrustedCertificateAction(new CertListener(){
            @Override
            public void certSelected(CertEvent ce) {
                // reload all certs from server
                loadTrustedCerts();
            }
        }, "Import"));

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int sr = trustedCertTable.getSelectedRow();
                TrustedCert tc = (TrustedCert)trustedCertTable.getTableSorter().getData(sr);

                // retrieve the latest version
                try {
                    TrustedCert updatedTrustedCert = getTrustedCertAdmin().findCertByPrimaryKey(tc.getGoid());
                    if (updatedTrustedCert == null) {
                        JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.deleted.error"),
                                                      resources.getString("view.error.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                        loadTrustedCerts();
                        return;
                    }

                    trustedCertTable.getTableSorter().updateData(sr, updatedTrustedCert);
                    CertPropertiesWindow cpw =
                            new CertPropertiesWindow(
                                    CertManagerWindow.this,
                                    updatedTrustedCert,
                                    flags.canUpdateSome(), // TODO do a permission check for *this* cert
                                    getRevocationCheckPolicies());

                    DialogDisplayer.display(cpw);
                } catch (FindException e) {
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.find.error"),
                                                  resources.getString("view.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        Utilities.setDoubleClickAction(trustedCertTable, propertiesButton);

        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int[] srs = trustedCertTable.getSelectedRows();
                Goid[] oids = new Goid[srs.length];

                String message;
                if ( srs.length == 1 ) {
                    String certName = (String)trustedCertTable.getValueAt(srs[0], TrustedCertTableSorter.CERT_TABLE_CERT_NAME_COLUMN_INDEX);
                    TrustedCert tc = (TrustedCert)trustedCertTable.getTableSorter().getData(srs[0]);
                    message = "<html>Are you sure you want to remove the certificate:  " +
                                                          certName + "?<br>" +
                                                          "<center>This action cannot be undone." +
                                                          "</center></html>";
                    oids[0] = tc.getGoid();
                } else {
                    message = "<html>Are you sure you want to remove " +srs.length+ " certificates?<br>" +
                                                          "<center>This action cannot be undone." +
                                                          "</center></html>";
                    for ( int i = 0; i < srs.length; i++ ) {
                        TrustedCert tc = (TrustedCert)trustedCertTable.getTableSorter().getData(srs[i]);
                        oids[i] = tc.getGoid();
                    }
                }

                Object[] options = {"Remove", "Cancel"};
                int result = JOptionPane.showOptionDialog(CertManagerWindow.this,
                                                          message,
                                                          "Remove the certificate(s)?",
                                                          0, JOptionPane.WARNING_MESSAGE,
                                                          null, options, options[1]);
                if (result == 0) {
                    try {
                        for ( Goid oid : oids ) {
                            getTrustedCertAdmin().deleteCert(oid);
                        }

                        // reload all certs from server
                        loadTrustedCerts();

                    } catch (FindException e) {
                        JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.find.error"),
                                                      resources.getString("save.error.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch (DeleteException e) {
                        JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.delete.error"),
                                                      resources.getString("save.error.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    } catch(ConstraintViolationException cve) {
                        JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.constraint.error"),
                                                      resources.getString("delete.error.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        pack();
        enableOrDisableButtons();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    /**
     * Load the certs from the database
     */
    public void loadTrustedCerts() {
        java.util.List<TrustedCert> certList;
        try {
            certList = getTrustedCertAdmin().findAllCerts();

            java.util.List<TrustedCert> certs = new ArrayList<TrustedCert>();
            foundExpiredCert = false;
            for (Object o : certList) {
                TrustedCert cert = (TrustedCert) o;
                certs.add(cert);
                if (!foundExpiredCert) foundExpiredCert = cert.isExpiredCert();
            }

            trustedCertTable.getTableSorter().setData(certs);
            trustedCertTable.getTableSorter().getRealModel().setRowCount(certs.size());
            trustedCertTable.getTableSorter().fireTableDataChanged();
        } catch (FindException e) {
            String msg = resources.getString("cert.find.error");
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(CertManagerWindow.this, msg,
                                          resources.getString("load.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
        } catch (CertificateException e) {
            String msg = "the certificate cannot be deserialized";
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(CertManagerWindow.this, msg,
                                          resources.getString("load.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
        expiredCertCautionLabel.setVisible(foundExpiredCert);
    }

    private Collection<RevocationCheckPolicy> getRevocationCheckPolicies() throws FindException {
         Collection<RevocationCheckPolicy> policies = revocationCheckPolicies;

        if ( revocationCheckPolicies == null ) {
            policies = loadRevocationCheckPolicies();
            revocationCheckPolicies = policies;
        }

        return policies;
    }

    private Collection<RevocationCheckPolicy> loadRevocationCheckPolicies() throws FindException {
        return getTrustedCertAdmin().findAllRevocationCheckPolicies();
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean propsEnabled = false;
        boolean removeEnabled = false;
        boolean multiSelect = trustedCertTable.getSelectedRowCount() > 1;
        int row = trustedCertTable.getSelectedRow();
        if (row >= 0) {
            removeEnabled = true;
            propsEnabled = true;
        }
        addButton.setEnabled(flags.canCreateSome());
        importButton.setEnabled(flags.canCreateSome());
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        propertiesButton.setEnabled( !multiSelect && propsEnabled ); // Child dialog should be read-only if !canUpdateAny
    }

    private void showCertificateValidation() {
        DialogDisplayer.display(new ManageCertificateValidationDialog(this), new Runnable() {
            @Override
            public void run() {
                // invalidate cache policies in case there are any new ones                
                revocationCheckPolicies = null;
            }
        });
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
