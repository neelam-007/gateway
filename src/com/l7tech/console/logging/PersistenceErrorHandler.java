package com.l7tech.console.logging;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.ObjectModelException;

import javax.swing.*;
import java.util.logging.Level;
import java.awt.*;

/**
 * <code>PersistenceErrorHandler</code> handles the persistence related
 * exceptions form the gateway.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PersistenceErrorHandler implements ErrorHandler {
    private static final String ERROR_MESSAGE =
      "<html><b>The Secure Span Gateway reported a " +
      "persistence error and was unable to complete the operation.</b><br></html>";

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
            ExceptionDialog d = ExceptionDialog.createExceptionDialog(getMainWindow(), "SecureSpan Manager - Warning", ERROR_MESSAGE, t, Level.WARNING);
            d.pack();
            Utilities.centerOnScreen(d);
            DialogDisplayer.display(d);
        } else {
            e.handle();
        }
    }

    private Frame getMainWindow() {
        return TopComponents.getInstance().getTopParent();
    }
}
