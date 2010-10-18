package com.l7tech.server.communityschemas;

import org.xml.sax.SAXException;

import java.io.IOException;

public interface SchemaManager {

    /**
     * Make this schema document available at the specified URI.  This supersedes any
     * previously-existing registered schema at this global URI.
     *
     * <p>NOTE: to register a schema with no TNS pass an empty string rather
     * than NULL as the tns. A NULL value means TNS lookup should not be
     * supported for the schema.</p>
     *
     * @param uri The URI for the schema (e.g. "policy:123" for a policy schema). Must not be null.
     * @param tns The tns for the schema, if lookup by TNS should be supported for the schema. May be null.
     * @param schemaDoc  the schema document to associate with this URI. Must not be null.
     */
    void registerSchema(String uri, String tns, String schemaDoc);

    /**
     * Remove any previously-registered schema document at the specified URI.
     *
     * @param uri The URI for the schema (e.g. "policy:123" for a policy schema). Must not be null.
     */
    void unregisterSchema(String uri);

    /**
     * Register interest in the specified URI.
     *
     * <p>Notify the schema manager of interest in the given URI. This may be
     * used by the schema manager to keep the schema for the URI available.</p>
     *
     * <p>This does not cause the URI to be processed.</p>
     *
     * <p>The caller must unregister the uri.</p>

     * @param uri The URI of interest.
     * @see #unregisterUri
     */
    void registerUri(String uri);

    /**
     * Remove any previously registered interest in the given URI.
     *
     * @param uri The URI to unregister.
     */
    void unregisterUri(String uri);

    /**
     * Get an up-to-date {@link SchemaHandle} for a schema document specified by its URI.
     * <p/>
     * The system will make an effort to ensure that frequently-accessed remote URI schemas are kept "hot", even
     * loading into Tarari where possible, even if all outstanding handles are temporarily closed.
     * <p/>
     * Caller must close the handle when they no longer need it.  Callers should not keep this handle for long
     * periods of time, since it is a handle to the version of the schema that was known when this method was
     * called.
     *
     * @param uri The URI for the schema to load (e.g. "policy:123" for a policy schema). Must not be null.
     * @return  a SchemaHandle for the given document.  Never null.  Caller should close this handle when they
     *          no longer need the schema.
     * @throws IOException  if a remote resource could not be accessed
     * @throws  SAXException if the schema or a dependent could not be compiled
     */
    SchemaHandle getSchemaByUri(String uri) throws IOException, SAXException;

    /**
     * Check if the schema for the URI is currently registered.
     *
     * <p>A schema is registered if it is a registered schema, or a dependency
     * of a registered schema or if it matches a registered URI or is a
     * dependency of a schema that matches a registered URI.</p>
     *
     * @param uri The URI for the schema. Must not be null.
     * @return True if the associated schema is registered.
     */
    boolean isSchemaRegistered(String uri);
}
