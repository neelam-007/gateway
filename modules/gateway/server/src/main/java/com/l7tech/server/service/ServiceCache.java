/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.service;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.Policy;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.ServiceEnablementEvent;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.event.system.ServiceReloadEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.*;
import com.l7tech.server.service.resolution.*;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ServiceUsageManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceStatistics;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.util.Decorator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ArrayUtils;
import com.l7tech.xml.TarariLoader;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.message.SoapKnob;
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

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contains cached services, with corresponding pre-parsed server-side policies and
 * service statistics.
 * <p/>
 * Entry point for runtime resolution.
 * <p/>
 * Thread safe.
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
    private final Set<Long> servicesThatAreDisabled = new HashSet<Long>();
    private final Collection<Decorator<PublishedService>> decorators;

    private final PlatformTransactionManager transactionManager;
    private final ServiceManager serviceManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ClusterInfoManager clusterInfoManager;

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

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
    private final AtomicBoolean needXpathCompile = new AtomicBoolean(true);


    /**
     * Constructor for bean usage via subclassing.
     */
    public ServiceCache(PolicyCache policyCache,
                        PlatformTransactionManager transactionManager,
                        ServiceManager serviceManager,
                        ServiceUsageManager serviceUsageManager,
                        ClusterInfoManager clusterInfoManager,
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
        this.serviceUsageManager = serviceUsageManager;
        this.clusterInfoManager = clusterInfoManager;
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
            needXpathCompile.set(true);
        } else if (applicationEvent instanceof ServiceCacheEvent.Deleted) {
            doCacheDelete((ServiceCacheEvent.Deleted) applicationEvent);
            needXpathCompile.set(true);
        } else if (applicationEvent instanceof PolicyCacheEvent.Invalid) {
            PolicyCacheEvent.Invalid invalidEvent = (PolicyCacheEvent.Invalid) applicationEvent;
            final Lock write = rwlock.writeLock();
            write.lock();
            try {
                handleInvalidPolicy(null, invalidEvent.getPolicyId(), invalidEvent.getException());
            } finally {
                write.unlock();
            }
            needXpathCompile.set(true);
        } else if (applicationEvent instanceof PolicyCacheEvent.Updated) {
            PolicyCacheEvent.Updated validEvent = (PolicyCacheEvent.Updated) applicationEvent;
            final Lock write = rwlock.writeLock();
            write.lock();
            try {
                handleValidPolicy(validEvent.getPolicy());
            } finally {
                write.unlock();
            }
            needXpathCompile.set(true);
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
            needXpathCompile.set(true);
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

    protected void initializeServiceCache() throws ObjectModelException {
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
                        cache(  new PublishedService( service ));
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
     * This is called upon gateway start up to re-initialize service statistics
     * in memory from database so that service usage counters are cumulative
     * across restart.
     *
     * @throws ObjectModelException if database error
     * @see <a href="http://sarek/bugzilla/show_bug.cgi?id=4616">Bug 4616</a>
     */
    private void initializeServiceStatistics() throws ObjectModelException {
        try {
            final Collection<PublishedService> services = serviceManager.findAll();
            final Set<Long> servicesByOid = new HashSet<Long>(services.size());
            for (PublishedService service : services) {
                servicesByOid.add(service.getOid());
            }
            final ServiceUsage[] sus = serviceUsageManager.findByNode(clusterInfoManager.thisNodeId());
            for (ServiceUsage su : sus) {
                final long serviceOid = su.getServiceid();
                // Retain usage data for existing published services only.
                if (servicesByOid.contains(serviceOid)) {
                    final ServiceStatistics stats = new ServiceStatistics(su.getServiceid(),
                                                                          (int)su.getRequests(),
                                                                          (int)su.getAuthorized(),
                                                                          (int)su.getCompleted());
                    serviceStatistics.put(stats.getServiceOid(), stats);
                }
            }
        } catch (FindException e) {
            throw new ObjectModelException("Failed to initialize service statistics.", e);
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
     * @param serviceOid the OID of the service to resolve to
     * @return the cached version of the service that this request resolve to. null if no match
     * @throws ServiceResolutionException
     */
    public PublishedService resolve(Message req, ResolutionListener rl, Long serviceOid) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            Collection<PublishedService> serviceSet;
            PublishedService service = services.get( serviceOid );
            if ( service == null ) {
                serviceSet = Collections.emptySet();
            } else {
                serviceSet = Collections.singleton(service);
            }

            return resolve( req, rl, serviceSet );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * @param req the soap request to resolve the service from
     * @return the cached version of the service that this request resolve to. null if no match
     * @throws ServiceResolutionException
     */
    public PublishedService resolve(Message req, ResolutionListener rl) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            Collection<PublishedService> serviceSet = Collections.unmodifiableCollection(services.values());
            return resolve( req, rl, serviceSet );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Caller must hold read lock
     */
    private PublishedService resolve(Message req, ResolutionListener rl, Collection<PublishedService> serviceSet) throws ServiceResolutionException {
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
            Set<PolicyMetadata> metadatas = getPolicyMetadata(serviceSet);
            if (rl != null && !notified) {
                if (!rl.notifyPreParseServices(req, metadatas))
                    return null;
            }

            PublishedService service = serviceSet.iterator().next();
            XmlKnob xk = (XmlKnob) req.getKnob(XmlKnob.class);

            if (!service.isSoap() || service.isLaxResolution()) {
                if (xk != null) xk.setTarariWanted(metadatas.iterator().next().isTarariWanted());
                return service;
            }

            // If this service is set to strict mode, validate that the message is soap, and that it matches an
            // operation supported in the WSDL.
            if (req.getKnob(SoapKnob.class) == null) {
                auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_NOT_SOAP);
                return null;
            } else {
                // avoid re-Tarari-ing request that's already DOM parsed unless some assertions need it bad
                if (xk != null) xk.setTarariWanted(metadatas.iterator().next().isTarariWanted());
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
    }

    private Set<PolicyMetadata> getPolicyMetadata( final Collection<PublishedService> services ) {
        Set<PolicyMetadata> metadata;

        if ( services.isEmpty() ) {
            metadata = Collections.emptySet();
        } else if ( services.size() == 1 ) {
            metadata = Collections.singleton( getPolicyMetadata( services.iterator().next().getPolicy() ) );
        } else {
            metadata = new LinkedHashSet<PolicyMetadata>();

            for ( PublishedService service : services ) {
                metadata.add( getPolicyMetadata( service.getPolicy() ) );
            }
        }
        return metadata;
    }

    /**
     * User should hold lock.
     */
    private PolicyMetadata getPolicyMetadata( final Policy policy ) {
        PolicyMetadata metadata = null;
        if ( policy != null ) {
            metadata = policyCache.getPolicyMetadata(policy);
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

    private static enum CachedServiceNotificationType {
        CREATED, ENABLED, DISABLED, UPDATED, DELETED
    }

    /**
     * adds or update a service to the cache. this should be called when the cache is initially populated and
     * when a service is saved or updated locally
     * @throws ServerPolicyException if a server assertion contructor for this service threw an exception
     */
    public void cache(PublishedService service) throws ServerPolicyException {
        final List<Pair<Long, CachedServiceNotificationType>> notificationMap = new LinkedList<Pair<Long, CachedServiceNotificationType>>();
        rwlock.writeLock().lock();
        try {
            cacheNoLock(service, notificationMap);
            updateCatchAll();
        } finally {
            rwlock.writeLock().unlock();
        }

        if (!notificationMap.isEmpty()) {
            notifyListeners(notificationMap);
        }
    }

    private void notifyListeners(final List<Pair<Long, CachedServiceNotificationType>> notificationList) {
        threadPool.execute(new Runnable() {
            public void run() {
                final List<Long> oids = new LinkedList<Long>();
                final List<Character> ops = new LinkedList<Character>();
                final List<Long> enabled = new LinkedList<Long>();
                final List<Long> disabled = new LinkedList<Long>();

                for (Pair<Long, CachedServiceNotificationType> entry : notificationList) {
                    final Long oid = entry.left;
                    final CachedServiceNotificationType type = entry.right;
                    switch(type) {
                        case CREATED:
                            oids.add(oid);
                            ops.add(EntityInvalidationEvent.CREATE);
                            break;
                        case UPDATED:
                            oids.add(oid);
                            ops.add(EntityInvalidationEvent.UPDATE);
                            break;
                        case DISABLED:
                            disabled.add(oid);
                            break;
                        case ENABLED:
                            enabled.add(oid);
                            break;
                        case DELETED:
                            oids.add(oid);
                            ops.add(EntityInvalidationEvent.DELETE);
                            break;
                    }
                }

                if (!oids.isEmpty()) {
                    logger.log(Level.INFO, "Created/Updated/Deleted: " + oids);
                    getApplicationContext().publishEvent(new EntityInvalidationEvent(ServiceCache.this, PublishedService.class, ArrayUtils.unbox(oids), ArrayUtils.unbox(ops)));
                }
                
                if (!enabled.isEmpty()) notifyEnablements(enabled, true);
                if (!disabled.isEmpty()) notifyEnablements(disabled, false);
            }
        });
    }

    private void notifyEnablements(List<Long> oids, boolean enabled) {
        if (oids == null || oids.isEmpty()) return;
        logger.log(Level.INFO, (enabled ? "Enabled" : "Disabled" + ": ") + oids);
        getApplicationContext().publishEvent(new ServiceEnablementEvent(ServiceCache.this, ArrayUtils.unbox(oids), enabled));
    }


    /**
     * Caller must hold lock protecting {@link #services}
     */
    private void cacheNoLock(PublishedService newService, List<Pair<Long, CachedServiceNotificationType>> notificationList) throws ServerPolicyException {
        final PublishedService service = decorate(newService);
        final Long oid = service.getOid();
        final PublishedService oldService = services.get(oid);

        final boolean update;
        if (oldService == null) {
            update = false;
        } else {
            update = true;
            if (oldService.isDisabled() && !newService.isDisabled()) {
                notificationList.add(new Pair<Long, CachedServiceNotificationType>(oid, CachedServiceNotificationType.ENABLED));
            } else if (newService.isDisabled() && !oldService.isDisabled()) {
                notificationList.add(new Pair<Long, CachedServiceNotificationType>(oid, CachedServiceNotificationType.DISABLED));
            }
        }

        if (update) {
            for (ServiceResolver resolver : notifyResolvers) {
                try {
                    resolver.serviceUpdated(service);
                } catch (ServiceResolutionException sre) {
                    auditor.logAndAudit(SystemMessages.SERVICE_WSDL_ERROR,
                            new String[]{service.displayName(), sre.getMessage()}, sre);
                }
            }
            notificationList.add(new Pair<Long, CachedServiceNotificationType>(oid, CachedServiceNotificationType.UPDATED));
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
            notificationList.add(new Pair<Long, CachedServiceNotificationType>(oid, CachedServiceNotificationType.CREATED));
            logger.finest("added service " + service.getName() + " in cache. oid=" + service.getOid());
        }

        // cache the service
        services.put(oid, service);

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

    /**
     * Get server policy handle for given service, null if invalid 
     */
    private ServerPolicyHandle getServerPolicyForService(final PublishedService service) {
        ServerPolicyHandle handle = null;

        Policy policy = service.getPolicy();
        if ( policy != null ) {
            handle = policyCache.getServerPolicy( policy );
        }

        return  handle;
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
                setDisabled(service, true);
            } else {
                auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_BAD_POLICY, service.getName(), service.getId() );
                auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_DISABLING_SERVICE, service.getId(), service.getName() );
                setDisabled(service, true);
            }
        }
    }

    private void handleValidPolicy( final Policy policy  ) {
        PublishedService service = getCachedServiceByPolicy( policy.getOid() );

        // if it is not cached then it will be disabled later
        if ( service != null && isDisabled(service) ) {
            auditor.logAndAudit( MessageProcessingMessages.SERVICE_CACHE_ENABLING_SERVICE, service.getName(), service.getId() );
            setDisabled(service, false);
        }
    }


    /**
     * Caller must hold lock
     */
    private boolean isDisabled( final PublishedService service ) {
        return servicesThatAreDisabled.contains( service.getOid() );
    }

    /**
     * Caller must hold lock
     */
    private void setDisabled( final PublishedService service, final boolean disabled ) {
        if ( disabled ) {
            servicesThatAreDisabled.add( service.getOid() );
        } else {
            servicesThatAreDisabled.remove( service.getOid() );
        }
    }

    /**
     * Caller must hold a lock protecting {@link #services} and {@link #serviceStatistics}.
     */
    private void removeNoLock(PublishedService service) {
        final Long key = service.getOid();
        final Policy policy = service.getPolicy();
        services.remove(key);
        if ( policy != null ) {
            policyCache.remove( policy.getOid() );
        }
        serviceStatistics.remove(key);
        for (ServiceResolver resolver : notifyResolvers) {
            resolver.serviceDeleted(service);
        }
        if ( policy != null ) {
            policy.forceRecompile();
        }
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
                Policy policy = service.getPolicy();
                if ( policy != null && policy.getOid() == policyOid ) {
                    out = service;
                    break;
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return out;
    }

    public Collection<PublishedService> getCachedServicesByURI(String uri) {
        Collection<PublishedService> foundServices = null;
        // if uri param provided, narrow down list using it
        if (uri != null) {
            try {
                foundServices = serviceManager.findAll();
            } catch (FindException fe) {
                logger.severe("Failed to determine the list of available services. " + ExceptionUtils.getMessage(fe));
                return null;
            }

            
            Set<PublishedService> serviceSubset = new HashSet<PublishedService>();
            serviceSubset.addAll(foundServices);
            Map<UriResolver.URIResolutionParam, List<Long>> uriToServiceMap = new HashMap<UriResolver.URIResolutionParam, List<Long>>();
            for (PublishedService s : serviceSubset) {
                String psUri = s.getRoutingUri();
                if (psUri == null) psUri = "";
                UriResolver.URIResolutionParam up = new UriResolver.URIResolutionParam(psUri);
                List<Long> listedServicesForThatURI = uriToServiceMap.get(up);
                if (listedServicesForThatURI == null) {
                    listedServicesForThatURI = new ArrayList<Long>();
                    uriToServiceMap.put(up, listedServicesForThatURI);
                }
                listedServicesForThatURI.add(s.getOid());
            }
            Result res = UriResolver.doResolve(uri, serviceSubset, uriToServiceMap, null);
            if (res.getMatches() == null || res.getMatches().size() == 0) {
                logger.warning("URI param '" + uri + "' did not resolve any service.");
            }

            foundServices = res.getMatches();
        }
        return foundServices;
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
        initializeServiceStatistics();
    }

    /**
     * @throws Exception
     * @see DisposableBean
     */
    public void destroy() throws Exception {
        checker.cancel();
    }

    private void checkIntegrity() {
        if (needXpathCompile.getAndSet(false))
            TarariLoader.compile();

        // Collect information about what has happened to services in the system since the last integrity check, so that
        // listeners can be notified outside our lock, and in a different thread

        final List<Pair<Long, CachedServiceNotificationType>> notificationMap = new LinkedList<Pair<Long, CachedServiceNotificationType>>();

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
                    notificationMap.add(new Pair<Long, CachedServiceNotificationType>(cacheid, CachedServiceNotificationType.DELETED));
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
                        PublishedService newService;
                        try {
                            newService = serviceManager.findByPrimaryKey(svcid);
                        } catch (FindException e) {
                            newService = null;
                            logger.log(Level.WARNING, "service scheduled for update or addition" +
                                    "cannot be retrieved", e);
                        }
                        if (newService != null) {
                            final Long oid = newService.getOid();
                            final Policy policy = newService.getPolicy();
                            String uniqueVersion = null;
                            if ( policy != null ) { // Get version for full policy if possible
                                uniqueVersion = policyCache.getUniquePolicyVersionIdentifer( policy.getOid() );
                            }
                            if ( uniqueVersion == null ) { // Use service version if that is all that is available
                                uniqueVersion = Integer.toString( newService.getVersion() );
                            }
                            final String newVersionUID = uniqueVersion;
                            try {
                                final String throwingVersion = servicesThatAreThrowing.get(oid);
                                if (throwingVersion == null || !throwingVersion.equals( newVersionUID ))
                                {
                                    // Try to cache it again
                                    cacheNoLock(new PublishedService( newService ), notificationMap);
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

                if (!notificationMap.isEmpty()) notifyListeners(notificationMap);
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
