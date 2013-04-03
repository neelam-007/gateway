package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.service.PublishedService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Service resolvers can locate a service based on resolution parameters.
 *
 * <p>Resolvers are used to match an inbound message to a service and to check
 * for conflicting resolution parameters between services.</p>
 *
 * @param <T> the type of value employed by this resolver
 */
public abstract class ServiceResolver<T> {
    protected final Logger logger = Logger.getLogger(getClass().getName()); // Not static so we get the real classname
    protected final Audit auditor;

    protected static final String SUFFIX_VALUE = ".value";

    protected final String PROP_BASE = getClass().getName();
    protected final String PROP_VALUE = PROP_BASE + SUFFIX_VALUE;
    private final String simpleName;

    public ServiceResolver( final AuditFactory auditorFactory ) {
        auditor = auditorFactory.newInstance( this, logger );
        simpleName = getClass().getSimpleName();
    }

    /**
     * @return the simple classname of this resolver, for logging purposes.
     */
    public String getSimpleName() {
        return simpleName;
    }

    /**
     * Configure this resolver to use the given settings.
     *
     * <p>Resolvers that support configuration should override this method and
     * call super.configure( ... ).</p>
     *
     * @param resolutionConfiguration The configuration to use
     */
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
     * <p>Resolvers that cache service information should override this method.</p>
     *
     * @param service The published service whose resolution parameters may have changed.
     * @throws ServiceResolutionException Can be thrown if resolution cannot be performed and this should be audited.
     */
    public void serviceUpdated( PublishedService service ) throws ServiceResolutionException {
    }

    /**
     * Notify resolver of a deleted service.
     *
     * <p>Resolvers that cache service information should override this method.</p>
     *
     * <p>This method may be called multiple times for a service (e.g. when disabled).</p>
     *
     * @param service The published service that is no longer available for resolution.
     */
    public void serviceDeleted( PublishedService service ) {
    }

    /**
     * Tries to resolve the request to a published service.
     *
     * <p><b><code>serviceSubset</code> was returned from <code>Collections.unmodifiableCollection()</code>, so it should
     * not be iterated over and cannot be modified.</b></p>
     *
     * <p>The target value for resolution is obtained from the target map. If
     * the value is null then this resolver is not applicable.</p>
     *
     * @param parameters The parameters to process
     * @param serviceSubset The collection of published services
     * @return The resolution result
     * @throws ServiceResolutionException If an error occurs
     * @see #PROP_VALUE
     */
    public abstract Result resolve( Map<String,Object> parameters,
                                    Collection<PublishedService> serviceSubset ) throws ServiceResolutionException;

    /**
     * Populate the given resolution parameter map for the message.
     *
     * <p>The target value for the message should be added to the parameter map
     * if this resolver is applicable for the message. Adding a value means
     * this resolver will later be used (even if the value is null).</p>
     *
     * @param request The message
     * @param parameters The parameter map to populate
     * @throws ServiceResolutionException If an error occurs
     * @see #PROP_VALUE
     */
    public abstract void populateResolutionParameters( Message request,
                                                       Map<String,Object> parameters ) throws ServiceResolutionException;

    /**
     * Generate the resolution parameters for the given service.
     *
     * <p>The resolution parameters should be exploded to include all values
     * relevant to this service. If a service has a single target value then
     * the returned collection will be the same size as the given collection.
     * If a service has multiple values then the size of the resulting
     * parameter collection is a multiple of the size of the given collection.</p>
     *
     * @param service The target service
     * @param parameterCollection The current set of parameters
     * @return The resolution parameters, which may be the given collection
     * @throws ServiceResolutionException If an error occurs
     * @see #buildTargetValues(PublishedService)
     */
    public Collection<Map<String,Object>> generateResolutionParameters( final PublishedService service,
                                                                        final Collection<Map<String,Object>> parameterCollection )
            throws ServiceResolutionException {
        final Set<T> values = new HashSet<T>( buildTargetValues( service ) );
        if ( values.isEmpty() ) {
            return parameterCollection;
        } else {
            final List<Map<String,Object>> resultParameterList = new ArrayList<Map<String,Object>>( parameterCollection.size() * values.size() );

            for ( final T value : values ) {
                for ( final Map<String, Object> parameters : parameterCollection ) {
                    final Map<String, Object> resultParameters = new HashMap<String, Object>( parameters );
                    resultParameters.put( PROP_VALUE, value );
                    resultParameterList.add( resultParameters );
                }
            }

            return resultParameterList;
        }
    }

    /**
     * Does this resolver require access to the body of a message?
     *
     * @return True if this resolver accesses the body.
     */
    public abstract boolean usesMessageContent();

    /**
     * Build the target values for the given service.
     *
     * @param service The target service.
     * @return A list of all values for the service (never null)
     * @throws ServiceResolutionException If an error occurs
     */
    protected abstract List<T> buildTargetValues( PublishedService service ) throws ServiceResolutionException;

    /**
     * Update the cached values for the given service.
     *
     * @param service The service
     * @param targetValues The values to use (never null)
     */
    protected abstract void updateServiceValues( PublishedService service,
                                                 List<T> targetValues );
}
