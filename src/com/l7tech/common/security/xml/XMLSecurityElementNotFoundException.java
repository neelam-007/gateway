package com.l7tech.common.security.xml;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Aug 28, 2003
 * Time: 3:21:13 PM
 * $Id$
 *
 * Indicates that an expected XML Element was not present.
 */
public class XMLSecurityElementNotFoundException extends Exception {
    public XMLSecurityElementNotFoundException() {
    }

    public XMLSecurityElementNotFoundException(String message) {
        super(message);
    }

    public XMLSecurityElementNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public XMLSecurityElementNotFoundException(Throwable cause) {
        super(cause);
    }
}
