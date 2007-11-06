package com.l7tech.console.logging;

import com.l7tech.console.util.TopComponents;
import com.l7tech.common.gui.util.DialogDisplayer;

import java.util.logging.Level;

/**
 * <code>DefaultErrorHandler</code> is the generic error message handler
 * that displays the dialog and logs the message.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefaultErrorHandler implements ErrorHandler {
    /**
     * handle the error event
     *
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final String message = e.getMessage();
        final Throwable throwable = e.getThrowable();
        final Level level = e.getLevel();
        e.getLogger().log(level, message, throwable);
        Throwable t = (level.intValue() >= Level.WARNING.intValue()) ? throwable : null;
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null, message, t);
    }
}
