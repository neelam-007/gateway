/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.server.MessageProcessor;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class XmlMessageAdapter extends MessageAdapter implements XmlMessage {
    public XmlMessageAdapter( TransportMetadata tm ) {
        super(tm);
    }

    synchronized void parse( String xml ) throws SAXException, IOException {
        try {
            // TODO: Ensure this is a lazy parser
            DocumentBuilder parser = MessageProcessor.getInstance().getDomParser();
            _document = parser.parse( new InputSource( new StringReader( xml ) ) );
        } catch ( ParserConfigurationException pce ) {
            throw new SAXException( pce );
        }
    }

    public synchronized XmlPullParser pullParser( String xml ) throws XmlPullParserException {
        XmlPullParser xpp = MessageProcessor.getInstance().getPullParser();
        xpp.setInput( new StringReader( xml ) );
        return xpp;
    }

    protected String serializeDoc(Document doc) throws IOException {
        final StringWriter sw = new StringWriter(512);
        XMLSerializer xmlSerializer = new XMLSerializer();
        xmlSerializer.setOutputCharStream(sw);
        OutputFormat of = new OutputFormat();
        of.setIndent(4);
        xmlSerializer.setOutputFormat(of);
        xmlSerializer.serialize(doc);
        return sw.toString();
    }

    protected Document _document;


}
