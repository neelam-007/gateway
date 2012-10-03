package com.l7tech.external.assertions.apiportalintegration.server.resource;

/**
 * Abstract parent class for all portal resources.
 */
public abstract class Resource {
    protected String getStringOrEmpty(final String s) {
        return s == null ? "" : s;
    }
}
