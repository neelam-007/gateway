/*
 * Copyright (C) 2004-2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.l7tech.console.event.WizardAdapter;
import com.l7tech.console.event.WizardEvent;
import com.l7tech.console.event.WizardListener;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.WrappingLabel;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.CollectionUtils.MapBuilder;
import com.l7tech.util.Functions.Binary;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.logging.Level;

import static com.l7tech.gui.util.Utilities.listModel;
import static com.l7tech.objectmodel.EntityUtil.name;
import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Functions.reduce;
import static java.util.Collections.sort;

/**
 * This class provides the step panel for the Federated Identity Provider dialog.
 */
public class FederatedIPTrustedCertsPanel extends IdentityProviderStepPanel {
    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.FederatedIdentityProviderDialog");

    private Goid oid;
    private JPanel mainPanel;
    private JPanel certPanel;
    private boolean x509CertSelected = false;
    private boolean limitationsAccepted = true;

    private TrustedCertsPanel trustedCertsPanel = null;
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

        oid = iProviderConfig.getGoid();
        x509CertSelected = iProviderConfig.isX509Supported();
        trustedCertsPanel.setCertificateGoids(iProviderConfig.getTrustedCertGoids());
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
        iProviderConfig.setTrustedCertGoids(trustedCertsPanel.getCertificateGoids());

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

        Collection<TrustedCert> certsInUse;
        if ( trustedCertsPanel.getCertificateGoids().length == 0 ) {
            final FederatedIPWarningDialog d = new FederatedIPWarningDialog(owner, createMsgPanel());
            d.pack();
            d.addWizardListener(wizardListener);
            Utilities.centerOnScreen(d);
            d.setVisible(true);
            return limitationsAccepted;
        } else if ( !(certsInUse = getCertsTrustedByAnotherProvider()).isEmpty() ) {
            final java.util.List<String> displayCertificates = map( certsInUse, name() );
            sort( displayCertificates, String.CASE_INSENSITIVE_ORDER );
            final JPanel panel = new JPanel();
            panel.setLayout( new BorderLayout( 4, 4 ) );
            panel.add( new WrappingLabel(resources.getString( "error-trusted-certificate-in-use" )), BorderLayout.NORTH );
            panel.add( new JScrollPane(new JList( listModel( displayCertificates ) )), BorderLayout.CENTER );
            panel.setPreferredSize( new Dimension( 420, 160 ) );
            int result = JOptionPane.showConfirmDialog( this,
                    panel,
                    resources.getString( "trusted-certificate-in-use-warning-dialog.title" ),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE );

            return result == JOptionPane.OK_OPTION;
        }

        return true;
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
        public void notifyError() {
            JOptionPane.showMessageDialog(FederatedIPTrustedCertsPanel.this,
                    resources.getString("cert.find.error"),
                    resources.getString("load.error.title"),
                    JOptionPane.ERROR_MESSAGE);
        }

    };

    private Collection<TrustedCert> getCertsTrustedByAnotherProvider() {
        try {
            if ( fedIdProvConfigs.isEmpty() ) {
                final IdentityAdmin idadmin = Registry.getDefault().getIdentityAdmin();
                final EntityHeader[] providerHeaders = idadmin.findAllIdentityProviderConfig();
                for ( final EntityHeader providerHeader : providerHeaders) {
                    final IdentityProviderConfig config = idadmin.findIdentityProviderConfigByID(providerHeader.getGoid());
                    if ( !config.getGoid().equals( oid) && config instanceof FederatedIdentityProviderConfig ) {
                        fedIdProvConfigs.add((FederatedIdentityProviderConfig) config);
                    }
                }
            }

            final Map<Goid,TrustedCert> trustedCertificates = reduce(
                    trustedCertsPanel.getTrustedCertificates(),
                    CollectionUtils.<Goid,TrustedCert>mapBuilder(),
                    new Binary<MapBuilder<Goid, TrustedCert>,MapBuilder<Goid,TrustedCert>,TrustedCert>(){
                @Override
                public MapBuilder<Goid, TrustedCert> call( final MapBuilder<Goid, TrustedCert> builder, final TrustedCert trustedCert ) {
                    builder.put( trustedCert.getGoid(), trustedCert );
                    return builder;
                }
            } ).map();

            final Set<Goid> usedTrustedCertificateOids = reduce( fedIdProvConfigs, new HashSet<Goid>(), new Binary<Set<Goid>,Set<Goid>,FederatedIdentityProviderConfig>(){
                @Override
                public Set<Goid> call( final Set<Goid> oids, final FederatedIdentityProviderConfig config ) {
                    oids.addAll( CollectionUtils.list( config.getTrustedCertGoids() ) );
                    return oids;
                }
            } );

            trustedCertificates.keySet().retainAll( usedTrustedCertificateOids );
            return trustedCertificates.values();
        } catch ( FindException e ) {
            ErrorManager.getDefault().notify( Level.WARNING, e, "Error checking trusted certificate use" );
        }

        return Collections.emptyList();
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
