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
                ErrorManager.getDefault().notify(Level.SEVERE, t, ERROR_MESSAGE);
            }
        });
    }
    private static final String ERROR_MESSAGE =
      "<html><b>The Secure Span Manager encountered an " +
      "internal error or misconfiguration and was unable to complete the operation.</b><br></html>";
}
