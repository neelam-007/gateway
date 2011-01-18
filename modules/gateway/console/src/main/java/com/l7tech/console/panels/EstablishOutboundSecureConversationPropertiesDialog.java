package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.EstablishOutboundSecureConversation;
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
public class EstablishOutboundSecureConversationPropertiesDialog extends AssertionPropertiesEditorSupport<EstablishOutboundSecureConversation> {
    private JPanel mainPanel;
    private JTextField serviceUrlTextField;
    private JTextField sctVarNameTextField;
    private JTextField clientEntropyTextField;
    private JTextField serverEntropyTextField;
    private JTextField fullKeyTextField;
    private JTextField creationTimeTextField;
    private JTextField expirationTimeTextField;
    private JFormattedTextField maxLifetimeTextField;
    private JComboBox maxLifetimeUnitComboBox;
    private JButton okButton;
    private JButton cancelButton;
    private JCheckBox useSystemDefaultCheckBox;

    private EstablishOutboundSecureConversation assertion;
    private TimeUnit oldTimeUnit;
    private boolean confirmed;

    public EstablishOutboundSecureConversationPropertiesDialog(Window owner, EstablishOutboundSecureConversation assertion) {
        super(owner, assertion);
        setData(assertion);
        initialize();
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData(EstablishOutboundSecureConversation assertion) {
        this.assertion = assertion;
    }

    @Override
    public EstablishOutboundSecureConversation getData(EstablishOutboundSecureConversation assertion) {
        viewToModel(assertion);
        return assertion;
    }

    private void initialize() {
        setContentPane(mainPanel);
        setModal(true);
        getRootPane().setDefaultButton(okButton);
        Utilities.centerOnScreen(this);
        Utilities.setEscKeyStrokeDisposes(this);

        DocumentListener validationListener = new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        enableOrDisableComponents();
                    }
                });
            }
        });

        serviceUrlTextField.getDocument().addDocumentListener(validationListener);

        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.#########"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum((double) 0);

        maxLifetimeTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });

        maxLifetimeTextField.getDocument().addDocumentListener(validationListener);

        maxLifetimeUnitComboBox.setModel(new DefaultComboBoxModel(TimeUnit.ALL));
        maxLifetimeUnitComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit) maxLifetimeUnitComboBox.getSelectedItem();
                Double time = (Double) maxLifetimeTextField.getValue();

                if (newTimeUnit != null && oldTimeUnit != null && newTimeUnit != oldTimeUnit) {
                    double oldMillis = oldTimeUnit.getMultiplier() * time;
                    maxLifetimeTextField.setValue(oldMillis / newTimeUnit.getMultiplier());
                }

                enableOrDisableComponents();
                oldTimeUnit = newTimeUnit;
            }
        });

        useSystemDefaultCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                boolean defaultSelected = useSystemDefaultCheckBox.isSelected();
                maxLifetimeTextField.setEnabled(! defaultSelected);
                maxLifetimeUnitComboBox.setEnabled(! defaultSelected);
                enableOrDisableComponents();
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

        modelToView();
    }

    private void modelToView() {
        serviceUrlTextField.setText(assertion.getServiceUrl());
        sctVarNameTextField.setText(assertion.getSecurityContextTokenVarName());
        clientEntropyTextField.setText(assertion.getClientEntropy());
        serverEntropyTextField.setText(assertion.getServerEntropy());
        fullKeyTextField.setText(assertion.getFullKey());
        creationTimeTextField.setText(assertion.getCreationTime());
        expirationTimeTextField.setText(assertion.getExpirationTime());

        TimeUnit timeUnit = assertion.getTimeUnit();
        Double lifetime = (double) assertion.getMaxLifetime();
        maxLifetimeTextField.setValue(lifetime / timeUnit.getMultiplier());
        maxLifetimeUnitComboBox.setSelectedItem(timeUnit);
        oldTimeUnit = timeUnit;

        boolean defaultSelected = assertion.isUseSystemDefaultSessionDuration();
        useSystemDefaultCheckBox.setSelected(defaultSelected);
        maxLifetimeTextField.setEnabled(! defaultSelected);
        maxLifetimeUnitComboBox.setEnabled(! defaultSelected);
    }

    private void viewToModel(EstablishOutboundSecureConversation assertion) {
        assertion.setServiceUrl(serviceUrlTextField.getText());
        assertion.setSecurityContextTokenVarName(sctVarNameTextField.getText());
        assertion.setClientEntropy(clientEntropyTextField.getText());
        assertion.setServerEntropy(serverEntropyTextField.getText());
        assertion.setFullKey(fullKeyTextField.getText());
        assertion.setCreationTime(creationTimeTextField.getText());
        assertion.setExpirationTime(expirationTimeTextField.getText());
        
        boolean defaultSelected = useSystemDefaultCheckBox.isSelected();
        assertion.setUseSystemDefaultSessionDuration(defaultSelected);
        if (! defaultSelected) {
            TimeUnit timeUnit = (TimeUnit) maxLifetimeUnitComboBox.getSelectedItem();
            Double lifetime = (Double) maxLifetimeTextField.getValue();
            assertion.setTimeUnit(timeUnit);
            assertion.setMaxLifetime((long)(lifetime * timeUnit.getMultiplier()));
        }
    }

    private void enableOrDisableComponents() {
        String serviceUrl = serviceUrlTextField.getText();
        boolean serviceUrlOk = serviceUrl != null && !serviceUrl.trim().isEmpty();

        int multiplier = ((TimeUnit) maxLifetimeUnitComboBox.getSelectedItem()).getMultiplier();
        boolean validLifetime = useSystemDefaultCheckBox.isSelected() ||
            ValidationUtils.isValidDouble(maxLifetimeTextField.getText().trim(), false,
                formatDouble((double)EstablishOutboundSecureConversation.MIN_SESSION_DURATION / multiplier), true,
                formatDouble((double)EstablishOutboundSecureConversation.MAX_SESSION_DURATION / multiplier), true);

        okButton.setEnabled(serviceUrlOk && validLifetime);
    }

    private double formatDouble(double doubleNum) {
        DecimalFormat formatter = new DecimalFormat("0.#########");
        return Double.parseDouble(formatter.format(doubleNum));
    }

    private void onOK() {
        confirmed = true;
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
