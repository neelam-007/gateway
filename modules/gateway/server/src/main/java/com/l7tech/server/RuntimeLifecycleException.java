package com.l7tech.server;

import com.l7tech.util.ExceptionUtils;

/**
 * User: megery
 */
public class RuntimeLifecycleException extends RuntimeException {
    public RuntimeLifecycleException( String message ) {
        super( message );
    }

    public RuntimeLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }

    public RuntimeLifecycleException(Throwable cause) {
        super("Lifecycle error: " + ExceptionUtils.getMessage(cause), cause);
    }
}
