package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.Locator;
import com.l7tech.console.action.Actions;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.security.FormAuthorizationPreparer;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Group;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.security.cert.CertificateExpiredException;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;


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
    private JButton closeButton;
    private TrustedCertsTable trustedCertTable = null;
    private JScrollPane certTableScrollPane;
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private FormAuthorizationPreparer securityFormAuthorizationPreparer;

    /**
     * Constructor
     *
     * @param owner The parent component
     */
    public CertManagerWindow(Frame owner) {
        super(owner, resources.getString("dialog.title"), true);

        final SecurityProvider provider = (SecurityProvider)Locator.getDefault().lookup(SecurityProvider.class);
        if (provider == null) {
            throw new IllegalStateException("Could not instantiate security provider");
        }
        securityFormAuthorizationPreparer = new FormAuthorizationPreparer(provider, new String[]{Group.ADMIN_GROUP_NAME});

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
            /**
             * Called whenever the value of the selection changes.
             *
             * @param e the event that characterizes the change.
             */
            public void valueChanged(ListSelectionEvent e) {

                enableOrDisableButtons();
            }
        });

        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                hide();
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
                CertPropertiesWindow cpw = new CertPropertiesWindow(CertManagerWindow.this, updatedTrustedCert, true);

                cpw.show();
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
        Actions.setEscKeyStrokeDisposes(this);

    }

    /**
     * Load the certs from the database
     */
    private void loadTrustedCerts() {

        java.util.List certList = null;
        try {
            certList = getTrustedCertAdmin().findAllCerts();

            Vector certs = new Vector();
            for (int i = 0; i < certList.size(); i++) {
                Object o = (Object)certList.get(i);
                certs.add(o);
            }

            trustedCertTable.getTableSorter().setData(certs);
            trustedCertTable.getTableSorter().getRealModel().setRowCount(certs.size());
            trustedCertTable.getTableSorter().fireTableDataChanged();


        } catch (RemoteException re) {
            JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.remote.exception"),
              resources.getString("load.error.title"),
              JOptionPane.ERROR_MESSAGE);
        } catch (FindException e) {
            JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.find.error"),
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
        removeButton.setEnabled(removeEnabled);
        propertiesButton.setEnabled(propsEnabled);
        applyFormSecurity();
    }


    private void applyFormSecurity() {
        // list components that are subject to security (they require the full admin role)
        securityFormAuthorizationPreparer.prepare(new Component[]{
            removeButton,
            addButton
        });
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

            Object o = w.getCollectedInformation();

            if (o instanceof TrustedCert) {

                final TrustedCert tc = (TrustedCert)o;

                if (tc != null) {

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {

                            try {
                                getTrustedCertAdmin().saveCert(tc);

                                // reload all certs from server
                                loadTrustedCerts();

                            } catch (SaveException e) {
                                if (embeddedCertificateExpiredException(e) != null) {
                                    JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.expired.error"),
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
                                if (embeddedCertificateExpiredException(e) != null) {
                                    JOptionPane.showMessageDialog(CertManagerWindow.this, resources.getString("cert.expired.error"),
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
        }

    };

    private Throwable embeddedCertificateExpiredException(Exception e) {
        if (e instanceof CertificateExpiredException) {
            return e;
        }
        Throwable t = e.getCause();
        while (t != null) {
            t = t.getCause();
            if (t instanceof CertificateExpiredException) {
                return t;
            }
        }
        return null;
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  - The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca =
          (TrustedCertAdmin)Locator.
          getDefault().lookup(TrustedCertAdmin.class);
        if (tca == null) {
            throw new RuntimeException("Could not find registered " + TrustedCertAdmin.class);
        }

        return tca;
    }
}
