/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.xml.tarari;

/**
 * A handle for an XML Schema document.
 */
public class SchemaHandle {
    private int refCount = 0;
    private final String namespaceUri;
    private final String schemaDoc;
    private boolean loaded;

    /**
     * Create a handle for tracking a schema.
     *
     * @param expr    the schema namespaceUri to track.  Must not be null.
     * @param document the document to track. Must not be null.
     */
    public SchemaHandle(String expr, String document) {
        if (expr == null || document == null) throw new NullPointerException();
        this.namespaceUri = expr;
        this.schemaDoc = document;
    }

    public String getNamespaceUri() {
        return namespaceUri;
    }

    public String getSchemaDoc() {
        return schemaDoc;
    }

    public synchronized void ref() {
        refCount++;
    }

    public synchronized void unref() {
        if (--refCount < 1)
            refCount = 0;
    }

    public synchronized boolean inUse() {
        return (refCount > 0);
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }
}
