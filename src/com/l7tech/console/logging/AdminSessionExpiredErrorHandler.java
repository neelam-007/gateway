package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.MainWindow;
import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;

import javax.security.auth.login.FailedLoginException;

/**
 * Special handler for errors occuring when the ssm is disconnected during the
 * execution of an operation requiring connectivity.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Sep 13, 2004<br/>
 * $Id$<br/>
 */
public class AdminSessionExpiredErrorHandler implements ErrorHandler {
    public static final String MSG = "The SecureSpan Manager is no longer logged in and is unable to " +
                                     "complete the operation.";
    public void handle(ErrorEvent e) {
        final Throwable t = e.getThrowable();
        if (isFailedLogin(t) && getMainWindow().isDisconnected()) {
            e.getLogger().log(e.getLevel(), MSG, t);
            ExceptionDialog d = new ExceptionDialog(getMainWindow(),
                                                    "SecureSpan Manager - Connection error",
                                                    MSG, t, e.getLevel());
            d.pack();
            Utilities.centerOnScreen(d);
            d.show();
        } else {
            e.handle();
        }
    }

    private boolean isFailedLogin(Throwable e) {
        if (e == null) return false;
        if (e instanceof FailedLoginException) {
            return true;
        } else {
            return isFailedLogin(e.getCause());
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

    private MainWindow mainFrame;
}
