/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.security.xml.decorator.DecorationRequirements;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.util.XmlUtil;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a MimeFacet whose first part is text/xml.
 */
class XmlFacet extends MessageFacet {
    private Document document = null;
    private ProcessorResult processorResult = null;
    private DecorationRequirements decorationRequirements = null;

    /** Can be assumed to be true if {@link #document} == null */
    private boolean firstPartValid;

    /**
     * Create a new XML message facet.
     *
     * @param message   the Message this facet will belong to.  May not be null.
     * @param delegate  the next facet in the chain.  May not be null for XmlFacet.
     *                  Typically, this would be the Message's already-installed MimeFacet.
     * @throws IOException if there is a problem obtaining the first part.
     *                     (Typically, the delegate is the MimeFacet and this can't happen.)
     * @throws SAXException if the outer content type is not text/xml.
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
            return new MimeKnobImpl(mk);
        } else if (c == XmlKnob.class) {
            final MimeKnob mk = (MimeKnob)super.getKnob(MimeKnob.class);
            return new XmlKnobImpl(mk);
        }
        return super.getKnob(c);
    }

    private class MimeKnobImpl implements MimeKnob {
        private final MimeKnob mk;

        public MimeKnobImpl(MimeKnob mk) {
            if (mk == null) throw new NullPointerException();
            this.mk = mk;
        }

        public boolean isMultipart() {
            return mk.isMultipart();
        }

        public PartIterator getParts() throws IOException {
            ensureFirstPartValid();
            return mk.getParts();
        }

        public PartInfo getPartByContentId(String contentId) throws IOException, NoSuchPartException {
            final PartInfo pi = mk.getPartByContentId(contentId);
            if (pi.getPosition() == 0)
                return getFirstPart();
            return pi;
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
            final PartInfo firstPart = mk.getFirstPart();
            if (firstPartValid)
                return firstPart;
            ensureFirstPartValid();
            return firstPart;
        }

        /**
         * Ensure that the first MIME part's bytes match the serialized form of the current document.
         *
         * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
         */
        private void ensureFirstPartValid() throws IOException {
            if (firstPartValid) {
                return;
            } else if (document == null) {
                firstPartValid = true;
                return;
            }

            final ContentTypeHeader textXml = ContentTypeHeader.XML_DEFAULT;
            final byte[] bytes = XmlUtil.nodeToString(document).getBytes(textXml.getEncoding());
            if (bytes != null) {
                final PartInfo firstPart = mk.getFirstPart();
                firstPart.setBodyBytes(bytes);
                firstPart.setContentType(textXml);
            }

            firstPartValid = true;
        }

    }

    private class XmlKnobImpl implements XmlKnob {
        private final MimeKnob mk;

        public XmlKnobImpl(MimeKnob mk) {
            if (mk == null) throw new NullPointerException();
            this.mk = mk;
        }

        public Document getDocument() throws SAXException, IOException {
            if (document == null) {
                final PartInfo firstPart = mk.getFirstPart();
                if (!firstPart.getContentType().isXml())
                    throw new SAXException("Content type of first part of message is not XML");
                try {
                    document = XmlUtil.parse(firstPart.getInputStream(false));
                } catch (NoSuchPartException e) {
                    throw new SAXException("Unable to parse XML: " + e);
                }
            }
            firstPartValid = false; // Assume caller is going to run roughshod over document
            return document;
        }

        public void setDocument(Document document) {
            firstPartValid = false;
            XmlFacet.this.document = document;
        }

        public ProcessorResult getProcessorResult() {
            return processorResult;
        }

        public void setProcessorResult(ProcessorResult pr) {
            processorResult = pr;
        }

        public void setDecorationRequirements(DecorationRequirements dr) {
            decorationRequirements = dr;
        }

        public DecorationRequirements getDecorationRequirements() {
            return decorationRequirements;
        }

        public DecorationRequirements getOrMakeDecorationRequirements() {
            DecorationRequirements dr = getDecorationRequirements();
            if (dr != null) return dr;
            dr = new DecorationRequirements();
            setDecorationRequirements(dr);
            return dr;
        }
    }
}
