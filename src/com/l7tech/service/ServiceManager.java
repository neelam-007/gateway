package com.l7tech.service;

import com.l7tech.objectmodel.*;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.ServiceResolutionException;

import java.util.Map;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: Jun 6, 2003
 *
 * Service API. Get instance of this through the Locator class.
 */
public interface ServiceManager extends EntityManager {
    /**
     * Retreive the actual PublishedService object from it's oid.
     *
     * @param oid
     * @return
     * @throws FindException
     */
    PublishedService findByPrimaryKey( long oid ) throws FindException;

    /**
     * Get what the server sees at that url.
     * Meant to be used by admin console entity.
     * @param url the url target to inspect.
     * @return the payload returned at that url (hopefully)
     * @throws java.rmi.RemoteException
     */
    String resolveWsdlTarget(String url) throws java.rmi.RemoteException;

    /**
     * saves a published service along with it's policy assertions
     * @param service
     * @return
     * @throws SaveException
     */
    long save( PublishedService service ) throws SaveException;

    public int getCurrentPolicyVersion(long policyId) throws FindException; 

    /**
     * updates a policy service. call this instead of save if the service
     * has an history. on the console side implementation, you can call save
     * either way and the oid will dictate whether the object should be saved
     * or updated.
     *
     * @param service
     * @throws UpdateException
     */
    void update( PublishedService service ) throws UpdateException, VersionException;

    /**
     * deletes the service
     * @param service
     * @throws DeleteException
     */
    void delete( PublishedService service ) throws DeleteException;

    /**
     * called at run time to discover which service is being invoked based
     * on the request headers and/or document.
     * @param request
     * @return
     */
    PublishedService resolveService( Request request ) throws ServiceResolutionException;

    void addServiceListener( ServiceListener listener );

    /** Returns an unmodifiable Map of service OIDs to cached PublishedService instances. */
    Map serviceMap();
}