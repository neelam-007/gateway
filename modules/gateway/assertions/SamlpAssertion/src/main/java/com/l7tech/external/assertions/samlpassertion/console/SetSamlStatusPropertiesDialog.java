package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.samlpassertion.SamlStatus;
import com.l7tech.external.assertions.samlpassertion.SetSamlStatusAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;

/**
 * Properties dialog for Set SAML Status assertion.
 */
public class SetSamlStatusPropertiesDialog extends AssertionPropertiesOkCancelSupport<SetSamlStatusAssertion> {

    //- PUBLIC

    public SetSamlStatusPropertiesDialog( final Window parent,
                                         final SetSamlStatusAssertion assertion ) {
        super( SetSamlStatusAssertion.class, parent, assertion, true );
        initComponents();
        setData(assertion);
    }

    @Override
    public SetSamlStatusAssertion getData( final SetSamlStatusAssertion assertion ) throws ValidationException {
        validateData();
        assertion.setSamlStatus( (SamlStatus)statusComboBox.getSelectedItem() );
        assertion.setVariableName( VariablePrefixUtil.fixVariableName( variableNameTextField.getText() ) );
        return assertion;
    }

    @Override
    public void setData( final SetSamlStatusAssertion assertion ) {
        if ( assertion.getSamlStatus() != null ) {
            versionComboBox.setSelectedItem( assertion.getSamlStatus().getSamlVersion() );
            statusComboBox.setSelectedItem( assertion.getSamlStatus() );
        }
        setText( variableNameTextField, assertion.getVariableName() );
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

        @Override
    protected void initComponents() {
        super.initComponents();

        versionComboBox.setModel( new DefaultComboBoxModel( new Integer[]{1,2} ) );
        versionComboBox.setRenderer( new TextListCellRenderer<Integer>( new Functions.Unary<String,Integer>(){
            @Override
            public String call( final Integer version ) {
                return version == 1 ? "1.1" : "2.0";
            }
        } ) );

        statusComboBox.setModel( new DefaultComboBoxModel( SamlStatus.getSaml1xStatuses().toArray() ) );
        statusComboBox.setRenderer( new TextListCellRenderer<SamlStatus>( new Functions.Unary<String,SamlStatus>(){
            @Override
            public String call( final SamlStatus dataType ) {
                return dataType.getValue();
            }
        } ) );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        versionComboBox.addActionListener( enableDisableListener );
    }

    //- PRIVATE

    private JPanel mainPanel;
    private JComboBox versionComboBox;
    private JComboBox statusComboBox;
    private JTextField variableNameTextField;

    private void setText( final JTextComponent textComponent, final String text ) {
        if ( text != null ) {
            textComponent.setText( text );
            textComponent.setCaretPosition( 0 );
        }
    }

    private void validateData() {
        // validation
        String message = VariableMetadata.validateName( VariablePrefixUtil.fixVariableName( variableNameTextField.getText() ) );
        if ( message != null ) {
            throw new ValidationException( message, "Invalid Property", null );
        }
    }

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly();


        if ( (Integer)versionComboBox.getSelectedItem() != ((SamlStatus)statusComboBox.getSelectedItem()).getSamlVersion() ) {
            if ( 1 == (Integer)versionComboBox.getSelectedItem() ) {
                statusComboBox.setModel( new DefaultComboBoxModel( SamlStatus.getSaml1xStatuses().toArray() ) );
            } else {
                statusComboBox.setModel( new DefaultComboBoxModel( SamlStatus.getSaml2xStatuses().toArray() ) );
            }
        }

        getOkButton().setEnabled( enableAny );
    }
}
