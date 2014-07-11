package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.jsonwebtoken.JwtEncodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebsignature.JsonWebSignature;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.exceptions.IllegalJwtSignatureException;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import static com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities.*;

/**
 * User: rseminoff
 * Date: 28/11/12
 */
public class JwtEncodePanel extends AssertionPropertiesOkCancelSupport<JwtEncodeAssertion> {

    private JPanel encodePanel;
    private AlgorithmSignaturePanel signatureAlgorithmPanel;
    private TextOptionPanel payloadPanel;
    private AlgorithmSecretPanel algorithmSecretPanel;
    private TextOptionPanel outputPanel;
    private JwtHeaderPanel jwtHeaderPanel;

    public JwtEncodePanel(final Frame parent, final JwtEncodeAssertion assertion) {
        super(JwtEncodeAssertion.class, parent, assertion, true);
        jwtHeaderPanel.setParent(this);
        signatureAlgorithmPanel.setParent(this);
        outputPanel.setVariableAsWritable();  // The variable specified in this field is expected to be writable
        this.add(encodePanel);
        initComponents();
    }

    @Override
    public void setData(JwtEncodeAssertion assertion) {
        payloadPanel.setPanelWithValues("JSON Payload Variable", getProcessedVariableProperty(assertion.getJsonPayload()));
        payloadPanel.setAssertion(assertion, this.getPreviousAssertion());
        signatureAlgorithmPanel.setPanelWithValue((assertion.getSignatureSelected() == JwtUtilities.SELECTED_SIGNATURE_VARIABLE ? getProcessedVariableProperty(assertion.getSignatureValue()) : assertion.getSignatureValue()), assertion.getSignatureSelected());
        signatureAlgorithmPanel.setAssertion(assertion, this.getPreviousAssertion());
        // If the algorithm selection is either SECRET or SECRET_BASE64, get the processed variable.
        algorithmSecretPanel.setPanelWithValue(((assertion.getAlgorithmSecretLocation() & JwtUtilities.SELECTED_SECRET_VARIABLE) == JwtUtilities.SELECTED_SECRET_VARIABLE ? getProcessedVariableProperty(assertion.getAlgorithmSecretValue()) : assertion.getAlgorithmSecretValue()), assertion.getAlgorithmSecretLocation());
        algorithmSecretPanel.setAssertion(assertion, this.getPreviousAssertion());
        jwtHeaderPanel.setPanelWithValues(assertion.getJwtHeaderVariable(), assertion.getJwtHeaderType());
        jwtHeaderPanel.setAssertion(assertion, this.getPreviousAssertion());
        outputPanel.setPanelWithValues("Output Variable", assertion.getOutputVariable());
        outputPanel.setAssertion(assertion, this.getPreviousAssertion());
        updatePanels();
    }

    @Override
    public JwtEncodeAssertion getData(JwtEncodeAssertion assertion) throws ValidationException {
        JwtHeaderPanel.JwtHeaderValues values = jwtHeaderPanel.getValueFromPanel();
        assertion.setJwtHeaderVariable(values.getVariable());
        assertion.setJwtHeaderType(values.getHeaderType());

        assertion.setJsonPayload(processValueForVariables(payloadPanel.getValueFromPanel()));
        assertion.setSignatureValue((signatureAlgorithmPanel.getSignatureSelection() == JwtUtilities.SELECTED_SIGNATURE_VARIABLE ? processValueForVariables(signatureAlgorithmPanel.getValueFromPanel()) : signatureAlgorithmPanel.getValueFromPanel()));
        assertion.setSignatureSelected(signatureAlgorithmPanel.getSignatureSelection());
        // If the algorithm selection is either SECRET or SECRET_BASE64, parse the field.
        assertion.setAlgorithmSecretValue(((algorithmSecretPanel.getSecretSelection() & JwtUtilities.SELECTED_SECRET_VARIABLE) == JwtUtilities.SELECTED_SECRET_VARIABLE ? processValueForVariables(algorithmSecretPanel.getValueFromPanel()) : algorithmSecretPanel.getValueFromPanel()));
        assertion.setAlgorithmSecretLocation(algorithmSecretPanel.getSecretSelection());
        assertion.setOutputVariable(outputPanel.getValueFromPanel());
        return assertion;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void updatePanels() {
        if ((signatureAlgorithmPanel != null) && (algorithmSecretPanel != null)) {
            // This updates the panels based on rules provided by the JWS Signature objects.
            if (signatureAlgorithmPanel.isVariableSelected()) {
                // All signature types are enabled in this case.
                algorithmSecretPanel.updateAvailableSecrets(AVAILABLE_SECRET_KEY + AVAILABLE_SECRET_PASSWORD + AVAILABLE_SECRET_VARIABLE, true);
            } else {
                // Only if the signature algorithm is NOT a variable, we need to sanitize the input based on the
                // selected algorithm in the list.
                String selectedAlgorithm = signatureAlgorithmPanel.getValueFromPanel();
                if (selectedAlgorithm != null) {
                    JsonWebSignature jws = null;
                    try {
                        jws = JsonWebSignature.getAlgorithm(selectedAlgorithm);
                    } catch (IllegalJwtSignatureException e) {
                        jws = null;
                    }
                    if (jws != null) {
                        // Update the available secrets, but automatically select Key if an RSA signature algorithm is selected.
                        algorithmSecretPanel.updateAvailableSecrets(jws.getUIAvailableSecrets(), false);
                    }
                }
                // That's it.
            }
        }
    }

    public void updateSignatureAlgorithmPanel(int selectedHeaderType) {

        switch (selectedHeaderType) {
            case (JwtUtilities.SUPPLIED_FULL_JWT_HEADER): {
                signatureAlgorithmPanel.enableSignatureSelections(false);
                // All available secret types are available if the full header is selected.
                algorithmSecretPanel.updateAvailableSecrets(AVAILABLE_SECRET_KEY + AVAILABLE_SECRET_PASSWORD + AVAILABLE_SECRET_VARIABLE, true);
                break;
            }
            default: {
                // Secret selections are based on selected algorithm as per normal.
                signatureAlgorithmPanel.enableSignatureSelections(true);
                updatePanels();
            }
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.encodePanel;
    }

    protected void initComponents() {
        super.initComponents();

        getCancelButton().addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

    }

    private String getProcessedVariableProperty(String property) {
        String[] totalVariables = Syntax.getReferencedNames(property);
        if (totalVariables.length == 1) {
            // There is only one variable declared.  We strip the variable markers from the variable
            // and display it in plain text.
            return totalVariables[0];
        }

        // Multiple variables are present in the field, so we return the field verbatim.
        return property;
    }

    private String processValueForVariables(String value) {
        // Check to see if the json payload field contains any explicit variables.
        String[] totalVariables = Syntax.getReferencedNames(value);
        if (totalVariables.length > 1) {
            // Multiple variables are explicitly defined in here, so we keep the value as is.
            return value;
        }

        // There is only one variable, so we need to prefix/suffix it if there's anything defined.
        // We use field as opposed to totalVariables[0] because the variable doesn't need to be
        // delimited in the UI.
        if (!value.trim().isEmpty()) {
            return Syntax.SYNTAX_PREFIX + value + Syntax.SYNTAX_SUFFIX;
        }

        return "";   // At this point, it's blank.
    }
}
