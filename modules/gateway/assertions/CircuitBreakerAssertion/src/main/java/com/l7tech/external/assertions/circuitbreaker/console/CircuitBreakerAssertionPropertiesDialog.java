package com.l7tech.external.assertions.circuitbreaker.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.policy.variable.Syntax;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 *
 * Properties dialog for circuit breaker. Opens when added new CB to policy in policy editor or right click and select properties.
 *
 */
public class CircuitBreakerAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<CircuitBreakerAssertion> {

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.circuitbreaker.console.CircuitBreakerAssertionPropertiesDialog");

    private JPanel contentPane;
    private JTextField policyFailureCircuitMaxFailuresTextField;
    private JTextField latencyCircuitMaxFailuresTextField;
    private JTextField policyFailureCircuitSamplingWindowTextField;
    private JTextField latencyCircuitMaxLatencyTextField;
    private JTextField policyFailureCircuitRecoveryPeriodTextField;
    private JTextField latencyCircuitSamplingWindowTextField;
    private JTextField latencyCircuitRecoveryPeriodTextField;
    private JCheckBox policyFailureCircuitEnabledCheckbox;
    private JCheckBox latencyCircuitEnabledCheckbox;
    private JTextField policyFailureCircuitEventTrackerIDTextField;
    private JTextField latencyCircuitEventTrackerIDTextField;
    private JCheckBox policyFailureCircuitCustomTrackerIdEnabledCheckBox;
    private JCheckBox latencyCircuitCustomTrackerIdEnabledCheckBox;

    private InputValidator inputValidator;

    @SuppressWarnings("WeakerAccess")   // must be public to be invoked by the default AssertionPropertiesEditor factory
    public CircuitBreakerAssertionPropertiesDialog(final Frame owner, final CircuitBreakerAssertion assertion) {
        super(CircuitBreakerAssertion.class, owner, assertion, true);

        initComponents();
        setData(assertion);
        updateEnableStateForPolicyFailureCircuit();
        updateEnableStateForLatencyCircuit();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        inputValidator = new InputValidator(this, getTitle());

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("policyFailureCircuitMaxFailuresLabel"), policyFailureCircuitMaxFailuresTextField,
                getOnlyOneContextVariableValidationRule(policyFailureCircuitMaxFailuresTextField, resources.getString("policyFailureCircuitMaxFailuresLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("policyFailureCircuitSamplingWindowLabel"), policyFailureCircuitSamplingWindowTextField,
                getOnlyOneContextVariableValidationRule(policyFailureCircuitSamplingWindowTextField, resources.getString("policyFailureCircuitSamplingWindowLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("policyFailureCircuitRecoveryPeriodLabel"), policyFailureCircuitRecoveryPeriodTextField,
                getOnlyOneContextVariableValidationRule(policyFailureCircuitRecoveryPeriodTextField, resources.getString("policyFailureCircuitRecoveryPeriodLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("policyFailureCircuitEventTrackerIDLabel"), policyFailureCircuitEventTrackerIDTextField, null);

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("latencyCircuitMaxFailuresLabel"), latencyCircuitMaxFailuresTextField,
                getOnlyOneContextVariableValidationRule(latencyCircuitMaxFailuresTextField, resources.getString("latencyCircuitMaxFailuresLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("latencyCircuitMaxLatencyLabel"), latencyCircuitMaxLatencyTextField,
                getOnlyOneContextVariableValidationRule(latencyCircuitMaxLatencyTextField, resources.getString("latencyCircuitMaxLatencyLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("latencyCircuitSamplingWindowLabel"), latencyCircuitSamplingWindowTextField,
                getOnlyOneContextVariableValidationRule(latencyCircuitSamplingWindowTextField, resources.getString("latencyCircuitSamplingWindowLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("latencyCircuitRecoveryPeriodLabel"), latencyCircuitRecoveryPeriodTextField,
                getOnlyOneContextVariableValidationRule(latencyCircuitRecoveryPeriodTextField, resources.getString("latencyCircuitRecoveryPeriodLabel")));

        inputValidator.constrainTextFieldToBeNonEmpty(resources.getString("latencyCircuitEventTrackerIDLabel"), latencyCircuitEventTrackerIDTextField, null);

        final ChangeListener enablementListenerForPolicyFailure = e -> updateEnableStateForPolicyFailureCircuit();
        final ChangeListener enablementListenerForLatencyFailure = e -> updateEnableStateForLatencyCircuit();

        final ChangeListener enablementListenerForPolicyFailureCircuitCustomTrackerId = e -> updatePolicyFailureCircuitCustomTrackerIdEnableState();
        final ChangeListener enablementListenerForLatencyCircuitCustomTrackerId = e -> updateLatencyCircuitCustomTrackerIdEnableState();

        policyFailureCircuitEnabledCheckbox.addChangeListener(enablementListenerForPolicyFailure);
        latencyCircuitEnabledCheckbox.addChangeListener(enablementListenerForLatencyFailure);

        policyFailureCircuitCustomTrackerIdEnabledCheckBox.addChangeListener(enablementListenerForPolicyFailureCircuitCustomTrackerId);
        latencyCircuitCustomTrackerIdEnabledCheckBox.addChangeListener(enablementListenerForLatencyCircuitCustomTrackerId);
    }

    @NotNull
    private InputValidator.ValidationRule getOnlyOneContextVariableValidationRule(final JTextField textField, final String label) {
        return () -> {
            String msg = MessageFormat.format(InputValidator.MUST_BE_NUMERIC, label, 1, Integer.MAX_VALUE);
            if (Syntax.isAnyVariableReferenced(textField.getText())) {
                if (Syntax.isOnlyASingleVariableReferenced(textField.getText())) {
                    return null;
                } else {
                    return MessageFormat.format(resources.getString("errorMessageFieldWithOneContextVariableAllowed"), label);
                }
            } else {
                int intValue;
                try {
                    intValue = Integer.parseInt(textField.getText());
                } catch(NumberFormatException nfe) {
                    return msg;
                }
                if (intValue < 1) {
                    return msg;
                } else {
                    return null;
                }
            }
        };
    }

    @Override
    public void setData(CircuitBreakerAssertion assertion) {
        policyFailureCircuitEnabledCheckbox.setSelected(assertion.isPolicyFailureCircuitEnabled());
        policyFailureCircuitEventTrackerIDTextField.setText(assertion.getPolicyFailureCircuitTrackerId());
        policyFailureCircuitCustomTrackerIdEnabledCheckBox.setSelected(assertion.isPolicyFailureCircuitCustomTrackerIdEnabled());
        policyFailureCircuitEventTrackerIDTextField.setEnabled(assertion.isPolicyFailureCircuitCustomTrackerIdEnabled());
        policyFailureCircuitMaxFailuresTextField.setText(assertion.getPolicyFailureCircuitMaxFailures());
        policyFailureCircuitSamplingWindowTextField.setText(assertion.getPolicyFailureCircuitSamplingWindow());
        policyFailureCircuitRecoveryPeriodTextField.setText(assertion.getPolicyFailureCircuitRecoveryPeriod());

        latencyCircuitEnabledCheckbox.setSelected(assertion.isLatencyCircuitEnabled());
        latencyCircuitEventTrackerIDTextField.setText(assertion.getLatencyCircuitTrackerId());  //display tracker id value always.
        latencyCircuitCustomTrackerIdEnabledCheckBox.setSelected(assertion.isLatencyCircuitCustomTrackerIdEnabled());
        latencyCircuitEventTrackerIDTextField.setEnabled(assertion.isLatencyCircuitCustomTrackerIdEnabled());
        latencyCircuitMaxFailuresTextField.setText(assertion.getLatencyCircuitMaxFailures());
        latencyCircuitRecoveryPeriodTextField.setText(assertion.getLatencyCircuitRecoveryPeriod());
        latencyCircuitSamplingWindowTextField.setText(assertion.getLatencyCircuitSamplingWindow());
        latencyCircuitMaxLatencyTextField.setText(assertion.getLatencyCircuitMaxLatency());
    }

    @Override
    public CircuitBreakerAssertion getData(CircuitBreakerAssertion assertion) throws ValidationException {
        final String error = inputValidator.validate();

        if (null != error) {
            throw new ValidationException(error);
        }

        assertion.setPolicyFailureCircuitEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
        assertion.setLatencyCircuitEnabled(latencyCircuitEnabledCheckbox.isSelected());
        assertion.setPolicyFailureCircuitCustomTrackerIdEnabled(policyFailureCircuitCustomTrackerIdEnabledCheckBox.isSelected());
        assertion.setLatencyCircuitCustomTrackerIdEnabled(latencyCircuitCustomTrackerIdEnabledCheckBox.isSelected());

        assertion.setPolicyFailureCircuitTrackerId(policyFailureCircuitEventTrackerIDTextField.getText());
        assertion.setPolicyFailureCircuitMaxFailures(policyFailureCircuitMaxFailuresTextField.getText());
        assertion.setPolicyFailureCircuitSamplingWindow(policyFailureCircuitSamplingWindowTextField.getText());
        assertion.setPolicyFailureCircuitRecoveryPeriod(policyFailureCircuitRecoveryPeriodTextField.getText());

        assertion.setLatencyCircuitTrackerId(latencyCircuitEventTrackerIDTextField.getText());
        assertion.setLatencyCircuitMaxFailures(latencyCircuitMaxFailuresTextField.getText());
        assertion.setLatencyCircuitMaxLatency(latencyCircuitMaxLatencyTextField.getText());
        assertion.setLatencyCircuitSamplingWindow(latencyCircuitSamplingWindowTextField.getText());
        assertion.setLatencyCircuitRecoveryPeriod(latencyCircuitRecoveryPeriodTextField.getText());

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void updateEnableStateForPolicyFailureCircuit() {
            policyFailureCircuitMaxFailuresTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
            policyFailureCircuitSamplingWindowTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
            policyFailureCircuitRecoveryPeriodTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
            policyFailureCircuitCustomTrackerIdEnabledCheckBox.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
            updatePolicyFailureCircuitCustomTrackerIdEnableState();
    }

    private void updateEnableStateForLatencyCircuit() {
        latencyCircuitMaxFailuresTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitMaxLatencyTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitSamplingWindowTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitRecoveryPeriodTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitCustomTrackerIdEnabledCheckBox.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        updateLatencyCircuitCustomTrackerIdEnableState();
    }

    private void updatePolicyFailureCircuitCustomTrackerIdEnableState() {
        policyFailureCircuitEventTrackerIDTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected() && policyFailureCircuitCustomTrackerIdEnabledCheckBox.isSelected());
    }

    private void updateLatencyCircuitCustomTrackerIdEnableState() {
        latencyCircuitEventTrackerIDTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected() && latencyCircuitCustomTrackerIdEnabledCheckBox.isSelected());
    }
}
