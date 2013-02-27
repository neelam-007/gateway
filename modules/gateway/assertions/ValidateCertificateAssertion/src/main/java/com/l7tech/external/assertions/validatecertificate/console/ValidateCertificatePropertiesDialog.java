package com.l7tech.external.assertions.validatecertificate.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.validatecertificate.ValidateCertificateAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.types.CertificateValidationType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Locale;
import java.util.ResourceBundle;

public class ValidateCertificatePropertiesDialog extends AssertionPropertiesOkCancelSupport<ValidateCertificateAssertion> {
    // re-use labels from ManageCertificateValidationDialog
    private static final ResourceBundle resource =
            ResourceBundle.getBundle("com.l7tech.console.resources.ManageCertificateValidationDialog", Locale.getDefault());
    private static final String SOURCE_VARIABLE = "Source Variable";
    private static final String VALIDATION_VALUE_VALIDATE = "validation.value.validate";
    private static final String VALIDATION_VALUE_VALIDATEPATH = "validation.value.validatepath";
    private static final String VALIDATION_VALUE_REVOCATION = "validation.value.revocation";
    private JPanel contentPanel;
    private JCheckBox failCheckBox;
    private JComboBox valTypeComboBox;
    private JTextField srcVarTextField;
    private JPanel variablePrefixPanel;
    private JLabel warningLabel;
    private TargetVariablePanel targetVariablePanel;
    private InputValidator validators;

    public ValidateCertificatePropertiesDialog(final Frame parent, final ValidateCertificateAssertion assertion) {
        super(assertion.getClass(), parent, (String) assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME), true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        super.initComponents();
        warningLabel.setText(ValidateCertificateAssertion.DISCLAIMER);
        for (final CertificateValidationType type : CertificateValidationType.values()) {
            valTypeComboBox.addItem(type);
        }
        valTypeComboBox.setRenderer(new TextListCellRenderer<CertificateValidationType>(
                new Functions.Unary<String, CertificateValidationType>() {
                    @Override
                    public String call(final CertificateValidationType type) {
                        switch (type) {
                            case CERTIFICATE_ONLY:
                                return resource.getString(VALIDATION_VALUE_VALIDATE);
                            case PATH_VALIDATION:
                                return resource.getString(VALIDATION_VALUE_VALIDATEPATH);
                            case REVOCATION:
                                return resource.getString(VALIDATION_VALUE_REVOCATION);
                            default:
                                throw new IllegalArgumentException("Unsupported CertificationValidationType: " + type);
                        }
                    }
                }, null, true));
        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent e) {
                getOkButton().setEnabled(targetVariablePanel.isEntryValid());
            }
        });
        variablePrefixPanel.setLayout(new BorderLayout());
        variablePrefixPanel.add(targetVariablePanel, BorderLayout.CENTER);
        validators = new InputValidator(this, getTitle());
        validators.constrainTextFieldToBeNonEmpty(SOURCE_VARIABLE, srcVarTextField, null);
        validators.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return VariableMetadata.validateName(srcVarTextField.getText().trim());
            }
        });
    }

    @Override
    public void setData(@NotNull final ValidateCertificateAssertion assertion) {
        failCheckBox.setSelected(!assertion.isLogOnly());
        valTypeComboBox.setSelectedItem(assertion.getValidationType());
        srcVarTextField.setText(assertion.getSourceVariable());
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());
        targetVariablePanel.setVariable(assertion.getVariablePrefix());
    }

    @Override
    public ValidateCertificateAssertion getData(@NotNull final ValidateCertificateAssertion assertion) throws ValidationException {
        validateInput();
        assertion.setLogOnly(!failCheckBox.isSelected());
        assertion.setValidationType((CertificateValidationType) valTypeComboBox.getSelectedItem());
        assertion.setSourceVariable(srcVarTextField.getText().trim());
        assertion.setVariablePrefix(targetVariablePanel.getVariable());
        return assertion;
    }

    private void validateInput() {
        final String error = validators.validate();
        if (error != null) {
            throw new ValidationException(error);
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPanel;
    }
}
