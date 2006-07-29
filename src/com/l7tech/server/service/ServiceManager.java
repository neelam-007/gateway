package com.l7tech.server.service;

import com.l7tech.common.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

/**
 * Service API. Get instance of this through the Locator class.
 * <p/>
 * <br/><br/>
 * Layer 7 Technologies, inc.<br/>
 * User: flascelles<br/>
 * Date: Jun 6, 2003
 */
public interface ServiceManager extends EntityManager<PublishedService, EntityHeader> {
    /**
     * Get what the server sees at that url.
     * Meant to be used by admin console entity.
     *
     * @param url the url target to inspect.
     * @return the payload returned at that url (hopefully)
     * @throws java.rmi.RemoteException
     */
    String resolveWsdlTarget(String url) throws java.rmi.RemoteException;

    /**
     * updates a policy service. call this instead of save if the service
     * has an history. on the console side implementation, you can call save
     * either way and the oid will dictate whether the object should be saved
     * or updated.
     *
     * @param service
     * @throws UpdateException
     * @throws PolicyAssertionException if a server assertion constructor threw an exception
     */
    void update(PublishedService service) throws UpdateException, VersionException;

    /**
     * returns the parsed server-side policy for a specific PublishedService
     */
    ServerPolicyHandle getServerPolicy(long serviceOid) throws FindException;

    /**
     * resolves to which published service the passed request applies to
     */
    PublishedService resolve(Message req) throws ServiceResolutionException;

    /**
     * returns a ServiceStatistics object for the specified serviceid
     */
    ServiceStatistics getServiceStatistics(long serviceOid) throws FindException;

    /**
     * returns all current ServiceStatistics
     */
    Collection<ServiceStatistics> getAllServiceStatistics() throws FindException;

    void setVisitorClassnames(String visitorClasses);

    @Transactional(propagation= Propagation.SUPPORTS)
    void initiateServiceCache();
}