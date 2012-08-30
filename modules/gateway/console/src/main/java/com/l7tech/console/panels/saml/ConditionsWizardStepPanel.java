/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.console.panels.saml;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.SquigglyFieldUtils;
import com.l7tech.gui.util.PauseListenerAdapter;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.assertion.SamlElementGenericConfig;
import com.l7tech.policy.assertion.xmlsec.RequireSaml;
import com.l7tech.policy.assertion.xmlsec.SamlPolicyAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import javax.swing.text.NumberFormatter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.text.DecimalFormat;

/**
 * The SAML Conditions <code>WizardStepPanel</code>
 * @author emil
 * @version Jan 20, 2005
 */
public class ConditionsWizardStepPanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JLabel titleLabel;
    private JCheckBox checkBoxCheckAssertionValidity;
    private SquigglyTextField textFieldAudienceRestriction;
    private JSpinner notBeforeSpinner;
    private JSpinner notOnOrAfterSpinner;
    private JPanel validityPanel;
    private JLabel notBeforeLabel;
    private JLabel notOnOrAfterLabel;
    private JRadioButton defaultValidityRadioButton;
    private JRadioButton specifyValidityRadioButton;
    private JFormattedTextField maxExpiryTextField;
    private JComboBox timeUnitComboBox;
    private JPanel maxExpiryPanel;

    private final boolean showTitleLabel;
    private final boolean issueMode;
    private TimeUnit oldTimeUnit;

    /**
     * Creates new form ConditionsWizardStepPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = false;
        initialize();
    }


    /**
     * Creates new form ConditionsWizardStepPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode, JDialog owner) {
        super(next);
        this.showTitleLabel = showTitleLabel;
        this.issueMode = issueMode;
        setOwner(owner);
        initialize();
    }

    /**
     * Creates new form ConditionsWizardStepPanel
     */
    public ConditionsWizardStepPanel(WizardStepPanel next) {
        this(next, true);
    }

    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, boolean issueMode) {
        this(next, showTitleLabel, issueMode, null);
    }

    public ConditionsWizardStepPanel(WizardStepPanel next, boolean showTitleLabel, JDialog parent) {
        this(next, showTitleLabel, false, parent);
    }

    @Override
    public void storeSettings(Object settings) throws IllegalArgumentException {
        SamlPolicyAssertion samlAssertion = (SamlPolicyAssertion) settings;
        samlAssertion.setAudienceRestriction(nullIfEmpty(textFieldAudienceRestriction.getText()));
        if (issueMode) {
            SamlElementGenericConfig issuerConfiguration = (SamlElementGenericConfig) samlAssertion;
            issuerConfiguration.setConditionsNotBeforeSecondsInPast( specifyValidityRadioButton.isSelected() ? (Integer)notBeforeSpinner.getValue() : -1);
            issuerConfiguration.setConditionsNotOnOrAfterExpirySeconds( specifyValidityRadioButton.isSelected() ? (Integer)notOnOrAfterSpinner.getValue() : -1);
        } else {
            final RequireSaml assertion = (RequireSaml) settings;
            assertion.setCheckAssertionValidity(checkBoxCheckAssertionValidity.isSelected());

            TimeUnit timeUnit = (TimeUnit) timeUnitComboBox.getSelectedItem();
            Double lifetime = (Double) maxExpiryTextField.getValue();
            assertion.setTimeUnit(timeUnit);
            assertion.setMaxExpiry((long)(lifetime * timeUnit.getMultiplier()));
        }
    }

    private String nullIfEmpty( final String text ) {
        return text!=null && text.trim().isEmpty() ? null : text;
    }

    @Override
    public void readSettings(Object settings) throws IllegalArgumentException {
        SamlPolicyAssertion samlAssertion = (SamlPolicyAssertion)settings;
        textFieldAudienceRestriction.setText(samlAssertion.getAudienceRestriction());

        if (issueMode) {
            SamlElementGenericConfig issuerConfiguration = (SamlElementGenericConfig) samlAssertion;
            final int secondsInPast = issuerConfiguration.getConditionsNotBeforeSecondsInPast();
            final int onOrAfterExpirySeconds = issuerConfiguration.getConditionsNotOnOrAfterExpirySeconds();
            if (secondsInPast != -1 || onOrAfterExpirySeconds != -1) {
                specifyValidityRadioButton.setSelected(true);
                notBeforeLabel.setEnabled(true);
                notBeforeSpinner.setEnabled(true);
                notOnOrAfterLabel.setEnabled(true);
                notOnOrAfterSpinner.setEnabled(true);
            } else {
                defaultValidityRadioButton.setSelected(true);
            }

            if (secondsInPast != -1) notBeforeSpinner.setValue(secondsInPast);
            if (onOrAfterExpirySeconds != -1) notOnOrAfterSpinner.setValue(onOrAfterExpirySeconds);
        } else {
            RequireSaml ass = (RequireSaml) samlAssertion;
            checkBoxCheckAssertionValidity.setSelected(ass.isCheckAssertionValidity());

            TimeUnit timeUnit = ass.getTimeUnit();
            Double lifetime = (double) ass.getMaxExpiry();
            maxExpiryTextField.setValue(lifetime / timeUnit.getMultiplier());
            timeUnitComboBox.setSelectedItem(timeUnit);
            oldTimeUnit = timeUnit;
        }
    }

    private void initialize() {
        setLayout(new BorderLayout());

        if (issueMode) {
            checkBoxCheckAssertionValidity.setVisible(false);
            maxExpiryPanel.setVisible(false);
            notBeforeSpinner.setModel(new SpinnerNumberModel(120, 0, 3600, 1));
            validationRules.add(new InputValidator.NumberSpinnerValidationRule(notBeforeSpinner, "Not Before seconds in past"));
            notOnOrAfterSpinner.setModel(new SpinnerNumberModel(300, 30, 3600, 1));
            validationRules.add(new InputValidator.NumberSpinnerValidationRule(notOnOrAfterSpinner, "Not On Or After seconds in future"));
            final ActionListener validityPeriodEnableListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final boolean b = specifyValidityRadioButton.isSelected();
                    notBeforeLabel.setEnabled(b);
                    notBeforeSpinner.setEnabled(b);
                    notOnOrAfterLabel.setEnabled(b);
                    notOnOrAfterSpinner.setEnabled(b);
                }
            };
            defaultValidityRadioButton.addActionListener( validityPeriodEnableListener );
            specifyValidityRadioButton.addActionListener( validityPeriodEnableListener );
        } else {
            validityPanel.setVisible(false);
            defaultValidityRadioButton.setVisible(false);
            specifyValidityRadioButton.setVisible(false);
            notBeforeLabel.setVisible(false);
            notBeforeSpinner.setVisible(false);
            notOnOrAfterLabel.setVisible(false);
            notOnOrAfterSpinner.setVisible(false);

            initMaxExpiryPanel();
        }

        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
        if (showTitleLabel) {
            titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD));
        } else {
            titleLabel.getParent().remove(titleLabel);
        }

        TextComponentPauseListenerManager.registerPauseListenerWhenFocused(textFieldAudienceRestriction, new PauseListenerAdapter() {
            @Override
            public void textEntryPaused(JTextComponent component, long msecs) {
                notifyListeners();
            }
        }, 300);
    }

    private void initMaxExpiryPanel() {
        final NumberFormatter numberFormatter = new NumberFormatter(new DecimalFormat("0.#########"));
        numberFormatter.setValueClass(Double.class);
        numberFormatter.setMinimum((double) 0);

        maxExpiryTextField.setFormatterFactory(new JFormattedTextField.AbstractFormatterFactory() {
            @Override
            public JFormattedTextField.AbstractFormatter getFormatter(JFormattedTextField tf) {
                return numberFormatter;
            }
        });

        maxExpiryTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        notifyListeners();
                    }
                });
            }
        }));

        timeUnitComboBox.setModel(new DefaultComboBoxModel(TimeUnit.ALL));
        timeUnitComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                TimeUnit newTimeUnit = (TimeUnit) timeUnitComboBox.getSelectedItem();
                Double time = (Double) maxExpiryTextField.getValue();

                if (newTimeUnit != null && oldTimeUnit != null && newTimeUnit != oldTimeUnit) {
                    double oldMillis = oldTimeUnit.getMultiplier() * time;
                    maxExpiryTextField.setValue(oldMillis / newTimeUnit.getMultiplier());
                }

                oldTimeUnit = newTimeUnit;

                notifyListeners();
            }
        });
    }

    private boolean validateMaxExpiry() {
        TimeUnit timeUnit = (TimeUnit) timeUnitComboBox.getSelectedItem();
        if (timeUnit == null) return true;

        int multiplier = ((TimeUnit) timeUnitComboBox.getSelectedItem()).getMultiplier();
        return ValidationUtils.isValidDouble(
            maxExpiryTextField.getText().trim(),
            false,
            0,
            RequireSaml.UPPER_BOUND_FOR_MAX_EXPIRY  / multiplier);
    }

    @Override
    public boolean canFinish() {
        return canAdvance();
    }

    @Override
    public boolean canAdvance() {
        final boolean validated = (SquigglyFieldUtils.validateSquigglyFieldForUris(textFieldAudienceRestriction) == null);
        return validateMaxExpiry() && validated;
    }

    /**
     * @return the wizard step label
     */
    @Override
    public String getStepLabel() {
        return "Conditions";
    }

    @Override
    public String getDescription() {
        if (issueMode) {
            return
          "<html>Specify SAML statement conditions such as assertion validity period and Audience Restriction</html>";
        } else {
            return
          "<html>Optionally indicate whether the token’s validity period should be checked, whether the token’s expiry " +
              "time should be further restricted, or whether to restrict the audience for the SAML token.</html>";
        }
    }
}