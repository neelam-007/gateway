package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.server.service.ServiceCacheResolver;


/**
 * The ResolutionManager enforces the uniqueness of resolution
 * parameters across all services.
 * <p/>
 * This is used by the ServiceManager when updating and saving services to ensure that resolution
 * parameters do not conflict.
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 25, 2003<br/>
 */
public class ReolutionManagerImpl implements ResolutionManager {

    //- PUBLIC

    public ReolutionManagerImpl( final ServiceCacheResolver serviceCache ) {
        this.serviceCache = serviceCache;
    }

    @Override
    public void checkDuplicateResolution( final PublishedService service ) throws ServiceResolutionException {
        serviceCache.checkResolution( service );
    }

    //- PRIVATE

    private final ServiceCacheResolver serviceCache;
}
