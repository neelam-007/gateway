/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.xml.tarari;

import com.tarari.xml.rax.RaxDocument;
import com.tarari.xml.rax.fastxpath.XPathResult;

/**
 * Represents resources held by the Tarari driver in the form of a RaxDocument and simultaneous XPathResult.
 */
public class TarariMessageContextImpl implements TarariMessageContext {
    private final RaxDocument raxDocument;
    private final XPathResult xpathResult;
    private final long compilerGeneration;

    TarariMessageContextImpl(RaxDocument doc, XPathResult xpathResult, long compilerGeneration) {
        if (doc == null || xpathResult == null) throw new IllegalArgumentException();
        this.raxDocument = doc;
        this.xpathResult = xpathResult;
        this.compilerGeneration = compilerGeneration;
    }

    /** Free resources used by this Tarari context.  After this is called, behavior of this instance is undefined. */
    public void close() {
        raxDocument.release();
    }

    /**
     * @return the {@link GlobalTarariContext} compiler generation count that was in effect when
     *         the Simultaneous XPaths were evaluated against this document.
     */
    public long getCompilerGeneration() {
        return compilerGeneration;
    }

    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            close();
        }
    }

    /** @return the RaxDocument.  Never null. */
    public RaxDocument getRaxDocument() {
        return raxDocument;
    }

    /** @return the Simultaneous XPath results for this document.  Never null. */
    public XPathResult getXpathResult() {
        return xpathResult;
    }
}
