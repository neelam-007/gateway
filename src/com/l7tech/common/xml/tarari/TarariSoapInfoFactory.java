/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.message.SoapInfo;
import com.l7tech.common.message.SoapInfoFactory;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.tarari.xml.Node;
import com.tarari.xml.NodeSet;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathProcessor;
import com.tarari.xml.xpath.XPathProcessorException;
import org.xml.sax.SAXException;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Tarari hardware-accelerated SoapInfoFactory.
 */
public class TarariSoapInfoFactory implements SoapInfoFactory {
    private static final Logger logger = Logger.getLogger(TarariSoapInfoFactory.class.getName());

    public SoapInfo getSoapInfo(InputStream inputStream) throws SoftwareFallbackException, SAXException {
        return getSoapInfoTarari(inputStream);
    }

    /** Hardware version of getSoapInfo.  Might require software fallback if hardware processing can't be done. */
    public static SoapInfo getSoapInfoTarari(InputStream in) throws SAXException, SoftwareFallbackException {
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

    /**
     * Load the specified InputStream, which is expected to produce an XML document, into the Tarari board,
     * run the current XPaths against it, and return the TarariContext.
     *
     * @param inputStream  An InputStream containing an XML document.  Must not be null.
     * @return the TarariContext pointing at the document and the processed results.  Never null.
     * @throws SAXException if this document is invalid.
     * @throws SoftwareFallbackException if this document could not be processed in hardware.
     *                                              the operation should be retried using software.
     */
    static TarariContext getProcessedContext(InputStream inputStream)
            throws SoftwareFallbackException, SAXException
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
}
