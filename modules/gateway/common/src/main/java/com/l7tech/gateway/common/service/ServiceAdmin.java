package com.l7tech.gateway.common.service;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.admin.AliasAdmin;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.util.CollectionUpdate;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.Policy;
import static com.l7tech.gateway.common.security.rbac.EntityType.SAMPLE_MESSAGE;
import static com.l7tech.gateway.common.security.rbac.EntityType.SERVICE;
import static com.l7tech.gateway.common.security.rbac.EntityType.*;
import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.uddi.UDDIRegistryInfo;
import com.l7tech.uddi.WsdlInfo;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.PolicyAssertionException;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

/**
 * Provides a remote interface for publishing searching and updating published services
 * and service policies.
 *
 * @see PublishedService
 * @see ServiceHeader
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured
@Administrative
public interface ServiceAdmin extends ServiceAdminPublic, AsyncAdminMethods, AliasAdmin<PublishedServiceAlias> {
    String ROLE_NAME_TYPE_SUFFIX = "Service";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    /**
     * Retrieve all available service documents for the given published service.
     *
     * @param serviceID The unique identifier of the service
     * @return The collection of ServiceDocuments
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    @Secured(types=SERVICE, stereotype=GET_PROPERTY_BY_ID)
    @Administrative(licensed=false)
    Collection<ServiceDocument> findServiceDocumentsByServiceID(String serviceID) throws FindException;

    /**
     * Retrieve a chunk of the available {@link PublishedService} headers  This is a version of
     * {@link #findAllPublishedServices()} that allows fetching the result in chunks, perhaps to reduce
     * latency.
     *
     * @return array of {@link ServiceHeader}s for requested subset of {@link PublishedService}s May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_HEADERS)
    @Administrative(licensed=false)
    ServiceHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws FindException;


    /**
     * Retrieves changes in list of published services.
     *
     * @param oldVersionID  version ID from previous retrieval
     * @return collection changes; never null
     * @throws FindException if there was a problem accessing the requested information
     * @see CollectionUpdate
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_HEADERS)
    @Administrative(licensed=false)
    CollectionUpdate<ServiceHeader> getPublishedServicesUpdate(final int oldVersionID) throws FindException;

    /**
     * Store the specified new or existing published service. If the specified {@link PublishedService} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     * <p/>
     * The policy XML in the policy property will be made the active version of the policy.
     * <p/>
     * This method will always activate the newly-saved policy XML immediately.
     * If this is undesirable, see {@link com.l7tech.gateway.common.admin.PolicyAdmin#savePolicy(com.l7tech.policy.Policy , boolean)}
     * for a way to save policy XML without immediately activating it.
     *
     * @param service the published service to create or update.  Must not be null.
     * @return the unique object ID that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws VersionException if the service version conflict is detected
     * @throws PolicyAssertionException if the server policy could not be instantiated for this policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long savePublishedService(PublishedService service)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException;

    /**
     * Store the specified new or existing published service. If the specified {@link PublishedService} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     * <p/>
     * The policy XML in the policy property of the PublishedService will be made the active version of the policy.
     *
     * @param service the published service to create or update.  Must not be null.
     * @param serviceDocuments the serviceDocuments to save. Null means no documents.
     * @return the unique object ID that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws VersionException if the service version conflict is detected
     * @throws PolicyAssertionException if the server policy could not be instantiated for this policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE, relevantArg=0)
    long savePublishedServiceWithDocuments(PublishedService service, Collection<ServiceDocument> serviceDocuments)
            throws UpdateException, SaveException, VersionException, PolicyAssertionException;

    /**
     * Validate the service policy and return the policy validation result. Only the server side validation rules
     * are invoked here.
     *
     * @param policyXml the policy xml document
     * @param policyType the type of policy this is
     * @param soap <code>true</code> if this policy is intended for SOAP services; <code>false</code> otherwise.
     * @param wsdl the contents of the WSDL with which this policy is intended to be compatible
     * @return the job identifier of the validation job.  Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a PolicyValidatorResult that contains
     *         policy validation warnings and errors
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    JobId<PolicyValidatorResult> validatePolicy(String policyXml, PolicyType policyType, boolean soap, Wsdl wsdl);

    /**
     * Validate the service policy and return the policy validation result. Only the server side validation rules
     * are invoked here.
     *
     * @param policyXml the policy xml document
     * @param policyType the type of policy this is
     * @param soap <code>true</code> if this policy is intended for SOAP services; <code>false</code> otherwise.
     * @param wsdl the contents of the WSDL with which this policy is intended to be compatible
     * @param fragments the policy fragments that were included with this policy when it was imported
     * @return the job identifier of the validation job.  Call {@link #getJobStatus(com.l7tech.gateway.common.AsyncAdminMethods.JobId) getJobStatus} to poll for job completion
     *         and {@link #getJobResult(JobId)} to pick up the result in the form of a PolicyValidatorResult that contains
     *         policy validation warnings and errors
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    JobId<PolicyValidatorResult> validatePolicy(String policyXml, PolicyType policyType, boolean soap, Wsdl wsdl, HashMap<String, Policy> fragments);

    /**
     * Find all URLs of the WSDLs from UDDI Registry given the service name pattern.
     *
     * @param uddiURL  The URL of the UDDI Registry
     * @param info     Type info for the UDDI Registry (optional if auth not present)
     * @param username The user account name (optional)
     * @param password The user account password (optional)
     * @param namePattern The string of the service name (wildcard % is supported)
     * @param caseSensitive  True if case sensitive, false otherwise.
     * @return A list of URLs of the WSDLs of the services whose name matches the namePattern.
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    WsdlInfo[] findWsdlUrlsFromUDDIRegistry(String uddiURL, UDDIRegistryInfo info, String username, char[] password, String namePattern, boolean caseSensitive) throws FindException ;

    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    String[] findUDDIRegistryURLs() throws FindException;

    /**
     * Gets the ThroughputQuota counter names already defined on this gateway. This is used by the ThroughputQuota assertion
     * properties dialog to populate a combo box to choose the counters from.
     * @return a string array with one item for each different counter name for this gateway
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    String[] listExistingCounterNames() throws FindException;

    /**
     * Finds the {@link SampleMessage} instance with the specified OID, or null if it does not exist.
     * @return the {@link SampleMessage} instance with the specified OID.  May be null if not present.
     */
    @Secured(types=SAMPLE_MESSAGE, stereotype=FIND_BY_PRIMARY_KEY)
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    SampleMessage findSampleMessageById(long oid) throws FindException;

    /**
     * Finds any {@link EntityHeader}s belonging to the {@link PublishedService}
     * with the specified OID and (optional) operation name.
     * @param serviceOid the OID of the {@link PublishedService} to which the SampleMessage belongs. Pass -1 for all services.
     * @param operationName the name of the operation for which the SampleMessage was saved. Pass null for all operations, or "" for messages that are not categorized by operation name.
     * @return an array of {@link EntityHeader}s. May be empty, but never null.
     */
    @Transactional(readOnly=true)
    @Secured(types=SAMPLE_MESSAGE, stereotype=FIND_HEADERS)
    @Administrative(licensed=false)
    EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws FindException;

    @Secured(types=SAMPLE_MESSAGE, stereotype=SAVE_OR_UPDATE)
    long saveSampleMessage(SampleMessage sm) throws SaveException;

    @Secured(types=SAMPLE_MESSAGE, stereotype=DELETE_ENTITY)
    void deleteSampleMessage(SampleMessage message) throws DeleteException;

    /**
     * Delete a {@link com.l7tech.gateway.common.service.PublishedService} by its unique identifier.

     * @param oid the unique identifier of the {@link com.l7tech.gateway.common.service.PublishedService} to delete.
     * @throws com.l7tech.objectmodel.DeleteException if the requested information could not be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    void deletePublishedService(String oid) throws DeleteException;

    /**
     * @param serviceoid id of the service to publish on the systinet (or other UDDI) registry
     * @return registrykey
     */
    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    String getPolicyURL(String serviceoid) throws FindException;

    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    String getConsumptionURL(String serviceoid ) throws FindException;

    @Transactional(readOnly=true)
    @Administrative(licensed=false)
    public Collection<UDDIRegistryInfo> getUDDIRegistryInfo();

    @Transactional(readOnly=true)
    @Secured(types=SERVICE_TEMPLATE, stereotype= MethodStereotype.FIND_ENTITIES)
    Set<ServiceTemplate> findAllTemplates();
}
