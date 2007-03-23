package com.l7tech.console.logging;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.util.TopComponents;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.util.logging.Level;

/**
 * Watches for error that may indicate that database failover is in progress.
 * Since this is difficult to detect, this code currently just scans for likely keywords.
 * Configure this as a last-resort error handler.
 */
public class DatabaseConnectionErrorHandler implements ErrorHandler {
    private static final String ERROR_MESSAGE = "<HTML><b>The SecureSpan Gateway is having trouble communicating with its database.</b>" +
            "<p>Please try again in a minute or two.";

    public void handle(ErrorEvent e) {
        if (!handled(e))
            e.handle();
    }

    private boolean handled(ErrorEvent e) {
        Throwable t = e.getThrowable();
        if (t == null) return false;

        // See if this stack trace contains keywords that indicate a db connection problem (Bug #3270)
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        t.printStackTrace(ps);
        ps.close();
        String stack = baos.toString();

        if (stack.contains("at com.mysql.jdbc.MysqlIO") && stack.contains("at org.hibernate.jdbc")) {
            e.getLogger().log(Level.WARNING, ERROR_MESSAGE, t);
            ExceptionDialog d = ExceptionDialog.createExceptionDialog(TopComponents.getInstance().getTopParent(),
                                                                      "SecureSpan Manager - Warning",
                                                                      ERROR_MESSAGE,
                                                                      new SsgDatabaseConnectionException(t),
                                                                      Level.WARNING);
            d.pack();
            Utilities.centerOnScreen(d);
            DialogDisplayer.display(d);
            return true;
        }

        return false;
    }

    /** Wrapper to hide the ugly low-level exception message in the error dialog. */
    private static class SsgDatabaseConnectionException extends IOException
            implements ExceptionDialog.ExceptionDialogDisplayMessageSource
    {
        public SsgDatabaseConnectionException(Throwable cause) {
            super();
            initCause(cause);
        }

        public String getDisplayMessage() {
            return "Timed out reading from database server";
        }
    }
}
