package com.l7tech.external.assertions.apiportalintegration.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.assertion.AssertionMetadata;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.regex.Pattern;

public class ApiPortalIntegrationPropertiesDialog extends AssertionPropertiesOkCancelSupport<ApiPortalIntegrationAssertion> {
    public ApiPortalIntegrationPropertiesDialog(final Frame parent, final ApiPortalIntegrationAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty(API_ID, apiIdTextField, null);
        validators.addRule(new AlphanumericOrHyphen(API_ID, apiIdTextField));
        validators.addRule(new AlphanumericOrHyphen(API_GROUP, apiGroupTextField));
        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        variablePrefixPanel.setLayout(new BorderLayout());
        variablePrefixPanel.add(targetVariablePanel, BorderLayout.CENTER);
    }

    @Override
    public void setData(final ApiPortalIntegrationAssertion assertion) {
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
        apiIdTextField.setText(assertion.getApiId());
        apiGroupTextField.setText(assertion.getApiGroup());
    }

    @Override
    public ApiPortalIntegrationAssertion getData(final ApiPortalIntegrationAssertion assertion) throws ValidationException {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
        assertion.setPortalManagedApiFlag(ModuleConstants.TEMP_PORTAL_MANAGED_SERVICE_INDICATOR);
        assertion.setApiId(apiIdTextField.getText().trim());
        assertion.setApiGroup(apiGroupTextField.getText().trim());
        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private static final Pattern ALPHANUMERIC_OR_HYPHEN = Pattern.compile("^[a-zA-Z0-9-]*$");
    private static final String API_ID = "API ID";
    private static final String API_GROUP = "API Group";
    private JPanel contentPane;
    private JTextField apiIdTextField;
    private JTextField apiGroupTextField;
    private JPanel variablePrefixPanel;
    private InputValidator validators;
    private TargetVariablePanel targetVariablePanel;

    private class AlphanumericOrHyphen implements InputValidator.ValidationRule {
        final String fieldName;
        final JTextField textField;

        private AlphanumericOrHyphen(@NotNull final String fieldName, @NotNull final JTextField textField) {
            this.fieldName = fieldName;
            this.textField = textField;
        }

        @Override
        public String getValidationError() {
            return ALPHANUMERIC_OR_HYPHEN.matcher(textField.getText().trim()).matches() ? null : fieldName + " must contain only alphanumeric characters or '-'.";
        }
    }
}
