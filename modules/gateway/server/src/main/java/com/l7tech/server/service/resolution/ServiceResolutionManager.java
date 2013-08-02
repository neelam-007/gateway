package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.message.Message;
import com.l7tech.message.SoapKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.transport.ResolutionConfigurationManager;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resolves messages to services.
 *
 * <p>Prior to 5.3 this functionality was part of the ServiceCache.</p>
 */
public class ServiceResolutionManager implements PostStartupApplicationListener, InitializingBean {

    //- PUBLIC

    public ServiceResolutionManager( final ResolutionConfigurationManager resolutionConfigurationManager,
                                     final String resolutionConfigurationName,
                                     final Collection<ServiceResolver> resolvers,
                                     final Collection<ServiceResolver> validatingResolvers ) {
        this.resolutionConfigurationManager = resolutionConfigurationManager;
        this.resolutionConfigurationName = resolutionConfigurationName;
        this.activeResolvers = Collections.unmodifiableCollection( new ArrayList<ServiceResolver>(resolvers) );
        this.validationResolvers = Collections.unmodifiableCollection( new ArrayList<ServiceResolver>(validatingResolvers) );

        final Collection<ServiceResolver> allResolvers = new ArrayList<ServiceResolver>();
        allResolvers.addAll( activeResolvers );
        allResolvers.addAll( validationResolvers );
        this.allResolvers = Collections.unmodifiableCollection( allResolvers );
    }

    /**
     * Caller must hold read lock
     */
    public PublishedService resolve( final Audit auditor,
                                     final Message req,
                                     final ServiceResolutionListener rl,
                                     Collection<PublishedService> serviceSet ) throws ServiceResolutionException {
        if (serviceSet.isEmpty()) {
            auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_NO_SERVICES);
            return null;
        }

        final boolean[] notified = {false};

        final Functions.TernaryThrows<Boolean,ServiceResolver,Map<String,Object>,Collection<PublishedService>,ServiceResolutionException> callback =
                new Functions.TernaryThrows<Boolean,ServiceResolver,Map<String,Object>,Collection<PublishedService>,ServiceResolutionException>(){
            @Override
            public Boolean call( final ServiceResolver resolver,
                              final Map<String, Object> parameters,
                              final Collection<PublishedService> serviceSet ) throws ServiceResolutionException {
                if (rl != null && resolver.usesMessageContent() && !notified[0]) {
                    notified[0] = true;
                    if (!rl.notifyMessageBodyAccess(req, serviceSet))
                        return false;
                }

                resolver.populateResolutionParameters( req, parameters );

                return true;
            }
        };

        serviceSet = doResolve( auditor, new HashMap<String,Object>(), callback, serviceSet, true );

        if ( serviceSet == null ) return null;

        if (serviceSet.isEmpty()) {
            auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NO_MATCH);
            return null;
        }

        if (serviceSet.size() > 1) {
            // Try one last filtering pass before giving up - we'll throw out strict SOAP services if the request is not SOAP (Bug #9316)
            // XXX This may be a lot of effort to go to just in order to support mixing SOAP and non-SOAP services on the same URI
            if (rl != null && !notified[0]) {
                notified[0] = true;
                if (!rl.notifyMessageBodyAccess(req, serviceSet))
                    return null;
            }

            if (!isSoap(req)) {
                serviceSet = filterOutStrictSoapServices(serviceSet);
            }
        }

        if (serviceSet.size() != 1) {
            auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_MULTI);
            return null;
        }

        PublishedService service = serviceSet.iterator().next();

        if (!service.isSoap() || service.isLaxResolution()) {
            return service;
        }

        if (rl != null && !notified[0]) {
            if (!rl.notifyMessageBodyAccess(req, serviceSet))
                return null;
        }

        // If this service is set to strict mode, validate that the message is soap, and that it matches an
        // operation supported in the WSDL.
        if (req.getKnob( SoapKnob.class) == null) {
            auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NOT_SOAP);
            return null;
        }

        if ( rl != null ) {
            rl.notifyMessageValidation( req, service );
        }

        for ( final ServiceResolver resolver : validationResolvers ) {
            final Map<String,Object> parameters = new HashMap<String,Object>();
            resolver.populateResolutionParameters( req, parameters );
            final Result services = resolver.resolve( parameters, serviceSet );
            if ( services.getMatches().isEmpty() ) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_OPERATION_MISMATCH, service.getName(), service.getId());
                return null;
            }
        }

        auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED, service.getName(), service.getId());

        return service;
    }

    public Collection<PublishedService> resolve( final @Nullable String path,
                                                 final @Nullable String soapAction,
                                                 final @Nullable String namespace,
                                                 Collection<PublishedService> serviceSet ) throws ServiceResolutionException {
        if (serviceSet.isEmpty()) {
            return Collections.emptySet();
        }

        // build resolution parameters
        final Map<String,Object> parameters = new HashMap<String,Object>();
        if ( path != null ) {
            parameters.put( UriResolver.class.getName() + ServiceResolver.SUFFIX_VALUE, path );
            parameters.put( CaseInsensitiveUriResolver.class.getName() + ServiceResolver.SUFFIX_VALUE, path.toLowerCase() );
        }
        if ( soapAction != null ) {
            parameters.put( SoapActionResolver.class.getName() + ServiceResolver.SUFFIX_VALUE, soapAction );
        }
        if ( namespace != null ) {
            parameters.put( UrnResolver.class.getName() + ServiceResolver.SUFFIX_VALUE, namespace );
        }

        // resolve
        serviceSet = doResolve( new LoggingAudit(logger), parameters, null, serviceSet, true );

        if ( serviceSet == null || serviceSet.isEmpty()) {
            return Collections.emptySet();
        }

        return serviceSet;
    }

    /**
     * Notification of a new service.
     *
     * @param auditor The auditor to use.
     * @param service The new service
     */
    public void notifyServiceCreated( final Audit auditor, final PublishedService service ) {
        for ( final ServiceResolver resolver : allResolvers ) {
            try {
                if (service.isDisabled()) {
                    resolver.serviceDeleted(service);
                } else {
                    resolver.serviceCreated(service);
                }
            } catch (ServiceResolutionException sre) {
                auditor.logAndAudit(SystemMessages.SERVICE_WSDL_ERROR,
                        new String[]{service.displayName(), sre.getMessage()}, sre);
            }
        }        
    }

    /**
     * Notification of an updated service.
     *
     * @param auditor The auditor to use.
     * @param service The updated service
     */
    public void notifyServiceUpdated( final Audit auditor, final PublishedService service ) {
        for ( final ServiceResolver resolver : allResolvers ) {
            try {
                if (service.isDisabled()) {
                    resolver.serviceDeleted(service);
                } else {
                    resolver.serviceUpdated(service);
                }
            } catch (ServiceResolutionException sre) {
                auditor.logAndAudit( SystemMessages.SERVICE_WSDL_ERROR,
                        new String[]{service.displayName(), sre.getMessage()}, sre);
            }
        }
    }

    /**
     * Notification of a deleted service.
     *
     * @param service The deleted service
     */
    public void notifyServiceDeleted( final PublishedService service ) {
        for ( final ServiceResolver resolver : allResolvers ) {
            resolver.serviceDeleted(service);
        }
    }

    /**
     * Check that the given service is resolvable.
     *
     * @param service The service to check
     * @throws NonUniqueServiceResolutionException If the service is not resolvable
     * @throws ServiceResolutionException If an error occurs
     */
    public void checkResolution( final PublishedService service, final Collection<PublishedService> serviceSet ) throws ServiceResolutionException {
        Collection<Map<String,Object>> parameterCollection = Collections.singleton( Collections.<String, Object>emptyMap() );
        for ( final ServiceResolver resolver : activeResolvers ) {
            parameterCollection = resolver.generateResolutionParameters( service, parameterCollection );
        }

        final Collection<Pair<Map<String,Object>,PublishedService>> conflictingParameterCollection = new ArrayList<Pair<Map<String,Object>,PublishedService>>();
        final Audit audit = new LoggingAudit( logger );
        for ( final Map<String,Object> parameters : parameterCollection ) {

            // Enable exact matching only for conflicts
            parameters.put( UriResolver.class.getName() + UriResolver.SUFFIX_EXACT_ONLY, Boolean.TRUE );
            parameters.put( CaseInsensitiveUriResolver.class.getName() + CaseInsensitiveUriResolver.SUFFIX_EXACT_ONLY, Boolean.TRUE );

            final Collection<PublishedService> services = doResolve( audit, parameters, null, serviceSet, false );
            if ( services==null ) continue;

            for ( final PublishedService conflictingService : services ) {
                if ( !Goid.equals(conflictingService.getGoid(), service.getGoid()) ) {
                    conflictingParameterCollection.add( new Pair<Map<String,Object>,PublishedService>(parameters,conflictingService) );
                }
            }
        }

        if ( !conflictingParameterCollection.isEmpty() ) {
            throw new NonUniqueServiceResolutionException( conflictingParameterCollection );
        }
    }

    @Override
    public void onApplicationEvent( final ApplicationEvent event ) {
        if ( event instanceof EntityInvalidationEvent ) {
            final EntityInvalidationEvent invalidationEvent = (EntityInvalidationEvent) event;
            if ( ResolutionConfiguration.class.equals( invalidationEvent.getEntityClass() ) ) {
                reloadResolverConfiguration();
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        reloadResolverConfiguration();
    }

    /**
     * Listener interface that can be implemented by clients of service resolution manager
     * if pre-processing is required before use of the body of a message.
     *
     * <p>Note that if the message body is not required during service resolution
     * then this listener may not be called.</p>
     */
    public static interface ServiceResolutionListener {

        /**
         * Notification that the contents of the given message are about to be used.
         *
         * @param message The message being used for resolution.
         * @param serviceSet The set of candidate services.
         * @return true if service resolution should continue.
         */
        boolean notifyMessageBodyAccess( Message message, Collection<PublishedService> serviceSet);

        /**
         * Notification that service access is about to be validated.
         */
        void notifyMessageValidation( Message message, PublishedService service );
    }

    //- PACKAGE

    Collection<ServiceResolver> getResolvers() {
        return allResolvers;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ServiceResolutionManager.class.getName() );

    private final ResolutionConfigurationManager resolutionConfigurationManager;

    private final String resolutionConfigurationName;

    /**
     * Resolvers that are used to actually resolve services.
     */
    private final Collection<ServiceResolver> activeResolvers;

    /**
     * Resolvers that are used to validate a message matches a service.
     */
    private final Collection<ServiceResolver> validationResolvers;

    /**
     * Resolvers that are notified of CRUD events on services.
     */
    private final Collection<ServiceResolver> allResolvers;

    /**
     * Caller must hold read lock
     */
    private Collection<PublishedService> doResolve( final Audit auditor,
                                                    final Map<String,Object> parameters,
                                                    final Functions.TernaryThrows<Boolean,ServiceResolver,Map<String,Object>,Collection<PublishedService>,ServiceResolutionException> parameterBuilder,
                                                    Collection<PublishedService> serviceSet,
                                                    final boolean takeShortcut ) throws ServiceResolutionException {
        for ( final ServiceResolver resolver : activeResolvers ) {
            if ( parameterBuilder != null && !parameterBuilder.call( resolver, parameters, serviceSet ) ) return null;

            Set<PublishedService> resolvedServices;
            final Result result = resolver.resolve(parameters, serviceSet);
            if (result == Result.NOT_APPLICABLE) {
                // next resolver gets the same subset
                continue;
            } else if (result == Result.NO_MATCH) {
                // Early failure
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_FAILED_EARLY, resolver.getSimpleName());
                return null;
            } else {
                // Matched at least one... Next resolver can narrow it down
                resolvedServices = result.getMatches();
            }

            int size = resolvedServices.size();
            // if remaining services are 0 or 1, we are done
            if ( takeShortcut && size == 1) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED_EARLY, resolver.getSimpleName());
                serviceSet = resolvedServices;
                break;
            } else if (size == 0) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_FAILED_EARLY, resolver.getSimpleName());
                return null;
            }

            // otherwise, try to narrow down further using next resolver
            serviceSet = resolvedServices;
        }

        return serviceSet;
    }

    private static boolean isSoap(Message req) throws ServiceResolutionException {
        try {
            return req.isSoap();
        } catch (Exception e) {
            throw new ServiceResolutionException("Unable to determine whether message is SOAP", e);
        }
    }

    /**
     * Remove any services from the specified collection that require SOAP and are not in lax mode.
     *
     * @param serviceSet the service list to filter.  Required, but may be empty.
     * @return the filtered list.  May be empty, but never null.
     */
    private static Collection<PublishedService> filterOutStrictSoapServices(Collection<PublishedService> serviceSet) {
        serviceSet = new HashSet<PublishedService>(serviceSet);
        final Iterator<PublishedService> ssit = serviceSet.iterator();
        while (ssit.hasNext()) {
            PublishedService service = ssit.next();
            if (service.isSoap() && !service.isLaxResolution()) {
                ssit.remove();
            }
        }
        return serviceSet;
    }

    private void reloadResolverConfiguration() {
        logger.config( "Reloading resolver configuration '"+resolutionConfigurationName+"'." );
        try {
            final ResolutionConfiguration configuration = resolutionConfigurationManager.findByUniqueName( resolutionConfigurationName );
            if ( configuration != null ) {
                for ( final ServiceResolver resolver : allResolvers ) {
                    resolver.configure( configuration );
                }
            }
        } catch ( final FindException fe ) {
            logger.log( Level.WARNING, "Error loading resolver configuration." );
        }
    }
}
