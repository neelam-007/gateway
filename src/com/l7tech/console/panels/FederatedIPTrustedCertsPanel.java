/*
 * Copyright (C) 2004-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.rbac.*;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.console.action.SecureAction;
import com.l7tech.console.event.*;
import com.l7tech.console.table.TrustedCertTableSorter;
import com.l7tech.console.table.TrustedCertsTable;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.*;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.rmi.RemoteException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides the step panel for the Federated Identity Provider dialog.
 */
public class FederatedIPTrustedCertsPanel extends IdentityProviderStepPanel {
    private static final Logger logger = Logger.getLogger(FederatedIPTrustedCertsPanel.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog");

    private JPanel mainPanel;
    private JScrollPane certScrollPane;
    private JButton addButton;
    private JButton removeButton;
    private JButton propertiesButton;
    private JButton createCertButton;
    private boolean x509CertSelected = false;
    private boolean limitationsAccepted = true;

    private TrustedCertsTable trustedCertTable = null;
    private X509Certificate ssgcert = null;
    private Collection<FederatedIdentityProviderConfig> fedIdProvConfigs = new ArrayList<FederatedIdentityProviderConfig>();

    private final SecureAction createCertAction = new CreateCertAction();

    /**
     * Construstor
     *
     * @param next  The next step panel
     */
    public FederatedIPTrustedCertsPanel(WizardStepPanel next) {
        super(next);
        initComponents();
    }

    public FederatedIPTrustedCertsPanel(WizardStepPanel next, boolean readOnly) {
        super(next, readOnly);
        initComponents();
    }

    /**
     * Get the description of the step
     * @return String The description of the step
     */
    public String getDescription() {
        return resources.getString("certPanelDescription");
    }

    /**
     * @return the wizard step label
     */
    public String getStepLabel() {
        return resources.getString("certPanelStepLabel");
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        x509CertSelected = iProviderConfig.isX509Supported();

        long[] oids = iProviderConfig.getTrustedCertOids();
        List<TrustedCert> certs = new ArrayList<TrustedCert>();
        TrustedCert cert;

        for (long oid : oids) {
            try {
                cert = getTrustedCertAdmin().findCertByPrimaryKey(oid);
                if (cert != null) certs.add(cert);
            } catch (RemoteException re) {
                JOptionPane.showMessageDialog(this, resources.getString("cert.remote.exception"),
                        resources.getString("load.error.title"),
                        JOptionPane.ERROR_MESSAGE);
            } catch (FindException e) {
                JOptionPane.showMessageDialog(this, resources.getString("cert.find.error"),
                        resources.getString("load.error.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        if (certs.size() > 0) {
            trustedCertTable.getTableSorter().setData(certs);
            trustedCertTable.getTableSorter().getRealModel().setRowCount(certs.size());
            trustedCertTable.getTableSorter().fireTableDataChanged();
        }
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

    /**
     * Store the values of all fields on the panel to the wizard object which is a used for
     * keeping all the modified values. The wizard object will be used for providing the
     * updated values when updating the server.
     *
     * @param settings the object representing wizard panel state
     */
    public void storeSettings(Object settings) {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        List<TrustedCert> data = trustedCertTable.getTableSorter().getAllData();
        long[] oids = new long[data.size()];

        for (int i = 0; i < data.size(); i++) {
            TrustedCert tc = data.get(i);
            oids[i] = tc.getOid();
        }

        iProviderConfig.setTrustedCertOids(oids);

    }

    /**
     * Perform any panel-specific last-second checking at the time the user presses the "Next"
     * (or "Finish") button
     * while this panel is showing.  The panel may veto the action by returning false here.
     * Since this method is called in response to user input it may take possibly-lengthy actions
     * such as downloading a remote file.
     *
     * This differs from canAdvance() in that it is called when the user actually hits the Next button,
     * whereas canAdvance() is used to determine if the Next button is even enabled.
     *
     * @return true if it is safe to advance to the next step; false if not (and the user may have
     *            been pestered with an error dialog).
     */
     public boolean onNextButton() {
        final JDialog owner = getOwner();
        if (trustedCertTable.getModel().getRowCount() == 0) {
            FederatedIPWarningDialog d = new FederatedIPWarningDialog(owner, createMsgPanel());
            d.pack();
            d.addWizardListener(wizardListener);
            Utilities.centerOnScreen(d);
            d.setVisible(true);
        } else {
            return true;
        }

        return limitationsAccepted;
    }

    /**
     *  Create the Message Panel
     *
     * @return  JPanel  The Message Panel
     */
    private JPanel createMsgPanel() {
        Icon icon = new ImageIcon(FederatedIPTrustedCertsPanel.class.getClassLoader().getResource("com/l7tech/console/resources" + "/check16.gif"));

        int position = 0;
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(5, 1, new Insets(5, 0, 0, 0), -1, -1));

        final JLabel virtualGroupMsg = new JLabel();
        virtualGroupMsg.setText(resources.getString("messagePanel.noVirtualGroupMessage"));
        virtualGroupMsg.setIcon(icon);
        panel1.add(virtualGroupMsg, new GridConstraints(position++, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));

        if(x509CertSelected) {
            final JLabel x509Msg = new JLabel();
            x509Msg.setText(resources.getString("messagePanel.manualCertRequiredMessage"));
            x509Msg.setIcon(icon);
            //noinspection UnusedAssignment
            panel1.add(x509Msg, new GridConstraints(position++, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        }
        return panel1;
    }

    /**
     * Initialize the components of the Panel
     */
    private void initComponents() {

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        if (trustedCertTable == null) {
            trustedCertTable = new TrustedCertsTable();
        }
        certScrollPane.setViewportView(trustedCertTable);
        certScrollPane.getViewport().setBackground(Color.white);

        // Hide the cert usage data column
        trustedCertTable.hideColumn(TrustedCertTableSorter.CERT_TABLE_CERT_USAGE_COLUMN_INDEX);

        // initialize the button states
        enableOrDisableButtons();

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


        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                CertSearchPanel sp = new CertSearchPanel(getOwner());
                sp.addCertListener(certListener);
                sp.pack();
                Utilities.centerOnScreen(sp);
                DialogDisplayer.display(sp);

            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    trustedCertTable.getTableSorter().deleteRow(row);
                }
            }
        });

        propertiesButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {

                int row = trustedCertTable.getSelectedRow();
                if (row >= 0) {
                    CertPropertiesWindow cpw = new CertPropertiesWindow(getOwner(), (TrustedCert) trustedCertTable.getTableSorter().getData(row), false);
                    DialogDisplayer.display(cpw);
                }
            }
        });

        createCertButton.setAction(createCertAction);

        applyFormSecurity();
    }

    private void applyFormSecurity() {
        boolean canEdit = !isReadOnly();
        addButton.setEnabled(canEdit);
    }

    /**
     * Enable or disable the fields based on the current selections.
     */
    private void enableOrDisableButtons() {
        boolean hasEditPermission = !isReadOnly();
        int row = trustedCertTable.getSelectedRow();
        boolean removeAndPropertiesEnabled = (row >= 0);

        removeButton.setEnabled(hasEditPermission && removeAndPropertiesEnabled);
        propertiesButton.setEnabled(removeAndPropertiesEnabled);
    }

    /**
     * Listener for handling the event of adding a cert to the FIP
     */
    private final CertListener certListener = new CertListenerAdapter() {
        public void certSelected(CertEvent e) {
            if(!trustedCertTable.getTableSorter().contains(e.getCert())) {
                TrustedCert tc = e.getCert();
                try {
                    if (isCertRelatedToSSG(tc)) {
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                      "This cert cannot be used as a trusted cert in this " +
                                                      "federated identity\nprovider because it is related to the " +
                                                      "SecureSpan Gateway's root cert.",
                                                      "Cannot add this cert",
                                                      JOptionPane.ERROR_MESSAGE);
                    } else if (isCertTrustedByAnotherProvider(tc)) {
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                      "This cert cannot be used as a trusted cert in this " +
                                                      "federated identity\nprovider because it is already " +
                                                      "trusted by another identity provider.",
                                                      "Cannot add this cert",
                                                      JOptionPane.ERROR_MESSAGE);
                    } else {
                        trustedCertTable.getTableSorter().addRow(tc);
                    }

                } catch (IOException e1) {
                    throw new RuntimeException(e1); //  not expected to happen
                } catch (CertificateException e1) {
                    throw new RuntimeException(e1); //  not expected to happen
                } catch (FindException e1) {
                    throw new RuntimeException(e1); //  not expected to happen
                }
            } else {
                // cert alreay exsits
                JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this, resources.getString("add.cert.duplicated"),
                        resources.getString("add.cert.error"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }

    };

    private boolean isCertTrustedByAnotherProvider(TrustedCert trustedCert) throws RemoteException, FindException {
        if (fedIdProvConfigs.isEmpty()) {
            IdentityAdmin idadmin = Registry.getDefault().getIdentityAdmin();
            EntityHeader[] providerHeaders = idadmin.findAllIdentityProviderConfig();
            for (EntityHeader providerHeader : providerHeaders) {
                IdentityProviderConfig config = idadmin.findIdentityProviderConfigByID(providerHeader.getOid());
                if (config instanceof FederatedIdentityProviderConfig) {
                    fedIdProvConfigs.add((FederatedIdentityProviderConfig) config);
                }
            }
        }
        for (FederatedIdentityProviderConfig cfg : fedIdProvConfigs) {
            long[] trustedCertOIDs = cfg.getTrustedCertOids();
            for (long trustedCertOID : trustedCertOIDs) {
                if (trustedCertOID == trustedCert.getOid()) {
                    return true;
                }

            }
        }
        return false;
    }

    /**
     * Check whether or not the passed cert is related somehow to the ssg's root cert.
     * @return true if it is
     */
    private boolean isCertRelatedToSSG(TrustedCert trustedCert) throws IOException, CertificateException {
        if (ssgcert == null) {
            ssgcert = getTrustedCertAdmin().getSSGRootCert();
        }
        byte[] certbytes = HexUtils.decodeBase64(trustedCert.getCertBase64());
        X509Certificate[] chainToVerify = CertUtils.decodeCertChain(certbytes);
        try {
            CertUtils.verifyCertificateChain(chainToVerify, ssgcert, chainToVerify.length);
        } catch (CertUtils.CertificateUntrustedException e) {
            // this is what we were hoping for
            logger.finest("the cert is not related.");
            return false;
        }
        logger.finest("The cert appears to be related!");
        return true;
    }

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the dialog has finished.
         *
         * @param we the event describing the dialog finish
         */
        public void wizardFinished(WizardEvent we) {
            limitationsAccepted = true;
        }

        /**
         * Invoked when the wizard has cancelled.
         *
         * @param we the event describing the dialog cancelled
         */
        public void wizardCanceled(WizardEvent we) {
            limitationsAccepted =false;
        }

    };

    private void createNewTrustedCert() {
        Dialog thisDlg = null;
        Container tmp = FederatedIPTrustedCertsPanel.this;
        do {
            if (tmp instanceof Dialog) {
                thisDlg = (Dialog)tmp;
                break;
            } else {
                tmp = tmp.getParent();
            }
        } while (tmp != null);
        final Dialog thisdlg2 = thisDlg;

        CertImportMethodsPanel sp = new CertImportMethodsPanel(new CertDetailsPanel(new CertUsagePanel(null)), true);
        Wizard w = new AddCertificateWizard(thisdlg2, sp);
        w.addWizardListener(new WizardListener() {
            public void wizardFinished(WizardEvent e) {
                Wizard w = (Wizard)e.getSource();
                Object o = w.getWizardInput();
                if (o instanceof TrustedCert) {
                    final TrustedCert tc = (TrustedCert)o;
                    long newid;
                    try {
                        newid = getTrustedCertAdmin().saveCert(tc);
                        tc.setOid(newid);
                        certListener.certSelected(new CertEvent(this, tc));
                    } catch (SaveException e1) {
                        logger.log(Level.WARNING, "error saving cert", e);
                        if (ExceptionUtils.causedBy(e1, CertificateExpiredException.class)) {
                            JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "The cert is expired",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                        } else if (ExceptionUtils.causedBy(e1, DuplicateObjectException.class)) {
                            JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "This cert has already been imported",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                        } else if (ExceptionUtils.causedBy(e1, CertificateNotYetValidException.class)) {
                            JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "This cert is not yet valid",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                        } else {
                            JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "Could not save cert",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                        }
                    } catch (UpdateException e1) {
                        logger.log(Level.WARNING, "error saving cert", e);
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "Could not save cert",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                    } catch (VersionException e1) {
                        logger.log(Level.WARNING, "error saving cert", e);
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "Could not save cert",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                    } catch (RemoteException e1) {
                        logger.log(Level.WARNING, "error saving cert", e);
                        JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                                                          "Could not save cert",
                                                          "Error saving cert",
                                                          JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            public void wizardSelectionChanged(WizardEvent e) {}
            public void wizardCanceled(WizardEvent e) {}
        });
        w.pack();
        Utilities.centerOnScreen(w);
        DialogDisplayer.display(w);
    }

    private class CreateCertAction extends SecureAction {
        public CreateCertAction() {
            super(new AttemptedCreate(com.l7tech.common.security.rbac.EntityType.TRUSTED_CERT));
        }

        public String getName() {
            return resources.getString("createCertButton.text");
        }

        protected String iconResource() {
            return null;
        }

        protected void performAction() {
            createNewTrustedCert();
        }

    }
}
