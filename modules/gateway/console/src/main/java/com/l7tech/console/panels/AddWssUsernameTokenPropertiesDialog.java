package com.l7tech.console.panels;

import com.l7tech.policy.assertion.xmlsec.AddWssUsernameToken;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.gui.util.RunOnChangeListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.ResourceBundle;

/**
 * Assertion properties editor for WS-Security UsernameToken.
 */
public class AddWssUsernameTokenPropertiesDialog extends AssertionPropertiesOkCancelSupport<AddWssUsernameToken> {

    //- PUBLIC

    public AddWssUsernameTokenPropertiesDialog( final Window parent, final AddWssUsernameToken assertion ) {
        super( AddWssUsernameToken.class, parent, assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString(), true );
        initComponents();
        setData(assertion);
    }

    @Override
    public AddWssUsernameToken getData( final AddWssUsernameToken assertion ) throws ValidationException {

        assertion.setUsername( usernameTextField.getText().trim() );
        if ( includePasswordCheckBox.isSelected() ) {
            assertion.setIncludePassword( true );
            assertion.setPassword( new String(passwordField.getPassword()) );
            assertion.setDigest( digestPasswordCheckBox.isSelected() );
        } else {
            assertion.setIncludePassword( false );
            assertion.setPassword( null );
            assertion.setDigest( false );
        }
        assertion.setIncludeNonce( includeNonceCheckBox.isSelected() );
        assertion.setIncludeCreated( includeCreatedCheckBox.isSelected() );
        assertion.setEncrypt( encryptCheckBox.isSelected() );

        return assertion;
    }

    @Override
    public void setData( final AddWssUsernameToken assertion ) {
        usernameTextField.setText( assertion.getUsername() == null ? "" : assertion.getUsername() );
        passwordField.setText( assertion.getPassword() == null ? "" : assertion.getPassword() );
        includePasswordCheckBox.setSelected( assertion.isIncludePassword() );
        includeNonceCheckBox.setSelected( assertion.isIncludeNonce() );
        includeCreatedCheckBox.setSelected( assertion.isIncludeCreated() );
        digestPasswordCheckBox.setSelected( assertion.isDigest() );
        encryptCheckBox.setSelected( assertion.isEncrypt() );

        updateState();
    }

    //- PROTECTED

    @Override
    protected void initComponents() {
        super.initComponents();

        showPasswordCheckBox.setSelected( false );
        showPasswordCheckBox.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                char echoChar = showPasswordCheckBox.isSelected() ?
                        0 :
                        '*';
                passwordField.setEchoChar( echoChar ); 
            }
        } );

        RunOnChangeListener stateUpdateListener = new RunOnChangeListener( new Runnable(){
            @Override
            public void run() {
                updateState();
            }
        } );

        includePasswordCheckBox.addActionListener( stateUpdateListener );
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

    private static final ResourceBundle bundle = ResourceBundle.getBundle( AddWssUsernameTokenPropertiesDialog.class.getName() );

    private JPanel mainPanel;
    private JTextField usernameTextField;
    private JPasswordField passwordField;
    private JCheckBox showPasswordCheckBox;
    private JCheckBox includePasswordCheckBox;
    private JCheckBox includeNonceCheckBox;
    private JCheckBox includeCreatedCheckBox;
    private JCheckBox digestPasswordCheckBox;
    private JCheckBox encryptCheckBox;

    private void updateState() {
        boolean enablePasswordSettings = includePasswordCheckBox.isSelected();
        passwordField.setEnabled( enablePasswordSettings );
        showPasswordCheckBox.setEnabled( enablePasswordSettings );
        digestPasswordCheckBox.setEnabled( enablePasswordSettings );  

        getOkButton().setEnabled( !isReadOnly() );
    }
}
