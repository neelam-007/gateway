/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MultipartMessage;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.server.MessageProcessor;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.*;
import java.util.Iterator;
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

        setInputStream(is);
        MultipartMessage mm = getMultipartMessage();

        final PartInfo soapPart;
        try {
            soapPart = mm.getPart(0);
        } catch (NoSuchPartException e) {
            throw new CausedIOException("Incoming message was missing the first multipart part");
        }

        // First part must be XML currently
        if (!soapPart.getContentType().getType().equalsIgnoreCase("text") ||
            !soapPart.getContentType().getSubtype().equalsIgnoreCase("xml"))
            throw new IOException("Incoming message did not have text/xml as first part");

        InputStream soapStream = soapPart.getInputStream(true); // don't bother saving soap part, since we will be parsing it immediately

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(soapStream, soapPart.getContentType().getEncoding()));

        StringBuffer xml = new StringBuffer();
        char[] buf = new char[4096];
        int read = reader.read(buf);
        while (read > 0) {
            xml.append(buf, 0, read);
            read = reader.read(buf);
        }
        return xml.toString();
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

    protected ContentTypeHeader getUpToDateFirstPartContentType() throws IOException {
        return ContentTypeHeader.XML_DEFAULT;
    }

    protected byte[] getUpToDateFirstPartBodyBytes() throws IOException {
        try {
            // Encoding used here must agree with the encoding returned by getUpToDateFirstPartContentType
            return XmlUtil.nodeToString(getDocument()).getBytes("UTF-8");
        } catch (SAXException e) {
            throw new CausedIOException("Unable to serialize message XML", e);
        }
    }

    protected Document _document;
    protected boolean soapPartParsed = false;
    protected Boolean soap = null;
}
