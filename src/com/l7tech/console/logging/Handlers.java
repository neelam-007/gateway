package com.l7tech.console.logging;

/**
 * The error handlers utility class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class Handlers {

    /**
     * this class canoot be instantiated
     */
    private Handlers() {}

    /**
     * Return the list of the default handlers. The handlers are executed
     * in that order.s
     *
     * @return the list of default handlers
     */
    static ErrorHandler[] defaultHandlers() {
        return new ErrorHandler[]{
            new PersistenceErrorHandler(),
            new RmiErrorHandler(),
            new DefaultErrorHandler()
        };
    }
}
