package com.l7tech.console.logging;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.VersionException;

import java.util.logging.Level;

/**
 * <code>VersionMismatchErrorHandler</code> is a generic handler that handles
 * version mismatch related exceptions coming from the gateway.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class VersionMismatchErrorHandler implements ErrorHandler {
    private static final String ERROR_MESSAGE = "The record has been changed in the meantime by another user.  " +
            "The SecureSpan Gateway was unable to complete the operation.";

    /**
     * handle the error event
     * 
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final Throwable throwable = ExceptionUtils.unnestToRoot(e.getThrowable());
        if (throwable instanceof VersionException) {
            final Throwable t = e.getThrowable();
            e.getLogger().log(Level.WARNING, ERROR_MESSAGE, t);
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null, ERROR_MESSAGE, null);
        } else {
            e.handle();
        }
    }
}
