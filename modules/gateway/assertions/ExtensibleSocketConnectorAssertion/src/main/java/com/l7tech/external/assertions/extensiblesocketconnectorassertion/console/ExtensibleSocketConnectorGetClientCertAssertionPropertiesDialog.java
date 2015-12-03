package com.l7tech.external.assertions.extensiblesocketconnectorassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.extensiblesocketconnectorassertion.ExtensibleSocketConnectorGetClientCertAssertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 30/03/12
 * Time: 1:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class ExtensibleSocketConnectorGetClientCertAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<ExtensibleSocketConnectorGetClientCertAssertion> {
    protected static final Logger logger = Logger.getLogger(ExtensibleSocketConnectorGetClientCertAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JTextField variableNameField;

    public ExtensibleSocketConnectorGetClientCertAssertionPropertiesDialog(Window owner, ExtensibleSocketConnectorGetClientCertAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    public ExtensibleSocketConnectorGetClientCertAssertion getData(ExtensibleSocketConnectorGetClientCertAssertion assertion) throws AssertionPropertiesOkCancelSupport.ValidationException {
        if (variableNameField.getText().trim().isEmpty()) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The variable name is required.");
        }

        assertion.setVariableName(variableNameField.getText().trim());

        return assertion;
    }

    @Override
    public void setData(ExtensibleSocketConnectorGetClientCertAssertion assertion) {
        variableNameField.setText(assertion.getVariableName() == null ? "" : assertion.getVariableName());
    }
}
