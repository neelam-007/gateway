package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.service.resolution.*;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.context.support.ApplicationObjectSupport;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains cached services, with corresponding pre-parsed server-side policies and
 * service statistics.
 * <p/>
 * Entry point for runtime resolution.
 * <p/>
 * Thread safe.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 26, 2003<br/>
 * $Id$
 */
public class ServiceCache extends ApplicationObjectSupport {

    public static final long INTEGRITY_CHECK_FREQUENCY = 4000; // 4 seconds
    private ServerPolicyFactory policyFactory;
    //public static final long INTEGRITY_CHECK_FREQUENCY = 10;

    /**
     * Constructor for bean usage via subclassing.
     */
    public ServiceCache(ServerPolicyFactory policyFactory) {
        if (policyFactory == null) {
            throw new IllegalArgumentException("Policy Factory is required");
        }
        this.policyFactory = policyFactory;
    }

    public synchronized void initiateIntegrityCheckProcess() {
        if (!running) {
            final ServiceCache tasker = this;
            TimerTask task = new TimerTask() {
                public void run() {
                    tasker.checkIntegrity();
                }
            };
            checker.schedule(task, INTEGRITY_CHECK_FREQUENCY, INTEGRITY_CHECK_FREQUENCY);
            running = true;
        }
    }

    /**
     * a service manager can use this to determine whether the cache has been populated or not
     *
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
     *
     * @return Map with Long service oid as a key and Long service version as values
     */
    private Map versionSnapshot() {
        Map output = new HashMap();
        for (Iterator i = services.values().iterator(); i.hasNext();) {
            PublishedService svc = (PublishedService)i.next();
            output.put(new Long(svc.getOid()), new Integer(svc.getVersion()));
        }
        return output;
    }

    /**
     * get pre-parsed root server assertion for cached service
     *
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
    public PublishedService resolve(Message req) throws ServiceResolutionException {
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
    public void cache(PublishedService service) throws InterruptedException {
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

    /**
     * Caller must hold locks protecting {@link #services} and {@link #serverPolicies}
     */
    private void cacheNoLock(PublishedService service) {
        boolean update = false;
        Long key = new Long(service.getOid());
        if (services.get(key) != null) update = true;
        if (update) {
            for (int i = 0; i < resolvers.length; i++) {
                resolvers[i].serviceUpdated(service);
            }
            logger.finest("updated service " + service.getName() + " in cache. oid=" + service.getOid() + " version=" + service.getVersion());
        } else {
            // make sure no duplicate exist
            //validate(service);
            for (int i = 0; i < resolvers.length; i++) {
                resolvers[i].serviceCreated(service);
            }
            logger.finest("added service " + service.getName() + " in cache. oid=" + service.getOid());
        }
        ServerAssertion serverPolicy = null;
        try {
            serverPolicy = policyFactory.makeServerPolicy(service.rootAssertion());
            // cache the service
            services.put(key, service);
            // cache the server policy for this service
            serverPolicies.put(key, serverPolicy);
        } catch (IOException e) {
            // Note, this exception does not passthrough on purpose. Please see bugzilla 958 if you have any issue
            // with this.
            logger.log(Level.SEVERE, "The service whose OID is " + service.getOid() + " cannot be read properly " +
              "and will be discarded from the service cache.", e);
            service.setDisabled(true);
        }
    }

    /**
     * removes a service from the cache
     *
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

    /**
     * Caller must hold locks protecting {@link #services}, {@link #serverPolicies} and {@link #serviceStatistics}.
     */
    private void removeNoLock(PublishedService service) {
        Long key = new Long(service.getOid());
        services.remove(key);
        serverPolicies.remove(key);
        serviceStatistics.remove(key);
        for (int i = 0; i < resolvers.length; i++) {
            resolvers[i].serviceDeleted(service);
        }
        logger.finest("removed service " + service.getName() + " from cache. oid=" + service.getOid());
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
            logger.log(Level.WARNING, "Interrupted while acquiring statistics lock!", e);
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
        checker.cancel();
    }

    private void checkIntegrity() {
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
            Map dbversions = null;

            // get db versions
            try {
                ServiceManager serviceManager = (ServiceManager)getApplicationContext().getBean("serviceManager");
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
            } else {
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
                            ServiceManager serviceManager = (ServiceManager)getApplicationContext().getBean("serviceManager");
                            toUpdateOrAdd = serviceManager.findByPrimaryKey(svcid.longValue());
                        } catch (FindException e) {
                            toUpdateOrAdd = null;
                            logger.log(Level.WARNING, "service scheduled for update or addition" +
                              "cannot be retrieved", e);
                        }
                        if (toUpdateOrAdd != null) {
                            cacheNoLock(toUpdateOrAdd);
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

        } finally {
            if (ciReadLock != null) ciReadLock.release();
        }
    }

    public void setPolicyFactory(ServerPolicyFactory policyFactory) {
        this.policyFactory = policyFactory;
    }

    // the cache data itself
    private final Map services = new HashMap();
    private final Map serverPolicies = new HashMap();
    private final Map serviceStatistics = new HashMap();

    // the resolvers
    private final NameValueServiceResolver[] resolvers = {new HttpUriResolver(), new SoapActionResolver(), new UrnResolver()};

    // read-write lock for thread safety
    private final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();

    private final Logger logger = Logger.getLogger(getClass().getName());

    //private final PeriodicExecutor checker = new PeriodicExecutor( this );
    // TODO replace with Jgroups notifications
    private final Timer checker = new Timer(true); // Don't use Background since this is high priority
    private boolean running = false;
}
