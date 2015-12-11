package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.WssReplayProtection;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.ValidationUtils;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;
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

        assertion.setBypassUniqueCheck(bypassUniqueCheckCheckBox.isSelected());
        assertion.setSaveIdAndExpiry(saveIdAndExpiryCheckBox.isSelected());
        if (saveIdAndExpiryCheckBox.isSelected()) {
            assertion.setVariablePrefix(prefixTargetVariablePanel.getVariable());
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

        bypassUniqueCheckCheckBox.setSelected(assertion.isBypassUniqueCheck());
        prefixTargetVariablePanel.setVariable(assertion.getVariablePrefix());
        saveIdAndExpiryCheckBox.setSelected(assertion.isSaveIdAndExpiry());

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

        expiryUnitComboBox.setModel( new DefaultComboBoxModel<>( TimeUnit.ALL ) );
        expiryUnitComboBox.setSelectedItem( TimeUnit.MINUTES );

        prefixTargetVariablePanel = new TargetVariablePanel();
        prefixVariablePanelHolder.setLayout(new BorderLayout());
        prefixVariablePanelHolder.add(prefixTargetVariablePanel, BorderLayout.CENTER);
        prefixTargetVariablePanel.setValueWillBeRead(false);
        prefixTargetVariablePanel.setAcceptEmpty(false);
        prefixTargetVariablePanel.setValueWillBeWritten(true);
        prefixTargetVariablePanel.setDefaultVariableOrPrefix(WssReplayProtection.VARIABLE_PREFIX);
        prefixTargetVariablePanel.setSuffixes(WssReplayProtection.getVariableSuffixes());

        RunOnChangeListener listener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        } );

        defaultRadioButton.addActionListener( listener );
        customRadioButton.addActionListener( listener );
        saveIdAndExpiryCheckBox.addActionListener(listener);
        bypassUniqueCheckCheckBox.addActionListener(listener);

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
    private JComboBox<TimeUnit> expiryUnitComboBox;
    private JPanel customControlsPanel;
    private JCheckBox bypassUniqueCheckCheckBox;
    private JPanel prefixVariablePanelHolder;
    private JCheckBox saveIdAndExpiryCheckBox;
    private TargetVariablePanel prefixTargetVariablePanel;

    private void enableAndDisableComponents() {
        if ( isReadOnly() ) {
            Utilities.setEnabled( mainPanel, false );
        } else {
            Utilities.setEnabled( customControlsPanel, customRadioButton.isSelected() );
            Utilities.setEnabled(prefixTargetVariablePanel, saveIdAndExpiryCheckBox.isSelected());

            if (bypassUniqueCheckCheckBox.isSelected()) {
                saveIdAndExpiryCheckBox.setSelected(true);
                Utilities.setEnabled(prefixTargetVariablePanel, true);
            }
        }
    }

    private void validateData() throws ValidationException {
        if (saveIdAndExpiryCheckBox.isSelected()) {
            String message = prefixTargetVariablePanel.getErrorMessage();

            if (null != message) {
                throw new ValidationException(MessageFormat.format(resources.getString("error.invalidPrefix"), message));
            }
        }

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
