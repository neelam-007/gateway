/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.xml.tarari;

import com.tarari.xml.XMLDocument;
import com.tarari.xml.xpath.RAXContext;

/**
 * Represents resources held in kernel-memory by the Tarari driver, namely
 * a document in one buffer and a token list in the other.
 */
public class TarariMessageContextImpl implements TarariMessageContext {
    private final XMLDocument tarariDoc;
    private final RAXContext raxContext;

    TarariMessageContextImpl(XMLDocument doc, RAXContext context) {
        if (doc == null || context == null) throw new IllegalArgumentException();
        this.tarariDoc = doc;
        this.raxContext = context;
    }

    public void close() {
        tarariDoc.release();
    }

    public XMLDocument getTarariDoc() {
        return tarariDoc;
    }

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
}
