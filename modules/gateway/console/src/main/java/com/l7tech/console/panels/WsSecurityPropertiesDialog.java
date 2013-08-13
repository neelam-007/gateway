package com.l7tech.console.panels;

import com.l7tech.console.event.CertEvent;
import com.l7tech.console.event.CertListenerAdapter;
import com.l7tech.console.util.Registry;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.xmlsec.WsSecurity;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.xml.WsSecurityVersion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.logging.Logger;

import static com.l7tech.console.util.VariablePrefixUtil.fixVariableName;
import static com.l7tech.gui.util.Utilities.comboBoxModel;
import static com.l7tech.policy.variable.VariableMetadata.validateName;

/**
 * Assertion properties dialog for WssDecoration assertion.
 */
public class WsSecurityPropertiesDialog extends AssertionPropertiesOkCancelSupport<WsSecurity> {

    //- PUBLIC

    public WsSecurityPropertiesDialog( Window parent, WsSecurity assertion ) {
        super(WsSecurity.class, parent, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public WsSecurity getData( final WsSecurity assertion ) throws ValidationException {
        assertion.setApplyWsSecurity( applyWsSecurityCheckBox.isSelected() );
        assertion.setClearDecorationRequirements( clearWsSecurityCheckBox.isSelected() );

        assertion.setReplaceSecurityHeader( recreateSecurityHeaderCheckBox.isSelected() );
        assertion.setRemoveUnmatchedSecurityHeaders( removeUnmatchedSecurityHeadersCheckBox.isSelected( ) );
        assertion.setUseSecurityHeaderMustUnderstand( useMustUnderstandCheckBox.isSelected() );

        assertion.setUseSecureSpanActor( actorSecureSpanDefaultRadioButton.isSelected() );

        if ( applyWsSecurityCheckBox.isSelected() ) {
            assertion.setWsSecurityVersion( (WsSecurityVersion) wssVersionComboBox.getSelectedItem());

            assertion.setRecipientTrustedCertificateGoid(null);
            assertion.setRecipientTrustedCertificateName( null );
            assertion.setRecipientTrustedCertificateVariable( null );
            if ( selectedRecipientCertificateRadioButton.isSelected() ) {
                assertion.setRecipientTrustedCertificateGoid(recipientCertificateOid);
            } else if ( namedRecipientCertificateRadioButton.isSelected() ) {
                assertion.setRecipientTrustedCertificateName( lookupCertificateTextField.getText().trim() );
            } else if ( useCertificateFromVariableRadioButton.isSelected() ) {
                assertion.setRecipientTrustedCertificateVariable( fixVariableName( certificateTextField.getText().trim() ) );
            }
        } else {
            assertion.setWsSecurityVersion(null);
            assertion.setRecipientTrustedCertificateGoid(null);
            assertion.setRecipientTrustedCertificateName( null );
        }
        return assertion;
    }

    @Override
    public void setData( final WsSecurity assertion ) {
        applyWsSecurityCheckBox.setSelected( assertion.isApplyWsSecurity() );
        clearWsSecurityCheckBox.setSelected( assertion.isClearDecorationRequirements() );

        recreateSecurityHeaderCheckBox.setSelected( assertion.isReplaceSecurityHeader() );
        removeUnmatchedSecurityHeadersCheckBox.setSelected( assertion.isRemoveUnmatchedSecurityHeaders() );
        useMustUnderstandCheckBox.setSelected( assertion.isUseSecurityHeaderMustUnderstand() );

        actorOmitDefaultRadioButton.setSelected( !assertion.isUseSecureSpanActor() );
        actorSecureSpanDefaultRadioButton.setSelected( assertion.isUseSecureSpanActor() );

        wssVersionComboBox.setSelectedItem( assertion.getWsSecurityVersion() );

        if ( assertion.getRecipientTrustedCertificateGoid() != null ) {
            selectedRecipientCertificateRadioButton.setSelected( true );
            recipientCertificateOid = assertion.getRecipientTrustedCertificateGoid();
            try {
                TrustedCert certificate = Registry.getDefault().getTrustedCertManager().findCertByPrimaryKey( recipientCertificateOid );
                selectedCertificateNameTextField.setText( certificate==null ? "<Not Found>" : certificate.getName() );
                selectedCertificateSubjectTextField.setText( certificate==null ? "<Not Found>" : certificate.getCertificate().getSubjectDN().toString() );
                selectedCertificateIssuerTextField.setText( certificate==null ? "<Not Found>" : certificate.getCertificate().getIssuerDN().toString() );
                selectedCertificateNameTextField.setCaretPosition( 0 );
                selectedCertificateSubjectTextField.setCaretPosition( 0 );
                selectedCertificateIssuerTextField.setCaretPosition( 0 );
            } catch (FindException e) {
                logger.warning("Could not find the specified certificate in the trust store. " + ExceptionUtils.getMessage(e));
            }
        } else if ( assertion.getRecipientTrustedCertificateName() != null ) {
            namedRecipientCertificateRadioButton.setSelected( true );
            lookupCertificateTextField.setText( assertion.getRecipientTrustedCertificateName() );
        } else if ( assertion.getRecipientTrustedCertificateVariable() != null ) {
            useCertificateFromVariableRadioButton.setSelected( true );
            certificateTextField.setText( assertion.getRecipientTrustedCertificateVariable() );
        } else {
            defaultRecipientCertificateRadioButton.setSelected( true );
        }

        isResponse = Assertion.isResponse( assertion );

        updateState();
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        super.initComponents();

        List<WsSecurityVersion> options = new ArrayList<WsSecurityVersion>(EnumSet.allOf(WsSecurityVersion.class));
        options.add( 0, null );
        wssVersionComboBox.setModel( comboBoxModel(options) );
        wssVersionComboBox.setRenderer( new TextListCellRenderer<WsSecurityVersion>( new Functions.Unary<String,WsSecurityVersion>(){
            @Override
            public String call( final WsSecurityVersion wsSecurityVersion ) {
                return wsSecurityVersion != null ? wsSecurityVersion.toString() : "<Not Specified>";
            }
        }, null, true ) );

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateState();
            }
        } );
        selectedRecipientCertificateRadioButton.addActionListener( stateUpdateListener );
        namedRecipientCertificateRadioButton.addActionListener( stateUpdateListener );
        useCertificateFromVariableRadioButton.addActionListener( stateUpdateListener );
        defaultRecipientCertificateRadioButton.addActionListener( stateUpdateListener );
        applyWsSecurityCheckBox.addActionListener( stateUpdateListener );

        selectButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doSelectRecipientTrustedCertificate();
            }
        } );

        lookupCertificateTextField.getDocument().addDocumentListener( stateUpdateListener );
        certificateTextField.getDocument().addDocumentListener( stateUpdateListener );
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
    private JRadioButton useCertificateFromVariableRadioButton;
    private JTextField selectedCertificateNameTextField;
    private JTextField lookupCertificateTextField;
    private JTextField certificateTextField;
    private JButton selectButton;
    private JRadioButton defaultRecipientCertificateRadioButton;
    private JTextField selectedCertificateSubjectTextField;
    private JTextField selectedCertificateIssuerTextField;
    private JPanel applySecuritySettingsPanel;
    private JCheckBox clearWsSecurityCheckBox;

    private Goid recipientCertificateOid;
    private boolean isResponse;

    private void doSelectRecipientTrustedCertificate() {
        CertSearchPanel sp = new CertSearchPanel(this, false, true);
        sp.addCertListener( new CertListenerAdapter(){
            @Override
            public void certSelected( final CertEvent ce ) {
                TrustedCert cert = ce.getCert();
                recipientCertificateOid = cert.getGoid();
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

    private void updateState() {
        boolean enableApplySettings = applyWsSecurityCheckBox.isSelected();
        Utilities.setEnabled(applySecuritySettingsPanel, enableApplySettings );
        if ( isResponse ) {
            wssVersionComboBox.setEnabled( false );
        }

        boolean enableSelection = selectedRecipientCertificateRadioButton.isSelected();
        selectButton.setEnabled( !isReadOnly() && enableSelection );

        boolean enableCertName = namedRecipientCertificateRadioButton.isSelected();
        lookupCertificateTextField.setEnabled( !isReadOnly() && enableCertName );
        boolean enableCert = useCertificateFromVariableRadioButton.isSelected();
        certificateTextField.setEnabled( !isReadOnly() && enableCert );

        boolean canOk =
                (useCertificateFromVariableRadioButton.isSelected() && validateName( fixVariableName( certificateTextField.getText().trim() ) )==null) ||
                (namedRecipientCertificateRadioButton.isSelected() && !lookupCertificateTextField.getText().trim().isEmpty()) ||
                (selectedRecipientCertificateRadioButton.isSelected() && !selectedCertificateNameTextField.getText().isEmpty()) ||
                defaultRecipientCertificateRadioButton.isSelected();

        getOkButton().setEnabled( !isReadOnly() && canOk );
    }
}
