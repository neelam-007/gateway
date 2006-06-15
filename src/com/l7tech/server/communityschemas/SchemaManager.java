/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.communityschemas;

import java.io.IOException;
import java.text.ParseException;
import java.util.regex.Pattern;

public interface SchemaManager {

    /**
     * Get the {@link CompiledSchema} for the specified schema document,
     * reusing an existing instance
     * <p/>
     * if possible.  If the schema was loaded from a URL, that URL should be supplied as a
     * System ID, so that imports using relative URIs can be resolved.
     *
     * @param   schemadoc the XML Schema Document to get a CompiledSchema for. Must not be null.
     * @param   systemId  the System ID from which the document was loaded. May be null or empty.
     * @return  a SchemaHandle for the given document.  Never null.  Caller should close this handle when they
     *          no longer need the schema.
     * @throws  ParseException if the schema or a dependent could not be compiled
     */
    SchemaHandle compile(String schemadoc,
                         String systemId,
                         Pattern[] urlWhitelist)
            throws ParseException;

    /**
     * Get the {@link CompiledSchema} for a remote schema document specified by its URL.
     * <p/>
     * The URL whitelist will be used to determine which (if any) nested import URLs should be dereferenced.
     * (It is assumed that the top-level URL passed to this method has already been approved.)
     * <p/>
     * The system will make an effort to ensure that frequently-accessed schemas are kept "hot", even loading
     * into Tarari where possible, even if all outstanding handles are temporarily closed.
     *
     * @param url            the URL of the remote schema to load.  Must not be null.
     * @param urlWhitelist   a whitelist of import URLs to allow while parsing the remote schema, or null to disallow imports.
     * @return  a SchemaHandle for the given document.  Never null.  Caller should close this handle when they
     *          no longer need the schema.
     * @throws IOException  if a remote resource could not be accessed
     * @throws  ParseException if the schema or a dependent could not be compiled
     */
    SchemaHandle fetchRemote(String url, Pattern[] urlWhitelist)
            throws IOException, ParseException;
}
