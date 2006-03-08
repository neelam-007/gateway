/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 */

package com.l7tech.service;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.RemoteException;

/**
 * Defines the operations to be exposed as admin web service.
 *
 * IMPORTANT: Once the web service is published, backward compatibility must
 *            be maintaned. Therefore, do not remove or modify existing
 *            method declarations.
 */
public interface ServiceAdminPublic {
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
    PublishedService findServiceByID(String oid) throws RemoteException, FindException;

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
     * @param serviceoid id of the service to publish on the systinet registry
     * @return registrykey
     */
    String publishToSystinetRegistry(String serviceoid) throws RemoteException, FindException;
}
