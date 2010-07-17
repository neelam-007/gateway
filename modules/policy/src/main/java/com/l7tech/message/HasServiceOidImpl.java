package com.l7tech.message;

/**
 * Facet that can be attached to a message to request hardwired service resolution.
 */
public class HasServiceOidImpl implements HasServiceOid {
    private final long serviceOid;

    /**
     * @param serviceOid the service oid to resolve.
     */
    public HasServiceOidImpl(long serviceOid) {
        this.serviceOid = serviceOid;
    }

    @Override
    public long getServiceOid() {
        return serviceOid;
    }
}
