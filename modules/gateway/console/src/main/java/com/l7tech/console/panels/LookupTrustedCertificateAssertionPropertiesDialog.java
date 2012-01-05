package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.policy.assertion.xmlsec.LookupTrustedCertificateAssertion;

import javax.swing.*;
import java.awt.*;

/**
 * Properties dialog for Look Up Certificate assertion
 */
public class LookupTrustedCertificateAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<LookupTrustedCertificateAssertion> {

    private JPanel mainPanel;
    private JTextField trustedCertificateNameTextField;
    private JCheckBox allowMultipleCertificatesCheckBox;
    private TargetVariablePanel targetVariablePanel;

    public LookupTrustedCertificateAssertionPropertiesDialog( final Window owner,
                                                              final LookupTrustedCertificateAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData( final LookupTrustedCertificateAssertion assertion ) {
        trustedCertificateNameTextField.setText( assertion.getTrustedCertificateName() );
        allowMultipleCertificatesCheckBox.setSelected( assertion.isAllowMultipleCertificates() );
        targetVariablePanel.setAssertion( assertion, getPreviousAssertion() );
        targetVariablePanel.setVariable( assertion.getVariableName() );
        updateEnabledState();
    }

    @Override
    public LookupTrustedCertificateAssertion getData( final LookupTrustedCertificateAssertion assertion ) throws ValidationException {
        assertion.setTrustedCertificateName( trustedCertificateNameTextField.getText().trim() );
        assertion.setAllowMultipleCertificates( allowMultipleCertificatesCheckBox.isSelected() );
        assertion.setVariableName( targetVariablePanel.getVariable() );
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        final RunOnChangeListener buttonStateUpdateListener = new RunOnChangeListener(){
            @Override
            public void run() {
                updateEnabledState();
            }
        };

        trustedCertificateNameTextField.getDocument().addDocumentListener( buttonStateUpdateListener );
        targetVariablePanel.addChangeListener(buttonStateUpdateListener);

        return mainPanel;
    }

    private void updateEnabledState() {
        getOkButton().setEnabled( !isReadOnly() && targetVariablePanel.isEntryValid() && !trustedCertificateNameTextField.getText().trim().isEmpty() );
    }

}
