/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import org.xml.sax.InputSource;

import javax.xml.parsers.*;
import java.io.*;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class XmlMessageAdapter extends MessageAdapter implements XmlMessage {
    public XmlMessageAdapter( TransportMetadata tm ) {
        super(tm);
    }

    synchronized void parse( InputStream is ) throws SAXException, IOException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder parser = dbf.newDocumentBuilder();
            _document = parser.parse( new InputSource( is ) );
        } catch ( ParserConfigurationException pce ) {
            throw new SAXException( pce );
        }
    }


    protected Document _document;
}
