package com.l7tech.console.logging;

import com.l7tech.common.util.Locator;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.LinkedList;
import java.util.Iterator;

/**
 * Class ErrorManager is the SSM central error handler.<br>
 * It is designed as a <i>Chain of Responibility</i> pattern for
 * Exceptions handling.<br>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class ErrorManager {
    protected static final Logger log = Logger.getLogger(ErrorManager.class.getName());
    protected static ErrorManager instance = new ErrorManager();

    protected LinkedList handlers = new LinkedList();
    /**
     * inly subclasses can instantiate the class
     */
    protected ErrorManager() {}

    /**
     * Singleton entry point
     * @return the eror manager singleton instance
     */
    public static ErrorManager getDefault() {
        return instance;
    }

    /**
     * Pushes an handlere onto the top of the handler
     * stack.
     *
     * @param eh the handler that is a new top of the stack
     */
    public void pushHandler(ErrorHandler eh) {
        handlers.addFirst(eh);
    }

    /**
     * Removes the handler at the top of the handler stack.
     * @throws    java.util.NoSuchElementException if the stack is empty.
     */
    public void popHandler() {
        handlers.removeFirst();
    }

    /**
     * Log and notify the user about the problem or error
     *
     * @param level the log level
     * @param t the throwable with the
     * @param message the message
     */
    public void notify(Level level, Throwable t, String message) {
        ErrorHandler[] defHandlers = Handlers.defaultHandlers();
        ErrorHandler[] eh = new ErrorHandler[handlers.size()+defHandlers.length];
        int index = 0;
        for (Iterator iterator = handlers.iterator(); iterator.hasNext();) {
            eh[index++] = (ErrorHandler)iterator.next();
        }
        for (int i = index, j = 0; i < index + defHandlers.length; i++, j++) {
            eh[i] = defHandlers[j];
        }
        ErrorEvent ee = new DefaultErrorEvent(eh, level, t, message, log);
        ee.handle();
    }
}
