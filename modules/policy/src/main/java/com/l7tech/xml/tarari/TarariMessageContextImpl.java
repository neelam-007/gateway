/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.xml.tarari;

import com.l7tech.message.SoapInfo;
import com.l7tech.util.SoapConstants;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.SoftwareFallbackException;
import com.l7tech.xml.TarariLoader;
import com.l7tech.xml.tarari.TarariElementCursor;
import com.l7tech.xml.tarari.GlobalTarariContextImpl;
import com.l7tech.xml.tarari.TarariUtil;
import com.tarari.xml.XmlException;
import com.tarari.xml.rax.cursor.RaxCursor;
import com.tarari.xml.rax.cursor.RaxCursorFactory;
import com.tarari.xml.rax.fastxpath.FNode;
import com.tarari.xml.rax.fastxpath.FNodeSet;
import com.tarari.xml.rax.fastxpath.XPathProcessor;
import com.tarari.xml.rax.fastxpath.XPathResult;
import com.tarari.xml.rax.RaxDocument;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents resources held by the Tarari driver in the form of a RaxDocument and simultaneous XPathResult.
 * This class should only be referenced by other classes that are already contaminated with direct or indirect
 * static references to Tarari classes.  Anyone else should instead reference the uncontaminated interface 
 * {@link TarariMessageContext} instead.
 */
public class TarariMessageContextImpl implements TarariMessageContext {
    private static final Logger logger = Logger.getLogger(TarariMessageContextImpl.class.getName());
    private static final RaxCursorFactory raxCursorFactory = new RaxCursorFactory();
    private static final QName[] EMPTY_QNAMES = new QName[0];

    private final RaxDocument raxDocument;
    private XPathResult xpathResult = null;
    private long compilerGeneration = 0;
    private RaxCursor raxCursor = null;

    TarariMessageContextImpl(RaxDocument doc) {
        if (doc == null) throw new IllegalArgumentException();
        this.raxDocument = doc;
    }

    public void close() {
        if (raxDocument != null && !raxDocument.isReleased()) {
            raxDocument.release();
        }
    }

    /**
     * Get the compiler generation count that was in effect when simultaneous xpath processing was
     * performed on the RaxDocument.  This will trigger simultaneous xpath evalutation if it has not
     * yet been performed on this document.
     *
     * @return the {@link GlobalTarariContext} compiler generation count that was in effect when
     *         the Simultaneous XPaths were evaluated against this document.
     * @throws SoftwareFallbackException if simultaneous xpath processing could not be performed.
     */
    public long getCompilerGeneration() throws SoftwareFallbackException {
        if (compilerGeneration < 1) getXpathResult(); // make sure it's initialized
        return compilerGeneration;
    }

    public ElementCursor getElementCursor() {
        if (raxCursor == null) {
            raxCursor = raxCursorFactory.createCursor("", getRaxDocument());
        }
        return new TarariElementCursor(raxCursor, this);
    }

    // TODO is there some better place to stow the soapaction value?
    public SoapInfo getSoapInfo(String soapAction) throws SoftwareFallbackException {
        List<QName> payloadNames = new ArrayList<QName>();
        boolean hasSecurityHeaders = false;
        XPathResult xpathResult = getXpathResult();
        GlobalTarariContextImpl globalContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        if (xpathResult.isSoap(globalContext.getSoapNamespaceUriIndices())) {
            int numUris = xpathResult.getCount( TarariUtil.XPATH_INDEX_PAYLOAD);
            if (numUris <= 0) {
                logger.info("Couldn't find a namespace URI for SOAP payload");
            } else {
                FNodeSet payloadNodes = xpathResult.getNodeSet(TarariUtil.XPATH_INDEX_PAYLOAD);
                if (payloadNodes == null || payloadNodes.size() < 1) {
                    throw new SoftwareFallbackException("SOAP payload NodeSet was null or empty but count was >= 1");
                }

                int numPayloads = payloadNodes.size();
                for (int i = 0; i < numPayloads; ++i) {
                    FNode first = payloadNodes.getNode(0);
                    if (first.getType() == FNode.ELEMENT_NODE) {
                        String pf = first.getPrefix();
                        String ns = first.getNamespace(pf);
                        String ln = first.getLocalName();
                        if (ns == null) ns = ""; // treat no namespace as same as namespace URI of empty string
                        QName q = pf == null ? new QName(ns, ln) : new QName(ns, ln, pf);
                        payloadNames.add(q);
                    }
                }
            }

            int numSec = xpathResult.getCount(TarariUtil.XPATH_INDEX_SECHEADER);
            if (numSec <= 0) {
                logger.fine("No Security header found");
            } else {
                FNodeSet secNodes = xpathResult.getNodeSet(TarariUtil.XPATH_INDEX_SECHEADER);
                if (secNodes == null || secNodes.size() <= 0) {
                    throw new SoftwareFallbackException("Security NodeSet empty or null, but count was >= 1");
                }

                int numNodes = secNodes.size();
                for (int i = 0; i < numNodes; ++i) {
                    FNode node = secNodes.getNode(i);
                    String uri = node.getNamespace(node.getPrefix());
                    if ( SoapConstants.SECURITY_URIS.contains(uri)) {
                        hasSecurityHeaders = true;
                        break;
                    }
                }
            }
            return new SoapInfo(true, soapAction, payloadNames.toArray(EMPTY_QNAMES), hasSecurityHeaders);
        } else {
            return new SoapInfo(false, soapAction, EMPTY_QNAMES, false);
        }
    }

    /** @return the RaxDocument.  Never null. */
    public RaxDocument getRaxDocument() {
        return raxDocument;
    }

    /**
     * Get the simultaneous xpath results for this document.  This will trigger simultaneous xpath processing for
     * this document if it has not yet been performed on this document.
     *
     * @return the Simultaneous XPath results for this document.  Never null.
     * @throws SoftwareFallbackException if simultaneous xpath processing could not be performed.
     */
    public XPathResult getXpathResult() throws SoftwareFallbackException {
        if (xpathResult != null)
            return xpathResult;
        GlobalTarariContextImpl globalContext = (GlobalTarariContextImpl)TarariLoader.getGlobalContext();
        GlobalTarariContextImpl.tarariLock.readLock().lock();
        try {
            // Lock in earliest compiler generation that might have been in effect when we evaluate the XPaths
            compilerGeneration = globalContext.getCompilerGeneration();
            final XPathProcessor xpathProcessor = new XPathProcessor(raxDocument);
            xpathResult = xpathProcessor.processXPaths();
        } catch (XmlException e) {
            // It's hard to see how failure here is possible unless there's a hardware problem.  Fallback to software.
            throw new SoftwareFallbackException(e);
        } finally {
            GlobalTarariContextImpl.tarariLock.readLock().unlock();
        }
        return xpathResult;
    }
}
