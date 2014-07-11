package com.l7tech.external.assertions.xmppassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.xmppassertion.XMPPGetAssociatedSessionIdAssertion;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: njordan
 * Date: 03/04/12
 * Time: 1:46 PM
 * To change this template use File | Settings | File Templates.
 */
public class XMPPGetAssociatedSessionIdAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<XMPPGetAssociatedSessionIdAssertion> {
    protected static final Logger logger = Logger.getLogger(XMPPGetAssociatedSessionIdAssertionPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JComboBox directionComboBox;
    private SquigglyTextField sessionIdField;
    private SquigglyTextField variableNameField;

    public XMPPGetAssociatedSessionIdAssertionPropertiesDialog(Window owner, XMPPGetAssociatedSessionIdAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    protected void initComponents() {
        directionComboBox.setModel(new DefaultComboBoxModel(new String[] {"Client \u2192 Server", "Server \u2192 Client"}));

        sessionIdField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateSessionId();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateSessionId();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateSessionId();
            }
        });

        variableNameField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                validateVariableName();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                validateVariableName();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                validateVariableName();
            }
        });

        super.initComponents();

        validateSessionId();
        validateVariableName();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    private void validateSessionId() {
        if(sessionIdField.getText().length() == 0 || !Syntax.validateAnyVariableReferences(sessionIdField.getText())) {
            sessionIdField.setAll();
            sessionIdField.setSquiggly();
            sessionIdField.setColor(Color.RED);
        } else {
            sessionIdField.setNone();
        }
    }

    private void validateVariableName() {
        if(variableNameField.getText().length() == 0 || !VariableMetadata.isNameValid(variableNameField.getText(), false)) {
            variableNameField.setAll();
            variableNameField.setSquiggly();
            variableNameField.setColor(Color.RED);
        } else {
            variableNameField.setNone();
        }
    }

    @Override
    public XMPPGetAssociatedSessionIdAssertion getData(XMPPGetAssociatedSessionIdAssertion assertion) throws AssertionPropertiesOkCancelSupport.ValidationException {
        if(sessionIdField.getText().length() == 0) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The session ID is required.");
        } else if(!Syntax.validateAnyVariableReferences(sessionIdField.getText())) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The session ID is not valid.");
        }
        if(variableNameField.getText().isEmpty()) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The variable name is required.");
        } else if(!VariableMetadata.isNameValid(variableNameField.getText(), false)) {
            throw new AssertionPropertiesOkCancelSupport.ValidationException("The variable name is not valid.");
        }

        assertion.setInbound(directionComboBox.getSelectedIndex() == 0);
        assertion.setSessionId(sessionIdField.getText());
        assertion.setVariableName(variableNameField.getText());

        return assertion;
    }

    @Override
    public void setData(XMPPGetAssociatedSessionIdAssertion assertion) {
        directionComboBox.setSelectedIndex(assertion.isInbound() ? 0 : 1);
        sessionIdField.setText(assertion.getSessionId() == null ? "" : assertion.getSessionId());
        variableNameField.setText(assertion.getVariableName() == null ? "" : assertion.getVariableName());

        validateSessionId();
        validateVariableName();
    }
}
