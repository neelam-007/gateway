package com.l7tech.proxy.datamodel;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.MimeBody;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;

/**
 * Base class for SSB pending requests and SSB responses from the Gateway.
 */
public class ClientXmlMessage {
    private final MimeBody mimeBody;
    private final Document originalDocument;
    private final HttpHeaders headers;
    private final boolean isSoapMessage;

    private boolean firstPartBodyMatchesDecoratedDocument = false; // if false, first part of mimeBodyot be in sync with decorated document

    /**
     * Create a new ClientXmlMessage.
     *
     * @param mimeBody   MimeBody instance that is managing the InputStream, or null.
     *                           If no MimeBody is provided, one will be made up by serializing the document
     *                           and treating it as a single-part message of type text/xml.
     * @param originalDocument   The XML document in the message.  Must not be null.
     * @param headers            HTTP headers for logging purposes, or null if there weren't any.
     * @throws IOException       if originalDocument needs to be serialized, but cannot be, due to some
     *                           canonicalizer problem (relative namespaces, maybe)
     */
    protected ClientXmlMessage(MimeBody mimeBody, Document originalDocument, HttpHeaders headers)
            throws IOException
    {
        if (originalDocument == null) throw new NullPointerException("soapEnvelope is null");
        if (mimeBody == null) {
            try {
                mimeBody = new MimeBody(XmlUtil.nodeToString(originalDocument).getBytes(),
                                                        ContentTypeHeader.XML_DEFAULT);
            } catch (NoSuchPartException e) {
                throw new RuntimeException(e); // can't happen with single-part XML_DEFAULT
            }
        }
        this.mimeBody = mimeBody;
        this.originalDocument = originalDocument;
        this.headers = headers;
        this.isSoapMessage = originalDocument == null ? false : SoapUtil.isSoapMessage(originalDocument);
    }

    /**
     * Get the working copy of the Document representing the request.  This returns a copy that can be
     * modified freely.
     * <p/>
     * If you want your changes to stick, you'll need to save them back by calling setUndecoratedDocument().
     *
     * @return A copy of the SOAP envelope Document, which may be freely modified.
     */
    public Document getDecoratedDocument() {
        return originalDocument;
    }

    /**
     * Get the actual Document representing the request, which should not be modified.  Any change
     * to this Document will prevent the reset() method from returning this PendingRequest to
     * its original state.  If you need to change the Document, use getDecoratedDocument() instead.
     *
     * @return A reference to the original SOAP envelope Document, which must not be modified in any way.
     */
    public final Document getOriginalDocument() {
        return originalDocument;
    }

    /** @return the HTTP headers that accompanied this message, or null if there weren't any. */
    public HttpHeaders getHeaders() {
        return headers;
    }

    /** @return true if the original document was SOAP */
    public boolean isSoap() {
        return isSoapMessage;
    }

    /** Ensure that the first part is up-to-date with the decorated document. */
    protected void ensureFirstPartBodyMatchesDecoratedDocument() throws IOException {
        if (firstPartBodyMatchesDecoratedDocument)
            return;

        byte[] xmlBytes = XmlUtil.nodeToString(getDecoratedDocument()).getBytes("UTF-8"); // agree with XML_DEFAULT about encoding
        mimeBody.getFirstPart().setBodyBytes(xmlBytes);
        mimeBody.getFirstPart().setContentType(ContentTypeHeader.XML_DEFAULT);
        firstPartBodyMatchesDecoratedDocument = true;
    }

    public boolean isMultipart() {
        return mimeBody.isMultipart();
    }

    /** @return the outer content type of the request, or a default.  never null. */
    public ContentTypeHeader getOuterContentType() {
        return mimeBody.getOuterContentType();
    }

    /**
     * @return the entire length of the current message body including any applied decorations,
     *         all attachments, and any MIME boundaries; but not including any HTTP or other headers
     *         that would accompany this message over wire.
     * @throws java.io.IOException if the main input stream could not be read, or a MIME syntax error was encountered.
     */
    public long getContentLength() throws IOException {
        try {
            ensureFirstPartBodyMatchesDecoratedDocument();
            long len = 0;
            len = mimeBody.getEntireMessageBodyLength();
            if (len < 0)
                throw new IllegalStateException("At least one multipart part length could not be determinated"); // can't happen
            return len;
        } catch (NoSuchPartException e) {
            throw new IllegalStateException("At least one multipart part's body has been lost"); // can't happen
        }
    }

    /**
     * @return an InputStream which will, when read, produce the entire current message body including any applied
     *         decorations, all attachments, and any MIME boundaries; but not including any HTTP or other headers
     *         that would accompany this message over wire.
     * @throws java.io.IOException if the main input stream could not be read, or a MIME syntax error was encountered.
     */
    public InputStream getEntireMessageBody() throws IOException {
        try {
            ensureFirstPartBodyMatchesDecoratedDocument();
            return mimeBody.getEntireMessageBodyAsInputStream(false);
        } catch (NoSuchPartException e) {
            throw new IllegalStateException("At least one multipart part's body has been lost"); // can't happen
        }
    }

    /**
     * Notify that the document may have changed, and that the first body part will need to be regenerated
     * next time the entire message body is reserialied.
     */
    public void invalidateFirstBodyPart() {
        firstPartBodyMatchesDecoratedDocument = false;
    }

    /**
     * Close the request and run the cleanup runnables.
     */
    public void close() {
        mimeBody.close();
    }
}
