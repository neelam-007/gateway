package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.server.audit.Auditor;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * @param <T> the type of value employed by this resolver
 */
public abstract class ServiceResolver<T> {
    protected final Logger logger = Logger.getLogger(getClass().getName()); // Not static so we get the real classname
    protected final Auditor auditor;

    protected static final String SUFFIX_APPLICABLE = ".applicable";
    protected static final String SUFFIX_VALUE = ".value";

    protected final String PROP_BASE = getClass().getName();
    protected final String PROP_APPLICABLE = PROP_BASE + SUFFIX_APPLICABLE;
    protected final String PROP_VALUE = PROP_BASE + SUFFIX_VALUE;

    public ServiceResolver( final Auditor.AuditorFactory auditorFactory ) {
        auditor = auditorFactory.newInstance( this, logger );
    }

    public void configure( ResolutionConfiguration resolutionConfiguration ) {        
    }

    /**
     * Notify resolver of a new service.
     *
     * @param service The published service that is now available for resolution.
     * @throws ServiceResolutionException Can be thrown if resolution cannot be performed and this should be audited.
     */
    public final void serviceCreated( final PublishedService service ) throws ServiceResolutionException {
        final List<T> targetValues = buildTargetValues( service );
        updateServiceValues( service, targetValues );
    }

    /**
     * Notify resolver of an updated service.
     *
     * @param service The published service whose resolution parameters may have changed.
     * @throws ServiceResolutionException Can be thrown if resolution cannot be performed and this should be audited.
     */
    public void serviceUpdated( PublishedService service ) throws ServiceResolutionException {
    }

    /**
     * Notify resolver of a deleted service.
     *
     * @param service The published service that is no longer available for resolution.
     */
    public void serviceDeleted( PublishedService service ) {
    }

    /**
     * Tries to resolve the request to a published service.
     *
     * <b><code>serviceSubset</code> was returned from <code>Collections.unmodifiableCollection()</code>, so it should
     * not be iterated over and cannot be modified.</b>
     *
     * @param parameters The parameters to process
     * @param serviceSubset The collection of published services
     * @return The resolution result
     * @throws ServiceResolutionException
     */
    public abstract Result resolve( Map<String,Object> parameters,
                                    Collection<PublishedService> serviceSubset ) throws ServiceResolutionException;

    public abstract void populateResolutionParameters( Message request,
                                                       Map<String,Object> parameters ) throws ServiceResolutionException;

    public abstract Collection<Map<String,Object>> generateResolutionParameters( PublishedService service,
                                                                                 Collection<Map<String,Object>> parameterCollection ) throws ServiceResolutionException;

    public abstract boolean usesMessageContent();

    protected abstract List<T> buildTargetValues( PublishedService service ) throws ServiceResolutionException;

    protected abstract void updateServiceValues( PublishedService service,
                                                 List<T> targetValues );
}
