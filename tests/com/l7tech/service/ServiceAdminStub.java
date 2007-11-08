package com.l7tech.service;

import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.common.uddi.UDDIRegistryInfo;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.console.util.Registry;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Logger;

/**
 * Class ServiceAdminStub.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 */
public class ServiceAdminStub extends ApplicationObjectSupport implements ServiceAdmin, InitializingBean {
    private static final Logger logger = Logger.getLogger(ServiceAdminStub.class.getName());
    private PolicyValidator policyValidator;
    private ServiceManager serviceManager;

    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     */
    public PublishedService findServiceByID(String oid) throws FindException {
        return serviceManager.findByPrimaryKey(toLong(oid));
    }


    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     *
     * @param url
     * @return a string containing the xml document
     */
    public String resolveWsdlTarget(String url) {
        return serviceManager.resolveWsdlTarget(url);
    }

    /**
     * saves a published service along with it's policy assertions
     *
     * @param service
     */
    public long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException {
        return serviceManager.save(service);
    }

    /**
     * saves a published service along with it's policy assertions
     *
     * @param service
     * @param docs ignored
     */
    public long savePublishedServiceWithDocuments(PublishedService service, Collection<ServiceDocument> docs)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException {
        return serviceManager.save(service);
    }

    /**
     * Find service docs by service id
     *
     * @param serviceID The service id
     * @return The documents
     * @throws FindException on find error
     */
    public Collection<ServiceDocument> findServiceDocumentsByServiceID(String serviceID) throws FindException {
        return Collections.EMPTY_LIST;
    }

    /**
     * deletes the service
     *
     * @param id service id
     */
    public void deletePublishedService(String id) throws DeleteException {
        PublishedService service = null;
        try {
            long oid = toLong(id);
            service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }


    }

    public PolicyValidatorResult validatePolicy(String policyXml, long serviceId) {
        try {
            PublishedService service = serviceManager.findByPrimaryKey(serviceId);
            Assertion assertion = WspReader.getDefault().parsePermissively(policyXml);
            return policyValidator.validate(assertion, service, Registry.getDefault().getLicenseManager());
        } catch (FindException e) {
            throw new RuntimeException("cannot get existing service: " + serviceId, e);
        } catch (IOException e) {
            throw new RuntimeException("cannot parse passed policy xml", e);
        } catch (InterruptedException e) {
            throw new RuntimeException("validation thread interrupted", e);
        }
    }


    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for all instances of the entity class corresponding to
     * this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public EntityHeader[] findAllPublishedServices() throws FindException {
        Collection res = serviceManager.findAllHeaders();
        return collectionToHeaderArray(res);
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    public EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) {
        throw new RuntimeException("Not Implemented");
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
     */
    public WsdlInfo[] findWsdlUrlsFromUDDIRegistry(String uddiURL, UDDIRegistryInfo info, String username, char[] password, String namePattern, boolean caseSensitive) throws FindException {
        WsdlInfo[] siList = new WsdlInfo[3];

        siList[0]= new WsdlInfo("Google Service", "http://api.google.com/GoogleSearch.wsdl");
        siList[1]= new WsdlInfo("Delayed Quote Service", "http://services.xmethods.net/soap/urn:xmethods-delayed-quotes.wsdl");
        siList[2]= new WsdlInfo("Stock Quote Service", "http://paris/wsdl/StockQuote_WSDL.wsdl");

        return siList;
    }

    public String[] findUDDIRegistryURLs() throws FindException {
        String[] urlList = new String[3];
        urlList[0] = "http://whale.l7tech.com:8080/uddi/inquiry";
        urlList[1] = "http://bones.l7tech.com:8080/uddi/inquiry";
        urlList[2] = "http://hugh.l7tech.com:8080/uddi/inquiry";

        return urlList;
    }

    public String[] listExistingCounterNames() throws FindException {
        return new String[0];
    }

    public SampleMessage findSampleMessageById(long oid) throws FindException {
        return null;
    }

    public EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws FindException {
        return new EntityHeader[0];
    }

    public long saveSampleMessage(SampleMessage sm) throws SaveException {
        return 0;
    }

    public void deleteSampleMessage(SampleMessage message) throws DeleteException {

    }

    private EntityHeader fromService(PublishedService s) {
        return new EntityHeader(Long.toString(s.getOid()), EntityType.SERVICE, s.getName(), null);
    }

    public void setPolicyValidator(PolicyValidator policyValidator) {
        this.policyValidator = policyValidator;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public void afterPropertiesSet() throws Exception {
        if (policyValidator == null) {
            throw new IllegalArgumentException("Policy Validator is required");
        }
    }


    /**
     * Parse the String service ID to long (database format). Throws runtime exc
     *
     * @param serviceID the service ID, must not be null, and .
     * @return the service ID representing <code>long</code>
     * @throws IllegalArgumentException if service ID is null
     * @throws NumberFormatException    on parse error
     */
    private long toLong(String serviceID)
      throws IllegalArgumentException {
        if (serviceID == null) {
            throw new IllegalArgumentException();
        }
        return Long.parseLong(serviceID);
    }

    private EntityHeader[] collectionToHeaderArray(Collection input) {
        if (input == null) return new EntityHeader[0];
        EntityHeader[] output = new EntityHeader[input.size()];
        int count = 0;
        java.util.Iterator i = input.iterator();
        while (i.hasNext()) {
            try {
                output[count] = (EntityHeader)i.next();
            } catch (ClassCastException e) {
                throw new RuntimeException("Collection contained something other than a EntityHeader", e);
            }
            ++count;
        }
        return output;
    }

    public String getPolicyURL(String serviceoid) throws FindException {
        throw new RuntimeException("Not Implemented");
    }

    public String getConsumptionURL(String serviceoid) throws FindException {
        throw new RuntimeException("Not Implemented");
    }

    public Collection<UDDIRegistryInfo> getUDDIRegistryInfo() {
        throw new RuntimeException("Not Implemented");
    }
}
