package com.l7tech.console.logging;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.ObjectModelException;

import java.util.logging.Level;

/**
 * <code>PersistenceErrorHandler</code> handles the persistence related
 * exceptions form the gateway.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PersistenceErrorHandler implements ErrorHandler {
    private static final String ERROR_MESSAGE = "The Secure Span Gateway reported a persistence error and was unable to complete the operation.";

    /**
     * handle the error event
     *
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final Throwable throwable = ExceptionUtils.unnestToRoot(e.getThrowable());
        if (throwable instanceof ObjectModelException) {
            final Throwable t = e.getThrowable();
            e.getLogger().log(Level.WARNING, ERROR_MESSAGE, t);
            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null, ERROR_MESSAGE, t);
        } else {
            e.handle();
        }
    }
}
