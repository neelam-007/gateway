package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * Properties dialog for Replay Protection assertion.
 */
public class ReplayProtectionPropertiesDialog extends AssertionPropertiesOkCancelSupport<WssReplayProtection> {

    //- PUBLIC

    public ReplayProtectionPropertiesDialog( final Window parent,
                                             final WssReplayProtection assertion ) {
        super( WssReplayProtection.class, parent, assertion, true );
        initComponents();
        setData(assertion);
    }

    @Override
    public WssReplayProtection getData( final WssReplayProtection assertion ) throws ValidationException {
        validateData();

        if ( customRadioButton.isSelected() ) {
            assertion.setCustomProtection( true );
            assertion.setCustomExpiryTime( Integer.parseInt(expiryTextField.getText().trim()) * ((TimeUnit)expiryUnitComboBox.getSelectedItem()).getMultiplier() );
            assertion.setCustomIdentifierVariable( VariablePrefixUtil.fixVariableName(identifierVariableTextField.getText()) );
            assertion.setCustomScope( scopeTextField.getText().trim().length()>0 ? scopeTextField.getText().trim() : null );
        } else {
            assertion.setCustomProtection( false );
            assertion.setCustomExpiryTime( 0 );
            assertion.setCustomIdentifierVariable( null );
            assertion.setCustomScope( null );
        }

        return assertion;
    }

    @Override
    public void setData( final WssReplayProtection assertion ) {
        if ( assertion.isCustomProtection() ) {
            customRadioButton.setSelected( true );
            scopeTextField.setText( assertion.getCustomScope()==null ? "" : assertion.getCustomScope() );
            identifierVariableTextField.setText( assertion.getCustomIdentifierVariable() );
            int expiryTime = assertion.getCustomExpiryTime();
            TimeUnit expiryTimeUnit = TimeUnit.largestUnitForValue( expiryTime, TimeUnit.MINUTES );
            expiryTextField.setText( Integer.toString(expiryTime / expiryTimeUnit.getMultiplier()) );
            expiryUnitComboBox.setSelectedItem( expiryTimeUnit  );
        } else {
            defaultRadioButton.setSelected( true );
            scopeTextField.setText( "" );
            identifierVariableTextField.setText( "" );
            expiryTextField.setText( "5" );
            expiryUnitComboBox.setSelectedItem( TimeUnit.MINUTES  );
        }

        enableAndDisableComponents();
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        Utilities.setMaxLength( scopeTextField.getDocument(), 250 );
        Utilities.setMaxLength( identifierVariableTextField.getDocument(), 250 );
        Utilities.setMaxLength( expiryTextField.getDocument(), 10 );

        expiryUnitComboBox.setModel( new DefaultComboBoxModel( TimeUnit.ALL ) );
        expiryUnitComboBox.setSelectedItem( TimeUnit.MINUTES );

        RunOnChangeListener listener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        } );
        defaultRadioButton.addActionListener( listener );
        customRadioButton.addActionListener( listener );

        enableAndDisableComponents();
    }

    //- PRIVATE

    private static final ResourceBundle resources = ResourceBundle.getBundle( ReplayProtectionPropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JRadioButton defaultRadioButton;
    private JRadioButton customRadioButton;
    private JTextField scopeTextField;
    private JTextField identifierVariableTextField;
    private JTextField expiryTextField;
    private JComboBox expiryUnitComboBox;
    private JPanel customControlsPanel;

    private void enableAndDisableComponents() {
        if ( isReadOnly() ) {
            Utilities.setEnabled( mainPanel, false );
        } else {
            Utilities.setEnabled( customControlsPanel, customRadioButton.isSelected() );   
        }
    }

    private void validateData() throws ValidationException {
        if ( customRadioButton.isSelected() ) {
            if ( identifierVariableTextField.getText().trim().isEmpty() ) {
                throw new ValidationException( resources.getString( "error.missingVariableName" ));
            }

            int multiplier = ((TimeUnit)expiryUnitComboBox.getSelectedItem()).getMultiplier();
            if ( !ValidationUtils.isValidInteger( expiryTextField.getText().trim(), false, 1, Integer.MAX_VALUE / multiplier )) {
                throw new ValidationException( resources.getString( "error.invalidExpiry" ));
            }
        }
    }
}
