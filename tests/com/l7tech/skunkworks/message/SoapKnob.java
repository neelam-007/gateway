/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.skunkworks.message;

import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.util.Iterator;
import java.io.IOException;

/**
 * Aspect of Message that represents a SOAP envelope.
 */
public interface SoapKnob extends Knob {
    /*
    Element getPayload();
    Iterator getHeaders();
    void addHeader(Element headerElement);
    */

    String getPayloadNamespaceUri() throws IOException, SAXException;
}
