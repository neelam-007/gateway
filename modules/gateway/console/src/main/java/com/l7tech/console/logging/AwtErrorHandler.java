package com.l7tech.console.logging;

import javax.swing.*;
import java.util.logging.Level;

/**
 * Class <code>AwtErrorHandler</code> is event dispatch thread
 * exception handler.
 * <p>
 * @see java.awt.EventDispatchThread
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class AwtErrorHandler {
    public void handle(final Throwable t) {
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                ErrorManager.getDefault().notify(Level.SEVERE, t, ErrorManager.DEFAULT_ERROR_MESSAGE);
            }
        });
    }
}
