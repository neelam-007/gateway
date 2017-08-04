package com.l7tech.external.assertions.samlpassertion.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAuthnRequestAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
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
        assertion.setVerifySignature( verifySignatureCheckBox.isEnabled() && verifySignatureCheckBox.isSelected() );
        assertion.setVariablePrefix( variablePrefixTextField.getVariable() );

        assertion.setRequiredAssertionConsumerServiceIndex(assertionConsumerServiceIndexCheckBox.isEnabled() && assertionConsumerServiceIndexCheckBox.isSelected());
        assertion.setRequiredAssertionConsumerServiceURL(assertionConsumerServiceURLCheckBox.isEnabled() && assertionConsumerServiceURLCheckBox.isSelected());
        assertion.setRequiredAttributeConsumingServiceIndex(attributeConsumingServiceIndexCheckBox.isEnabled() && attributeConsumingServiceIndexCheckBox.isSelected());
        assertion.setRequiredProtocolBinding(protocolBindingCheckBox.isEnabled() && protocolBindingCheckBox.isSelected());
        assertion.setRequiredProviderName(providerNameCheckBox.isEnabled() && providerNameCheckBox.isSelected());

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
        verifySignatureCheckBox.setSelected( assertion.isVerifySignature() );
        this.variablePrefixTextField.setVariable(assertion.getVariablePrefix() );
        this.variablePrefixTextField.setAssertion(assertion,getPreviousAssertion());

        String[] suffixes = ProcessSamlAuthnRequestAssertion.VARIABLE_SUFFIXES.toArray( new String[ProcessSamlAuthnRequestAssertion.VARIABLE_SUFFIXES.size()] );
        variablePrefixTextField.setSuffixes(suffixes);

        assertionConsumerServiceIndexCheckBox.setSelected(assertion.isRequiredAssertionConsumerServiceIndex());
        assertionConsumerServiceURLCheckBox.setSelected(assertion.isRequiredAssertionConsumerServiceURL());
        attributeConsumingServiceIndexCheckBox.setSelected(assertion.isRequiredAttributeConsumingServiceIndex());
        protocolBindingCheckBox.setSelected(assertion.isRequiredProtocolBinding());
        providerNameCheckBox.setSelected(assertion.isRequiredProviderName());

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
        bindingComboBox.setRenderer(new TextListCellRenderer<>((Functions.Unary<String, ProcessSamlAuthnRequestAssertion.SamlProtocolBinding>) binding -> {
            try {
                return resourceBundle.getString("bindings." + binding.name() + ".label");
            } catch (MissingResourceException mre) {
                return binding.toString();
            }
        }) );

        variablePrefixTextField = new TargetVariablePanel();
        variablePrefixTextFieldPanel.setLayout(new BorderLayout());
        variablePrefixTextFieldPanel.add(variablePrefixTextField, BorderLayout.CENTER);

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        bindingComboBox.addActionListener( enableDisableListener );
        extractSAMLRequestFromCheckBox.addActionListener( enableDisableListener );
        variablePrefixTextField.addChangeListener(enableDisableListener);

        if ( isReadOnly() ) {
            variablePrefixTextField.setEnabled( false );
        }

    }

    //- PRIVATE

    private static final ResourceBundle resourceBundle = ResourceBundle.getBundle( ProcessSamlAuthnRequestPropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JCheckBox extractSAMLRequestFromCheckBox;
    private JComboBox bindingComboBox;
    private JPanel variablePrefixTextFieldPanel;
    private TargetVariablePanel variablePrefixTextField;
    private JCheckBox verifySignatureCheckBox;
    private JCheckBox assertionConsumerServiceURLCheckBox;
    private JCheckBox assertionConsumerServiceIndexCheckBox;
    private JCheckBox attributeConsumingServiceIndexCheckBox;
    private JCheckBox providerNameCheckBox;
    private JCheckBox protocolBindingCheckBox;
    private ProcessSamlAuthnRequestAssertion assertion;

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

        assertionConsumerServiceURLCheckBox.setEnabled(enableAny);
        assertionConsumerServiceIndexCheckBox.setEnabled(enableAny);
        attributeConsumingServiceIndexCheckBox.setEnabled(enableAny);
        providerNameCheckBox.setEnabled(enableAny);
        protocolBindingCheckBox.setEnabled(enableAny);

        getOkButton().setEnabled( enableAny && variablePrefixTextField.isEntryValid());
    }
}
