package com.l7tech.console.panels;

import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.CreateSecurityContextToken;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * @author ghuang
 */
public class CreateSecurityContextTokenPropertiesDialog extends AssertionPropertiesEditorSupport<CreateSecurityContextToken> {
    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JFormattedTextField lifetimeTextField;
    private JComboBox lifetimeUnitComboBox;
    private JTextField varPrefixTextField;
    private JLabel varPrefixStatusLabel;

    private static final long MILLIS_100_YEARS = 100L * 365L * 86400L * 1000L;

    private CreateSecurityContextToken assertion;
    private TimeUnit oldTimeUnit;
    private boolean confirmed;

    public CreateSecurityContextTokenPropertiesDialog(Window owner, CreateSecurityContextToken assertion) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(CreateSecurityContextToken assertion) {
        this.assertion = assertion;
    }

    @Override
    public CreateSecurityContextToken getData(CreateSecurityContextToken assertion) {
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

        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.#########"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum((double) 0);
        
        lifetimeTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });

        lifetimeTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enableOrDisableOkButton();
                    }
                });
            }
        }));

        lifetimeUnitComboBox.setModel(new DefaultComboBoxModel(TimeUnit.ALL));
        lifetimeUnitComboBox.addActionListener(new ActionListener() {
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

        varPrefixTextField.setDocument(new MaxLengthDocument(128));
        TextComponentPauseListenerManager.registerPauseListener(
            varPrefixTextField,
            new PauseListener() {
                @Override
                public void textEntryPaused(JTextComponent component, long msecs) {
                    if(validateVariablePrefix()) {
                        okButton.setEnabled(!isReadOnly());
                    } else {
                        okButton.setEnabled(false);

                        // Check if the status label gets a long text to display.
                        if (varPrefixStatusLabel.getMinimumSize().width + 30 > mainPanel.getMinimumSize().width) {
                            setSize(varPrefixStatusLabel.getMinimumSize().width + 30, getSize().height);
                        }
                    }
                }

                @Override
                public void textEntryResumed(JTextComponent component) {
                    clearVariablePrefixStatus();
                }
            },
            500
        );

        modelToView();
    }

    private void modelToView() {
        clearVariablePrefixStatus();
        varPrefixTextField.setText(assertion.getVariablePrefix());
        validateVariablePrefix();

        TimeUnit timeUnit = assertion.getTimeUnit();
        Double lifetime = (double) assertion.getLifetime();
        lifetimeTextField.setValue(lifetime / timeUnit.getMultiplier());
        lifetimeUnitComboBox.setSelectedItem(timeUnit);

        oldTimeUnit = timeUnit;
    }

    private void viewToModel(CreateSecurityContextToken assertion) {
        TimeUnit timeUnit = (TimeUnit) lifetimeUnitComboBox.getSelectedItem();
        Double lifetime = (Double) lifetimeTextField.getValue();
        assertion.setTimeUnit(timeUnit);
        assertion.setLifetime((long)(lifetime * timeUnit.getMultiplier()));

        String prefix = varPrefixTextField.getText();
        if (prefix == null || prefix.trim().isEmpty()) prefix = CreateSecurityContextToken.DEFAULT_VARIABLE_PREFIX;
        assertion.setVariablePrefix(prefix);
    }

    private void enableOrDisableOkButton() {
        int multiplier = ((TimeUnit) lifetimeUnitComboBox.getSelectedItem()).getMultiplier();
        boolean enabled = ValidationUtils.isValidDouble(lifetimeTextField.getText().trim(), false, 0, false, MILLIS_100_YEARS  / multiplier, true)
            && validateVariablePrefix();
        okButton.setEnabled(enabled);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private void clearVariablePrefixStatus() {
        VariablePrefixUtil.clearVariablePrefixStatus(varPrefixStatusLabel);
    }

    private boolean validateVariablePrefix() {
        return VariablePrefixUtil.validateVariablePrefix(
            varPrefixTextField.getText(),
            SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet(),
            assertion.getVariableSuffixes(),
            varPrefixStatusLabel);
    }
}