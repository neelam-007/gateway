package com.l7tech.server.service.resolution;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.message.Message;
import com.l7tech.message.SoapKnob;

import java.util.*;

/**
 * Resolves messages to services.
 *
 * <p>Prior to 5.3 this functionality was part of the ServiceCache.</p>
 */
public class ServiceResolutionManager {

    //- PUBLIC

    public ServiceResolutionManager( final Collection<ServiceResolver> resolvers,
                                     final Collection<ServiceResolver> validatingResolvers ) {
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

        boolean notified = false;
        for (ServiceResolver resolver : activeResolvers) {
            if (rl != null && resolver.usesMessageContent() && !notified) {
                notified = true;
                if (!rl.notifyMessageBodyAccess(req, serviceSet))
                    return null;
            }

            Set<PublishedService> resolvedServices;
            Result result = resolver.resolve(req, serviceSet);
            if (result == Result.NOT_APPLICABLE) {
                // next resolver gets the same subset
                continue;
            } else if (result == Result.NO_MATCH) {
                // Early failure
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_FAILED_EARLY, resolver.getClass().getSimpleName());
                return null;
            } else {
                // Matched at least one... Next resolver can narrow it down
                resolvedServices = result.getMatches();
            }

            int size = resolvedServices.size();
            // if remaining services are 0 or 1, we are done
            if (size == 1) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED_EARLY, resolver.getClass().getSimpleName());
                serviceSet = resolvedServices;
                break;
            } else if (size == 0) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_FAILED_EARLY, resolver.getClass().getSimpleName());
                return null;
            }

            // otherwise, try to narrow down further using next resolver
            serviceSet = resolvedServices;
        }

        if (serviceSet.isEmpty()) {
            auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NO_MATCH);
            return null;
        }

        if (serviceSet.size() > 1) {
            // Try one last filtering pass before giving up - we'll throw out strict SOAP services if the request is not SOAP (Bug #9316)
            // XXX This may be a lot of effort to go to just in order to support mixing SOAP and non-SOAP services on the same URI
            if (rl != null && !notified) {
                notified = true;
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

        if (rl != null && !notified) {
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
            final Result services = resolver.resolve( req, serviceSet );
            if ( services.getMatches().isEmpty() ) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_OPERATION_MISMATCH, service.getName(), service.getId());
                return null;
            }
        }

        auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED, service.getName(), service.getId());
        return service;
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

    //- PRIVATE

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
}
