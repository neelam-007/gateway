/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.util.MultipartUtil;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.MessageProcessor;
import com.l7tech.server.attachments.ServerMultipartMessageReader;
import com.l7tech.server.transport.jms.BytesMessageInputStream;
import com.l7tech.server.transport.jms.JmsUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class XmlMessageAdapter extends MessageAdapter implements XmlMessage {
    private static final Logger logger = Logger.getLogger(XmlMessageAdapter.class.getName());

    public XmlMessageAdapter( TransportMetadata tm ) {
        super(tm);
    }

    synchronized void parse( String xml ) throws SAXException, IOException {
        // TODO: Ensure this is a lazy parser
        _document = XmlUtil.getDocumentBuilder().parse( new InputSource( new StringReader( xml ) ) );
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
     * @param id the String representation of the request id (or response Id - not supported yet) for forming part
     *           of the file name for storing the raw attachments if buffer limit exceeded.
     * @return the XML as a String.  Never null.
     * @throws IOException if a multipart message has an invalid format, or the content cannot be read
     */
    protected String getMessageXml(InputStream is, String id) throws IOException {
        String ctype = (String)getParameter(Message.PARAM_HTTP_CONTENT_TYPE);
        MultipartUtil.HeaderValue contentTypeHeader = MultipartUtil.parseHeader(XmlUtil.CONTENT_TYPE + ": " + ctype);

        if (XmlUtil.MULTIPART_CONTENT_TYPE.equals(contentTypeHeader.getValue())) {

            multipart = true;

            if(!soapPartParsed) {
                String multipartBoundary = MultipartUtil.unquote((String)contentTypeHeader.getParam(XmlUtil.MULTIPART_BOUNDARY));
                if (multipartBoundary == null) throw new IOException("Multipart header '" + contentTypeHeader.getName() + "' did not contain a boundary");

                String innerType = MultipartUtil.unquote((String)contentTypeHeader.getParam(XmlUtil.MULTIPART_TYPE));
                if (innerType.startsWith(XmlUtil.TEXT_XML)) {
                    multipartReader = new ServerMultipartMessageReader(is, multipartBoundary);
                    multipartReader.setFileCacheId(id);

                    MultipartUtil.Part part = multipartReader.getSoapPart();
                    if (part == null)
                        return ""; // Bug #1350 - avoid NPE with empty request
                    final String soapPartContentType = part.getHeader(XmlUtil.CONTENT_TYPE).getValue();
                    if (!soapPartContentType.equals(innerType)) throw new IOException("Content-Type of first part doesn't match type of Multipart header");

                    soapPartParsed = true;
                    return part.getContent();
                } else throw new IOException("Expected first part of multipart message to be XML (was '" + innerType + "')");
            } else {
                if(multipartReader != null) {
                    return multipartReader.getSoapPart().getContent();
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

    public Map getAttachments() throws IOException {
        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
         return multipartReader.getMessageAttachments();
    }

    public MultipartUtil.Part getSoapPart() throws IOException {
        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
        return multipartReader.getMessagePart(0);
    }

    public String getMultipartBoundary() {
        if(multipartReader == null) throw new IllegalStateException("The attachment cannot be retrieved as the soap part has not been read.");
        return multipartReader.getMultipartBoundary();
    }

     public ServerMultipartMessageReader getMultipartReader() {
        return multipartReader;
    }

    public boolean isMultipart() throws IOException {
        // if not multipart set, may be because the request has not been read yet
        // and if it was and not multipart message, the call is cheap as messages are cached 
        if (!multipart) {
            if (this instanceof XmlRequest) {
               ((XmlRequest)this).getRequestXml();
            } else if(this instanceof XmlResponse) {
                ((XmlResponse)this).getResponseXml();
            }
        }
        return multipart;
    }

    public boolean isSoap() {
        if ( soap == null ) {
            Element docEl = null;

            boolean ok;
            try {
                Document doc = getDocument();
                if (doc == null) return false;
                docEl = doc.getDocumentElement();
                ok = true;
            } catch (NoDocumentPresentException e) {
                logger.fine("No document present");
                ok = false;
            } catch ( Exception e ) {
                logger.log(Level.INFO, "Unable to check if document is SOAP", e);
                ok = false;
            }

            ok = ok && docEl.getLocalName().equals(SoapUtil.ENVELOPE_EL_NAME);
            if ( ok ) {
                String docUri = docEl.getNamespaceURI();

                // Check that envelope is one of the recognized namespaces
                for ( Iterator i = SoapUtil.ENVELOPE_URIS.iterator(); i.hasNext(); ) {
                    String envUri = (String)i.next();
                    if (envUri.equals(docUri)) ok = true;
                }
            }

            soap = ok ? Boolean.TRUE : Boolean.FALSE;
        }

        return soap.booleanValue();
    }

    protected Document _document;
    protected boolean soapPartParsed = false;
    protected boolean multipart = false;
    protected ServerMultipartMessageReader multipartReader = null;
    protected Boolean soap = null;

}
