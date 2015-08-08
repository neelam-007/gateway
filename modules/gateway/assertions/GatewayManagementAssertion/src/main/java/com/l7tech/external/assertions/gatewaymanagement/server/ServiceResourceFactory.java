package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.gatewaymanagement.server.ResourceFactory.InvalidResourceException.ExceptionType;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.PolicyImportContext;
import com.l7tech.gateway.api.impl.PolicyValidationContext;
import com.l7tech.gateway.api.impl.VersionComment;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceDocument;
import com.l7tech.gateway.common.service.ServiceDocumentWsdlStrategy;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.SecurityFilter;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.server.service.ServiceDocumentManager;
import com.l7tech.server.service.ServiceDocumentResolver;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.uddi.ServiceWsdlUpdateChecker;
import com.l7tech.util.*;
import com.l7tech.util.Eithers.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.soap.SoapVersion;
import org.springframework.transaction.PlatformTransactionManager;

import javax.wsdl.WSDLException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;

import static com.l7tech.util.Eithers.*;
import static com.l7tech.util.Either.left;
import static com.l7tech.util.Either.right;
import static com.l7tech.util.Option.optional;
import static com.l7tech.util.Option.some;

/**
 *
 */
@ResourceFactory.ResourceType(type=ServiceMO.class)
public class ServiceResourceFactory extends SecurityZoneableEntityManagerResourceFactory<ServiceMO, PublishedService, ServiceHeader> {

    //- PUBLIC

    public static final String UNKNOWN_SOAP_VERSION = "unspecified";

    public ServiceResourceFactory( final RbacServices services,
                                   final SecurityFilter securityFilter,
                                   final PlatformTransactionManager transactionManager,
                                   final ServiceManager serviceManager,
                                   final ServiceDocumentManager serviceDocumentManager,
                                   final ServiceDocumentResolver serviceDocumentResolver,
                                   final ServiceWsdlUpdateChecker uddiServiceWsdlUpdateChecker,
                                   final PolicyHelper policyHelper,
                                   final FolderResourceFactory folderResourceFactory,
                                   final SecurityZoneManager securityZoneManager,
                                   final PolicyVersionManager policyVersionManager ) {
        super( false, false, services, securityFilter, transactionManager, serviceManager, securityZoneManager );
        this.serviceManager = serviceManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.serviceDocumentResolver = serviceDocumentResolver;
        this.uddiServiceWsdlUpdateChecker = uddiServiceWsdlUpdateChecker;
        this.policyHelper = policyHelper;
        this.folderResourceFactory = folderResourceFactory;
        this.policyVersionManager = policyVersionManager;
    }

    @ResourceMethod(name="ImportPolicy", selectors=true, resource=true)
    public PolicyImportResult importPolicy( final Map<String,String> selectorMap,
                                            final PolicyImportContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException,PolicyImportResult>>(){
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException,PolicyImportResult> execute() throws ObjectModelException {
                try {
                    final PublishedService service = selectEntity( selectorMap );
                    checkPermitted( OperationType.UPDATE, null, service );
                    policyHelper.checkPolicyAssertionAccess( service.getPolicy() );
                    PolicyImportResult result = policyHelper.importPolicy( service.getPolicy(), resource );
                    serviceManager.update( service );
                    return right2( result );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, false ) );
    }

    @ResourceMethod(name="ExportPolicy", selectors=true)
    public PolicyExportResult exportPolicy( final Map<String,String> selectorMap ) throws ResourceNotFoundException {
        return extract( transactional( new TransactionalCallback<Either<ResourceNotFoundException,PolicyExportResult>>(){
            @Override
            public Either<ResourceNotFoundException,PolicyExportResult> execute() throws ObjectModelException {
                try {
                    final PublishedService service = selectEntity( selectorMap );
                    checkPermitted(OperationType.READ, null, service);
                    return right( policyHelper.exportPolicy( service.getPolicy() ) );
                } catch ( ResourceNotFoundException e ) {
                    return left( e );
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="ValidatePolicy", selectors=true, resource=true)
    public PolicyValidationResult validatePolicy( final Map<String,String> selectorMap,
                                                  final PolicyValidationContext resource ) throws ResourceNotFoundException, InvalidResourceException {
        return extract2( transactional( new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, PolicyValidationResult>>() {
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, PolicyValidationResult> execute() throws ObjectModelException {
                checkPermittedForSomeEntity( OperationType.READ, EntityType.SERVICE );
                try {
                    return right2( policyHelper.validatePolicy( resource, new PolicyHelper.PolicyResolver() {
                        private PublishedService service;

                        private PublishedService getService() throws ResourceNotFoundException {
                            PublishedService service = this.service;
                            if ( service == null ) {
                                service = selectEntity( selectorMap );
                                checkPermitted( OperationType.READ, null, service );
                                this.service = service;
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
                                throw new ResourceAccessException( "Invalid WSDL" );
                            }
                        }

                        @Override
                        public SoapVersion resolveSoapVersion() throws ResourceNotFoundException {
                            return getService().getSoapVersion();
                        }
                    } ) );
                } catch ( ResourceNotFoundException e ) {
                    return left2_1( e );
                } catch ( InvalidResourceException e ) {
                    return left2_2( e );
                }
            }
        }, true ) );
    }

    @ResourceMethod(name="SetVersionComment", selectors=true, resource=true)
    public void setVersionComment( final Map<String,String> selectorMap,
                                   final VersionComment resource ) throws ResourceNotFoundException, InvalidResourceException {
        Eithers.extract2(transactional(new TransactionalCallback<E2<ResourceNotFoundException, InvalidResourceException, String>>() {
            @SuppressWarnings({"unchecked"})
            @Override
            public E2<ResourceNotFoundException, InvalidResourceException, String> execute() throws ObjectModelException {
                try {
                    final PublishedService service = selectEntity( selectorMap );
                    checkPermitted( OperationType.READ, null, service );
                    policyHelper.checkPolicyAssertionAccess( service.getPolicy() );

                    // update comment
                    PolicyVersion policyVersion = null;
                    if (resource.getVersionNumber() != null) {
                        policyVersion = policyVersionManager.findPolicyVersionForPolicy(service.getPolicy().getGoid(), resource.getVersionNumber());
                    } else {
                        policyVersion = policyVersionManager.findActiveVersionForPolicy(service.getPolicy().getGoid());
                    }

                    if (policyVersion == null) throw new InvalidResourceException(ExceptionType.INVALID_VALUES,"Version not found " + resource.getVersionNumber());
                    checkPermitted(OperationType.UPDATE, null, policyVersion);
                    policyVersion.setName(resource.getComment());
                    validate(policyVersion);
                    policyVersionManager.update(policyVersion);
                    return right2(policyVersion.getName());
                } catch (FindException e) {
                    return left2_1(new ResourceNotFoundException(ExceptionUtils.getMessage(e), e));
                } catch (ResourceNotFoundException e) {
                    return left2_1(e);
                } catch (InvalidResourceException e) {
                    return left2_2(e);
                }
            }
        }, false));
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
        if ( publishedService.isSoap() ) {
            properties.put( "soapVersion", soapVersionToString(publishedService.getSoapVersion()) );
        }
        //adding the service properties to the properties map
        for (final String key : publishedService.getPropertyNames()) {
            properties.put(PROPERTY_PREFIX + key, publishedService.getProperty(key));
        }
        serviceDetail.setProperties( properties );

        // handle SecurityZone
        doSecurityZoneAsResource( service, publishedService );

        return service;
    }

    public Pair<PublishedService,Collection<ServiceDocument>> fromResource(Object resource) throws InvalidResourceException {
        ServiceEntityBag bag = (ServiceEntityBag)fromResourceAsBag(resource);
        return new Pair<PublishedService,Collection<ServiceDocument>>(bag.getPublishedService(),bag.getServiceDocuments());
    }

    @Override
    public ServiceMO asResource(PublishedService entity) {
        try {
            return asResource(loadEntityBag(entity));
        } catch (ObjectModelException e) {
            return null;
        }
    }

    @Override
    public PublishedService fromResource(Object resource, boolean strict) throws InvalidResourceException {
        return fromResourceAsBag(resource, strict).getEntity();
    }

    @Override
    public EntityBag<PublishedService> fromResourceAsBag( final Object resource ) throws InvalidResourceException {
        return fromResourceAsBag(resource, true);
    }

    public EntityBag<PublishedService> fromResourceAsBag( final Object resource, boolean strict ) throws InvalidResourceException {
        if ( !(resource instanceof ServiceMO) )
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.UNEXPECTED_TYPE, "expected service");

        final ServiceMO serviceMO = (ServiceMO) resource;
        final ServiceDetail serviceDetail = serviceMO.getServiceDetail();
        if ( serviceDetail == null ) {
            throw new InvalidResourceException(InvalidResourceException.ExceptionType.MISSING_VALUES, "missing details");
        }
        Option<Folder> folder = folderResourceFactory.getFolder( optional( serviceDetail.getFolderId() ) );
        if ( !folder.isSome() ) {
            if(strict) {
                throw new InvalidResourceException( ExceptionType.INVALID_VALUES, "Folder not found");
            } else {
                Folder folderParent = new Folder();
                folderParent.setId(optional(serviceDetail.getFolderId()).orSome(Folder.ROOT_FOLDER_ID.toString()));
                folder = some(folderParent);
            }
        }
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
        final Collection<Resource> documentResources = resourceHelper.getResources(resourceSetMap, ResourceHelper.RESTFUL_TAG, false, null);

        final PublishedService service = new PublishedService();
        final Collection<ServiceDocument> serviceDocuments = new ArrayList<ServiceDocument>();

        service.setName( asName(serviceDetail.getName()) );
        service.setDisabled( !serviceDetail.getEnabled() );
        service.setFolder( folder.some() );
        service.setRoutingUri( getRoutingUri( getServiceMapping(serviceDetail.getServiceMappings(), ServiceDetail.HttpMapping.class) ) );
        service.setHttpMethods( getHttpMethods( getServiceMapping(serviceDetail.getServiceMappings(), ServiceDetail.HttpMapping.class) ) );
        service.setLaxResolution( isLaxResolution( getServiceMapping(serviceDetail.getServiceMappings(), ServiceDetail.SoapMapping.class) ) );
        service.getPolicy().setXml( policyHelper.validatePolicySyntax(policyResource.getContent()) );
        //getting the properties prefixed with properties and adding them as service properties
        if(serviceDetail.getProperties() != null) {
            for (final Map.Entry<String, Object> property : serviceDetail.getProperties().entrySet()) {
                if (property.getKey().startsWith(PROPERTY_PREFIX)) {
                    service.putProperty(property.getKey().substring(PROPERTY_PREFIX.length()), property.getValue().toString());
                }
            }
        }
        setProperties( service, serviceDetail.getProperties(), PublishedService.class );
        setSoapVersion( service, getProperty(serviceDetail.getProperties(), "soapVersion", Option.<String>none(), String.class) );
        if(service.isSoap()) {
            addWsdl(service, serviceDocuments, wsdlResources);
            service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(serviceDocuments) );
        }
        else {
            for(Resource documentResource : documentResources) {
                addServiceDocument(serviceDocuments, documentResource);
            }
        }

        // handle SecurityZone
        doSecurityZoneFromResource( serviceMO, service, strict );

        return new ServiceEntityBag( service, serviceDocuments );
    }

    private void addServiceDocument(Collection<ServiceDocument> serviceDocuments, Resource documentResource) {
        ServiceDocument document = new ServiceDocument();
        document.setType(documentResource.getType());
        switch (document.getType()) {
            case ResourceHelper.SWAGGER_JSON_TYPE:
                document.setContentType("application/json");
                break;
            case ResourceHelper.SWAGGER_YAML_TYPE:
                document.setContentType("text/plain");
                break;
            default:
                document.setContentType("application/octet-stream");
        }

        document.setUri(documentResource.getSourceUrl());
        document.setContents(documentResource.getContent());

        serviceDocuments.add(document);
    }

    @Override
    protected EntityBag<PublishedService> loadEntityBag( final PublishedService entity ) throws ObjectModelException {
        //TODO: determine the type of the service

        return new ServiceEntityBag( entity, serviceDocumentManager.findByServiceIdAndType(entity.getGoid(), WSDL_IMPORT ) );
    }

    @Override
    protected void updateEntityBag( final EntityBag<PublishedService> oldEntityBag,
                                    final EntityBag<PublishedService> newEntityBag ) throws InvalidResourceException {
        final ServiceEntityBag oldServiceEntityBag = cast( oldEntityBag, ServiceEntityBag.class );
        final PublishedService oldPublishedService = oldServiceEntityBag.getPublishedService();

        final ServiceEntityBag newServiceEntityBag = cast( newEntityBag, ServiceEntityBag.class );
        final PublishedService newPublishedService = newServiceEntityBag.getPublishedService();

        oldPublishedService.setFolder( folderResourceFactory.checkMovePermitted( oldPublishedService.getFolder(), newPublishedService.getFolder() ) );
        oldPublishedService.setName( newPublishedService.getName() );
        oldPublishedService.setDisabled( newPublishedService.isDisabled() );
        oldPublishedService.setRoutingUri( newPublishedService.getRoutingUri() );
        oldPublishedService.setHttpMethods( newPublishedService.getHttpMethodsReadOnly() );
        oldPublishedService.setLaxResolution( newPublishedService.isLaxResolution() );
        oldPublishedService.setWssProcessingEnabled( newPublishedService.isWssProcessingEnabled() );
        oldPublishedService.setSecurityZone( newPublishedService.getSecurityZone() );
        if ( newPublishedService.soapVersionSet() ) {
            oldPublishedService.setSoapVersion( newPublishedService.getSoapVersion() );
        }
        oldPublishedService.setProperties(newPublishedService.getProperties());
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

        try {
            policyHelper.checkPolicyAssertionAccess( policy );
        } catch (InvalidResourceException e) {
            throw new SaveException(e);
        }

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
    protected void afterCreateEntity( final EntityBag<PublishedService> entityBag, final Goid identifier ) throws ObjectModelException {
        final ServiceEntityBag serviceEntityBag = cast( entityBag, ServiceEntityBag.class );
        final Collection<ServiceDocument> serviceDocuments = serviceEntityBag.getServiceDocuments();

        for ( final ServiceDocument serviceDocument : serviceDocuments ) {
            serviceDocument.setGoid( ServiceDocument.DEFAULT_GOID );
            serviceDocument.setServiceId(identifier);
            serviceDocumentManager.save( serviceDocument );
        }
    }

    @Override
    protected void afterUpdateEntity( final EntityBag<PublishedService> entityBag ) throws ObjectModelException {
        final ServiceEntityBag serviceEntityBag = cast( entityBag, ServiceEntityBag.class );
        final Goid serviceGoid = serviceEntityBag.getPublishedService().getGoid();

        if ( serviceEntityBag.serviceDocumentsReplaced() ) {
            final Collection<ServiceDocument> existingServiceDocuments = serviceDocumentManager.findByServiceId( serviceGoid );
            for ( final ServiceDocument serviceDocument : existingServiceDocuments ) {
                serviceDocumentManager.delete(serviceDocument);
            }

            for ( final ServiceDocument serviceDocument : serviceEntityBag.getServiceDocuments() ) {
                serviceDocument.setGoid( ServiceDocument.DEFAULT_GOID );
                serviceDocument.setServiceId( serviceGoid );
                serviceDocumentManager.save( serviceDocument );
            }
        }
    }

    public static class ServiceEntityBag extends EntityBag<PublishedService> {
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

        public Collection<ServiceDocument> getServiceDocuments() {
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
    private static final String PROPERTY_PREFIX = "property.";

    private final ServiceManager serviceManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final ServiceDocumentResolver serviceDocumentResolver;
    private final ServiceWsdlUpdateChecker uddiServiceWsdlUpdateChecker;
    private final PolicyHelper policyHelper;
    private final FolderResourceFactory folderResourceFactory;
    private final ResourceHelper resourceHelper = new ResourceHelper();
    private final PolicyVersionManager policyVersionManager;

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
        try {
            // Recreate the policy xml instead of using the existing one so that all ID's will be proper and fully updated.
            // ID's could be incorrect in the stored xml in the case of a policy that was saves pre 8.0 (Goid update) SSG-8854
            resource.setContent( WspWriter.getPolicyXml(policy.getAssertion()) );
        } catch (IOException e) {
            throw new ResourceAccessException( "Could not retrieve policy xml", e );
        }
        if (policy.getVersion() != -1 ) {
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

    private void setSoapVersion( final PublishedService service,
                                 final Option<String> soapVersionText ) throws InvalidResourceException {
        if ( service.isSoap() ) {
            final SoapVersion soapVersion = soapVersionFromString( soapVersionText.map( TextUtils.trim() ) );
            if ( soapVersion != null ) {
                service.setSoapVersion( soapVersion );
            }
        }
    }

    private String soapVersionToString( final SoapVersion soapVersion ) {
        String soapVersionText = soapVersion != null ? soapVersion.getVersionNumber() : "";
        if ( soapVersionText.isEmpty() ) {
            soapVersionText = UNKNOWN_SOAP_VERSION;
        }
        return soapVersionText;
    }

    private SoapVersion soapVersionFromString( final Option<String> soapVersionText ) throws InvalidResourceException {
        SoapVersion soapVersion = null;

        if ( soapVersionText.isSome() ) {
            if ( UNKNOWN_SOAP_VERSION.equals( soapVersionText.some() ) ) {
                soapVersion = SoapVersion.UNKNOWN;
            } else {
                for ( final SoapVersion candidateVersion : SoapVersion.values() ) {
                    if ( !candidateVersion.getVersionNumber().isEmpty() &&
                         candidateVersion.getVersionNumber().equals( soapVersionText.some() ) ) {
                        soapVersion = candidateVersion;
                        break;
                    }
                }
            }

            if ( soapVersion == null ) {
                throw new InvalidResourceException(InvalidResourceException.ExceptionType.INVALID_VALUES, "unknown soapVersion");
            }
        }

        return soapVersion;
    }
}
