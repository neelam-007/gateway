package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import static com.l7tech.common.security.rbac.EntityType.TRUSTED_CERT;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * This class is the main window of the trusted certificate manager
 * <p/>
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p/>
 * $Id$
 */
public class CertManagerWindow extends JDialog {

    private JPanel mainPanel;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton gatewayPropertiesButton;
    private JButton closeButton;
    private TrustedCertsTable trustedCertTable = null;
    private TrustedCertsTable gatewayCertTable = null;
    private JScrollPane certTableScrollPane;
    private JScrollPane gatewayCertTableScrollPane;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertManagerWindow.class.getName());
    private final PermissionFlags flags;

    /**
     * Constructor
     *
     * @param owner The parent component
     */
    public CertManagerWindow(Frame owner) throws RemoteException {
        super(owner, resources.getString("dialog.title"), true);

        flags = PermissionFlags.get(TRUSTED_CERT);

        final SecurityProvider provider = Registry.getDefault().getSecurityProvider();
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }

        initialize();
        loadTrustedCerts();
        loadCerts();
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
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableButtons();
            }
        });

        if (gatewayCertTable == null) {
            gatewayCertTable = new TrustedCertsTable();
        }
        gatewayCertTableScrollPane.setViewportView(gatewayCertTable);
        gatewayCertTableScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        gatewayCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_ISSUER_NAME_COLUMN_INDEX);
        gatewayCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);

        gatewayCertTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                enableOrDisableGatewayButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setVisible(false);
                dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        CertImportMethodsPanel sp = new CertImportMethodsPanel(new CertDetailsPanel(new CertUsagePanel(null)), true);

                        JFrame f = TopComponents.getInstance().getMainWindow();
                        Wizard w = new AddCertificateWizard(f, sp);
                        w.addWizardListener(wizardListener);

                        // register itself to listen to the addEvent
                        //addEntityListener(listener);

                        w.pack();
                        w.setSize(780, 560);
                        Utilities.centerOnScreen(w);
                        w.setVisible(true);

                    }
                });
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int sr = trustedCertTable.getSelectedRow();
                TrustedCert tc = (TrustedCert)trustedCertTable.getTableSorter().getData(sr);
                TrustedCert updatedTrustedCert = null;

                // retrieve the latest version
                try {
                    updatedTrustedCert = getTrustedCertAdmin().findCertByPrimaryKey(tc.getOid());
                } catch (FindException e) {
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.find.error"),
                                                  resources.getString("view.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (RemoteException e) {
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.remote.exception"),
                                                  resources.getString("view.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                }

                trustedCertTable.getTableSorter().updateData(sr, updatedTrustedCert);
                CertPropertiesWindow cpw = new CertPropertiesWindow(CertManagerWindow.this, updatedTrustedCert, flags.canUpdateSome()); // TODO do a permission check for *this* cert

                cpw.setVisible(true);
            }
        });

        gatewayPropertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int sr = gatewayCertTable.getSelectedRow();
                TrustedCert certHolder = (TrustedCert) gatewayCertTable.getTableSorter().getData(sr);
                CertPropertiesWindow cpw = new CertPropertiesWindow(CertManagerWindow.this, certHolder, false, false);
                cpw.setVisible(true);
            }
        });

        removeButton.addActionListener(new ActionListener() {
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
                    } catch (RemoteException e) {
                        JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.remote.exception"),
                                                      resources.getString("save.error.title"),
                                                      JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        pack();
        enableOrDisableButtons();
        enableOrDisableGatewayButtons();
        Utilities.setEscKeyStrokeDisposes(this);
    }

    /**
     * Load the certs from the database
     */
    private void loadTrustedCerts() throws RemoteException {

        java.util.List certList;
        try {
            certList = getTrustedCertAdmin().findAllCerts();

            Vector<TrustedCert> certs = new Vector<TrustedCert>();
            for (Object o : certList) {
                TrustedCert cert = (TrustedCert) o;
                certs.add(cert);
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
        }
    }

    /**
     * Load the certs from the SSG
     */
    private void loadCerts() throws RemoteException {

        Vector<TrustedCert> certList = new Vector<TrustedCert>();
        try {
            TrustedCert rootCertHolder = new TrustedCert();
            rootCertHolder.setName("CA");
            rootCertHolder.setCertificate(getTrustedCertAdmin().getSSGRootCert());
            certList.add(rootCertHolder);

            TrustedCert sslCertHolder = new TrustedCert();
            sslCertHolder.setName("SSL");
            sslCertHolder.setCertificate(getTrustedCertAdmin().getSSGSslCert());
            certList.add(sslCertHolder);

            gatewayCertTable.getTableSorter().setData(certList);
            gatewayCertTable.getTableSorter().getRealModel().setRowCount(certList.size());
            gatewayCertTable.getTableSorter().fireTableDataChanged();
        } catch (Exception e) {
            String msg = resources.getString("cert.find.error");
            logger.log(Level.WARNING, msg, e);
            JOptionPane.showMessageDialog(CertManagerWindow.this, msg,
                                          resources.getString("load.error.title"),
                                          JOptionPane.ERROR_MESSAGE);
        }
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
        removeButton.setEnabled(flags.canDeleteSome() && removeEnabled);
        propertiesButton.setEnabled(propsEnabled); // Child dialog should be read-only if !canUpdateAny
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableGatewayButtons() {
        boolean propsEnabled = false;
        int row = gatewayCertTable.getSelectedRow();
        if (row >= 0) {
            propsEnabled = true;
        }
        gatewayPropertiesButton.setEnabled(propsEnabled);
    }

    /**
     * The callback for saving the new cert to the database
     */
    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the wizard has finished.
         *
         * @param we the event describing the wizard finish
         */
        public void wizardFinished(WizardEvent we) {

            // update the provider
            Wizard w = (Wizard)we.getSource();

            Object o = w.getWizardInput();

            if (o instanceof TrustedCert) {

                final TrustedCert tc = (TrustedCert)o;
                if (tc.isTrustedForSsl() || tc.isTrustedForSigningServerCerts()) {
                    tc.setVerifyHostname(true);
                }

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {

                        try {
                            getTrustedCertAdmin().saveCert(tc);

                            // reload all certs from server
                            loadTrustedCerts();

                        } catch (SaveException e) {
                            if (ExceptionUtils.causedBy(e, CertificateExpiredException.class)) {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.expired.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            } else if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.duplicate.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            } else if (ExceptionUtils.causedBy(e, CertificateNotYetValidException.class)) {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.notyetvalid.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.save.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            }
                        } catch (RemoteException e) {
                            JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.remote.exception"),
                                                          resources.getString("save.error.title"),
                                                          JOptionPane.ERROR_MESSAGE);
                        } catch (VersionException e) {
                            JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.version.error"),
                                                          resources.getString("save.error.title"),
                                                          JOptionPane.ERROR_MESSAGE);
                        } catch (UpdateException e) {
                            if (ExceptionUtils.causedBy(e, CertificateExpiredException.class)) {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.expired.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            } else if (ExceptionUtils.causedBy(e, DuplicateObjectException.class)) {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.duplicate.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.update.error"),
                                                              resources.getString("save.error.title"),
                                                              JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    }
                });
            }
        }

    };

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
