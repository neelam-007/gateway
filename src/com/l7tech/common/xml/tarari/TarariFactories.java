/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.message.SoapInfo;
import com.l7tech.common.message.SoapInfoFactory;
import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariLoader;
import com.tarari.xml.Node;
import com.tarari.xml.NodeSet;
import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLDocumentException;
import com.tarari.xml.xpath.RAXContext;
import com.tarari.xml.xpath.XPathProcessor;
import com.tarari.xml.xpath.XPathProcessorException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * Tarari hardware-accelerated SoapInfoFactory.
 */
public class TarariFactories implements SoapInfoFactory, TarariMessageContextFactory {
    private static final Logger logger = Logger.getLogger(TarariFactories.class.getName());

    public SoapInfo getSoapInfo(TarariMessageContext messageContext) throws SAXException {
        GlobalTarariContext globalContext = TarariLoader.getGlobalContext();
        return getSoapInfoTarari(globalContext, messageContext);
    }

    public TarariMessageContext makeMessageContext(InputStream messageBody) throws IOException, SAXException, SoftwareFallbackException {
        TarariMessageContextImpl messageContext = getProcessedContext(messageBody);
        return messageContext;
    }

    private static class TarariSoapInfo extends SoapInfo {
        public TarariSoapInfo(String payloadNsUri, boolean hasSecurityNode) {
            super(payloadNsUri, hasSecurityNode);
        }
    }

    /** Hardware version of getSoapInfo.  Might require software fallback if hardware processing can't be done. */
    public static SoapInfo getSoapInfoTarari(GlobalTarariContext globalContext, TarariMessageContext messageContext1)
            throws SAXException
    {
        TarariMessageContextImpl messageContext = (TarariMessageContextImpl) messageContext1;
        String payloadNs = null;
        boolean hasSecurityHeaders = false;
        RAXContext raxContext = messageContext.getRaxContext();
        if (raxContext.isSoap(globalContext.getSoapNamespaceUriIndices())) {
            int numUris = messageContext.getRaxContext().getCount(0, TarariUtil.XPATH_INDEX_PAYLOAD);
            if (numUris <= 0) {
                logger.info("Couldn't find a namespace URI for SOAP payload");
            } else {
                NodeSet payloadNodes = raxContext.getNodeSet(0, TarariUtil.XPATH_INDEX_PAYLOAD);
                if (payloadNodes == null) {
                    throw new SAXException("SOAP payload NodeSet was null but count was >= 1");
                }
                Node first = payloadNodes.getFirstNode();
                if (first.getNodeType() == Node.ELEMENT_NODE) {
                    payloadNs = raxContext.getNamespaceByPrefix(first, first.getPrefix());
                }
            }

            int numSec = raxContext.getCount(0, TarariUtil.XPATH_INDEX_SECHEADER);
            if (numSec <= 0) {
                logger.fine("No Security header found");
            } else {
                NodeSet secNodes = raxContext.getNodeSet(0, TarariUtil.XPATH_INDEX_SECHEADER);
                if (secNodes == null || secNodes.getNodeCount() <= 0) {
                    throw new SAXException("Security NodeSet empty or null, but count was >= 1");
                }

                Node node = secNodes.getFirstNode();
                while (node != null) {
                    String uri = raxContext.getNamespaceByPrefix(node, node.getPrefix());
                    if (SoapUtil.SECURITY_URIS.contains(uri)) {
                        hasSecurityHeaders = true;
                        break;
                    }
                    node = secNodes.getNextNode();
                }
            }
            return new TarariSoapInfo(payloadNs, hasSecurityHeaders);
        } else {
            return null;
        }
    }

    /**
     * Load the specified InputStream, which is expected to produce an XML document, into the Tarari board,
     * run the current XPaths against it, and return the TarariMessageContext.
     * <p>
     * The caller must close the TarariMessageContext when they are finished with it.
     *
     * @param inputStream  An InputStream containing an XML document.  Must not be null.
     * @return the TarariMessageContext pointing at the document and the processed results.  Never null.
     *         Caller is responsible for closing the context.
     * @throws SAXException if this document is invalid.
     * @throws SoftwareFallbackException if this document could not be processed in hardware.
     *                                              the operation should be retried using software.
     */
    static TarariMessageContextImpl getProcessedContext(InputStream inputStream)
            throws SoftwareFallbackException, SAXException
    {
        try {
            XMLDocument tarariDoc = null;
            tarariDoc = new XMLDocument(inputStream);
            XPathProcessor tproc = new XPathProcessor(tarariDoc);
            RAXContext context = tproc.processXPaths();
            return new TarariMessageContextImpl(tarariDoc, context, TarariLoader.getGlobalContext().getCompilerGeneration());
        } catch (XPathProcessorException e) {
            TarariUtil.translateException(e);
        } catch (XMLDocumentException e) {
            TarariUtil.translateException(e);
        }
        throw new RuntimeException(); // unreachable
    }
}
