package com.l7tech.console.panels;

import com.l7tech.console.util.PasswordGuiUtils;
import com.l7tech.policy.assertion.credential.wss.WssDigest;

import javax.swing.*;
import java.awt.*;

public class WssDigestAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<WssDigest> {
    private JPanel contentPane;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private JLabel plaintextPasswordWarningLabel;
    private JTextField usernameTextField;
    private JCheckBox nonceCheckBox;
    private JCheckBox timestampCheckBox;

    public WssDigestAssertionPropertiesDialog(Frame owner, WssDigest assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    public void setData(WssDigest assertion) {
        usernameTextField.setText(assertion.getRequiredUsername());
        passwordField.setText(assertion.getRequiredPassword());
        nonceCheckBox.setSelected(assertion.isRequireNonce());
        timestampCheckBox.setSelected(assertion.isRequireTimestamp());
    }

    @Override
    public WssDigest getData(WssDigest assertion) throws ValidationException {
        assertion.setRequiredUsername(usernameTextField.getText());
        assertion.setRequiredPassword(new String(passwordField.getPassword()));
        assertion.setRequireNonce(nonceCheckBox.isSelected());
        assertion.setRequireTimestamp(timestampCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        PasswordGuiUtils.configureOptionalSecurePasswordField(this, passwordField, showPasswordCheckBox, plaintextPasswordWarningLabel);
        return contentPane;
    }
}
