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
    private JTextField oauthTokenTextField;
    private JTextField oauthCallbackTextField;
    private TargetVariablePanel targetVariablePanel;
    private JTextField oauthVerifierTextField;
    private JRadioButton clientRadioButton;
    private JRadioButton serverRadioButton;
    private JPanel serverPanel;
    private JPanel clientPanel;
    private JLabel oauthVersionLabel;
    private JCheckBox allowCustomOAuthQueryParamsCheckBox;
    private InputValidator validators;
    private InputValidator.ValidationRule authHeaderRule;

    public GenerateOAuthSignatureBaseStringPropertiesDialog(final Window owner, final GenerateOAuthSignatureBaseStringAssertion assertion) {
        super(GenerateOAuthSignatureBaseStringAssertion.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty("Request URL", requestUrlTextField, null);
        validators.addRule(new NonEmptyEditableComboBoxRule("HTTP Method", methodComboBox));
        validators.addRule(new QueryStringConsumerKeyRule());
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

        clientRadioButton.addActionListener(runOnChangeListener);
        serverRadioButton.addActionListener(runOnChangeListener);
        authHeaderCheckBox.addActionListener(runOnChangeListener);
        oauthVersionCheckBox.addActionListener(runOnChangeListener);
    }

    private void enableDisableComponents() {
        clientPanel.setEnabled(clientRadioButton.isSelected());
        final Component[] clientChildren = clientPanel.getComponents();
        for (final Component clientChild : clientChildren) {
            clientChild.setEnabled(clientRadioButton.isSelected());
        }
        serverPanel.setEnabled(serverRadioButton.isSelected());
        final Component[] serverChildren = serverPanel.getComponents();
        for (final Component serverChild : serverChildren) {
            serverChild.setEnabled(serverRadioButton.isSelected());
        }
        authHeaderTextField.setEnabled(serverRadioButton.isSelected() && authHeaderCheckBox.isSelected());
        oauthVersionLabel.setEnabled(clientRadioButton.isSelected() && oauthVersionCheckBox.isSelected());

        if (serverRadioButton.isSelected() && authHeaderCheckBox.isSelected()) {
            authHeaderRule = validators.constrainTextFieldToBeNonEmpty("Authorization Header", authHeaderTextField, null);
        } else {
            validators.removeRule(authHeaderRule);
            authHeaderRule = null;
        }
    }

    @Override
    public void setData(final GenerateOAuthSignatureBaseStringAssertion assertion) {
        requestUrlTextField.setText(assertion.getRequestUrl());
        methodComboBox.setSelectedItem(assertion.getHttpMethod());
        queryStringTextField.setText(assertion.getQueryString());
        allowCustomOAuthQueryParamsCheckBox.setSelected(assertion.isAllowCustomOAuthQueryParams());
        useMessageTargetAsCheckBox.setSelected(assertion.isUseMessageTarget());
        authHeaderCheckBox.setSelected(assertion.isUseAuthorizationHeader());
        authHeaderTextField.setText(assertion.getAuthorizationHeader());
        oauthConsumerKeyTextField.setText(assertion.getOauthConsumerKey());
        oauthSignatureMethodComboBox.setSelectedItem(assertion.getOauthSignatureMethod());
        oauthVersionCheckBox.setSelected(assertion.isUseOAuthVersion());
        oauthTokenTextField.setText(assertion.getOauthToken());
        oauthCallbackTextField.setText(assertion.getOauthCallback());
        oauthVerifierTextField.setText(assertion.getOauthVerifier());
        clientRadioButton.setSelected(assertion.getUsageMode().equals(GenerateOAuthSignatureBaseStringAssertion.UsageMode.CLIENT));
        serverRadioButton.setSelected(assertion.getUsageMode().equals(GenerateOAuthSignatureBaseStringAssertion.UsageMode.SERVER));
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        enableDisableComponents();
    }

    @Override
    public GenerateOAuthSignatureBaseStringAssertion getData(final GenerateOAuthSignatureBaseStringAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }

        final GenerateOAuthSignatureBaseStringAssertion.UsageMode usageMode = clientRadioButton.isSelected() ?
                GenerateOAuthSignatureBaseStringAssertion.UsageMode.CLIENT : GenerateOAuthSignatureBaseStringAssertion.UsageMode.SERVER;
        assertion.setUsageMode(usageMode);

        // server or client side
        assertion.setRequestUrl(requestUrlTextField.getText().trim());
        assertion.setHttpMethod(methodComboBox.getSelectedItem().toString());
        assertion.setVariablePrefix(targetVariablePanel.getVariable().trim());
        assertion.setQueryString(getTrimmedValueOrNull(queryStringTextField));
        assertion.setAllowCustomOAuthQueryParams(allowCustomOAuthQueryParamsCheckBox.isSelected());

        // server side only
        assertion.setUseMessageTarget(useMessageTargetAsCheckBox.isSelected());
        assertion.setUseAuthorizationHeader(authHeaderCheckBox.isSelected());
        assertion.setAuthorizationHeader(getTrimmedValueOrNull(authHeaderTextField));

        // client side only
        assertion.setUseOAuthVersion(oauthVersionCheckBox.isSelected());
        assertion.setOauthSignatureMethod(oauthSignatureMethodComboBox.getSelectedItem().toString());
        assertion.setOauthConsumerKey(getTrimmedValueOrNull(oauthConsumerKeyTextField));
        assertion.setOauthToken(getTrimmedValueOrNull(oauthTokenTextField));
        assertion.setOauthCallback(getTrimmedValueOrNull(oauthCallbackTextField));
        assertion.setOauthVerifier(getTrimmedValueOrNull(oauthVerifierTextField));
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.contentPane;
    }

    private String getTrimmedValueOrNull(final JTextField textField) {
        String value = null;
        if (!textField.getText().trim().isEmpty()) {
            value = textField.getText().trim();
        }
        return value;
    }

    private class QueryStringConsumerKeyRule implements InputValidator.ValidationRule {
        @Override
        public String getValidationError() {
            String error = null;
            if(clientRadioButton.isSelected() && queryStringTextField.getText().trim().isEmpty() &&
                    oauthConsumerKeyTextField.getText().isEmpty()){
                error = "Must specify Query string and/or oauth_consumer_key.";
            }
            return error;
        }
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
