package com.l7tech.console.logging;

import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.MainWindow;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.VersionException;

import javax.swing.*;
import java.util.logging.Level;

/**
 * <code>VersionMismatchErrorHandler</code> is a generic handler that handles
 * version mismatch related exceptions coming from the gateway.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class VersionMismatchErrorHandler implements ErrorHandler {
    private MainWindow mainFrame;
    private static final String ERROR_MESSAGE =
      "<html><b>The record has changed in the meantime by another user." +
      "The Gateway was unable to complete the operation.</b><br></html>";

    /**
     * handle the error event
     * 
     * @param e the error event
     */
    public void handle(ErrorEvent e) {
        final Throwable throwable = ExceptionUtils.unnestToRoot(e.getThrowable());
        if (throwable instanceof VersionException) {
            final Throwable t = e.getThrowable();
            Level level = e.getLevel();
            e.getLogger().log(level, ERROR_MESSAGE, t);
            ExceptionDialog d = new ExceptionDialog(getMainWindow(), "Securespan Manager - Gateway Warning", ERROR_MESSAGE, t, Level.WARNING);
            d.pack();
            Utilities.centerOnScreen(d);
            d.show();
        } else {
            e.handle();
        }
    }

    private JFrame getMainWindow() {
        if (mainFrame != null) return mainFrame;

        TopComponents instance = TopComponents.getInstance();
        if (instance.hasMainWindow()) {
            mainFrame = instance.getMainWindow();
        }

        return mainFrame;
    }
}
