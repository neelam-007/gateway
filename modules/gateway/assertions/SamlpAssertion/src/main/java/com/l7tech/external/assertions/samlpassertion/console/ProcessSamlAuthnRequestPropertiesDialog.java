package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion;
import com.l7tech.gui.util.PauseListener;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.TextComponentPauseListenerManager;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Properties dialog for the Process SAML AuthnRequest assertion.
 */
public class ProcessSamlAuthnRequestPropertiesDialog extends AssertionPropertiesOkCancelSupport<ProcessSamlAuthnRequestAssertion> {

    //- PUBLIC

    public ProcessSamlAuthnRequestPropertiesDialog( final Window parent,
                                                    final ProcessSamlAuthnRequestAssertion assertion ) {
        super( ProcessSamlAuthnRequestAssertion.class, parent, assertion, true );
        initComponents();
        setData(assertion);
    }

    @Override
    public ProcessSamlAuthnRequestAssertion getData( final ProcessSamlAuthnRequestAssertion assertion ) throws ValidationException {
        if ( extractSAMLRequestFromCheckBox.isSelected() ) {
            assertion.setSamlProtocolBinding( (ProcessSamlAuthnRequestAssertion.SamlProtocolBinding) bindingComboBox.getSelectedItem() );   
        } else {
            assertion.setSamlProtocolBinding( null );
        }
        assertion.setCheckValidityPeriod( checkValidityPeriodCheckBox.isSelected() );
        assertion.setAudienceRestriction( getText(audienceRestrictionTextField) );
        assertion.setVerifySignature( verifySignatureCheckBox.isEnabled() && verifySignatureCheckBox.isSelected() );
        assertion.setVariablePrefix( variablePrefixTextField.getText() );

        return assertion;
    }

    @Override
    public void setData( final ProcessSamlAuthnRequestAssertion assertion ) {
        this.assertion = assertion;

        if ( assertion.getSamlProtocolBinding() != null ) {
            extractSAMLRequestFromCheckBox.setSelected( true );
            bindingComboBox.setSelectedItem( assertion.getSamlProtocolBinding() );
        } else {
            extractSAMLRequestFromCheckBox.setSelected( false );
            bindingComboBox.setSelectedIndex( 0 );
        }
        checkValidityPeriodCheckBox.setSelected( assertion.isCheckValidityPeriod() );
        setText( audienceRestrictionTextField, assertion.getAudienceRestriction() );
        verifySignatureCheckBox.setSelected( assertion.isVerifySignature() );
        setText( variablePrefixTextField, assertion.getVariablePrefix() );

        enableDisableComponents();
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

        @Override
    protected void initComponents() {
        super.initComponents();

        bindingComboBox.setModel( new DefaultComboBoxModel( ProcessSamlAuthnRequestAssertion.SamlProtocolBinding.values() ) );
        bindingComboBox.setRenderer( new TextListCellRenderer<ProcessSamlAuthnRequestAssertion.SamlProtocolBinding>( new Functions.Unary<String,ProcessSamlAuthnRequestAssertion.SamlProtocolBinding>(){
            @Override
            public String call( final ProcessSamlAuthnRequestAssertion.SamlProtocolBinding binding ) {
                try {
                    return resourceBundle.getString( "bindings." + binding.name() + ".label" );
                } catch ( MissingResourceException mre ) {
                    return binding.toString();
                }
            }
        } ) );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        bindingComboBox.addActionListener( enableDisableListener );
        extractSAMLRequestFromCheckBox.addActionListener( enableDisableListener );

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

    //- PRIVATE

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle( ProcessSamlAuthnRequestPropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JCheckBox extractSAMLRequestFromCheckBox;
    private JComboBox bindingComboBox;
    private JTextField variablePrefixTextField;
    private JCheckBox verifySignatureCheckBox;
    private JLabel varPrefixStatusLabel;
    private JCheckBox checkValidityPeriodCheckBox;
    private JTextField audienceRestrictionTextField;

    private ProcessSamlAuthnRequestAssertion assertion;

    private String getText( final JTextComponent textComponent ) {
        String text = null;

        if ( textComponent.isEnabled() && textComponent.getText() != null && !textComponent.getText().trim().isEmpty() ) {
            text = textComponent.getText().trim();
        }

        return text;
    }

    private void setText( final JTextComponent textComponent, final String text ) {
        if ( text != null ) {
            textComponent.setText( text );
            textComponent.setCaretPosition( 0 );
        }
    }

    private void enableDisableComponents() {
        boolean enableAny = !isReadOnly();

        boolean enableBindingSelection = extractSAMLRequestFromCheckBox.isSelected();
        bindingComboBox.setEnabled( enableAny && enableBindingSelection );

        boolean enableSignature = !bindingComboBox.isEnabled() || ProcessSamlAuthnRequestAssertion.SamlProtocolBinding.HttpRedirect != bindingComboBox.getSelectedItem();
        verifySignatureCheckBox.setEnabled( enableSignature );

        getOkButton().setEnabled( enableAny );
    }

    private boolean validateVariablePrefix() {
        String[] suffixes = ProcessSamlAuthnRequestAssertion.VARIABLE_SUFFIXES.toArray( new String[ProcessSamlAuthnRequestAssertion.VARIABLE_SUFFIXES.size()] );

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
