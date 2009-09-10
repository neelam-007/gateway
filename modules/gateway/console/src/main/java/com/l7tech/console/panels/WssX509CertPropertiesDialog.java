package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.console.util.VariablePrefixUtil;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Properties dialog for the WSS X.509 certificate assertion.
 */
public class WssX509CertPropertiesDialog extends AssertionPropertiesOkCancelSupport<RequireWssX509Cert> {

    //- PUBLIC

    public WssX509CertPropertiesDialog( final Window owner, final RequireWssX509Cert assertion ) {
        super(RequireWssX509Cert.class, assertion, owner, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public RequireWssX509Cert getData( final RequireWssX509Cert assertion ) throws ValidationException {
        assertion.setAllowMultipleSignatures( allowMultipleSignatures.isSelected() );
        if ( assertion.isAllowMultipleSignatures() ) {
            String variable = VariablePrefixUtil.fixVariableName(signatureElementVariableTextField.getText());
            if ( variable.length() > 0 ) {
                assertion.setSignatureElementVariable( variable );
            } else {
                assertion.setSignatureElementVariable( null );
            }

            String referenceVariable = VariablePrefixUtil.fixVariableName(signatureReferenceTextField.getText());
            if ( referenceVariable.length() > 0 ) {
                assertion.setSignatureReferenceElementVariable( referenceVariable );
            } else {
                assertion.setSignatureReferenceElementVariable( null );
            }
        } else {
            assertion.setSignatureElementVariable( null );
            assertion.setSignatureReferenceElementVariable( null );
        }
        return assertion;
    }

    @Override
    public void setData( final RequireWssX509Cert assertion ) {
        allowMultipleSignatures.setSelected( assertion.isAllowMultipleSignatures() );
        signatureElementVariableTextField.setText( assertion.getSignatureElementVariable() );
        signatureReferenceTextField.setText( assertion.getSignatureReferenceElementVariable() );
        doValidation();
        doUpdateEnabledState();
    }

    public static void main( String[] args ) {
        new WssX509CertPropertiesDialog( null, new RequireWssX509Cert() ).setVisible(true);
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        allowMultipleSignatures.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doUpdateEnabledState();
            }
        } );

        return mainPanel;
    }

    //- PRIVATE

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.WssX509CertPropertiesDialog");

    private JPanel mainPanel;
    private JCheckBox allowMultipleSignatures;
    private JTextField signatureElementVariableTextField;
    private JLabel signatureElementVariableLabel;
    private JTextField signatureReferenceTextField;
    private JLabel signatureReferenceLabel;

    private void doValidation() {
        getOkButton().setEnabled( !isReadOnly() );
    }

    private void doUpdateEnabledState() {
        boolean enabled = allowMultipleSignatures.isSelected();
        signatureElementVariableTextField.setEnabled( enabled );
        signatureElementVariableLabel.setEnabled( enabled );
        signatureReferenceTextField.setEnabled( enabled );
        signatureReferenceLabel.setEnabled( enabled );
    }
}
