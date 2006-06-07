/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import com.l7tech.common.message.Message;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.util.Closeable;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;

public final class SchemaHandle implements Closeable {
    private final CompiledSchema cs;
    private volatile boolean closed = false;

    SchemaHandle(CompiledSchema cs) {
        this.cs = cs;
    }

    public void validateMessage(Message msg, SchemaValidationErrorHandler errorHandler) throws NoSuchPartException, IOException, SAXException {
        cs.validateMessage(msg, errorHandler);
    }

    public void validateElements(Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
        cs.validateElements(elementsToValidate, errorHandler);
    }

    public void close() {
        if (closed) return;
        closed = true;
        cs.unref();
    }

    protected void finalize() throws Throwable {
        try {
            super.finalize();
        } finally {
            close();
        }
    }
}
