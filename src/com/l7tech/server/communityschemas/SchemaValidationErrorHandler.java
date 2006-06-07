/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXParseException;
import org.xml.sax.SAXException;

import java.util.Collection;
import java.util.ArrayList;

/**
 * Error handler to keep track of any errors
 */
public class SchemaValidationErrorHandler implements ErrorHandler {
    public void warning(SAXParseException exception) throws SAXException {
        // ignore warnings
    }

    public void error(SAXParseException exception) throws SAXException {
        errors.add(exception);
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        errors.add(exception);
    }

    /**
     * prepare this object for another parse operation
     * (forget about previous errors)
     */
    public void reset() {
        errors.clear();
    }

    /**
     * get the errors recorded during parse operation
     *
     * @return a collection of SAXParseException objects
     */
    public Collection<SAXParseException> recordedErrors() {
        return errors;
    }

    private final ArrayList<SAXParseException> errors = new ArrayList<SAXParseException>();
}
