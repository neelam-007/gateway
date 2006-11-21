package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.MainWindow;
import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;

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
        if (isFailedLogin(t) && TopComponents.getInstance().isDisconnected()) {
            e.getLogger().log(e.getLevel(), MSG, t);
            ExceptionDialog d = ExceptionDialog.createExceptionDialog(TopComponents.getInstance().getTopParent(),
                                                                      "SecureSpan Manager - Connection error",
                                                                      MSG, t, e.getLevel());
            d.pack();
            Utilities.centerOnScreen(d);
            DialogDisplayer.display(d);
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
}
