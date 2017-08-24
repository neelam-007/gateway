package com.l7tech.external.assertions.circuitbreaker.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
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

    private InputValidator inputValidator;

    @SuppressWarnings("WeakerAccess")   // must be public to be invoked by the default AssertionPropertiesEditor factory
    public CircuitBreakerAssertionPropertiesDialog(final Frame owner, final CircuitBreakerAssertion assertion) {
        super(CircuitBreakerAssertion.class, owner, assertion, true);

        initComponents();
        setData(assertion);
        updateEnableState();
    }

    @Override
    protected void initComponents() {
        // TODO Make use of buildTextFieldContextVariableValidationRule() from JmsRoutingAssertionDialog, for context variable check, once introduced in 12.4 sprint..
        super.initComponents();

        inputValidator = new InputValidator(this, getTitle());
        inputValidator.constrainTextFieldToNumberRange(resources.getString("policyFailureCircuitMaxFailuresLabel"), policyFailureCircuitMaxFailuresTextField, 1, Integer.MAX_VALUE);
        inputValidator.constrainTextFieldToNumberRange(resources.getString("policyFailureCircuitSamplingWindowLabel"), policyFailureCircuitSamplingWindowTextField, 1, Integer.MAX_VALUE);
        inputValidator.constrainTextFieldToNumberRange(resources.getString("policyFailureCircuitRecoveryPeriodLabel"), policyFailureCircuitRecoveryPeriodTextField, 1, Integer.MAX_VALUE);

        inputValidator.constrainTextFieldToNumberRange(resources.getString("latencyCircuitMaxFailuresLabel"), latencyCircuitMaxFailuresTextField, 1, Integer.MAX_VALUE);
        inputValidator.constrainTextFieldToNumberRange(resources.getString("latencyCircuitMaxLatencyLabel"), latencyCircuitMaxLatencyTextField, 1, Integer.MAX_VALUE);
        inputValidator.constrainTextFieldToNumberRange(resources.getString("latencyCircuitSamplingWindowLabel"), latencyCircuitSamplingWindowTextField, 1, Integer.MAX_VALUE);
        inputValidator.constrainTextFieldToNumberRange(resources.getString("latencyCircuitRecoveryPeriodLabel"), latencyCircuitRecoveryPeriodTextField, 1, Integer.MAX_VALUE);

        inputValidator.addRule(() -> {
            if (!policyFailureCircuitEnabledCheckbox.isSelected() && !latencyCircuitEnabledCheckbox.isSelected()) {
                return resources.getString("neitherCircuitEnabledError");
            }
            return null;
        });

        final ChangeListener enablementListener = e -> updateEnableState();

        policyFailureCircuitEnabledCheckbox.addChangeListener(enablementListener);
        latencyCircuitEnabledCheckbox.addChangeListener(enablementListener);
    }

    @Override
    public void setData(CircuitBreakerAssertion assertion) {
        policyFailureCircuitEnabledCheckbox.setSelected(assertion.isPolicyFailureCircuitEnabled());
        latencyCircuitEnabledCheckbox.setSelected(assertion.isLatencyCircuitEnabled());

        policyFailureCircuitMaxFailuresTextField.setText(Integer.toString(assertion.getPolicyFailureCircuitMaxFailures()));
        policyFailureCircuitSamplingWindowTextField.setText(Integer.toString(assertion.getPolicyFailureCircuitSamplingWindow()));
        policyFailureCircuitRecoveryPeriodTextField.setText(Integer.toString(assertion.getPolicyFailureCircuitRecoveryPeriod()));
        latencyCircuitMaxFailuresTextField.setText(Integer.toString(assertion.getLatencyCircuitMaxFailures()));
        latencyCircuitRecoveryPeriodTextField.setText(Integer.toString(assertion.getLatencyCircuitRecoveryPeriod()));
        latencyCircuitSamplingWindowTextField.setText(Integer.toString(assertion.getLatencyCircuitSamplingWindow()));
        latencyCircuitMaxLatencyTextField.setText(Integer.toString(assertion.getLatencyCircuitMaxLatency()));
    }

    @Override
    public CircuitBreakerAssertion getData(CircuitBreakerAssertion assertion) throws ValidationException {
        final String error = inputValidator.validate();

        if (null != error) {
            throw new ValidationException(error);
        }

        assertion.setPolicyFailureCircuitEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
        assertion.setLatencyCircuitEnabled(latencyCircuitEnabledCheckbox.isSelected());

        try {
            if (assertion.isPolicyFailureCircuitEnabled()) {
                assertion.setPolicyFailureCircuitMaxFailures(Integer.valueOf(policyFailureCircuitMaxFailuresTextField.getText()));
                assertion.setPolicyFailureCircuitSamplingWindow(Integer.valueOf(policyFailureCircuitSamplingWindowTextField.getText()));
                assertion.setPolicyFailureCircuitRecoveryPeriod(Integer.valueOf(policyFailureCircuitRecoveryPeriodTextField.getText()));
            }

            if (assertion.isLatencyCircuitEnabled()) {
                assertion.setLatencyCircuitMaxFailures(Integer.valueOf(latencyCircuitMaxFailuresTextField.getText()));
                assertion.setLatencyCircuitMaxLatency(Integer.valueOf(latencyCircuitMaxLatencyTextField.getText()));
                assertion.setLatencyCircuitSamplingWindow(Integer.valueOf(latencyCircuitSamplingWindowTextField.getText()));
                assertion.setLatencyCircuitRecoveryPeriod(Integer.valueOf(latencyCircuitRecoveryPeriodTextField.getText()));
            }
        } catch (NumberFormatException nfe) {
            throw new ValidationException("Please enter a valid number.");
        }

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void updateEnableState() {
        policyFailureCircuitMaxFailuresTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
        policyFailureCircuitSamplingWindowTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());
        policyFailureCircuitRecoveryPeriodTextField.setEnabled(policyFailureCircuitEnabledCheckbox.isSelected());

        latencyCircuitMaxFailuresTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitMaxLatencyTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitSamplingWindowTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
        latencyCircuitRecoveryPeriodTextField.setEnabled(latencyCircuitEnabledCheckbox.isSelected());
    }
}
