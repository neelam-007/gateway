package com.l7tech.console.logging;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

/**
 * Class ErrorManager is the SSM central error handler.<br>
 * It is designed as a <i>Chain of Responibility</i> pattern for
 * Exceptions handling.<br>
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public class ErrorManager {
    public static final String DEFAULT_ERROR_MESSAGE ="The SecureSpan Manager encountered an internal error or " +
            "misconfiguration and was unable to complete the operation.";

    //- PUBLIC

    /**
     * Singleton entry point
     *
     * @return the eror manager singleton instance
     */
    public static ErrorManager getDefault() {
        return instance;
    }

    /**
     * Pushes an handler onto the top of the handler stack.
     *
     * @param eh the handler that is a new top of the stack
     */
    public void pushHandler(ErrorHandler eh) {
        synchronized (handlerLock) {
            handlers.addFirst(eh);
        }
    }

    /**
     * Removes the handler at the top of the handler stack.
     *
     * @throws    java.util.NoSuchElementException if the stack is empty.
     */
    public void popHandler() {
        synchronized (handlerLock) {
            handlers.removeFirst();
        }
    }

    /**
     * Log and notify the user about the problem or error
     *
     * @param level the log level
     * @param t the throwable with the
     * @param message the message
     */
    public void notify(final Level level, final Throwable t, final String message) {
        notify(level, t, message, null);
    }

    /**
     * Log and notify the user about the problem or error
     *
     * @param level the log level
     * @param t the throwable with the
     * @param message the message or message pattern
     * @param args the pattern arguments, may be null
     */
    public void notify(final Level level, final Throwable t, final String message, final Object[] args) {
        // log
        if ( log.isLoggable( Level.FINE )) {
            log.log( Level.FINE, "Handling error notification with message '"+message+"'.", t );
        }

        // format if required
        String formattedMessage = message;
        if (message !=null && (args !=null && args.length > 0)) {
            formattedMessage = MessageFormat.format(message, args);
        }

        // create handler list
        ErrorHandler[] eh;
        synchronized (handlerLock) {
            ErrorHandler[] defHandlers = Handlers.defaultHandlers();                             
            eh = new ErrorHandler[handlers.size()+defHandlers.length];
            int index = 0;
            for (Iterator iterator = handlers.iterator(); iterator.hasNext();) {
                eh[index++] = (ErrorHandler)iterator.next();
            }
            for (int i = index, j = 0; i < index + defHandlers.length; i++, j++) {
                eh[i] = defHandlers[j];
            }
        }

        // process the error
        processError(new DefaultErrorEvent(eh, level, t, formattedMessage, log));
    }

    //- PROTECTED

    /**
     * Only subclasses can instantiate the class
     */
    protected ErrorManager() {}

    //- PRIVATE

    private static final Logger log = Logger.getLogger(ErrorManager.class.getName());
    private static final ErrorManager instance = new ErrorManager();

    private Object handlerLock = new Object();
    private LinkedList handlers = new LinkedList();

    /**
     * Process the given error event. 
     *
     * @param ee The event to handle
     */
    private void processError(final ErrorEvent ee) {
        // queue and process / block
        ee.handle();
    }
}
