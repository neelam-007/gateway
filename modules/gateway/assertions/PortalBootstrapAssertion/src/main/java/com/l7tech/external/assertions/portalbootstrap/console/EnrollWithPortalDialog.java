package com.l7tech.external.assertions.portalbootstrap.console;

import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapExtensionInterface;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Dialog for enrolling the Gateway with a Portal server.
 */
public class EnrollWithPortalDialog extends JDialog {
    private static final Logger logger = Logger.getLogger( EnrollWithPortalDialog.class.getName() );

    private JTextField enrollmentUrlField;
    private JButton enrollButton;
    private JButton cancelButton;
    private JPanel contentPanel;

    public EnrollWithPortalDialog( Window owner ) {
        super( owner, JDialog.DEFAULT_MODALITY_TYPE );
        setContentPane( contentPanel );

        Utilities.attachDefaultContextMenu( enrollmentUrlField );

        cancelButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent e ) {
                dispose();
            }
        } );

        enrollButton.addActionListener( new ActionListener() {
            @Override
            public void actionPerformed( ActionEvent ae ) {
                final String urlText = enrollmentUrlField.getText();

                // TODO better URL checks
                if ( urlText.length() < 1 ) {
                    showError( "Enrollment URL is required." );
                    return;
                }

                URL url = null;
                try {
                    url = new URL( urlText );
                } catch ( Exception e ) {
                    logger.log( Level.FINE, "bad url" , e );
                    showError( "Invalid enrollment URL." );
                    return;
                }

                DialogDisplayer.showConfirmDialog( EnrollWithPortalDialog.this,
                        "Give Portal server " + url.getHost() + " full control of this Gateway?",
                        "Confirm Enrollment",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE,
                        new DialogDisplayer.OptionListener() {
                            @Override
                            public void reportResult( int option ) {
                                if ( JOptionPane.OK_OPTION == option ) {
                                    PortalBootstrapExtensionInterface portalboot =
                                        Registry.getDefault().getExtensionInterface( PortalBootstrapExtensionInterface.class, null );
                                    try {
                                        portalboot.enrollWithPortal( urlText );
                                        DialogDisplayer.showMessageDialog( EnrollWithPortalDialog.this,
                                                "Gateway Enrolled Successfully",
                                                new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        EnrollWithPortalDialog.this.dispose();
                                                    }
                                                }  );
                                    } catch ( Exception e ) {
                                        String msg = "Unable to enroll: " + ExceptionUtils.getMessage( e );
                                        logger.log( Level.WARNING, msg, e );
                                        showError( msg );
                                    }
                                }
                            }
                        } );
            }
        } );
    }

    private void showError( String s ) {
        DialogDisplayer.showMessageDialog( this, "Error", s, null );
    }
}
