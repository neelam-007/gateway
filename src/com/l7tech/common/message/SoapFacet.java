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
import com.l7tech.common.xml.MessageNotSoapException;
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
import java.util.List;
import java.util.logging.Logger;

/**
 * Provides the SOAP aspect of a Message.
 */
class SoapFacet extends MessageFacet {
    private SoapFaultDetail faultDetail = null;

    private String payloadNsUri;
    private boolean hasSecurityNode;
    private transient static final Logger logger = Logger.getLogger(SoapFacet.class.getName());

    private TarariContext getProcessedContext() throws XMLDocumentException, IOException, NoSuchPartException, XPathProcessorException {
        XMLDocument tarariDoc = null;
        tarariDoc = new XMLDocument(getMessage().getMimeKnob().getFirstPart().getInputStream(false));
        XPathProcessor tproc = new XPathProcessor(tarariDoc);
        RAXContext context = tproc.processXPaths();
        return new TarariContext(tarariDoc, context);
    }

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
     * Create a new SoapFacet for the specified message, wrapping the specified root facet.
     *
     * @param message  the message being enhanced.  May not be null.
     * @param facet  the previous root facet.  May not be null.  Must contain an XML facet.
     * @throws SAXException if the first part's content type is not text/xml.
     * @throws IOException if XML serialization throws IOException, perhaps due to a lazy Document.
     * @throws MessageNotSoapException if there is an XML document but it doesn't look like a valid SOAP envelope
     */
    public SoapFacet(Message message, MessageFacet facet)
            throws SAXException, IOException, MessageNotSoapException
    {
        super(message, facet);

        if (TarariUtil.isTarariPresent()) {
            TarariContext context = null;
            TarariContext context2 = null;
            try {
                context = getProcessedContext();
                if (!context.raxContext.isSoap(TarariUtil.getUriIndices())) {
                    context2 = getProcessedContext();
                    if (!context.raxContext.isSoap(TarariUtil.getUriIndices())) {
                        // What I say to you two times is true
                        throw new MessageNotSoapException("This message might not be SOAP");
                    }
                }
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
                        this.payloadNsUri = context.raxContext.getNamespaceByPrefix(first, first.getPrefix());
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
                            hasSecurityNode = true;
                            break;
                        }
                        node = secNodes.getNextNode();
                    }
                }
            } catch (XMLDocumentException e) {
                throw new SAXException(e);
            } catch (NoSuchPartException e) {
                throw new SAXException(e);
            } catch (XPathProcessorException e) {
                throw new SAXException(e);
            } finally {
                if (context != null) context.close();
                if (context2 != null) context2.close();
            }
        } else {
            Document doc = message.getXmlKnob().getDocument(false);
            if (SoapUtil.isSoapMessage(doc)) {
                try {
                    List els = SoapUtil.getSecurityElements(doc);
                    if (els != null && !els.isEmpty()) hasSecurityNode = true;
                } catch (InvalidDocumentFormatException e) {
                    throw new SAXException(e);
                }
            } else {
                throw new MessageNotSoapException();
            }
        }
    }

    public MessageKnob getKnob(Class c) {
        if (c == SoapKnob.class) {
            return new SoapKnob() {

                public String getPayloadNamespaceUri() throws IOException, SAXException {
                    if (payloadNsUri != null) {
                        return payloadNsUri;
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
                    return hasSecurityNode;
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
