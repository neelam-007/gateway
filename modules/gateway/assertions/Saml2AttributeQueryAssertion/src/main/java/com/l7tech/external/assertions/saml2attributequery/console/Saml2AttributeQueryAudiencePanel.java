package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 9:22:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryAudiencePanel extends WizardStepPanel {
    private JPanel mainPanel;
    private JCheckBox validityCheckBox;
    private JLabel notBeforeLabel;
    private JSpinner notBeforeSpinner;
    private JLabel notOnOrAfterLabel;
    private JSpinner notOnOrAfterSpinner;
    private JTextField audienceField;

    public Saml2AttributeQueryAudiencePanel(WizardStepPanel nextStep, boolean readonly) {
        super(nextStep, readonly);

        initialize();
    }

    private void initialize() {
        validityCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                boolean enable = validityCheckBox.isSelected();
                notBeforeLabel.setEnabled(enable);
                notBeforeSpinner.setEnabled(enable);
                notOnOrAfterLabel.setEnabled(enable);
                notOnOrAfterSpinner.setEnabled(enable);
                Saml2AttributeQueryAudiencePanel.this.notifyListeners();
            }
        });

        notBeforeSpinner.setModel(new SpinnerNumberModel(120, 0, 3600, 1));
        notOnOrAfterSpinner.setModel(new SpinnerNumberModel(300, 30, 3600, 1));

        audienceField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                Saml2AttributeQueryAudiencePanel.this.notifyListeners();
            }

            public void insertUpdate(DocumentEvent evt) {
                Saml2AttributeQueryAudiencePanel.this.notifyListeners();
            }

            public void removeUpdate(DocumentEvent evt) {
                Saml2AttributeQueryAudiencePanel.this.notifyListeners();
            }
        });

        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    public void readSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        if(assertion.getConditionsNotBeforeSecondsInPast() > -1 || assertion.getConditionsNotOnOrAfterExpirySeconds() > -1) {
            validityCheckBox.setSelected(true);
        }
        if(assertion.getConditionsNotBeforeSecondsInPast() > -1) {
            notBeforeSpinner.setValue(assertion.getConditionsNotBeforeSecondsInPast());
        } else {
            notBeforeSpinner.setValue(0);
        }
        if(assertion.getConditionsNotOnOrAfterExpirySeconds() > -1) {
            notOnOrAfterSpinner.setValue(assertion.getConditionsNotOnOrAfterExpirySeconds());
        } else {
            notOnOrAfterSpinner.setValue(30);
        }

        if(assertion.getAudienceRestriction() == null) {
            audienceField.setText("");
        } else {
            audienceField.setText(assertion.getAudienceRestriction());
        }
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        if(validityCheckBox.isSelected()) {
            assertion.setConditionsNotBeforeSecondsInPast((Integer)notBeforeSpinner.getValue());
            assertion.setConditionsNotOnOrAfterExpirySeconds((Integer)notOnOrAfterSpinner.getValue());
        } else {
            assertion.setConditionsNotBeforeSecondsInPast(-1);
            assertion.setConditionsNotOnOrAfterExpirySeconds(-1);
        }

        assertion.setAudienceRestriction(audienceField.getText());
    }

    public String getStepLabel() {
        return "Audience Restriction";
    }

    public boolean onNextButton() {
        return canAdvance();
    }

    public boolean canAdvance() {
        return true;
    }
}