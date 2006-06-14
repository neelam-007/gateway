package com.l7tech.console.logging;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;

import java.net.SocketException;
import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;
import java.rmi.UnknownHostException;
import java.rmi.ConnectIOException;
import java.rmi.ConnectException;
import java.util.logging.Level;
import java.security.AccessControlException;

import org.springframework.remoting.RemoteAccessException;

/**
 * This is now more of a "remoting" error handler, not just RMI.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RmiErrorHandler implements ErrorHandler {
    private MainWindow mainFrame;
     private static final String ERROR_MESSAGE =
      "<html><b>The SecureSpan Manager encountered an " +
      "gateway error or communication error and was unable to complete the operation.</b><br></html>";

    /**
     * handle the error event
     * 
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final Throwable throwable = ExceptionUtils.unnestToRoot(e.getThrowable());
        final RemoteException rex = (RemoteException) ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteException.class);
        final RemoteAccessException raex = (RemoteAccessException) ExceptionUtils.getCauseIfCausedBy(e.getThrowable(), RemoteAccessException.class);

        if (throwable instanceof SocketException ||
            rex instanceof ConnectException ||
            rex instanceof ConnectIOException ||
            rex instanceof NoSuchObjectException ||
            rex instanceof UnknownHostException ||
            throwable instanceof AccessControlException) {
            // prevent error cascade during repaint if it's a network problem
            e.getLogger().log(Level.WARNING, "Disconnected from gateway, notifiying workspace.");
            getMainWindow().disconnectFromGateway();
        }

        if (throwable instanceof RemoteException ||
            throwable instanceof SocketException ||
            throwable instanceof AccessControlException) {

            Throwable t = e.getThrowable();
            String message = ERROR_MESSAGE;
            e.getLogger().log(Level.SEVERE, message, t);
            Level level = Level.SEVERE;
            if (rex instanceof NoSuchObjectException ||
                throwable instanceof AccessControlException) {
                message = "SecureSpan Gateway restarted, please log in again.";
                level = Level.WARNING;
                t = null;
            }
            else if ((rex instanceof ConnectException) ||
                     (raex != null && throwable instanceof java.net.SocketException)) {
                message = "SecureSpan Gateway unavailable (Network issue or server stopped).";
                level = Level.WARNING;
                t = null;
            }
            ExceptionDialog d = ExceptionDialog.createExceptionDialog(getMainWindow(), "SecureSpan Manager - Gateway error", message, t, level);
            d.pack();
            Utilities.centerOnScreen(d);
            d.setVisible(true);
        } else {
            e.handle();
        }
    }

    private MainWindow getMainWindow() {
        if (mainFrame != null) return mainFrame;

        TopComponents instance = TopComponents.getInstance();
        if (instance.hasMainWindow()) {
            mainFrame = instance.getMainWindow();
        }

        return mainFrame;
    }
}
