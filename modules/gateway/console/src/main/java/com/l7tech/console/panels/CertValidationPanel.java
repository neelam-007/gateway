package com.l7tech.console.panels;

import com.l7tech.console.util.Registry;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.TrustedCertAdmin;
import com.l7tech.objectmodel.FindException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.Resolver;
import com.l7tech.util.ResolvingComparator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author ghuang
 */
public class CertValidationPanel extends WizardStepPanel {
    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.CertificateDialog");
    private static Logger logger = Logger.getLogger(CertValidationPanel.class.getName());

    private JPanel mainPanel;
    private JCheckBox certificateIsATrustCheckBox;
    private JCheckBox verifySslHostnameCheckBox;
    private JRadioButton revocationCheckDefaultRadioButton;
    private JRadioButton revocationCheckDisabledRadioButton;
    private JRadioButton revocationCheckSelectedRadioButton;
    private JComboBox revocationCheckPolicyComboBox;

    public CertValidationPanel(WizardStepPanel next) {
        super(next);
        initialize();
    }

    private void initialize() {
        add(mainPanel);

        certificateIsATrustCheckBox.setSelected(true); // the default is set to be selected.

        setVerifySslHostnameCheckBoxEnabled(false);

        revocationCheckSelectedRadioButton.addChangeListener(new ChangeListener(){
            public void stateChanged(ChangeEvent e) {
                setRevocationCheckPolicyComboState();
            }
        });
        revocationCheckPolicyComboBox.setRenderer(new Renderers.RevocationCheckPolicyRenderer());

        setRevocationCheckPolicyComboState();

        if (! populateRevocationCheckPolicies()) {
            disableAll();
        }
    }

    @Override
    public String getStepLabel() {
        return "Configure Validation";
    }

    @Override
    public String getDescription() {
        return resources.getString("usage.desc.validation");
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    /**
     * Provides the wizard panel with the opportunity to update the
     * settings with its current customized state.
     * Rather than updating its settings with every change in the GUI,
     * it should collect them, and then only save them when requested to
     * by this method.
     *
     * @param settings the object representing wizard panel state by the wizard are not valid.
     */
    @Override
    public void storeSettings(Object settings) {
        if (settings != null) {
            if (settings instanceof TrustedCert) {
                TrustedCert tc = (TrustedCert) settings;

                tc.setTrustAnchor(certificateIsATrustCheckBox.isSelected());
                tc.setVerifyHostname(verifySslHostnameCheckBox.isEnabled() && verifySslHostnameCheckBox.isSelected());

                if (revocationCheckSelectedRadioButton.isSelected()) {
                    tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.SPECIFIED);
                    tc.setRevocationCheckPolicyOid(((RevocationCheckPolicy)revocationCheckPolicyComboBox.getSelectedItem()).getGoid());
                } else if (revocationCheckDisabledRadioButton.isSelected()) {
                    tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);
                    tc.setRevocationCheckPolicyOid(null);
                } else if (revocationCheckDefaultRadioButton.isSelected()){
                    tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
                    tc.setRevocationCheckPolicyOid(null);
                }
            }
        }
    }
    
    public boolean isTrustAnchor() {
        return certificateIsATrustCheckBox.isSelected();
    }

    public boolean isVerifyHostname() {
        return verifySslHostnameCheckBox.isEnabled() && verifySslHostnameCheckBox.isSelected();
    }

    public void setVerifySslHostnameCheckBoxEnabled(boolean enabled) {
        verifySslHostnameCheckBox.setEnabled(enabled);
    }

    /**
     * Set the enabled state of the RevocationCheckPolicy drop down list.
     */
    private void setRevocationCheckPolicyComboState() {
        revocationCheckPolicyComboBox.setEnabled(revocationCheckSelectedRadioButton.isSelected());
    }

    private boolean populateRevocationCheckPolicies() {
        boolean populated = false;
        java.util.List<RevocationCheckPolicy> revocationCheckPolicies = new ArrayList<RevocationCheckPolicy>();

        try {
            TrustedCertAdmin tca = getTrustedCertAdmin();
            if (tca != null) {
                revocationCheckPolicies.addAll(tca.findAllRevocationCheckPolicies());
                populated = true;
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Unable to load certificate data from server", fe);
            JOptionPane.showMessageDialog(this,
                resources.getString("cert.load.error"),
                resources.getString("load.error.title"),
                JOptionPane.ERROR_MESSAGE);
        }

        Collections.sort(revocationCheckPolicies, new ResolvingComparator(new Resolver<RevocationCheckPolicy, String>() {
            @Override
            public String resolve(RevocationCheckPolicy rcp) {
                String name = rcp.getName();
                if (name == null)
                    name = "";
                return name.toLowerCase();
            }
        }, false));

        DefaultComboBoxModel model = new DefaultComboBoxModel(revocationCheckPolicies.toArray());
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
     * Retrieve the object reference of the Trusted Cert Admin service
     *
     * @return TrustedCertAdmin  The object reference.
     * @throws RuntimeException if the object reference of the Trusted Cert Admin service is not found.
     */
    private static TrustedCertAdmin getTrustedCertAdmin()  {
        Registry reg = Registry.getDefault();
        if (!reg.isAdminContextPresent()) {
            logger.warning("Cannot get Trusted Cert Admin due to no Admin Context present.");
            return null;
        }
        return reg.getTrustedCertManager();
    }

    private void disableAll() {

        verifySslHostnameCheckBox.setEnabled(false);
        certificateIsATrustCheckBox.setEnabled(false);

        // radios
        revocationCheckDefaultRadioButton.setEnabled(false);
        revocationCheckDisabledRadioButton.setEnabled(false);
        revocationCheckSelectedRadioButton.setEnabled(false);

        // combos
        revocationCheckPolicyComboBox.setEnabled(false);
    }
}
