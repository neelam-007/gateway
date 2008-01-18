package com.l7tech.server.service;

import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.audit.SystemMessages;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.SoapKnob;
import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.util.Decorator;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.TarariLoader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.event.system.ServiceReloadEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.policy.*;
import com.l7tech.server.service.resolution.*;
import com.l7tech.service.PublishedService;
import com.l7tech.service.ServiceStatistics;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.*;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.MessageFormat;

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
public class ServiceCache
        extends ApplicationObjectSupport
        implements InitializingBean, DisposableBean, ApplicationListener
{
    private static final Logger logger = Logger.getLogger(ServiceCache.class.getName());

    public static final long INTEGRITY_CHECK_FREQUENCY = 4000; // 4 seconds

    // the cache data itself
    private final Map<Long, PublishedService> services = new HashMap<Long, PublishedService>();
    private final Map<Long, ServiceStatistics> serviceStatistics = new HashMap<Long, ServiceStatistics>();
    private final Map<Long, String> servicesThatAreThrowing = new HashMap<Long, String>();
    private final Collection<Decorator<PublishedService>> decorators;

    private final PlatformTransactionManager transactionManager;
    private final ServiceManager serviceManager;

    // TODO replace with Jgroups notifications
    private final Timer checker; // Don't use Background since this is high priority
    // read-write lock for thread safety
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock(false);

    /**
     * Resolvers that are used to actually resolve services.
     * Not final due to Spring requirement -- see {@link #initApplicationContext}
     */
    private ServiceResolver[] activeResolvers;

    /**
     * Resolvers that are notified of CRUD events on services.
     * Not final due to Spring requirement -- see {@link #initApplicationContext}
     */
    private ServiceResolver[] notifyResolvers;

    //private final PeriodicExecutor checker = new PeriodicExecutor( this );
    private boolean running = false;

    private Auditor auditor;
    private boolean hasCatchAllService = false;
    private SoapOperationResolver soapOperationResolver;
    private final PolicyCache policyCache;


    /**
     * Constructor for bean usage via subclassing.
     */
    public ServiceCache(PolicyCache policyCache,
                        PlatformTransactionManager transactionManager,
                        ServiceManager serviceManager,
                        Collection<Decorator<PublishedService>> decorators,
                        Timer timer)
    {
        if (policyCache == null) throw new IllegalArgumentException("Policy Cache is required");
        if (timer == null) timer = new Timer("Service cache refresh", true);

        this.policyCache = policyCache;
        if (decorators == null)
            this.decorators = Collections.emptyList();
        else
            this.decorators = decorators;
        this.checker = timer;
        this.transactionManager = transactionManager;
        this.serviceManager = serviceManager;
    }

    @Override
    protected void initApplicationContext() throws BeansException {
        ApplicationContext spring = getApplicationContext();

        activeResolvers = new ServiceResolver[] {
            new OriginalUrlServiceOidResolver(spring),
            new UriResolver(spring),
            new SoapActionResolver(spring),
            new UrnResolver(spring),
        };

        soapOperationResolver = new SoapOperationResolver(spring);

        ServiceResolver[] allResolvers = new ServiceResolver[activeResolvers.length+1];
        System.arraycopy(activeResolvers, 0, allResolvers, 0, activeResolvers.length);
        allResolvers[activeResolvers.length] = soapOperationResolver;
        this.notifyResolvers = allResolvers;
    }

    public synchronized void initiateIntegrityCheckProcess() {
        if (!running) {
            final ServiceCache tasker = this;
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    try {
                        tasker.checkIntegrity();
                    } catch(Exception e) {
                        logger.log(Level.WARNING, "Error checking service cache integrity.", e);
                    }
                }
            };
            checker.schedule(task, INTEGRITY_CHECK_FREQUENCY, INTEGRITY_CHECK_FREQUENCY);
            running = true;
        }
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof ServiceCacheEvent.Updated) {
            doCacheUpdate((ServiceCacheEvent.Updated) applicationEvent);
        } else if (applicationEvent instanceof ServiceCacheEvent.Deleted) {
            doCacheDelete((ServiceCacheEvent.Deleted) applicationEvent);
        } else if (applicationEvent instanceof PolicyCacheEvent.Invalid) {
            PolicyCacheEvent.Invalid invalidEvent = (PolicyCacheEvent.Invalid) applicationEvent;
            final Lock read = rwlock.readLock();
            read.lock();
            try {
                handleInvalidPolicy(null, invalidEvent.getPolicyId(), invalidEvent.getException());
            } finally {
                read.unlock();
            }
        } else if (applicationEvent instanceof PolicyCacheEvent.Updated) {
            PolicyCacheEvent.Updated validEvent = (PolicyCacheEvent.Updated) applicationEvent;
            final Lock read = rwlock.readLock();
            read.lock();
            try {
                handleValidPolicy(validEvent.getPolicy());
            } finally {
                read.unlock();
            }
        } else if (applicationEvent instanceof Started) {
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult(TransactionStatus status) {
                    try {
                        initializeServiceCache();
                    } catch (ObjectModelException e) {
                        throw new RuntimeException("Error intializing service cache", e);
                    }
                }
            });
        }
    }

    private void doCacheDelete(final ServiceCacheEvent.Deleted deleted) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    removeFromCache(deleted.getService());
                }
            }
        });
    }

    private void doCacheUpdate(final ServiceCacheEvent.Updated updated) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCompletion(int status) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) {
                    // get service. version property must be up-to-date
                    PublishedService svcnow;
                    try {
                        svcnow = serviceManager.findByPrimaryKey(updated.getService().getOid());
                    } catch (FindException e) {
                        svcnow = null;
                        logger.log(Level.WARNING, "could not get service back", e);
                    }

                    if (svcnow != null) {
                        try {
                            cache(svcnow);
                            TarariLoader.compile();
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
                        }
                    }
                }
            }
        });
    }

    private void initializeServiceCache() throws ObjectModelException {
        // build the cache if necessary
        try {
            if (size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.info("building service cache");
                Collection<PublishedService> services = serviceManager.findAll();
                for (PublishedService service : services) {
                    try {
                        // cache a copy not bound to hibernate session
                        cache(  new PublishedService( service ) );
                    } catch (ServerPolicyException e) {
                        Assertion ass = e.getAssertion();

                        String ordinal = ass == null ? "" : "#" + Integer.toString(ass.getOrdinal());
                        String what = ass == null ? "<unknown>" : "(" + ass.getClass().getSimpleName() + ")";
                        String msg = MessageFormat.format( "Disabling PublishedService #{0} ({1}); policy could not be compiled (assertion {2} {3})",
                                        service.getOid(), service.getName(), ordinal, what );
                        logger.log(Level.WARNING, msg, e);
                        // We don't actually disable the service here -- only the admin should be doing that.
                        // Instead, we will let the service cache continue to monitor the situation
                    } catch (Exception e) {
                        String msg = MessageFormat.format( "Disabling PublishedService #{0} ({1}); policy could not be compiled",
                                        service.getOid(), service.getName() );
                        logger.log(Level.WARNING, msg, e);
                    }
                }
                TarariLoader.compile();
            }
            // make sure the integrity check is running
            logger.info("initiate service cache version check process");
            initiateIntegrityCheckProcess();
        } catch (Exception e) {
            throw new ObjectModelException("Exception building cache", e);
        }
    }


    /**
     * a service manager can use this to determine whether the cache has been populated or not
     *
     * @return the number of services currently cached
     */
    public int size() {
        rwlock.readLock().lock();
        try {
            return services.size();
        } finally {
            rwlock.readLock().unlock();
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
    public ServerPolicyHandle getServerPolicy(long serviceOid) {
        rwlock.readLock().lock();
        try {
            PublishedService service = services.get( serviceOid );
            if (service == null) return null;
            return getServerPolicyForService( service );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    public static interface ResolutionListener {
        boolean notifyPreParseServices(Message message, Set<PolicyMetadata> serviceSet);
    }

    /**
     * @param req the soap request to resolve the service from
     * @return the cached version of the service that this request resolve to. null if no match
     * @throws ServiceResolutionException
     */
    public PublishedService resolve(Message req, ResolutionListener rl) throws ServiceResolutionException {
        Set<PublishedService> serviceSet;
        rwlock.readLock().lock();
        try {
            serviceSet = new HashSet<PublishedService>();
            serviceSet.addAll(services.values());

            if (serviceSet.isEmpty()) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NO_SERVICES);
                return null;
            }

            boolean notified = false;
            for (ServiceResolver resolver : activeResolvers) {
                if (rl != null && resolver.usesMessageContent() && !notified) {
                    notified = true;
                    if (!rl.notifyPreParseServices(req, getPolicyMetadata(serviceSet)))
                        return null;
                }

                Set<PublishedService> resolvedServices;
                Result result = resolver.resolve(req, serviceSet);
                if (result == Result.NOT_APPLICABLE) {
                    // next resolver gets the same subset
                    continue;
                } else if (result == Result.NO_MATCH) {
                    // Early failure
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_FAILED_EARLY, resolver.getClass().getSimpleName());
                    return null;
                } else {
                    // Matched at least one... Next resolver can narrow it down
                    resolvedServices = result.getMatches();
                }

                int size = resolvedServices.size();
                // if remaining services are 0 or 1, we are done
                if (size == 1) {
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED_EARLY, resolver.getClass().getSimpleName());
                    serviceSet = resolvedServices;
                    break;
                } else if (size == 0) {
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_FAILED_EARLY, resolver.getClass().getSimpleName());
                    return null;
                }

                // otherwise, try to narrow down further using next resolver
                serviceSet = resolvedServices;
            }

            if (serviceSet.isEmpty()) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NO_MATCH);
                return null;
            } else if (serviceSet.size() == 1) {
                if (rl != null && !notified) {
                    if (!rl.notifyPreParseServices(req, getPolicyMetadata(serviceSet)))
                        return null;
                }

                PublishedService service = serviceSet.iterator().next();
                XmlKnob xk = (XmlKnob) req.getKnob(XmlKnob.class);

                if (!service.isSoap() || service.isLaxResolution()) {
                    if (xk != null) xk.setTarariWanted(getPolicyMetadata(service.getPolicy()).isTarariWanted());
                    return service;
                }

                // If this service is set to strict mode, validate that the message is soap, and that it matches an
                // operation supported in the WSDL.
                if (req.getKnob(SoapKnob.class) == null) {
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NOT_SOAP);
                    return null;
                } else {
                    // avoid re-Tarari-ing request that's already DOM parsed unless some assertions need it bad
                    if (xk != null) xk.setTarariWanted(getPolicyMetadata(service.getPolicy()).isTarariWanted());
                    Result services = soapOperationResolver.resolve(req, serviceSet);
                    if (services.getMatches().isEmpty()) {
                        auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_OPERATION_MISMATCH, service.getName(), service.getId());
                        return null;
                    } else {
                        auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED, service.getName(), service.getId());
                        return service;
                    }
                }
            } else {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_MULTI);
                return null;
            }
        } finally {
            rwlock.readLock().unlock();
        }
    }

    private Set<PolicyMetadata> getPolicyMetadata( final Set<PublishedService> services ) {
        Set<PolicyMetadata> metadata = new LinkedHashSet<PolicyMetadata>();

        for ( PublishedService service : services ) {
            metadata.add( getPolicyMetadata( service.getPolicy() ) );              
        }

        return metadata;
    }

    /**
     * User should hold lock.
     */
    private PolicyMetadata getPolicyMetadata( final Policy policy ) {
        PolicyMetadata metadata = null;
        ServerPolicyHandle sph = null;
        try {
            sph = policyCache.getServerPolicy( policy );
            if ( sph != null ) {
                metadata = sph.getPolicyMetadata();
            }
        } finally {
            ResourceUtils.closeQuietly( sph );
        }

        if ( metadata == null ) {
            // use defaults
            metadata = new PolicyMetadata() {
                public boolean isTarariWanted() { return false; }
                public boolean isWssInPolicy() { return false; }
                public boolean isMultipart() { return false; }
            };
        }

        return metadata;
    }

    /**
     * adds or update a service to the cache. this should be called when the cache is initially populated and
     * when a service is saved or updated locally
     * @throws ServerPolicyException if a server assertion contructor for this service threw an exception
     */
    public void cache(PublishedService service) throws ServerPolicyException {
        rwlock.writeLock().lock();
        try {
            cacheNoLock(service);
            updateCatchAll();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    /**
     * Caller must hold lock protecting {@link #services}
     */
    private void cacheNoLock(PublishedService publishedService) throws ServerPolicyException {
        boolean update = false;
        PublishedService service = decorate(publishedService);
        Long key = service.getOid();
        if (services.get(key) != null) update = true;
        if (update) {
            for (ServiceResolver resolver : notifyResolvers) {
                try {
                    resolver.serviceUpdated(service);
                } catch (ServiceResolutionException sre) {
                    auditor.logAndAudit(SystemMessages.SERVICE_WSDL_ERROR,
                            new String[]{service.displayName(), sre.getMessage()}, sre);
                }
            }
            logger.finest("updated service " + service.getName() + " in cache. oid=" + service.getOid() + " version=" + service.getVersion());
        } else {
            // make sure no duplicate exist
            //validate(service);
            for (ServiceResolver resolver : notifyResolvers) {
                try {
                    resolver.serviceCreated(service);
                } catch (ServiceResolutionException sre) {
                    auditor.logAndAudit(SystemMessages.SERVICE_WSDL_ERROR,
                            new String[]{service.displayName(), sre.getMessage()}, sre);
                }
            }
            logger.finest("added service " + service.getName() + " in cache. oid=" + service.getOid());
        }

        // cache the service
        services.put(key, service);

        // cache the server policy for this service
        final Policy policy = service.getPolicy();
        if (policy == null) throw new ServerPolicyException(null, "Service #" + service.getOid() + " (" + service.getName() + ") has no policy");
        policyCache.update(policy);
    }

    /**
     * removes a service from the cache
     *
     * @param service The service to remove from the cache
     */
    public void removeFromCache(PublishedService service) {
        rwlock.writeLock().lock();
        try {
            removeNoLock(service);
            updateCatchAll();
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private ServerPolicyHandle getServerPolicyForService(final PublishedService service) {
        return policyCache.getServerPolicy( service.getPolicy() );
    }

    private void handleInvalidPolicy( final PublishedService publishedService,
                                      final Long policyId,
                                      final Exception exception ) {
        PublishedService service = publishedService;

        if ( service == null ) {
            service = getCachedServiceByPolicy( policyId );    
        }

        // if it is not cached then it will be disabled later
        if ( service != null && !service.isDisabled() ) {
            if ( exception instanceof ServerPolicyInstantiationException ) {
                auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_BAD_POLICY_FORMAT, service.getName(), service.getId() );
                service.setDisabled(true);
            } else {
                auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_BAD_POLICY, service.getName(), service.getId() );
                auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_DISABLING_SERVICE, service.getId(), service.getName() );
                service.setDisabled(true);
            }
        }
    }

    private void handleValidPolicy( final Policy policy  ) {
        PublishedService service = getCachedServiceByPolicy( policy.getOid() );

        // if it is not cached then it will be disabled later
        if ( service != null && service.isDisabled() ) {
            auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_ENABLING_SERVICE, service.getName(), service.getId() );
            service.setDisabled(false);
        }
    }

    /**
     * Caller must hold a lock protecting {@link #services} and {@link #serviceStatistics}.
     */
    private void removeNoLock(PublishedService service) {
        Long key = service.getOid();
        services.remove(key);
        policyCache.remove( service.getPolicy().getOid() );
        serviceStatistics.remove(key);
        for (ServiceResolver resolver : notifyResolvers) {
            resolver.serviceDeleted(service);
        }
        service.getPolicy().forceRecompile();
        logger.finest("removed service " + service.getName() + " from cache. oid=" + service.getOid());
    }

    /**
     * gets a service from the cache
     */
    public PublishedService getCachedService(long oid) {
        PublishedService out = null;
        rwlock.readLock().lock();
        try {
            out = services.get(oid);
        } finally {
            rwlock.readLock().unlock();
        }
        return out;
    }

    /**
     * gets a service from the cache
     */
    public PublishedService getCachedServiceByPolicy(long policyOid) {
        PublishedService out = null;
        rwlock.readLock().lock();
        try {
            for ( PublishedService service : services.values() ) {
                if ( service.getPolicy().getOid() == policyOid ) {
                    out = service;
                    break;
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return out;
    }

    public boolean hasCatchAllService() {
        rwlock.readLock().lock();
        try {
            return hasCatchAllService;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * get all current service stats
     */
    public Collection<ServiceStatistics> getAllServiceStatistics() {
        rwlock.readLock().lock();
        try {
            Collection<ServiceStatistics> output = new ArrayList<ServiceStatistics>();
            output.addAll(serviceStatistics.values());
            return output;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * get statistics for cached service.
     * those stats are lazyly created
     */
    public ServiceStatistics getServiceStatistics(long serviceOid) {
        ServiceStatistics stats;
        Lock read = null;
        Lock write = null;
        try {
            read = rwlock.readLock();
            read.lock();
            stats = serviceStatistics.get(serviceOid);
            if (stats == null) {
                // Upgrade read lock to write lock
                read.unlock();
                read = null;
                stats = new ServiceStatistics(serviceOid);
                write = rwlock.writeLock();
                write.lock();
                serviceStatistics.put(serviceOid, stats);
                write.unlock();
                write = null;
            } else {
                read.unlock();
                read = null;
            }
            return stats;
        } finally {
            if (read != null) read.unlock();
            if (write != null) write.unlock();
        }
    }

    /**
     * @throws Exception
     * @see InitializingBean
     */
    public void afterPropertiesSet() throws Exception {
        this.auditor = new Auditor(this, getApplicationContext(), logger);
    }

    /**
     * @throws Exception
     * @see DisposableBean
     */
    public void destroy() throws Exception {
        checker.cancel();
    }

    private void checkIntegrity() {
        Lock ciReadLock = rwlock.readLock();
        ciReadLock.lock();
        try {
            Map<Long, Integer> cacheversions = versionSnapshot();
            Map<Long, Integer> dbversions;

            // get db versions
            try {
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
                ciReadLock.unlock();
                ciReadLock = null;
            } else {
                ciReadLock.unlock();
                ciReadLock = null;
                Lock ciWriteLock = rwlock.writeLock();
                ciWriteLock.lock();
                try {
                    for (Long svcid : updatesAndAdditions) {
                        PublishedService toUpdateOrAdd;
                        try {
                            toUpdateOrAdd = serviceManager.findByPrimaryKey(svcid);
                        } catch (FindException e) {
                            toUpdateOrAdd = null;
                            logger.log(Level.WARNING, "service scheduled for update or addition" +
                                    "cannot be retrieved", e);
                        }
                        if (toUpdateOrAdd != null) {
                            final Long oid = toUpdateOrAdd.getOid();
                            final String newVersionUID = policyCache.getUniquePolicyVersionIdentifer( toUpdateOrAdd.getPolicy().getOid() );
                            try {
                                final String throwingVersion = servicesThatAreThrowing.get(oid);
                                if (throwingVersion == null || !throwingVersion.equals( newVersionUID ))
                                {
                                    // Try to cache it again
                                    cacheNoLock( new PublishedService( toUpdateOrAdd ) );
                                    if (throwingVersion != null) {
                                        logger.log(Level.INFO, "Policy for service #" + oid + " is no longer invalid");
                                        servicesThatAreThrowing.remove(oid);
                                    }
                                }
                            } catch (ServerPolicyException e) {
                                logger.log(Level.WARNING, "Policy for service #" + oid + " is invalid: " + ExceptionUtils.getMessage(e), e);
                                servicesThatAreThrowing.put(oid, newVersionUID);
                            }
                        } // otherwise, next integrity check shall delete this service from cache
                    }
                    // Trigger xpath compilation if the set of registered xpaths has changed
                    TarariLoader.compile();
                    for (Long key : deletions) {
                        PublishedService serviceToDelete = services.get(key);
                        removeNoLock(serviceToDelete);
                    }
                    updateCatchAll();
                } finally {
                    ciWriteLock.unlock();
                }
            }

        } finally {
            if (ciReadLock != null) ciReadLock.unlock();
            getApplicationContext().publishEvent(new ServiceReloadEvent(this));
        }
    }

    private void updateCatchAll() {
        hasCatchAllService = false;
        for (PublishedService p : services.values()) {
            if ("/*".equals(p.getRoutingUri())) {
                hasCatchAllService = true;
                break;
            }
        }
    }

    /**
     * Run service decorators
     */
    private PublishedService decorate(PublishedService publishedService) {
        PublishedService decorated = publishedService;
        for(Decorator<PublishedService> decorator : decorators) {
            decorated = decorator.decorate(decorated);
        }
        return decorated;
    }

}
