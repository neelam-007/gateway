/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.l7tech.common.message.SoapInfo;
import com.l7tech.common.message.SoapInfoFactory;
import com.l7tech.common.message.TarariMessageContextFactory;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.SoftwareFallbackException;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.server.tarari.GlobalTarariContextImpl;
import com.tarari.xml.rax.fastxpath.XPathResult;
import com.tarari.xml.rax.fastxpath.FNodeSet;
import com.tarari.xml.rax.fastxpath.FNode;
import com.tarari.xml.rax.fastxpath.XPathProcessor;
import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.XmlSource;
import com.tarari.xml.XmlParseException;
import com.tarari.xml.XmlConfigException;
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
        return getProcessedContext(messageBody);
    }

    private static class TarariSoapInfo extends SoapInfo {
        public TarariSoapInfo(boolean isSoap, String payloadNsUri, boolean hasSecurityNode) {
            super(isSoap, payloadNsUri, hasSecurityNode);
        }
    }

    /** Hardware version of getSoapInfo.  Might require software fallback if hardware processing can't be done. */
    public static SoapInfo getSoapInfoTarari(GlobalTarariContext globalContext, TarariMessageContext messageContext1)
            throws SAXException
    {
        TarariMessageContextImpl messageContext = (TarariMessageContextImpl) messageContext1;
        String payloadNs = null;
        boolean hasSecurityHeaders = false;
        XPathResult xpathResult = messageContext.getXpathResult();
        if (xpathResult.isSoap(((GlobalTarariContextImpl)globalContext).getSoapNamespaceUriIndices())) {
            int numUris = xpathResult.getCount(TarariUtil.XPATH_INDEX_PAYLOAD);
            if (numUris <= 0) {
                logger.info("Couldn't find a namespace URI for SOAP payload");
            } else {
                FNodeSet payloadNodes = xpathResult.getNodeSet(TarariUtil.XPATH_INDEX_PAYLOAD);
                if (payloadNodes == null || payloadNodes.size() < 1) {
                    throw new SAXException("SOAP payload NodeSet was null or empty but count was >= 1");
                }

                FNode first = payloadNodes.getNode(0);
                if (first.getType() == FNode.ELEMENT_NODE) {
                    payloadNs = first.getNamespace(first.getPrefix());
                }
            }

            int numSec = xpathResult.getCount(TarariUtil.XPATH_INDEX_SECHEADER);
            if (numSec <= 0) {
                logger.fine("No Security header found");
            } else {
                FNodeSet secNodes = xpathResult.getNodeSet(TarariUtil.XPATH_INDEX_SECHEADER);
                if (secNodes == null || secNodes.size() <= 0) {
                    throw new SAXException("Security NodeSet empty or null, but count was >= 1");
                }

                int numNodes = secNodes.size();
                for (int i = 0; i < numNodes; ++i) {
                    FNode node = secNodes.getNode(i);
                    String uri = node.getNamespace(node.getPrefix());
                    if (SoapUtil.SECURITY_URIS.contains(uri)) {
                        hasSecurityHeaders = true;
                        break;
                    }
                }
            }
            return new TarariSoapInfo(true, payloadNs, hasSecurityHeaders);
        } else {
            return new TarariSoapInfo(false, null, false);
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
     * @throws IOException if there was a problem reading this document InputStream.
     */
    static TarariMessageContextImpl getProcessedContext(InputStream inputStream)
            throws SoftwareFallbackException, SAXException, IOException {
        final GlobalTarariContextImpl globalContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        if (globalContext == null)
            throw new SoftwareFallbackException("No GlobalTarariContext");

        // Lock in earliest compiler generation that might have been in effect when we evaluate the XPaths
        final long generation = globalContext.getCompilerGeneration();

        try {
            XmlSource xmlSource = new XmlSource(inputStream);
            RaxDocument doc = RaxDocument.createDocument(xmlSource);
            final XPathProcessor xpathProcessor = new XPathProcessor(doc);
            XPathResult xpathResult = xpathProcessor.processXPaths();

            // Have to mutex here to make sure nobody changes the compiler generation while we are preparing the result :(
            return new TarariMessageContextImpl(doc,
                                                xpathResult,
                                                generation);
        } catch (XmlParseException e) {
            TarariUtil.translateException(e);
        } catch (XmlConfigException e) {
            TarariUtil.translateException(e);
        }
        throw new RuntimeException(); // unreachable
    }
}
