package com.l7tech.server.util;

/**
 * Component for update of a database schema.
 */
public interface SchemaUpdater {

    /**
     * Verify that the schema is the expected version.
     *
     * <p>This will upgrade to the current schema version if possible.</p>
     *
     * @throws SchemaException If an error occurs.
     */
    void ensureCurrentSchema() throws SchemaException;

    class SchemaException extends RuntimeException {
        public SchemaException( final String message ) {
            super( message );
        }

        public SchemaException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }

    class SchemaVersionException extends SchemaException {
        /**
         * Create a new schema version exception for the given versions.
         *
         * @param expected The expected version
         * @param found The current version
         */
        public SchemaVersionException( final int expected,
                                       final int found ) {
            super( "Expected schema version " + expected + ", but found version " + found );
        }
    }

    class SchemaUpgradeException extends SchemaException {
        public SchemaUpgradeException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }
}
