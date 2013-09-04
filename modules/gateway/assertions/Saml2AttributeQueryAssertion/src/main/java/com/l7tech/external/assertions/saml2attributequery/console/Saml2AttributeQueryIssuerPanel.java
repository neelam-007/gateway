package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.external.assertions.saml2attributequery.Saml2AttributeQueryAssertion;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 14-Jan-2009
 * Time: 9:22:18 PM
 * To change this template use File | Settings | File Templates.
 */
public class Saml2AttributeQueryIssuerPanel extends WizardStepPanel {

    private JPanel mainPanel;
    private JTextField issuerField;

    public Saml2AttributeQueryIssuerPanel(WizardStepPanel nextStep, boolean readonly) {
        super(nextStep, readonly);

        initialize();
    }

    private void initialize() {
        issuerField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                Saml2AttributeQueryIssuerPanel.this.notifyListeners();
            }

            public void insertUpdate(DocumentEvent evt) {
                Saml2AttributeQueryIssuerPanel.this.notifyListeners();
            }

            public void removeUpdate(DocumentEvent evt) {
                Saml2AttributeQueryIssuerPanel.this.notifyListeners();
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

        if(assertion.getIssuer() == null) {
            issuerField.setText("");
        } else {
            issuerField.setText(assertion.getIssuer());
        }
    }

    public void storeSettings(Object settings) throws IllegalArgumentException {
        if(!(settings instanceof Saml2AttributeQueryAssertion)) {
            throw new IllegalArgumentException();
        }

        Saml2AttributeQueryAssertion assertion = (Saml2AttributeQueryAssertion)settings;

        assertion.setIssuer(issuerField.getText());
    }

    public String getStepLabel() {
        return "Issuer";
    }

    public boolean onNextButton() {
        return canAdvance();
    }

    public boolean canAdvance() {
        return issuerField.getText().trim().length() > 0;
    }
}