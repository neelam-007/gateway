package com.l7tech.server.service;

import com.l7tech.common.LicenseException;
import com.l7tech.common.io.ByteLimitInputStream;
import static com.l7tech.common.security.rbac.EntityType.SERVICE;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.uddi.UddiAgentException;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.GatewayFeatureSets;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.service.uddi.UddiAgent;
import com.l7tech.server.service.uddi.UddiAgentFactory;
import com.l7tech.server.sla.CounterIDManager;
import com.l7tech.server.systinet.RegistryPublicationManager;
import com.l7tech.server.transport.http.SslClientTrustManager;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.service.ServiceAdmin;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the ServiceAdmin admin api.
 *
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 6, 2003
 */
public class ServiceAdminImpl implements ServiceAdmin {
    private SSLContext sslContext;

    private AssertionLicense licenseManager;
    private RegistryPublicationManager registryPublicationManager;
    private UddiAgentFactory uddiAgentFactory;
    private ServiceManager serviceManager;
    private PolicyValidator policyValidator;
    private SampleMessageManager sampleMessageManager;
    private CounterIDManager counterIDManager;
    private SslClientTrustManager trustManager;
    private RoleManager roleManager;

    public ServiceAdminImpl(AssertionLicense licenseManager,
                            RegistryPublicationManager registryPublicationManager,
                            UddiAgentFactory uddiAgentFactory,
                            ServiceManager serviceManager,
                            PolicyValidator policyValidator,
                            SampleMessageManager sampleMessageManager,
                            CounterIDManager counterIDManager,
                            SslClientTrustManager trustManager,
                            RoleManager roleManager) {
        this.licenseManager = licenseManager;
        this.registryPublicationManager = registryPublicationManager;
        this.uddiAgentFactory = uddiAgentFactory;
        this.serviceManager = serviceManager;
        this.policyValidator = policyValidator;
        this.sampleMessageManager = sampleMessageManager;
        this.counterIDManager = counterIDManager;
        this.trustManager = trustManager;
        this.roleManager = roleManager;
    }

    private void checkLicense() throws RemoteException {
        try {
            licenseManager.requireFeature(GatewayFeatureSets.SERVICE_ADMIN);
        } catch (LicenseException e) {
            // New exception to conceal original stack trace from LicenseManager
            throw new RemoteException(ExceptionUtils.getMessage(e), new LicenseException(e.getMessage()));
        }
    }

    public String resolveWsdlTarget(String url) throws IOException {
        GetMethod get = null;
        try {
            checkLicense();
            URL urltarget = new URL(url);
            HttpClient client = new HttpClient();
            get = new GetMethod(url);
            // bugfix for 1857 (next 3 lines)
            get.setHttp11(true);
            String hostval = urltarget.getHost();
            if (urltarget.getPort() > 0) {
                hostval = hostval + ":" + urltarget.getPort();
            }
            get.setRequestHeader("HOST", hostval);

            // support for passing username and password in the url from the ssm
            String userinfo = urltarget.getUserInfo();
            if (userinfo != null && userinfo.indexOf(':') > -1) {
                String login = userinfo.substring(0, userinfo.indexOf(':'));
                String passwd = userinfo.substring(userinfo.indexOf(':')+1, userinfo.length());
                HttpState state = client.getState();
                get.setDoAuthentication(true);
                state.setAuthenticationPreemptive(true);
                state.setCredentials(null, null, new UsernamePasswordCredentials(login, passwd));
            }

            HostConfiguration hconf = getHostConfigurationWithTrustManager(urltarget);
            int ret;
            if (hconf != null) {
                ret = client.executeMethod(hconf, get);
            } else {
                ret = client.executeMethod(get);
            }
            if (ret == 200) {
                byte[] body = HexUtils.slurpStream(new ByteLimitInputStream(get.getResponseBodyAsStream(), 16, 10*1024*1024));
                String charset = get.getResponseCharSet();
                return new String(body, charset);
            } else {
                String msg = "The URL '" + url + "' is returning status code " + ret;
                throw new IOException(msg);
            }
        } catch (HttpException e) {
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

    public PublishedService findServiceByID(String serviceID) throws RemoteException, FindException {
        long oid = toLong(serviceID);
        PublishedService service = serviceManager.findByPrimaryKey(oid);
        if (service != null) {
            logger.finest("Returning service id " + oid + ", version " + service.getVersion());
        }
        return service;
    }

    public EntityHeader[] findAllPublishedServices() throws RemoteException, FindException {
        Collection<EntityHeader> res = serviceManager.findAllHeaders();
        return collectionToHeaderArray(res);
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws RemoteException, FindException {
            Collection<EntityHeader> res = serviceManager.findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
    }

    public PolicyValidatorResult validatePolicy(String policyXml, long serviceid) throws RemoteException {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceid);
            Assertion assertion = WspReader.parseStrictly(policyXml);
            return policyValidator.validate(assertion, service, licenseManager);
        } catch (FindException e) {
            logger.log(Level.WARNING, "cannot get existing service: " + serviceid, e);
            throw new RemoteException("cannot get existing service: " + serviceid, e);
        } catch (IOException e) {
            logger.log(Level.WARNING, "cannot parse passed policy xml: " + policyXml, e);
            throw new RemoteException("cannot parse passed policy xml", e);
        }
    }

    /**
     * this save method handles both save and updates.
     * the distinction happens on the server side by inspecting the oid of the object
     * if the oid appears to be "virgin" a save is invoked, otherwise an update call is made.
     * @param service the object to be saved or upodated
     * @throws RemoteException
     *
     */
    public long savePublishedService(PublishedService service)
            throws RemoteException, UpdateException, SaveException, VersionException, PolicyAssertionException
    {
        checkLicense();
        long oid;

        if (service.getOid() > 0) {
            // UPDATING EXISTING SERVICE
            oid = service.getOid();
            logger.fine("Updating PublishedService: " + oid);
            serviceManager.update(service);
        } else {
            // SAVING NEW SERVICE
            logger.fine("Saving new PublishedService");
            oid = serviceManager.save(service);
            addManageServiceRole(service);
        }
        return oid;
    }

    private void addManageServiceRole(PublishedService service) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();
        if (currentUser == null) throw new IllegalStateException("Couldn't get current user");

        String name = "Manage " + service.getName() + " Service (#" + service.getOid() + ")";

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        newRole.addPermission(READ, SERVICE, service.getId()); // Read this service
        newRole.addPermission(UPDATE, SERVICE, service.getId()); // Update this service
        newRole.addPermission(DELETE, SERVICE, service.getId()); // Delete this service

        boolean omnipotent;
        try {
            omnipotent = roleManager.isPermittedForAllEntities(currentUser, SERVICE, READ);
            omnipotent = omnipotent & roleManager.isPermittedForAllEntities(currentUser, SERVICE, UPDATE);
            omnipotent = omnipotent & roleManager.isPermittedForAllEntities(currentUser, SERVICE, DELETE);
        } catch (FindException e) {
            throw new SaveException("Coudln't get existing permissions", e);
        }

        if (!omnipotent) {
            logger.info("Assigning current User to new Role");
            newRole.addAssignedUser(currentUser);
        }
        roleManager.save(newRole);
    }

    public void deletePublishedService(String serviceID) throws RemoteException, DeleteException {
        PublishedService service;
        try {
            long oid = toLong(serviceID);
            checkLicense();
            service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    // ************************************************
    // PRIVATES
    // ************************************************

    private EntityHeader[] collectionToHeaderArray(Collection<EntityHeader> input) throws RemoteException {
        if (input == null) return new EntityHeader[0];
        EntityHeader[] output = new EntityHeader[input.size()];
        int count = 0;
        for (EntityHeader in : input) {
            try {
                output[count] = in;
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, null, e);
                throw new RemoteException("Collection contained something other than a EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    public String[] findUDDIRegistryURLs() throws RemoteException, FindException {
        Properties uddiProps;
        try {
            uddiProps = uddiAgentFactory.getUddiProperties();
        } catch (UddiAgentException uae) {
            throw new FindException(uae.getMessage());
        }

        // get all UDDI Registry URLs
        int uddiNumber = 1;
        String url;
        List<String> urlList = new ArrayList<String>();
        do {
            url = uddiProps.getProperty(UddiAgent.PROP_INQUIRY_URLS + "." + uddiNumber++);
            if (url != null) urlList.add(url);
        } while (url != null);

        String[] urls = new String[urlList.size()];
        if(urlList.size() > 0) {
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
     * @param namePattern  The string of the service name (wildcard % is supported)
     * @param caseSensitive  True if case sensitive, false otherwise.
     * @return A list of URLs of the WSDLs of the services whose name matches the namePattern.
     * @throws RemoteException  on remote communication error
     * @throws FindException   if there was a problem accessing the requested information.
     */
    public WsdlInfo[] findWsdlUrlsFromUDDIRegistry(String uddiURL, String namePattern, boolean caseSensitive) throws RemoteException, FindException {
        checkLicense();
        try {
            UddiAgent uddiAgent = uddiAgentFactory.getUddiAgent();
            return uddiAgent.getWsdlByServiceName(uddiURL, namePattern, caseSensitive);
        } catch (UddiAgentException e) {
            String msg = "Error searching UDDI registry";
            logger.log(Level.WARNING, msg, e);
            throw new FindException(msg, e);
        }
    }

    public String[] listExistingCounterNames() throws RemoteException, FindException {
        // get all the names for the counters
        return counterIDManager.getDistinctCounterNames();
    }

    public SampleMessage findSampleMessageById(long oid) throws RemoteException, FindException {
        return sampleMessageManager.findByPrimaryKey(oid);
    }

    public EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws RemoteException, FindException {
        return sampleMessageManager.findHeaders(serviceOid, operationName);
    }

    public long saveSampleMessage(SampleMessage sm) throws SaveException, RemoteException {
        checkLicense();
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

    public void deleteSampleMessage(SampleMessage message) throws DeleteException, RemoteException {
        checkLicense();
        sampleMessageManager.delete(message);
    }


    /**
     * Parse the String service ID to long (database format). Throws runtime exc
     * @param serviceID the service ID, must not be null, and .
     * @return the service ID representing <code>long</code>
     *
     * @throws IllegalArgumentException if service ID is null
     * @throws NumberFormatException on parse error
     */
    private long toLong(String serviceID)
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
            final int port = url.getPort() == -1 ? 443 : url.getPort();
            hconf = new HostConfiguration();
            Protocol protocol = new Protocol(url.getProtocol(), new SecureProtocolSocketFactory() {
                public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
                }

                public Socket createSocket(String host, int port, InetAddress clientAddress, int clientPort) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port, clientAddress, clientPort);
                }

                public Socket createSocket(String host, int port) throws IOException {
                    return getSSLContext().getSocketFactory().createSocket(host, port);
                }
            }, port);
            hconf.setHost(url.getHost(), port, protocol);
        }
        return hconf;
    }

    public String getPolicyURL(String serviceoid) throws RemoteException, FindException {
        return registryPublicationManager.getExternalSSGPolicyURL(serviceoid);
    }

    public String getConsumptionURL(String serviceoid) throws RemoteException, FindException {
        return registryPublicationManager.getExternalSSGConsumptionURL(serviceoid);
    }
}
