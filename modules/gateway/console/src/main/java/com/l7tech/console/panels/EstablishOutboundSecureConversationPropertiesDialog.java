package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.EstablishOutboundSecureConversation;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.util.ISO8601Date;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.text.ParseException;

/**
 * @author ghuang
 */
public class EstablishOutboundSecureConversationPropertiesDialog extends AssertionPropertiesOkCancelSupport<EstablishOutboundSecureConversation> {
    private JPanel mainPanel;
    private JTextField serviceUrlTextField;
    private JTextField sctVarNameTextField;
    private JTextField clientEntropyTextField;
    private JTextField serverEntropyTextField;
    private JTextField keySizeTextField;
    private JTextField fullKeyTextField;
    private JTextField creationTimeTextField;
    private JTextField expirationTimeTextField;
    private JFormattedTextField maxLifetimeTextField;
    private JComboBox maxLifetimeUnitComboBox;
    private JCheckBox useSystemDefaultCheckBox;
    private JCheckBox allowUsingSessionCheckBox;

    private TimeUnit oldTimeUnit;

    public EstablishOutboundSecureConversationPropertiesDialog( final Window owner,
                                                                final EstablishOutboundSecureConversation assertion) {
        super(EstablishOutboundSecureConversation.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData( final EstablishOutboundSecureConversation assertion ) {
        modelToView(assertion);
    }

    @Override
    public EstablishOutboundSecureConversation getData( final EstablishOutboundSecureConversation assertion ) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        final DocumentListener validationListener = new RunOnChangeListener(new Runnable() {
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
        keySizeTextField.getDocument().addDocumentListener(validationListener);

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

        creationTimeTextField.getDocument().addDocumentListener(validationListener);
        expirationTimeTextField.getDocument().addDocumentListener(validationListener);

        pack();
        Utilities.centerOnParentWindow(this);
    }

    @Override
    protected void configureView() {
        enableOrDisableComponents();
    }

    private void modelToView(EstablishOutboundSecureConversation assertion) {
        serviceUrlTextField.setText(assertion.getServiceUrl());
        sctVarNameTextField.setText(assertion.getSecurityContextTokenVarName());
        clientEntropyTextField.setText(assertion.getClientEntropy());
        serverEntropyTextField.setText(assertion.getServerEntropy());
        keySizeTextField.setText(assertion.getKeySize());
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

        allowUsingSessionCheckBox.setSelected(assertion.isAllowInboundMsgUsingSession());
    }

    private void viewToModel(EstablishOutboundSecureConversation assertion) {
        assertion.setServiceUrl(serviceUrlTextField.getText());
        assertion.setSecurityContextTokenVarName(sctVarNameTextField.getText());
        assertion.setClientEntropy(clientEntropyTextField.getText());
        assertion.setServerEntropy(serverEntropyTextField.getText());
        assertion.setKeySize(keySizeTextField.getText());
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

        assertion.setAllowInboundMsgUsingSession(allowUsingSessionCheckBox.isSelected());
    }

    private void enableOrDisableComponents() {
        String serviceUrl = serviceUrlTextField.getText();
        boolean serviceUrlOk = serviceUrl != null && !serviceUrl.trim().isEmpty();

        int multiplier = ((TimeUnit) maxLifetimeUnitComboBox.getSelectedItem()).getMultiplier();
        String maxLifeTime = maxLifetimeTextField.getText().trim();
        boolean validLifetime = useSystemDefaultCheckBox.isSelected() || "0".equals(maxLifeTime) ||
            ValidationUtils.isValidDouble(maxLifeTime, false,
                formatDouble((double)EstablishOutboundSecureConversation.MIN_SESSION_DURATION / multiplier), true,   // MIN: 1 min
                formatDouble((double)EstablishOutboundSecureConversation.MAX_SESSION_DURATION / multiplier), true); // MAX: 24 hrs

        boolean validCreationTime = true;
        try {
            String creationTimeStr = creationTimeTextField.getText();
            if (Syntax.getReferencedNames(creationTimeStr).length == 0) {
                ISO8601Date.parse(creationTimeStr).getTime();
            }
        } catch (ParseException e) {
            validCreationTime = false;
        }

        boolean validExpirationTime = true;
        try {
            String expirationTimeStr = expirationTimeTextField.getText();
            if (Syntax.getReferencedNames(expirationTimeStr).length == 0) {
                ISO8601Date.parse(expirationTimeStr).getTime();
            }
        } catch (ParseException e) {
            validCreationTime = false;
        }

        boolean validKeySize = true;
        try {
            if ( Syntax.getReferencedNames( keySizeTextField.getText() ).length == 0 &&
                 !ValidationUtils.isValidInteger( keySizeTextField.getText(), true, 0, 100000 )) {
                validKeySize = false;
            }
        } catch ( VariableNameSyntaxException e ) {
            validKeySize = false;
        }

        getOkButton().setEnabled(!isReadOnly() && serviceUrlOk && validCreationTime && validExpirationTime && validLifetime && validKeySize);
    }

    private double formatDouble(double doubleNum) {
        DecimalFormat formatter = new DecimalFormat("0.#########");
        return Double.parseDouble(formatter.format(doubleNum));
    }

}
