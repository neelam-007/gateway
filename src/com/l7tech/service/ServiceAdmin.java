package com.l7tech.service;

import com.l7tech.common.uddi.WsdlInfo;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidatorResult;

import java.rmi.RemoteException;

/**
 * Provides a remote interface for publishing searching and updating published services
 * and service policies.
 *
 * @see PublishedService
 * @see EntityHeader
 * @see Entity
 */
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
     */
    long savePublishedService(PublishedService service)
                    throws RemoteException, UpdateException, SaveException, VersionException;

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
    WsdlInfo[] findWsdlUrlsFromUDDIRegistry(String uddiURL, String namePattern, boolean caseSensitive) throws RemoteException, FindException ;

    String[] findUDDIRegistryURLs() throws RemoteException, FindException;

    /**
     * Gets the ThroughputQuota counter names already defined on this gateway. This is used by the ThroughputQuota assertion
     * properties dialog to populate a combo box to choose the counters from.
     * @return a string array with one item for each different counter name for this gateway
     */
    String[] listExistingCounterNames() throws RemoteException, FindException;

    /**
     * Finds the {@link SampleMessage} instance with the specified OID, or null if it does not exist.
     * @return the {@link SampleMessage} instance with the specified OID.  May be null if not present.
     */
    SampleMessage findSampleMessageById(long oid) throws RemoteException, FindException;

    /**
     * Finds any {@link EntityHeader}s belonging to the {@link PublishedService}
     * with the specified OID and (optional) operation name.
     * @param serviceOid the OID of the {@link PublishedService} to which the SampleMessage belongs. Pass -1 for all services.
     * @param operationName the name of the operation for which the SampleMessage was saved. Pass null for all operations, or "" for messages that are not categorized by operation name.
     * @return an array of {@link EntityHeader}s. May be empty, but never null.
     */
    EntityHeader[] findSampleMessageHeaders(long serviceOid, String operationName) throws RemoteException, FindException;

    long saveSampleMessage(SampleMessage sm) throws SaveException, RemoteException;

    void deleteSampleMessage(SampleMessage message) throws DeleteException, RemoteException;
}
