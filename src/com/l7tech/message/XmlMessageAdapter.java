/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.common.io.EmptyInputStream;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.CausedIllegalStateException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manage an XML message.  Not even close to thread-safe.
 * @author alex
 * @version $Revision$
 */
public abstract class XmlMessageAdapter extends MessageAdapter implements XmlMessage {
    private static final Logger logger = Logger.getLogger(XmlMessageAdapter.class.getName());

    public XmlMessageAdapter( TransportMetadata tm ) {
        super(tm);
        try {
            initialize(new EmptyInputStream(), ContentTypeHeader.XML_DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e); // can't happen
        }
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

    protected void invalidateFirstBodyPart() {
        super.invalidateFirstBodyPart();
        _document = null;
    }

    public Document getDocument() throws SAXException, IOException {
        if (_document != null)
            return _document;

        InputStream in;
        try {
            in = getFirstPart().getInputStream(false);  // TODO should be no problem consuming document here
        } catch (NoSuchPartException e) {
            throw new CausedIllegalStateException("First part's body was already destructively read", e);
        }

        invalidateFirstBodyPart();
        _document = XmlUtil.parse(in);
        return _document;
    }

    public void setDocument(Document doc) {
        if (!isInitialized()) {
            try {
                // TODO remove this if possible; hopefully we don't really need auto-init after all
                initialize(new ByteArrayInputStream(XmlUtil.nodeToString(doc).getBytes()), ContentTypeHeader.XML_DEFAULT);
            } catch (IOException e) {
                throw new RuntimeException(e); // can't happen
            }
        }
        invalidateFirstBodyPart();
        _document = doc;
    }

    protected Document _document;
    protected boolean soapPartParsed = false;
    protected Boolean soap = null;
}
