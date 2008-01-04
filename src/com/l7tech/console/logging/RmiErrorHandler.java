package com.l7tech.console.logging;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.admin.TimeoutRuntimeException;
import org.springframework.remoting.RemoteAccessException;

import java.net.SocketException;
import java.rmi.*;
import java.security.AccessControlException;
import java.util.logging.Level;
import java.awt.Frame;

/**
 * This is now more of a "remoting" error handler, not just RMI.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RmiErrorHandler implements ErrorHandler {
     private static final String ERROR_MESSAGE = "A SecureSpan Gateway error or a communication error occurred.";

    /**
     * handle the error event
     *
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final Throwable throwable = ExceptionUtils.unnestToRoot(e.getThrowable());
        final RemoteException rex = (RemoteException) ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteException.class);
        final RemoteAccessException raex = (RemoteAccessException) ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteAccessException.class);

        final Frame topParent = TopComponents.getInstance().getTopParent();
        if (throwable instanceof SocketException ||
            throwable instanceof TimeoutRuntimeException ||
            rex instanceof ConnectException ||
            rex instanceof ConnectIOException ||
            rex instanceof NoSuchObjectException ||
            rex instanceof UnknownHostException ||
            throwable instanceof AccessControlException ||
            (throwable instanceof NoClassDefFoundError && TopComponents.getInstance().isApplet())) {
            // prevent error cascade during repaint if it's a network problem
            e.getLogger().log(Level.WARNING, "Disconnected from gateway, notifiying workspace.");
            TopComponents.getInstance().setConnectionLost(true);
            TopComponents.getInstance().disconnectFromGateway();
        }

        if (topParent != null &&
            (throwable instanceof RemoteException ||
             throwable instanceof SocketException ||
             throwable instanceof AccessControlException ||
             throwable instanceof TimeoutRuntimeException ||
             (throwable instanceof NoClassDefFoundError && TopComponents.getInstance().isApplet()))) {

            Throwable t = e.getThrowable();
            String message = ERROR_MESSAGE;
            e.getLogger().log(Level.SEVERE, message, t);
            if (rex instanceof NoSuchObjectException ||
                throwable instanceof AccessControlException) {
                message = "The SecureSpan Gateway restarted, please login again.";
                t = null;
            }
            else if ((rex instanceof ConnectException) ||
                     (raex != null && throwable instanceof java.net.SocketException) ||
                    (throwable instanceof NoClassDefFoundError && TopComponents.getInstance().isApplet()) ||
                    (throwable instanceof TimeoutRuntimeException)) {
                message = "SecureSpan Gateway unavailable (Network issue or server stopped).";
                t = null;
            }
            topParent.repaint();
            // if t = null, show message dialog, otherwise, show error dialog.
            DialogDisplayer.showMessageDialog(topParent, null, message, t);
        } else {
            e.handle();
        }
    }
}
