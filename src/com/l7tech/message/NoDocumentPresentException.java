/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import org.xml.sax.SAXException;

/**
 * @author alex
 * @version $Revision$
 */
public class NoDocumentPresentException extends SAXException {
    public NoDocumentPresentException() {
        super("No document that can be parsed is present");
    }
}
