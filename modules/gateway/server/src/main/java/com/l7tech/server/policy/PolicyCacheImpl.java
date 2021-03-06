package com.l7tech.server.policy;

import com.google.common.annotations.VisibleForTesting;
import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.imp.PersistentEntityUtil;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.EntitiesProcessedInBatch;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.system.LicenseChangeEvent;
import com.l7tech.server.event.system.PolicyReloadEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.folder.FolderCache;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.module.AssertionModuleRegistrationEvent;
import com.l7tech.server.policy.module.AssertionModuleUnregistrationEvent;
import com.l7tech.server.service.ServiceAndPolicyCacheSharedLock;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.EmptyIterator;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Nullary;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.inject.Inject;
import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;
import static com.l7tech.server.event.EntityInvalidationEvent.CREATE;
import static com.l7tech.server.event.EntityInvalidationEvent.UPDATE;
import static com.l7tech.util.ArrayUtils.box;
import static com.l7tech.util.ArrayUtils.zipI;
import static java.lang.String.format;

/**
 * Policy cache maintains ServerPolicies / Policies and their metadata.
 *
 * <p>Caches the compiled {@link ServerAssertion} versions of {@link Policy}
 * objects, and maintains a dependency tree of policies and their
 * {@link Include}d policies, if any.</p>
 *
 * <p>When a policy is invalid, the tree structure is preserved to allow the
 * cache to be re-populated if it later becomes valid.</p>
 *
 * <p>This class publishes {@link PolicyCacheEvent} application events for any
 * changes to cached information. These events can be used to manage any items
 * associated with the policies (such as a service).</p>
 *
 * @see Policy
 * @see PolicyMetadata
 * @see ServerPolicy
 * @see ServerPolicyMetadata
 * @see PolicyCacheEvent
 */
@SuppressWarnings("Convert2Lambda")
@ManagedResource(description="Policy Cache", objectName="l7tech:type=PolicyCache")
public class PolicyCacheImpl implements PolicyCache, ApplicationContextAware, PostStartupApplicationListener {

    private static final String POLICY_MUST_NOT_BE_NULL = "policy must not be null";
    private static final String COMPILE_BRAND_NEW_POLICY = "Can't compile a brand-new policy--it must be saved first";
    private static final PolicyCacheEntry EMPTY_ENTRY = new PolicyCacheEntry();
    private static final Logger logger = Logger.getLogger( PolicyCacheImpl.class.getName() );
    private static final WspReader.Visibility cacheAssertionVisibility = WspReader.Visibility.omitDisabled;

    private Level TRACE_LEVEL = Level.FINEST;
    private final PlatformTransactionManager transactionManager;
    private Auditor auditor;
    private ApplicationEventPublisher eventSink;
    private PolicyManager policyManager;
    private PolicyVersionManager policyVersionManager;
    private final ServerPolicyFactory policyFactory;
    private final FolderCache folderCache;
    private final EntitiesProcessedInBatch entitiesProcessedInBatch;

    @Inject
    private DesignTimeEntityProvider entityProvider;

    private final ReadWriteLock lock = ServiceAndPolicyCacheSharedLock.getLock();
    private boolean cacheIsInvalid = true;
    private boolean initialized = false;
    private final Set<Goid> policiesThatAreUnlicensed = new HashSet<>();
    private final Map<Goid, PolicyCacheEntry> policyCache = new HashMap<>(); // policy cache entries must be closed on removal
    private final Map<String, Goid> guidToGoidMap = new HashMap<>();
    private final ConcurrentMap<Pair<PolicyType,String>, Set<String>> policyTypeAndTagToGuidsMap = new ConcurrentHashMap<>();
    private final Map<String, RegisteredPolicy> guidToPolicyMap = new HashMap<>();

    //- PUBLIC

    /**
     * Create a PolicyCache.
     *
     * @param transactionManager  the transaction manager, or null to avoid attempting to have cache updates participate in transactions.
     * @param policyFactory the policy factory to use for compiling policy trees.  Required.       
     */
    public PolicyCacheImpl( final PlatformTransactionManager transactionManager,
                            final ServerPolicyFactory policyFactory,
                            final FolderCache folderCache,
                            final EntitiesProcessedInBatch entitiesProcessedInBatch ) {
        this.transactionManager = transactionManager;
        this.policyFactory = policyFactory;
        this.folderCache = folderCache;
        this.entitiesProcessedInBatch = entitiesProcessedInBatch;
    }

    public void setPolicyManager( final PolicyManager policyManager ) {
        this.policyManager = policyManager;
    }

    public void setPolicyVersionManager( final PolicyVersionManager policyVersionManager ) {
        this.policyVersionManager = policyVersionManager;
    }

    /**
     *
     */
    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) {
        auditor = new Auditor(this, applicationContext, logger);
        eventSink = applicationContext;
    }

    /**
     *
     */
    public void setApplicationEventPublisher( final ApplicationEventPublisher applicationEventPublisher ) {
        eventSink = applicationEventPublisher;
    }

    @Override
    public PolicyMetadata getPolicyMetadata(Policy policy) {
        checkArgument(policy != null, POLICY_MUST_NOT_BE_NULL);

        return getPolicyMetadata( policy.getGoid() );
    }

    @Override
    public PolicyMetadata getPolicyMetadata(Goid policyGoid) {
        checkArgument(!Goid.isDefault(policyGoid), COMPILE_BRAND_NEW_POLICY);

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyGoid );
            if( pce != null && pce.isValid() ) {
                return pce.getPolicyMetadata();
            }
        } finally {
            read.unlock();
        }

        return null;
    }

    @Override
    public PolicyMetadata getPolicyMetadataByGuid(String guid) {

        final Lock read = lock.readLock();
        read.lock();
        try {
            Goid policyGoid = guidToGoidMap.get(guid);
            if (policyGoid != null) {
                checkArgument(!Goid.isDefault(policyGoid), COMPILE_BRAND_NEW_POLICY);

                PolicyCacheEntry pce = cacheGet( policyGoid );
                if( pce != null && pce.isValid() ) {
                    return pce.getPolicyMetadata();
                }
            }
        } finally {
            read.unlock();
        }

        return null;
    }

    @Override
    public List<Folder> getFolderPath( final Goid policyGoid ) {
        final PolicyCacheEntry pce = cacheGetWithLock(policyGoid);
        return pce!=null && pce.policy.getFolder()!=null ?
                folderCache.findPathByPrimaryKey( pce.policy.getFolder().getGoid() ) :
                Collections.emptyList();
    }

    @Override
    public boolean isStarted() {
        return initialized;
    }

    /**
     *
     */
    @Override
    public ServerPolicyHandle getServerPolicy( final Policy policy ) {
        checkArgument(policy != null, POLICY_MUST_NOT_BE_NULL);

        return getServerPolicy( policy.getGoid() );
    }

    /**
     *
     */
    @Override
    public ServerPolicyHandle getServerPolicy( final Goid policyGoid ) {
        checkArgument(!Goid.isDefault(policyGoid), COMPILE_BRAND_NEW_POLICY);

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyGoid );
            if( pce != null && pce.isValid() ) {
                return pce.serverPolicy.ref();
            }
        } finally {
            read.unlock();
        }

        return null;
    }

    @Override
    public ServerPolicyHandle getServerPolicy(final String policyGuid) {
        checkArgument(policyGuid != null, COMPILE_BRAND_NEW_POLICY);

        final Lock read = lock.readLock();
        read.lock();
        try {
            Goid goid = guidToGoidMap.get(policyGuid);
            if(goid != null) {
                PolicyCacheEntry pce = cacheGet( goid );
                if( pce != null && pce.isValid() ) {
                    return pce.serverPolicy.ref();
                }
            }
        } finally {
            read.unlock();
        }

        return null;
    }

    @Override
    public Set<String> getPoliciesByTypeAndTag( final PolicyType type,
                                                      final String tag ) {
        ensureCacheValid();

        final Pair<PolicyType,String> key = new Pair<>(type, tag);

        Set<String> guids = policyTypeAndTagToGuidsMap.get(key);
        if (guids != null) {
            return guids;
        }

        guids = new HashSet<>();
        final Lock read = lock.readLock();
        read.lock();
        try {
            for ( PolicyCacheEntry pce : policyCache.values() ) {
                if ( (type == null || type == pce.policy.getType()) &&
                     (tag == null || tag.equals( pce.policy.getInternalTag() ))) {
                    if ( pce.policy.isDisabled() ) {
                        logger.log( Level.INFO,
                                "Ignoring disabled ''{0}'' : #{1} ({2})",
                                new Object[]{ pce.policy.getType(), pce.policy.getGoid(), pce.policy.getName() }  );
                    } else {
                        guids.add( pce.policy.getGuid() );
                    }
                }
            }
        } finally {
            read.unlock();
        }

        policyTypeAndTagToGuidsMap.putIfAbsent(key, guids);

        return guids;
    }

    @Override
    public String registerGlobalPolicy( final String name,
                                        final PolicyType type,
                                        final String tag,
                                        final String xml ) {
        ensureCacheValid();

        RegisteredPolicy policy = new RegisteredPolicy( name, type, tag, xml );

        final Lock write = lock.writeLock();
        write.lock();
        try {
            policyTypeAndTagToGuidsMap.clear();
            guidToPolicyMap.put( policy.getGuid(), policy );
            updateInternal( policy );
        } finally {
            write.unlock();
        }

        return policy.getGuid();
    }

    @Override
    public void unregisterGlobalPolicy( final String policyGuid ) {
        ensureCacheValid();

        Goid goid;

        final Lock read = lock.readLock();
        read.lock();
        try {
            guidToPolicyMap.remove(policyGuid);
            goid = guidToGoidMap.get( policyGuid );
        } finally {
            read.unlock();
        }

        if(goid != null) {
            removeInternal( goid );
        }
    }

    /**
     *
     */
    @Override
    public Map<Goid, Integer> getDependentVersions( final Goid policyGoid ) {
        ensureCacheValid();

        Map<Goid,Integer> dependentVersions;

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyGoid );
            if( pce != null && pce.isValid() ) {
                dependentVersions = pce.getMetadata().getDependentVersions( true );
            } else {
                dependentVersions = Collections.emptyMap();                
            }
        } finally {
            read.unlock();
        }

        return dependentVersions;
    }

    /**
     *
     */
    @Override
    public String getUniquePolicyVersionIdentifer( final Goid policyGoid ) {
        ensureCacheValid();

        String uniqueId = null;

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyGoid );
            if( pce != null && pce.isValid() ) {
                uniqueId = pce.getMetadata().getPolicyUniqueIdentifier();
            }
        } finally {
            read.unlock();
        }

        return uniqueId;
    }

    /**
     *
     */
    @Override
    public void update( final Policy policy ) {
        ensureCacheValid();
        checkArgument(!Goid.isDefault(policy.getGoid()), "Can't update a brand-new policy--it must be saved first");

        perhapsUpdateInternal(preparedPolicy(policy));
    }

    /**
     * Get a prepared version of the specified policy that is safe to enroll in the policy cache.
     *
     * @param policy policy to prepare.  Required.
     * @return a read-only copy of this policy, with any design-time entities attached to the assertion beans.
     */
    @NotNull
    private Policy preparedPolicy(@NotNull Policy policy) {
        return new Policy(policy, cacheAssertionVisibility, true, entityProvider);
    }

    /**
     *
     */
    @Override
    public void validate( final Policy policy ) throws CircularPolicyException {
        ensureCacheValid();
        checkNotNull(policy, POLICY_MUST_NOT_BE_NULL);

        // get using policies
        Set<String> policies = toPolicyGuids( findUsagesInternal( policy.getGoid() ) );
        if (policies.isEmpty()) {
            return;
        }

        final Assertion thisRootAssertion;
        try {
            thisRootAssertion = policy.getAssertion();
            if (thisRootAssertion != null) {
                final Iterator assIt = thisRootAssertion.preorderIterator();
                while( assIt.hasNext() ) {
                    final Assertion ass = (Assertion) assIt.next();
                    if( ass instanceof Include ) {
                        final Include include = (Include) ass;
                        if ( policies.contains( include.getPolicyGuid() ) ) {
                            throw new CircularPolicyException( policy, include.getPolicyGuid(), include.getPolicyName() );
                        }
                    }
                }
            }
        } catch( IOException ioe ) {
            // the policy is invalid, but that's ok
        }
    }

    /**
     *
     */
    @Override
    public void validateRemove( final Goid goid ) throws PolicyDeletionForbiddenException {
        ensureCacheValid();

        final Lock read = lock.readLock();
        read.lock();
        try {
            ensureNotUsed( goid );
        } finally {
            read.unlock();
        }

    }

    /**
     *
     */
    @Override
    public Set<Policy> findUsages( final Goid goid ) {
        ensureCacheValid();

        return findUsagesInternal( goid );
    }

    /**
     *
     */
    @Override
    public boolean remove( final Goid goid ) {
        ensureCacheValid();

        return removeInternal( goid );
    }

    /**
     *
     */
    @ManagedOperation(description="Rebuild Policy Cache")
    public void initializePolicyCache() {
        final long startTime = System.currentTimeMillis();
        logAndAudit( MessageProcessingMessages.POLICY_CACHE_BUILD );

        List<PolicyCacheEvent> events = new ArrayList<>();
        final Lock write = lock.writeLock();
        write.lock();
        try {
            // record initialized
            initialized = true;

            // clear cached data
            policiesThatAreUnlicensed.clear();
            for ( PolicyCacheEntry pce : policyCache.values() ) {
                ResourceUtils.closeQuietly( pce );
            }
            policyCache.clear();
            guidToGoidMap.clear();
            policyTypeAndTagToGuidsMap.clear();

            // rebuild
            for( Policy policy : policyManager.findAll() ) {
                policy = preparedPolicy(policy);
                findDependentPoliciesIfDirty(policy, null, new HashSet<>(), new HashMap<>(), events);
            }

            // rebuild registered
            for ( RegisteredPolicy policy : guidToPolicyMap.values() ) {
                findDependentPoliciesIfDirty( policy, null, new HashSet<>(), new HashMap<>(), events );
            }

            cacheIsInvalid = false;
        } catch ( FindException fe ) {
            markDirty();
            logger.log( Level.WARNING, "Error accessing policies", fe );
        } finally {
            write.unlock();
        }

        for ( PolicyCacheEvent event : events ) {
            publishEvent( event );
        }
        publishEvent(new PolicyCacheEvent.Started(this));

        trace();
        logger.fine(new Supplier<String>() {
            @Override
            public String get() {
                return "Initialized policy cache in " + (System.currentTimeMillis()-startTime) + "ms.";
            }
        });
    }

    /**
     *
     */
    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if( applicationEvent instanceof LicenseChangeEvent || applicationEvent instanceof AssertionModuleRegistrationEvent) {
            transactionIfAvailable( new Runnable() {
                @Override
                public void run() {
                    ensureCacheValid();
                    resetUnlicensed();
                }
            });
        } else if( applicationEvent instanceof AssertionModuleUnregistrationEvent) {
            transactionIfAvailable( new Runnable() {
                @Override
                public void run() {
                    ensureCacheValid();
                    resetAllForModuleUnload();
                }
            });
        } else if( applicationEvent instanceof EntityInvalidationEvent) {
            final EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;

            processEntityInvalidationEvent(event);
        } else if ( applicationEvent instanceof Started ) {
            transactionIfAvailable( new Runnable() {
                @Override
                public void run() {
                    initializePolicyCache();
                }
            });
        }
    }

    private void processEntityInvalidationEvent(EntityInvalidationEvent event) {
        if( Policy.class.isAssignableFrom( event.getEntityClass() ) ) {
            processPolicyEntityInvalidationEvent(event);
        } else if ( PolicyVersion.class.isAssignableFrom( event.getEntityClass() ) ) {
            processPolicyVersionEntityInvalidationEvent(event);
        }
    }

    private void processPolicyEntityInvalidationEvent(EntityInvalidationEvent event) {
        ensureCacheValid();
        Goid policyGoid = Policy.DEFAULT_GOID;
        try {
            for( Goid goid : event.getEntityIds() ) {
                policyGoid = goid;
                Policy policy = policyManager.findByPrimaryKey( goid );
                if( policy == null ) {
                    notifyDelete( goid );
                } else {
                    final PolicyVersion activeVersionForPolicy = policyVersionManager.findActiveVersionForPolicy(goid);
                    if (activeVersionForPolicy != null) {
                        policyUpdate(policy, activeVersionForPolicy.getOrdinal());
                    } else {
                        notifyUpdate(preparedPolicy(policy));
                    }
                }
            }
        } catch( FindException fe ) {
            markDirty();
            logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { Goid.toString(policyGoid) }, fe );
        }
        publishReload();
    }

    private void processPolicyVersionEntityInvalidationEvent(EntityInvalidationEvent event) {
        // If an old policy version is activated or if an old policy version was active and is
        // edited then the version can change without any change to the policy entity. In this
        // case we reload the policy to pick up the updated version (revision) information.
        ensureCacheValid();

        Goid policyGoid = PolicyVersion.DEFAULT_GOID;
        try {
            Iterable<Pair<Goid, Character>> pairs = zipI(event.getEntityIds(), box(event.getEntityOperations()));
            List<Pair<Goid, Character>> iterList = newArrayList(pairs);

            for( final Pair<Goid,Character> entityInfo : iterList) {
                if ( ((int) CREATE) != entityInfo.right && ((int) UPDATE) != entityInfo.right) {
                    continue;
                }
                final Goid goid = entityInfo.left;
                final PolicyVersion version = policyVersionManager.findByPrimaryKey( goid );
                if ( version == null || !version.isActive() ) {
                    continue;
                }

                final PolicyCacheEntry pce = Optional.ofNullable(cacheGetWithLock( version.getPolicyGoid() )).orElse(EMPTY_ENTRY);
                final PolicyHeader policyHeader = pce.getPolicyHeader();
                if ( policyHeader != null &&
                        policyHeader.getPolicyRevision() != version.getOrdinal() ) {
                    final Policy policy = policyManager.findByPrimaryKey( version.getPolicyGoid() );

                    if( policy == null ) {
                        notifyDelete( version.getPolicyGoid() );
                    } else {
                        policyUpdate(policy, version.getOrdinal());
                    }
                }
            }
        } catch( FindException fe ) {
            markDirty();
            logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { Goid.toString(policyGoid) }, fe );
        }
    }

    private void policyUpdate(Policy policy, long versionOfPolicy) {
        if ( !entitiesProcessedInBatch.wasProcessed( policy.getGoid(), versionOfPolicy ) ) {
            entitiesProcessedInBatch.addProcessedEntity( policy.getGoid(), versionOfPolicy );
            notifyUpdate( preparedPolicy( policy ) );
        } else {
            logger.fine(new Supplier<String>() {
                @Override
                public String get() {
                    return format("Not recompiling policy %s with version %s as it was already processed in this batch", policy.getGoid(), versionOfPolicy);
                }
            });
        }
    }

    /**
     * Get the cache size.
     *
     * @return the number of services currently cached
     */
    @ManagedAttribute(description="Cache Size", currencyTimeLimit=30)
    public int getSize() {
        lock.readLock().lock();
        try {
            return policyCache.size();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the identifiers for cached policies.
     *
     * @return the cached policies
     */
    @ManagedAttribute(description="Cached Policies", currencyTimeLimit=30)
    public Set<Goid> getPolicies() {
        lock.readLock().lock();
        try {
            return new TreeSet<>(policyCache.keySet());
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Get the identifiers for unlicensed policies.
     *
     * @return the policies that are unlicensed
     */
    @ManagedAttribute(description="Unlicensed Policies", currencyTimeLimit=30)
    public Set<Goid> getUnlicensedPolicies() {
        lock.readLock().lock();
        try {
            return new TreeSet<>(policiesThatAreUnlicensed);
        } finally {
            lock.readLock().unlock();
        }
    }

    //- PROTECTED

    void setTraceLevel(final Level level) {
        TRACE_LEVEL = level;
    }

    /**
     *
     */
    boolean isInCache( final Goid policyGoid ) {
        return cacheGetWithLock( policyGoid ) != null;
    }

    /**
     *
     */
    void setLicenseStatus( final Goid policyGoid, final boolean licensed ) {
        if ( licensed ) {
            policiesThatAreUnlicensed.remove( policyGoid );
        } else {
            policiesThatAreUnlicensed.add( policyGoid );
        }
    }

    /**
     * Caller is responsible for ensuring close of the returned server assertion
     */
    protected ServerAssertion buildServerPolicy( final Policy policy )
            throws ServerPolicyException, InvalidPolicyException {
        ServerAssertion serverRootAssertion;

        // build policy
        Assertion ass = getAssertion(policy);

        checkValue(policy.getVisibility() == cacheAssertionVisibility, new Supplier<InvalidPolicyException>() {
            @Override
            public InvalidPolicyException get() {
                return new InvalidPolicyException("Policy has not been screened for disabled assertions");
            }
        });

        if (ass == null) {
            // Policy has no enabled assertions.  For policy enforcement purposes, we will treat it as though it were an empty All assertion
            // and build a server policy that always succeeds without doing anything.  This will match previous behavior since
            // ServerPolicyHandle.checkRequest() never checked whether the target root assertion was enabled and ServerAllAssertion would always
            // succeed if it had no enabled children.
            ass = new TrueAssertion();
        }

        // build server policy, create dummy if unlicensed        
        try {
            serverRootAssertion = policyFactory.compilePolicy( ass, true );
            setLicenseStatus( policy.getGoid(), true );
        } catch( LicenseException le ) {
            setLicenseStatus( policy.getGoid(), false );
            logAndAudit( MessageProcessingMessages.POLICY_CACHE_UNLICENSED, policy.getName(), Goid.toString( policy.getGoid() ), ExceptionUtils.getMessage( le ));
            final String message = "Assertion not available: " + ExceptionUtils.getMessage( le );
            serverRootAssertion = new AbstractServerAssertion<UnknownAssertion>( new UnknownAssertion() ) {
                @Override
                public AssertionStatus checkRequest( PolicyEnforcementContext context ) throws PolicyAssertionException {
                    throw new PolicyAssertionException( getAssertion(), message, new LicenseException(message) );
                }
            };
        }

        return serverRootAssertion;
    }

    protected void logAndAudit( final AuditDetailMessage message, final String ... params ) {
        auditor.logAndAudit( message, params );
    }

    protected void logAndAudit( final AuditDetailMessage message, final String[] params, final Exception ex ) {
        auditor.logAndAudit( message, params, ex );
    }

    // - PRIVATE

    private void notifyUpdate( final Policy policy ) {
          updateInternal( policy );
    }

    private void notifyDelete( final Goid goid ) {
        logger.log(Level.FINE, new Supplier<String>() {
            @Override
            public String get() {
                return MessageFormat.format( "Policy #{0} has been deleted", goid );
            }
        });
        removeInternal( goid );
    }

    private Set<Policy> findUsagesInternal( final Goid goid ) {
        Set<Policy> policies = new HashSet<>();

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( goid );
            if( pce != null ) {
                final Set<Goid> usedByOids = pce.usedBy;
                for( Goid aGoid : usedByOids ) {
                    PolicyCacheEntry userPce = cacheGet( aGoid );
                    if( userPce != null ) {
                        policies.add( userPce.policy );
                    }
                }
            }
        } finally {
            read.unlock();
        }

        return Collections.unmodifiableSet( policies );
    }

    /**
     * Find all usages of the policy, populate given Map.
     */
    private void findAllUsages( final Goid goid, final Map<Goid, Policy> usingPolicies ) {
        final Lock read = lock.readLock();
        read.lock();
        try {
            for( Policy policy : findUsagesInternal( goid ) ) {
                if( !usingPolicies.containsKey( policy.getGoid() )) {
                    usingPolicies.put( policy.getGoid(), policy );
                    findAllUsages( policy.getGoid(), usingPolicies );
                }
            }
        } finally {
            read.unlock();
        }
    }

    /**
     * Caller must hold lock
     */
    private void updateUsage( final Goid usingPolicyId, final boolean used, final Set<Goid> policyIds ) {
        for( Goid useeGoid : policyIds ) {
            PolicyCacheEntry useePdi = cacheGet(useeGoid);
            if( useePdi != null ) {
                if ( used ) {
                    useePdi.usedBy.add( usingPolicyId );
                } else {
                    useePdi.usedBy.remove( usingPolicyId );
                }
            }
        }
    }

    /**
     * Add an item to the cache, update the usage structure (usedBy)
     */
    private void cacheReplace( final PolicyCacheEntry pce ) {
        if (!PersistentEntityUtil.isLocked(pce.policy))
            throw new IllegalArgumentException("Unlocked policy may not be placed into the policy cache");
        PolicyCacheEntry replaced = policyCache.put( pce.policyId, pce );
        if ( replaced != null ) {
            ResourceUtils.closeQuietly( replaced );
            guidToGoidMap.remove(replaced.policy.getGuid());
            pce.usedBy.addAll( replaced.usedBy );
            updateUsage( pce.policyId, false, replaced.getUsedPolicyIds());
        }
        guidToGoidMap.put(pce.policy.getGuid(), pce.policyId);
        updateUsage( pce.policyId, true, pce.getUsedPolicyIds());
    }

    /**
     * Remove an item from the cache, update the usage structure (usedBy)
     *
     * If a child is invalid and has no remaining users, it is removed from the
     * cache. No event is fired in this case since it is not a state change,
     * just book keeping.
     */
    private PolicyCacheEntry cacheRemove( final Goid policyId ) {
        PolicyCacheEntry removed = policyCache.remove( policyId );
        if ( removed != null ) {
            ResourceUtils.closeQuietly( removed );
            guidToGoidMap.remove(removed.policy.getGuid());
            updateUsage( policyId, false, removed.getUsedPolicyIds());

            Set<Goid> policiesToRemove = new HashSet<>();
            for ( Goid usedPolicyId : removed.getUsedPolicyIds() ) {
                PolicyCacheEntry entry = cacheGet( usedPolicyId );
                if ( entry != null && !entry.isValid() && entry.usedBy.isEmpty() ) {
                    policiesToRemove.add( usedPolicyId );      
                }
            }

            for ( Goid removePolicyId : policiesToRemove ) {
                cacheRemove( removePolicyId );    
            }
        }
        return removed;
    }

    /**
     * Get an item from the cache
     */
    private PolicyCacheEntry cacheGet( final Goid policyId ) {
        return policyCache.get( policyId );
    }

    /**
     * Get an item from the cache with a read lock
     */
    private PolicyCacheEntry cacheGetWithLock( final Goid policyOid ) {
        PolicyCacheEntry pce;

        final Lock read = lock.readLock();
        read.lock();
        try {
            pce = cacheGet( policyOid );
        } finally {
            read.unlock();
        }

        return pce;
    }

    /**
     * Update only if the version has changed.
     *
     * Policy should be read only.
     */
    private void perhapsUpdateInternal( final Policy policy ) {
        final PolicyCacheEntry entry = cacheGetWithLock( policy.getGoid() );
        if ( entry == null ||
             entry.isDirty() ||
             !entry.isValid() ||
             entry.policy.getVersion() != policy.getVersion() ||
             !entry.policy.getXml().equals(policy.getXml()) ) {
            updateInternal( policy );
        }
    }

    /**
     * Policy should be read only
     */
    private void updateInternal( final Policy policy ) {
        logger.log(Level.FINE,
                new Supplier<String>() {
                    @Override
                    public String get() {
                        return MessageFormat.format("Policy #{0} ({1}) has been created or updated; updating caches", policy.getGoid(), policy.getName());
                    }
                });

        List<PolicyCacheEvent> events = new ArrayList<>();
        final Lock write = lock.writeLock();
        write.lock();
        try {
            policyTypeAndTagToGuidsMap.clear();

            final Map<Goid, Policy> usingPolicies = new HashMap<>();
            findAllUsages( policy.getGoid(), usingPolicies );

            // mark self and users as dirty
            markDirty( Collections.singleton( policy.getGoid() ) );
            markDirty( usingPolicies.keySet() );

            // rebuild self
            buildPolicyCacheEntry( policy, null, new HashSet<>(), new HashMap<>(), events );

            // rebuild parents
            for( Policy usingPolicy : usingPolicies.values() ) {
                findDependentPoliciesIfDirty( usingPolicy, null, new HashSet<>(), new HashMap<>(), events );
                trace( cacheGet( usingPolicy.getGoid() ) );
            }

            trace( cacheGet( policy.getGoid() ) );
        } finally {
            write.unlock();
        }

        for ( PolicyCacheEvent event : events ) {
            publishEvent( event );
        }
    }

    private boolean removeInternal( final Goid goid ) {
        boolean removedPolicy = false;
        final Policy removed;
        final Set<Goid> invalidated = new HashSet<>();
        final Lock write = lock.writeLock();
        write.lock();
        try {
            policyTypeAndTagToGuidsMap.clear();

            PolicyCacheEntry pce = cacheGet( goid );
            if ( pce == null ) {
                removed = null;
                logger.log( Level.FINE, "No entry found for Policy #{0}, ignoring", goid );
            } else {
                removedPolicy = true;
                final Policy deletedPolicy = pce.policy;
                final Map<Goid,Policy> usingPolicies = new HashMap<>();
                findAllUsages( goid, usingPolicies );
                markDirty( usingPolicies.keySet() );

                logger.log( Level.FINE, "Policy #{0} has ben deleted; removing from cache", goid );
                if ( usingPolicies.isEmpty() ) {
                    cacheRemove( goid );
                } else {
                    // cache as invalid, just in case the OID re-appears somehow
                    logger.log( Level.FINE, "Caching Policy #{0} as invalid for users: {1}", new Object[]{goid, usingPolicies.keySet()} );

                    // update users
                    recursiveInvalidate( deletedPolicy, null , usingPolicies.keySet(), invalidated );
                }

                removed = deletedPolicy;

                trace( cacheGet( goid ));
            }
        } finally {
            write.unlock();
        }

        if ( removed != null )
            publishEvent( new PolicyCacheEvent.Deleted(this, removed) );

        for ( Goid policyId : invalidated ) {
            publishEvent( new PolicyCacheEvent.Invalid(this, policyId, null) );
        }

        return removedPolicy;
    }

    /**
     * Recursively invalidate the parents of the given policy.
     */
    private void recursiveInvalidate( Policy policy, @Nullable Goid usedPolicyId, Set<Goid> policiesToInvalidate, Set<Goid> invalidated ) {
        // invalidate the given policy
        Goid policyId = policy.getGoid();
        PolicyCacheEntry entry = cacheGet( policyId );
        if ( entry != null ) {
            invalidated.add( policyId );
            cacheReplace( new PolicyCacheEntry( entry.policy, usedPolicyId ) );
        }

        // invalidate parents
        Set<Policy> parents = findUsagesInternal( policyId );
        for( Policy parentPolicy : parents ) {
            Goid parentPolicyId = parentPolicy.getGoid();
            if ( policiesToInvalidate.contains( parentPolicyId )) {
                policiesToInvalidate.remove( parentPolicyId );
                recursiveInvalidate( parentPolicy, policyId, policiesToInvalidate, invalidated );
            }
        }
    }

    /**
     * Rebuild dependencies only if dirty.
     *
     * Caller must hold lock.
     *
     * WARNING: This will not populate the params unless the policy is dirty!
     */
    private void findDependentPoliciesIfDirty( final Policy thisPolicy,
                                               final @Nullable Policy parentPolicy,
                                               final Set<Goid> seenGoids,
                                               final Map<Goid, Integer> dependentVersions,
                                               final List<PolicyCacheEvent> events ) {
        PolicyCacheEntry pce = cacheGet( thisPolicy.getGoid() );
        if ( pce == null || pce.isDirty() ) {
            buildPolicyCacheEntry(thisPolicy, parentPolicy, seenGoids, dependentVersions, events );
        }
    }

    /**
     * Examine the given policy to find all its dependent policies.
     *
     * <p>These are those reachable via an {@link Include} assertion in the
     * policy, as well as any that can be found recursively in any policies
     * referenced by {@link Include}d assertions.</p>
     *
     * <p>Caller must hold write lock.</p>
     *
     * @param thisPolicy        the Policy to run the dependency check on (should be read-only)
     * @param parentPolicy      the Policy that includes this policy, if known
     * @param seenGoids         the Policy OIDs seen thus far in this policy stack, to detect cycles
     *                          (always pass a new, mutable <code>{@link Set}&lt;{@link Long}&gt;)
     * @param dependentVersions the OIDs and versions of policies seen thus far in this path through the policy tree, to
     *                          track dependencies. (always pass a new, mutable <code>{@link java.util.Map}&lt;Long, Integer&gt;)
     * @return the set of policies thisPolicy uses
     * //@throws InvalidPolicyException if this policy, or one of its descendants, could not be parsed
     * //@throws FindException          if one of this policy's descendants could not be loaded
     */
    @VisibleForTesting
    PolicyCacheEntry buildPolicyCacheEntry(final Policy thisPolicy,
                                                   final @Nullable Policy parentPolicy,
                                                   final Set<Goid> seenGoids,
                                                   final Map<Goid, Integer> dependentVersions,
                                                   final List<PolicyCacheEvent> events ) {
        final Goid thisPolicyId = thisPolicy.getGoid();
        dependentVersions.put( thisPolicyId, thisPolicy.getVersion() );
        final Set<Goid> descendentPolicies = new HashSet<>();
        logger.log(Level.FINE, new Supplier<String>() {
            @Override
            public String get() {
                return MessageFormat.format("Processing Policy #{0} ({1})", thisPolicyId, thisPolicy.getName());
            }
        });

        Assertion assertion;
        PolicyMetadata meta = null;
        ServerAssertion serverAssertion = null;
        Exception exception = null;
        MutableObject<Goid> usedInvalidPolicyId = new MutableObject<>();
        try {
            try {
                // validate / parse policy
                checkValue(seenGoids.add(thisPolicyId), new Supplier<CircularPolicyException>() {
                    @Override
                    public CircularPolicyException get() {
                        return new CircularPolicyException( parentPolicy, thisPolicy.getGuid(), thisPolicy.getName() );
                    }
                });

                assertion = getAssertion(thisPolicy);

                // process included policies
                final Iterator<Assertion> assit = Optional.ofNullable(assertion.preorderIterator()).orElse(new EmptyIterator<>());
                while( assit.hasNext() ) {
                    final Assertion ass = assit.next();
                    if( !( ass instanceof Include ) ) {
                        continue;
                    }

                    final Include include = (Include) ass;
                    final String includedGuid = include.getPolicyGuid();
                    logger.log(Level.FINE, new Supplier<String>() {
                        @Override
                        public String get() {
                            return MessageFormat.format("Policy #{0} ({1}) includes Policy #{2} ({3})",
                                    thisPolicyId, thisPolicy.getName(), includedGuid, include.getPolicyName());
                        }
                    });

                    checkValue(includedGuid != null, new Supplier<InvalidPolicyException>() {
                        @Override
                        public InvalidPolicyException get() {
                            return new InvalidPolicyException( "Found Include assertion with no PolicyGUID in Policy #" + thisPolicy.getGuid() );
                        }
                    });

                    this.checkInclude(thisPolicy, parentPolicy, include, seenGoids, descendentPolicies, dependentVersions, events, usedInvalidPolicyId);
                }

                // construct server policy and related metadata
                meta = collectMetadata( thisPolicy, assertion, descendentPolicies );
                serverAssertion = buildServerPolicy( thisPolicy );
            } catch ( ServerPolicyException spe ) {
                boolean alwaysAuditException = true;
                if (ExceptionUtils.causedBy(spe, PolicyAssertionException.class)) {
                    alwaysAuditException = false;
                }
                auditInvalidPolicy( thisPolicy, spe, alwaysAuditException );
                exception = spe;
            } catch ( ServerPolicyInstantiationException | CircularPolicyException | InvalidPolicyException e ) {
                auditInvalidPolicy( thisPolicy, e, false );
                exception = e;
            } catch ( FindException fe ) {
                markDirty();
                logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { Goid.toString(thisPolicyId) }, fe );
                exception = fe;
            }

            // update cache structure (even if policy is invalid)
            seenGoids.remove( thisPolicyId );

            PolicyCacheEntry pce = createPolicyCacheEntry(thisPolicyId, thisPolicy, dependentVersions, descendentPolicies, meta, serverAssertion, usedInvalidPolicyId.getValue());

            cacheReplace(pce);
            serverAssertion = null; // null here since serverAssertion now in the cache, and will be closed on removal

            createPolicyCacheEvent(thisPolicyId, thisPolicy, exception, events, pce);

            return pce;
        } finally {
            ResourceUtils.closeQuietly( serverAssertion );
        }
    }

    private void checkInclude(Policy thisPolicy,
                              @Nullable Policy parentPolicy,
                              Include include,
                              Set<Goid> seenGoids,
                              Set<Goid> descendentPolicies,
                              Map<Goid, Integer> dependentVersions,
                              List<PolicyCacheEvent> events,
                              MutableObject<Goid> usedInvalidPolicyIdHolder)
            throws FindException, ServerPolicyInstantiationException, CircularPolicyException {
        String includedGuid = include.getPolicyGuid();
        final Goid includedGoid;

        // Get cached info for include, rebuild if required
        PolicyCacheEntry includedInfo = null;
        if(guidToGoidMap.containsKey(includedGuid)) {
            includedInfo = cacheGet( guidToGoidMap.get(includedGuid) );
        }
        if( includedInfo == null || includedInfo.isDirty() ) {

            logger.log(Level.FINE, new Supplier<String>() {
                @Override
                public String get() {
                    return MessageFormat.format("Creating new dependency info for #{0} ({1})", includedGuid, include.getPolicyName());
                }
            });

            final Policy  includedPolicy = policyManager.findByGuid( includedGuid );
            checkValue(includedPolicy != null, new Supplier<ServerPolicyInstantiationException>() {
                @Override
                public ServerPolicyInstantiationException get() {
                    return new ServerPolicyInstantiationException("Include assertion in Policy #" + thisPolicy.getGoid() + " refers to Policy #" + includedGuid + ", which does not exist");
                }
            });

            includedGoid = includedPolicy.getGoid();
            descendentPolicies.add( includedGoid);
            includedInfo = buildPolicyCacheEntry(preparedPolicy(includedPolicy), thisPolicy, seenGoids, dependentVersions, events );
        } else {
            includedGoid = includedInfo.policyId;
            descendentPolicies.add( includedGoid );
        }

        if ( includedInfo.isValid() ) {
            logger.log(Level.FINE, new Supplier<String>() {
                @Override
                public String get() {
                    return MessageFormat.format("Found dependency info for #{0} ({1})", Goid.toString( includedGoid ), include.getPolicyName());
                }
            });

            dependentVersions.putAll( includedInfo.getMetadata().getDependentVersions( true ));
            descendentPolicies.addAll( includedInfo.getMetadata().getUsedPolicyIds( false ) );
            for ( Goid policyGoid : includedInfo.getMetadata().getUsedPolicyIds( false ) ) {
                checkValue(!seenGoids.contains(policyGoid), new Supplier<CircularPolicyException>() {
                    @Override
                    public CircularPolicyException get() {
                        return new CircularPolicyException( parentPolicy, thisPolicy.getGuid(), thisPolicy.getName() );
                    }
                });
            }
        } else {
            logger.log(Level.FINE, new Supplier<String>() {
                @Override
                public String get() {
                    return MessageFormat.format("Found dependency info for #{0} ({1}) [invalid policy]", Goid.toString( includedGoid ), include.getPolicyName());
                }
            });

            // Add usedBy so we are notified when this policy is updated
            includedInfo.usedBy.add( thisPolicy.getGoid() );
            usedInvalidPolicyIdHolder.setValue(includedInfo.policyId);
            throw new ServerPolicyInstantiationException("Include assertion in Policy #" + thisPolicy.getGoid() + " refers to Policy #'"+includedGoid+"' which is invalid");
        }
    }

    private static Assertion getAssertion(Policy thisPolicy) throws InvalidPolicyException {
        Assertion assertion;
        try {
            assertion = thisPolicy.getAssertion();
        } catch( IOException ioe ) {
            throw new InvalidPolicyException( "Invalid policy with id " + thisPolicy.getGoid() + ": " + ExceptionUtils.getMessage( ioe ), ioe );
        }
        return assertion;
    }

    @NotNull
    private PolicyCacheEntry createPolicyCacheEntry(Goid thisPolicyId, Policy thisPolicy, Map<Goid, Integer> dependentVersions, Set<Goid> descendentPolicies, PolicyMetadata meta, ServerAssertion serverAssertion, Goid usedInvalidPolicyId) {
        PolicyCacheEntry pce;
        if ( serverAssertion != null ) {
            ServerPolicy serverPolicy = new ServerPolicy( thisPolicy, meta, descendentPolicies, dependentVersions, serverAssertion, new Nullary<Collection<Folder>>(){
                @Override
                public Collection<Folder> call() {
                    return getFolderPath( thisPolicyId );
                }
            } );
            pce = new PolicyCacheEntry(preparedPolicy(thisPolicy), serverPolicy, null );
        } else {
            pce = new PolicyCacheEntry(preparedPolicy(thisPolicy), usedInvalidPolicyId );
        }
        return pce;
    }

    private void createPolicyCacheEvent(Goid thisPolicyId, Policy thisPolicy, Exception exception, List<PolicyCacheEvent> events, PolicyCacheEntry pce) {
        if ( pce.isValid() ) {
            events.add( new PolicyCacheEvent.Updated( this, thisPolicy ) );
        } else {
            events.add( new PolicyCacheEvent.Invalid( this, thisPolicyId, exception ) );
        }
    }

    private void auditInvalidPolicy( final Policy policy, final Exception exception, final boolean alwaysAuditException ) {
        //noinspection ThrowableResultOfMethodCallIgnored
        logAndAudit( MessageProcessingMessages.POLICY_CACHE_INVALID,
                new String[] { policy.getName(), policy.getId(), ExceptionUtils.getMessage( exception )},
                alwaysAuditException ? exception : ExceptionUtils.getDebugException( exception ));
    }

    /**
     * Mark entire cache as invalid
     */
    private void markDirty() {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            policyTypeAndTagToGuidsMap.clear();
            cacheIsInvalid = true;
        } finally {
            write.unlock();
        }
    }

    /**
     * Mark the given policies as dirty
     *
     * Caller should hold lock 
     */
    private void markDirty( final Set<Goid> policyIds ) {
        for ( Goid policyId : policyIds ) {
            PolicyCacheEntry entry = cacheGet( policyId );
            if ( entry != null ) {
                entry.markDirty();
            }
        }
    }

    private void ensureNotUsed( final Goid goid ) throws PolicyDeletionForbiddenException {
        PolicyCacheEntry pce = cacheGet( goid );
        if( pce != null && !pce.usedBy.isEmpty() ) {
            for ( Goid id : pce.usedBy ) {
                final PolicyCacheEntry entry = cacheGet( id );
                if ( entry != null ) {
                    throw new PolicyDeletionForbiddenException( pce.policy, EntityType.POLICY, entry.policy );
                }
            }
       }
    }

    private Set<String> toPolicyGuids( final Set<Policy> policies ) {
        Set<String> guids = new HashSet<>();

        for ( Policy policy : policies ) {
            guids.add( policy.getGuid() );
        }

        return guids;
    }

    /**
     * Caller should hold write lock.
     *
     * Used policies must be in cache.
     */
    private PolicyMetadata collectMetadata( final Policy policy, final @Nullable Assertion rootAssertion, final Set<Goid> usedPolicyGoids ) throws FindException {
        // find policy revision
        long policyRevision = 0L;
        final PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(policy.getGoid());
        if ( activeVersion != null ) {
            policyRevision = activeVersion.getOrdinal();
        }

        // determine other metadata
        MutableBoolean wssInPolicy = new MutableBoolean(false);
        MutableBoolean multipartInPolicy = new MutableBoolean(false);

        Set<String> allVarsUsed = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, VariableMetadata> allVarsSet = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        Iterator<Assertion> i = rootAssertion != null ? rootAssertion.preorderIterator() : new EmptyIterator();
        while( i.hasNext() ) {
            Assertion ass = i.next();
            if ( !ass.isEnabled() ) {
                continue;
            }

            getAssertionMetadata(ass, wssInPolicy, multipartInPolicy);
            getAssertionVariables(allVarsUsed, allVarsSet, ass);
        }

        // need to check included policies also (which must have been cached before calling)
        for( Goid usedPolicyGoid : usedPolicyGoids ) {
            getIncludedPolicyMetadataAndVariables(usedPolicyGoid, wssInPolicy, multipartInPolicy, allVarsUsed, allVarsSet);
        }

        final boolean metaWssInPolicy = wssInPolicy.booleanValue();
        final boolean metaMultipartInPolicy = multipartInPolicy.booleanValue();
        final String[] metaVariablesUsed = allVarsUsed.toArray( new String[allVarsUsed.size()] );
        final VariableMetadata[] metaVariablesSet = allVarsSet.values().toArray(new VariableMetadata[0]);
        final PolicyHeader policyHeader = new PolicyHeader(policy, policyRevision);

        return new PolicyMetadata() {

            @Override
            public boolean isWssInPolicy() {
                return metaWssInPolicy;
            }

            @Override
            public boolean isMultipart() {
                return metaMultipartInPolicy;
            }

            @Override
            public String[] getVariablesUsed() {
                return metaVariablesUsed;
            }

            @Override
            public VariableMetadata[] getVariablesSet() {
                return metaVariablesSet;
            }

            @Override
            public PolicyHeader getPolicyHeader() {
                return policyHeader;
            }
        };
    }

    private void getIncludedPolicyMetadataAndVariables(Goid usedPolicyGoid, MutableBoolean wssInPolicy, MutableBoolean multipartInPolicy, Set<String> allVarsUsed, Map<String, VariableMetadata> allVarsSet) {
        PolicyCacheEntry entry = cacheGet( usedPolicyGoid );
        if( entry != null && entry.isValid() ) {
            PolicyMetadata metadata = entry.handle.getPolicyMetadata();
            wssInPolicy.setValue(wssInPolicy.booleanValue() || metadata.isWssInPolicy());
            multipartInPolicy.setValue(multipartInPolicy.booleanValue() || metadata.isMultipart());
            String[] varsUsed = metadata.getVariablesUsed();
            if (varsUsed != null) {
                allVarsUsed.addAll(Arrays.asList(varsUsed));
            }
            mergeVariablesSet(allVarsSet, PolicyVariableUtils.getVariablesSetNoThrow(metadata));
        }
    }

    private static void getAssertionMetadata(Assertion ass, MutableBoolean wssInPolicy, MutableBoolean multipartInPolicy) {
        if( Assertion.isRequest( ass ) && Assertion.isWSSecurity( ass ) ) {
            wssInPolicy.setValue(true);
        } else if ( Assertion.isMultipart( ass ) && Assertion.isRequest( ass ) ) {
            multipartInPolicy.setValue(true);
        }
    }

    private static void getAssertionVariables(Set<String> allVarsUsed, Map<String, VariableMetadata> allVarsSet, Assertion ass) {
        if (ass instanceof UsesVariables) {
            String[] vars = PolicyVariableUtils.getVariablesUsedNoThrow((UsesVariables) ass);
            if (vars != null) {
                allVarsUsed.addAll(Arrays.asList(vars));
            }
        }

        if (ass instanceof SetsVariables) {
            VariableMetadata[] vms = PolicyVariableUtils.getVariablesSetNoThrow((SetsVariables) ass);
            mergeVariablesSet(allVarsSet, vms);
        }
    }

    private static void mergeVariablesSet(Map<String, VariableMetadata> allVarsSet, VariableMetadata[] vms) {
        if (vms != null) {
            for (VariableMetadata vm : vms) {
                allVarsSet.put(vm.getName(), vm);
            }
        }
    }

    private void resetUnlicensed() {
        Set<Goid> unlicensed;

        final Lock read = lock.readLock();
        read.lock();
        try {
            unlicensed = new HashSet<>(policiesThatAreUnlicensed);
        } finally {
            read.unlock();
        }

        int numUnlicensed = unlicensed.size();
        if (numUnlicensed < 1) return;

        logAndAudit( MessageProcessingMessages.POLICY_CACHE_RESETTING_POLICIES, Integer.toString(numUnlicensed) );

        reset( unlicensed );
    }

    private void resetAllForModuleUnload() {
        Set<Goid> forReset;

        final Lock read = lock.readLock();
        read.lock();
        try {
            forReset = new HashSet<>(policyCache.keySet());
        } finally {
            read.unlock();
        }

        logAndAudit( MessageProcessingMessages.POLICY_CACHE_MODULE_UNLOAD );

        reset( forReset );
    }

    private void reset( final Set<Goid> policyIds ) {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            policyTypeAndTagToGuidsMap.clear();
            markDirty( policyIds );
            for ( Goid id : policyIds ) {
                PolicyCacheEntry pce = cacheGet(id);
                if (pce == null) continue; // no longer relevant

                Policy policy = null;
                try {
                    policy = policyManager.findByPrimaryKey( id );
                } catch ( FindException fe ) {
                    markDirty();
                    logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { id.toString() }, fe );
                }

                if ( policy != null ) {
                    updateInternal(preparedPolicy(policy));
                } else {
                    removeInternal( id );
                }
            }
        } finally {
            write.unlock();
        }

        publishEvent( new PolicyCacheEvent.Reset(this) );
    }

    private void ensureCacheValid() {
        boolean isInvalid;

        final Lock read = lock.readLock();
        read.lock();
        try {
            isInvalid = initialized && cacheIsInvalid;
        } finally {
            read.unlock();
        }

        if ( isInvalid ) {
            initializePolicyCache();
        }
    }

    private void transactionIfAvailable( final Runnable callback ) {
        if ( transactionManager != null ) {
            new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
                @Override
                protected void doInTransactionWithoutResult( final TransactionStatus status ) {
                    callback.run();
                }
            });
        } else {
            callback.run();
        }
    }
    
    private void publishEvent( final PolicyCacheEvent event ) {
        eventSink.publishEvent( event );
    }

    private void publishReload() {
        eventSink.publishEvent( new PolicyReloadEvent( this ) );
    }

    private void trace() {
        if( logger.isLoggable( TRACE_LEVEL ) ) {
            Lock read = lock.readLock();
            read.lock();
            try {
                for( PolicyCacheEntry dpi : policyCache.values() ) {
                    trace( dpi );
                }
            } finally {
                read.unlock();
            }
        }
    }

    private void trace( final PolicyCacheEntry pce ) {
        if( pce != null && logger.isLoggable( TRACE_LEVEL ) ) {
            StringBuilder builder = new StringBuilder();

            builder.append( "Version: " );
            builder.append( pce.policy.getVersion() );
            builder.append( '\n' );

            if ( pce.isValid() ) {
                builder.append( "Unique Version: " );
                builder.append( pce.getMetadata().getPolicyUniqueIdentifier() );
                builder.append( '\n' );

                builder.append( "Uses: " );
                builder.append( pce.getMetadata().getUsedPolicyIds(false) );
                builder.append( '\n' );
            } else {
                builder.append( "Status: Invalid\n" );
            }

            builder.append( "Used By: " );
            builder.append( pce.usedBy );
            builder.append( '\n' );

            logger.log( TRACE_LEVEL, "Dependency info for policy " + pce.policyId + "\n" + builder.toString() );
        }
    }

    private static class PolicyCacheEntry implements Closeable {
        private final Goid policyId;
        private final Policy policy;

        // one of these is two fields is required
        private final ServerPolicyHandle handle;
        private final ServerPolicy serverPolicy;
        private final Goid usesPolicyId;

        private final Set<Goid> usedBy;
        private boolean dirty = false;

        /**
         * Create a empty policy cache entry. Not intended to use more than once.
         */
        private PolicyCacheEntry() {
            this.policyId = null;
            this.policy = null;
            this.handle = null;
            this.serverPolicy = null;
            this.usesPolicyId = null;
            this.usedBy = null;
        }

        /**
         * Create a policy cache entry with the given server policy handle.
         *
         * <p>This constructor is for use with valid policies.</p>
         *
         * @param policy The policy, must not be null
         * @param serverPolicy The serverPolicy (must not be null)
         * @param usedBy Ids for policies that use this policy (may be null)
         */
        private PolicyCacheEntry( final Policy policy,
                                  final ServerPolicy serverPolicy,
                                  final Set<Goid> usedBy ) {
            if ( policy == null ) throw new IllegalArgumentException(POLICY_MUST_NOT_BE_NULL);
            if ( serverPolicy == null ) throw new IllegalArgumentException("serverPolicy must not be null");
            this.policyId = policy.getGoid();
            this.policy = policy;
            this.handle = serverPolicy.ref();
            this.serverPolicy = serverPolicy;
            this.usesPolicyId = null;
            this.usedBy = new HashSet<>();
            if ( usedBy != null ) {
                this.usedBy.addAll( usedBy );
            }
        }

        /**
         * Create a policy cache entry for an invalid policy.
         *
         * <p>A .</p>
         *
         * @param policy The policy, must not be null
         * @param usesPolicyId The id of the policy that this item depends on (may be null)
         */
        private PolicyCacheEntry( final Policy policy,
                                  final @Nullable Goid usesPolicyId ) {
            if ( policy == null ) throw new IllegalArgumentException(POLICY_MUST_NOT_BE_NULL);
            this.policyId = policy.getGoid();
            this.policy = policy;
            this.handle = null;
            this.serverPolicy = null;
            this.usesPolicyId = usesPolicyId;
            this.usedBy = new HashSet<>();
        }

        private ServerPolicyMetadata getMetadata() {
            if ( handle == null ) {
                return null;
            } else {
                return handle.getMetadata();
            }
        }

        private Set<Goid> getUsedPolicyIds() {
            Set<Goid> policyIds;

            if ( handle == null ) {
                if ( usesPolicyId == null ) {
                    policyIds = Collections.emptySet();                    
                } else {
                    policyIds = Collections.singleton(usesPolicyId);
                }
            } else {
                policyIds = handle.getMetadata().getUsedPolicyIds( false );
            }

            return policyIds;
        }

        public boolean isValid() {
            return handle != null; 
        }

        boolean isDirty() {
            return dirty;
        }

        void markDirty() {
            dirty = true;
        }

        @Override
        public void close() {
            if ( handle != null ) {
                handle.close();
            }
        }

        public PolicyMetadata getPolicyMetadata() {
          if ( handle == null ) {
                return null;
            } else {
                return handle.getPolicyMetadata();
            }
        }

        public PolicyHeader getPolicyHeader() {
            if ( handle == null || handle.getPolicyMetadata() == null) {
                return null;
            }
            return handle.getPolicyMetadata().getPolicyHeader();
        }
    }

    /**
     * Registered policies are ones that can be registered by assertions. These can be global policies for example. Registered policies should never by stored in the database!
     */
    private static final class RegisteredPolicy extends Policy {
        private static final AtomicLong registeredOidCounter = new AtomicLong( -100000L );

        RegisteredPolicy( final String name,
                          final PolicyType type,
                          final String tag,
                          final String xml ) {
            super(type, name, xml, false);
            if ( name==null || name.trim().isEmpty() ) throw new IllegalArgumentException("name is required.");
            if ( type==null ) throw new IllegalArgumentException("type is required.");
            if ( xml==null || xml.trim().isEmpty() ) throw new IllegalArgumentException("xml is required.");

            setGoid(new Goid(0,registeredOidCounter.getAndDecrement()));
            setGuid(UUID.randomUUID().toString());
            setInternalTag(tag);
            setVisibility(WspReader.Visibility.omitDisabled);
            super.lock();
        }
    }

    /**
     * Utility method to throw an exception if a condition is not true.
     *
     * @param value value to check
     * @param exceptionSupplier construct the exception
     * @param <T> type of exception
     * @throws T exception
     */
    private static <T extends Exception> void checkValue(boolean value, @NotNull Supplier<T> exceptionSupplier) throws T {
        if (!value) {
            throw exceptionSupplier.get();
        }
    }
}
