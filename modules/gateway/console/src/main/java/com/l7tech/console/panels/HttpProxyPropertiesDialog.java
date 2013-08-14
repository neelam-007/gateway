package com.l7tech.console.panels;

import com.l7tech.gateway.common.resources.HttpProxyConfiguration;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.objectmodel.EntityUtil;
import com.l7tech.util.ValidationUtils;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ResourceBundle;

/**
 * Properties dialog for HTTP proxy configurations.
 */
public class HttpProxyPropertiesDialog extends OkCancelDialog<HttpProxyConfiguration> {

    //- PUBLIC

    public HttpProxyPropertiesDialog( final Window owner,
                                      final HttpProxyConfiguration httpProxyConfiguration ) {
        super( owner, resources.getString("dialog.title"), true, new HttpProxyPanel(httpProxyConfiguration) );
        pack();
        setMinimumSize( getContentPane().getMinimumSize() );
        Utilities.centerOnParentWindow( this );
    }

    public static class HttpProxyPanel extends ValidatedPanel<HttpProxyConfiguration> {
        private final HttpProxyConfiguration httpProxyConfiguration;
        private JPanel mainPanel;
        private JTextField hostTextField;
        private JTextField usernameTextField;
        private JTextField portTextField;
        private JButton manageSecurePasswordsButton;
        private JComboBox passwordComboBox;

        public HttpProxyPanel( final HttpProxyConfiguration httpProxyConfiguration ) {
            this.httpProxyConfiguration = httpProxyConfiguration;
            init();
        }

        @Override
        protected HttpProxyConfiguration getModel() {
            return httpProxyConfiguration;
        }

        @Override
        protected void initComponents() {
            add(mainPanel);

            portTextField.setDocument( new NumberField( 5, 0xFFFF ) );
            Utilities.setMaxLength( hostTextField.getDocument(), EntityUtil.getMaxFieldLength(HttpProxyConfiguration.class, "host", 128));
            Utilities.setMaxLength( usernameTextField.getDocument(), EntityUtil.getMaxFieldLength(HttpProxyConfiguration.class, "username", 255));

            usernameTextField.getDocument().addDocumentListener( new RunOnChangeListener(){
                @Override
                public void run() {
                    enableAndDisableComponents();
                }
            } );
            manageSecurePasswordsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed( ActionEvent e) {
                    doManagePasswords();
                }
            });

            setText( hostTextField, httpProxyConfiguration.getHost() );
            setText( portTextField, httpProxyConfiguration.getPort() > 0 ? Integer.toString( httpProxyConfiguration.getPort()) : "" );
            setText( usernameTextField, httpProxyConfiguration.getUsername() );
            if ( httpProxyConfiguration.getPasswordGoid() != null )
                ((SecurePasswordComboBox)passwordComboBox).setSelectedSecurePassword( httpProxyConfiguration.getPasswordGoid() );

            enableAndDisableComponents();            
        }

        @Override
        public void focusFirstComponent() {
            hostTextField.requestFocus();
        }

        @Override
        protected void doUpdateModel() {
            checkValid();
            
            if ( hostTextField.getText().trim().isEmpty() ) {
                httpProxyConfiguration.setHost( null );
                httpProxyConfiguration.setPort( 0 );
                httpProxyConfiguration.setUsername( null );
                httpProxyConfiguration.setPasswordGoid(null);
            } else {
                httpProxyConfiguration.setHost( hostTextField.getText().trim() );
                httpProxyConfiguration.setPort( Integer.parseInt(portTextField.getText().trim()) );
                httpProxyConfiguration.setUsername( usernameTextField.getText() );
                if ( passwordComboBox.isEnabled() ) {
                    httpProxyConfiguration.setPasswordGoid(((SecurePasswordComboBox) passwordComboBox).getSelectedSecurePassword().getGoid());
                } else {
                    httpProxyConfiguration.setPasswordGoid(null);
                }
            }
        }

        private void enableAndDisableComponents() {
            final boolean enablePasswordComponents = !usernameTextField.getText().trim().isEmpty();
            passwordComboBox.setEnabled( enablePasswordComponents );
            manageSecurePasswordsButton.setEnabled( enablePasswordComponents );
        }

        private void checkValid() {
            final String host = hostTextField.getText().trim();
            if ( !host.isEmpty() ) {
                if ( !ValidationUtils.isValidUri( "http://" + host ) ) {
                    throw new IllegalStateException("Host is invalid.");
                }
            
                if ( !ValidationUtils.isValidInteger( portTextField.getText().trim(), false, 1, 0xFFFF ) ) {
                    throw new IllegalStateException("Port is invalid (1-65535)");
                }

                if ( passwordComboBox.isEnabled() && passwordComboBox.getSelectedItem()==null ) {
                    throw new IllegalStateException("Password is required when credentials are used.");
                }
            }

            checkSyntax();
        }

        private void setText( final JTextComponent textComponent, final String text ) {
            textComponent.setText( text==null ? "" : text );
            textComponent.setCaretPosition( 0 );
        }

        private void doManagePasswords() {
            final SecurePasswordManagerWindow securePasswordManagerWindow = new SecurePasswordManagerWindow(SwingUtilities.getWindowAncestor(this));
            securePasswordManagerWindow.pack();
            Utilities.centerOnParentWindow(securePasswordManagerWindow);
            DialogDisplayer.display(securePasswordManagerWindow, new Runnable() {
                @Override
                public void run() {
                    ((SecurePasswordComboBox)passwordComboBox).reloadPasswordList();
                }
            });
        }

        private void createUIComponents() {
            passwordComboBox = new SecurePasswordComboBox();
        }
    }

    //- PRIVATE

    private static final ResourceBundle resources = ResourceBundle.getBundle( HttpProxyPropertiesDialog.class.getName() );

}
