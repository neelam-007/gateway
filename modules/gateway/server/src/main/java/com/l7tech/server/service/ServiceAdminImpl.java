package com.l7tech.server.service;

import com.l7tech.common.io.ByteLimitInputStream;
import com.l7tech.common.io.IOUtils;
import com.l7tech.gateway.common.AsyncAdminMethodsImpl;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gateway.common.audit.SystemMessages;
import static com.l7tech.gateway.common.security.rbac.EntityType.SERVICE;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.PolicyReference;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.uddi.UddiAgentFactory;
import com.l7tech.server.sla.CounterIDManager;
import com.l7tech.server.uddi.RegistryPublicationManager;
import com.l7tech.server.uddi.UDDITemplateManager;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.uddi.WsdlInfo;
import com.l7tech.uddi.UddiAgent;
import com.l7tech.uddi.UddiAgentException;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;

import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.ConnectTimeoutException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

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
import java.text.MessageFormat;

/**
 * Server side implementation of the ServiceAdmin admin api.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 6, 2003
 * @noinspection OverloadedMethodsWithSameNumberOfParameters,ValidExternallyBoundObject,NonJaxWsWebServices
 */
public final class ServiceAdminImpl implements ServiceAdmin, ApplicationContextAware {
    private static final ServiceHeader[] EMPTY_ENTITY_HEADER_ARRAY = new ServiceHeader[0];

    private SSLContext sslContext;

    private final AssertionLicense licenseManager;
    private final RegistryPublicationManager registryPublicationManager;
    private final UddiAgentFactory uddiAgentFactory;
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
    private final BlockingQueue<Runnable> validatorQueue = new LinkedBlockingQueue<Runnable>();
    private final ExecutorService validatorExecutor;
    private Auditor auditor;

    private CollectionUpdateProducer<ServiceHeader, FindException> publishedServicesUpdateProducer =
            new CollectionUpdateProducer<ServiceHeader, FindException>(5 * 60 * 1000, 100, new ServiceHeaderDifferentiator()) {
                protected Collection<ServiceHeader> getCollection() throws FindException {
                    return serviceManager.findAllHeaders();
                }
            };

    public ServiceAdminImpl(AssertionLicense licenseManager,
                            RegistryPublicationManager registryPublicationManager,
                            UddiAgentFactory uddiAgentFactory,
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
                            ServiceTemplateManager serviceTemplateManager)
    {
        this.licenseManager = licenseManager;
        this.registryPublicationManager = registryPublicationManager;
        this.uddiAgentFactory = uddiAgentFactory;
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

        int maxConcurrency = serverConfig.getIntProperty(ServerConfig.PARAM_POLICY_VALIDATION_MAX_CONCURRENCY, 15);
        validatorExecutor = new ThreadPoolExecutor(1, maxConcurrency, 5 * 60, TimeUnit.SECONDS, validatorQueue);
    }

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
                byte[] body = IOUtils.slurpStream(new ByteLimitInputStream(get.getResponseBodyAsStream(), 16, 10*1024*1024));
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

    public PublishedService findServiceByID(String serviceID) throws FindException {
        long oid = toLong(serviceID);
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

    public Collection<ServiceDocument> findServiceDocumentsByServiceID(String serviceID) throws FindException  {
        long oid = toLong(serviceID);
        return serviceDocumentManager.findByServiceId(oid);
    }

    public ServiceHeader[] findAllPublishedServices() throws FindException {
        Collection<ServiceHeader> res = serviceManager.findAllHeaders();
        return collectionToHeaderArray(res);
    }

    public ServiceHeader[] findAllPublishedServices(boolean includeAliases) throws FindException {
        Collection<ServiceHeader> res = serviceManager.findAllHeaders(includeAliases);
        return collectionToHeaderArray(res);
    }

    public ServiceHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws FindException {
            Collection<ServiceHeader> res = serviceManager.findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
    }

    public PublishedServiceAlias findAliasByEntityAndFolder(Long serviceOid, Long folderOid) throws FindException {
        return serviceAliasManager.findAliasByEntityAndFolder(serviceOid, folderOid);
    }

    public CollectionUpdate<ServiceHeader> getPublishedServicesUpdate(final int oldVersionID) throws FindException {
        return publishedServicesUpdateProducer.createUpdate(oldVersionID);
    }

    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml,
                                                       final PolicyType policyType,
                                                       final boolean soap,
                                                       final Wsdl wsdl)
    {
        final Assertion assertion;
        try {
            assertion = wspReader.parsePermissively(policyXml);
        } catch (IOException e) {
            throw new RuntimeException("Cannot parse passed Policy XML: " + ExceptionUtils.getMessage(e), e);
        }

        return validatePolicy(assertion, policyType, soap, wsdl);
    }

    private JobId<PolicyValidatorResult> validatePolicy(final Assertion assertion, final PolicyType policyType, final boolean soap, final Wsdl wsdl) {
        return asyncSupport.registerJob(validatorExecutor.submit(AdminInfo.find().wrapCallable(new Callable<PolicyValidatorResult>() {
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

    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml, final PolicyType policyType, final boolean soap, Wsdl wsdl, HashMap<String, Policy> fragments) {
        final Assertion assertion;
        try {
            assertion = wspReader.parsePermissively(policyXml);
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
    public long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        final Policy policy = service.getPolicy();
        if (policy != null && isDefaultOid(service) != isDefaultOid(policy))
            throw new SaveException("Unable to save new service with existing policy, or to update existing service with new policy");

        long oid;
        try {
            if (service.getOid() > 0) {
                // UPDATING EXISTING SERVICE
                oid = service.getOid();
                logger.fine("Updating PublishedService: " + oid);
                serviceManager.update(service);
                if (policy != null) {
                    PolicyVersion ver = policyVersionManager.checkpointPolicy(policy, true, false);
                    auditor.logAndAudit(SystemMessages.POLICY_VERSION_ACTIVATION, Long.toString(ver.getOrdinal()), Long.toString(policy.getOid()));
                }
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

    public long saveAlias(PublishedServiceAlias psa) throws UpdateException, SaveException, VersionException, PolicyAssertionException, IllegalStateException {
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
    public long savePublishedServiceWithDocuments(PublishedService service, Collection<ServiceDocument> serviceDocuments)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        long oid;
        boolean newService = true;

        if (service.getOid() > 0) {
            newService = false;
        }

        service.parseWsdlStrategy(new SafeWsdlPublishedService(serviceDocuments));
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

    public void deletePublishedService(String serviceID) throws DeleteException {
        final PublishedService service;
        try {
            //Check to see if this service has any aliases
            Collection<PublishedServiceAlias> aliases = serviceAliasManager.findAllAliasesForEntity(new Long(serviceID));
            for(PublishedServiceAlias psa: aliases){
                serviceAliasManager.delete(psa);
            }
            long oid = toLong(serviceID);
            service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            roleManager.deleteEntitySpecificRole(SERVICE, service.getOid());
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    public void deleteEntityAlias(String serviceID) throws DeleteException {
        final PublishedServiceAlias alias;
        try {
            long oid = toLong(serviceID);
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

    public String[] findUDDIRegistryURLs() throws FindException {
        Properties uddiProps;
        try {
            uddiProps = uddiAgentFactory.getUddiProperties();
        } catch ( UddiAgentException uae) {
            throw new FindException(uae.getMessage());
        }

        // get all UDDI Registry URLs
        int uddiNumber = 1;
        String url;
        List<String> urlList = new ArrayList<String>();
        do {
            url = uddiProps.getProperty( UddiAgent.PROP_INQUIRY_URLS + '.' + uddiNumber++);
            if (url != null) urlList.add(url);
        } while (url != null);

        String[] urls = new String[urlList.size()];
        if(!urlList.isEmpty()) {
            for (int i = 0; i < urlList.size(); i++) {
                urls[i] = urlList.get(i);
            }
        }
        return urls;
    }

    /**
     * Find all URLs of the WSDLs from UDDI Registry given the service name pattern.
     *
     * @param uddiURL  The URL of the UDDI Registry
     * @param info     Type info for the UDDI Registry (optional if auth not present)
     * @param username The user account name (optional)
     * @param password The user account password (optional)
     * @param namePattern  The string of the service name (wildcard % is supported)
     * @param caseSensitive  True if case sensitive, false otherwise.
     * @return A list of URLs of the WSDLs of the services whose name matches the namePattern.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    public WsdlInfo[] findWsdlUrlsFromUDDIRegistry(final String uddiURL,
                                                   final UDDIRegistryInfo info,
                                                   final String username,
                                                   final char[] password,
                                                   final String namePattern,
                                                   final boolean caseSensitive) throws FindException {
        try {
            UddiAgent uddiAgent = uddiAgentFactory.getUddiAgent();
            WsdlInfo[] wsdlInfo = uddiAgent.getWsdlByServiceName(uddiURL, info, username, password, namePattern, caseSensitive);
            //noinspection unchecked
            Arrays.sort(wsdlInfo, new ResolvingComparator(new Resolver<WsdlInfo,String>(){
                public String resolve(WsdlInfo key) {
                    return key.getName();
                }
            }, false));
            return wsdlInfo;
        } catch (UddiAgentException e) {
            String msg = "Error searching UDDI registry";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg + ": " + ExceptionUtils.getMessage(e));
        }
    }

    public String[] listExistingCounterNames() throws FindException {
        // get all the names for the counters
        return counterIDManager.getDistinctCounterNames();
    }

    public SampleMessage findSampleMessageById(long oid) throws FindException {
        return sampleMessageManager.findByPrimaryKey(oid);
    }

    public EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws FindException {
        return sampleMessageManager.findHeaders(serviceOid, operationName);
    }

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

    public void deleteSampleMessage(SampleMessage message) throws DeleteException {
        sampleMessageManager.delete(message);
    }

    public Set<ServiceTemplate> findAllTemplates() {
        return serviceTemplateManager.findAll();
    }

    /**
     * Parse the String service ID to long (database format). Throws runtime exc
     * @param serviceID the service ID, must not be null, and .
     * @return the service ID representing <code>long</code>
     *
     * @throws IllegalArgumentException if service ID is null
     * @throws NumberFormatException on parse error
     */
    private static long toLong(String serviceID)
      throws IllegalArgumentException {
        if (serviceID == null) {
                throw new IllegalArgumentException();
            }
        return Long.parseLong(serviceID);
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
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
                }

                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port, clientAddress, clientPort);
                }

                public Socket createSocket(String host, int port) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port);
                }

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

    public String getPolicyURL(String serviceoid) throws FindException {
        return registryPublicationManager.getExternalSSGPolicyURL(serviceoid);
    }

    public String getConsumptionURL(String serviceoid) throws FindException {
        return registryPublicationManager.getExternalSSGConsumptionURL(serviceoid);
    }

    public Collection<UDDIRegistryInfo> getUDDIRegistryInfo() {
        return uddiTemplateManager.getTemplatesAsUDDIRegistryInfo();
    }

    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        return asyncSupport.getJobStatus(jobId);
    }

    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        return asyncSupport.getJobResult(jobId);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
    }

}
