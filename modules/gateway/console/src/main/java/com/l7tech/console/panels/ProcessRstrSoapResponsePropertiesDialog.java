package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.xmlsec.ProcessRstrSoapResponse;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;

/**
 *
 */
public class ProcessRstrSoapResponsePropertiesDialog extends AssertionPropertiesOkCancelSupport<ProcessRstrSoapResponse> {

    //- PUBLIC

    /**
     * Create a new dialog with the given owner and data.
     *
     * @param owner     The owner for the dialog
     * @param assertion The assertion data
     */
    public ProcessRstrSoapResponsePropertiesDialog( final Window owner,
                                                final ProcessRstrSoapResponse assertion ) {
        super(ProcessRstrSoapResponse.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public ProcessRstrSoapResponse getData( final ProcessRstrSoapResponse assertion ) throws ValidationException {
        assertion.setTokenType( (SecurityTokenType)tokenTypeComboBox.getSelectedItem() );
        assertion.setVariablePrefix( varPrefixTextField.getVariable() );

        return assertion;
    }

    @Override
    public void setData( final ProcessRstrSoapResponse assertion ) {
        selectIfNotNull( tokenTypeComboBox, assertion.getTokenType() );

        varPrefixTextField.setAssertion( assertion );
        varPrefixTextField.setSuffixes( ProcessRstrSoapResponse.getVariableSuffixes() );
        varPrefixTextField.setVariable( assertion.getVariablePrefix() );

        enableOrDisableComponents();
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        // Components and layout
        varPrefixTextField = new TargetVariablePanel();
        varPrefixPanel.setLayout(new BorderLayout());
        varPrefixPanel.add(varPrefixTextField, BorderLayout.CENTER);

        // Models
        tokenTypeComboBox.setModel( new DefaultComboBoxModel( new Object[]{
                SecurityTokenType.UNKNOWN,
                SecurityTokenType.SAML2_ASSERTION,
                SecurityTokenType.SAML_ASSERTION,
                SecurityTokenType.WSSC_CONTEXT,
        } ) );
        tokenTypeComboBox.setRenderer( new TextListCellRenderer<SecurityTokenType>( new Functions.Unary<String,SecurityTokenType>(){
            @Override
            public String call( final SecurityTokenType securityTokenType ) {
                return SecurityTokenType.UNKNOWN == securityTokenType ?  "<Any>" : securityTokenType.getName();
            }
        } ) );

        // Actions
        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            protected void run() {
                enableOrDisableComponents();
            }
        };
        varPrefixTextField.addChangeListener( enableDisableListener );

        enableOrDisableComponents();
    }

    //- PRIVATE

    private JComboBox tokenTypeComboBox;
    private JPanel varPrefixPanel;
    private JPanel mainPanel;

    private TargetVariablePanel varPrefixTextField;

    private void selectIfNotNull( final JComboBox component,
                                  final Object value ) {
        if ( value != null ) {
            component.setSelectedItem( value );
        }
    }

    private void enableOrDisableComponents() {
        getOkButton().setEnabled( !isReadOnly() && varPrefixTextField.isEntryValid() );
    }
}

