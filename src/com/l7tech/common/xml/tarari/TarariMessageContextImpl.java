/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari;

import com.tarari.xml.XMLDocument;
import com.tarari.xml.XMLStreamContext;
import com.tarari.xml.xpath.RAXContext;

/**
 * Represents resources held in kernel-memory by the Tarari driver, namely
 * a document in one buffer and a token list in the other.
 */
public class TarariMessageContextImpl implements TarariMessageContext {
    private final XMLDocument tarariDoc;
    private final RAXContext raxContext;
    private final XMLStreamContext streamContext;
    private final long compilerGeneration;

    TarariMessageContextImpl(XMLDocument doc, RAXContext raxContext, XMLStreamContext streamContext, long compilerGeneration) {
        if (streamContext == null || raxContext == null) throw new IllegalArgumentException();
        this.tarariDoc = doc;
        this.raxContext = raxContext;
        this.streamContext = streamContext;
        this.compilerGeneration = compilerGeneration;
    }

    public void close() {
        tarariDoc.release();
    }

    /**
     * @return the {@link GlobalTarariContext} compiler generation count that was in effect when this
     *         TarariMessageContext was produced.
     */
    public long getCompilerGeneration() {
        return compilerGeneration;
    }

    /**
     * @return the RAXContext. Never null.
     */
    public RAXContext getRaxContext() {
        return raxContext;
    }

    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            close();
        }
    }

    public XMLStreamContext getStreamContext() {
        return streamContext;
    }
}
