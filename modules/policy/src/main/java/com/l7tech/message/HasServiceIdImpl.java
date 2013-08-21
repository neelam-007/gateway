package com.l7tech.message;

import com.l7tech.objectmodel.Goid;

/**
 * Facet that can be attached to a message to request hardwired service resolution.
 */
public class HasServiceIdImpl implements HasServiceId {
    private final Goid serviceGoid;

    /**
     * @param serviceGoid the service oid to resolve.
     */
    public HasServiceIdImpl(Goid serviceGoid) {
        this.serviceGoid = serviceGoid;
    }

    @Override
    public Goid getServiceGoid() {
        return serviceGoid;
    }
}
