/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml;

/**
 * Exception thrown if too many copies of an element are noticed in a document.
 */
public class TooManyChildElementsException extends InvalidDocumentFormatException {
    public TooManyChildElementsException( String nsuri, String name ) {
        super( "Too many \"" + name + "\" child elements" );
        this.nsuri = nsuri;
        this.name = name;
    }

    public String getNsUri() {
        return nsuri;
    }

    public String getName() {
        return name;
    }

    private String nsuri;
    private String name;
}
