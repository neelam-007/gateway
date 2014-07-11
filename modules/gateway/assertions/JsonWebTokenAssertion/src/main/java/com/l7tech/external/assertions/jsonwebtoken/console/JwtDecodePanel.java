package com.l7tech.external.assertions.jsonwebtoken.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.jsonwebtoken.JwtDecodeAssertion;
import com.l7tech.external.assertions.jsonwebtoken.jsonwebtoken.JwtUtilities;
import com.l7tech.policy.variable.Syntax;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * User: rseminoff
 * Date: 28/11/12
 */
public class JwtDecodePanel extends AssertionPropertiesOkCancelSupport<JwtDecodeAssertion> {
    private TextOptionPanel tokenPanel;
    private AlgorithmSecretPanel secretPanel;
    private TextOptionPanel outputPanel;
    private JPanel decodePanel;


    public JwtDecodePanel(final Frame parent, final JwtDecodeAssertion assertion) {
        super(JwtDecodeAssertion.class, parent, assertion, true);
        this.add(decodePanel);
        secretPanel.setPanelForDecode(); // Changes "Private Key" to "Public Key"
        outputPanel.setVariableAsWritable();  // The variable specified in this field is expected to be writable
        initComponents();
    }

    @Override
    public void setData(JwtDecodeAssertion assertion) {
        tokenPanel.setPanelWithValues("Incoming JWT Token Variable", getProcessedVariableProperty(assertion.getIncomingToken()));
        tokenPanel.setAssertion(assertion, this.getPreviousAssertion());
        secretPanel.setPanelWithValue(((assertion.getAlgorithmSecretLocation() & JwtUtilities.SELECTED_SECRET_VARIABLE) == JwtUtilities.SELECTED_SECRET_VARIABLE ? getProcessedVariableProperty(assertion.getAlgorithmSecretValue()) : assertion.getAlgorithmSecretValue()), assertion.getAlgorithmSecretLocation());
        secretPanel.setAssertion(assertion, this.getPreviousAssertion());
        outputPanel.setPanelWithValues("Output Variable", assertion.getOutputVariable());
        outputPanel.setAssertion(assertion, this.getPreviousAssertion());
    }

    @Override
    public JwtDecodeAssertion getData(JwtDecodeAssertion assertion) throws ValidationException {
        assertion.setIncomingToken(processValueForVariables(tokenPanel.getValueFromPanel()));
        assertion.setAlgorithmSecretLocation(secretPanel.getSecretSelection());
        assertion.setAlgorithmSecretValue(((secretPanel.getSecretSelection() & JwtUtilities.SELECTED_SECRET_VARIABLE) == JwtUtilities.SELECTED_SECRET_VARIABLE ? processValueForVariables(secretPanel.getValueFromPanel()) : secretPanel.getValueFromPanel()));
        assertion.setOutputVariable(outputPanel.getValueFromPanel());
        return assertion;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    protected JPanel createPropertyPanel() {
        return this.decodePanel;
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
        // Variables with ${ }?
        String[] totalVariables = Syntax.getReferencedNames(value);
        if (totalVariables.length >= 1) {
            return totalVariables[0]; // Return only the first variable.  That's what the UI says.  Variable.  Not variables.
        }

        return value;   // At this point, it's blank.
    }

}
