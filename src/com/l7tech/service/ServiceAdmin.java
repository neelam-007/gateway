package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.PolicyValidatorResult;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.io.IOException;

/**
 * Provides a remote interface for publishing searching and updating published services
 * and service policies.
 *
 * @see PublishedService
 * @see EntityHeader
 * @see Entity
 */
public interface ServiceAdmin extends Remote {
    /**
     * Retrieve all available published services.
     *
     * @return array of entity headers for all existing published services.  May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    EntityHeader[] findAllPublishedServices() throws RemoteException, FindException;

    /**
     * Retrieve a specified published service given its service ID.
     *
     * @param oid the unique identifier of the service
     * @return the requested {@link PublishedService}, or null if no service with that service ID was found
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    PublishedService findServiceByID(long oid) throws RemoteException, FindException;

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
     * @throws ResolutionParameterTooLongException if the service resolution parameters were too long
     */
    long savePublishedService(PublishedService service)
      throws RemoteException, UpdateException, SaveException,
      VersionException, ResolutionParameterTooLongException;

    /**
     * Delete a {@link PublishedService} by its unique identifier.

     * @param oid the unique identifier of the {@link PublishedService} to delete.
     * @throws RemoteException on remote communication error
     * @throws DeleteException if the requested information could not be deleted
     */
    void deletePublishedService(long oid) throws RemoteException, DeleteException;

    /**
     * Get a wsdl document from a URL. The WSDL document will be resolved by the gateway so that the manager
     * can get at services that are 'hidden' behind the gateway.
     * This is meant to be used when a service is originally published.
     *
     * @param url the url that the gateway will use to resolve the wsdl document. this may contain
     * userinfo type credentials
     * @return the contents resolved by this url
     *
     * @throws RemoteException on remote communication error or if the remote service returned something else than 200
     * @throws IOException thrown on I/O error accessing the WSDL url
     * @throws MalformedURLException thrown on malformed WSDL url
     */
    String resolveWsdlTarget(String url) throws RemoteException, IOException, MalformedURLException;

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
}
