package com.l7tech.service;

import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.service.resolution.UrnResolver;
import com.l7tech.service.resolution.SoapActionResolver;
import com.l7tech.service.resolution.ServiceResolutionException;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 26, 2003
 * Time: 1:30:29 PM
 * $Id$
 *
 * Contains cached services and maintains the corresponding service resolution parameters.
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

    public PublishedService resolve(Request req) throws ServiceResolutionException {
        Set services = null;
        try {
            services = getAll();
        } catch (InterruptedException e) {
            throw new ServiceResolutionException(e.getMessage(), e);
        }
        if ( services == null || services.isEmpty() ) return null;
        Set resolvedServices = null;
        // resolve with soapaction first
        resolvedServices = soapResolver.resolve(req, services);
        if (resolvedServices == null || resolvedServices.isEmpty()) return null;
        if (resolvedServices.size() == 1) {
            return (PublishedService)resolvedServices.iterator().next();
        }
        // if necessary, try to resolve with urn
        resolvedServices = urnResolver.resolve(req, services);
        if (resolvedServices == null || resolvedServices.isEmpty()) return null;
        if (resolvedServices.size() == 1) {
            return (PublishedService)resolvedServices.iterator().next();
        } else {
            logger.severe("cache integrity error or resolver bug. this request resolves to more than one service.");
        }
        return null;
    }

    /**
     * adds or update a service to the cache. this should be called when the cache is initially populated and
     * when a service is saved or updated locally
     */
    public void cache(PublishedService service) throws InterruptedException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            Long key = new Long(service.getOid());
            boolean update = false;
            if (services.get(key) != null) update = true;
            services.put(key, service);
            if (update) {
                soapResolver.serviceUpdated(service);
                urnResolver.serviceUpdated(service);
            } else {
                soapResolver.serviceCreated(service);
                urnResolver.serviceCreated(service);
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
     * removes a service from the cache
     * @param service
     */
    public void removeFromCache(PublishedService service) throws InterruptedException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            services.remove(new Long(service.getOid()));
            soapResolver.serviceDeleted(service);
            urnResolver.serviceDeleted(service);
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
            soapResolver.setServices(set);
            urnResolver.setServices(set);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }

    private Map services = new HashMap();
    private ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();
    private final Logger logger = LogManager.getInstance().getSystemLogger();
    private final SoapActionResolver soapResolver = new SoapActionResolver();
    private final UrnResolver urnResolver = new UrnResolver();

    private static class SingletonHolder {
        private static ServiceCache singleton = new ServiceCache();
    }
}
