/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.transport.jms.BytesMessageInputStream;
import com.l7tech.server.transport.jms.JmsUtil;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.StringTokenizer;

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

    /**
     * Gets the XML part of the message from the provided reader.
     * <p>
     * Works with both multipart/related (SOAP with Attachments) as long as the first part is text/xml,
     * and of course without attachments.
     * <p>
     * @param is the underlying input stream for the message
     * @return the XML as a String
     * @throws IOException if a multipart message has an invalid format, or the content cannot be read
     */
    protected String getMessageXml(InputStream is) throws IOException {
        String ctype = (String)getParameter(Message.PARAM_HTTP_CONTENT_TYPE);
        HeaderValue contentTypeHeader = parseHeader(XmlUtil.CONTENT_TYPE + ": " + ctype);

        if (XmlUtil.MULTIPART_CONTENT_TYPE.equals(contentTypeHeader.value)) {

            multipart = true;

            if(!soapPartParsed) {
                String multipartBoundary = (String)contentTypeHeader.params.get(XmlUtil.MULTIPART_BOUNDARY);
                if (multipartBoundary == null) throw new IOException("Multipart header '" + contentTypeHeader.getName() + "' did not contain a boundary");

                String innerType = (String)contentTypeHeader.params.get(XmlUtil.MULTIPART_TYPE);
                if (innerType.startsWith(XmlUtil.TEXT_XML)) {
                    multipartReader = new MultipartMessageReader(is, multipartBoundary);

                    Message.Part part = multipartReader.getSoapPart();
                    if (!part.getHeader(XmlUtil.CONTENT_TYPE).value.equals(innerType)) throw new IOException("Content-Type of first part doesn't match type of Multipart header");

                    soapPartParsed = true;
                    return part.content;
                } else throw new IOException("Expected first part of multipart message to be XML (was '" + innerType + "')");
            } else {
                if(multipartReader != null) {
                    return multipartReader.getSoapPart().content;
                } else {
                    // should never happen
                    throw new IllegalStateException("The soap part was parsed once but the multipartReader is NULL.");
                }
            }

        } else {
            // Not multipart, read the whole thing
            StringBuffer xml = new StringBuffer();

            BufferedReader reader;
            if(is instanceof BytesMessageInputStream) {
                reader = new BufferedReader(new InputStreamReader(is, JmsUtil.DEFAULT_ENCODING));
            } else {
                reader = new BufferedReader(new InputStreamReader(is));
            }

            char[] buf = new char[1024];
            int read = reader.read(buf);
            while (read > 0) {
                xml.append(buf, 0, read);
                read = reader.read(buf);
            }
            return xml.toString();
        }
    }

    private Message.HeaderValue parseHeader(String header) throws IOException {
        StringTokenizer stok = new StringTokenizer(header, ":; ", false);
        Message.HeaderValue result = new Message.HeaderValue();
        while (stok.hasMoreTokens()) {
            String tok = stok.nextToken();
            int epos = tok.indexOf("=");
            if (epos == -1) {
                if (result.name == null)
                    result.name = tok;
                else if (result.value == null)
                    result.value = unquote(tok);
                else
                    throw new IOException("Encountered unexpected bare word '" + tok + "' in header");
            } else if (epos > 0) {
                String name = tok.substring(0,epos);
                String value = tok.substring(epos+1);
                value = unquote( value );

                result.params.put(name,value);
            } else throw new IOException("Invalid Content-Type header format ('=' at position " + epos + ")");
        }
        return result;
    }

    private String unquote( String value ) throws IOException {
        if (value.startsWith("\"")) {
            if (value.endsWith("\"")) {
                value = value.substring(1,value.length()-1);
            } else throw new IOException("Invalid header format (mismatched quotes in value)");
        }
        return value;
    }

    protected Document _document;
    protected boolean soapPartParsed = false;
    protected boolean multipart = false;
    protected MultipartMessageReader multipartReader = null;

}
