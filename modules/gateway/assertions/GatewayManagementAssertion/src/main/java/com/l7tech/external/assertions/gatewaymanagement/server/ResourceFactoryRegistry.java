package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.api.impl.AccessorSupport;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.ExceptionUtils;
import com.sun.ws.management.InternalErrorFault;
import com.sun.ws.management.enumeration.Enumeration;
import com.sun.ws.management.framework.transfer.TransferSupport;
import com.sun.ws.management.server.EnumerationItem;
import com.sun.ws.management.server.EnumerationIterator;
import com.sun.ws.management.server.EnumerationSupport;
import com.sun.ws.management.server.HandlerContext;
import com.sun.ws.management.server.IteratorFactory;
import com.sun.ws.management.soap.FaultException;
import com.sun.ws.management.soap.SOAP;
import org.springframework.beans.factory.InitializingBean;
import org.xmlsoap.schemas.ws._2004._08.addressing.EndpointReferenceType;

import javax.xml.parsers.DocumentBuilder;
import java.util.*;
import java.util.logging.Logger;

/**
 * Registry for ResourceFactories.
 */
public class ResourceFactoryRegistry implements InitializingBean {

    //- PUBLIC

    /**
     * Create a new registry with the given factories.
     *
     * @param factories The map of resource names to factories.
     */
    public ResourceFactoryRegistry( final List<ResourceFactory<?,?>> factories ) {
        this.factories = Collections.unmodifiableMap(asMap(factories));
    }

    /**
     * Get the resource factory for the given resource URI.
     *
     * @param resourceUri The URI for the resource factory
     * @return The resource factory or null if not found
     */
    @SuppressWarnings({"unchecked"})
    public <R,E> ResourceFactory<R,E> getResourceFactory( final String resourceUri ) {
        ResourceFactory<R,E> factory = null;

        if ( resourceUri.startsWith(RESOURCE_URI_PREFIX) ) {
            final String resourceId = resourceUri.substring( RESOURCE_URI_PREFIX.length() );
            factory = (ResourceFactory<R,E>) factories.get( resourceId );
        }

        return factory;
    }

    /**
     * Performs registration of the resource factories.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        for ( Map.Entry<String,ResourceFactory<?,?>> resourceFactoryEntry : factories.entrySet() ) {
            final String resourceUri =  RESOURCE_URI_PREFIX + resourceFactoryEntry.getKey();
            try {
                logger.fine("Registering enumeration support for resource '"+resourceUri+"'.");
                EnumerationSupport.registerIteratorFactory(
                       resourceUri,
                        new ResourceIteratorFactory( resourceUri, resourceFactoryEntry.getValue() ) );
            } catch ( Exception e ) {
                throw ExceptionUtils.wrap( e );
            }
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ResourceFactoryRegistry.class.getName());

    private static final String RESOURCE_URI_PREFIX = ResourceHandler.MANAGEMENT_NAMESPACE + "/";

    private final Map<String,ResourceFactory<?,?>> factories;

    /**
     * Build resource factory map using factory annotation. 
     */
    private Map<String, ? extends ResourceFactory<?,?>> asMap( final List<ResourceFactory<?,?>> factories ) {
        final Map<String,ResourceFactory<?,?>> factoryMap = new HashMap<String,ResourceFactory<?,?>>();

        for ( final ResourceFactory factory : factories ) {
            final ResourceFactory.ResourceType resourceType =
                    factory.getClass().getAnnotation( ResourceFactory.ResourceType.class );

            if ( resourceType == null ) {
                throw new IllegalStateException("Attempt to register resource factory '"+factory.getClass().getName()+"' missing ResourceType annotation.");
            }

            if ( factoryMap.put( AccessorSupport.getResourceName( resourceType.type() ), factory ) != null ) {
                throw new IllegalStateException("Attempt to register resource factory '"+factory.getClass().getName()+"' with duplicate resource name.");
            }
        }

        return factoryMap;
    }

    /**
     * IteratorFactory for a resource factory.
     */
    private final static class ResourceIteratorFactory implements IteratorFactory {
        private final String resourceUri;
        private final ResourceFactory<?,?> resourceFactory;

        ResourceIteratorFactory( final String resourceUri,
                                 final ResourceFactory<?,?> resourceFactory ) {
            this.resourceUri = resourceUri;
            this.resourceFactory = resourceFactory;
        }

        @SuppressWarnings({ "unchecked" })
        @Override
        public EnumerationIterator newIterator( final HandlerContext context,
                                                final Enumeration request,
                                                final DocumentBuilder db,
                                                final boolean includeItem,
                                                final boolean includeEPR ) throws FaultException {
            Map<String,Object> properties = (Map<String,Object>) context.getRequestProperties();
            properties.put( "com.l7tech.status.entityType", getEntityType(resourceFactory) );

            return new ResourceEnumerationIterator( resourceUri, resourceFactory, context.getURL(), includeItem, includeEPR );
        }

        private String getEntityType( final ResourceFactory<?,?> factory ) {
            final EntityType type = factory.getType();
            return type==null ? null : type.getName();
        }
    }

    /**
     * EnumerationIterator for a resource factory
     */
    private final static class ResourceEnumerationIterator implements EnumerationIterator {
        private final String resourceUri;
        private final ResourceFactory<?,?> resourceFactory;
        private final Collection<Map<String,String>> resourceSelectors;
        private final Iterator<Map<String,String>> resourceSelectorIterator;
        private final String address;
        private final boolean includeItem;
        private final boolean includeEPR;

        ResourceEnumerationIterator( final String resourceUri,
                                     final ResourceFactory<?,?> resourceFactory,
                                     final String address,
                                     final boolean includeItem,
                                     final boolean includeEPR ) {
            this.resourceUri = resourceUri;
            this.resourceFactory = resourceFactory;
            this.address = address;
            this.includeItem = includeItem;
            this.includeEPR = includeEPR;

            try {
                resourceSelectors = resourceFactory.getResources();
                resourceSelectorIterator = resourceSelectors.iterator();
            } catch ( Exception e ) {
                throw (InternalErrorFault) new InternalErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(e), null, ExceptionUtils.getDebugException(e), null)).initCause(e);
            }
        }

        @Override
        public int estimateTotalItems() {
            return resourceSelectors.size();
        }

        @Override
        public boolean isFiltered() {
            return false;
        }

        @Override
        public boolean hasNext() {
            return resourceSelectorIterator.hasNext();
        }

        @Override
        public EnumerationItem next() {
            final Map<String,String> selectors = resourceSelectorIterator.next();

            final Object resource;
            if ( includeItem ) {
                try {
                    resource = resourceFactory.getResource( selectors );
                } catch (ResourceFactory.ResourceNotFoundException e) {
                    throw (InternalErrorFault) new InternalErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(e), null, ExceptionUtils.getDebugException(e), null)).initCause(e);
                }
            } else {
                resource = null;
            }

            final EndpointReferenceType epr;
            if ( includeEPR ) {
                try {
                    epr = TransferSupport.createEpr(
                            address,
                            resourceUri,
                            selectors );
                } catch ( Exception e ) {
                    throw(InternalErrorFault)  new InternalErrorFault(SOAP.createFaultDetail(ExceptionUtils.getMessage(e), null, ExceptionUtils.getDebugException(e), null)).initCause(e);
                }
            } else {
                epr = null;
            }

            return new EnumerationItem( resource, epr );
        }

        @Override
        public void release() {
        }
    }    
}
