package com.l7tech.service;

import com.l7tech.objectmodel.ObjectModelException;

/**
 * This exception is thrown when an attempt is made to a publish service whose resolution parameter would be
 * too long to fit in the resolution parameter table.
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Sep 27, 2004<br/>
 * $Id$
 */
public class ResolutionParameterTooLongException extends ObjectModelException {
    public ResolutionParameterTooLongException() {
        super();
    }

    public ResolutionParameterTooLongException(String message) {
        super(message);
    }

    public ResolutionParameterTooLongException(String message, Throwable cause) {
        super(message, cause);
    }
}
