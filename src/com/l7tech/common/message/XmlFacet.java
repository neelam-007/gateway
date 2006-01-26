/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.common.message;

import com.l7tech.common.mime.*;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a MimeFacet whose first part is text/xml.
 */
public class XmlFacet extends MessageFacet {
    private Document originalDocument = null;  // the original Document
    private Document workingDocument = null;  // the working Document

    /** Can be assumed to be true if {@link #workingDocument} == null */
    private boolean firstPartValid = true;

    /**
     * Create a new XML message facet.
     *
     * @param message   the Message this facet will belong to.  May not be null.
     * @param delegate  the next facet in the chain.  May not be null for XmlFacet.
     *                  Typically, this would be the Message's already-installed MimeFacet.
     * @throws IOException if there is a problem obtaining the first part.
     *                     (Typically, the delegate is the MimeFacet and this can't happen.)
     * @throws SAXException if the first part's content type is not text/xml.
     */
    public XmlFacet(Message message, MessageFacet delegate) throws IOException, SAXException {
        super(message, delegate);
        if (!message.getMimeKnob().getFirstPart().getContentType().isXml())
            throw new SAXException("Message first part is not text/xml");
    }

    public MessageKnob getKnob(Class c) {
        if (c == MimeKnob.class) {
            // Wrap it with one that DTRT
            final MimeKnob mk = (MimeKnob)super.getKnob(MimeKnob.class);
            return new MimeKnobWrapper(mk);
        } else if (c == XmlKnob.class) {
            final MimeKnob mk = (MimeKnob)super.getKnob(MimeKnob.class);
            return new XmlKnobImpl(mk);
        }
        return super.getKnob(c);
    }

    /** Wraps the existing MimeKnob to add the abilitiy to keep the first part in sync with the XML Document. */
    private class MimeKnobWrapper implements MimeKnob {
        private final MimeKnob mk;

        public MimeKnobWrapper(MimeKnob mk) {
            if (mk == null) throw new NullPointerException();
            this.mk = mk;
        }

        public boolean isMultipart() {
            return mk.isMultipart();
        }

        public PartIterator getParts() throws IOException {
            ensureFirstPartValid();
            return new PartIteratorWrapper(mk.getParts());
        }

        public PartInfo getPart(int num) throws IOException, NoSuchPartException {
            ensureFirstPartValid();
            return mk.getPart(num);
        }

        public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
            final PartInfo pi = mk.getPartByContentId(contentId);
            if (pi.getPosition() == 0)
                return getFirstPart();
            return pi;
        }

        public void setContentLengthLimit(long sizeLimit) throws IOException {
            mk.setContentLengthLimit(sizeLimit);
        }

        public ContentTypeHeader getOuterContentType() throws IOException {
            return mk.getOuterContentType();
        }

        public long getContentLength() throws IOException {
            ensureFirstPartValid();
            return mk.getContentLength();
        }

        public InputStream getEntireMessageBodyAsInputStream() throws IOException, NoSuchPartException {
            ensureFirstPartValid();
            return mk.getEntireMessageBodyAsInputStream();
        }

        /**
         * @throws IOException if XML serialization throws IOException
         */
        public PartInfo getFirstPart() throws IOException {
            final PartInfo firstPart = new PartInfoWrapper(mk.getFirstPart());
            if (firstPartValid)
                return firstPart;
            ensureFirstPartValid();
            return firstPart;
        }

        /**
         * Ensure that the first MIME part's bytes match the serialized form of the current workingDocument.
         *
         * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
         */
        private void ensureFirstPartValid() throws IOException {
            if (firstPartValid) {
                return;
            } else if (workingDocument == null) {
                firstPartValid = true;
                return;
            }

            final ContentTypeHeader textXml = ContentTypeHeader.XML_DEFAULT;
            final byte[] bytes = XmlUtil.nodeToString(workingDocument).getBytes(textXml.getEncoding());
            if (bytes != null) {
                final PartInfo firstPart = mk.getFirstPart();
                firstPart.setBodyBytes(bytes);
                firstPart.setContentType(textXml);
            }

            firstPartValid = true;
        }

        private class PartIteratorWrapper implements PartIterator {
            private final PartIterator delegate;

            private PartIteratorWrapper(PartIterator delegate) {
                this.delegate = delegate;
            }

            public boolean hasNext() throws IOException {
                return delegate.hasNext();
            }

            public PartInfo next() throws IOException, NoSuchPartException {
                final PartInfo pi = delegate.next();
                if (pi.getPosition() == 0)
                    return getFirstPart();
                return pi;
            }
        }
    }

    private class XmlKnobImpl implements XmlKnob {
        private final MimeKnob mk;

        public XmlKnobImpl(MimeKnob mk) {
            if (mk == null) throw new NullPointerException();
            this.mk = mk;
        }

        public Document getDocumentReadOnly() throws SAXException, IOException {
            if (workingDocument == null) {
                final PartInfo firstPart = mk.getFirstPart();
                if (!firstPart.getContentType().isXml())
                    throw new SAXException("Content type of first part of message is not XML");
                try {
                    workingDocument = XmlUtil.parse(firstPart.getInputStream(false));
                } catch (NoSuchPartException e) {
                    throw new SAXException("Unable to parse XML: " + e);
                }
            }
            return workingDocument;
        }

        public Document getDocumentWritable() throws SAXException, IOException {
            Document working = getDocumentReadOnly();
            firstPartValid = false;
            TarariKnob.invalidate(getMessage());
            if (getMessage().isEnableOriginalDocument() && originalDocument == null)
                originalDocument = (Document)working.cloneNode(true); // todo find a way to skip this if it wont be needed
            return working;
        }

        public Document getOriginalDocument() throws SAXException, IOException {
            if (!getMessage().isEnableOriginalDocument())
                throw new UnsupportedOperationException("originalDocumentSupport is not enabled");
            if (originalDocument == null)
                originalDocument = (Document)getDocumentReadOnly().cloneNode(true);
            return originalDocument;
        }

        public void setDocument(Document document) {
            firstPartValid = false;
            workingDocument = document;
            TarariKnob.invalidate(getMessage());
        }
    }

    private class PartInfoWrapper implements PartInfo {
        private final PartInfo delegate;

        private PartInfoWrapper(PartInfo delegate) {
            this.delegate = delegate;
        }

        public MimeHeader getHeader(String name) {
            return delegate.getHeader(name);
        }

        public int getPosition() {
            return delegate.getPosition();
        }

        public InputStream getInputStream(boolean destroyAsRead) throws IOException, NoSuchPartException {
            return delegate.getInputStream(destroyAsRead);
        }

        public void setBodyBytes(byte[] newBody) throws IOException {
            delegate.setBodyBytes(newBody);
            if (isFirstPart()) {
                if (workingDocument != null && getMessage().isEnableOriginalDocument() && originalDocument == null)
                    originalDocument = (Document)workingDocument.cloneNode(true); // todo find a way to skip this if it wont be needed
                workingDocument = null;
                firstPartValid = false;
                TarariKnob.invalidate(getMessage());
            }
        }

        private boolean isFirstPart() {
            return delegate.getPosition() == 0;
        }

        public void setContentType(ContentTypeHeader newContentType) {
            delegate.setContentType(newContentType);
        }

        public MimeHeaders getHeaders() {
            return delegate.getHeaders();
        }

        public long getContentLength() {
            return delegate.getContentLength();
        }

        public long getActualContentLength() throws IOException, NoSuchPartException {
            return delegate.getActualContentLength();
        }

        public ContentTypeHeader getContentType() {
            return delegate.getContentType();
        }

        public String getContentId(boolean stripAngleBrackets) {
            return delegate.getContentId(stripAngleBrackets);
        }

        public boolean isValidated() {
            return delegate.isValidated();
        }

        public void setValidated(boolean validated) {
            delegate.setValidated(validated);
        }
    }
}
