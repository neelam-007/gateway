package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.TimeoutRuntimeException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.identity.ValidationException;
import com.l7tech.util.ExceptionUtils;
import org.springframework.remoting.RemoteAccessException;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import javax.swing.*;
import java.awt.*;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.rmi.*;
import java.security.AccessControlException;
import java.util.logging.Level;

/**
 * This is now more of a "remoting" error handler, not just RMI.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RmiErrorHandler implements ErrorHandler {
     private static final String ERROR_MESSAGE = "A Gateway error or a communication error occurred.";

    /**
     * handle the error event
     *
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final Throwable throwable = ExceptionUtils.unnestToRoot(e.getThrowable());
        final RemoteException rex = ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteException.class);
        final RemoteAccessException raex = ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteAccessException.class);

        final Frame topParent = TopComponents.getInstance().getTopParent();
        if (throwable instanceof SocketException ||
            throwable instanceof SocketTimeoutException ||
            throwable instanceof TimeoutRuntimeException ||
            throwable instanceof SSLException ||
            rex instanceof ConnectException ||
            rex instanceof ConnectIOException ||
            rex instanceof NoSuchObjectException ||
            rex instanceof UnknownHostException ||
            throwable instanceof AccessControlException ||
            (throwable instanceof NoClassDefFoundError && TopComponents.getInstance().isApplet())) {
            // prevent error cascade during repaint if it's a network problem
            e.getLogger().log(Level.WARNING, "Disconnected from gateway, notifying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();
        }

        if (topParent != null && throwable instanceof SSLException) {
            Throwable t = e.getThrowable();
            String message = "A Gateway keystore or SSL/TLS communication error occurred.";
            e.getLogger().log(Level.SEVERE, message, t);
            if (throwable instanceof SSLHandshakeException) {
                message = "The SSL/TLS handshake with the Gateway has failed: " + ExceptionUtils.getMessage(throwable);
                t = null;
            }
            refreshUI( topParent );
            DialogDisplayer.showMessageDialog(topParent, null, message, t);
        } else if (topParent != null &&
            (throwable instanceof RemoteException ||
             throwable instanceof SocketException ||
             throwable instanceof SocketTimeoutException ||
             throwable instanceof AccessControlException ||
             throwable instanceof TimeoutRuntimeException ||
             (throwable instanceof NoClassDefFoundError && TopComponents.getInstance().isApplet()))) {

            Throwable t = e.getThrowable();
            String message = ERROR_MESSAGE;
            e.getLogger().log(Level.SEVERE, message, ExceptionUtils.getDebugException(t));
            if (rex instanceof NoSuchObjectException ||
                throwable instanceof AccessControlException) {
                if (throwable instanceof AccessControlException && throwable.getMessage().startsWith("Admin request not permitted on this port")) {
                    message = throwable.getMessage();
                } else {
                    message = "Gateway Inactivity Session Timeout has been reached.";
                }
                t = null;
            }
            else if ((rex instanceof ConnectException) ||
                     (raex != null && (throwable instanceof SocketException || throwable instanceof SocketTimeoutException)) ||
                    (throwable instanceof NoClassDefFoundError && TopComponents.getInstance().isApplet()) ||
                    (throwable instanceof TimeoutRuntimeException)) {
                message = "Connection to the Gateway has been broken.";
                t = null;
            }

            // ensure redraw after any errors
            refreshUI( topParent );

            // if t = null, show message dialog, otherwise, show error dialog.
            DialogDisplayer.showMessageDialog(topParent, null, message, t);
        } else if (throwable instanceof ValidationException){
            e.getLogger().log(Level.WARNING, "Invalid User.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();
            String message = "Invalid User.  Connection to the Gateway has been broken.";
            refreshUI( topParent );
            DialogDisplayer.showMessageDialog(topParent, null, message, throwable);
        } else {
            e.handle();
        }
    }

    private void refreshUI( final Frame topParent ) {
        if ( topParent instanceof RootPaneContainer ) {
            ((RootPaneContainer) topParent).getContentPane().validate();
        } else {
            topParent.validate();
        }
    }
}