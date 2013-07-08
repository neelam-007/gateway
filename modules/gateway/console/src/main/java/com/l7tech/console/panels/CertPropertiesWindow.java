package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gui.util.GuiCertUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DocumentSizeFilter;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.VersionException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.AccessControlException;
import java.security.AccessController;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class provides a dialog for viewing a trusted certificate and its usage.
 * Users can modify the cert name and ussage via the dialog.
 * <p/>
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class CertPropertiesWindow extends JDialog {
    private JPanel mainPanel;
    private JPanel certMainPanel;
    private JPanel certPanel;
    private JTextField certExpiredOnTextField;
    private JTextField certIssuedToTextField;
    private JTextField certIssuedByTextField;
    private JTextField certNameTextField;
    private JLabel certNameLabel;
    private JCheckBox signingServerCertCheckBox;
    private JCheckBox signingSAMLTokenCheckBox;
    private JCheckBox signingClientCertCheckBox;
    private JCheckBox outboundSSLConnCheckBox;
    private JCheckBox samlAttestingEntityCheckBox;
    private JCheckBox verifySslHostnameCheckBox;
    private JTabbedPane tabPane;
    private JTextPane descriptionText;
    private JButton saveButton;
    private JButton cancelButton;
    private JButton exportButton;
    private JRadioButton revocationCheckDefaultRadioButton;
    private JRadioButton revocationCheckDisabledRadioButton;
    private JRadioButton revocationCheckSelectedRadioButton;
    private JComboBox revocationCheckPolicyComboBox;
    private JCheckBox certificateIsATrustCheckBox;
    private JTextPane validationDescriptionText;
    private SecurityZoneWidget zoneControl;

    private final TrustedCert trustedCert;
    private final Collection<RevocationCheckPolicy> revocationCheckPolicies; // null if not provided

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(CertPropertiesWindow.class.getName());

    private Window owner;

    /**
     * Constructor
     *
     * @param owner    The parent component.
     * @param tc       The trusted certificate.
     * @param editable TRUE if the properties are editable
     */
    public CertPropertiesWindow(Window owner, TrustedCert tc, boolean editable, Collection<RevocationCheckPolicy> policies) {
        this(owner, tc, editable, true, policies);
    }

    /**
     * Constructor
     *
     * @param owner    The parent component.
     * @param tc       The trusted certificate.
     * @param editable TRUE if the properties are editable
     * @param options  TRUE to display the options and validity tabs
     */
    public CertPropertiesWindow(Window owner, TrustedCert tc, boolean editable, boolean options) {
        this(owner, tc, editable, options, null);
    }

    /**
     * Constructor
     *
     * @param owner    The parent component.
     * @param tc       The trusted certificate.
     * @param editable TRUE if the properties are editable
     * @param options  TRUE to display the options and validity tabs
     */
    public CertPropertiesWindow(Window owner, TrustedCert tc, boolean editable, boolean options, Collection<RevocationCheckPolicy> policies) {
        super(owner, resources.getString("cert.properties.dialog.title"), CertPropertiesWindow.DEFAULT_MODALITY_TYPE);
        this.owner = owner;
        this.trustedCert = tc;
        this.revocationCheckPolicies = policies;

        final Authorizer authorizer = Registry.getDefault().getSecurityProvider();
        if (authorizer == null) {
            throw new IllegalStateException("Could not instantiate authorization provider");
        }

        AttemptedOperation ao;
        if (tc.getOid() == TrustedCert.DEFAULT_OID) {
            ao = new AttemptedCreate(EntityType.TRUSTED_CERT);
        } else {
            ao = new AttemptedUpdate(EntityType.TRUSTED_CERT, tc);
        }
        
        editable = editable && authorizer.hasPermission(ao);
        initialize(editable, options);
    }

    /**
     * Helper method to load {@link RevocationCheckPolicy RevocationCheckPolicies} from the gateway.
     *
     * @return The collection of policies in no particular order (can be empty but not null)
     * @throws FindException if an error occurs loading the policies
     */
    public static Collection<RevocationCheckPolicy> loadRevocationCheckPolicies() throws FindException {
        Collection<RevocationCheckPolicy> policies = new ArrayList();

        TrustedCertAdmin tca = getTrustedCertAdmin();
        policies = new ArrayList( tca.findAllRevocationCheckPolicies() );

        return policies;
    }

    /**
     * Initialization of the window
     *
     * @param editable TRUE if the properties are editable
     * @param options  TRUE to display the options tab
     */
    private void initialize(final boolean editable, boolean options) {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        Utilities.setEscKeyStrokeDisposes(this);

        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);
        certMainPanel.setBackground(Color.white);
        certPanel.setLayout(new FlowLayout());
        certPanel.setBackground(Color.white);

        // disable the fields if the properties should not be modified
        if (!editable) {
            disableAll();
        } else {
            ActionListener sslOptionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    verifySslHostnameCheckBox.setEnabled(signingServerCertCheckBox.isSelected() || outboundSSLConnCheckBox.isSelected());
                }
            };
            signingServerCertCheckBox.addActionListener(sslOptionListener);
            outboundSSLConnCheckBox.addActionListener(sslOptionListener);
        }

        // disable the options tab if not required
        if (!options) {
            for (int t=3; t>1; t--) {
                tabPane.setEnabledAt(t, false);
                tabPane.remove(t);
            }
            saveButton.setVisible(false);
        } else {
            if ( !populateRevocationCheckPolicies() ) {
                disableAll();    
            }
        }

        revocationCheckSelectedRadioButton.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                setRevocationCheckPolicyComboState(editable);
            }
        });
        revocationCheckPolicyComboBox.setRenderer(new Renderers.RevocationCheckPolicyRenderer());

        zoneControl.configure(trustedCert.getOid() == TrustedCert.DEFAULT_OID ? OperationType.CREATE : editable ? OperationType.UPDATE : OperationType.READ, trustedCert);

        populateData();
        setRevocationCheckPolicyComboState(editable);

        verifySslHostnameCheckBox.setEnabled(editable &&
                (signingServerCertCheckBox.isSelected() || outboundSSLConnCheckBox.isSelected()));

        saveButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                //validate fields
                String errorMsg = validateFields();
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(mainPanel, errorMsg, resources.getString("save.error.title"), JOptionPane.ERROR_MESSAGE);
                    return;
                }

                TrustedCert tc;

                // create a new trusted cert
                try {
                    tc = (TrustedCert)trustedCert.clone();
                } catch (CloneNotSupportedException e) {
                    String errmsg = "Internal error! Unable to clone the trusted certificate.";
                    logger.severe(errmsg);
                    JOptionPane.showMessageDialog(mainPanel, errmsg + " \n" + "The certificate has not been updated",
                                                  resources.getString("save.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                    return;
                }

                try {
                    // udpate the trusted cert with the new values
                    updateTrustedCert(tc);

                    // save the cert
                    getTrustedCertAdmin().saveCert(tc);

                    // Update the trusted certs in the owner windsow
                    if (owner instanceof CertManagerWindow) {
                        ((CertManagerWindow)owner).loadTrustedCerts();
                    }
                } catch (SaveException e) {
                    logger.warning("Unable to save the trusted certificate in server");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.save.error"),
                                                  resources.getString("save.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);

                } catch (VersionException e) {
                    logger.warning("Unable to save the trusted certificate: " + trustedCert.getName() + "; version exception.");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.version.error"),
                                                  resources.getString("save.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                } catch (UpdateException e) {
                    logger.warning("Unable to update the trusted certificate in server");
                    JOptionPane.showMessageDialog(mainPanel, resources.getString("cert.update.error"),
                                                  resources.getString("save.error.title"),
                                                  JOptionPane.ERROR_MESSAGE);
                }

                // suceeded, update the original trusted cert
                updateTrustedCert(trustedCert);

                // close the dialog
                dispose();

            }
        });

        exportButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                AccessController.doPrivileged(new PrivilegedAction<Object>() {
                    public Object run() {
                        try {
                            GuiCertUtil.exportCertificate(CertPropertiesWindow.this, trustedCert.getCertificate());
                        } catch (AccessControlException ace) {
                            TopComponents.getInstance().showNoPrivilegesErrorMessage();
                        }

                        return null;
                    }
                });
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                //hide();
                dispose();
            }
        });

        descriptionText.setText(resources.getString("usage.desc"));
        descriptionText.getCaret().setDot(0);

        validationDescriptionText.setText(resources.getString("usage.desc.validation"));
        validationDescriptionText.getCaret().setDot(0);

        //set max length size for certificate name
        ((AbstractDocument)certNameTextField.getDocument()).setDocumentFilter(new DocumentSizeFilter(128));

        pack();
        Utilities.centerOnParentWindow(this);
    }

    /**
     * Disable all fields and buttons
     */
    private void disableAll() {

        // all text fields
        certNameTextField.setEnabled(false);

        // all check boxes
        signingServerCertCheckBox.setEnabled(false);
        signingSAMLTokenCheckBox.setEnabled(false);
        signingClientCertCheckBox.setEnabled(false);
        outboundSSLConnCheckBox.setEnabled(false);
        samlAttestingEntityCheckBox.setEnabled(false);
        verifySslHostnameCheckBox.setEnabled(false);
        certificateIsATrustCheckBox.setEnabled(false);

        // radios
        revocationCheckDefaultRadioButton.setEnabled(false);
        revocationCheckDisabledRadioButton.setEnabled(false);
        revocationCheckSelectedRadioButton.setEnabled(false);

        // combos
        revocationCheckPolicyComboBox.setEnabled(false);

        // all buttons except the Export/Cancel button
        saveButton.setEnabled(false);
        cancelButton.setText(resources.getString("closeButton.label"));
        cancelButton.setToolTipText(resources.getString("closeButton.tooltip"));
    }

    /**
     * Set the enabled state of the RevocationCheckPolicy drop down list.
     *
     * @param editable true if editable
     */
    private void setRevocationCheckPolicyComboState(boolean editable) {
        revocationCheckPolicyComboBox.setEnabled(editable && revocationCheckSelectedRadioButton.isSelected());
    }

    /**
     *
     */
    private boolean populateRevocationCheckPolicies() {
        boolean populated = false;
        java.util.List<RevocationCheckPolicy> policies = new ArrayList();

        if (revocationCheckPolicies != null) {
            policies.addAll( revocationCheckPolicies );    
            populated = true;
        } else {
            try {
                TrustedCertAdmin tca = getTrustedCertAdmin();
                policies.addAll( tca.findAllRevocationCheckPolicies() );
                populated = true;
            } catch (FindException fe) {
                logger.log(Level.WARNING, "Unable to load certificate data from server", fe);
                JOptionPane.showMessageDialog(this,
                        resources.getString("cert.load.error"),
                        resources.getString("load.error.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        Collections.sort(policies, new ResolvingComparator(new Resolver<RevocationCheckPolicy,String>(){
            public String resolve(RevocationCheckPolicy rcp) {
                String name = rcp.getName();
                if (name == null)
                    name = "";
                return name.toLowerCase();
            }
        }, false));

        DefaultComboBoxModel model = new DefaultComboBoxModel(policies.toArray());
        revocationCheckPolicyComboBox.setModel(model);
        if (model.getSize() > 0) {
            revocationCheckPolicyComboBox.setSelectedIndex(0);
        } else {
            // disable if there is nothing to select
            revocationCheckSelectedRadioButton.setEnabled(false);    
        }
        
        return populated;
    }



    /**
     * Populate the data to the views
     */
    private void populateData() {
        if (trustedCert == null) {
            return;
        }

        X509Certificate cert = trustedCert.getCertificate();

        // populate the general data
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Calendar cal = Calendar.getInstance();
        cal.setTime(cert.getNotAfter());
        certExpiredOnTextField.setText(sdf.format(cal.getTime()));

        //Set the appropriate values and move the carat position to the begining to properly display large values [SSM-4313]
        certIssuedToTextField.setText(cert.getSubjectDN().getName());
        certIssuedToTextField.setCaretPosition(0);
        certIssuedByTextField.setText(cert.getIssuerDN().getName());
        certIssuedByTextField.setCaretPosition(0);
        certNameTextField.setText(trustedCert.getName());
        certNameTextField.setCaretPosition(0);

        if (!certNameTextField.isEnabled()) {
            certNameTextField.setOpaque(false);
            if (trustedCert.getName()==null || trustedCert.getName().trim().length()==0) {
                certNameTextField.setVisible(false);
                certNameLabel.setVisible(false);
            }
        }

        // diasble the cert options that are not allowed based on the key usage specified in the cert
/*        boolean [] keyUsageArray = cert.getKeyUsage();
        if(keyUsageArray != null && !keyUsageArray[CertUtils.KeyUsage.keyCertSign]) {
            signingServerCertCheckBox.setEnabled(false);
            signingSAMLTokenCheckBox.setEnabled(false);
            signingClientCertCheckBox.setEnabled(false);
        }*/

        // populate the details
        JComponent certView = getCertView(cert);

        if (certView == null) {
            certView = new JLabel();
        }

        certPanel.add(certView);

        // populate the usage
        signingClientCertCheckBox.setSelected(trustedCert.isTrustedForSigningClientCerts());

        signingSAMLTokenCheckBox.setSelected(trustedCert.isTrustedAsSamlIssuer());

        signingServerCertCheckBox.setSelected(trustedCert.isTrustedForSigningServerCerts());

        outboundSSLConnCheckBox.setSelected(trustedCert.isTrustedForSsl());

        samlAttestingEntityCheckBox.setSelected(trustedCert.isTrustedAsSamlAttestingEntity());

        // populate validation

        certificateIsATrustCheckBox.setSelected(trustedCert.isTrustAnchor());

        verifySslHostnameCheckBox.setSelected(trustedCert.isVerifyHostname());

        TrustedCert.PolicyUsageType policyUsageType = trustedCert.getRevocationCheckPolicyType();
        if (policyUsageType == null)
            policyUsageType = TrustedCert.PolicyUsageType.USE_DEFAULT;
        switch (policyUsageType) {
            case USE_DEFAULT:
                revocationCheckDefaultRadioButton.setSelected(true);
                break;
            case NONE:
                revocationCheckDisabledRadioButton.setSelected(true);
                break;
            case SPECIFIED:
                revocationCheckSelectedRadioButton.setSelected(true);
                break;
        }

        if (trustedCert.getRevocationCheckPolicyOid() != null) {
            revocationCheckPolicyComboBox.setSelectedItem(findRevocationCheckPolicyByOid(trustedCert.getRevocationCheckPolicyOid()));
        }
    }

    /**
     *
     */
    private RevocationCheckPolicy findRevocationCheckPolicyByOid(long oid) {
        RevocationCheckPolicy policy = null;

        ComboBoxModel model = (DefaultComboBoxModel) revocationCheckPolicyComboBox.getModel();
        for (int i=0; i<model.getSize(); i++) {
            RevocationCheckPolicy current = (RevocationCheckPolicy) model.getElementAt(i);
            if (current.getOid() == oid) {
                policy = current;
                break;
            }
        }

        return policy;
    }

    /**
     * Returns a properties instance filled out with info about the certificate.
     */
    private JComponent getCertView(X509Certificate cert) {

        com.l7tech.gui.widgets.CertificatePanel certPanel = null;
        try {
            certPanel = new com.l7tech.gui.widgets.CertificatePanel(cert);
            certPanel.setCertBorderEnabled(false);
        } catch (CertificateEncodingException ee) {
            logger.warning("Unable to decode the certificate: " + trustedCert.getName());
        } catch (NoSuchAlgorithmException ae) {
            logger.warning("Unable to decode the certificate: " + trustedCert.getName() + ", Algorithm is not supported:" + cert.getSigAlgName());
        }

        return certPanel;
    }

    /**
     * Update the trusted cert object with the current settings (from the form).
     *
     * @param tc The trusted cert to be updated.
     */
    private void updateTrustedCert(TrustedCert tc) {
        if (tc != null) {
            tc.setName(certNameTextField.getText().trim());
            tc.setTrustedForSigningClientCerts(signingClientCertCheckBox.isSelected());
            tc.setTrustedAsSamlIssuer(signingSAMLTokenCheckBox.isSelected());
            tc.setTrustedForSigningServerCerts(signingServerCertCheckBox.isSelected());
            tc.setTrustedForSsl(outboundSSLConnCheckBox.isSelected());
            tc.setTrustedAsSamlAttestingEntity(samlAttestingEntityCheckBox.isSelected());
            tc.setVerifyHostname(verifySslHostnameCheckBox.isSelected() && verifySslHostnameCheckBox.isEnabled());

            tc.setTrustAnchor(certificateIsATrustCheckBox.isSelected());
            if (revocationCheckSelectedRadioButton.isSelected()) {
                tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
                tc.setRevocationCheckPolicyOid(((RevocationCheckPolicy)revocationCheckPolicyComboBox.getSelectedItem()).getOid());
            } else if (revocationCheckDisabledRadioButton.isSelected()) {
                tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);
                tc.setRevocationCheckPolicyOid(null);
            } else {
                tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
                tc.setRevocationCheckPolicyOid(null);
            }
            tc.setSecurityZone(zoneControl.getSelectedZone());
        }
    }

    /**
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private static TrustedCertAdmin getTrustedCertAdmin() throws RuntimeException {
        TrustedCertAdmin tca = Registry.getDefault().getTrustedCertManager();
        return tca;
    }

    /**
     * @return  Returns NULL if all fields are valid, otherwise it'll return an error message describing the invalid field.
     */
    private String validateFields() {
        //certificate name cannot be empty
        String certName = certNameTextField.getText();
        if (certName == null || certName.length() <= 0){
            return resources.getString("cert.certname.empty.error");
        }

        return null;
    }
}
