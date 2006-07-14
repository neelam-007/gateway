package com.l7tech.server.service;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Background;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.common.LicenseException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.UnknownAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.xml.SchemaValidation;
import com.l7tech.server.policy.ServerPolicy;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.service.resolution.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

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
 */
public class ServiceCache extends ApplicationObjectSupport implements DisposableBean, ApplicationListener {

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

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof LicenseEvent) {
            Background.scheduleOneShot(new TimerTask() {
                public void run() {
                    resetUnlicensed();
                }
            }, 0);
        }
    }

    private void resetUnlicensed() {
        Sync write = rwlock.writeLock();
        try {
            write.acquire();
            List<Long> unlicensed = new ArrayList<Long>(servicesThatAreUnlicensed);

            int numUnlicensed = unlicensed.size();
            if (numUnlicensed < 0) return;
            logger.info("License has changed -- resetting " + numUnlicensed + " affected services");
            for (Long oid : unlicensed) {
                PublishedService service = services.get(oid);
                if (service == null) continue; // no longer relevant

                removeNoLock(service);
                try {
                    cacheNoLock(service);
                } catch (ServerPolicyException e) {
                    logger.log(Level.WARNING, "Unable to reenable service after license change: " + service.getName() + ": " + ExceptionUtils.getMessage(e), e);
                }
            }
        } catch (InterruptedException e) {
            logger.log(Level.WARNING, "interruption in service cache", e);
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } finally {
            if (write != null) write.release();
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
    private Map<Long, Integer> versionSnapshot() {
        Map<Long, Integer> output = new HashMap<Long, Integer>();
        for (PublishedService svc : services.values()) {
            output.put(svc.getOid(), svc.getVersion());
        }
        return output;
    }

    /**
     * get pre-parsed root server assertion for cached service
     *
     * @param serviceOid id of the service of which we want the parsed server side root assertion
     */
    public ServerPolicyHandle getServerPolicy(long serviceOid) throws InterruptedException {
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            ServerPolicyHandle handle = serverPolicies.get(serviceOid);
            if (handle == null) return null;
            return handle.getTarget().ref();
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
        Set<PublishedService> serviceSet;
        Sync read = rwlock.readLock();
        try {
            read.acquire();
            serviceSet = new HashSet<PublishedService>();
            serviceSet.addAll(services.values());

            if (serviceSet.isEmpty()) {
                logger.finest("resolution failed because no services in the cache");
                return null;
            }
            for (NameValueServiceResolver resolver : resolvers) {
                Set<PublishedService> resolvedServices;
                boolean passthrough = false;
                try {
                    resolvedServices = resolver.resolve(req, serviceSet);
                } catch (NoServiceOIDResolutionPassthroughException e) {
                    resolvedServices = serviceSet;
                    passthrough = true;
                }

                int newResolvedServicesSize = 0;
                if (resolvedServices != null) {
                    newResolvedServicesSize = resolvedServices.size();
                }

                // if remaining services are 0 or 1, we are done
                if (newResolvedServicesSize == 1 && !passthrough) {
                    logger.finest("service resolved by " + resolver.getClass().getName());
                    return resolvedServices.iterator().next();
                } else if (newResolvedServicesSize == 0) {
                    logger.info("resolver " + resolver.getClass().getName() + " eliminated all possible services");
                    return null;
                }

                // otherwise, try to narrow down further using next resolver
                serviceSet = resolvedServices;
            }

            if (serviceSet.isEmpty()) {
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
     * @throws ServerPolicyException if a server assertion contructor for this service threw an exception
     */
    public void cache(PublishedService service) throws InterruptedException, ServerPolicyException {
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
     * @throws ServerPolicyException if a server policy tree can't be created for this service
     */
    private void cacheNoLock(PublishedService service) throws ServerPolicyException {
        boolean update = false;
        Long key = service.getOid();
        if (services.get(key) != null) update = true;
        if (update) {
            for (NameValueServiceResolver resolver : resolvers) {
                resolver.serviceUpdated(service);
            }
            logger.finest("updated service " + service.getName() + " in cache. oid=" + service.getOid() + " version=" + service.getVersion());
        } else {
            // make sure no duplicate exist
            //validate(service);
            for (NameValueServiceResolver resolver : resolvers) {
                resolver.serviceCreated(service);
            }
            logger.finest("added service " + service.getName() + " in cache. oid=" + service.getOid());
        }
        ServerAssertion serverRootAssertion;
        try {
            // cache the service
            PublishedService oldService = services.put(key, service);
            if (oldService != service) {
                final ServerPolicyHandle handle = serverPolicies.get(key);
                if (handle != null) {
                    TimerTask runnable = new TimerTask() {
                        public void run() {
                            handle.close();
                        }
                    };
                    Background.scheduleOneShot(runnable, 0);
                }
            }
            // cache the server policy for this service
            try {
                serverRootAssertion = policyFactory.compilePolicy(service.rootAssertion(), true);
                servicesThatAreUnlicensed.remove(service.getOid());
            } catch (final LicenseException e) {
                serverRootAssertion = new AbstractServerAssertion<UnknownAssertion>(new UnknownAssertion()) {
                    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws PolicyAssertionException {
                        throw new PolicyAssertionException(getAssertion(), "Assertion not available: " + ExceptionUtils.getMessage(e));
                    }
                };
                servicesThatAreUnlicensed.add(service.getOid());
            }
            if (serverRootAssertion != null) {
                serverPolicies.put(key, new ServerPolicy(serverRootAssertion).ref());
            } else {
                logger.log(Level.SEVERE, "Service '" + service.getName() + "' (#" + service.getOid() + ") will be disabled; it has an unsupported policy format.");
                service.setDisabled(true);
            }
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
        Long key = service.getOid();
        services.remove(key);
        serverPolicies.remove(key);
        serviceStatistics.remove(key);
        for (NameValueServiceResolver resolver : resolvers) {
            resolver.serviceDeleted(service);
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
            out = services.get(oid);
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
    public Collection<ServiceStatistics> getAllServiceStatistics() throws InterruptedException {
        Sync read = null;
        try {
            read = rwlock.readLock();
            read.acquire();
            Collection<ServiceStatistics> output = new ArrayList<ServiceStatistics>();
            output.addAll(serviceStatistics.values());
            return output;
        } finally {
            if (read != null) {
                read.release();
            }
        }
    }

    /**
     * get statistics for cached service.
     * those stats are lazyly created
     */
    public ServiceStatistics getServiceStatistics(long serviceOid) throws InterruptedException {
        ServiceStatistics stats;
        Sync read = null;
        Sync write = null;
        try {
            read = rwlock.readLock();
            read.acquire();
            stats = serviceStatistics.get(serviceOid);
            if (stats == null) {
                // Upgrade read lock to write lock
                read.release();
                read = null;
                stats = new ServiceStatistics(serviceOid);
                write = rwlock.writeLock();
                write.acquire();
                serviceStatistics.put(serviceOid, stats);
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
            if (read != null) read.release();
            if (write != null) write.release();
        }
    }

    /**
     * {@see DisaposableBean}
     * @throws Exception
     */
    public void destroy() throws Exception {
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
            Map<Long, Integer> cacheversions = versionSnapshot();
            Map<Long, Integer> dbversions;

            // get db versions
            try {
                ServiceManager serviceManager = (ServiceManager)getApplicationContext().getBean("serviceManager");
                dbversions = serviceManager.findVersionMap();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "error getting versions. " +
                  "this integrity check is stopping prematurely", e);
                return;
            }

            // actual check logic
            ArrayList<Long> updatesAndAdditions = new ArrayList<Long>();
            ArrayList<Long> deletions = new ArrayList<Long>();
            // 1. check that all that is in db is present in cache and that version is same
            for (Long dbid : dbversions.keySet()) {
                // is it already in cache?
                Integer cacheversion = cacheversions.get(dbid);
                if (cacheversion == null) {
                    logger.info("service " + dbid + " to be added to cache.");
                    updatesAndAdditions.add(dbid);
                } else {
                    // check actual version
                    Integer dbversion = dbversions.get(dbid);
                    if (!dbversion.equals(cacheversion)) {
                        updatesAndAdditions.add(dbid);
                        logger.info("service " + dbid + " to be updated in cache because outdated.");
                    }

                }
            }
            // 2. check for things in cache not in db (deletions)
            for (Long cacheid : cacheversions.keySet()) {
                if (dbversions.get(cacheid) == null) {
                    deletions.add(cacheid);
                    logger.info("service " + cacheid + " to be deleted from cache because no longer in database.");
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
                    for (Long svcid : updatesAndAdditions) {
                        PublishedService toUpdateOrAdd;
                        try {
                            ServiceManager serviceManager = (ServiceManager) getApplicationContext().getBean("serviceManager");
                            toUpdateOrAdd = serviceManager.findByPrimaryKey(svcid);
                        } catch (FindException e) {
                            toUpdateOrAdd = null;
                            logger.log(Level.WARNING, "service scheduled for update or addition" +
                                    "cannot be retrieved", e);
                        }
                        if (toUpdateOrAdd != null) {
                            final Long oid = toUpdateOrAdd.getOid();
                            try {
                                final Integer throwingVersion = servicesThatAreThrowing.get(oid);
                                if (throwingVersion == null || throwingVersion != toUpdateOrAdd.getVersion())
                                {
                                    // Try to cache it again
                                    cacheNoLock(toUpdateOrAdd);
                                    if (throwingVersion != null) {
                                        logger.log(Level.INFO, "Policy for service #" + oid + " is no longer invalid");
                                        servicesThatAreThrowing.remove(oid);
                                    }
                                }
                            } catch (ServerPolicyException e) {
                                logger.log(Level.WARNING, "Policy for service #" + oid + " is invalid: " + ExceptionUtils.getMessage(e), e);
                                servicesThatAreThrowing.put(oid, toUpdateOrAdd.getVersion());
                            }
                        } // otherwise, next integrity check shall delete this service from cache
                    }
                    // Trigger xpath compilation if the set of registered xpaths has changed
                    TarariLoader.compile();
                    // todo, need to check for right schemas there as well
                    for (Long key : deletions) {
                        PublishedService serviceToDelete = services.get(key);
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

    /**
     * assumes you already have a lock through new policy contruction
     */
    public Collection<String> getAllPolicySchemas() {
        ArrayList<String> output = new ArrayList<String>();
        for (PublishedService publishedService : services.values()) {
            try {
                Assertion root = publishedService.rootAssertion();
                slurpSchemas(root, output);
            } catch (IOException e) {
                logger.log(Level.SEVERE, "cannot parse policy(?!)", e);
                return output;
            }
        }
        return output;
    }

    private void slurpSchemas(Assertion toInspect, ArrayList<String> container) {
        if (toInspect instanceof CompositeAssertion) {
            CompositeAssertion ca = (CompositeAssertion)toInspect;
            for (Iterator i = ca.children(); i.hasNext();) {
                Assertion a = (Assertion)i.next();
                slurpSchemas(a, container);
            }
        } else if (toInspect instanceof SchemaValidation) {
            SchemaValidation tq = (SchemaValidation)toInspect;
            if (tq.getResourceInfo() instanceof StaticResourceInfo) {
                StaticResourceInfo sri = (StaticResourceInfo)tq.getResourceInfo();
                String value = sri.getDocument();
                if (value != null) {
                    container.add(value);
                }
            }
        }
    }


    // the cache data itself
    private final Map<Long, PublishedService> services = new HashMap<Long, PublishedService>();
    private final Map<Long, ServerPolicyHandle> serverPolicies = new HashMap<Long, ServerPolicyHandle>();
    private final Map<Long, ServiceStatistics> serviceStatistics = new HashMap<Long, ServiceStatistics>();
    private final Map<Long, Integer> servicesThatAreThrowing = new HashMap<Long, Integer>();
    private final Set<Long> servicesThatAreUnlicensed = new HashSet<Long>();

    // the resolvers
    private final NameValueServiceResolver[] resolvers = {new OriginalUrlServiceOidResolver(), new HttpUriResolver(), new SoapActionResolver(), new UrnResolver()};

    // read-write lock for thread safety
    private final ReadWriteLock rwlock = new WriterPreferenceReadWriteLock();

    private final Logger logger = Logger.getLogger(getClass().getName());

    //private final PeriodicExecutor checker = new PeriodicExecutor( this );
    // TODO replace with Jgroups notifications
    private final Timer checker = new Timer(true); // Don't use Background since this is high priority
    private boolean running = false;
}
