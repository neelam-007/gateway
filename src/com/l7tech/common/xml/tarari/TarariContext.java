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
class TarariContext {
    TarariContext(XMLDocument doc, RAXContext context) {
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
