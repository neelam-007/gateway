package com.l7tech.service;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.*;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.ServerPolicyFactory;

import javax.wsdl.WSDLException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 26, 2003
 * Time: 1:30:29 PM
 * $Id$
 *
 * Contains cached services, entry point for resolution.
 * Thread safe.
 *
 */
public class ServiceCache {

    /**
     * get the service cache singleton
     */
    public static ServiceCache getInstance() {
        return SingletonHolder.singleton;
    }

    /**
     * a service manager can use this to determine whether the cache has been populated or not
     * @return the number of services currently cached
     */
    public int size() throws InterruptedException {
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            return services.size();
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
    }

    /**
     * produces a snapshot of the versions of all cached services
     * @return Map with Long service oid as a key and Long service version as values
     */
    public Map versionSnapshot() throws InterruptedException {
        Sync read = rwlock.readLock();
        Map output = new HashMap();
        try {
            read.acquire();
            for (Iterator i = services.values().iterator(); i.hasNext(); ) {
                PublishedService svc = (PublishedService)i.next();
                output.put(new Long(svc.getOid()), new Long(svc.getVersion()));
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
        return output;
    }

    /**
     *
     * @param serviceOid id of the service of which we want the parsed server side root assertion
     * @return
     */
    public ServerAssertion getServerPolicy(long serviceOid) throws InterruptedException {
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            serverPolicies.get(new Long(serviceOid));
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
        return null;
    }

    /**
     * @param req the soap request to resolve the service from
     * @return the cached version of the service that this request resolve to. null if no match
     * @throws ServiceResolutionException
     */
    public PublishedService resolve(Request req) throws ServiceResolutionException {
        Set services = null;
        try {
            services = getAll();
        } catch (InterruptedException e) {
            throw new ServiceResolutionException(e.getMessage(), e);
        }

        if (services == null || services.isEmpty()) {
            logger.finest("resolution failed because no services in the cache");
            return null;
        }

        for (int i = 0; i < resolvers.length; i++) {
            Set resolvedServices = resolvers[i].resolve(req, services);
            if (resolvedServices == null || resolvedServices.isEmpty()) return null;
            if (resolvedServices.size() == 1) {
                logger.finest("service resolved by " + resolvers[i].getClass().getName());
                return (PublishedService)resolvedServices.iterator().next();
            }
            services = resolvedServices;
        }

        if (services == null || services.isEmpty()) {
            logger.fine("resolvers find no match for request");
        } else {
            logger.severe("cache integrity error or resolver bug. this request resolves to more than one service.");
        }

        return null;
    }

    /**
     * adds or update a service to the cache. this should be called when the cache is initially populated and
     * when a service is saved or updated locally
     */
    public void cache(PublishedService service) throws InterruptedException, DuplicateObjectException,
                                                        WSDLException, IOException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            Long key = new Long(service.getOid());
            boolean update = false;
            if (services.get(key) != null) update = true;

            if (update) {
                for (int i = 0; i < resolvers.length; i++) {
                    resolvers[i].serviceUpdated(service);
                }
                logger.finest("updated service in cache. oid=" + service.getOid());
            } else {
                // make sure no duplicate exist
                //validate(service);
                for (int i = 0; i < resolvers.length; i++) {
                    resolvers[i].serviceCreated(service);
                }
                logger.finest("added service in cache. oid=" + service.getOid());
            }
            // cache the service
            services.put(key, service);
            // cache the server policy for this service
            ServerAssertion serverPolicy = ServerPolicyFactory.getInstance().makeServerPolicy(service.rootAssertion());
            serverPolicies.put(key, serverPolicy);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }

    /**
     * removes a service from the cache
     * @param service
     */
    public void removeFromCache(PublishedService service) throws InterruptedException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            Long key = new Long(service.getOid());
            services.remove(key);
            serverPolicies.remove(key);
            for (int i = 0; i < resolvers.length; i++) {
                resolvers[i].serviceDeleted(service);
            }
            logger.finest("removed service from cache. oid=" + service.getOid());
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }

    /**
     * gets a service from the cache
     */
    public PublishedService getCachedService(long oid) throws InterruptedException {
        Sync read = rwlock.readLock();
        PublishedService out = null;
        try {
            read.acquire();
            out = (PublishedService)services.get(new Long(oid));
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
        return out;
    }

    /**
     * get all the services in the cache.
     * these services should not be modified
     * @return a Set containing all PublishedService objects in the cache
     */
    public Set getAll() throws InterruptedException {
        Sync read = rwlock.readLock();
        HashSet set = new HashSet();
        try {
            read.acquire();
            set.addAll(services.values());
            return set;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
    }

    /**
     * switch the cache
     * @param newCache keys are Long with service oid, values are PublishedServices corresponding to key
     */
    public void replaceCache(HashMap newCache) throws InterruptedException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            services = newCache;
            HashSet set = new HashSet();
            set.addAll(services.values());
            for (int i = 0; i < resolvers.length; i++) {
                resolvers[i].setServices(set);
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }

    /**
     * check for potential resolution conflicts
     *
     * @deprecated should use the new resolution table instead of this to ensure uniqueness of resolution parameters
     */
    public void validate(PublishedService candidateService) throws WSDLException, DuplicateObjectException{
        // Make sure WSDL is valid
        candidateService.parsedWsdl();

        Map localServices = services;
        // Check for duplicate services
        for (int i = 0; i < resolvers.length; i++) {
            localServices = resolvers[i].matchingServices(candidateService, localServices);
            if (localServices == null || localServices.isEmpty()) {
                return;
            }
        }
        throw new DuplicateObjectException( "Duplicate service resolution parameters!" );
    }

    private Map services = new HashMap(); // not final because of replaceCache
    private final Map serverPolicies = new HashMap();
    private final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private final NameValueServiceResolver[] resolvers = {new SoapActionResolver(), new UrnResolver()};

    private static class SingletonHolder {
        private static ServiceCache singleton = new ServiceCache();
    }
}
