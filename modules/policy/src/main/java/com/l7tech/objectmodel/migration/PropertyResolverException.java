package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.ObjectModelException;

/**
 * @author jbufu
 */
public class PropertyResolverException extends ObjectModelException {

    public PropertyResolverException() {
    }

    public PropertyResolverException(String message) {
        super(message);
    }

    public PropertyResolverException(String message, Throwable cause) {
        super(message, cause);
    }

    public PropertyResolverException(Throwable cause) {
        super(cause);
    }
}
