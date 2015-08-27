package com.l7tech.external.assertions.portalbootstrap.console;

import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.portalbootstrap.PortalBootstrapExtensionInterface;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gui.util.ClipboardActions;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
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
        super( owner, "Enroll with SaaS Portal", JDialog.DEFAULT_MODALITY_TYPE );
        setContentPane( contentPanel );

        Utilities.attachDefaultContextMenu( enrollmentUrlField );
        Utilities.setEscKeyStrokeDisposes( this );
        getRootPane().setDefaultButton( enrollButton );

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

                if ( urlText.length() < 1 ) {
                    showError( "Enrollment URL is required." );
                    return;
                } else if ( !looksLikeEnrollmentUrl( urlText ) ) {
                    showError( "Enrollment URL is not valid." );
                    return;
                }

                URL url = null;
                try {
                    url = new URL( urlText );
                } catch ( Exception e ) {
                    logger.log( Level.FINE, "bad url", e );
                    showError( "Invalid enrollment URL." );
                    return;
                }

                DialogDisplayer.showConfirmDialog( EnrollWithPortalDialog.this,
                        new JLabel("<html>Portal enrollment cannot be reversed. Please do not cancel or exit this process.<br/>Allow API Portal to install portal-specific software on this Gateway?</html>"),
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
                                        AsyncAdminMethods.JobId<Boolean> enrollJobId = portalboot.enrollWithPortal(urlText);
                                        Either<String, Boolean> result =
                                                AdminGuiUtils.doAsyncAdmin(portalboot, EnrollWithPortalDialog.this, "Enroll with SaaS Portal", "Enrolling...", enrollJobId, false);

                                        if (result.isLeft()) {
                                            String msg = "Unable to enroll: " + result.left();
                                            logger.log( Level.WARNING, msg );
                                            showError( msg );
                                        } else {
                                            DialogDisplayer.showMessageDialog( EnrollWithPortalDialog.this,
                                                    "Gateway Enrolled Successfully",
                                                    "Gateway Enrolled Successfully",
                                                    JOptionPane.INFORMATION_MESSAGE,
                                                    new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            EnrollWithPortalDialog.this.dispose();
                                                        }
                                                    } );
                                            TopComponents.getInstance().refreshPoliciesFolderNode();
                                        }
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

    @Override
    public void setVisible( boolean b ) {
        if ( b && !isVisible() ) {
            prefillUrlFromClipboard();
        }
        super.setVisible( b );
    }

    private void prefillUrlFromClipboard() {
        try {
            Clipboard clip = ClipboardActions.getClipboard();
            if ( null == clip )
                return;
            Transferable transferable = clip.getContents( null );
            if ( null == transferable )
                return;
            if ( !transferable.isDataFlavorSupported( DataFlavor.stringFlavor ) )
                return;
            String contents = (String) transferable.getTransferData( DataFlavor.stringFlavor );
            if ( !looksLikeEnrollmentUrl( contents ) )
                return;
            enrollmentUrlField.setText( contents );
            enrollmentUrlField.setCaretPosition( 0 );

        } catch ( Exception e ) {
            logger.log( Level.INFO, "Unable to read clipboard: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
        }

    }

    private boolean looksLikeEnrollmentUrl( String contents ) {
        return contents.matches( "^https://.*?\\?.*?sckh=.*$" );
    }

    private void showError( String s ) {
        DialogDisplayer.showMessageDialog( this, "Error", s, null );
    }
}
