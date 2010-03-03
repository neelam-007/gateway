package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapVerifyElementAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.CertSearchPanel;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.logging.Logger;

public class NonSoapVerifyElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapVerifyElementAssertion> {

    private static final Logger logger = Logger.getLogger( NonSoapVerifyElementAssertionPropertiesDialog.class.getName() );

    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;
    private JRadioButton certSelectRadioButton;
    private JRadioButton certLookupRadioButton;
    private JTextField lookupCertificateTextField;
    private JTextField selectedCertificateNameTextField;
    private JTextField selectedCertificateSubjectTextField;
    private JTextField selectedCertificateIssuerTextField;
    private JButton selectButton;
    private JCheckBox keyInfoOverrideCheckBox;
    private JRadioButton certExpectKeyInfoRadioButton;

    private long selectedVerifyCertificateOid;

    public NonSoapVerifyElementAssertionPropertiesDialog(Window owner, NonSoapVerifyElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getXpathExpressionLabel().setText("dsig:Signature element(s) to verify XPath:");

        // create extra panel components -- copied from WsSecurityAssertion
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    protected JPanel createExtraPanel() {

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateState();
            }
        } );

        keyInfoOverrideCheckBox.addActionListener( stateUpdateListener );
        certExpectKeyInfoRadioButton.addActionListener( stateUpdateListener );
        certSelectRadioButton.addActionListener( stateUpdateListener );
        certLookupRadioButton.addActionListener( stateUpdateListener );

        selectButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doSelectRecipientTrustedCertificate();
            }
        } );
        lookupCertificateTextField.getDocument().addDocumentListener( stateUpdateListener );
        
        return contentPane;
    }

    private void doSelectRecipientTrustedCertificate() {
        CertSearchPanel sp = new CertSearchPanel(this, false, true);
        sp.addCertListener( new CertListenerAdapter(){
            @Override
            public void certSelected( final CertEvent ce ) {
                TrustedCert cert = ce.getCert();
                selectedVerifyCertificateOid = cert.getOid();
                selectedCertificateNameTextField.setText( cert.getName() );
                selectedCertificateSubjectTextField.setText( cert.getCertificate().getSubjectDN().toString() );
                selectedCertificateIssuerTextField.setText( cert.getCertificate().getIssuerDN().toString() );
                selectedCertificateNameTextField.setCaretPosition( 0 );
                selectedCertificateSubjectTextField.setCaretPosition( 0 );
                selectedCertificateIssuerTextField.setCaretPosition( 0 );
                updateState();
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }

    @Override
    public void setData(NonSoapVerifyElementAssertion assertion) {
        super.setData(assertion);

        if (assertion.getVerifyCertificateOid() > 0L) {
            certSelectRadioButton.setSelected(true);
            keyInfoOverrideCheckBox.setSelected(assertion.isIgnoreKeyInfo());
            selectedVerifyCertificateOid = assertion.getVerifyCertificateOid();
            
            // fill out the selected cert details
            try {
                TrustedCert certificate = Registry.getDefault().getTrustedCertManager().findCertByPrimaryKey( selectedVerifyCertificateOid );
                selectedCertificateNameTextField.setText( certificate==null ? "<Not Found>" : certificate.getName() );
                selectedCertificateSubjectTextField.setText( certificate==null ? "<Not Found>" : certificate.getSubjectDn() );
                selectedCertificateIssuerTextField.setText( certificate==null ? "<Not Found>" : certificate.getIssuerDn() );
                selectedCertificateNameTextField.setCaretPosition( 0 );
                selectedCertificateSubjectTextField.setCaretPosition( 0 );
                selectedCertificateIssuerTextField.setCaretPosition( 0 );
            } catch (FindException e) {
                logger.warning("Could not find the specified certificate in the trust store. " + ExceptionUtils.getMessage(e));
            }

        } else if (assertion.getVerifyCertificateName() != null && assertion.getVerifyCertificateName().length() > 0) {
            certLookupRadioButton.setSelected(true);
            selectedVerifyCertificateOid = -1;
            lookupCertificateTextField.setText(assertion.getVerifyCertificateName());
            keyInfoOverrideCheckBox.setSelected(assertion.isIgnoreKeyInfo());

        } else {
            certExpectKeyInfoRadioButton.setSelected(true);
            selectedVerifyCertificateOid = -1;
            lookupCertificateTextField.setText(null);
            keyInfoOverrideCheckBox.setSelected(false);
        }

        updateState();
    }

    @Override
    public NonSoapVerifyElementAssertion getData(NonSoapVerifyElementAssertion assertion) throws ValidationException {
        assertion = super.getData(assertion);

        if (certSelectRadioButton.isSelected()) {
            assertion.setVerifyCertificateOid(selectedVerifyCertificateOid);
            assertion.setIgnoreKeyInfo(keyInfoOverrideCheckBox.isSelected());

        } else if (certLookupRadioButton.isSelected()) {
            assertion.setVerifyCertificateName(lookupCertificateTextField.getText().trim());
            assertion.setVerifyCertificateOid(-1);
            assertion.setIgnoreKeyInfo(keyInfoOverrideCheckBox.isSelected());

        } else {
            assertion.setVerifyCertificateName(null);
            assertion.setVerifyCertificateOid(-1);
        }
        
        return assertion;
    }

    /**
     * Update the state of any/all components based on UI events
     */
    private void updateState() {
        keyInfoOverrideCheckBox.setEnabled(certSelectRadioButton.isSelected() || certLookupRadioButton.isSelected());
        lookupCertificateTextField.setEnabled(certLookupRadioButton.isSelected());
        selectButton.setEnabled(certSelectRadioButton.isSelected());
        
        boolean canOk =
                (certSelectRadioButton.isSelected() && !selectedCertificateNameTextField.getText().isEmpty()) ||
                (certLookupRadioButton.isSelected() && !lookupCertificateTextField.getText().isEmpty()) ||
                (certExpectKeyInfoRadioButton.isSelected());
        getOkButton().setEnabled( canOk );
    }
}
