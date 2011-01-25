package com.l7tech.server.service;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.server.service.resolution.ServiceResolutionException;

/**
 * Interface for service resolution
 */
public interface ServiceCacheResolver {

    PublishedService resolve( Message req, ServiceCache.ResolutionListener rl) throws ServiceResolutionException;

    void checkResolution( PublishedService service ) throws ServiceResolutionException;
}
