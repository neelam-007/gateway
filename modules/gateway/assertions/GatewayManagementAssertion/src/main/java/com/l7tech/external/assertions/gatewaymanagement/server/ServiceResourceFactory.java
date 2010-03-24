package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.PolicyExportResult;
import com.l7tech.gateway.api.PolicyImportResult;
import com.l7tech.gateway.api.PolicyValidationContext;
import com.l7tech.gateway.api.PolicyValidationResult;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceSet;
import com.l7tech.gateway.api.ServiceDetail;
import com.l7tech.gateway.api.ServiceMO;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceDocumentResolver;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.uddi.ServiceWsdlUpdateChecker;
import com.l7tech.util.Functions;
import com.l7tech.wsdl.Wsdl;
import org.springframework.transaction.PlatformTransactionManager;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 
 */
@ResourceFactory.ResourceType(type=ServiceMO.class)
public class ServiceResourceFactory extends EntityManagerResourceFactory<ServiceMO, PublishedService, ServiceHeader> {

    //- PUBLIC

    public ServiceResourceFactory( final RbacServices services,
                                   final SecurityFilter securityFilter,
                                   final PlatformTransactionManager transactionManager,
                                   final ServiceManager serviceManager,
                                   final ServiceDocumentManager serviceDocumentManager,
                                   final ServiceDocumentResolver serviceDocumentResolver,
                                   final ServiceWsdlUpdateChecker uddiServiceWsdlUpdateChecker,
                                   final PolicyHelper policyHelper ) {
        super( false, false, services, securityFilter, transactionManager, serviceManager );
        this.serviceManager = serviceManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.serviceDocumentResolver = serviceDocumentResolver;
        this.uddiServiceWsdlUpdateChecker = uddiServiceWsdlUpdateChecker;
        this.policyHelper = policyHelper;
    }

    @ResourceMethod(name="ImportPolicy", selectors=true, resource=true)
    public PolicyImportResult importPolicy( final Map<String,String> selectorMap,
                                            final PolicyImportContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return transactional( new TransactionalCallback<PolicyImportResult,ResourceFactoryException>(){
            @Override
            public PolicyImportResult execute() throws ObjectModelException, ResourceFactoryException {
                final PublishedService service = selectEntity( selectorMap );
                PolicyImportResult result = policyHelper.importPolicy( service.getPolicy(), resource );
                serviceManager.update( service );
                return result;
            }
        }, false, ResourceNotFoundException.class, InvalidResourceException.class );
    }

    @ResourceMethod(name="ExportPolicy", selectors=true)
    public PolicyExportResult exportPolicy( final Map<String,String> selectorMap ) throws ResourceNotFoundException {
        return transactional( new TransactionalCallback<PolicyExportResult,ResourceNotFoundException>(){
            @Override
            public PolicyExportResult execute() throws ObjectModelException, ResourceNotFoundException {
                final PublishedService service = selectEntity( selectorMap );
                return policyHelper.exportPolicy( service.getPolicy() );
            }
        }, true, ResourceNotFoundException.class );
    }

    @ResourceMethod(name="ValidatePolicy", selectors=true, resource=true)
    public PolicyValidationResult validatePolicy( final Map<String,String> selectorMap,
                                                  final PolicyValidationContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return transactional( new TransactionalCallback<PolicyValidationResult,ResourceFactoryException>(){
            @Override
            public PolicyValidationResult execute() throws ObjectModelException, ResourceFactoryException {
                return policyHelper.validatePolicy( resource, new PolicyHelper.PolicyResolver(){
                    private PublishedService service;

                    private PublishedService getService() throws ResourceNotFoundException {
                        PublishedService service = this.service;
                        if ( service == null ) {
                            service = this.service = selectEntity( selectorMap );
                        }
                        return service;
                    }

                    @Override
                    public Policy resolve() throws ResourceNotFoundException {
                        return getService().getPolicy();
                    }
                    @Override
                    public Wsdl resolveWsdl() throws ResourceNotFoundException {
                        try {
                            return getService().parsedWsdl();
                        } catch ( WSDLException e ) {
                            throw new ResourceAccessException("Invalid WSDL");
                        }
                    }
                } );
            }
        }, true, ResourceNotFoundException.class, InvalidResourceException.class );
    }

    //- PROTECTED

    @Override
    protected ServiceMO asResource( final EntityBag<PublishedService> entityBag ) {
        final ServiceEntityBag serviceEntityBag = cast( entityBag, ServiceEntityBag.class );
        final PublishedService publishedService = serviceEntityBag.getPublishedService();
        final Collection<ServiceDocument> serviceDocuments = serviceEntityBag.getServiceDocuments();
        final Policy policy = publishedService.getPolicy();

        final ServiceMO service = ManagedObjectFactory.createService();
        final ServiceDetail serviceDetail = ManagedObjectFactory.createServiceDetail();

        service.setId( publishedService.getId() );
        service.setServiceDetail(serviceDetail);
        final List<ResourceSet> resourceSets = new ArrayList<ResourceSet>();
        resourceSets.add( buildPolicyResourceSet( policy ) );
        if ( publishedService.isSoap() ) {
            resourceSets.add( buildWsdlResourceSet( publishedService, serviceDocuments ) );
        }
        service.setResourceSets( resourceSets );

        serviceDetail.setId( publishedService.getId() );
        serviceDetail.setVersion( publishedService.getVersion() );
        serviceDetail.setFolderId( getFolderId( publishedService ) );        
        serviceDetail.setName( publishedService.getName() );
        serviceDetail.setEnabled( !publishedService.isDisabled() );
        serviceDetail.setServiceMappings( buildServiceMappings(publishedService) );
        final Map<String,Object> properties = getProperties( publishedService, PublishedService.class );
        properties.put( "policyRevision", policy.getVersionOrdinal() );
        serviceDetail.setProperties( properties );

        return service;
    }

    @Override
    protected EntityBag<PublishedService> fromResourceAsBag( final Object resource ) throws InvalidResourceException {
        if ( !(resource instanceof ServiceMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected service");

        final ServiceMO serviceMO = (ServiceMO) resource;
        final ServiceDetail serviceDetail = serviceMO.getServiceDetail();
        final Map<String,ResourceSet> resourceSetMap = resourceHelper.getResourceSetMap( serviceMO.getResourceSets() );
        final Resource policyResource = resourceHelper.getResource( resourceSetMap, ResourceHelper.POLICY_TAG, ResourceHelper.POLICY_TYPE, true, null );
        final Collection<Resource> wsdlResources = resourceHelper.getResources( resourceSetMap, ResourceHelper.WSDL_TAG, false, new Functions.UnaryThrows<String,String, IOException>(){
            @Override
            public String call( final String url ) throws IOException {
                String resource = null;
                if ( url.toLowerCase().startsWith("http:") ||
                     url.toLowerCase().startsWith("https:") ) {
                    resource = serviceDocumentResolver.resolveWsdlTarget( url );                       
                }
                return resource;
            }
        } );
        if ( serviceDetail == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing details");
        }
                                                                        
        final PublishedService service = new PublishedService();
        final Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();

        service.setName( asName(serviceDetail.getName()) );
        service.setDisabled( !serviceDetail.getEnabled() );
        service.setRoutingUri( getRoutingUri( getServiceMapping(serviceDetail.getServiceMappings(), ServiceDetail.HttpMapping.class) ) );
        service.setHttpMethods( getHttpMethods( getServiceMapping(serviceDetail.getServiceMappings(), ServiceDetail.HttpMapping.class) ) );
        service.setLaxResolution( isLaxResolution( getServiceMapping(serviceDetail.getServiceMappings(), ServiceDetail.SoapMapping.class) ) );
        service.getPolicy().setXml( policyHelper.validatePolicySyntax(policyResource.getContent()) );
        setProperties( service, serviceDetail.getProperties(), PublishedService.class );
        addWsdl( service, serviceDocuments, wsdlResources );
        service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(serviceDocuments) );

        return new ServiceEntityBag( service, serviceDocuments );
    }

    @Override
    protected EntityBag<PublishedService> loadEntityBag( final PublishedService entity ) throws ObjectModelException {
        return new ServiceEntityBag( entity, serviceDocumentManager.findByServiceIdAndType(entity.getOid(), WSDL_IMPORT ) );
    }

    @Override
    protected void updateEntityBag( final EntityBag<PublishedService> oldEntityBag,
                                    final EntityBag<PublishedService> newEntityBag ) throws InvalidResourceException {
        final ServiceEntityBag oldServiceEntityBag = cast( oldEntityBag, ServiceEntityBag.class );
        final PublishedService oldPublishedService = oldServiceEntityBag.getPublishedService();

        final ServiceEntityBag newServiceEntityBag = cast( newEntityBag, ServiceEntityBag.class );
        final PublishedService newPublishedService = newServiceEntityBag.getPublishedService();

        oldPublishedService.setName( newPublishedService.getName() );
        oldPublishedService.setDisabled( newPublishedService.isDisabled() );
        oldPublishedService.setRoutingUri( newPublishedService.getRoutingUri() );
        oldPublishedService.setHttpMethods( newPublishedService.getHttpMethodsReadOnly() );
        oldPublishedService.setLaxResolution( newPublishedService.isLaxResolution() );
        oldPublishedService.setWssProcessingEnabled( newPublishedService.isWssProcessingEnabled() );
        oldPublishedService.getPolicy().setXml( newPublishedService.getPolicy().getXml() );

        final boolean wsdlUpdated = isWsdlUpdated(oldEntityBag, newEntityBag);
        try {
            if ( !uddiServiceWsdlUpdateChecker.isWsdlUpdatePermitted( newPublishedService ) && wsdlUpdated ) {
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES, "WSDL update not permitted" );
            }
        } catch ( UpdateException e ) {
            throw new ResourceAccessException(e);
        }

        if ( wsdlUpdated ) {
            if ( oldPublishedService.isSoap() && newPublishedService.getWsdlXml()==null ) {
                throw new InvalidResourceException( InvalidResourceException.ExceptionType.MISSING_VALUES, "WSDL is required for SOAP services." );
            }
            
            try {
                oldPublishedService.setWsdlUrl( newPublishedService.getWsdlUrl() );
            } catch ( MalformedURLException e ) {
                throw new ResourceAccessException(e); // invalid URL should have been caught earlier in fromResource...
            }
            oldPublishedService.setWsdlXml( newPublishedService.getWsdlXml() );
            oldServiceEntityBag.replaceServiceDocuments( newServiceEntityBag.getServiceDocuments() );
            oldPublishedService.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(oldServiceEntityBag.getServiceDocuments()) );
        }
    }

    @Override
    protected void beforeCreateEntity( final EntityBag<PublishedService> entityBag ) throws ObjectModelException {
        final ServiceEntityBag serviceEntityBag = cast( entityBag, ServiceEntityBag.class );
        final PublishedService service = serviceEntityBag.getPublishedService();
        final Policy policy = service.getPolicy();
                                             
        final Folder root = new Folder("Root Node", null);
        root.setOid( -5002 );
        service.setFolder( root );
        service.setInternal( false );

        if( policy != null ) {
            if ( policy.getGuid() == null ) {
                UUID guid = UUID.randomUUID();
                policy.setGuid(guid.toString());
            }

            if ( policy.getName() == null ) {
                policy.setName( service.generatePolicyName() );
            }
        }
    }

    @Override
    protected void afterCreateEntity( final EntityBag<PublishedService> entityBag, final long identifier ) throws ObjectModelException {
        final ServiceEntityBag serviceEntityBag = cast( entityBag, ServiceEntityBag.class );
        final Collection<ServiceDocument> serviceDocuments = serviceEntityBag.getServiceDocuments();

        for ( final ServiceDocument serviceDocument : serviceDocuments ) {
            serviceDocument.setOid(-1);
            serviceDocument.setServiceId(identifier);
            serviceDocumentManager.save( serviceDocument );
        }
    }

    @Override
    protected void afterUpdateEntity( final EntityBag<PublishedService> entityBag ) throws ObjectModelException {
        final ServiceEntityBag serviceEntityBag = cast( entityBag, ServiceEntityBag.class );
        final long serviceOid = serviceEntityBag.getPublishedService().getOid();

        if ( serviceEntityBag.serviceDocumentsReplaced() ) {
            final Collection<ServiceDocument> existingServiceDocuments = serviceDocumentManager.findByServiceId( serviceOid );
            for ( final ServiceDocument serviceDocument : existingServiceDocuments ) {
                serviceDocumentManager.delete(serviceDocument);
            }

            for ( final ServiceDocument serviceDocument : serviceEntityBag.getServiceDocuments() ) {
                serviceDocument.setOid(-1);
                serviceDocument.setServiceId( serviceOid );
                serviceDocumentManager.save( serviceDocument );
            }
        }
    }

    protected static class ServiceEntityBag extends EntityBag<PublishedService> {
        private boolean serviceDocumentsReplaced;
        private Collection<ServiceDocument> serviceDocuments;

        protected ServiceEntityBag( final PublishedService entity,
                                    final Collection<ServiceDocument> serviceDocuments ) {
            super( entity );
            this.serviceDocumentsReplaced = false;
            this.serviceDocuments = Collections.unmodifiableCollection( serviceDocuments );
        }

        protected void replaceServiceDocuments( final Collection<ServiceDocument> serviceDocuments ) {
            this.serviceDocumentsReplaced = true;
            this.serviceDocuments = Collections.unmodifiableCollection( serviceDocuments );
        }

        protected PublishedService getPublishedService() {
            return getEntity();
        }

        protected Collection<ServiceDocument> getServiceDocuments() {
            return serviceDocuments;
        }

        protected boolean serviceDocumentsReplaced() {
            return serviceDocumentsReplaced;
        }

        @Override
        public Iterator<PersistentEntity> iterator() {
            final List<PersistentEntity> entities = new ArrayList<PersistentEntity>();
            entities.add( getPublishedService() );
            entities.addAll( getServiceDocuments() );
            return entities.iterator();
        }
    }

    //- PRIVATE

    private static final String WSDL_IMPORT = "WSDL-IMPORT";

    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final ServiceDocumentResolver serviceDocumentResolver;
    private final ServiceWsdlUpdateChecker uddiServiceWsdlUpdateChecker;
    private final PolicyHelper policyHelper;
    private final ResourceHelper resourceHelper = new ResourceHelper();

    private List<ServiceDetail.ServiceMapping> buildServiceMappings( final PublishedService publishedService ) {
        final List<ServiceDetail.ServiceMapping> mappings = new ArrayList<ServiceDetail.ServiceMapping>();

        final ServiceDetail.HttpMapping httpMapping = ManagedObjectFactory.createHttpMapping();
        httpMapping.setUrlPattern( publishedService.getRoutingUri() );
        httpMapping.setVerbs( Functions.map( publishedService.getHttpMethodsReadOnly(), new Functions.Unary<String,HttpMethod>(){
            @Override
            public String call( final HttpMethod httpMethod ) {
                return httpMethod.name();
            }
        }) );
        mappings.add( httpMapping );

        if ( publishedService.isSoap() ) {
            ServiceDetail.SoapMapping soapMapping = ManagedObjectFactory.createSoapMapping();
            soapMapping.setLax( publishedService.isLaxResolution() );
            mappings.add( soapMapping );
        }

        return mappings;
    }

    private ResourceSet buildPolicyResourceSet( final Policy policy ) {
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.POLICY_TAG );
        final Resource resource = ManagedObjectFactory.createResource();
        resourceSet.setResources( Collections.singletonList(resource) );
        resource.setType( ResourceHelper.POLICY_TYPE );
        resource.setContent( policy.getXml() );
        if ( policy.getVersion() != Policy.DEFAULT_OID ) {
            resource.setVersion( policy.getVersion() );
        }
        return resourceSet;
    }

    private ResourceSet buildWsdlResourceSet( final PublishedService publishedService,
                                              final Collection<ServiceDocument> serviceDocuments ) {
        final ResourceSet resourceSet = ManagedObjectFactory.createResourceSet();
        resourceSet.setTag( ResourceHelper.WSDL_TAG );
        resourceSet.setRootUrl( publishedService.getWsdlUrl() );
        resourceSet.setResources( new ArrayList<Resource>() );

        final Resource resource = ManagedObjectFactory.createResource();
        resource.setType( ResourceHelper.WSDL_TYPE );
        resource.setContent( publishedService.getWsdlXml() );
        resource.setSourceUrl( publishedService.getWsdlUrl() );
        resourceSet.getResources().add( resource );

        for ( ServiceDocument serviceDocument : serviceDocuments ) {
            Resource includedResource = ManagedObjectFactory.createResource();
            includedResource.setType( resourceHelper.getType(
                    serviceDocument.getUri(), 
                    serviceDocument.getContents(),
                    ResourceHelper.WSDL_TYPE) );
            includedResource.setContent( serviceDocument.getContents() );
            includedResource.setSourceUrl( serviceDocument.getUri() );
            resourceSet.getResources().add( includedResource );
        }

        return resourceSet;
    }

    @SuppressWarnings({ "unchecked" })
    private <MT extends ServiceDetail.ServiceMapping> MT getServiceMapping( final Collection<? extends ServiceDetail.ServiceMapping> mappings,
                                                                            final Class<MT> mappingType ) throws InvalidResourceException
    {
        MT mapping = null;

        if ( mappings != null ) {
            for ( final ServiceDetail.ServiceMapping currentMapping : mappings ) {
                if ( mappingType.isInstance( currentMapping ) ) {
                    if ( mapping != null ) {
                        throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "duplicate service mapping" );
                    }
                    mapping = (MT) currentMapping;
                }
            }
        }

        return mapping;
    }


    private String getRoutingUri( final ServiceDetail.HttpMapping httpMapping ) {
        String routingUri = null;

        if ( httpMapping != null ) {
            routingUri = httpMapping.getUrlPattern();
        }

        return routingUri;
    }

    private Set<HttpMethod> getHttpMethods( final ServiceDetail.HttpMapping httpMapping ) throws InvalidResourceException {
        final Set<HttpMethod> httpMethods = EnumSet.of( HttpMethod.POST );

        if ( httpMapping != null ) {
            final Collection<String> verbs = httpMapping.getVerbs();
            if ( verbs != null ) {
                httpMethods.clear();
                for ( final String verb : verbs ) {
                    try {
                        httpMethods.add( HttpMethod.valueOf(verb) );
                    } catch ( IllegalArgumentException iae ) {
                        throw new InvalidResourceException( InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "invalid HTTP verb '"+verb+"'" );
                    }
                }
            }
        }

        return httpMethods;
    }

    private boolean isLaxResolution( final ServiceDetail.SoapMapping soapMapping ) {
        boolean lax = false;

        if ( soapMapping != null ) {
            lax = soapMapping.isLax();
        }

        return lax;
    }

    private void addWsdl( final PublishedService publishedService,
                          final Collection<ServiceDocument> serviceDocuments,
                          final Collection<Resource> wsdlResources ) throws InvalidResourceException {
        if ( wsdlResources != null ) {
            boolean first = true;
            for ( final Resource resource : wsdlResources ) {
                // performs basic initial validation check, the WSDL will be
                // parsed fully when saving the service (for the resolution parameters)
                resourceHelper.getType( resource.getSourceUrl(), resource.getContent(), ResourceHelper.WSDL_TYPE, true );

                if ( first ) {
                    first = false;
                    try {
                        publishedService.setWsdlUrl( resource.getSourceUrl() );
                    } catch ( MalformedURLException e ) {
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "invalid WSDL resource URL '"+resource.getSourceUrl()+"'");
                    }
                    publishedService.setWsdlXml( resource.getContent() );
                } else {
                    final ServiceDocument document = new ServiceDocument();
                    if ( !ResourceHelper.WSDL_TYPE.equals(resource.getType()) &&
                         !ResourceHelper.SCHEMA_TYPE.equals(resource.getType())) {
                        throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES,
                                "unexpected WSDL resource type '"+resource.getType()+"'");
                    }
                    document.setType( WSDL_IMPORT );
                    document.setContentType( "text/xml" );
                    document.setUri( resource.getSourceUrl() );
                    document.setContents( resource.getContent() );
                    serviceDocuments.add( document );
                }
            }
        }
    }

    private boolean isWsdlUpdated( final EntityBag<PublishedService> oldEntityBag,
                                   final EntityBag<PublishedService> newEntityBag ) {
        boolean updated;

        final ServiceEntityBag oldServiceEntityBag = cast( oldEntityBag, ServiceEntityBag.class );
        final PublishedService oldPublishedService = oldServiceEntityBag.getPublishedService();
        final Collection<ServiceDocument> oldServiceDocuments = oldServiceEntityBag.getServiceDocuments();

        final ServiceEntityBag newServiceEntityBag = cast( newEntityBag, ServiceEntityBag.class );
        final PublishedService newPublishedService = newServiceEntityBag.getPublishedService();
        final Collection<ServiceDocument> newServiceDocuments = newServiceEntityBag.getServiceDocuments();

        if ( oldPublishedService.getWsdlXml() != null ) {
            updated = !oldPublishedService.getWsdlXml().equals( newPublishedService.getWsdlXml() );

            if ( !updated ) {
                if ( oldPublishedService.getWsdlUrl() != null ) {
                    updated = !oldPublishedService.getWsdlUrl().equals( newPublishedService.getWsdlUrl() );
                } else {
                    updated = newPublishedService.getWsdlUrl() != null;
                }
            }

            if ( !updated ) {
                updated = oldServiceDocuments.size() != newServiceDocuments.size();
            }

            if ( !updated ) {
                final Map<String,String> oldContent = asWsdlImportMap( oldServiceDocuments );
                final Map<String,String> newContent = asWsdlImportMap( newServiceDocuments );
                updated = !oldContent.equals( newContent );
            }
        } else {
            updated = newPublishedService.getWsdlXml() != null;
        }

        return updated;
    }

    private Map<String,String> asWsdlImportMap( final Collection<ServiceDocument> serviceDocuments ) {
        Map<String,String> documents = new HashMap<String,String>();

        if ( serviceDocuments != null ) {
            for ( final ServiceDocument serviceDocument : serviceDocuments ) {
                if ( !WSDL_IMPORT.equals( serviceDocument.getType() )) continue;

                final String uri = serviceDocument.getUri();
                final String content = serviceDocument.getContents();

                documents.put( uri, content );
            }
        }

        return documents;
    }

}
