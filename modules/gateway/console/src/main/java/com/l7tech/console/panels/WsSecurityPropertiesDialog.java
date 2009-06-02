package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.event.CertEvent;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.FindException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.logging.Logger;

/**
 * Assertion properties dialog for WssDecoration assertion.
 */
public class WsSecurityPropertiesDialog extends AssertionPropertiesOkCancelSupport<WsSecurity> {

    //- PUBLIC

    public WsSecurityPropertiesDialog( Window parent, WsSecurity assertion ) {
        super(WsSecurity.class, parent, bundle.getString("dialog.title"), true);
        initComponents();
        setData(assertion);
    }

    @Override
    public WsSecurity getData( final WsSecurity assertion ) throws ValidationException {
        assertion.setApplyWsSecurity( applyWsSecurityCheckBox.isSelected() );

        assertion.setReplaceSecurityHeader( recreateSecurityHeaderCheckBox.isSelected() );
        assertion.setRemoveUnmatchedSecurityHeaders( removeUnmatchedSecurityHeadersCheckBox.isSelected( ) );
         assertion.setUseSecurityHeaderMustUnderstand( useMustUnderstandCheckBox.isSelected() );

        assertion.setUseSecureSpanActor( actorSecureSpanDefaultRadioButton.isSelected() );

        assertion.setWsSecurityVersion( (String)wssVersionComboBox.getSelectedItem() );

        assertion.setRecipientTrustedCertificateOid( 0L );
        assertion.setRecipientTrustedCertificateName( null );
        if ( selectedRecipientCertificateRadioButton.isSelected() ) {
            assertion.setRecipientTrustedCertificateOid( recipientCertificateOid );
        } else if ( namedRecipientCertificateRadioButton.isSelected() ) {
            assertion.setRecipientTrustedCertificateName( lookupCertificateTextField.getText().trim() );
        }

        return assertion;
    }

    @Override
    public void setData( final WsSecurity assertion ) {
        applyWsSecurityCheckBox.setSelected( assertion.isApplyWsSecurity() );

        recreateSecurityHeaderCheckBox.setSelected( assertion.isReplaceSecurityHeader() );
        removeUnmatchedSecurityHeadersCheckBox.setSelected( assertion.isRemoveUnmatchedSecurityHeaders() );
        useMustUnderstandCheckBox.setSelected( assertion.isUseSecurityHeaderMustUnderstand() );

        actorOmitDefaultRadioButton.setSelected( !assertion.isUseSecureSpanActor() );
        actorSecureSpanDefaultRadioButton.setSelected( assertion.isUseSecureSpanActor() );

        wssVersionComboBox.setSelectedItem( assertion.getWsSecurityVersion() );

        if ( assertion.getRecipientTrustedCertificateOid() > 0L ) {
            selectedRecipientCertificateRadioButton.setSelected( true );
            recipientCertificateOid = assertion.getRecipientTrustedCertificateOid();
            try {
                TrustedCert certificate = Registry.getDefault().getTrustedCertManager().findCertByPrimaryKey( recipientCertificateOid );
                selectedCertificateNameTextField.setText( certificate==null ? "<Not Found>" : certificate.getName() );
                selectedCertificateSubjectTextField.setText( certificate==null ? "<Not Found>" : certificate.getSubjectDn() );
                selectedCertificateIssuerTextField.setText( certificate==null ? "<Not Found>" : certificate.getIssuerDn() );
            } catch (FindException e) {
                logger.warning("Could not find the specified certificate in the trust store. " + ExceptionUtils.getMessage(e));
            }
        } else if ( assertion.getRecipientTrustedCertificateName() != null ) {
            namedRecipientCertificateRadioButton.setSelected( true );
            lookupCertificateTextField.setText( assertion.getRecipientTrustedCertificateName() );
        } else {
            defaultRecipientCertificateRadioButton.setSelected( true );
        }
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        super.initComponents();

        wssVersionComboBox.setModel( new DefaultComboBoxModel(new String[]{ "1.0", "1.1" }) );

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateState();
            }
        } );
        selectedRecipientCertificateRadioButton.addActionListener( stateUpdateListener );
        namedRecipientCertificateRadioButton.addActionListener( stateUpdateListener );
        defaultRecipientCertificateRadioButton.addActionListener( stateUpdateListener );

        selectButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doSelectRecipientTrustedCertificate();
            }
        } );
    }

    @Override
    protected void configureView() {
        super.configureView();
        updateState();
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }    

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( WsSecurityPropertiesDialog.class.getName() );
    private static final ResourceBundle bundle = ResourceBundle.getBundle(WsSecurityPropertiesDialog.class.getName());

    private JPanel mainPanel;
    private JCheckBox applyWsSecurityCheckBox;
    private JCheckBox recreateSecurityHeaderCheckBox;
    private JCheckBox removeUnmatchedSecurityHeadersCheckBox;
    private JCheckBox useMustUnderstandCheckBox;
    private JRadioButton actorOmitDefaultRadioButton;
    private JRadioButton actorSecureSpanDefaultRadioButton;
    private JComboBox wssVersionComboBox;
    private JRadioButton selectedRecipientCertificateRadioButton;
    private JRadioButton namedRecipientCertificateRadioButton;
    private JTextField selectedCertificateNameTextField;
    private JTextField lookupCertificateTextField;
    private JButton selectButton;
    private JRadioButton defaultRecipientCertificateRadioButton;
    private JTextField selectedCertificateSubjectTextField;
    private JTextField selectedCertificateIssuerTextField;

    private long recipientCertificateOid;

    private void doSelectRecipientTrustedCertificate() {
        CertSearchPanel sp = new CertSearchPanel(this);
        sp.addCertListener( new CertListenerAdapter(){
            @Override
            public void certSelected( final CertEvent ce ) {
                TrustedCert cert = ce.getCert();
                recipientCertificateOid = cert.getOid();
                selectedCertificateNameTextField.setText( cert.getName() );
                selectedCertificateSubjectTextField.setText( cert.getSubjectDn() );
                selectedCertificateIssuerTextField.setText( cert.getIssuerDn() );
            }
        });
        sp.pack();
        Utilities.centerOnScreen(sp);
        DialogDisplayer.display(sp);
    }

    private void updateState() {
        boolean enableSelection = selectedRecipientCertificateRadioButton.isSelected();
        selectButton.setEnabled( !isReadOnly() && enableSelection );        
    }
}
