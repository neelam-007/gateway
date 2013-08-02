package com.l7tech.message;

import com.l7tech.objectmodel.Goid;

/**
 * Interface for message knobs that can provide a service GOID for service resolution.
 * 
 * <p>It is expected that the service GOID is provided by internal configuration,
 * this should not be used for service GOIDs provided in a request.</p>
 */
public interface HasServiceGoid extends MessageKnob {

    /**
     * Get the service GOID.
     *
     * <p>If a target service GOID is known it should be returned. If the
     * service GOID is not specified then null should be returned.</p>
     *
     * @return The service goid or null.
     */
    Goid getServiceGoid();
}
