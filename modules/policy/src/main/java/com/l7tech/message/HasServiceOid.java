package com.l7tech.message;

/**
 * Interface for message knobs that can provide a service OID for service resolution.
 */
public interface HasServiceOid {

    /**
     * Get the service OID.
     *
     * <p>If a target service OID is known it should be returned. If the
     * service OID is not specified then 0 should be returned.</p>
     *
     * @return The service oid or 0.
     */
    long getServiceOid();
}
