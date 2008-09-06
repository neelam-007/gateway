/*
 * Copyright (C) 2004-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * This class provides the step panel for the Federated Identity Provider dialog.
 */
public class FederatedIPTrustedCertsPanel extends IdentityProviderStepPanel {
    private static final Logger logger = Logger.getLogger(FederatedIPTrustedCertsPanel.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog");

    private JPanel mainPanel;
    private JPanel certPanel;
    private boolean x509CertSelected = false;
    private boolean limitationsAccepted = true;

    private TrustedCertsPanel trustedCertsPanel = null;
    private X509Certificate ssgcert = null;
    private Collection<FederatedIdentityProviderConfig> fedIdProvConfigs = new ArrayList<FederatedIdentityProviderConfig>();

    /**
     * Construstor
     *
     * @param next  The next step panel
     */
    public FederatedIPTrustedCertsPanel(WizardStepPanel next) {
        super(next);
        initComponents(false);
    }

    public FederatedIPTrustedCertsPanel(WizardStepPanel next, boolean readOnly) {
        super(next, readOnly);
        initComponents(readOnly);
    }

    /**
     * Get the description of the step
     * @return String The description of the step
     */
    @Override
    public String getDescription() {
        return resources.getString("certPanelDescription");
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return resources.getString("certPanelStepLabel");
    }

    /**
     * Populate the configuration data from the wizard input object to the visual components of the panel.
     *
     * @param settings The current value of configuration items in the wizard input object.
     * @throws IllegalArgumentException if the data provided by the wizard are not valid.
     */
    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;

        x509CertSelected = iProviderConfig.isX509Supported();
        trustedCertsPanel.setCertificateOids( iProviderConfig.getTrustedCertOids() );
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
    @Override
    public void storeSettings(Object settings) {
        if (!(settings instanceof FederatedIdentityProviderConfig))
            throw new IllegalArgumentException("The settings object must be FederatedIdentityProviderConfig");

        FederatedIdentityProviderConfig iProviderConfig = (FederatedIdentityProviderConfig) settings;
        iProviderConfig.setTrustedCertOids(trustedCertsPanel.getCertificateOids());

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
    @Override
    public boolean onNextButton() {
        final JDialog owner = getOwner();
        if (trustedCertsPanel.getCertificateOids().length == 0) {
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
    private void initComponents(final boolean readOnly) {

        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);

        trustedCertsPanel = new TrustedCertsPanel(readOnly, 0, certListener);

        certPanel.add( trustedCertsPanel, BorderLayout.CENTER );
    }

    /**
     * Listener for handling the event of adding a cert to the FIP
     */
    private final TrustedCertsPanel.TrustedCertListener certListener = new TrustedCertsPanel.TrustedCertListenerSupport(this) {
        @Override
        public boolean addTrustedCert( final TrustedCert tc ) {
            boolean addOk = true;
            try {
                if (isCertTrustedByAnotherProvider(tc)) {
                    addOk = false;
                    JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                            "This cert cannot be used as a trusted cert in this " +
                                    "federated identity\nprovider because it is already " +
                                    "trusted by another identity provider.",
                            "Cannot add this cert",
                            JOptionPane.ERROR_MESSAGE);
                }
            } catch (FindException e1) {
                throw new RuntimeException(e1); //  not expected to happen
            }

            return addOk;
        }

        @Override
        public void notifyError() {
            JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                    resources.getString("cert.find.error"),
                    resources.getString("load.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }

    };

    private boolean isCertTrustedByAnotherProvider(TrustedCert trustedCert) throws FindException {
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

    private WizardListener wizardListener = new WizardAdapter() {
        /**
         * Invoked when the dialog has finished.
         *
         * @param we the event describing the dialog finish
         */
        @Override
        public void wizardFinished(WizardEvent we) {
            limitationsAccepted = true;
        }

        /**
         * Invoked when the wizard has cancelled.
         *
         * @param we the event describing the dialog cancelled
         */
        @Override
        public void wizardCanceled(WizardEvent we) {
            limitationsAccepted =false;
        }

    };

}
