/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.message;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.SoapFaultUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.SoapFaultDetail;
import com.l7tech.common.xml.TarariUtil;
import com.tarari.xml.Node;
import com.tarari.xml.NodeSet;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathProcessor;
import com.tarari.xml.xpath.XPathProcessorException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides the SOAP aspect of a Message.
 */
class SoapFacet extends MessageFacet {
    private SoapFaultDetail faultDetail = null;
    private final SoapInfo soapInfo;

    private transient static final Logger logger = Logger.getLogger(SoapFacet.class.getName());

    /**
     * Load the specified InputStream, which is expected to produce an XML document, into the Tarari board,
     * run the current XPaths against it, and return the TarariContext.
     *
     * @param inputStream  An InputStream containing an XML document.  Must not be null.
     * @return the TarariContext pointing at the document and the processed results.  Never null.
     * @throws SAXException if this document is invalid.
     * @throws TarariUtil.SoftwareFallbackException if this document could not be processed in hardware.
     *                                              the operation should be retried using software.
     */
    private static TarariContext getProcessedContext(InputStream inputStream)
            throws TarariUtil.SoftwareFallbackException, SAXException
    {
        try {
            XMLDocument tarariDoc = null;
            tarariDoc = new XMLDocument(inputStream);
            XPathProcessor tproc = new XPathProcessor(tarariDoc);
            RAXContext context = tproc.processXPaths();
            return new TarariContext(tarariDoc, context);
        } catch (XPathProcessorException e) {
            TarariUtil.translateException(e);
        } catch (XMLDocumentException e) {
            TarariUtil.translateException(e);
        }
        throw new RuntimeException(); // unreachable
    }

    /**
     * Represents information extracted from a SOAP document that can be used for future service resolution.
     * The same SoapInfo class is used for both software or hardware processing.
     */
    static class SoapInfo {
        SoapInfo(String payloadNsUri, boolean hasSecurityNode) {
            this.payloadNsUri = payloadNsUri;
            this.hasSecurityNode = hasSecurityNode;
        }

        final String payloadNsUri;
        final boolean hasSecurityNode;
    }

    /**
     * Represents resources held in kernel-memory by the Tarari driver, namely
     * a document in one buffer and a token list in the other.
     */
    private static class TarariContext {
        private TarariContext(XMLDocument doc, RAXContext context) {
            this.tarariDoc = doc;
            this.raxContext = context;
        }

        void close() {
            tarariDoc.release();
            raxContext = null;
            tarariDoc = null;
        }

        protected void finalize() throws Throwable {
            try {
                super.finalize();
            } finally {
                close();
            }
        }

        XMLDocument tarariDoc;
        RAXContext raxContext;
    }

    /**
     * Create a new SoapFacet for the specified message, wrapping the specified root facet.  SoapFacets can only
     * be created on messages for which {@link #getSoapInfo} has already returned a non-null value.
     *
     * @param message  the message being enhanced.  May not be null.
     * @param facet  the previous root facet.  May not be null.  Must contain an XML facet.
     * @param soapInfo  the non-null result of calling getSoapInfo() with this message's first part content.
     */
    public SoapFacet(Message message, MessageFacet facet, SoapInfo soapInfo) {
        super(message, facet);

        if (soapInfo == null) throw new NullPointerException();
        this.soapInfo = soapInfo;
    }

    /**
     * Check if an already-known-to-be-XML Message is actually SOAP, and if so, returns a SoapInfo
     * for creating a SoapFacet.
     *
     * @param message  the Message to examine.  Must already have a MimeKnob and an XmlKnob.
     * @return the SoapInfo if this Message is SOAP, or null if it is not SOAP.
     * @throws IOException if there is a problem reading the Message data.
     * @throws SAXException if the XML is not well formed or has invalid namespace declarations
     * @throws NoSuchPartException if the Message first part has already been destructively read
     */
    public static SoapInfo getSoapInfo(Message message) throws IOException, SAXException, NoSuchPartException {
        if (TarariUtil.isTarariPresent()) {
            try {
                return getSoapInfoTarari(message.getMimeKnob().getFirstPart().getInputStream(false));
            } catch (TarariUtil.SoftwareFallbackException e) {
                // TODO if this happens a lot for perfectly reasonable reasons, downgrade to something below INFO
                logger.log(Level.INFO, "Falling back from hardware to software processing", e);
                // fallthrough to software
            }
        }
        return getSoapInfoDom(message.getXmlKnob().getDocument(false));
    }

    /** Software fallback version of getSoapInfo.  Requires DOM parsing have been done already. */
    private static SoapInfo getSoapInfoDom(Document document) throws SAXException {
        boolean hasSecurityNode = false;
        String payloadNs = null;
        if (SoapUtil.isSoapMessage(document)) {
            try {
                List els = SoapUtil.getSecurityElements(document);
                if (els != null && !els.isEmpty()) hasSecurityNode = true;
                payloadNs = SoapUtil.getPayloadNamespaceUri(document);
                return new SoapInfo(payloadNs, hasSecurityNode);
            } catch (InvalidDocumentFormatException e) {
                throw new SAXException(e);
            }
        } else {
            return null;
        }
    }

    /** Hardware version of getSoapInfo.  Might require software fallback if hardware processing can't be done. */
    private static SoapInfo getSoapInfoTarari(InputStream in) throws SAXException, TarariUtil.SoftwareFallbackException {
        TarariContext context = null;
        String payloadNs = null;
        boolean hasSecurityHeaders = false;
        try {
            context = getProcessedContext(in);
            if (context.raxContext.isSoap(TarariUtil.getUriIndices())) {
                int numUris = context.raxContext.getCount(0,6);
                if (numUris <= 0) {
                    logger.info("Couldn't find a namespace URI for SOAP payload");
                } else {
                    NodeSet payloadNodes = context.raxContext.getNodeSet(0,6);
                    if (payloadNodes == null) {
                        throw new SAXException("SOAP payload NodeSet was null but count was >= 1");
                    }
                    Node first = payloadNodes.getFirstNode();
                    if (first.getNodeType() == Node.ELEMENT_NODE) {
                        payloadNs = context.raxContext.getNamespaceByPrefix(first, first.getPrefix());
                    }
                }

                int numSec = context.raxContext.getCount(0,7);
                if (numSec <= 0) {
                    logger.fine("No Security header found");
                } else {
                    NodeSet secNodes = context.raxContext.getNodeSet(0,7);
                    if (secNodes == null || secNodes.getNodeCount() <= 0) {
                        throw new SAXException("Security NodeSet empty or null, but count was >= 1");
                    }

                    Node node = secNodes.getFirstNode();
                    while (node != null) {
                        String uri = context.raxContext.getNamespaceByPrefix(node, node.getPrefix());
                        if (SoapUtil.SECURITY_URIS.contains(uri)) {
                            hasSecurityHeaders = true;
                            break;
                        }
                        node = secNodes.getNextNode();
                    }
                }
                return new SoapInfo(payloadNs, hasSecurityHeaders);
            } else {
                return null;
            }
        } finally {
            if (context != null) context.close();
        }
    }

    public MessageKnob getKnob(Class c) {
        if (c == SoapKnob.class) {
            return new SoapKnob() {

                public String getPayloadNamespaceUri() throws IOException, SAXException {
                    if (soapInfo != null && soapInfo.payloadNsUri != null) {
                        return soapInfo.payloadNsUri;
                    } else {
                        return SoapUtil.getPayloadNamespaceUri(getMessage().getXmlKnob().getDocument(false));
                    }
                }

                public boolean isFault() throws SAXException, IOException, InvalidDocumentFormatException {
                    return gatherFaultDetail() != null;
                }

                public SoapFaultDetail getFaultDetail() throws InvalidDocumentFormatException, IOException, SAXException {
                    if (!isFault()) throw new IllegalStateException("Message is not a SOAP fault");
                    return faultDetail;
                }

                public void setFaultDetail(SoapFaultDetail faultDetail) {
                    if (faultDetail == null || faultDetail.getFaultActor() == null || faultDetail.getFaultActor().length() < 1)
                        throw new IllegalArgumentException("Fault detail is null or has a missing or empty fault actor");
                    SoapFacet.this.faultDetail = faultDetail;
                }

                public boolean isSecurityHeaderPresent() {
                    return soapInfo.hasSecurityNode;
                }

                /**
                 * Get the fault details for this message.
                 *
                 * @return the fault details for this message if it's a SOAP fault, or null if it isn't.
                 * @throws SAXException if the first part's content type is not text/xml.
                 * @throws IOException if there is a problem reading XML from the first part's InputStream
                 * @throws InvalidDocumentFormatException if the message is not SOAP or there is more than one Body
                 * @throws com.l7tech.common.xml.MissingRequiredElementException if the message looks like a SOAP fault but has a missing or empty faultcode
                 */
                private SoapFaultDetail gatherFaultDetail() throws SAXException, IOException, InvalidDocumentFormatException {
                    if (faultDetail == null)
                       faultDetail = SoapFaultUtils.gatherSoapFaultDetail(getMessage().getXmlKnob().getDocument(false));
                    return faultDetail;
                }
            };
        }
        return super.getKnob(c);
    }
}
