/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.util;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
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
    /**
     * Returns the {@link Document} from a {@link Message}, or null if the message is not XML.
     * @param soapmsg
     * @return the {@link Document} from the specified {@link Message}, or null if the message is not XML.
     * @throws SAXException
     * @throws IOException
     */
    public static Document getDocument(Message soapmsg) throws SAXException, IOException {
        if ( soapmsg instanceof XmlMessage ) {
            XmlMessage xmsg = (XmlMessage)soapmsg;
            return xmsg.getDocument();
        }
        return null;
    }

    /**
     * Returns the SOAP:Envelope (document) element from a {@link Request}, or null if the message is not XML.
     * @param request
     * @return the SOAP:Envelope (document) element for the specified {@link Request}, or null if the message is not XML.
     * @throws SAXException
     * @throws IOException
     */
    public static Element getEnvelope( Request request ) throws SAXException, IOException {
        Document doc = getDocument( request );
        if ( doc == null )
            return null;
        else
            return doc.getDocumentElement();
    }

    /**
     * Returns the SOAP:Body {@link Element} for the specified {@link Request}, or null if the message is not SOAP or not XML.
     * @param request
     * @return the SOAP:Body {@link Element} for the specified {@link Request}, or null if the message is not SOAP or not XML.
     * @throws SAXException
     * @throws IOException
     */
    public static Element getBodyElement( Request request ) throws SAXException, IOException, InvalidDocumentFormatException {
        Document doc = getDocument(request);
        return doc == null ? null : getBodyElement(doc);
    }



}
