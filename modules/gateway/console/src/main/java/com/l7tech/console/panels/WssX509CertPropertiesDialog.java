package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;

import javax.swing.*;
import java.util.*;
import java.awt.*;

/**
 * Properties dialog for the WSS X.509 certificate assertion.
 */
public class WssX509CertPropertiesDialog extends AssertionPropertiesOkCancelSupport<RequestWssX509Cert> {

    //- PUBLIC

    public WssX509CertPropertiesDialog( final Window owner, final RequestWssX509Cert assertion ) {
        super(RequestWssX509Cert.class, owner, resources.getString("dialog.title"), true);
        initComponents();
        setData(assertion);
    }

    @Override
    public RequestWssX509Cert getData( final RequestWssX509Cert assertion ) throws ValidationException {
        assertion.setAllowMultipleSignatures( allowMultipleSignatures.isSelected() );
        return assertion;
    }

    @Override
    public void setData( final RequestWssX509Cert assertion ) {
        this.allowMultipleSignatures.setSelected( assertion.isAllowMultipleSignatures() );
        doValidation();
    }

    public static void main( String[] args ) {
        new WssX509CertPropertiesDialog( null, new RequestWssX509Cert() ).setVisible(true);
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    //- PRIVATE

    private static final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.WssX509CertPropertiesDialog");

    private JPanel mainPanel;
    private JCheckBox allowMultipleSignatures;

    private void doValidation() {
        getOkButton().setEnabled( !isReadOnly() );
    }
}
