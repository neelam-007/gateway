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

/**
 * Public handle to a schema obtained from SchemaManager.compile().
 */
public final class SchemaHandle implements Closeable {
    private final CompiledSchema cs;
    private volatile boolean closed = false;

    SchemaHandle(CompiledSchema cs) {
        this.cs = cs;
    }

    /** Validate the entire message against this schema. */
    public void validateMessage(Message msg, SchemaValidationErrorHandler errorHandler) throws NoSuchPartException, IOException, SAXException {
        cs.validateMessage(msg, errorHandler);
    }

    /** Validate just these elements against this schema.  This will not be hardware accelerated. */
    public void validateElements(Element[] elementsToValidate, SchemaValidationErrorHandler errorHandler) throws IOException, SAXException {
        cs.validateElements(elementsToValidate, errorHandler);
    }

    /**
     * Report that noone will ever again try to use this handle. This may cause the underlying schema to
     * be immediately unloaded/destroyed/etc, without waiting for the finalizer to get around to it,
     * if this was the last handle using it.
     */
    public void close() {
        if (closed) return;
        closed = true;
        cs.unref();
    }

    protected void finalize() throws Throwable {
        try {
            close();
        } finally {
            super.finalize();
        }
    }

    CompiledSchema getCompiledSchema() {
        return cs;
    }
}
