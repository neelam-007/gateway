package com.l7tech.console.logging;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;

import java.net.SocketException;
import java.rmi.RemoteException;
import java.util.logging.Level;

/**
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
        if (throwable instanceof RemoteException || throwable instanceof SocketException) {
            if (throwable instanceof SocketException) {
                // prevent error cascade during repaint if it's a network problem
                getMainWindow().disconnectFromGateway();
            }
            final Throwable t = e.getThrowable();
            Level level = e.getLevel();
            if (level != Level.SEVERE) { // bump if necessary
                level = Level.SEVERE;
            }
            e.getLogger().log(level, ERROR_MESSAGE, t);
            ExceptionDialog d = ExceptionDialog.createExceptionDialog(getMainWindow(), "SecureSpan Manager - Gateway error", ERROR_MESSAGE, t, level);
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
