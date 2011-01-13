package com.l7tech.console.panels;

import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.xmlsec.CancelSecurityContext;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author ghuang
 */
public class CancelSecurityContextPropertiesDialog extends AssertionPropertiesOkCancelSupport<CancelSecurityContext> {

    private static final ResourceBundle resources = ResourceBundle.getBundle( CancelSecurityContextPropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JCheckBox failIfNotExistCheckBox;
    private JComboBox permitCancellationComboBox;
    private JTextField serviceUrlTextField;
    private JRadioButton inboundSecureConversationSessionRadioButton;
    private JRadioButton outboundSecureConversationSessionRadioButton;

    public CancelSecurityContextPropertiesDialog( final Window owner,
                                                  final CancelSecurityContext assertion ) {
        super(CancelSecurityContext.class, owner, assertion, true);
        initComponents();
        setData(assertion);
    }

    @Override
    public void setData( final CancelSecurityContext assertion ) {
        modelToView(assertion);
    }

    @Override
    public CancelSecurityContext getData( final CancelSecurityContext assertion ) {
        viewToModel(assertion);
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        permitCancellationComboBox.setModel( new DefaultComboBoxModel( CancelSecurityContext.AuthorizationType.values() ) );
        permitCancellationComboBox.setRenderer( new TextListCellRenderer<CancelSecurityContext.AuthorizationType>( new Functions.Unary<String,CancelSecurityContext.AuthorizationType>(){
            @Override
            public String call( final CancelSecurityContext.AuthorizationType authorizationType ) {
                return resources.getString( "permit-cancellation." + authorizationType );
            }
        } ) );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            protected void run() {
                enableDisableComponents();
            }
        };

        inboundSecureConversationSessionRadioButton.addActionListener( enableDisableListener );
        outboundSecureConversationSessionRadioButton.addActionListener( enableDisableListener );
        serviceUrlTextField.getDocument().addDocumentListener( enableDisableListener );

        pack();
        setMinimumSize( getContentPane().getMinimumSize() );
        Utilities.centerOnParentWindow(this);
    }

    private void enableDisableComponents(){
        boolean enableOk = true;

        permitCancellationComboBox.setEnabled( inboundSecureConversationSessionRadioButton.isSelected() );
        serviceUrlTextField.setEnabled( outboundSecureConversationSessionRadioButton.isSelected() );

        if ( outboundSecureConversationSessionRadioButton.isSelected() && serviceUrlTextField.getText().trim().isEmpty() ) {
            enableOk = false;
        }

        getOkButton().setEnabled( enableOk && !isReadOnly() );
    }

    private void modelToView( final CancelSecurityContext assertion ) {
        if ( assertion.isCancelInbound() ) {
            inboundSecureConversationSessionRadioButton.setSelected( true );
        } else {
            outboundSecureConversationSessionRadioButton.setSelected( true );
        }

        if (  assertion.isCancelInbound() && assertion.getRequiredAuthorization() != null ) {
            permitCancellationComboBox.setSelectedItem(assertion.getRequiredAuthorization());
        } else {
            permitCancellationComboBox.setSelectedIndex( 0 );
        }

        if ( !assertion.isCancelInbound() && assertion.getOutboundServiceUrl() != null ) {
            serviceUrlTextField.setText( assertion.getOutboundServiceUrl() );
            serviceUrlTextField.setCaretPosition(0);
        } else {
            serviceUrlTextField.setText( "" );
        }

        failIfNotExistCheckBox.setSelected(assertion.isFailIfNotExist());
        enableDisableComponents();
    }

    private void viewToModel( final CancelSecurityContext assertion ) {
        if ( inboundSecureConversationSessionRadioButton.isSelected() ) {
            assertion.setCancelInbound( true );
            assertion.setRequiredAuthorization( (CancelSecurityContext.AuthorizationType)permitCancellationComboBox.getSelectedItem() );
            assertion.setOutboundServiceUrl( null );
        } else {
            assertion.setCancelInbound( false );
            assertion.setRequiredAuthorization( null ); // sets to default
            assertion.setOutboundServiceUrl( serviceUrlTextField.getText().trim() );
        }
        assertion.setFailIfNotExist(failIfNotExistCheckBox.isSelected());
    }
}