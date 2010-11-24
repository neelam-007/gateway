package com.l7tech.server.communityschemas;

import com.l7tech.gateway.common.audit.Audit;

import java.io.IOException;

/**
 * A resolver for XML Schema source.
 */
public interface SchemaSourceResolver {

    /**
     * Get the unique identifier for the resolver.
     *
     * @return the identifier
     */
    String getId();

    /**
     * Should schemas from this source be considered transient;
     *
     * @return true for transient.
     */
    boolean isTransient();

    /**
     * Should schemas from this source be considered remote;
     *
     * @return true for remote, false for local.
     */
    boolean isRemote();

    /**
     * Access an XML Schema by target namespace.
     *
     * @param audit the audit to use (required)
     * @param targetNamespace The target namespace of the schema (may be null)
     * @return The (unique) schema for the target namespace or null.
     */
    SchemaSource getSchemaByTargetNamespace( Audit audit, String targetNamespace ) throws IOException;

    /**
     * Access an XML Schema by URI.
     *
     * @param audit the audit to use (required)
     * @param uri The uri, which may be relative (required)
     * @return The schema for the URI or null.
     */
    SchemaSource getSchemaByUri(  Audit audit, String uri ) throws IOException;

    /**
     * Refresh Schema by URI.
     *
     * <p>This may be called periodically to check if the schema is up to date.
     * If a refresh is required the invalidation listener should be notified.</p>
     *
     * @param audit the audit to use (required)
     * @param uri The uri, which may be relative (required)
     */
    void refreshSchemaByUri(  Audit audit, String uri ) throws IOException;

    /**
     * Register the listener for invalidation of schemas from this source.
     *
     * @param listener The listener to use for invalidation callbacks.
     */
    void registerInvalidationListener( SchemaInvalidationListener listener );

    /**
     * Interface for schema source.
     *
     * <p>Schema source implementations should be immutable.</p>
     */
    interface SchemaSource {
        /**
         * Get the URI of the schema.
         *
         * @return The uri, never null.
         */
        String getUri();

        /**
         * Get the content for the schema.
         *
         * @return The content, never null.
         */
        String getContent();

        /**
         * Is this schema from a transient source.
         *
         * @return true if transient.
         */
        boolean isTransient();

        /**
         * Get the identifier of the resolver that created this source.
         *
         * @return The resolver id.
         */
        String getResolverId();
    }

    /**
     * Default implementation for schema source.
     */
    static final class DefaultSchemaSource implements SchemaSource {
        private final String uri;
        private final String content;
        private final String resolverId;
        private final boolean isTransient;

        public DefaultSchemaSource( final String uri,
                                    final String content,
                                    final SchemaSourceResolver resolver ) {
            this( uri, content, resolver.getId(), resolver.isTransient() );
        }

        public DefaultSchemaSource( final String uri,
                                    final String content,
                                    final String resolverId,
                                    final boolean isTransient ) {
            this.uri = uri;
            this.content = content;
            this.resolverId = resolverId;
            this.isTransient = isTransient;
        }

        @Override
        public String getUri() {
            return uri;
        }

        @Override
        public String getContent() {
            return content;
        }

        @Override
        public String getResolverId() {
            return resolverId;
        }

        @Override
        public boolean isTransient() {
            return isTransient;
        }
    }

    /**
     * Interface for schema invalidation callbacks.
     */
    interface SchemaInvalidationListener {

        /**
         * Invalidate the schema at the given uri.
         *
         * @param uri The uri.
         * @param validReplacement false if the replacement is known to be invalid.
         * @return true if invalidated
         */
        boolean invalidateSchemaByUri( String uri, boolean validReplacement );
    }
}
