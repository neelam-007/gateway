package com.l7tech.service;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.util.Locator;
import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.PersistenceContext;
import com.l7tech.objectmodel.TransactionException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.service.resolution.NameValueServiceResolver;
import com.l7tech.service.resolution.ServiceResolutionException;
import com.l7tech.service.resolution.SoapActionResolver;
import com.l7tech.service.resolution.UrnResolver;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Nov 26, 2003
 * Time: 1:30:29 PM
 * $Id$
 *
 * Contains cached services, with corresponding pre-parsed server-side policies and
 * service statistics.
 *
 * Entry point for runtime resolution.
 *
 * Thread safe.
 *
 */
public class ServiceCache {

    public static final long INTEGRITY_CHECK_FREQUENCY = 4000; // 4 seconds

    /**
     * get the service cache singleton
     */
    public static ServiceCache getInstance() {
        return SingletonHolder.singleton;
    }

    public ServiceCache() {
        // uncomment this to turn on the periodic cache integrity check
        checker.start();
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
    private Map versionSnapshot() {
        Map output = new HashMap();
        for (Iterator i = services.values().iterator(); i.hasNext(); ) {
            PublishedService svc = (PublishedService)i.next();
            output.put(new Long(svc.getOid()), new Integer(svc.getVersion()));
        }
        return output;
    }

    /**
     * get pre-parsed root server assertion for cached service
     * @param serviceOid id of the service of which we want the parsed server side root assertion
     * @return
     */
    public ServerAssertion getServerPolicy(long serviceOid) throws InterruptedException {
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            return (ServerAssertion)serverPolicies.get(new Long(serviceOid));
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
    }

    /**
     * @param req the soap request to resolve the service from
     * @return the cached version of the service that this request resolve to. null if no match
     * @throws ServiceResolutionException
     */
    public PublishedService resolve(Request req) throws ServiceResolutionException {
        Set serviceSet = null;
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            serviceSet = new HashSet();
            serviceSet.addAll(services.values());

            if (serviceSet == null || serviceSet.isEmpty()) {
                logger.finest("resolution failed because no services in the cache");
                return null;
            }

            for (int i = 0; i < resolvers.length; i++) {
                Set resolvedServices = resolvers[i].resolve(req, serviceSet);
                if (resolvedServices == null || resolvedServices.isEmpty()) return null;
                if (resolvedServices.size() == 1) {
                    logger.finest("service resolved by " + resolvers[i].getClass().getName());
                    return (PublishedService)resolvedServices.iterator().next();
                }
                serviceSet = resolvedServices;
            }

            if (serviceSet == null || serviceSet.isEmpty()) {
                logger.fine("resolvers find no match for request");
            } else {
                logger.warning("cache integrity error or resolver bug. this request resolves to" +
                        "more than one service. this should be corrected at next cache integrity" +
                        "check");
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw new ServiceResolutionException("Interruption exception in cache", e);
        } finally {
            if (read != null) read.release();
        }

        return null;
    }

    /**
     * adds or update a service to the cache. this should be called when the cache is initially populated and
     * when a service is saved or updated locally
     */
    public void cache(PublishedService service) throws InterruptedException, IOException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            cacheNoLock(service);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }

    private void cacheNoLock(PublishedService service) throws IOException {
        boolean update = false;
        Long key = new Long(service.getOid());
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
    }

    /**
     * removes a service from the cache
     * @param service
     */
    public void removeFromCache(PublishedService service) throws InterruptedException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            removeNoLock(service);
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }

    private void removeNoLock(PublishedService service) {
        Long key = new Long(service.getOid());
        services.remove(key);
        serverPolicies.remove(key);
        serviceStatistics.remove(key);
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i].serviceDeleted(service);
        }
        logger.finest("removed service from cache. oid=" + service.getOid());
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
     * switch the cache
     * @param newServiceCache a set containing the PublishedService objects to cache
     *
    public void replaceCache(Set newServiceCache) throws InterruptedException, IOException {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            services.clear();
            serverPolicies.clear();
            serviceStatistics.clear();
            for (Iterator i = newServiceCache.iterator(); i.hasNext();) {
                PublishedService svc = (PublishedService)i.next();
                cacheNoLock(svc);
            }
            for (int i = 0; i < resolvers.length; i++) {
                resolvers[i].setServices(newServiceCache);
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (write != null) write.release();
        }
    }*/

    /**
     * check for potential resolution conflicts
     *
     * @deprecated should use the new resolution table instead of this to ensure uniqueness of resolution parameters
     *
    public void validate(PublishedService candidateService) throws WSDLException,
                                                              DuplicateObjectException, InterruptedException {
        // Make sure WSDL is valid
        candidateService.parsedWsdl();
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            Map localServices = services;
            // Check for duplicate services
            for (int i = 0; i < resolvers.length; i++) {
                localServices = resolvers[i].matchingServices(candidateService, localServices);
                if (localServices == null || localServices.isEmpty()) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) read.release();
        }
        throw new DuplicateObjectException( "Duplicate service resolution parameters!" );
    }*/

    /**
     * get all current service stats
     */ 
    public Collection getAllServiceStatistics() throws InterruptedException {
        Sync read = null;
        try {
            read = rwlock.readLock();
            read.acquire();
            Collection output = new ArrayList();
            output.addAll(serviceStatistics.values());
            return output;
        } finally {
            if (read != null) {
                read.release();
                read = null;
            }
        }
    }

    /**
     * get statistics for cached service.
     * those stats are lazyly created
     */
    public ServiceStatistics getServiceStatistics(long serviceOid) throws InterruptedException {
        Long oid = new Long(serviceOid);
        ServiceStatistics stats;
        Sync read = null;
        Sync write = null;
        try {
            read = rwlock.readLock();
            read.acquire();
            stats = (ServiceStatistics)serviceStatistics.get(oid);
            if (stats == null) {
                // Upgrade read lock to write lock
                read.release();
                read = null;
                stats = new ServiceStatistics(serviceOid);
                write = rwlock.writeLock();
                write.acquire();
                serviceStatistics.put(oid, stats);
                write.release();
                write = null;
            } else {
                read.release();
                read = null;
            }
            return stats;
        } catch (InterruptedException e) {
            logger.log(Level.WARNING,  "Interrupted while acquiring statistics lock!", e);
            Thread.currentThread().interrupt();
            throw e;
        } finally {
            if (read != null) {
                read.release();
                read = null;
            }
            if (write != null) {
                write.release();
                write = null;
            }
        }
    }

    public void destroy() {
        checker.die();
    }


    /**
     * this is called by the PeriodicIntegrityChecker thread
     */
    private void checkIntegrity(ServiceManagerImp serviceManager) {
        Sync ciReadLock = rwlock.readLock();
        try {
            ciReadLock.acquire();
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "error getting read lock. " +
                                     "this integrity check is stopping prematurely", e);
            return;
        }
        try {
            Map cacheversions = versionSnapshot();
            PersistenceContext context = null;
            try {
                context = PersistenceContext.getCurrent();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "error getting persistence context. " +
                                         "this integrity check is stopping prematurely", e);
                return;
            }
            try {
                Map dbversions = null;
                // begin transaction
                try {
                    context.beginTransaction();
                } catch (TransactionException e) {
                    logger.log(Level.SEVERE, "error begining transaction. " +
                                             "this integrity check is stopping prematurely", e);
                    return;
                }

                // get db versions
                try {
                    dbversions = serviceManager.getServiceVersions();
                } catch (FindException e) {
                    logger.log(Level.SEVERE, "error getting versions. " +
                                             "this integrity check is stopping prematurely", e);
                    return;
                }

                // actual check logic
                ArrayList updatesAndAdditions = new ArrayList();
                ArrayList deletions = new ArrayList();
                // 1. check that all that is in db is present in cache and that version is same
                for (Iterator i = dbversions.keySet().iterator(); i.hasNext();) {
                    Long dbid = (Long)i.next();
                    // is it already in cache?
                    Integer cacheversion = (Integer)cacheversions.get(dbid);
                    if (cacheversion == null) {
                        logger.fine("service " + dbid + " to be added to cache.");
                        updatesAndAdditions.add(dbid);
                    } else {
                        // check actual version
                        Integer dbversion = (Integer)dbversions.get(dbid);
                        if (!dbversion.equals(cacheversion)) {
                            updatesAndAdditions.add(dbid);
                            logger.fine("service " + dbid + " to be updated in cache because outdated.");
                        }

                    }
                }
                // 2. check for things in cache not in db (deletions)
                for (Iterator i = cacheversions.keySet().iterator(); i.hasNext();) {
                    Long cacheid = (Long)i.next();
                    if (dbversions.get(cacheid) == null) {
                        deletions.add(cacheid);
                        logger.fine("service " + cacheid + " to be deleted from cache because no longer in database.");
                    }
                }

                // 3. make the updates
                if (updatesAndAdditions.isEmpty() && deletions.isEmpty()) {
                    // nothing to do. we're done
                    ciReadLock.release();
                    ciReadLock = null;
                }
                else {
                    Sync ciWriteLock = rwlock.writeLock();
                    ciReadLock.release();
                    ciReadLock = null;
                    try {
                        ciWriteLock.acquire();
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "could not get write lock. this integrity" +
                                                 "check is stopping prematurely", e);
                        return;
                    }
                    try {
                        for (Iterator i = updatesAndAdditions.iterator(); i.hasNext();) {
                            Long svcid = (Long)i.next();
                            PublishedService toUpdateOrAdd = null;
                            try {
                                toUpdateOrAdd = serviceManager.findByPrimaryKey(svcid.longValue());
                            } catch (FindException e) {
                                toUpdateOrAdd = null;
                                logger.log(Level.WARNING, "service scheduled for update or addition" +
                                                          "cannot be retrieved", e);
                            }
                            if (toUpdateOrAdd != null) {
                                try {
                                    cacheNoLock(toUpdateOrAdd);
                                } catch (IOException e) {
                                    logger.log(Level.WARNING, "exception updating cache", e);
                                }
                            } // otherwise, next integrity check shall delete this service from cache
                        }
                        for (Iterator i = deletions.iterator(); i.hasNext();) {
                            Long key = (Long)i.next();
                            PublishedService serviceToDelete = (PublishedService)services.get(key);
                            removeNoLock(serviceToDelete);
                        }
                    } finally {
                        ciWriteLock.release();
                    }
                }

                // close hib transaction
                try {
                    context.rollbackTransaction();
                } catch (TransactionException e) {
                    logger.log(Level.WARNING, "error rollbacking transaction", e);
                }
            } finally {
                context.close();
            }
        } finally {
            if (ciReadLock != null) ciReadLock.release();
        }
    }

    private class PeriodicIntegrityChecker extends Thread {
        public void run() {
            try {
                sleep(INTEGRITY_CHECK_FREQUENCY*2);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "interruption", e);
                return;
            }
            logger.finest("initiating cache integrity check process");
            // get the service manager
            ServiceManager locmanager = (ServiceManager)Locator.getDefault().lookup(ServiceManager.class);
            if (locmanager != null && locmanager instanceof ServiceManagerImp) {
                serviceManager = (ServiceManagerImp)locmanager;
            } else {
                logger.severe("cannot resolve a service manager");
                return;
            }
            while (true) {
                if (die) break;
                try {
                    sleep(INTEGRITY_CHECK_FREQUENCY);
                } catch (InterruptedException e) {
                    logger.log(Level.SEVERE, "interruption", e);
                    break;
                }
                try {
                    checkIntegrity(serviceManager);
                } catch (Throwable e) {
                    logger.log(Level.WARNING, "unhandled exception", e);
                }
                lastCheck = System.currentTimeMillis();
            }
            logger.finest("cache integrity check process stopping");
        }

        public long getLastCheck() {
            return lastCheck;
        }
        public void die() {
            die = true;
        }

        private long lastCheck = -1;
        private boolean die = false;
        private ServiceManagerImp serviceManager = null;
    }

    private static class SingletonHolder {
        private static ServiceCache singleton = new ServiceCache();
    }

    // the cache data itself
    private final Map services = new HashMap();
    private final Map serverPolicies = new HashMap();
    private final Map serviceStatistics = new HashMap();

    // the resolvers
    private final NameValueServiceResolver[] resolvers = {new SoapActionResolver(), new UrnResolver()};

    // read-write lock for thread safety
    private final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();

    private final Logger logger = LogManager.getInstance().getSystemLogger();

    private final PeriodicIntegrityChecker checker = new PeriodicIntegrityChecker();
}
