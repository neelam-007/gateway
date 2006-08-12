package com.l7tech.service;

import static com.l7tech.common.security.rbac.EntityType.SAMPLE_MESSAGE;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.PolicyAssertionException;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.rmi.RemoteException;

/**
 * Provides a remote interface for publishing searching and updating published services
 * and service policies.
 *
 * @see PublishedService
 * @see EntityHeader
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public interface ServiceAdmin extends ServiceAdminPublic {
    /**
     * Retrieve a chunk of the available {@link PublishedService} headers  This is a version of
     * {@link #findAllPublishedServices} that allows fetching the result in chunks, perhaps to reduce
     * latency.
     *
     * @return array of {@link EntityHeader}s for requested subset of {@link PublishedService}s May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    @Secured(stereotype=FIND_HEADERS)
    EntityHeader[] findAllPublishedServicesByOffset(int offset, int windowSize) throws RemoteException, FindException;

    /**
     * Store the specified new or existing published service. If the specified {@link PublishedService} contains a
     * unique object ID that already exists, this will replace the objects current configuration with the new configuration.
     * Otherwise, a new object will be created.
     *
     * @param service the published service to create or update.  Must not be null.
     * @return the unique object ID that was updated or created.
     * @throws RemoteException on remote communication error
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws VersionException if the service version conflict is detected
     * @throws PolicyAssertionException if the server policy could not be instantiated for this policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long savePublishedService(PublishedService service)
            throws RemoteException, UpdateException, SaveException, VersionException, PolicyAssertionException;

    /**
     * Validate the service policy and return the policy validation result. Only the server side validation rules
     * are invoked here.
     * @param policyXml the policy xml document
     * @param serviceId the service unique ID
     * @return the policy validation result that contains policy validation warnings
     * and errors
     *
     * @throws RemoteException on remote communication error
     */
    @Transactional(readOnly=true)
    PolicyValidatorResult validatePolicy(String policyXml, long serviceId) throws RemoteException;

    /**
     * Find all URLs of the WSDLs from UDDI Registry given the service name pattern.
     *
     * @param uddiURL  The URL of the UDDI Registry
     * @param namePattern The string of the service name (wildcard % is supported)
     * @param caseSensitive  True if case sensitive, false otherwise.
     * @return A list of URLs of the WSDLs of the services whose name matches the namePattern.
     * @throws RemoteException           on remote communication error
     * @throws FindException   if there was a problem accessing the requested information.
     */
    @Transactional(readOnly=true)
    WsdlInfo[] findWsdlUrlsFromUDDIRegistry(String uddiURL, String namePattern, boolean caseSensitive) throws RemoteException, FindException ;

    @Transactional(readOnly=true)
    String[] findUDDIRegistryURLs() throws RemoteException, FindException;

    /**
     * Gets the ThroughputQuota counter names already defined on this gateway. This is used by the ThroughputQuota assertion
     * properties dialog to populate a combo box to choose the counters from.
     * @return a string array with one item for each different counter name for this gateway
     */
    @Transactional(readOnly=true)
    String[] listExistingCounterNames() throws RemoteException, FindException;

    /**
     * Finds the {@link SampleMessage} instance with the specified OID, or null if it does not exist.
     * @return the {@link SampleMessage} instance with the specified OID.  May be null if not present.
     */
    @Secured(types=SAMPLE_MESSAGE, stereotype=FIND_BY_PRIMARY_KEY)
    @Transactional(readOnly=true)
    SampleMessage findSampleMessageById(long oid) throws RemoteException, FindException;

    /**
     * Finds any {@link EntityHeader}s belonging to the {@link PublishedService}
     * with the specified OID and (optional) operation name.
     * @param serviceOid the OID of the {@link PublishedService} to which the SampleMessage belongs. Pass -1 for all services.
     * @param operationName the name of the operation for which the SampleMessage was saved. Pass null for all operations, or "" for messages that are not categorized by operation name.
     * @return an array of {@link EntityHeader}s. May be empty, but never null.
     */
    @Transactional(readOnly=true)
    @Secured(types=SAMPLE_MESSAGE, stereotype=FIND_HEADERS)
    EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws RemoteException, FindException;

    @Secured(types=SAMPLE_MESSAGE, stereotype=SAVE_OR_UPDATE)
    long saveSampleMessage(SampleMessage sm) throws SaveException, RemoteException;

    @Secured(types=SAMPLE_MESSAGE, stereotype=DELETE_ENTITY)
    void deleteSampleMessage(SampleMessage message) throws DeleteException, RemoteException;

    /**
     * Delete a {@link com.l7tech.service.PublishedService} by its unique identifier.

     * @param oid the unique identifier of the {@link com.l7tech.service.PublishedService} to delete.
     * @throws java.rmi.RemoteException on remote communication error
     * @throws com.l7tech.objectmodel.DeleteException if the requested information could not be deleted
     */
    @Secured(stereotype= DELETE_BY_ID)
    void deletePublishedService(String oid) throws RemoteException, DeleteException;

    /**
     * @param serviceoid id of the service to publish on the systinet registry
     * @return registrykey
     */
    @Transactional(readOnly=true)
    String getPolicyURL(String serviceoid) throws RemoteException, FindException;

    @Transactional(readOnly=true)
    String getConsumptionURL(String serviceoid ) throws RemoteException, FindException;
}
