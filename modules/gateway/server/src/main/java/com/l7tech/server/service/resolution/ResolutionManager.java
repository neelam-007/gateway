package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.service.PublishedService;

/**
 * @author alex
 */
public interface ResolutionManager {

    /**
     * Check if the given service can be resolved for all operations.
     *
     * @param service The service to check
     * @throws ServiceResolutionException If an error occurs
     * @throws NonUniqueServiceResolutionException If the given service does not uniquely resolve
     */
    void checkDuplicateResolution(PublishedService service) throws ServiceResolutionException;
}
