package com.l7tech.server.service;

import com.l7tech.admin.AccessManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ResolutionParameterTooLongException;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.service.uddi.UddiAgentV3;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.orm.hibernate.support.HibernateDaoSupport;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Collection;
import java.util.Properties;
import java.util.Vector;
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
public class ServiceAdminImpl extends HibernateDaoSupport implements ServiceAdmin {

    public static final String SERVICE_DEPENDENT_URL_PORTION = "/services/serviceAdmin";
    private final String UDDI_CONFIG_FILENAME = "uddi.properties";

    private ServiceManager serviceManager;
    private PolicyValidator policyValidator;
    private Properties uddiProps = null;
    private final AccessManager accessManager;

    public ServiceAdminImpl(AccessManager accessManager) {
        this.accessManager = accessManager;
    }

    public String resolveWsdlTarget(String url) throws IOException, MalformedURLException {
        try {
            URL urltarget = new URL(url);
            HttpClient client = new HttpClient();
            GetMethod get = new GetMethod(url);
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
            int ret = client.executeMethod(get);
            byte[] body = null;
            if (ret == 200) {
                body = get.getResponseBody();
                return new String(body, get.getResponseCharSet());
            } else {
                String msg = "The URL " + url + " is returning code " + ret;
                logger.info(msg);
                throw new RemoteException(msg);
            }
        } catch (MalformedURLException e) {
            String msg = "Bad url: " + url;
            logger.log(Level.WARNING, msg, e);
            throw e;
        } catch (HttpException e) {
            String msg = "Http error getting " + url;
            logger.log(Level.WARNING, msg, e);
            IOException ioe =new  IOException(msg);
            ioe.initCause(e);
            throw ioe;
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
            Collection res = serviceManager.findAllHeaders();
            return collectionToHeaderArray(res);
    }

    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize)
                    throws RemoteException, FindException {
            Collection res = serviceManager.findAllHeaders(offset, windowSize);
            return collectionToHeaderArray(res);
    }

    public PolicyValidatorResult validatePolicy(String policyXml, long serviceid) throws RemoteException {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceid);
            Assertion assertion = WspReader.parse(policyXml);
            return policyValidator.validate(assertion, service);
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
    public long savePublishedService(PublishedService service) throws RemoteException,
                                    UpdateException, SaveException, VersionException, ResolutionParameterTooLongException {

            accessManager.enforceAdminRole();
            long oid = PublishedService.DEFAULT_OID;

            if (service.getOid() > 0) {
                // UPDATING EXISTING SERVICE
                oid = service.getOid();
                logger.fine("Updating PublishedService: " + oid);
                serviceManager.update(service);
            } else {
                // SAVING NEW SERVICE
                logger.fine("Saving new PublishedService");
                oid = serviceManager.save(service);
            }
            return oid;
    }

    public void deletePublishedService(String serviceID) throws RemoteException, DeleteException {
        PublishedService service = null;
        try {
            long oid = toLong(serviceID);
            accessManager.enforceAdminRole();
            service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public void setPolicyValidator(PolicyValidator policyValidator) {
        this.policyValidator = policyValidator;
    }
    // ************************************************
    // PRIVATES
    // ************************************************

    private EntityHeader[] collectionToHeaderArray(Collection input) throws RemoteException {
        if (input == null) return new EntityHeader[0];
        EntityHeader[] output = new EntityHeader[input.size()];
        int count = 0;
        java.util.Iterator i = input.iterator();
        while (i.hasNext()) {
            try {
                output[count] = (EntityHeader)i.next();
            } catch (ClassCastException e) {
                logger.log(Level.SEVERE, null, e);
                throw new RemoteException("Collection contained something other than a EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    public String[] findUDDIRegistryURLs() throws RemoteException, FindException {
        try {
            uddiProps = readUDDIConfig();
        } catch (IOException ioe) {
            throw new FindException(ioe.getMessage());
        }

        // get all UDDI Registry URLs
        int uddiNumber = 1;
        String url;
        Vector urlList = new Vector();
        do {

            url = uddiProps.getProperty(UddiAgentV3.INQUIRY_URL_PROP_NAME + "." + uddiNumber++);
            if(url != null) {
                urlList.add(url);
            }

        } while (url != null);

        String[] urls = new String[urlList.size()];
        if(urlList.size() > 0) {
            for (int i = 0; i < urlList.size(); i++) {
                String s = (String) urlList.elementAt(i);
                urls[i] = s;
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

        // note: we only support V3 agent
        try {
            uddiProps = readUDDIConfig();
        } catch (IOException ioe) {
            logger.severe("IOException caught. Could not load UDDI Registry properties");
            throw new FindException("Could not load UDDI Registry propertie");
        }
        UddiAgentV3 uddiAgent = new UddiAgentV3(uddiURL, uddiProps);

        return uddiAgent.getWsdlByServiceName(namePattern, caseSensitive);
    }

    private Properties readUDDIConfig() throws IOException {
        String ssgConfigPath = ServerConfig.getInstance().getProperty("ssg.conf");
        String uddiConfigFileName = ssgConfigPath + "/" + UDDI_CONFIG_FILENAME;
        Properties props = new Properties();

        FileInputStream propStream = null;
        try {
            propStream = null;

            File file = new File(uddiConfigFileName);
            if (file.exists()) {
                propStream = new FileInputStream(file);
                props.load(propStream);
                logger.info("Loading UDDI Registry properties from " + uddiConfigFileName);
                return props;
            } else {
                logger.severe(uddiConfigFileName + " not found");
                throw new FileNotFoundException("Couldn't load " + uddiConfigFileName + ", File not found!");
            }

        } catch (IOException ioe) {
            logger.severe("Couldn't load " + uddiConfigFileName);
            throw new IOException("Couldn't load " + uddiConfigFileName);
        } finally {
            if (propStream != null) {
                try {
                    propStream.close();
                } catch (IOException e) {
                    logger.warning("io exception");
                }
            }
        }
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
      throws IllegalArgumentException, NumberFormatException {
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
}
