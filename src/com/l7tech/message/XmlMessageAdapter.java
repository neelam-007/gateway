/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.MessageProcessor;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.*;

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
     * @param reader the underlying reader for the message
     * @return the XML as a String
     * @throws IOException if a multipart message has an invalid format, or the content cannot be read
     */
    protected String getMessageXml(Reader reader) throws IOException {
        String ctype = (String)getParameter(Message.PARAM_HTTP_CONTENT_TYPE);
        HeaderValue contentTypeHeader = parseHeader(XmlUtil.CONTENT_TYPE + ": " + ctype);

        if(breader == null) {
            breader = new BufferedReader(reader);
        }

        if (XmlUtil.MULTIPART_CONTENT_TYPE.equals(contentTypeHeader.value)) {
            multipart = true;
            multipartBoundary = (String)contentTypeHeader.params.get(XmlUtil.MULTIPART_BOUNDARY);
            if (multipartBoundary == null) throw new IOException("Multipart header did not contain a boundary");
            String innerType = (String)contentTypeHeader.params.get(XmlUtil.MULTIPART_TYPE);
            if (innerType.startsWith(XmlUtil.TEXT_XML)) {
                String firstBoundary = breader.readLine();
                if (!firstBoundary.equals(XmlUtil.MULTIPART_BOUNDARY_PREFIX + multipartBoundary)) throw new IOException("Initial multipart boundary not found");
                Part part = parseMultipart(breader, 0);
                if (!part.getHeader(XmlUtil.CONTENT_TYPE).value.equals(innerType)) throw new IOException("Content-Type of first part doesn't match type of Multipart header");
                return part.content;
            } else throw new IOException("Expected first part of multipart message to be XML");
        } else {
            // Not multipart, read the whole thing
            StringBuffer xml = new StringBuffer();

            char[] buf = new char[1024];
            int read = reader.read(buf);
            while (read > 0) {
                xml.append(buf, 0, read);
                read = reader.read(buf);
            }
            return xml.toString();
        }
    }

    protected Map getMessageAttachments(Reader reader) throws IOException {
        if(breader == null) {
            breader = new BufferedReader(reader);
        }

        // parse all multiple parts
        parseAllMultiparts(breader);

        Map attachments = new HashMap();

        Set parts = multipartParts.keySet();
        Iterator itr = parts.iterator();
        while (itr.hasNext()) {
            Object o = (Object) itr.next();
            Object val  = (Object) multipartParts.get(o);
            if(val instanceof Part) {
                Message.Part part = (Part) val;
                if(part.getPosition() > 0) {
                    attachments.put(part.getHeader(XmlUtil.CONTENT_ID).value, part);
                }
            } else {
                throw new RuntimeException("The entry retrived from multipartParts object is not the type of com.l7tech.Message.Part");
            }
            System.out.println("The object is: " + o.toString());
        }
        return attachments;
    }

    protected Part getMessagePart(Reader reader, int position) throws IOException {
        if(breader == null) {
             breader = new BufferedReader(reader);
        }

        if(multipartParts.size() < position) {
             return parseMultipart(breader, position);
        } else {
            return getMessagePartFromMap(position);
        }
    }

    private Part getMessagePartFromMap(int position) {

        Part part = null;

        Set keys = multipartParts.keySet();

        Iterator itr = keys.iterator();
        while (itr.hasNext()) {
            Part currentPart = (Part) multipartParts.get(itr.next());
            if(currentPart.getPosition() == position) {
                part = currentPart;
                break;
            }
        }
        return part;
    }

    private HeaderValue parseHeader(String header) throws IOException {
        StringTokenizer stok = new StringTokenizer(header, ":; ", false);
        HeaderValue result = new HeaderValue();
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

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param breader  The data source
     * @param lastPart The part to be parsed
     * @return Part The part parsed
     * @throws IOException
     */
    private Part parseMultipart(BufferedReader breader, int lastPart) throws IOException {

        StringBuffer xml = new StringBuffer();
        Part part = null;

        if(multipartParts.size() > 0 && multipartParts.size() > lastPart) {
            // the part to be retrived is already parsed
            return getMessagePartFromMap(lastPart);
        }

        for (int i = 0; i <= lastPart; i++) {

            part = new Part();
            String line;
            boolean headers = true;
            while ((line = breader.readLine()) != null) {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    HeaderValue header = parseHeader(line);
                    part.headers.put(header.name, header);
                } else {
                    if (line.startsWith("--" + multipartBoundary)) {
                        // The boundary is on a line by itself so the previous content doesn't actually contain the last \n
                        // The next part is left in the reader for later
                        if (xml.length() > 0 && xml.charAt(xml.length()-1) == '\n')
                            xml.deleteCharAt(xml.length()-1);
                        break;
                    } else {
                        xml.append(line);
                        xml.append("\n");
                    }
                }
            }
            part.content = xml.toString();
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).value, part);
        }
        return part;
    }

    /**
     * This parser only peels the the multipart message up to the part required.
     *
     * @param breader  The data source
     * @throws IOException
     */
    private void parseAllMultiparts(BufferedReader breader) throws IOException {

        StringBuffer xml = new StringBuffer();
        Part part = null;

        String line;
        while ((line = breader.readLine()) != null) {
            part = new Part();
            boolean headers = true;
            do {
                if (headers) {
                    if (line.length() == 0) {
                        headers = false;
                        continue;
                    }
                    HeaderValue header = parseHeader(line);
                    part.headers.put(header.name, header);
                } else {
                    if (line.startsWith("--" + multipartBoundary)) {
                        // The boundary is on a line by itself so the previous content doesn't actually contain the last \n
                        // The next part is left in the reader for later
                        if (xml.length() > 0 && xml.charAt(xml.length() - 1) == '\n')
                            xml.deleteCharAt(xml.length() - 1);
                        break;
                    } else {
                        xml.append(line);
                        xml.append("\n");
                    }
                }
            } while ((line = breader.readLine()) != null);

            part.content = xml.toString();
            part.setPostion(multipartParts.size());
            multipartParts.put(part.getHeader(XmlUtil.CONTENT_ID).value, part);
        }
    }

    protected Document _document;

    protected boolean multipart;
    protected String multipartBoundary;
    protected BufferedReader breader = null;
    protected Map multipartParts = new HashMap();
}
