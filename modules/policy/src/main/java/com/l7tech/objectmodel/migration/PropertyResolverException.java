package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.ObjectModelException;

/**
 * Exception thrown if an dependency analysis failed for a property value.
 *
 * @author jbufu
 */
public class PropertyResolverException extends ObjectModelException {

    public PropertyResolverException() {
        super();
    }

    public PropertyResolverException( String message ) {
        super( message );
    }

    public PropertyResolverException( String message, Throwable cause ) {
        super( message, cause );
    }

    public PropertyResolverException(Throwable cause) {
        super(cause);
    }
}
