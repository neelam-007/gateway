package com.l7tech.server.service;

import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gateway.common.transport.ResolutionConfiguration;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.resolution.NonUniqueServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.sla.CounterIDManager;
import com.l7tech.server.tokenservice.SecurityTokenServiceTemplateRegistry;
import com.l7tech.server.transport.ResolutionConfigurationManager;
import com.l7tech.server.uddi.ServiceWsdlUpdateChecker;
import com.l7tech.server.uddi.UDDIHelper;
import com.l7tech.server.uddi.UDDITemplateManager;
import com.l7tech.uddi.*;
import com.l7tech.util.*;
import org.springframework.beans.factory.DisposableBean;

import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ServiceAdmin admin api.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * @author flascelles
 * @noinspection OverloadedMethodsWithSameNumberOfParameters,ValidExternallyBoundObject,NonJaxWsWebServices
 */
public final class ServiceAdminImpl implements ServiceAdmin, DisposableBean {
    private static final ServiceHeader[] EMPTY_ENTITY_HEADER_ARRAY = new ServiceHeader[0];

    private final AssertionLicense licenseManager;
    private final UDDIHelper uddiHelper;
    private final ServiceManager serviceManager;
    private final ServiceAliasManager serviceAliasManager;
    private final PolicyValidator policyValidator;
    private final SampleMessageManager sampleMessageManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final CounterIDManager counterIDManager;
    private final WspReader wspReader;
    private final UDDITemplateManager uddiTemplateManager;
    private final PolicyVersionManager policyVersionManager;
    private final ServiceTemplateManager serviceTemplateManager;
    private final ServiceDocumentResolver serviceDocumentResolver;
    private final SecurityTokenServiceTemplateRegistry tokenServiceTemplateRegistry;
    private final ServiceCache serviceCache;
    private final ResolutionConfigurationManager resolutionConfigurationManager;

    private final AsyncAdminMethodsImpl asyncSupport = new AsyncAdminMethodsImpl();
    private final ExecutorService validatorExecutor;

    private final UDDIRegistryAdmin uddiRegistryAdmin;
    private final ServiceWsdlUpdateChecker uddiServiceWsdlUpdateChecker;

    private CollectionUpdateProducer<ServiceHeader, FindException> publishedServicesUpdateProducer =
            new CollectionUpdateProducer<ServiceHeader, FindException>(5 * 60 * 1000, 100, new ServiceHeaderDifferentiator()) {
                @Override
                protected Collection<ServiceHeader> getCollection() throws FindException {
                    return serviceManager.findAllHeaders();
                }
            };

    public ServiceAdminImpl(AssertionLicense licenseManager,
                            UDDIHelper uddiHelper,
                            ServiceManager serviceManager,
                            ServiceAliasManager serviceAliasManager,
                            PolicyValidator policyValidator,
                            SampleMessageManager sampleMessageManager,
                            ServiceDocumentManager serviceDocumentManager,
                            CounterIDManager counterIDManager,
                            WspReader wspReader,
                            UDDITemplateManager uddiTemplateManager,
                            PolicyVersionManager policyVersionManager,
                            ServerConfig serverConfig,
                            ServiceTemplateManager serviceTemplateManager,
                            ServiceDocumentResolver serviceDocumentResolver,
                            UDDIRegistryAdmin uddiRegistryAdmin,
                            ServiceWsdlUpdateChecker uddiServiceWsdlUpdateChecker,
                            SecurityTokenServiceTemplateRegistry tokenServiceTemplateRegistry,
                            ServiceCache serviceCache,
                            ResolutionConfigurationManager resolutionConfigurationManager)
    {
        this.licenseManager = licenseManager;
        this.uddiHelper = uddiHelper;
        this.serviceManager = serviceManager;
        this.serviceAliasManager = serviceAliasManager;
        this.policyValidator = policyValidator;
        this.sampleMessageManager = sampleMessageManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.counterIDManager = counterIDManager;
        this.wspReader = wspReader;
        this.uddiTemplateManager = uddiTemplateManager;
        this.policyVersionManager = policyVersionManager;
        this.serviceTemplateManager = serviceTemplateManager;
        this.serviceDocumentResolver = serviceDocumentResolver;
        this.uddiRegistryAdmin = uddiRegistryAdmin;
        this.uddiServiceWsdlUpdateChecker = uddiServiceWsdlUpdateChecker;
        this.tokenServiceTemplateRegistry = tokenServiceTemplateRegistry;
        this.serviceCache = serviceCache;
        this.resolutionConfigurationManager = resolutionConfigurationManager;

        int maxConcurrency = validated(serverConfig).getIntProperty(ServerConfig.PARAM_POLICY_VALIDATION_MAX_CONCURRENCY, 15);
        BlockingQueue<Runnable> validatorQueue = new LinkedBlockingQueue<Runnable>();
        validatorExecutor = new ThreadPoolExecutor(1, maxConcurrency, 5 * 60, TimeUnit.SECONDS, validatorQueue );
    }

    @Override
    public String resolveWsdlTarget(String url) throws IOException {
        return serviceDocumentResolver.resolveWsdlTarget( url );
    }

    @Override
    public String resolveUrlTarget(String url, String maxSizeClusterProperty) throws IOException {
        return serviceDocumentResolver.resolveDocumentTarget(url, DownloadDocumentType.MOD_ASS, maxSizeClusterProperty);
    }

    @Override
    public String resolveUrlTarget(String url, DownloadDocumentType docType) throws IOException {
        return serviceDocumentResolver.resolveDocumentTarget(url, docType);
    }

    @Override
    public PublishedService findServiceByID(String serviceID) throws FindException {
        long oid = parseServiceOid(serviceID);
        PublishedService service = serviceManager.findByPrimaryKey(oid);
        if (service != null) {
            logger.finest("Returning service id " + oid + ", version " + service.getVersion());
            Policy policy = service.getPolicy();
            PolicyVersion policyVersion = policyVersionManager.findActiveVersionForPolicy(policy.getOid());
            if (policyVersion != null) {
                policy.setVersionOrdinal(policyVersion.getOrdinal());
                policy.setVersionActive(true);
            }
        }
        return service;
    }

    @Override
    public Collection<ServiceDocument> findServiceDocumentsByServiceID(String serviceID) throws FindException  {
        long oid = parseServiceOid(serviceID);
        return serviceDocumentManager.findByServiceId(oid);
    }

    @Override
    public ServiceHeader[] findAllPublishedServices() throws FindException {
        Collection<ServiceHeader> res = serviceManager.findAllHeaders();
        return collectionToHeaderArray(res);
    }

    @Override
    public ServiceHeader[] findAllPublishedServices(boolean includeAliases) throws FindException {
        Collection<ServiceHeader> res = serviceManager.findAllHeaders(includeAliases);
        return collectionToHeaderArray(res);
    }

    @Override
    public ServiceHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws FindException {
            Collection<ServiceHeader> res = serviceManager.findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
    }

    @Override
    public PublishedServiceAlias findAliasByEntityAndFolder(Long serviceOid, Long folderOid) throws FindException {
        return serviceAliasManager.findAliasByEntityAndFolder(serviceOid, folderOid);
    }

    @Override
    public CollectionUpdate<ServiceHeader> getPublishedServicesUpdate(final int oldVersionID) throws FindException {
        return publishedServicesUpdateProducer.createUpdate(oldVersionID);
    }

    @Override
    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml,
                                                       final PolicyValidationContext pvc)
    {
        final Assertion assertion;
        try {
            assertion = wspReader.parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse passed Policy XML: " + ExceptionUtils.getMessage(e), e);
        }

        return validatePolicy(assertion, pvc);
    }

    private JobId<PolicyValidatorResult> validatePolicy(final Assertion assertion, final PolicyValidationContext pvc) {
        return asyncSupport.registerJob(validatorExecutor.submit(AdminInfo.find(false).wrapCallable(new Callable<PolicyValidatorResult>() {
            @Override
            public PolicyValidatorResult call() throws Exception {
                try {
                    return policyValidator.validate(assertion, pvc, licenseManager);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Policy validation failure: " + ExceptionUtils.getMessage(e), e);
                    throw new RuntimeException(e);
                }
            }
        })), PolicyValidatorResult.class);
    }

    @Override
    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml, final PolicyValidationContext pvc, HashMap<String, Policy> fragments) {
        final Assertion assertion;
        try {
            assertion = wspReader.parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
            addPoliciesToPolicyReferenceAssertions(assertion, fragments);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse passed Policy XML: " + ExceptionUtils.getMessage(e), e);
        }

        return validatePolicy(assertion, pvc);
    }

    private void addPoliciesToPolicyReferenceAssertions(Assertion rootAssertion, HashMap<String, Policy> fragments) throws IOException {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                addPoliciesToPolicyReferenceAssertions(child, fragments);
            }
        } else if(rootAssertion instanceof PolicyReference) {
            PolicyReference policyReference = (PolicyReference)rootAssertion;
            if(fragments.containsKey(policyReference.retrievePolicyGuid())) {
                policyReference.replaceFragmentPolicy(fragments.get(policyReference.retrievePolicyGuid()));
                addPoliciesToPolicyReferenceAssertions(policyReference.retrieveFragmentPolicy().getAssertion(), fragments);
            }
        }
    }

    private static boolean isDefaultOid(PersistentEntity entity) {
        return entity.getOid() == PersistentEntity.DEFAULT_OID;
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or updated
     *
     */
    @Override
    public long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        return doSavePublishedService( service, true );
    }

    private long doSavePublishedService(PublishedService service, boolean checkWsdl)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        final Policy policy = service.getPolicy();
        if (policy != null && isDefaultOid(service) != isDefaultOid(policy))
            throw new SaveException("Unable to save new service with existing policy, or to update existing service with new policy");

        long oid;
        try {
            if (!isDefaultOid(service)) {
                // UPDATING EXISTING SERVICE
                oid = service.getOid();
                logger.fine("Updating PublishedService: " + oid);

                if ( checkWsdl ) {
                    uddiServiceWsdlUpdateChecker.isWsdlUpdatePermitted( service, true );
                }


                PublishedService previous = serviceManager.findByPrimaryKey(service.getOid());
                if (previous == null)
                    throw new UpdateException("Unable to update service: previous version not found");
                // Saving an existing published service must never change its policy xml (or folder) as a side-effect. (Bug #6405)
                if (policy != null)
                    service.setPolicy(previous.getPolicy());
                service.setFolder(previous.getFolder());
                service.setTracingEnabled(previous.isTracingEnabled());

                serviceManager.update(service);
            } else {
                // SAVING NEW SERVICE
                logger.fine("Saving new PublishedService");
                if(policy != null && policy.getGuid() == null) {
                    UUID guid = UUID.randomUUID();
                    policy.setGuid(guid.toString());
                }

                // Services may not be saved for the first time with the trace bit set.
                service.setTracingEnabled(false);

                oid = serviceManager.save(service);
                if (policy != null) {
                    policyVersionManager.checkpointPolicy(policy, true, true);
                }
                serviceManager.addManageServiceRole(service);
            }
        } catch (UpdateException e) {
            throw e;
        } catch (SaveException e) {
            throw e;
        } catch (ObjectModelException e) {
            throw new SaveException(e);
        }
        return oid;
    }

    @Override
    public long saveAlias(PublishedServiceAlias psa) throws UpdateException, SaveException, VersionException {
        long oid;
        try {
            if (psa.getOid() > 0) {
                // UPDATING EXISTING SERVICE
                oid = psa.getOid();
                logger.fine("Updating PublishedServiceAlias: " + oid);
                serviceAliasManager.update(psa);
            } else {
                // SAVING NEW SERVICE
                logger.fine("Saving new PublishedServiceAlias");
                oid = serviceAliasManager.save(psa);
            }
        } catch (UpdateException e) {
            throw e;
        } catch (SaveException e) {
            throw e;
        } catch (ObjectModelException e) {
            throw new SaveException(e);
        }
        return oid;
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or updated
     */
    @Override
    public long savePublishedServiceWithDocuments(PublishedService service, Collection<ServiceDocument> serviceDocuments)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        long oid;
        boolean newService = true;

        if (service.getOid() > 0) {
            newService = false;
        }

        service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(serviceDocuments) );
        if ( !uddiServiceWsdlUpdateChecker.isWsdlUpdatePermitted( service ) ) {
            throw new UpdateException("Cannot change the WSDL for a Published Service when its WSDL is under UDDI control");
        }
        oid = doSavePublishedService(service, false);

        try {
            Collection<ServiceDocument> existingServiceDocuments = serviceDocumentManager.findByServiceId(oid);
            for (ServiceDocument serviceDocument : existingServiceDocuments) {
                serviceDocumentManager.delete(serviceDocument);
            }
            for (ServiceDocument serviceDocument : serviceDocuments) {
                serviceDocument.setOid(-1);
                serviceDocument.setServiceId(oid);
                serviceDocumentManager.save(serviceDocument);
            }
        } catch (FindException fe) {
            String message = "Error getting service documents '"+fe.getMessage()+"'.";
            if (newService) throw new SaveException(message);
            else throw new UpdateException(message);
        } catch (DeleteException de) {
            String message = "Error removing old service document '"+de.getMessage()+"'.";
            if (newService) throw new SaveException(message);
            else throw new UpdateException(message);
        }

        return oid;
    }

    @Override
    public void setTracingEnabled(long serviceOid, boolean tracingEnabled) throws UpdateException {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceOid);
            service.setTracingEnabled(tracingEnabled);
            serviceManager.update(service);
        } catch (FindException e) {
            throw new UpdateException("Unable to find service with oid " + serviceOid + ": " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void deletePublishedService(String serviceID) throws DeleteException {
        final PublishedService service;
        try {
            long oid = parseServiceOid(serviceID);

            //Check to see if this service has any aliases
            Collection<PublishedServiceAlias> aliases = serviceAliasManager.findAllAliasesForEntity(new Long(serviceID));
            for(PublishedServiceAlias psa: aliases){
                serviceAliasManager.delete(psa);
            }
            service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            serviceManager.deleteRoles(service.getOid());
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    @Override
    public void deleteEntityAlias(String serviceID) throws DeleteException {
        final PublishedServiceAlias alias;
        try {
            long oid = parseServiceOid(serviceID);
            alias = serviceAliasManager.findByPrimaryKey(oid);
            serviceAliasManager.delete(alias);
            logger.info("Deleted PublishedServiceAlias: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private ServiceHeader[] collectionToHeaderArray(Collection<ServiceHeader> input) {
        if (input == null) return EMPTY_ENTITY_HEADER_ARRAY;
        ServiceHeader[] output = new ServiceHeader[input.size()];
        int count = 0;
        for (ServiceHeader in : input) {
            try {
                output[count] = in;
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, null, e);
                throw new RuntimeException("Collection contained something other than a EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    @Override
    public UDDINamedEntity[] findPoliciesFromUDDIRegistry( final long registryOid, final String namePattern ) throws FindException {
        final UDDIRegistry uddiRegistry = uddiRegistryAdmin.findByPrimaryKey(registryOid);
        if ( uddiRegistry == null ) throw new FindException("Invalid registry " + registryOid);

        return doWithUDDIClient( uddiRegistry, new Functions.UnaryThrows<UDDINamedEntity[],UDDIClient,UDDIException>(){
            @Override
            public UDDINamedEntity[] call( final UDDIClient uddiClient ) throws UDDIException {
                Collection<UDDINamedEntity> policies = uddiClient.listPolicies( namePattern, null );
                UDDINamedEntity[] uddiNamedEntities = policies.toArray( new UDDINamedEntity[policies.size()] );
                sort( uddiNamedEntities );
                return uddiNamedEntities;
            }
        });
    }

    private void sort( final UDDINamedEntity[] uddiNamedEntities ) {
        Arrays.sort(uddiNamedEntities, new ResolvingComparator<UDDINamedEntity,String>(new Resolver<UDDINamedEntity, String>() {
            @Override
            public String resolve(UDDINamedEntity key) {
                return key.getName();
            }
        }, false));
    }

    @Override
    public JobId<WsdlPortInfo[]> findWsdlInfosForSingleBusinessService(final long registryOid,
                                                                       final String serviceKey,
                                                                       final boolean getFirstOnly) throws FindException {
        return asyncSupport.registerJob(validatorExecutor.submit(AdminInfo.find(false).wrapCallable(new Callable<WsdlPortInfo[]>() {
            @Override
            public WsdlPortInfo[] call() throws Exception {
                final UDDIRegistry uddiRegistry = uddiRegistryAdmin.findByPrimaryKey(registryOid);
                if (uddiRegistry == null) throw new FindException("Invalid registry " + registryOid);

                return doWithUDDIClient( uddiRegistry, new Functions.UnaryThrows<WsdlPortInfo[],UDDIClient,UDDIException>(){
                    @Override
                    public WsdlPortInfo[] call( final UDDIClient uddiClient ) throws UDDIException {
                        final WsdlPortInfo[] wsdlPortInfoInfo = uddiHelper.getWsdlInfoForServiceKey(uddiClient, serviceKey, getFirstOnly);
                        for (WsdlPortInfo wsdlPortInfo : wsdlPortInfoInfo) {
                            wsdlPortInfo.setUddiRegistryOid(registryOid);
                        }
                        Arrays.sort(wsdlPortInfoInfo, new ResolvingComparator<WsdlPortInfo,String>(new Resolver<WsdlPortInfo, String>() {
                            @Override
                            public String resolve(WsdlPortInfo key) {
                                return key.getBusinessServiceKey() + "" + key.getWsdlPortName();
                            }
                        }, false));
                        return wsdlPortInfoInfo;
                    }
                } );
            }
        })), WsdlPortInfo[].class);
    }

    @Override
    public JobId<WsdlPortInfo[]> findWsdlInfosFromUDDIRegistry(final long registryOid,
                                                               final String namePattern,
                                                               final boolean caseSensitive,
                                                               final boolean getWsdlURL) {
        return asyncSupport.registerJob(validatorExecutor.submit(AdminInfo.find(false).wrapCallable(new Callable<WsdlPortInfo[]>() {
            @Override
            public WsdlPortInfo[] call() throws Exception {
                final UDDIRegistry uddiRegistry = uddiRegistryAdmin.findByPrimaryKey(registryOid);
                if (uddiRegistry == null) throw new FindException("Invalid registry " + registryOid);

                return doWithUDDIClient( uddiRegistry, new Functions.UnaryThrows<WsdlPortInfo[],UDDIClient,UDDIException>(){
                    @Override
                    public WsdlPortInfo[] call( final UDDIClient uddiClient ) throws UDDIException {
                        WsdlPortInfo[] wsdlPortInfoInfo = uddiHelper.getWsdlByServiceName(uddiClient, namePattern, caseSensitive, getWsdlURL);
                        for (WsdlPortInfo wsdlPortInfo : wsdlPortInfoInfo) {
                            wsdlPortInfo.setUddiRegistryOid(registryOid);
                        }
                        Arrays.sort(wsdlPortInfoInfo, new ResolvingComparator<WsdlPortInfo, String>(new Resolver<WsdlPortInfo, String>() {
                            @Override
                            public String resolve(WsdlPortInfo key) {
                                return key.getBusinessServiceName();
                            }
                        }, false));
                        return wsdlPortInfoInfo;
                    }
                });
            }
        })), WsdlPortInfo[].class);
    }

    @Override
    public JobId<UDDINamedEntity[]> findBusinessesFromUDDIRegistry(final long registryOid,
                                                                        final String namePattern,
                                                                        final boolean caseSensitive){
        return asyncSupport.registerJob(validatorExecutor.submit(AdminInfo.find(false).wrapCallable(new Callable<UDDINamedEntity[]>(){
            @Override
            public UDDINamedEntity[] call() throws Exception {
                final UDDIRegistry uddiRegistry = uddiRegistryAdmin.findByPrimaryKey(registryOid);
                if ( uddiRegistry == null ) throw new FindException("Invalid registry " + registryOid);

                return doWithUDDIClient( uddiRegistry, new Functions.UnaryThrows<UDDINamedEntity[],UDDIClient,UDDIException>(){
                    @Override
                    public UDDINamedEntity[] call( final UDDIClient uddiClient ) throws UDDIException {
                        UDDINamedEntity [] uddiNamedEntities = uddiHelper.getMatchingBusinesses(uddiClient, namePattern, caseSensitive);
                        sort( uddiNamedEntities );
                        return uddiNamedEntities;
                    }
                });
            }
        })), UDDINamedEntity[].class);
    }

    private FindException buildFindException(UDDIException e)  {
        String msg = "Error searching UDDI registry: "+ ExceptionUtils.getMessage(e)+".";
        if ( ExceptionUtils.causedBy( e, MalformedURLException.class ) ||
             ExceptionUtils.causedBy( e, URISyntaxException.class ) ||
             ExceptionUtils.causedBy( e, UnknownHostException.class ) ||
             ExceptionUtils.causedBy( e, ConnectException.class ) ||
             ExceptionUtils.causedBy( e, SocketTimeoutException.class ) ||
             ExceptionUtils.causedBy( e, SocketException.class )) {
            msg = msg + ". Caused by '" + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e )) + "'";
            logger.log(Level.WARNING, msg , ExceptionUtils.getDebugException( e ));
        } else {
            logger.log(Level.WARNING, msg, ExceptionUtils.getDebugException( e ));
        }
        return new FindException(msg);
    }

    private <T> T doWithUDDIClient( final UDDIRegistry uddiRegistry,
                                    final Functions.UnaryThrows<T,UDDIClient,UDDIException> callback ) throws FindException {
        UDDIClient client = null;
        try {
            client = uddiHelper.newUDDIClient( uddiRegistry );
            return callback.call( client );
        } catch ( UDDIException e ) {
            throw buildFindException(e);
        } finally {
            ResourceUtils.closeQuietly( client );
        }
    }

    @Override
    public String[] listExistingCounterNames() throws FindException {
        // get all the names for the counters
        return counterIDManager.getDistinctCounterNames();
    }

    @Override
    public SampleMessage findSampleMessageById(long oid) throws FindException {
        return sampleMessageManager.findByPrimaryKey(oid);
    }

    @Override
    public EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws FindException {
        return sampleMessageManager.findHeaders(serviceOid, operationName);
    }

    @Override
    public long saveSampleMessage(SampleMessage sm) throws SaveException {
        long oid = sm.getOid();
        if (sm.getOid() == SampleMessage.DEFAULT_OID) {
            oid = sampleMessageManager.save(sm);
        } else {
            try {
                sampleMessageManager.update(sm);
            } catch (UpdateException e) {
                throw new SaveException("Couldn't update existing SampleMessage", e.getCause());
            }
        }
        return oid;
    }

    @Override
    public void deleteSampleMessage(SampleMessage message) throws DeleteException {
        sampleMessageManager.delete(message);
    }

    @Override
    public Set<ServiceTemplate> findAllTemplates() {
        return serviceTemplateManager.findAll();
    }

    @Override
    public ServiceTemplate createSecurityTokenServiceTemplate(String wsTrustNamespace) {
        return tokenServiceTemplateRegistry.createServiceTemplate(wsTrustNamespace);
    }

    @Override
    public ResolutionReport generateResolutionReport( final PublishedService service,
                                                      final Collection<ServiceDocument> serviceDocuments ) throws FindException {
        if ( serviceDocuments != null ) {
            service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(serviceDocuments) );
        }

        final ResolutionConfiguration configuration = resolutionConfigurationManager.findByUniqueName( "Default" );
        final boolean pathRequired = configuration!= null && configuration.isPathRequired();

        final Collection<ConflictInfo> conflictInformation = new ArrayList<ConflictInfo>();
        try {
            serviceCache.checkResolution( service );
        } catch ( NonUniqueServiceResolutionException e ) {
            for ( final Long serviceOid : e.getConflictingServices() ) {
                for ( final Triple<String,String,String> parameters : e.getParameters( serviceOid ) ) {
                    conflictInformation.add( new ConflictInfo(
                        parameters.left,
                        e.getServiceName( serviceOid, true ),
                        e.getServiceName( serviceOid, false ),
                        serviceOid,
                        parameters.right,
                        parameters.middle
                    ) );
                }
            }
        } catch ( ServiceResolutionException e ) {
            throw new FindException( ExceptionUtils.getMessage(e) );
        }

        return new ResolutionReport(
                service.getRoutingUri()!=null||!pathRequired,
                conflictInformation.toArray(new ConflictInfo[conflictInformation.size()]) );
    }

    @Override
    public ResolutionReport generateResolutionReportForNewService( final PublishedService service, final Collection<ServiceDocument> serviceDocuments ) throws FindException {
        if ( service.getOid() != PublishedService.DEFAULT_OID ) throw new FindException("Service must not be persistant");
        return generateResolutionReport( service, serviceDocuments==null ? Collections.<ServiceDocument>emptyList() : serviceDocuments );
    }

    /**
     * Parse the String service ID to long (database format)
     *
     * @param serviceoid the service ID, must not be null, and .
     * @return the service ID representing <code>long</code>
     * @throws FindException if service ID is missing or invalid
     */
    private static long parseServiceOid( final String serviceoid ) throws FindException {
        if ( serviceoid == null ) {
            throw new FindException("Missing required service identifier");
        }
        try {
            return Long.parseLong(serviceoid);
        } catch ( NumberFormatException nfe ) {
            throw new FindException("Invalid service identifier '"+serviceoid+"'.");
        }
    }

    private final Logger logger = Logger.getLogger(getClass().getName());

    protected void initDao() throws Exception {
        if  (serviceManager == null) {
            throw new IllegalArgumentException("service manager is required");
        }
        if  (policyValidator == null) {
            throw new IllegalArgumentException("Policy Validator is required");
        }
    }

    @Override
    public String getPolicyURL(final String serviceoid, final boolean fullPolicyUrl) throws FindException {
        return uddiHelper.getExternalPolicyUrlForService( parseServiceOid(serviceoid), fullPolicyUrl, false);
    }

    @Override
    public String getConsumptionURL(final String serviceoid) throws FindException {
        return uddiHelper.getExternalUrlForService( parseServiceOid(serviceoid));
    }

    @Override
    public Collection<UDDIRegistryInfo> getUDDIRegistryInfo() {
        return uddiTemplateManager.getTemplatesAsUDDIRegistryInfo();
    }

    @Override
    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        return asyncSupport.getJobStatus(jobId);
    }

    @Override
    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        return asyncSupport.getJobResult(jobId);
    }

    @Override
    public <OUT extends Serializable> void cancelJob(JobId<OUT> jobId, boolean interruptIfRunning) {
        asyncSupport.cancelJob(jobId, interruptIfRunning);
    }

    @Override
    public void destroy() throws Exception {
        if (validatorExecutor != null) validatorExecutor.shutdown();
    }

    private Config validated( final Config config ) {
        final ValidatedConfig vc = new ValidatedConfig( config, logger, new Resolver<String,String>(){
            @Override
            public String resolve( final String key ) {
                return ServerConfig.getInstance().getClusterPropertyName( key );
            }
        } );

        vc.setMinimumValue( ServerConfig.PARAM_POLICY_VALIDATION_MAX_CONCURRENCY, 1 );

        return vc;
    }
}
