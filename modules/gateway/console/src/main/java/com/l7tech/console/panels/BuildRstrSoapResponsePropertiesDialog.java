package com.l7tech.console.panels;

import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.BuildRstrSoapResponse;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * @author ghuang
 */
public class BuildRstrSoapResponsePropertiesDialog extends AssertionPropertiesEditorSupport<BuildRstrSoapResponse> {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JRadioButton responseForIssuanceRadioButton;
    private JRadioButton responseForCancelRadioButton;
    private JPanel rstrConfigPanel;
    private JTextField tokenIssuedTextField;
    private JCheckBox lifetimeCheckBox;
    private JFormattedTextField lifetimeTextField;
    private JComboBox lifetimeUnitComboBox;
    private JCheckBox attachedRefCheckBox;
    private JCheckBox unattachedRefCheckBox;
    private JCheckBox keySizeCheckBox;
    private JComboBox keySizeComboBox;
    private JTextField varPrefixTextField;

    private static final String AUTOMATIC_KEYSIZE_ITEM = "Automatic";
    private static final long MILLIS_100_YEARS = 100L * 365L * 86400L * 1000L;

    private BuildRstrSoapResponse assertion;
    private TimeUnit oldTimeUnit;
    private boolean confirmed;

    public BuildRstrSoapResponsePropertiesDialog(Window owner, BuildRstrSoapResponse assertion) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(BuildRstrSoapResponse assertion) {
        this.assertion = assertion;
    }

    @Override
    public BuildRstrSoapResponse getData(BuildRstrSoapResponse assertion) {
        viewToModel(assertion);
        return assertion;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onOK();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        DocumentListener validationListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                enableOrDisableOkButton();
            }
        });
        tokenIssuedTextField.getDocument().addDocumentListener(validationListener);

        responseForIssuanceRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Enable or disable the token-issued TextField and all RSTR configuration components
                enableOrDisableAllRstrComponents(responseForIssuanceRadioButton.isSelected());
                enableOrDisableOkButton();
            }
        });

        responseForCancelRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Enable or disable the token-issued TextField and all RSTR configuration components
                enableOrDisableAllRstrComponents(responseForIssuanceRadioButton.isSelected());
                enableOrDisableOkButton();
            }
        });

        lifetimeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                lifetimeTextField.setEnabled(lifetimeCheckBox.isSelected());
                lifetimeUnitComboBox.setEnabled(lifetimeCheckBox.isSelected());
                enableOrDisableOkButton();
            }
        });;

        lifetimeTextField.getDocument().addDocumentListener(validationListener);

        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.####"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum((double) 0);

        lifetimeTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });

        lifetimeUnitComboBox.setModel(new DefaultComboBoxModel(TimeUnit.ALL));
        lifetimeUnitComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit) lifetimeUnitComboBox.getSelectedItem();
                Double time = (Double) lifetimeTextField.getValue();

                if (newTimeUnit != null && oldTimeUnit != null && newTimeUnit != oldTimeUnit) {
                    double oldMillis = oldTimeUnit.getMultiplier() * time;
                    lifetimeTextField.setValue(oldMillis / newTimeUnit.getMultiplier());
                }

                enableOrDisableOkButton();
                oldTimeUnit = newTimeUnit;
            }
        });

        keySizeCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                keySizeComboBox.setEnabled(keySizeCheckBox.isSelected());
                enableOrDisableOkButton();
            }
        });;

        keySizeComboBox.setModel(new DefaultComboBoxModel(new Object[] {"", AUTOMATIC_KEYSIZE_ITEM, "128", "256", "512", "1024"}));
        ((JTextField)keySizeComboBox.getEditor().getEditorComponent()).setHorizontalAlignment(JTextField.RIGHT);
        ((JTextField)keySizeComboBox.getEditor().getEditorComponent()).setDocument(new MaxLengthDocument(10));
        ((JTextField)keySizeComboBox.getEditor().getEditorComponent()).getDocument().addDocumentListener(validationListener);

        modelToView();
    }

    private void enableOrDisableAllRstrComponents(boolean enabled) {
        rstrConfigPanel.setEnabled(enabled);

        tokenIssuedTextField.setEnabled(enabled);
        attachedRefCheckBox.setEnabled(enabled);
        unattachedRefCheckBox.setEnabled(enabled);

        lifetimeCheckBox.setEnabled(enabled);
        lifetimeTextField.setEnabled(lifetimeCheckBox.isSelected() && enabled);
        lifetimeUnitComboBox.setEnabled(lifetimeCheckBox.isSelected() && enabled);

        keySizeCheckBox.setEnabled(enabled);
        keySizeComboBox.setEnabled(keySizeCheckBox.isSelected() && enabled);
    }

    private void modelToView() {
        varPrefixTextField.setText(assertion.getVariablePrefix());

        // Check if the response is for token issuance or token cancellation.
        if (assertion.isResponseForIssuance()) {
            responseForIssuanceRadioButton.setSelected(true);

            // Enable the token-issued TextField and all RSTR configuration components
            enableOrDisableAllRstrComponents(true);
        } else {
            responseForCancelRadioButton.setSelected(true);

            // Disable the token-issued TextField and all RSTR configuration components
            enableOrDisableAllRstrComponents(false);

            return;
        }

        // Continue to set RSTR configuration since the response-for-issuance radio button is selected
        tokenIssuedTextField.setText(assertion.getTokenIssued());

        attachedRefCheckBox.setSelected(assertion.isIncludeAttachedRef());
        unattachedRefCheckBox.setSelected(assertion.isIncludeUnattachedRef());

        // Set enabled/disabled status for Lifetime and KeySize
        lifetimeCheckBox.setSelected(assertion.isIncludeLifetime());
        lifetimeTextField.setEnabled(assertion.isIncludeLifetime());
        lifetimeUnitComboBox.setEnabled(assertion.isIncludeLifetime());

        // Set their values first.  Note: determine their enabled or disabled status below later.  The reason why setting
        // values first is because we want to see what values these components have even though they are disabled.
        TimeUnit timeUnit = assertion.getTimeUnit();
        Double lifetime = (double) assertion.getLifetime();
        lifetimeTextField.setValue(lifetime / timeUnit.getMultiplier());
        lifetimeUnitComboBox.setSelectedItem(timeUnit);
        oldTimeUnit = timeUnit;

        keySizeCheckBox.setSelected(assertion.isIncludeKeySize());
        keySizeComboBox.setEnabled(assertion.isIncludeKeySize());

        if (assertion.getKeySize() == BuildRstrSoapResponse.AUTOMATIC_KEY_SIZE) {
            keySizeComboBox.setSelectedItem(AUTOMATIC_KEYSIZE_ITEM);
        } else {
            keySizeComboBox.setSelectedItem(Integer.toString(assertion.getKeySize()));
        }
    }

    private void viewToModel(BuildRstrSoapResponse assertion) {
        String prefix = varPrefixTextField.getText();
        if (prefix == null || prefix.trim().isEmpty()) prefix = BuildRstrSoapResponse.DEFAULT_VARIABLE_PREFIX;
        assertion.setVariablePrefix(prefix);

        assertion.setResponseForIssuance(responseForIssuanceRadioButton.isSelected());

        if (responseForIssuanceRadioButton.isSelected()) {
            assertion.setTokenIssued(tokenIssuedTextField.getText());

            assertion.setIncludeLifetime(lifetimeCheckBox.isSelected());
            if (lifetimeCheckBox.isSelected()) {
                TimeUnit timeUnit = (TimeUnit) lifetimeUnitComboBox.getSelectedItem();
                Double lifetime = (Double) lifetimeTextField.getValue();
                assertion.setTimeUnit(timeUnit);
                assertion.setLifetime((long)(lifetime * timeUnit.getMultiplier()));
            }

            assertion.setIncludeAttachedRef(attachedRefCheckBox.isSelected());
            assertion.setIncludeUnattachedRef(unattachedRefCheckBox.isSelected());

            assertion.setIncludeKeySize(keySizeCheckBox.isSelected());
            if (keySizeCheckBox.isSelected()) {
                assertion.setIncludeKeySize(true);

                String value = (String) keySizeComboBox.getEditor().getItem();
                if (AUTOMATIC_KEYSIZE_ITEM.equals(value)) {
                    assertion.setKeySize(BuildRstrSoapResponse.AUTOMATIC_KEY_SIZE);
                } else {
                    assertion.setKeySize(Integer.parseInt(value));
                }
            }
        }
    }

    /**
     * Validate all RSTR components and enable/disable the OK button to indicate the validation result.
     */
    private void enableOrDisableOkButton() {
        // If the response is for token cancellation, then enable the OK button.
        if (responseForCancelRadioButton.isSelected()) {
            okButton.setEnabled(true);
            return;
        }
        // Else if the response is for token issuance, then check the following each component.

        // Check token variable
        String tokenIssued = tokenIssuedTextField.getText();
        if (tokenIssued == null || tokenIssued.trim().isEmpty()) {
            okButton.setEnabled(false);
            return;
        }

        // Check Lifetime
        if (lifetimeCheckBox.isSelected()) {
            int multiplier = ((TimeUnit) lifetimeUnitComboBox.getSelectedItem()).getMultiplier();
            boolean lifetimeStatusOk = ValidationUtils.isValidDouble(lifetimeTextField.getText().trim(), false, 0, MILLIS_100_YEARS  / multiplier);
            if (! lifetimeStatusOk) {
                okButton.setEnabled(false);
                return;
            }
        }

        // Check KeySize
        if (keySizeCheckBox.isSelected()) {
            boolean keySizeStatusOk;
            try {
                String value = ((JTextField) keySizeComboBox.getEditor().getEditorComponent()).getText();
                if (value == null || value.trim().isEmpty()) {
                    keySizeStatusOk = false;
                } else if (AUTOMATIC_KEYSIZE_ITEM.equals(value)) {
                    keySizeStatusOk = true;
                } else {
                    Integer.parseInt(value);
                    keySizeStatusOk = true;
                }
            } catch (NumberFormatException e) {
                keySizeStatusOk = false;
            }

            if (! keySizeStatusOk) {
                okButton.setEnabled(false);
                return;
            }
        }

        // Finally everything is OK, then set the OK button as enabled.
        okButton.setEnabled(true);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
