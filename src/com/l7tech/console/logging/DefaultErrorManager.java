package com.l7tech.console.logging;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.ExceptionDialog;
import com.l7tech.console.util.ComponentRegistry;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class ErrorManager is the console error handler. It handles user notifying
 * anbd error logging.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class DefaultErrorManager extends ErrorManager {
    static final Logger log = Logger.getLogger(DefaultErrorManager.class.getName());
    private JFrame mainFrame = null;

    /**
     * Log and notify the user about the problem or error
     *
     * @param level the log level
     * @param t the throwable with the
     * @param message the message
     */
    public void notify(Level level, Throwable t, String message) {
        log.log(level, message, t);
        ExceptionDialog d = new ExceptionDialog(getMainWindow(), "Securespan Manager - message", message, t, level);
        d.pack();
        Utilities.centerOnScreen(d);
        d.show();
    }

    private JFrame getMainWindow() {
        if (mainFrame != null) return mainFrame;

        ComponentRegistry instance = ComponentRegistry.getInstance();
        if (instance.hasMainWindow()) {
            mainFrame = instance.getMainWindow();
        }

        return mainFrame;
    }
}
