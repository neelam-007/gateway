package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.apiportalintegration.LookupApiKeyAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

/**
 * Dialog for LookupApiKeyAssertion properties.
 */
public class LookupApiKeyPropertiesDialog extends AssertionPropertiesOkCancelSupport<LookupApiKeyAssertion> {
    private JPanel contentPane;
    private JTextField apiKey;
    private JPanel variablePrefixPanel;
    private JTextField serviceIdTextField;
    private InputValidator validators;
    private TargetVariablePanel targetVariablePanel;

    public LookupApiKeyPropertiesDialog(final Frame parent, final LookupApiKeyAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    public void initComponents() {
        super.initComponents();

        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        variablePrefixPanel.setLayout(new BorderLayout());
        variablePrefixPanel.add(targetVariablePanel, BorderLayout.CENTER);

        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty("apiKey", apiKey, null);
    }

    @Override
    public void setData(final LookupApiKeyAssertion assertion) {
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        apiKey.setText(assertion.getApiKey());
        serviceIdTextField.setText(assertion.getServiceId());
    }

    @Override
    public LookupApiKeyAssertion getData(final LookupApiKeyAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        assertion.setApiKey(apiKey.getText().trim());
        assertion.setVariablePrefix(targetVariablePanel.getVariable().trim());
        assertion.setServiceId(serviceIdTextField.getText().trim());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
