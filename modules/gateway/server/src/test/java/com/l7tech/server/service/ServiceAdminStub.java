package com.l7tech.server.service;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.uddi.WsdlInfo;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUpdateProducer;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.gateway.common.AsyncAdminMethodsImpl;
import com.l7tech.gateway.common.service.*;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
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
    private ServiceAliasManager serviceAliasManager;

    private AsyncAdminMethodsImpl asyncSupport = new AsyncAdminMethodsImpl();
    private CollectionUpdateProducer<ServiceHeader, FindException> publishedServicesUpdateProducer =
            new CollectionUpdateProducer<ServiceHeader, FindException>(5000, 10, new ServiceHeaderDifferentiator()) {
                @Override
                protected Collection<ServiceHeader> getCollection() throws FindException {
                    return serviceManager.findAllHeaders();
                }
            };

    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     */
    @Override
    public PublishedService findServiceByID(String oid) throws FindException {
        return serviceManager.findByPrimaryKey(toLong(oid));
    }

    @Override
    public PublishedServiceAlias findAliasByEntityAndFolder(Long serviceOid, Long folderOid) throws FindException {
        return serviceAliasManager.findAliasByEntityAndFolder(serviceOid, folderOid);
    }

    /**
     * Used by the console to retreive the actual wsdl located at a target
     * as seen by the ssg.
     *
     * @param url
     * @return a string containing the xml document
     */
    @Override
    public String resolveWsdlTarget(String url) {
        return serviceManager.resolveWsdlTarget(url);
    }

    /**
     * saves a published service along with it's policy assertions
     *
     * @param service
     */
    @Override
    public long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException {
        return serviceManager.save(service);
    }

    @Override
    public long saveAlias(PublishedServiceAlias serviceAlias) throws UpdateException, SaveException, VersionException {
        return serviceAliasManager.save(serviceAlias);
    }

    /**
     * saves a published service along with it's policy assertions
     *
     * @param service
     * @param docs ignored
     */
    @Override
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
    @Override
    public Collection<ServiceDocument> findServiceDocumentsByServiceID(String serviceID) throws FindException {
        return Collections.emptyList();
    }

    /**
     * deletes the service
     *
     * @param id service id
     */
    @Override
    public void deletePublishedService(String id) throws DeleteException {
        try {
            long oid = toLong(id);
            PublishedService service = serviceManager.findByPrimaryKey(oid);
            serviceManager.delete(service);
            logger.info("Deleted PublishedService: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }


    }

    @Override
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
    
    @Override
    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml,
                                                       final PolicyType policyType,
                                                       final boolean soap,
                                                       final Wsdl wsdl)
    {
        Future<PolicyValidatorResult> future = new FutureTask<PolicyValidatorResult>(new Callable<PolicyValidatorResult>() {
            @Override
            public PolicyValidatorResult call() throws Exception {
                try {
                    final Assertion assertion = WspReader.getDefault().parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
                    return policyValidator.validate(assertion, policyType, wsdl, soap,
                            new AssertionLicense() {
                                @Override
                                public boolean isAssertionEnabled( Assertion assertion ) {
                                    return true;
                                }
                            });
                } catch (IOException e) {
                    throw new RuntimeException("cannot parse passed policy xml", e);
                }
            }
        });
        return asyncSupport.registerJob(future, PolicyValidatorResult.class);
    }

    @Override
    public JobId<PolicyValidatorResult> validatePolicy(final String policyXml,
                                                       final PolicyType policyType,
                                                       final boolean soap,
                                                       final Wsdl wsdl,
                                                       HashMap<String, Policy> fragments)
    {
        return validatePolicy(policyXml, policyType, soap, wsdl);
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for all instances of the entity class corresponding to
     * this Manager.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    @Override
    public ServiceHeader[] findAllPublishedServices() throws FindException {
        Collection<ServiceHeader> res = serviceManager.findAllHeaders();
        return collectionToHeaderArray(res);
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for all instances of the entity class corresponding to
     * this Manager.
     * @param includeAliases if true the returned array can contain aliases
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    @Override
    public ServiceHeader[] findAllPublishedServices(boolean includeAliases) throws FindException {
        Collection<ServiceHeader> res = serviceManager.findAllHeaders(includeAliases);
        return collectionToHeaderArray(res);
    }

    /**
     * Returns an unmodifiable collection of <code>EntityHeader</code>
     * objects for instances of this entity class from a list sorted
     * by <code>oid</code>, selecting only a specific subset of the list.
     *
     * @return A <code>Collection</code> of EntityHeader objects.
     */
    @Override
    public ServiceHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public CollectionUpdate<ServiceHeader> getPublishedServicesUpdate(int oldVersionID) throws FindException {
        return publishedServicesUpdateProducer.createUpdate(oldVersionID);
    }

    /**
     * Not used right now, deleted a test which used it but was no longer needed following the move to UDDIRegistry
     * Leaving in case we add test coverage
     *
     * @param registryOid
     * @param namePattern   The string of the service name (wildcard % is supported)
     * @param caseSensitive True if case sensitive, false otherwise.   @return
     * @throws FindException
     */
    @Override
    public WsdlInfo[] findWsdlUrlsFromUDDIRegistry(long registryOid, String namePattern, boolean caseSensitive) throws FindException {
        WsdlInfo[] siList = new WsdlInfo[3];

        siList[0] = new WsdlInfo("Google Service", "http://api.google.com/GoogleSearch.wsdl");
        siList[1] = new WsdlInfo("Delayed Quote Service", "http://services.xmethods.net/soap/urn:xmethods-delayed-quotes.wsdl");
        siList[2] = new WsdlInfo("Stock Quote Service", "http://paris/wsdl/StockQuote_WSDL.wsdl");

        return siList;
    }

    @Override
    public UDDINamedEntity[] findBusinessesFromUDDIRegistry(long uddiRegistryOid, String namePattern, boolean caseSensitive) throws FindException {
        return new UDDINamedEntity[0];
    }

    @Override
    public String[] listExistingCounterNames() throws FindException {
        return new String[0];
    }

    @Override
    public SampleMessage findSampleMessageById(long oid) throws FindException {
        return null;
    }

    @Override
    public EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws FindException {
        return new EntityHeader[0];
    }

    @Override
    public long saveSampleMessage(SampleMessage sm) throws SaveException {
        return 0;
    }

    @Override
    public void deleteSampleMessage(SampleMessage message) throws DeleteException {
    }

    public void setPolicyValidator(PolicyValidator policyValidator) {
        this.policyValidator = policyValidator;
    }

    public void setServiceManager(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public void setServiceAliasManager(ServiceAliasManager serviceAliasManager){
        this.serviceAliasManager = serviceAliasManager;
    }
    
    @Override
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

    private ServiceHeader[] collectionToHeaderArray(Collection<ServiceHeader> input) {
        if (input == null) return new ServiceHeader[0];
        Collection<ServiceHeader> checked =  Collections.checkedCollection( new ArrayList<ServiceHeader>(), ServiceHeader.class );
        checked.addAll( input );
        return checked.toArray( new ServiceHeader[input.size()] );
    }

    @Override
    public String getPolicyURL(String serviceoid) throws FindException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public String getConsumptionURL(String serviceoid) throws FindException {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Collection<UDDIRegistryInfo> getUDDIRegistryInfo() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public Set<ServiceTemplate> findAllTemplates() {
        throw new RuntimeException("Not Implemented");
    }

    @Override
    public <OUT extends Serializable> String getJobStatus(JobId<OUT> jobId) {
        return asyncSupport.getJobStatus(jobId);
    }

    @Override
    public <OUT extends Serializable> JobResult<OUT> getJobResult(JobId<OUT> jobId) throws UnknownJobException, JobStillActiveException {
        return asyncSupport.getJobResult(jobId);
    }

}
