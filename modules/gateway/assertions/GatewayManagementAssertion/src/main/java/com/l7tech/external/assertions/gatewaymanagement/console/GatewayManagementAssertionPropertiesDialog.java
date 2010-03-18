package com.l7tech.external.assertions.gatewaymanagement.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 *
 */
public class GatewayManagementAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<GatewayManagementAssertion> {

    //- PUBLIC

    public GatewayManagementAssertionPropertiesDialog( final Window parent,
                                                       final GatewayManagementAssertion assertion ) {
        super( GatewayManagementAssertion.class, parent, assertion, true );
        this.assertion = assertion;
        initComponents();
        setData(assertion);
    }

    @Override
    public GatewayManagementAssertion getData( final GatewayManagementAssertion assertion ) throws ValidationException {
        if ( variablePrefixTextField.getText() != null && !variablePrefixTextField.getText().trim().isEmpty() ) {
            assertion.setVariablePrefix( variablePrefixTextField.getText().trim() );
        } else {
            assertion.setVariablePrefix( null );
        }
        return assertion;
    }

    @Override
    public void setData( final GatewayManagementAssertion assertion ) {
        this.assertion = assertion;
        this.variablePrefixTextField.setText( assertion.getVariablePrefix()==null ? "" : assertion.getVariablePrefix() );
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        super.initComponents();

        if ( isReadOnly() ) {
            variablePrefixTextField.setEnabled( false );               
        }

        variablePrefixTextField.setText("");
        clearVariablePrefixStatus();
        TextComponentPauseListenerManager.registerPauseListener(
            variablePrefixTextField,
            new PauseListener() {
                @Override
                public void textEntryPaused( final JTextComponent component, final long milliseconds ) {
                    if( validateVariablePrefix() ) {
                        getOkButton().setEnabled(true);
                    } else {
                        getOkButton().setEnabled(false);
                    }
                }

                @Override
                public void textEntryResumed(JTextComponent component) {
                    clearVariablePrefixStatus();
                }
            },
            500
        );

    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JTextField variablePrefixTextField;
    private JLabel varPrefixStatusLabel;

    private GatewayManagementAssertion assertion;

    private boolean validateVariablePrefix() {
        String[] suffixes = GatewayManagementAssertion.VARIABLE_SUFFIXES.toArray( new String[GatewayManagementAssertion.VARIABLE_SUFFIXES.size()] );

        return VariablePrefixUtil.validateVariablePrefix(
            variablePrefixTextField.getText(),
            SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet(),
            suffixes,
            varPrefixStatusLabel);
    }

    private void clearVariablePrefixStatus() {
        VariablePrefixUtil.clearVariablePrefixStatus(varPrefixStatusLabel);
    }
}
