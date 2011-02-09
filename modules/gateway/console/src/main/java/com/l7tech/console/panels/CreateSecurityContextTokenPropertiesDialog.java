package com.l7tech.console.panels;

import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.CreateSecurityContextToken;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;

/**
 * @author ghuang
 */
public class CreateSecurityContextTokenPropertiesDialog extends AssertionPropertiesEditorSupport<CreateSecurityContextToken> {
    private static final String AUTOMATIC_KEYSIZE_ITEM = "Automatic";

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JFormattedTextField lifetimeTextField;
    private JComboBox lifetimeUnitComboBox;
    private JCheckBox useSystemDefaultCheckBox;
    private JComboBox keySizeComboBox;
    private JPanel varPrefixPanel;
    private TargetVariablePanel varPrefixTextField;

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


        varPrefixTextField = new TargetVariablePanel();
        varPrefixPanel.setLayout(new BorderLayout());
        varPrefixPanel.add(varPrefixTextField, BorderLayout.CENTER);
        varPrefixTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                enableOrDisableOkButton();
            }
        });

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

        keySizeComboBox.setModel(new DefaultComboBoxModel(new Object[] {AUTOMATIC_KEYSIZE_ITEM, "128", "256", "512", "1024"}));
        ((JTextField)keySizeComboBox.getEditor().getEditorComponent()).setHorizontalAlignment(JTextField.RIGHT);
        ((JTextField)keySizeComboBox.getEditor().getEditorComponent()).setDocument(new MaxLengthDocument(10));

        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.#########"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum((double) 0);
        
        lifetimeTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
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

        useSystemDefaultCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean defaultSelected = useSystemDefaultCheckBox.isSelected();
                lifetimeTextField.setEnabled(! defaultSelected);
                lifetimeUnitComboBox.setEnabled(! defaultSelected);
                enableOrDisableOkButton();
            }
        });



        modelToView();
    }

    private void modelToView() {

        varPrefixTextField.setSuffixes(assertion.getVariableSuffixes());
        varPrefixTextField.setVariable(assertion.getVariablePrefix());
        varPrefixTextField.setAssertion(assertion,getPreviousAssertion());

        keySizeComboBox.setSelectedItem(Integer.toString(assertion.getKeySize()));
        if ( keySizeComboBox.getSelectedItem() == null ) {
            keySizeComboBox.setSelectedItem(AUTOMATIC_KEYSIZE_ITEM);
        }

        TimeUnit timeUnit = assertion.getTimeUnit();
        Double lifetime = (double) assertion.getLifetime();
        lifetimeTextField.setValue(lifetime / timeUnit.getMultiplier());
        lifetimeUnitComboBox.setSelectedItem(timeUnit);
        oldTimeUnit = timeUnit;

        boolean defaultSelected = assertion.isUseSystemDefaultSessionDuration();
        useSystemDefaultCheckBox.setSelected(defaultSelected);
        lifetimeTextField.setEnabled(! defaultSelected);
        lifetimeUnitComboBox.setEnabled(! defaultSelected);
    }

    private void viewToModel(CreateSecurityContextToken assertion) {
        boolean defaultSelected = useSystemDefaultCheckBox.isSelected();
        assertion.setUseSystemDefaultSessionDuration(defaultSelected);
        if (! defaultSelected) {
            TimeUnit timeUnit = (TimeUnit) lifetimeUnitComboBox.getSelectedItem();
            Double lifetime = (Double) lifetimeTextField.getValue();
            assertion.setTimeUnit(timeUnit);
            assertion.setLifetime((long)(lifetime * timeUnit.getMultiplier()));
        }

        String value = (String) keySizeComboBox.getSelectedItem();
        if (AUTOMATIC_KEYSIZE_ITEM.equals(value)) {
            assertion.setKeySize( 0 );
        } else {
            assertion.setKeySize(Integer.parseInt(value));
        }

        String prefix = varPrefixTextField.getVariable();
        if (prefix == null || prefix.trim().isEmpty()) prefix = CreateSecurityContextToken.DEFAULT_VARIABLE_PREFIX;
        assertion.setVariablePrefix(prefix);
    }

    private void enableOrDisableOkButton() {
        int multiplier = ((TimeUnit) lifetimeUnitComboBox.getSelectedItem()).getMultiplier();
        boolean validLifetime = useSystemDefaultCheckBox.isSelected() ||
            ValidationUtils.isValidDouble(lifetimeTextField.getText().trim(), false,
                formatDouble((double)CreateSecurityContextToken.MIN_SESSION_DURATION / multiplier), true,
                formatDouble((double)CreateSecurityContextToken.MAX_SESSION_DURATION / multiplier), true);

        okButton.setEnabled(varPrefixTextField.isEntryValid() && validLifetime);
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    private double formatDouble(double doubleNum) {
        DecimalFormat formatter = new DecimalFormat("0.#########");
        return Double.parseDouble(formatter.format(doubleNum));
    }
}
