/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

import com.l7tech.util.SoapUtil;
import com.l7tech.message.Message;
import com.l7tech.message.XmlMessage;
import com.l7tech.message.Request;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerSoapUtil extends SoapUtil {
    public static Document getDocument(Message soapmsg) throws SAXException, IOException {
        if ( soapmsg instanceof XmlMessage ) {
            XmlMessage xmsg = (XmlMessage)soapmsg;
            return xmsg.getDocument();
        } else {
            throw new IllegalArgumentException( "Can't find a URN in a non-XML request!" );
        }
    }

    public static Element getEnvelope( Request request ) throws SAXException, IOException {
        Document doc = getDocument( request );
        Element env = doc.getDocumentElement();
        return env;
    }

    static Element getEnvelopePart( Request request, String elementName ) throws SAXException, IOException {
        return getEnvelopePart( getDocument( request ), elementName );
    }

    public static Element getHeaderElement( Request request ) throws SAXException, IOException {
        return getEnvelopePart( request, HEADER_EL_NAME );
    }

    public static Element getBodyElement( Request request ) throws SAXException, IOException {
        return getEnvelopePart( request, BODY_EL_NAME );
    }



}
