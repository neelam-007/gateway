package com.l7tech.server.service;

import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.gateway.common.cluster.ServiceUsage;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceStatistics;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.imp.GoidEntityUtil;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.cluster.ServiceUsageManager;
import com.l7tech.server.event.GoidEntityInvalidationEvent;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.ServiceEnablementEvent;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.event.system.ServiceReloadEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.policy.*;
import com.l7tech.server.service.resolution.NonUniqueServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionException;
import com.l7tech.server.service.resolution.ServiceResolutionManager;
import com.l7tech.server.service.resolution.UriResolver;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.TarariLoader;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
        implements InitializingBean, DisposableBean, PostStartupApplicationListener, ServiceCacheResolver
{
    private static final Logger logger = Logger.getLogger(ServiceCache.class.getName());

    public static final long INTEGRITY_CHECK_FREQUENCY = 4000; // 4 seconds

    // the cache data itself
    private final Map<Goid, PublishedService> services = new HashMap<Goid, PublishedService>();
    private final ConcurrentMap<Goid, ServiceStatistics> serviceStatistics = new ConcurrentHashMap<Goid, ServiceStatistics>();
    private final Map<Goid, String> servicesThatAreThrowing = new HashMap<Goid, String>();
    private final Set<Goid> servicesThatAreDisabled = new HashSet<Goid>();

    private final PlatformTransactionManager transactionManager;
    private final ServiceManager serviceManager;
    private final ServiceUsageManager serviceUsageManager;
    private final ClusterInfoManager clusterInfoManager;

    private final ExecutorService threadPool = Executors.newSingleThreadExecutor();

    private final Timer checker; // Don't use Background since this is high priority
    // read-write lock for thread safety
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock(false);

    //private final PeriodicExecutor checker = new PeriodicExecutor( this );
    private boolean running = false;

    private Auditor auditor;
    private Goid nonSoapCatchAllServiceOid = null;
    private final PolicyCache policyCache;
    private final ServiceResolutionManager serviceResolutionManager;
    private final AtomicBoolean needXpathCompile = new AtomicBoolean(true);

    /**
     * Constructor for bean usage via subclassing.
     */
    public ServiceCache(PolicyCache policyCache,
                        PlatformTransactionManager transactionManager,
                        ServiceManager serviceManager,
                        ServiceUsageManager serviceUsageManager,
                        ServiceResolutionManager serviceResolutionManager,
                        ClusterInfoManager clusterInfoManager,
                        Timer timer)
    {
        if (policyCache == null) throw new IllegalArgumentException("Policy Cache is required");
        if (timer == null) timer = new Timer("Service cache refresh", true);

        this.policyCache = policyCache;
        this.serviceResolutionManager = serviceResolutionManager;
        this.checker = timer;
        this.transactionManager = transactionManager;
        this.serviceManager = serviceManager;
        this.serviceUsageManager = serviceUsageManager;
        this.clusterInfoManager = clusterInfoManager;
    }

    @ManagedOperation(description="Check Cache Integrity")
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

    @Override
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
                    // reload service. version property must be up-to-date
                    loadServiceToCache( updated.getService().getGoid() );
                }
            }
        });
    }

    private void loadServiceToCache( final Goid serviceGoid ) {
        PublishedService publishedService;
        try {
            publishedService = serviceManager.findByPrimaryKey(serviceGoid);
        } catch ( FindException e) {
            publishedService = null;
            logger.log( Level.WARNING, "could not get service back", e);
        }

        if (publishedService != null) {
            try {
                GoidEntityUtil.lock(publishedService);
                GoidEntityUtil.lock(publishedService.getPolicy());
                cache(publishedService);
                TarariLoader.compile();
            } catch (Exception e) {
                logger.log(Level.WARNING, "could not update service cache: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    protected void initializeServiceCache() throws ObjectModelException {
        // build the cache if necessary
        try {
            if (size() > 0) {
                logger.finest("cache already built (?)");
            } else {
                logger.info("Building service cache");
                final long startTime = System.currentTimeMillis();
                Collection<PublishedService> services = serviceManager.findAll();
                for (PublishedService service : services) {
                    try {
                        // cache a copy not bound to hibernate session
                        cache(  new PublishedService( service, true ));
                    } catch (ServerPolicyException e) {
                        Assertion ass = e.getAssertion();

                        String ordinal = ass == null ? "" : "#" + Integer.toString(ass.getOrdinal());
                        String what = ass == null ? "<unknown>" : "(" + ass.getClass().getSimpleName() + ")";
                        String msg = MessageFormat.format( "Disabling PublishedService #{0} ({1}); policy could not be compiled (assertion {2} {3})",
                                        service.getGoid(), service.getName(), ordinal, what );
                        logger.log(Level.WARNING, msg, e);
                        // We don't actually disable the service here -- only the admin should be doing that.
                        // Instead, we will let the service cache continue to monitor the situation
                    } catch (Exception e) {
                        String msg = MessageFormat.format( "Disabling PublishedService #{0} ({1}); policy could not be compiled",
                                        service.getGoid(), service.getName() );
                        logger.log(Level.WARNING, msg, e);
                    }
                }
                TarariLoader.compile();
                logger.info("Built service cache in " + (System.currentTimeMillis()-startTime) + "ms.");
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
            final Set<Goid> servicesByGoid = new HashSet<Goid>(services.size());
            for (PublishedService service : services) {
                servicesByGoid.add(service.getGoid());
            }
            final ServiceUsage[] sus = serviceUsageManager.findByNode(clusterInfoManager.thisNodeId());
            for (ServiceUsage su : sus) {
                final Goid serviceGoid = su.getServiceid();
                // Retain usage data for existing published services only.
                if (servicesByGoid.contains(serviceGoid)) {
                    final ServiceStatistics stats = new ServiceStatistics(su.getServiceid(),
                                                                          (int)su.getRequests(),
                                                                          (int)su.getAuthorized(),
                                                                          (int)su.getCompleted());
                    serviceStatistics.put(stats.getServiceGoid(), stats);
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
    private Map<Goid, Integer> versionSnapshot() {
        Map<Goid, Integer> output = new HashMap<Goid, Integer>();
        for (PublishedService svc : services.values()) {
            output.put(svc.getGoid(), svc.getVersion());
        }
        return output;
    }

    /**
     * get pre-parsed root server assertion for cached service
     *
     * @param serviceGoid id of the service of which we want the parsed server side root assertion
     */
    public ServerPolicyHandle getServerPolicy(Goid serviceGoid) {
        rwlock.readLock().lock();
        try {
            PublishedService service = services.get( serviceGoid );
            if (service == null) return null;
            return getServerPolicyForService( service );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Listener interface that can be implemented by clients of the service
     * cache if pre-processing is required before use of the body of a message.
     *
     * <p>Note that if the message body is not required during service resolution
     * then this listener may not be called.</p>
     */
    public static interface ResolutionListener {

        /**
         * Notification that the contents of the given message are about to be used.
         *
         * @param message The message being used for resolution.
         * @param serviceSet The metadata for all candidate services.
         * @return true if service resolution should be continued
         */
        boolean notifyPreParseServices(Message message, Set<ServiceMetadata> serviceSet);

        /**
         * Notification that the catch all service will be resolved.
         *
         * @return true if the catch all service may be used.
         */
        boolean notifyResolveCatchAll();
    }

    public static final class ServiceMetadata {
        private final PolicyMetadata policyMetadata;
        private final boolean wssProcessingRequired;

        public ServiceMetadata( final PolicyMetadata policyMetadata,
                                final boolean wssProcessingRequired ) {
            this.policyMetadata = policyMetadata;
            this.wssProcessingRequired = wssProcessingRequired;
        }

        public boolean isMultipart() {
            return policyMetadata.isMultipart();
        }

        public boolean isTarariWanted() {
            return policyMetadata.isTarariWanted();
        }

        public boolean isWssInPolicy() {
            return policyMetadata.isWssInPolicy();
        }

        public boolean isWssProcessingRequired() {
            return wssProcessingRequired;
        }
    }

    /**
     * @param req the soap request to resolve the service from
     * @param rl resolution listener to notify if a resolver is about to examine the Message contents, or null
     * @return the cached version of the service that this request resolve to. null if no match
     * @throws ServiceResolutionException on resolution error
     */
    @Override
    public PublishedService resolve(Message req, ResolutionListener rl) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            Collection<PublishedService> serviceSet = Collections.unmodifiableCollection(services.values());
            PublishedService result = resolve( req, rl, serviceSet );
            if (result == null && rl.notifyResolveCatchAll() && nonSoapCatchAllServiceOid != null && UriResolver.canResolveByURI(req)) {
                result = services.get(nonSoapCatchAllServiceOid);
                if (result != null)
                    auditor.logAndAudit(MessageProcessingMessages.SERVICE_CACHE_RESOLVED_CATCHALL, result.getName(), result.getId());
            }
            return result;
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Resolve the given resolution parameters to the matching service.
     *
     * @param path The service path (ignored if null)
     * @param soapAction The SOAP action (ignored if null)
     * @param namespace The namespace (ignored if null)
     * @throws ServiceResolutionException If an error occurs
     * @return The services that match the given resolution parameters
     */
    public Collection<PublishedService> resolve( final @Nullable String path,
                                                 final @Nullable String soapAction,
                                                 final @Nullable String namespace ) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            Collection<PublishedService> serviceSet = Collections.unmodifiableCollection(services.values());
            return serviceResolutionManager.resolve( path, soapAction, namespace, serviceSet );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Check that the given published service resolves without conflicts.
     *
     * @param service The service to check
     * @throws NonUniqueServiceResolutionException If the service is not resolvable
     * @throws ServiceResolutionException If an error occurs
     */
    @Override
    public void checkResolution( final PublishedService service ) throws ServiceResolutionException {
        rwlock.readLock().lock();
        try {
            Collection<PublishedService> serviceSet = Collections.unmodifiableCollection(services.values());
            serviceResolutionManager.checkResolution( service, serviceSet );
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * Caller must hold read lock
     */
    private PublishedService resolve( final Message req,
                                      final ResolutionListener rl,
                                      final Collection<PublishedService> serviceSet ) throws ServiceResolutionException {
        return serviceResolutionManager.resolve( auditor, req, new ServiceResolutionManager.ServiceResolutionListener() {
            @Override
            public boolean notifyMessageBodyAccess( final Message message,
                                                    final Collection<PublishedService> serviceSet ) {
                return rl.notifyPreParseServices( message, getServiceMetadata(serviceSet) );
            }

            @Override
            public void notifyMessageValidation( final Message req, final PublishedService service ) {
                // avoid re-Tarari-ing request that's already DOM parsed unless some assertions need it bad
                XmlKnob xk = req.getKnob(XmlKnob.class);
                if ( xk != null ) {
                    xk.setTarariWanted( getServiceMetadata(service).isTarariWanted() );
                }
            }
        }, serviceSet );
    }

    private Set<ServiceMetadata> getServiceMetadata( final Collection<PublishedService> services ) {
        Set<ServiceMetadata> metadata;

        if ( services.isEmpty() ) {
            metadata = Collections.emptySet();
        } else if ( services.size() == 1 ) {
            metadata = Collections.singleton( getServiceMetadata( services.iterator().next() ) );
        } else {
            metadata = new LinkedHashSet<ServiceMetadata>();

            for ( PublishedService service : services ) {
                metadata.add( getServiceMetadata( service ) );
            }
        }
        return metadata;
    }

    /**
     * User should hold lock.
     */
    private ServiceMetadata getServiceMetadata( final PublishedService service ) {
        PolicyMetadata policyMetadata = null;

        Policy policy = service.getPolicy();
        if ( policy != null ) {
            policyMetadata = policyCache.getPolicyMetadata(policy);
        }

        if ( policyMetadata == null ) {
            // use defaults
            final PolicyHeader policyHeader = policy == null ? null : new PolicyHeader(policy);
            policyMetadata = new PolicyMetadata() {
                @Override
                public boolean isTarariWanted() { return false; }
                @Override
                public boolean isWssInPolicy() { return false; }
                @Override
                public boolean isMultipart() { return false; }
                @Override
                public PolicyHeader getPolicyHeader() { return policyHeader; }
                @Override
                public String[] getVariablesUsed() { return new String[0]; }
                @Override
                public VariableMetadata[] getVariablesSet() { return new VariableMetadata[0]; }
            };
        }

        return new ServiceMetadata( policyMetadata, service.isWssProcessingEnabled() );
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
        final List<Pair<Goid, CachedServiceNotificationType>> notificationMap = new LinkedList<Pair<Goid, CachedServiceNotificationType>>();
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

    private void notifyListeners(final List<Pair<Goid, CachedServiceNotificationType>> notificationList) {
        threadPool.execute(new Runnable() {
            @Override
            public void run() {
                final List<Goid> goids = new LinkedList<Goid>();
                final List<Character> ops = new LinkedList<Character>();
                final List<Goid> enabled = new LinkedList<Goid>();
                final List<Goid> disabled = new LinkedList<Goid>();

                for (Pair<Goid, CachedServiceNotificationType> entry : notificationList) {
                    final Goid goid = entry.left;
                    final CachedServiceNotificationType type = entry.right;
                    switch(type) {
                        case CREATED:
                            goids.add(goid);
                            ops.add(GoidEntityInvalidationEvent.CREATE);
                            break;
                        case UPDATED:
                            goids.add(goid);
                            ops.add(GoidEntityInvalidationEvent.UPDATE);
                            break;
                        case DISABLED:
                            disabled.add(goid);
                            break;
                        case ENABLED:
                            enabled.add(goid);
                            break;
                        case DELETED:
                            goids.add(goid);
                            ops.add(GoidEntityInvalidationEvent.DELETE);
                            break;
                    }
                }

                if (!goids.isEmpty()) {
                    logger.log(Level.INFO, "Created/Updated/Deleted: " + goids);
                    getApplicationContext().publishEvent(new GoidEntityInvalidationEvent(ServiceCache.this, PublishedService.class, goids.toArray(new Goid[goids.size()]), ArrayUtils.unboxChars(ops)));
                }
                
                if (!enabled.isEmpty()) notifyEnablements(enabled, true);
                if (!disabled.isEmpty()) notifyEnablements(disabled, false);
            }
        });
    }

    private void notifyEnablements(List<Goid> goids, boolean enabled) {
        if (goids == null || goids.isEmpty()) return;
        logger.log(Level.INFO, (enabled ? "Enabled" : "Disabled" + ": ") + goids);
        getApplicationContext().publishEvent(new ServiceEnablementEvent(ServiceCache.this, goids.toArray(new Goid[goids.size()]), enabled));
    }


    /**
     * Caller must hold lock protecting {@link #services}
     */
    private void cacheNoLock(PublishedService service, List<Pair<Goid, CachedServiceNotificationType>> notificationList) throws ServerPolicyException {
        final Goid goid = service.getGoid();
        final PublishedService oldService = services.get(goid);

        final boolean update;
        if (oldService == null) {
            update = false;
        } else {
            update = true;
            if (oldService.isDisabled() && !service.isDisabled()) {
                notificationList.add(new Pair<Goid, CachedServiceNotificationType>(goid, CachedServiceNotificationType.ENABLED));
            } else if (service.isDisabled() && !oldService.isDisabled()) {
                notificationList.add(new Pair<Goid, CachedServiceNotificationType>(goid, CachedServiceNotificationType.DISABLED));
            }
        }

        if (update) {
            serviceResolutionManager.notifyServiceUpdated( auditor, service );
            notificationList.add(new Pair<Goid, CachedServiceNotificationType>(goid, CachedServiceNotificationType.UPDATED));
            logger.finest("updated service " + service.getName() + " in cache. oid=" + service.getGoid() + " version=" + service.getVersion());
        } else {
            serviceResolutionManager.notifyServiceCreated( auditor, service );
            notificationList.add(new Pair<Goid, CachedServiceNotificationType>(goid, CachedServiceNotificationType.CREATED));
            logger.finest("added service " + service.getName() + " in cache. oid=" + service.getGoid());
        }

        // cache the service
        if (!GoidEntityUtil.isLocked(service))
            throw new IllegalArgumentException("Unlocked service may not be placed into the service cache");
        services.put(goid, service);

        // cache the server policy for this service
        final Policy policy = service.getPolicy();
        if (policy == null)
            throw new ServerPolicyException(null, "Service #" + service.getGoid() + " (" + service.getName() + ") has no policy");
        if (!GoidEntityUtil.isLocked(policy))
            throw new IllegalArgumentException("Unlocked service policy may not be placed into the service cache");
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
                                      final Goid policyId,
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
        PublishedService service = getCachedServiceByPolicy( policy.getGoid() );

        if ( service!=null && service.getPolicy()!=null && service.getPolicy().getVersion() != policy.getVersion() ) {
            logger.fine("service " + service.getGoid() + " to be updated in cache because of outdated policy.");
            loadServiceToCache( service.getGoid() );
        }

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
        return servicesThatAreDisabled.contains( service.getGoid() );
    }

    /**
     * Caller must hold lock
     */
    private void setDisabled( final PublishedService service, final boolean disabled ) {
        if ( disabled ) {
            servicesThatAreDisabled.add( service.getGoid() );
        } else {
            servicesThatAreDisabled.remove( service.getGoid() );
        }
    }

    /**
     * Caller must hold a lock protecting {@link #services}
     */
    private void removeNoLock(PublishedService service) {
        final Goid key = service.getGoid();
        final Policy policy = service.getPolicy();
        services.remove(key);
        if ( policy != null ) {
            policyCache.remove( policy.getGoid() );
        }
        serviceStatistics.remove(key);
        serviceResolutionManager.notifyServiceDeleted( service );
        if ( policy != null ) {
            policy.forceRecompile();
        }
        logger.finest("removed service " + service.getName() + " from cache. goid=" + service.getGoid());
    }

    /**
     * Get all cached services.
     *
     * @return The services.
     */
    public Collection<PublishedService> getCachedServices() {
        rwlock.readLock().lock();
        try {
            return  Collections.unmodifiableCollection(services.values());
        } finally {
            rwlock.readLock().unlock();
        }
    }

    /**
     * gets a service from the cache
     */
    public PublishedService getCachedService(Goid goid) {
        PublishedService out = null;
        rwlock.readLock().lock();
        try {
            out = services.get(goid);
        } finally {
            rwlock.readLock().unlock();
        }
        return out;
    }

    /**
     * gets a service from the cache
     */
    public PublishedService getCachedServiceByPolicy(Goid policyGoid) {
        PublishedService out = null;
        rwlock.readLock().lock();
        try {
            for ( PublishedService service : services.values() ) {
                Policy policy = service.getPolicy();
                if ( policy != null && policy.getGoid().equals(policyGoid) ) {
                    out = service;
                    break;
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return out;
    }

    public List<PublishedService> getInternalServices() {
        List<PublishedService> out = new ArrayList<PublishedService>();
        rwlock.readLock().lock();
        try {
            for ( PublishedService service : services.values() ) {
                if( service.isInternal() ) {
                    out.add(service);
                }
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return out;
    }

    /**
     * get all current per-node service stats.
     * @return a snapshot of the service statistics.
     */
    public Collection<ServiceStatistics> getAllServiceStatistics() {
        return new ArrayList<ServiceStatistics>(serviceStatistics.values());
    }

    /**
     * get per-node statistics for cached service.
     * those stats are lazyly created
     * @param serviceGoid service whose stats to fetch.  required
     * @return stats for this service, possibly just-created
     */
    public ServiceStatistics getServiceStatistics(Goid serviceGoid) {
        ServiceStatistics stats = serviceStatistics.get(serviceGoid);
        if (stats == null) {
            stats = new ServiceStatistics(serviceGoid);
            ServiceStatistics oldStats = serviceStatistics.putIfAbsent(serviceGoid, stats);
            if (oldStats != null)
                return oldStats;
        }
        return stats;
    }

    /**
     * @throws Exception
     * @see InitializingBean
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        this.auditor = new Auditor(this, getApplicationContext(), logger);
        initializeServiceStatistics();
    }

    /**
     * @throws Exception
     * @see DisposableBean
     */
    @Override
    public void destroy() throws Exception {
        checker.cancel();
        if (threadPool != null)
            threadPool.shutdown();
    }

    private void checkIntegrity() {
        if (needXpathCompile.getAndSet(false))
            TarariLoader.compile();

        // Collect information about what has happened to services in the system since the last integrity check, so that
        // listeners can be notified outside our lock, and in a different thread

        final List<Pair<Goid, CachedServiceNotificationType>> notificationMap = new LinkedList<Pair<Goid, CachedServiceNotificationType>>();

        Lock ciReadLock = rwlock.readLock();
        ciReadLock.lock();
        try {
            Map<Goid, Integer> cacheversions = versionSnapshot();
            Map<Goid, Integer> dbversions;

            // get db versions
            try {
                dbversions = serviceManager.findVersionMap();
            } catch (FindException e) {
                logger.log(Level.SEVERE, "error getting versions. " +
                  "this integrity check is stopping prematurely", e);
                return;
            }

            // actual check logic
            ArrayList<Goid> updatesAndAdditions = new ArrayList<Goid>();
            ArrayList<Goid> deletions = new ArrayList<Goid>();
            // 1. check that all that is in db is present in cache and that version is same
            for (Goid dbid : dbversions.keySet()) {
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
            for (Goid cacheid : cacheversions.keySet()) {
                if (dbversions.get(cacheid) == null) {
                    notificationMap.add(new Pair<Goid, CachedServiceNotificationType>(cacheid, CachedServiceNotificationType.DELETED));
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
                    for (Goid svcid : updatesAndAdditions) {
                        PublishedService newService;
                        try {
                            newService = serviceManager.findByPrimaryKey(svcid);
                        } catch (FindException e) {
                            newService = null;
                            logger.log(Level.WARNING, "service scheduled for update or addition" +
                                    "cannot be retrieved", e);
                        }
                        if (newService != null) {
                            final Goid goid = newService.getGoid();
                            final Policy policy = newService.getPolicy();
                            String uniqueVersion = null;
                            if ( policy != null ) { // Get version for full policy if possible
                                uniqueVersion = policyCache.getUniquePolicyVersionIdentifer( policy.getGoid() );
                            }
                            if ( uniqueVersion == null ) { // Use service version if that is all that is available
                                uniqueVersion = Integer.toString( newService.getVersion() );
                            }
                            final String newVersionUID = uniqueVersion;
                            try {
                                final String throwingVersion = servicesThatAreThrowing.get(goid);
                                if (throwingVersion == null || !throwingVersion.equals( newVersionUID ))
                                {
                                    // Try to cache it again
                                    cacheNoLock(new PublishedService( newService, true ), notificationMap);
                                    if (throwingVersion != null) {
                                        logger.log(Level.INFO, "Policy for service #" + goid + " is no longer invalid");
                                        servicesThatAreThrowing.remove(goid);
                                    }
                                }
                            } catch (ServerPolicyException e) {
                                logger.log(Level.WARNING, "Policy for service #" + goid + " is invalid: " + ExceptionUtils.getMessage(e), e);
                                servicesThatAreThrowing.put(goid, newVersionUID);
                            }
                        } // otherwise, next integrity check shall delete this service from cache
                    }
                    // Trigger xpath compilation if the set of registered xpaths has changed
                    TarariLoader.compile();
                    for (Goid key : deletions) {
                        PublishedService serviceToDelete = services.get(key);
                        if ( serviceToDelete != null ) {
                            removeNoLock(serviceToDelete);
                        }
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
        for (PublishedService p : services.values()) {
            if (!p.isDisabled() && !p.isSoap() && "/*".equals(p.getRoutingUri())) {
                nonSoapCatchAllServiceOid = p.getGoid();
                return;
            }
        }
        nonSoapCatchAllServiceOid = null;
    }

    /**
     * 
     */
    @ManagedResource(description="Service Cache", objectName="l7tech:type=ServiceCache")
    public static class ManagedServiceCache {
        private final ServiceCache cache;

        protected ManagedServiceCache( final ServiceCache serviceCache ) {
            this.cache = serviceCache;
        }

        @ManagedAttribute(description="Cache Size", currencyTimeLimit=30)
        public int getSize() {
            return cache.size();
        }

        /**
         * Get the identifiers for cached services.
         *
         * @return the cached services
         */
        @ManagedAttribute(description="Cached Services", currencyTimeLimit=30)
        public Set<Goid> getServices() {
            cache.rwlock.readLock().lock();
            try {
                return new TreeSet<Goid>(cache.services.keySet());
            } finally {
               cache.rwlock.readLock().unlock();
            }
        }

        /**
         * Get the identifiers for services with errors.
         *
         * @return the services with errors
         */
        @ManagedAttribute(description="Services With Errors", currencyTimeLimit=30)
        public Set<Goid> getServicesWithErrors() {
            cache.rwlock.readLock().lock();
            try {
                return new TreeSet<Goid>(cache.servicesThatAreThrowing.keySet());
            } finally {
                cache.rwlock.readLock().unlock();
            }
        }

        /**
         * Get the identifiers for services that are disabled.
         *
         * @return the disatbled services
         */
        @ManagedAttribute(description="Disabled Services", currencyTimeLimit=30)
        public Set<Goid> getDisabledServices() {
            cache.rwlock.readLock().lock();
            try {
                return new TreeSet<Goid>(cache.servicesThatAreDisabled);
            } finally {
                cache.rwlock.readLock().unlock();
            }
        }
    }
}
