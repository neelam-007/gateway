package com.l7tech.console.logging;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.util.TopComponents;

import javax.swing.*;
import java.util.logging.Level;

/**
 * <code>DefaultErrorHandler</code> is the generic error message handler
 * that displays the dialog and logs the message.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefaultErrorHandler implements ErrorHandler {
    private JFrame mainFrame = null;

    /**
     * handle the error event
     *
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final String message = e.getMessage();
        final Throwable t = e.getThrowable();
        final Level level = e.getLevel();
        e.getLogger().log(level, message, t);
        ExceptionDialog d = ExceptionDialog.createExceptionDialog(TopComponents.getInstance().getTopParent(), "SecureSpan Manager - Message", message, t, level);
        d.pack();
        Utilities.centerOnScreen(d);
        DialogDisplayer.display(d);
    }
}
