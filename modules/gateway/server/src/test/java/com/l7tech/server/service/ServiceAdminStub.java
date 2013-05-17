package com.l7tech.server.service;

import com.l7tech.server.admin.AsyncAdminMethodsImpl;
import com.l7tech.gateway.common.service.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionLicense;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.uddi.UDDINamedEntity;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.uddi.WsdlPortInfo;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.util.CollectionUpdateProducer;
import com.l7tech.util.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;
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

    @Override
    public JobId<WsdlPortInfo[]> findWsdlInfosForSingleBusinessService(long registryOid, String serviceKey, boolean getFirstOnly) throws FindException {
        return null;
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

    @Override
    public String resolveUrlTarget(String url, String maxSizeClusterProperty) throws IOException {
        return null;
    }

    @Override
    public String resolveUrlTarget(String url, DownloadDocumentType docType) throws IOException {
        return null;
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
                                                       final PolicyValidationContext pvc)
    {
        Future<PolicyValidatorResult> future = new FutureTask<PolicyValidatorResult>(new Callable<PolicyValidatorResult>() {
            @Override
            public PolicyValidatorResult call() throws Exception {
                try {
                    final Assertion assertion = WspReader.getDefault().parsePermissively(policyXml, WspReader.INCLUDE_DISABLED);
                    return policyValidator.validate(assertion, pvc,
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
                                                       final PolicyValidationContext pvc,
                                                       HashMap<String, Policy> fragments)
    {
        return validatePolicy(policyXml, pvc);
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

    @Override
    public JobId<WsdlPortInfo[]> findWsdlInfosFromUDDIRegistry(long registryOid, String namePattern, boolean caseSensitive, boolean getWsdlURL) {
        return null;
    }

    @Override
    public JobId<UDDINamedEntity[]> findBusinessesFromUDDIRegistry(long registryOid, String namePattern, boolean caseSensitive) {
        return null;
    }

    @Override
    public UDDINamedEntity[] findPoliciesFromUDDIRegistry( final long registryOid, final String namePattern ) throws FindException {
        return new UDDINamedEntity[0];
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
    public String getPolicyURL(String serviceoid, boolean fullPolicyURL) throws FindException {
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
    public ServiceTemplate createSecurityTokenServiceTemplate(String wsTrustNamespace) {
        return null;
    }

    @Override
    public ResolutionReport generateResolutionReport( final PublishedService service, final Collection<ServiceDocument> serviceDocuments ) throws FindException {
        throw new FindException("Not implemented");
    }

    @Override
    public ResolutionReport generateResolutionReportForNewService( final PublishedService service, final Collection<ServiceDocument> serviceDocuments ) throws FindException {
        throw new FindException("Not implemented");
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

    @NotNull
    @Override
    public Collection<PublishedService> findBySecurityZoneOid(long securityZoneOid) {
        return null;
    }
}