package com.l7tech.external.assertions.generateoauthsignaturebasestring.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.generateoauthsignaturebasestring.GenerateOAuthSignatureBaseStringAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class GenerateOAuthSignatureBaseStringPropertiesDialog extends AssertionPropertiesOkCancelSupport<GenerateOAuthSignatureBaseStringAssertion> {
    private JPanel contentPane;
    private JTextField requestUrlTextField;
    private JComboBox methodComboBox;
    private JTextField queryStringTextField;
    private JCheckBox useMessageTargetAsCheckBox;
    private JTextField authHeaderTextField;
    private JCheckBox authHeaderCheckBox;
    private JTextField oauthConsumerKeyTextField;
    private JComboBox oauthSignatureMethodComboBox;
    private JCheckBox oauthVersionCheckBox;
    private JTextField oauthVersionTextField;
    private JTextField oauthTokenTextField;
    private JTextField oauthCallbackTextField;
    private JLabel oauthConsumerKeyLabel;
    private JLabel oauthSigMethodLabel;
    private JLabel oauthTimestampLabel;
    private JLabel oauthNonceLabel;
    private JLabel oauthTokenLabel;
    private JLabel oauthCallbackLabel;
    private JCheckBox clientParametersCheckBox;
    private TargetVariablePanel targetVariablePanel;
    private JTextField oauthVerifierTextField;
    private JLabel oauthVerifierLabel;
    private JComboBox oauthTimestampComboBox;
    private JComboBox oauthNonceComboBox;
    private InputValidator validators;
    private List<InputValidator.ValidationRule> requiredFields;

    public GenerateOAuthSignatureBaseStringPropertiesDialog(final Window owner, final GenerateOAuthSignatureBaseStringAssertion assertion) {
        super(GenerateOAuthSignatureBaseStringAssertion.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        requiredFields = new ArrayList<InputValidator.ValidationRule>();
        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty("Request URL", requestUrlTextField, null);
        validators.addRule(new NonEmptyEditableComboBoxRule("HTTP Method", methodComboBox));
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        final ActionListener runOnChangeListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        });

        clientParametersCheckBox.addActionListener(runOnChangeListener);
        authHeaderCheckBox.addActionListener(runOnChangeListener);
        oauthVersionCheckBox.addActionListener(runOnChangeListener);
        useMessageTargetAsCheckBox.addActionListener(runOnChangeListener);
    }

    private void enableDisableComponents() {
        authHeaderTextField.setEnabled(authHeaderCheckBox.isSelected());
        final boolean clientParams = clientParametersCheckBox.isSelected();
        oauthConsumerKeyLabel.setEnabled(clientParams);
        oauthConsumerKeyTextField.setEnabled(clientParams);
        oauthSigMethodLabel.setEnabled(clientParams);
        oauthSignatureMethodComboBox.setEnabled(clientParams);
        oauthTimestampLabel.setEnabled(clientParams);
        oauthTimestampComboBox.setEnabled(clientParams);
        oauthNonceLabel.setEnabled(clientParams);
        oauthNonceComboBox.setEnabled(clientParams);
        oauthVersionCheckBox.setEnabled(clientParams);
        oauthVersionTextField.setEnabled(clientParams && oauthVersionCheckBox.isSelected());
        oauthTokenLabel.setEnabled(clientParams);
        oauthTokenTextField.setEnabled(clientParams);
        oauthCallbackLabel.setEnabled(clientParams);
        oauthCallbackTextField.setEnabled(clientParams);
        oauthVerifierLabel.setEnabled(clientParams);
        oauthVerifierTextField.setEnabled(clientParams);

        if (clientParams && !authHeaderCheckBox.isSelected() && !useMessageTargetAsCheckBox.isSelected() && requiredFields.isEmpty()) {
            requiredFields.add(validators.constrainTextFieldToBeNonEmpty("oauth_consumer_key", oauthConsumerKeyTextField, null));
            requiredFields.add(validators.ensureComboBoxSelection("oauth_signature_method", oauthSignatureMethodComboBox));
            final NonEmptyEditableComboBoxRule timestamp = new NonEmptyEditableComboBoxRule("oauth_timestamp", oauthTimestampComboBox);
            validators.addRule(timestamp);
            requiredFields.add(timestamp);
            final NonEmptyEditableComboBoxRule nonce = new NonEmptyEditableComboBoxRule("oauth_nonce", oauthNonceComboBox);
            validators.addRule(nonce);
            requiredFields.add(nonce);
        } else {
            for (final InputValidator.ValidationRule requiredField : requiredFields) {
                validators.removeRule(requiredField);
            }
            requiredFields.clear();
        }
    }

    @Override
    public void setData(final GenerateOAuthSignatureBaseStringAssertion assertion) {
        requestUrlTextField.setText(assertion.getRequestUrl());
        methodComboBox.setSelectedItem(assertion.getHttpMethod());
        queryStringTextField.setText(assertion.getQueryString());
        useMessageTargetAsCheckBox.setSelected(assertion.isUseMessageTarget());
        authHeaderCheckBox.setSelected(assertion.isUseAuthorizationHeader());
        clientParametersCheckBox.setSelected(assertion.isUseManualParameters());
        authHeaderTextField.setText(assertion.getAuthorizationHeader());
        oauthConsumerKeyTextField.setText(assertion.getOauthConsumerKey());
        oauthSignatureMethodComboBox.setSelectedItem(assertion.getOauthSignatureMethod());
        oauthTimestampComboBox.setSelectedItem(assertion.getOauthTimestamp());
        oauthNonceComboBox.setSelectedItem(assertion.getOauthNonce());
        oauthVersionCheckBox.setSelected(assertion.isUseOAuthVersion());
        oauthVersionTextField.setText(assertion.getOauthVersion());
        oauthTokenTextField.setText(assertion.getOauthToken());
        oauthCallbackTextField.setText(assertion.getOauthCallback());
        oauthVerifierTextField.setText(assertion.getOauthVerifier());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        enableDisableComponents();
    }

    @Override
    public GenerateOAuthSignatureBaseStringAssertion getData(final GenerateOAuthSignatureBaseStringAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }

        // required fields
        assertion.setRequestUrl(requestUrlTextField.getText().trim());
        assertion.setHttpMethod(methodComboBox.getSelectedItem().toString());
        assertion.setUseMessageTarget(useMessageTargetAsCheckBox.isSelected());
        assertion.setUseAuthorizationHeader(authHeaderCheckBox.isSelected());
        assertion.setUseManualParameters(clientParametersCheckBox.isSelected());
        assertion.setUseOAuthVersion(oauthVersionCheckBox.isSelected());
        assertion.setOauthSignatureMethod(oauthSignatureMethodComboBox.getSelectedItem().toString());
        assertion.setVariablePrefix(targetVariablePanel.getVariable().trim());

        // optional fields
        assertion.setQueryString(getTrimmedValueOrNull(queryStringTextField));
        assertion.setAuthorizationHeader(getTrimmedValueOrNull(authHeaderTextField));
        assertion.setOauthConsumerKey(getTrimmedValueOrNull(oauthConsumerKeyTextField));
        assertion.setOauthTimestamp(getTrimmedValueOrNull(oauthTimestampComboBox));
        assertion.setOauthNonce(getTrimmedValueOrNull(oauthNonceComboBox));
        assertion.setOauthVersion(getTrimmedValueOrNull(oauthVersionTextField));
        assertion.setOauthToken(getTrimmedValueOrNull(oauthTokenTextField));
        assertion.setOauthCallback(getTrimmedValueOrNull(oauthCallbackTextField));
        assertion.setOauthVerifier(getTrimmedValueOrNull(oauthVerifierTextField));
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.contentPane;
    }

    private String getTrimmedValueOrNull(final JComboBox comboBox) {
        String value = null;
        if (!comboBox.getSelectedItem().toString().trim().isEmpty()) {
            value = comboBox.getSelectedItem().toString().trim();
        }
        return value;
    }

    private String getTrimmedValueOrNull(final JTextField textField) {
        String value = null;
        if (!textField.getText().trim().isEmpty()) {
            value = textField.getText().trim();
        }
        return value;
    }

    private class NonEmptyEditableComboBoxRule implements InputValidator.ValidationRule {
        private final String fieldName;
        private final JComboBox comboBox;

        private NonEmptyEditableComboBoxRule(final String fieldName, final JComboBox comboBox) {
            this.fieldName = fieldName;
            this.comboBox = comboBox;
        }

        @Override
        public String getValidationError() {
            return (comboBox.getSelectedItem() == null || comboBox.getSelectedItem().toString().trim().isEmpty()) ?
                    "The " + fieldName + " field must not be empty." : null;
        }
    }
}
