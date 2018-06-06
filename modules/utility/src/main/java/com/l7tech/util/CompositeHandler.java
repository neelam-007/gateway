package com.l7tech.util;

import java.util.List;
import java.util.logging.Handler;

/**
 *  Abstract class for a handler that may use many handlers.
 */
public abstract class CompositeHandler extends Handler {

    /**
     * Gets list of handlers used by its list of log sinks
     * @return Immutable list of handlers used.
     */
    public abstract List<Handler> getHandlers();
}
