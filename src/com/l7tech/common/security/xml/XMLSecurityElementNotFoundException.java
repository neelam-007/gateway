package com.l7tech.common.security.xml;

/**
 * Indicates that an expected XML Element was not present.
 * 
 * <br/><br/>
 * Layer 7 technologies, inc.<br/>
 * User: flascell<br/>
 * Date: Aug 28, 2003<br/>
 * $Id$
 *
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
