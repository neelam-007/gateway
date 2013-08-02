package com.l7tech.console.panels;

import com.l7tech.console.util.CipherSuiteGuiUtil;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.gateway.common.resources.HttpConfiguration;
import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.util.Functions;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.util.ResourceBundle;

/**
 * Properties dialog for HTTP configurations.
 */
public class HttpConfigurationPropertiesDialog extends JDialog {

    private static final ResourceBundle resources = ResourceBundle.getBundle( HttpConfigurationPropertiesDialog.class.getName() );

    private static final Object ANY = new Object(); // for use in models to represent "<ANY>"

    private JPanel mainPanel;
    private JButton okButton;
    private JButton cancelButton;
    private JSpinner connectionTimeoutSpinner;
    private JCheckBox connectionUseSystemDefaultCheckBox;
    private JSpinner readTimeoutSpinner;
    private JCheckBox readUseSystemDefaultCheckBox;
    private JCheckBox followRedirectsCheckBox;
    private JTextField hostTextField;
    private JTextField portTextField;
    private JTextField pathTextField;
    private JTextField usernameTextField;
    private JComboBox passwordComboBox;
    private JTextField ntlmDomainTextField;
    private JTextField ntlmHostTextField;
    private JComboBox tlsVersionComboBox;
    private JRadioButton useDefaultPrivateKeyRadioButton;
    private JRadioButton doNotUseAnyPrivateKeyRadioButton;
    private JRadioButton useCustomPrivateKeyRadioButton;
    private JComboBox privateKeyComboBox;
    private JButton managePrivateKeysButton;
    private JRadioButton useDefaultCipherSuitesRadioButton;
    private JRadioButton useCustomCipherSuitesRadioButton;
    private JButton cipherSuitesButton;
    private JComboBox protocolComboBox;
    private JRadioButton useDefaultHTTPProxyRadioButton;
    private JRadioButton doNotUseAnHTTPProxyRadioButton;
    private JRadioButton useSpecifiedHTTPProxyRadioButton;
    private JTextField proxyHostTextField;
    private JTextField proxyPortTextField;
    private JTextField proxyUsernameTextField;
    private JComboBox proxyPasswordComboBox;
    private JButton manageSecurePasswordsButton;
    private JButton proxyManageSecurePasswordsButton;
    private JLabel tlsVersionLabel;
    private JLabel tlsKeyLabel;
    private JTabbedPane tabbedPanel;
    private SecurityZoneWidget zoneControl;

    private boolean readOnly;
    private boolean wasOk;
    private HttpConfiguration httpConfiguration;
    private String tlsCipherSuiteList;

    public HttpConfigurationPropertiesDialog( final Window owner,
                                              final HttpConfiguration httpConfiguration,
                                              final boolean readOnly ) {
        super( owner, JDialog.DEFAULT_MODALITY_TYPE );
        this.httpConfiguration = httpConfiguration;
        this.readOnly = readOnly;
        init();
        modelToView( httpConfiguration );
        enableAndDisableComponents();
    }

    private void init() {
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setTitle( resources.getString("dialog.title") );
        add( mainPanel );

        final RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableAndDisableComponents();
            }
        };

        connectionTimeoutSpinner.setValue( 30 );
        readTimeoutSpinner.setValue( 60 );

        Utilities.setMaxLength( hostTextField.getDocument(), getMaxLength("host", 128) );
        Utilities.setMaxLength( pathTextField.getDocument(), getMaxLength("path", 4096) );
        Utilities.setMaxLength( usernameTextField.getDocument(), getMaxLength("username", 255) );
        Utilities.setMaxLength( ntlmDomainTextField.getDocument(), getMaxLength("ntlmDomain", 255) );
        Utilities.setMaxLength( ntlmHostTextField.getDocument(), getMaxLength("ntlmHost", 128) );
        Utilities.setMaxLength( proxyHostTextField.getDocument(), getProxyMaxLength("host", 128) );
        Utilities.setMaxLength( proxyUsernameTextField.getDocument(), getProxyMaxLength("username", 255) );

        connectionTimeoutSpinner.setModel(new SpinnerNumberModel(1,1,86400,1));
        readTimeoutSpinner.setModel(new SpinnerNumberModel(1,1,86400,1));

        final InputValidator validator = new InputValidator( this, getTitle() );
        validator.constrainTextFieldToBeNonEmpty( "Host", hostTextField, null );
        validator.constrainTextFieldToNumberRange( "Port", portTextField, 1, 0xFFFF, true );
        validator.addRule( new PasswordValidationRule( "Password", usernameTextField, (SecurePasswordComboBox)passwordComboBox ) );
        validator.constrainTextFieldToBeNonEmpty( "Proxy Host", proxyHostTextField, null );
        validator.constrainTextFieldToNumberRange( "Proxy Port", proxyPortTextField, 1, 0xFFFF );
        validator.addRule( new PasswordValidationRule( "Proxy Password", proxyUsernameTextField, (SecurePasswordComboBox)proxyPasswordComboBox ) );
        validator.addRule( new InputValidator.ComponentValidationRule(tlsVersionComboBox) {
            @Override
            public String getValidationError() {
                final Object item = tlsVersionComboBox.getSelectedItem();
                return item == null || item == ANY || CipherSuiteGuiUtil.isSupportedTlsVersion(item)
                        ? null
                        : "The selected TLS version is not available with the Gateway's current security provider configuration.";
            }
        });
        validator.attachToButton( okButton, new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doOk();
            }
        } );

        protocolComboBox.addActionListener( enableDisableListener );
        usernameTextField.getDocument().addDocumentListener( enableDisableListener );
        useDefaultPrivateKeyRadioButton.addActionListener( enableDisableListener );
        doNotUseAnyPrivateKeyRadioButton.addActionListener( enableDisableListener );
        useCustomPrivateKeyRadioButton.addActionListener( enableDisableListener );
        connectionUseSystemDefaultCheckBox.addActionListener( enableDisableListener );
        readUseSystemDefaultCheckBox.addActionListener( enableDisableListener );
        useDefaultHTTPProxyRadioButton.addActionListener( enableDisableListener );
        doNotUseAnHTTPProxyRadioButton.addActionListener( enableDisableListener );
        useSpecifiedHTTPProxyRadioButton.addActionListener( enableDisableListener );
        proxyUsernameTextField.getDocument().addDocumentListener( enableDisableListener );
        useDefaultCipherSuitesRadioButton.addActionListener( enableDisableListener );
        useCustomCipherSuitesRadioButton.addActionListener( enableDisableListener );

        protocolComboBox.setModel( new DefaultComboBoxModel( new Object[]{ ANY, HttpConfiguration.Protocol.HTTP, HttpConfiguration.Protocol.HTTPS } ) );
        protocolComboBox.setRenderer( new TextListCellRenderer<Object>( new Functions.Unary<String,Object>(){
            @Override
            public String call( final Object protocol ) {
                return protocol == ANY ? resources.getString("protocol.any") : ((HttpConfiguration.Protocol)protocol).name();
            }
        }, null, true ) );
        tlsVersionComboBox.setModel( new DefaultComboBoxModel( new Object[]{ ANY, "SSLv3", "TLSv1", "TLSv1.1", "TLSv1.2"} ) );
        tlsVersionComboBox.setRenderer( new TextListCellRenderer<Object>( new Functions.Unary<String,Object>(){
            @Override
            public String call( final Object protocol ) {
                return protocol == ANY ? resources.getString("tls-protocol.any") : resources.getString("tls-protocol." + protocol) ;
            }
        }, null, true ) );
        privateKeyComboBox.setRenderer( TextListCellRenderer.<Object>basicComboBoxRenderer() );
        privateKeyComboBox.setMinimumSize( new Dimension( 100, privateKeyComboBox.getPreferredSize().height  ) );
        privateKeyComboBox.setPreferredSize( new Dimension( 100, privateKeyComboBox.getPreferredSize().height ) );

        pathTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased( final KeyEvent e ) {
                updatePath();
            }
        });
        pathTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost( final FocusEvent e ) {
                updatePath();
            }
        });

        cipherSuitesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManageCipherSuites();
            }
        });

        managePrivateKeysButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManageKeys();
            }
        });
        manageSecurePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManagePasswords();
            }
        });
        proxyManageSecurePasswordsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                doManagePasswords();
            }
        });

        cancelButton.addActionListener( new ActionListener(){
            @Override
            public void actionPerformed( final ActionEvent e ) {
                doCancel();
            }
        } );
        zoneControl.configure(HttpConfiguration.DEFAULT_GOID.equals(httpConfiguration.getGoid()) ? OperationType.CREATE : readOnly ? OperationType.READ : OperationType.UPDATE, httpConfiguration);
        pack();
        setMinimumSize( getMinimumSize() );
        getRootPane().setDefaultButton( cancelButton );
        Utilities.setEscKeyStrokeDisposes( this );
        Utilities.centerOnParentWindow( this );
    }

    public boolean wasOk() {
        return wasOk;
    }

    private void doOk() {
        updatePath();
        viewToModel( httpConfiguration );
        wasOk = true;
        dispose();
    }

    private void doCancel() {
        dispose();
    }

    private void doManageCipherSuites() {
        CipherSuiteDialog.show(this, null, ModalityType.DOCUMENT_MODAL, readOnly, tlsCipherSuiteList, new Functions.UnaryVoid<String>() {
            @Override
            public void call(String s) {
                tlsCipherSuiteList = s;
            }
        });
    }

    private void doManageKeys() {
        final PrivateKeyManagerWindow privateKeyManagerWindow = new PrivateKeyManagerWindow(this);
        privateKeyManagerWindow.pack();
        Utilities.centerOnParentWindow(privateKeyManagerWindow);
        DialogDisplayer.display(privateKeyManagerWindow, new Runnable() {
            @Override
            public void run() {
                ((PrivateKeysComboBox)privateKeyComboBox).repopulate();
            }
        });
    }

    private void doManagePasswords() {
        final SecurePasswordComboBox passwordComboBox = (SecurePasswordComboBox)this.passwordComboBox;
        final SecurePassword password = passwordComboBox.getSelectedSecurePassword();

        final SecurePasswordComboBox proxyPasswordComboBox = (SecurePasswordComboBox)this.proxyPasswordComboBox;
        final SecurePassword proxyPassword = proxyPasswordComboBox.getSelectedSecurePassword();

        final SecurePasswordManagerWindow securePasswordManagerWindow = new SecurePasswordManagerWindow(this);
        securePasswordManagerWindow.pack();
        Utilities.centerOnParentWindow(securePasswordManagerWindow);
        DialogDisplayer.display(securePasswordManagerWindow, new Runnable() {
            @Override
            public void run() {
                passwordComboBox.reloadPasswordList();
                proxyPasswordComboBox.reloadPasswordList();

                if ( password != null ) {
                    passwordComboBox.setSelectedSecurePassword( password.getOid() );
                }
                if ( proxyPassword != null ) {
                    proxyPasswordComboBox.setSelectedSecurePassword( proxyPassword.getOid() );
                }
            }
        });
    }

    private void modelToView( final HttpConfiguration httpConfiguration ) {
        setText( hostTextField, httpConfiguration.getHost() );
        setText( portTextField, httpConfiguration.getPort()>0 ? Integer.toString(httpConfiguration.getPort()) : "");
        protocolComboBox.setSelectedItem( httpConfiguration.getProtocol() );
        if ( protocolComboBox.getSelectedItem()==null ) protocolComboBox.setSelectedIndex( 0 );
        setText( pathTextField, httpConfiguration.getPath() );
        setText( usernameTextField, httpConfiguration.getUsername() );
        if ( httpConfiguration.getPasswordOid()!=null )
            ((SecurePasswordComboBox)passwordComboBox).setSelectedSecurePassword( httpConfiguration.getPasswordOid() );
        setText( ntlmDomainTextField, httpConfiguration.getNtlmDomain() );
        setText( ntlmHostTextField, httpConfiguration.getNtlmHost() );

        tlsVersionComboBox.setSelectedItem( httpConfiguration.getTlsVersion() );
        if ( tlsVersionComboBox.getSelectedItem()==null ) tlsVersionComboBox.setSelectedIndex( 0 );
        switch ( httpConfiguration.getTlsKeyUse() ) {
            case DEFAULT:
                useDefaultPrivateKeyRadioButton.setSelected( true );
                break;
            case NONE:
                doNotUseAnyPrivateKeyRadioButton.setSelected( true );
                break;
            case CUSTOM:
                useCustomPrivateKeyRadioButton.setSelected( true );
                final PrivateKeysComboBox privateKeyDropDown = (PrivateKeysComboBox) privateKeyComboBox;
                privateKeyDropDown.select( httpConfiguration.getTlsKeystoreOid(), httpConfiguration.getTlsKeystoreAlias() );
                break;
        }

        tlsCipherSuiteList = httpConfiguration.getTlsCipherSuites();
        useDefaultCipherSuitesRadioButton.setSelected(tlsCipherSuiteList == null);
        useCustomCipherSuitesRadioButton.setSelected(tlsCipherSuiteList != null);

        if ( httpConfiguration.getConnectTimeout() == -1 ) {
            connectionUseSystemDefaultCheckBox.setSelected( true );
            connectionTimeoutSpinner.setValue( 30 );
        } else {
            connectionUseSystemDefaultCheckBox.setSelected( false );
            connectionTimeoutSpinner.setValue( httpConfiguration.getConnectTimeout() / 1000 );
        }
        if ( httpConfiguration.getReadTimeout() == -1 ) {
            readUseSystemDefaultCheckBox.setSelected( true );
            readTimeoutSpinner.setValue( 60 );
        } else {
            readUseSystemDefaultCheckBox.setSelected( false );
            readTimeoutSpinner.setValue( httpConfiguration.getReadTimeout() / 1000 );
        }
        followRedirectsCheckBox.setSelected( httpConfiguration.isFollowRedirects() );

        switch ( httpConfiguration.getProxyUse() ) {
            case DEFAULT:
                useDefaultHTTPProxyRadioButton.setSelected( true );
                break;
            case NONE:
                doNotUseAnHTTPProxyRadioButton.setSelected( true );
                break;
            case CUSTOM:
                useSpecifiedHTTPProxyRadioButton.setSelected( true );
                final HttpProxyConfiguration proxyConfiguration = httpConfiguration.getProxyConfiguration();
                if ( proxyConfiguration != null ) {
                    setText( proxyHostTextField, proxyConfiguration.getHost() );
                    setText( proxyPortTextField, proxyConfiguration.getPort() );
                    setText( proxyUsernameTextField, proxyConfiguration.getUsername() );
                    if ( proxyConfiguration.getPasswordOid()!=null )
                        ((SecurePasswordComboBox)proxyPasswordComboBox).setSelectedSecurePassword( proxyConfiguration.getPasswordOid() );
                }
                break;
        }
    }

    private void viewToModel( final HttpConfiguration httpConfiguration ) {
        httpConfiguration.setHost( getText( hostTextField, true ) );
        if ( portTextField.getText().isEmpty() ) {
            httpConfiguration.setPort( 0 );
        } else {
            httpConfiguration.setPort( Integer.parseInt( portTextField.getText() ) );
        }
        if ( protocolComboBox.getSelectedItem() == ANY ) {
            httpConfiguration.setProtocol( null );
        } else {
            httpConfiguration.setProtocol( (HttpConfiguration.Protocol) protocolComboBox.getSelectedItem() );
        }
        httpConfiguration.setPath( getText( pathTextField, true) );

        httpConfiguration.setUsername( getText( usernameTextField, true ) );
        if ( passwordComboBox.isEnabled() ) {
            httpConfiguration.setPasswordOid( ((SecurePasswordComboBox)passwordComboBox).getSelectedSecurePassword().getOid() );
        } else {
            httpConfiguration.setPasswordOid( null );
        }
        httpConfiguration.setNtlmDomain( getText( ntlmDomainTextField, true ) );
        httpConfiguration.setNtlmHost( getText( ntlmHostTextField, true ) );

        httpConfiguration.setTlsKeystoreOid( 0 );
        httpConfiguration.setTlsKeystoreAlias( null );
        if ( tlsVersionComboBox.isEnabled() ) {
            if ( tlsVersionComboBox.getSelectedItem() == ANY ) {
                httpConfiguration.setTlsVersion( null );
            } else {
                httpConfiguration.setTlsVersion( (String) tlsVersionComboBox.getSelectedItem() );
            }
            if ( useDefaultPrivateKeyRadioButton.isSelected() ) {
                httpConfiguration.setTlsKeyUse( HttpConfiguration.Option.DEFAULT );
            } else if ( doNotUseAnyPrivateKeyRadioButton.isSelected() ) {
                httpConfiguration.setTlsKeyUse( HttpConfiguration.Option.NONE );
            } else {
                httpConfiguration.setTlsKeyUse( HttpConfiguration.Option.CUSTOM );
                final PrivateKeysComboBox privateKeyDropDown = (PrivateKeysComboBox) privateKeyComboBox;
                httpConfiguration.setTlsKeystoreOid( privateKeyDropDown.getSelectedKeystoreId() );
                httpConfiguration.setTlsKeystoreAlias( privateKeyDropDown.getSelectedKeyAlias() );
            }
            httpConfiguration.setTlsCipherSuites( useCustomCipherSuitesRadioButton.isSelected() ? tlsCipherSuiteList : null );
        } else {
            httpConfiguration.setTlsVersion( null );
            httpConfiguration.setTlsKeyUse( HttpConfiguration.Option.DEFAULT );
            httpConfiguration.setTlsCipherSuites( null );
        }

        if ( connectionUseSystemDefaultCheckBox.isSelected() ) {
            httpConfiguration.setConnectTimeout( -1 );
        } else {
            httpConfiguration.setConnectTimeout( ((Integer)connectionTimeoutSpinner.getValue()) * 1000 );
        }
        if ( readUseSystemDefaultCheckBox.isSelected() ) {
            httpConfiguration.setReadTimeout( -1 );
        } else {
            httpConfiguration.setReadTimeout( ((Integer)readTimeoutSpinner.getValue()) * 1000 );
        }
        httpConfiguration.setFollowRedirects( followRedirectsCheckBox.isSelected() );

        if ( useDefaultHTTPProxyRadioButton.isSelected() ) {
            httpConfiguration.setProxyUse( HttpConfiguration.Option.DEFAULT );
            httpConfiguration.setProxyConfiguration( new HttpProxyConfiguration() );
        } else if ( doNotUseAnHTTPProxyRadioButton.isSelected() ) {
            httpConfiguration.setProxyUse( HttpConfiguration.Option.NONE );
            httpConfiguration.setProxyConfiguration( new HttpProxyConfiguration() );
        } else {
            final HttpProxyConfiguration proxyConfiguration = new HttpProxyConfiguration();
            proxyConfiguration.setHost( getText( proxyHostTextField, true ) );
            proxyConfiguration.setPort( Integer.parseInt( proxyPortTextField.getText() ) );
            proxyConfiguration.setUsername( getText( proxyUsernameTextField, true ) );
            if ( proxyPasswordComboBox.isEnabled() ) {
                proxyConfiguration.setPasswordOid( ((SecurePasswordComboBox)proxyPasswordComboBox).getSelectedSecurePassword().getOid() );
            } else {
                proxyConfiguration.setPasswordOid( null );    
            }
            httpConfiguration.setProxyUse( HttpConfiguration.Option.CUSTOM );
            httpConfiguration.setProxyConfiguration( proxyConfiguration );
        }
        httpConfiguration.setSecurityZone(zoneControl.getSelectedZone());
    }

    private void setText( final JTextComponent textComponent, final Object text ) {
        textComponent.setText( text==null ? "" : text.toString() );
        textComponent.setCaretPosition( 0 );
    }

    private String getText( final JTextComponent component, final boolean trim ) {
        String text = component.isEnabled() ? component.getText() : null;
        if ( text != null && trim ) {
            text = text.trim();
        }
        if ( text != null && text.isEmpty() ) {
            text = null;            
        }
        return text;
    }

    private int getMaxLength( final String property, final int defaultValue ) {
        return EntityUtil.getMaxFieldLength( HttpConfiguration.class, property, defaultValue );
    }

    private int getProxyMaxLength( final String property, final int defaultValue ) {
        return EntityUtil.getMaxFieldLength( HttpProxyConfiguration.class, property, defaultValue );
    }

    /**
     * Ensure path starts with a "/" if not empty.
     */
    private void updatePath() {
        String path = pathTextField.getText();
        if ( path != null && !path.isEmpty() && !path.startsWith("/") ) {
            final int caret = pathTextField.getCaretPosition();
            pathTextField.setText( "/" + path );
            pathTextField.setCaretPosition( caret + 1 );
        }
    }

    private void enableAndDisableComponents() {
        if ( readOnly ) {
            okButton.setEnabled( false );
            Utilities.setEnabled( tabbedPanel, false );
            tabbedPanel.setEnabled( true ); // the tabs are enabled, all contents are disabled
        } else {
            final boolean enablePasswordComponents = !usernameTextField.getText().trim().isEmpty();
            passwordComboBox.setEnabled( enablePasswordComponents );
            manageSecurePasswordsButton.setEnabled( enablePasswordComponents );

            final boolean enableTlsSettings = protocolComboBox.getSelectedItem() != HttpConfiguration.Protocol.HTTP;
            tlsVersionLabel.setEnabled( enableTlsSettings );
            tlsVersionComboBox.setEnabled( enableTlsSettings );
            useDefaultPrivateKeyRadioButton.setEnabled( enableTlsSettings );
            doNotUseAnyPrivateKeyRadioButton.setEnabled( enableTlsSettings );
            useCustomPrivateKeyRadioButton.setEnabled( enableTlsSettings );
            tlsKeyLabel.setEnabled( enableTlsSettings );
            useDefaultCipherSuitesRadioButton.setEnabled( enableTlsSettings );
            useCustomCipherSuitesRadioButton.setEnabled( enableTlsSettings );

            final boolean enableCipheSuiteSelection = useCustomCipherSuitesRadioButton.isSelected() && useCustomCipherSuitesRadioButton.isEnabled();
            cipherSuitesButton.setEnabled( enableCipheSuiteSelection );

            final boolean enableKeySelection = useCustomPrivateKeyRadioButton.isSelected() && useCustomPrivateKeyRadioButton.isEnabled();
            privateKeyComboBox.setEnabled( enableKeySelection );
            managePrivateKeysButton.setEnabled( enableKeySelection );

            final boolean enableConnectionTimeout = !connectionUseSystemDefaultCheckBox.isSelected() && connectionUseSystemDefaultCheckBox.isEnabled();
            connectionTimeoutSpinner.setEnabled( enableConnectionTimeout );

            final boolean enableReadTimeout = !readUseSystemDefaultCheckBox.isSelected() && readUseSystemDefaultCheckBox.isEnabled();
            readTimeoutSpinner.setEnabled( enableReadTimeout );

            final boolean enableProxySettings = useSpecifiedHTTPProxyRadioButton.isSelected() && useSpecifiedHTTPProxyRadioButton.isEnabled();
            proxyHostTextField.setEnabled( enableProxySettings );
            proxyPortTextField.setEnabled( enableProxySettings );
            proxyUsernameTextField.setEnabled( enableProxySettings );
            proxyPasswordComboBox.setEnabled( enableProxySettings && !proxyUsernameTextField.getText().trim().isEmpty() );
            proxyManageSecurePasswordsButton.setEnabled( enableProxySettings && !proxyUsernameTextField.getText().trim().isEmpty() );
        }
    }

    private void createUIComponents() {
        privateKeyComboBox = new PrivateKeysComboBox(true, false, false);
        passwordComboBox = new SecurePasswordComboBox();
        proxyPasswordComboBox = new SecurePasswordComboBox();
    }

    public static class PasswordValidationRule implements InputValidator.ValidationRule {
        private final String fieldName;
        private final JTextComponent usernameComponent;
        private final SecurePasswordComboBox passwordComboBox;

        public PasswordValidationRule( final String fieldName,
                                       final JTextComponent usernameComponent,
                                       final SecurePasswordComboBox passwordComboBox ) {
            this.fieldName = fieldName;
            this.usernameComponent = usernameComponent;
            this.passwordComboBox = passwordComboBox;
        }

        @Override
        public String getValidationError() {
            String error = null;

            if ( usernameComponent.getText() != null &&
                 !usernameComponent.getText().trim().isEmpty() &&
                 passwordComboBox.getSelectedSecurePassword() == null ) {
                error =  fieldName + " is required when credentials are used.";                        
            }

            return error;
        }
    }
}

