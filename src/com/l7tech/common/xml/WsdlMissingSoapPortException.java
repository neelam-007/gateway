/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

/**
 * @author alex
 * @version $Revision$
 */
public class WsdlMissingSoapPortException extends InvalidDocumentFormatException {
    public WsdlMissingSoapPortException(String wsdlFilename) {
        super("WSDL " + wsdlFilename + " has no SOAP port.");
    }
}
