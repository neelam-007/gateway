/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.wsdl;

import com.l7tech.util.InvalidDocumentFormatException;

/**
 * @author alex
 * @version $Revision$
 */
public class WsdlMissingSoapPortException extends InvalidDocumentFormatException {
    public WsdlMissingSoapPortException(String wsdlFilename) {
        super("WSDL " + wsdlFilename + " has no SOAP port.");
    }
}
