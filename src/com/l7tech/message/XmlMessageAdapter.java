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
        _dbf.setNamespaceAware(true);
    }

    synchronized void parse( String xml ) throws SAXException, IOException {
        try {
            // TODO: Ensure this is a lazy parser
            DocumentBuilder parser = _dbf.newDocumentBuilder();
            _document = parser.parse( new InputSource( new StringReader( xml ) ) );
        } catch ( ParserConfigurationException pce ) {
            throw new SAXException( pce );
        }
    }

    protected Document _document;
    protected DocumentBuilderFactory _dbf = DocumentBuilderFactory.newInstance();
}
