package com.l7tech.external.assertions.gatewaymanagement.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.gatewaymanagement.GatewayManagementAssertion;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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
        if ( variablePrefixTextField.getVariable() != null && !variablePrefixTextField.getVariable().trim().isEmpty() ) {
            assertion.setVariablePrefix( variablePrefixTextField.getVariable().trim() );
        } else {
            assertion.setVariablePrefix( null );
        }
        return assertion;
    }

    @Override
    public void setData( final GatewayManagementAssertion assertion ) {
        this.assertion = assertion;
        this.variablePrefixTextField.setVariable( assertion.getVariablePrefix()==null ? "" : assertion.getVariablePrefix() );
        this.variablePrefixTextField.setAssertion(assertion);

        String[] suffixes = GatewayManagementAssertion.VARIABLE_SUFFIXES.toArray( new String[GatewayManagementAssertion.VARIABLE_SUFFIXES.size()] );
        variablePrefixTextField.setSuffixes(suffixes);
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        super.initComponents();

        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixTextFieldPanel.setLayout(new BorderLayout());
        variablePrefixTextFieldPanel.add(variablePrefixTextField, BorderLayout.CENTER);
        variablePrefixTextField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
               getOkButton().setEnabled(variablePrefixTextField.isEntryValid());
            }
        });

        if ( isReadOnly() ) {
            variablePrefixTextField.setEnabled( false );               
        }

        variablePrefixTextField.setVariable("");
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JPanel variablePrefixTextFieldPanel;
    private TargetVariablePanel variablePrefixTextField;

    private GatewayManagementAssertion assertion;

}
