/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.GuiPasswordCallbackHandler;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import static com.l7tech.objectmodel.EntityType.TRUSTED_CERT;
import com.l7tech.console.event.CertListener;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.*;
import com.l7tech.util.Functions;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.CertUtils;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.lang.reflect.InvocationTargetException;

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
    public CertManagerWindow(Frame owner) {
        super(owner, resources.getString("dialog.title"), true);

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

        importButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent e) {
                importTrustedCerts();
            }
        } );

        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                int sr = trustedCertTable.getSelectedRow();
                TrustedCert tc = (TrustedCert)trustedCertTable.getTableSorter().getData(sr);

                // retrieve the latest version
                try {
                    TrustedCert updatedTrustedCert = getTrustedCertAdmin().findCertByPrimaryKey(tc.getOid());
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
                int sr = trustedCertTable.getSelectedRow();

                String certName = (String)trustedCertTable.getValueAt(sr, TrustedCertTableSorter.CERT_TABLE_CERT_NAME_COLUMN_INDEX);
                TrustedCert tc = (TrustedCert)trustedCertTable.getTableSorter().getData(sr);

                Object[] options = {"Remove", "Cancel"};
                int result = JOptionPane.showOptionDialog(null,
                                                          "<html>Are you sure you want to remove the certificate:  " +
                                                          certName + "?<br>" +
                                                          "<center>This action cannot be undone." +
                                                          "</center></html>",
                                                          "Remove the certificate?",
                                                          0, JOptionPane.WARNING_MESSAGE,
                                                          null, options, options[1]);
                if (result == 0) {
                    try {
                        getTrustedCertAdmin().deleteCert(tc.getOid());

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
     * Import certificates from a PKCS#12 keystore.
     */
    private void importTrustedCerts() {
        final java.util.List<X509Certificate[]> certificateChains = new ArrayList<X509Certificate[]>();
        GuiCertUtil.importCertificates( this, false, new GuiPasswordCallbackHandler(this),  new Functions.Unary<Boolean,GuiCertUtil.ImportedData>(){
            @Override
            public Boolean call( final GuiCertUtil.ImportedData importedData ) {
                certificateChains.add( importedData.getCertificateChain() );
                return true;
            }
        } );

        // If there are no certificates then a warning should already have been displayed
        if ( certificateChains.size() > 0 ) {
            final CertificateImportDialog dialog = new CertificateImportDialog( this, certificateChains );
            DialogDisplayer.display( dialog, new Runnable() {
                @Override
                public void run() {
                    if ( dialog.wasOk() ) {
                        final JProgressBar progressBar = new JProgressBar();
                        progressBar.setIndeterminate(true);
                        final CancelableOperationDialog cancelDialog =
                                new CancelableOperationDialog( CertManagerWindow.this, 
                                                               resources.getString("importing.title"),
                                                               resources.getString("importing.label"),
                                                               progressBar);
                        cancelDialog.pack();
                        cancelDialog.setModal(true);
                        Utilities.centerOnParentWindow(cancelDialog);

                        final Collection<String> duplicateCerts = new LinkedHashSet<String>();
                        final Collection<String> errorCerts = new LinkedHashSet<String>();
                        Callable<Boolean> callable = new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                final boolean importAsTrustAnchor = dialog.isImportAsTrustAnchor();
                                final boolean importChain = dialog.isImportChain();
                                final Collection<X509Certificate[]> certificates = dialog.getCertificateChains();
                                importCertificates(importAsTrustAnchor, importChain, certificates, duplicateCerts, errorCerts );
                                return Boolean.TRUE;
                            }
                        };

                        try {
                            Utilities.doWithDelayedCancelDialog( callable, cancelDialog, 500L );
                        } catch (InterruptedException e) {
                            logger.finer("Import operation interrupted (cancelled)");
                        } catch (InvocationTargetException e) {
                            // we have handled any expected exceptions elsewhere
                            if ( cancelDialog.wasCancelled() ) {
                                logger.log( Level.WARNING, "Error during (cancelled) certificate import.", e );
                            } else {
                                throw ExceptionUtils.wrap( e.getTargetException() );
                            }
                        }

                        if ( !cancelDialog.wasCancelled() ) {
                            handleImportResult( duplicateCerts, errorCerts );                            
                        }
                    }
                }
            });
        }
    }

    private void importCertificates( final boolean importAsTrustAnchor,
                                     final boolean importChain,
                                     final Collection<X509Certificate[]> certificates,
                                     final Collection<String> duplicateCerts,
                                     final Collection<String> errorCerts ) {
        for ( X509Certificate[] certificateChain : certificates ) {
            if ( Thread.interrupted() ) break;

            if ( importChain ) {
                List<X509Certificate> reverseCertificateChain = Arrays.asList(certificateChain);
                Collections.reverse( reverseCertificateChain );
                boolean first = true;
                Collection<String> targetCertDupe = Collections.emptyList(); // we only care if the last is a dupe
                for ( X509Certificate certificate : reverseCertificateChain ) {
                    targetCertDupe = new ArrayList<String>();
                    boolean isTrustAnchor = false;
                    if ( first ) {
                        isTrustAnchor =  importAsTrustAnchor;
                        first = false;
                    }
                    saveCert( certificate, isTrustAnchor, targetCertDupe, errorCerts );
                }
                duplicateCerts.addAll( targetCertDupe );
            } else {
                saveCert( certificateChain[0], importAsTrustAnchor, duplicateCerts, errorCerts );
            }
        }
    }

    private void handleImportResult( final Collection<String> duplicateCerts,
                                     final Collection<String> errorCerts ) {
        if ( !duplicateCerts.isEmpty() || !errorCerts.isEmpty() ) {
            StringBuilder errorMessage = new StringBuilder();

            errorMessage.append( resources.getString("error.header") );
            errorMessage.append( "\n\n  " );
            if ( !duplicateCerts.isEmpty() ) {
                errorMessage.append( resources.getString("error.duplicates") );
                for ( String certDn : duplicateCerts ) {
                    errorMessage.append( "\n" );
                    errorMessage.append( "    " );
                    errorMessage.append( certDn );
                }
                errorMessage.append( "\n\n  " );
            }
            if ( !errorCerts.isEmpty() ) {
                errorMessage.append( resources.getString("error.failures") );
                for ( String certDn : errorCerts ) {
                    errorMessage.append( "\n" );
                    errorMessage.append( "    " );
                    errorMessage.append( certDn );
                }
                errorMessage.append( "\n" );
            }

            final JTextArea textArea = new JTextArea();
            textArea.setEditable( false );
            textArea.setText( errorMessage.toString() );
            final JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(400, 200));
            DialogDisplayer.showMessageDialog(this, scrollPane, "Certificate Import Failures", JOptionPane.WARNING_MESSAGE, new Runnable(){
                @Override
                public void run() {
                    // reload all certs from server
                    loadTrustedCerts();
                }
            });
        } else {
            // reload all certs from server
            loadTrustedCerts();
        }
    }

    private void saveCert( final X509Certificate certificate,
                           final boolean isTrustAnchor,
                           final Collection<String> duplicateCerts,
                           final Collection<String> errorCerts ) {

        TrustedCert cert = new TrustedCert();
        String name = CertUtils.extractFirstCommonNameFromCertificate( certificate );
        if ( name == null ) {
            name = certificate.getSubjectX500Principal().getName().trim();   
        } else {
            name = name.trim();
        }
        cert.setName( name );
        cert.setCertificate( certificate );
        cert.setTrustAnchor( isTrustAnchor );

        try {
            getTrustedCertAdmin().saveCert( cert );
        } catch ( DuplicateObjectException doe ) {
            duplicateCerts.add( certificate.getSubjectX500Principal().getName() );
        } catch ( ObjectModelException e) {
            errorCerts.add( certificate.getSubjectX500Principal().getName() );
            logger.log( Level.WARNING, "Certificate import failed", e);
        } catch ( VersionException e) {
            // doesn't happen for new certs
            errorCerts.add( certificate.getSubjectX500Principal().getName() );
            logger.log( Level.WARNING, "Certificate import failed", e);
        }
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
        int row = trustedCertTable.getSelectedRow();
        if (row >= 0) {
            removeEnabled = true;
            propsEnabled = true;
        }
        addButton.setEnabled(flags.canCreateSome());
        importButton.setEnabled(flags.canCreateSome());
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        propertiesButton.setEnabled(propsEnabled); // Child dialog should be read-only if !canUpdateAny
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
