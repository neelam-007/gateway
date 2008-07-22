/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import org.xml.sax.SAXException;

import java.io.IOException;

public interface SchemaManager {

    /**
     * Make this schema document available at the specified global pseudo-URL.  This supersedes any
     * previously-existing global schema at this global URL.
     *
     * @param globalUrl  a real or pseudo-URL, ie: "policy:123".  Must not be null.
     * @param schemadoc  the schema document to associate with this URL.  Must not be null.
     */
    void registerSchema(String globalUrl, String schemadoc);

    /**
     * Remove any previously-registered schema document at the specified URL.
     *
     * @param globalUrl  a real or pseudo-URL, ie: "policy:123".  Must not be null.
     */
    void unregisterSchema(String globalUrl);

    /**
     * Get an up-to-date {@link SchemaHandle} for a schema document specified by its URL.
     * <p/>
     * The system will make an effort to ensure that frequently-accessed remote URL schemas are kept "hot", even
     * loading into Tarari where possible, even if all outstanding handles are temporarily closed.
     * <p/>
     * Caller must close the handle when they no longer need it.  Callers should not keep this handle for long
     * periods of time, since it is a handle to the version of the schema that was known when this method was
     * called.
     *
     * @param url the URL of the remote schema to load, or the pseudo-URL of a global schema.  Must not be null.
     * @return  a SchemaHandle for the given document.  Never null.  Caller should close this handle when they
     *          no longer need the schema.
     * @throws IOException  if a remote resource could not be accessed
     * @throws  SAXException if the schema or a dependent could not be compiled
     */
    SchemaHandle getSchemaByUrl(String url) throws IOException, SAXException;
}
