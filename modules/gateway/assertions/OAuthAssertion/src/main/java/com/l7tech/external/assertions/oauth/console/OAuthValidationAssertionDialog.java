package com.l7tech.external.assertions.oauth.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.oauth.OAuthValidationAssertion;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;

public class OAuthValidationAssertionDialog extends AssertionPropertiesOkCancelSupport<OAuthValidationAssertion> {
    private JPanel contentPane;
    private JTextField oauthTokenSignatureTextField;
    private JTextField oauthTokenPlainTextField;
    private JRadioButton sha1WithRsaRadioButton;
    private JRadioButton sha256WithRsaRadioButton;
    private JRadioButton sha1WithHmacRadioButton;
    private JRadioButton sha256WithHmacRadioButton;
    private JCheckBox failPolicyCheckBox;
    private JTextField verifyCertName;
    private JCheckBox signatureEscapedCheckBox;

    public OAuthValidationAssertionDialog(Window owner, OAuthValidationAssertion assertion) {
        super(OAuthValidationAssertion.class, owner, "OAuth Token Verification Properties", true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        setContentPane(createContentPane());
        getRootPane().setDefaultButton(getOkButton());
        Utilities.setEscKeyStrokeDisposes(this);
        pack();

        ButtonGroup msgDigestGroup = new ButtonGroup();
        msgDigestGroup.add(sha1WithRsaRadioButton);
        msgDigestGroup.add(sha256WithRsaRadioButton);

        // disabled until we have to support HMAC
        sha1WithHmacRadioButton.setEnabled(false);
        sha1WithHmacRadioButton.setSelected(false);
        sha256WithHmacRadioButton.setEnabled(false);
        sha256WithHmacRadioButton.setSelected(false);
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public OAuthValidationAssertion getData(OAuthValidationAssertion assertion) throws ValidationException {

        // need to add field validation to test for missing required fields.
        assertion.setOAuthTokenSignature(oauthTokenSignatureTextField.getText());
        assertion.setOAuthTokenText(oauthTokenPlainTextField.getText());
        assertion.setVerifyCertificateName(verifyCertName.getText());
        assertion.setFailOnMismatch(failPolicyCheckBox.isSelected());
        assertion.setSignatureEncoded(signatureEscapedCheckBox.isSelected());

        if (sha256WithRsaRadioButton.isSelected())
            assertion.setSignatureAlgorithm("SHA256withRSA");
        else
            assertion.setSignatureAlgorithm("SHA1withRSA");

        return assertion;
    }

    @Override
    public void setData(OAuthValidationAssertion assertion) {

        if (assertion.getOAuthTokenSignature() != null)
            oauthTokenSignatureTextField.setText(assertion.getOAuthTokenSignature());
        if (assertion.getOAuthTokenText() != null)
            oauthTokenPlainTextField.setText(assertion.getOAuthTokenText());
        if (assertion.getVerifyCertificateName() != null)
            verifyCertName.setText(assertion.getVerifyCertificateName());

        if ("SHA256withRSA".equals(assertion.getSignatureAlgorithm()))
            sha256WithRsaRadioButton.setSelected(true);
        else
            sha1WithRsaRadioButton.setSelected(true);
        
        failPolicyCheckBox.setSelected(assertion.isFailOnMismatch());
        signatureEscapedCheckBox.setSelected(assertion.isSignatureEncoded());
    }

}
