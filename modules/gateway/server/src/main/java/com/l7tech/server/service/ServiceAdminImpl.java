package com.l7tech.server.service;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.gateway.common.AsyncAdminMethodsImpl;
import com.l7tech.gateway.common.admin.UDDIRegistryAdmin;
import com.l7tech.gateway.common.uddi.UDDIRegistry;
import com.l7tech.gateway.common.uddi.UDDIProxiedService;
import com.l7tech.gateway.common.service.*;
import com.l7tech.objectmodel.*;
import static com.l7tech.objectmodel.EntityType.SERVICE;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.sla.CounterIDManager;
import com.l7tech.server.uddi.UDDITemplateManager;
import com.l7tech.server.uddi.UDDIHelper;
import com.l7tech.uddi.*;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
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
 * @author flascelles<br/>
 * @noinspection OverloadedMethodsWithSameNumberOfParameters,ValidExternallyBoundObject,NonJaxWsWebServices
 */
public final class ServiceAdminImpl implements ServiceAdmin, DisposableBean {
    private static final ServiceHeader[] EMPTY_ENTITY_HEADER_ARRAY = new ServiceHeader[0];

    private SSLContext sslContext;

    private final AssertionLicense licenseManager;
    private final UDDIHelper uddiHelper;
    private final ServiceManager serviceManager;
    private final ServiceAliasManager serviceAliasManager;
    private final PolicyValidator policyValidator;
    private final SampleMessageManager sampleMessageManager;
    private final ServiceDocumentManager serviceDocumentManager;
    private final CounterIDManager counterIDManager;
    private final X509TrustManager trustManager;
    private final RoleManager roleManager;
    private final WspReader wspReader;
    private final UDDITemplateManager uddiTemplateManager;
    private final PolicyVersionManager policyVersionManager;
    private final ServiceTemplateManager serviceTemplateManager;

    private final AsyncAdminMethodsImpl asyncSupport = new AsyncAdminMethodsImpl();
    private final ExecutorService validatorExecutor;

    private final UDDIRegistryAdmin uddiRegistryAdmin;

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
                            X509TrustManager trustManager,
                            RoleManager roleManager,
                            WspReader wspReader,
                            UDDITemplateManager uddiTemplateManager,
                            PolicyVersionManager policyVersionManager,
                            ServerConfig serverConfig,
                            ServiceTemplateManager serviceTemplateManager, UDDIRegistryAdmin uddiRegistryAdmin)
    {
        this.licenseManager = licenseManager;
        this.uddiHelper = uddiHelper;
        this.serviceManager = serviceManager;
        this.serviceAliasManager = serviceAliasManager;
        this.policyValidator = policyValidator;
        this.sampleMessageManager = sampleMessageManager;
        this.serviceDocumentManager = serviceDocumentManager;
        this.counterIDManager = counterIDManager;
        this.trustManager = trustManager;
        this.roleManager = roleManager;
        this.wspReader = wspReader;
        this.uddiTemplateManager = uddiTemplateManager;
        this.policyVersionManager = policyVersionManager;
        this.serviceTemplateManager = serviceTemplateManager;
        this.uddiRegistryAdmin = uddiRegistryAdmin;

        int maxConcurrency = serverConfig.getIntProperty(ServerConfig.PARAM_POLICY_VALIDATION_MAX_CONCURRENCY, 15);
        BlockingQueue<Runnable> validatorQueue = new LinkedBlockingQueue<Runnable>();
        validatorExecutor = new ThreadPoolExecutor(1, maxConcurrency, 5 * 60, TimeUnit.SECONDS, validatorQueue );
    }

    @Override
    public String resolveWsdlTarget(String url) throws IOException {
        GetMethod get = null;
        try {
            URL urltarget = new URL(url);
            HttpClient client = new HttpClient();
            HttpClientParams clientParams = client.getParams();
            HostConfiguration hconf = getHostConfigurationWithTrustManager(urltarget);
            // bugfix for 1857 (next 3 lines)
            clientParams.setVersion( HttpVersion.HTTP_1_1);
            StringBuilder hostval = new StringBuilder(urltarget.getHost());
            if (urltarget.getPort() > 0)
                hostval.append(':').append(urltarget.getPort());
            get = hconf == null ? new GetMethod(url) : new GetMethod(urltarget.getFile());
            get.setRequestHeader("HOST", hostval.toString());

            // support for passing username and password in the url from the ssm
            String userinfo = urltarget.getUserInfo();
            if (userinfo != null && userinfo.indexOf(':') > -1) {
                String login = userinfo.substring(0, userinfo.indexOf(':'));
                String passwd = userinfo.substring(userinfo.indexOf(':')+1, userinfo.length());
                HttpState state = client.getState();
                get.setDoAuthentication(true);
                clientParams.setAuthenticationPreemptive(true);
                state.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(login, passwd));
            }

            int ret;
            if (hconf != null) {
                ret = client.executeMethod(hconf, get);
            } else {
                ret = client.executeMethod(get);
            }
            if (ret == 200) {
                //noinspection IOResourceOpenedButNotSafelyClosed
                byte[] body = IOUtils.slurpStream(new ByteOrderMarkInputStream(new ByteLimitInputStream(get.getResponseBodyAsStream(), 16, 10*1024*1024)));
                String charset = get.getResponseCharSet();
                return new String(body, charset);
            } else {
                String msg = "The URL '" + url + "' is returning status code " + ret;
                throw new IOException(msg);
            }
        } catch ( HttpException e) {
            String msg = "Http error getting " + url;
            IOException ioe =new  IOException(msg);
            ioe.initCause(e);
            throw ioe;
        } finally {
            if (get != null) {
                get.releaseConnection();
            }
        }
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
                                                       final PolicyType policyType,
                                                       final boolean soap,
                                                       final Wsdl wsdl)
    {
        final Assertion assertion;
        try {
            assertion = wspReader.parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse passed Policy XML: " + ExceptionUtils.getMessage(e), e);
        }

        return validatePolicy(assertion, policyType, soap, wsdl);
    }

    private JobId<PolicyValidatorResult> validatePolicy(final Assertion assertion, final PolicyType policyType, final boolean soap, final Wsdl wsdl) {
        return asyncSupport.registerJob(validatorExecutor.submit(AdminInfo.find(false).wrapCallable(new Callable<PolicyValidatorResult>() {
            @Override
            public PolicyValidatorResult call() throws Exception {
                try {
                    return policyValidator.validate(assertion, policyType, wsdl, soap, licenseManager);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Policy validation failure: " + ExceptionUtils.getMessage(e), e);
                    throw new RuntimeException(e);
                }
            }
        })), PolicyValidatorResult.class);
    }

    @Override
    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml, final PolicyType policyType, final boolean soap, Wsdl wsdl, HashMap<String, Policy> fragments) {
        final Assertion assertion;
        try {
            assertion = wspReader.parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
            addPoliciesToPolicyReferenceAssertions(assertion, fragments);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse passed Policy XML: " + ExceptionUtils.getMessage(e), e);
        }

        return validatePolicy(assertion, policyType, soap, wsdl);
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
     * @param service the object to be saved or upodated
     *
     */
    @Override
    public long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        final Policy policy = service.getPolicy();
        if (policy != null && isDefaultOid(service) != isDefaultOid(policy))
            throw new SaveException("Unable to save new service with existing policy, or to update existing service with new policy");

        long oid;
        try {
            if (!isDefaultOid(service)) {
                // UPDATING EXISTING SERVICE
                final boolean wsdlXmlChanged = service.isParsedWsdlNull();

                oid = service.getOid();
                logger.fine("Updating PublishedService: " + oid);

                if (policy != null) {
                    // Saving an existing published service must never change its policy xml (or folder) as a side-effect. (Bug #6405)
                    PublishedService previous = serviceManager.findByPrimaryKey(service.getOid());
                    if (previous != null) {
                        service.setFolder(previous.getFolder());
                        service.setPolicy(previous.getPolicy());
                    }
                }

                serviceManager.update(service);
                //check if UDDI needs to be updated
                if(wsdlXmlChanged) checkUpdateUDDI(service);
            } else {
                // SAVING NEW SERVICE
                logger.fine("Saving new PublishedService");
                if(policy != null && policy.getGuid() == null) {
                    UUID guid = UUID.randomUUID();
                    policy.setGuid(guid.toString());
                }
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

    /**
     * Check if this service has published a proxied BusinessService to UDDI. If so update it following an update
     * to the WSDL xml of the service
     * @param service
     */
    private void checkUpdateUDDI(PublishedService service){
        try {
            final UDDIProxiedService uddiProxiedService = uddiRegistryAdmin.getUDDIProxiedService(service.getOid());
            if(uddiProxiedService == null) return;//nothing published for this service

            //check it's configured for updates
            if(!uddiProxiedService.isUpdateProxyOnLocalChange()) return;

            logger.log(Level.INFO, "Updating Gateway WSDL in UDDI");
            uddiRegistryAdmin.publishGatewayWsdl(uddiProxiedService);

            logger.log(Level.INFO, "Gateway WSDL in UDDI has been updated");

        } catch (FindException e) {
            logger.log(Level.WARNING, "Could not look up if this service '" + service.getOid()+"' has published to UDDI");
        } catch (UDDIRegistryAdmin.PublishProxiedServiceException e) {
            logger.log(Level.WARNING, "Could not update UDDI following update to Published Service's WSDL. Serivce oid: " + service.getOid(), e);
        } catch (VersionException e) {
            logger.log(Level.WARNING, "Version exception trying to save UDDIProxiedService following update UDDI. Serivce oid: " + service.getOid(), e);
        } catch (SaveException e) {
            logger.log(Level.WARNING, "Could not save UDDIProxiedService following update UDDI. Serivce oid: " + service.getOid(), e);
            //this should not happen, as this is always an update
        } catch (UpdateException e) {
            logger.log(Level.WARNING, "Could not update UDDIProxiedService following update UDDI. Serivce oid: " + service.getOid(), e);
        } catch (UDDIRegistryAdmin.UDDIRegistryNotEnabledException e) {
            logger.log(Level.WARNING, "Could not update UDDIProxiedService following update UDDI. Serivce oid: " + service.getOid(), e);
        }
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
     * @param service the object to be saved or upodated
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
        oid = savePublishedService(service);

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
            roleManager.deleteEntitySpecificRoles(SERVICE, service.getOid());
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
    public UDDINamedEntity[] findBusinessesFromUDDIRegistry(final long registryOid, String namePattern, boolean caseSensitive) throws FindException {
        try {
            final UDDIRegistry uddiRegistry = uddiRegistryAdmin.findByPrimaryKey(registryOid);
            UDDINamedEntity [] uddiNamedEntities = uddiHelper.getMatchingBusinesses(getUDDIClient(uddiRegistry), namePattern, caseSensitive);
            //noinspection unchecked
            Arrays.sort(uddiNamedEntities, new ResolvingComparator(new Resolver<UDDINamedEntity, String>() {

                @Override
                public String resolve(UDDINamedEntity key) {
                    return key.getName();
                }
            }, false));
            return uddiNamedEntities;
        } catch (UDDIException e) {
            String msg = "Error searching UDDI registry '"+ExceptionUtils.getMessage(e)+"'";
            if ( ExceptionUtils.causedBy( e, MalformedURLException.class ) ||
                 ExceptionUtils.causedBy( e, URISyntaxException.class ) ||
                 ExceptionUtils.causedBy( e, UnknownHostException.class ) ||
                 ExceptionUtils.causedBy( e, ConnectException.class ) ||
                 ExceptionUtils.causedBy( e, NoRouteToHostException.class )) {
                logger.log(Level.WARNING, msg + " : '" + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e ))+ "'", ExceptionUtils.getDebugException( e ));
            } else {
                logger.log(Level.WARNING, msg, e);
            }
            throw new FindException(msg);
        }
    }

    @Override
    public WsdlPortInfo[] findWsdlUrlsFromUDDIRegistry(final long registryOid, String namePattern, boolean caseSensitive) throws FindException {
        try {
            final UDDIRegistry uddiRegistry = uddiRegistryAdmin.findByPrimaryKey(registryOid);
            WsdlPortInfo[] wsdlPortInfoInfo = uddiHelper.getWsdlByServiceName(getUDDIClient(uddiRegistry), namePattern, caseSensitive);
            for(WsdlPortInfo wsdlPortInfo: wsdlPortInfoInfo){
                wsdlPortInfo.setUddiRegistryOid(registryOid);
            }
            //noinspection unchecked
            Arrays.sort(wsdlPortInfoInfo, new ResolvingComparator(new Resolver<WsdlPortInfo,String>(){
                @Override
                public String resolve(WsdlPortInfo key) {
                    return key.getBusinessServiceKey() + "" + key.getWsdlPortName();
                }
            }, false));
            return wsdlPortInfoInfo;
        } catch (UDDIException e) {
            String msg = "Error searching UDDI registry '"+ExceptionUtils.getMessage(e)+"'";
            if ( ExceptionUtils.causedBy( e, MalformedURLException.class ) ||
                 ExceptionUtils.causedBy( e, URISyntaxException.class ) ||
                 ExceptionUtils.causedBy( e, UnknownHostException.class ) ||
                 ExceptionUtils.causedBy( e, ConnectException.class ) ||
                 ExceptionUtils.causedBy( e, NoRouteToHostException.class )) {
                logger.log(Level.WARNING, msg + " : '" + ExceptionUtils.getMessage(ExceptionUtils.unnestToRoot(e ))+ "'", ExceptionUtils.getDebugException( e ));
            } else {
                logger.log(Level.WARNING, msg, e);
            }
            throw new FindException(msg);
        }
    }

    private UDDIClient getUDDIClient(final UDDIRegistry uddiRegistry) {
        return UDDIHelper.newUDDIClient( uddiRegistry );
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

    private synchronized SSLContext getSSLContext() {
        if (sslContext == null) {
            try {
                sslContext = SSLContext.getInstance("SSL");
                sslContext.init(null, new TrustManager[]{trustManager}, null);
            } catch (NoSuchAlgorithmException e) {
                logger.log(Level.SEVERE, "Unable to get sslcontext", e);
                throw new RuntimeException(e);
            } catch (KeyManagementException e) {
                logger.log(Level.SEVERE, "Unable to get sslcontext", e);
                throw new RuntimeException(e);
            }
        }
        return sslContext;
    }

    private HostConfiguration getHostConfigurationWithTrustManager(URL url) {
        HostConfiguration hconf = null;
        if ("https".equals(url.getProtocol())) {
            final int fport = url.getPort() == -1 ? 443 : url.getPort();
            hconf = new HostConfiguration();
            Protocol protocol = new Protocol(url.getProtocol(), (ProtocolSocketFactory) new SecureProtocolSocketFactory() {
                @Override
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port, clientAddress, clientPort);
                }

                @Override
                public Socket createSocket(String host, int port) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port);
                }

                @Override
                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort, HttpConnectionParams httpConnectionParams) throws IOException {
                    Socket socket = getSSLContext().getSocketFactory().createSocket();
                    int connectTimeout = httpConnectionParams.getConnectionTimeout();

                    socket.bind(new InetSocketAddress(clientAddress, clientPort));

                    try {
                        socket.connect(new InetSocketAddress(host, port), connectTimeout);
                    }
                    catch(SocketTimeoutException ste) {
                        throw new ConnectTimeoutException("Timeout when connecting to host '"+host+"'.", ste);
                    }

                    return socket;
                }
            }, fport);
            hconf.setHost(url.getHost(), fport, protocol);
        }
        return hconf;
    }

    @Override
    public String getPolicyURL(final String serviceoid, final boolean fullPolicyUrl) throws FindException {
        return uddiHelper.getExternalPolicyUrlForService( parseServiceOid(serviceoid), fullPolicyUrl);
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
    public void destroy() throws Exception {
        if (validatorExecutor != null) validatorExecutor.shutdown();
    }
}
