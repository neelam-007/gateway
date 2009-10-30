package com.l7tech.gateway.common.schema;

/**
 * An exception thrown when a schema is failed to fetch.
 *
 * @author ghuang
 */
public class FetchSchemaFailureException extends Exception {
    public FetchSchemaFailureException(String message) {
        super(message);
    }
}
