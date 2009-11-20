/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.gateway.common.LicenseException;
import com.l7tech.gateway.common.audit.AuditDetailMessage;
import com.l7tech.gateway.common.audit.MessageProcessingMessages;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.CircularPolicyException;
import com.l7tech.policy.InvalidPolicyException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyDeletionForbiddenException;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.*;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.event.PolicyCacheEvent;
import com.l7tech.server.event.system.LicenseEvent;
import com.l7tech.server.event.system.PolicyReloadEvent;
import com.l7tech.server.event.system.Started;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.*;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.Closeable;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Policy cache maintains ServerPolcies / Policies and their metadata.
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
@ManagedResource(description="Policy Cache", objectName="l7tech:type=PolicyCache")
public class PolicyCacheImpl implements PolicyCache, ApplicationContextAware, ApplicationListener {

    //- PUBLIC

    /**
     * Create a PolicyCache.
     *
     * @param transactionManager  the transaction manager, or null to avoid attempting to have cache updates participate in transactions.
     * @param policyFactory the policy factory to use for compiling policy trees.  Required.       
     */
    public PolicyCacheImpl( final PlatformTransactionManager transactionManager,
                            final ServerPolicyFactory policyFactory ) {
        this.transactionManager = transactionManager;
        this.policyFactory = policyFactory;
    }

    /**
     *
     */
    public void setPolicyManager( final PolicyManager policyManager ) {
        this.policyManager = policyManager;
    }

    /**
     *
     */
    @Override
    public void setApplicationContext( final ApplicationContext applicationContext ) throws BeansException {
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
        if( policy == null ) {
            throw new IllegalArgumentException( "policy must not be null" );
        }

        return getPolicyMetadata( policy.getOid() );
    }

    @Override
    public PolicyMetadata getPolicyMetadata(long policyOid) {
        if( policyOid == Policy.DEFAULT_OID )
            throw new IllegalArgumentException( "Can't compile a brand-new policy--it must be saved first" );

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyOid );
            if( pce != null && pce.isValid() ) {
                return pce.getPolicyMetadata();
            }
        } finally {
            read.unlock();
        }

        return null;
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
        if( policy == null ) {
            throw new IllegalArgumentException( "policy must not be null" );
        }

        return getServerPolicy( policy.getOid() );
    }

    /**
     *
     */
    @Override
    public ServerPolicyHandle getServerPolicy( final long policyOid ) {
        if( policyOid == Policy.DEFAULT_OID )
            throw new IllegalArgumentException( "Can't compile a brand-new policy--it must be saved first" );

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyOid );
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
        if(policyGuid == null) {
            throw new IllegalArgumentException( "Can't compile a brand-new policy--it must be saved first" );
        }

        final Lock read = lock.readLock();
        read.lock();
        try {
            Long oid = guidToOidMap.get(policyGuid);
            if(oid != null) {
                PolicyCacheEntry pce = cacheGet( oid );
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
    public Set<String> getGlobalPoliciesByType( final PolicyType type ) {
        ensureCacheValid();

        final Set<String> guids = new HashSet<String>();
        final Lock read = lock.readLock();
        read.lock();
        try {
            for ( PolicyCacheEntry pce : policyCache.values() ) {
                if ( type == null || type == pce.policy.getType() ) {
                    guids.add( pce.policy.getGuid() );
                }
            }
        } finally {
            read.unlock();
        }

        return guids;
    }

    @Override
    public String registerGlobalPolicy( final String name,
                                        final PolicyType type,
                                        final String xml ) {
        ensureCacheValid();

        RegisteredPolicy policy = new RegisteredPolicy( name, type, xml );

        final Lock write = lock.writeLock();
        write.lock();
        try {
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

        Long oid = null;

        final Lock read = lock.readLock();
        read.lock();
        try {
            guidToPolicyMap.remove( policyGuid );
            oid = guidToOidMap.get( policyGuid );
        } finally {
            read.unlock();
        }

        if(oid != null) {
            removeInternal( oid );
        }
    }

    /**
     *
     */
    @Override
    public Map<Long, Integer> getDependentVersions( final long policyOid ) {
        ensureCacheValid();

        Map<Long,Integer> dependentVersions;

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyOid );
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
    public String getUniquePolicyVersionIdentifer( final long policyOid ) {
        ensureCacheValid();

        String uniqueId = null;

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyOid );
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

        if( policy.getOid() == Policy.DEFAULT_OID )
            throw new IllegalArgumentException( "Can't update a brand-new policy--it must be saved first" );

        updateInternal(new Policy(policy, true));
    }

    /**
     *
     */
    @Override
    public void validate( final Policy policy ) throws CircularPolicyException {
        ensureCacheValid();

        if ( policy == null) throw new IllegalArgumentException( "policy must not be null" );

        // get using policies
        Set<String> policies = toPolicyGuids( findUsagesInternal( policy.getOid() ) );
        if ( !policies.isEmpty() ) {
            final Assertion thisRootAssertion;
            try {
                thisRootAssertion = policy.getAssertion();

                final Iterator assit = thisRootAssertion.preorderIterator();
                while( assit.hasNext() ) {
                    final Assertion ass = (Assertion) assit.next();
                    if( ass instanceof Include ) {
                        final Include include = (Include) ass;
                        if ( policies.contains( include.getPolicyGuid() ) ) {
                            throw new CircularPolicyException( policy, include.getPolicyGuid(), include.getPolicyName() );
                        }
                    }
                }
            } catch( IOException ioe ) {
                // the policy is invalid, but that's ok
            }
        }
    }

    /**
     *
     */
    @Override
    public void validateRemove( final long oid ) throws PolicyDeletionForbiddenException {
        ensureCacheValid();

        final Lock read = lock.readLock();
        read.lock();
        try {
            ensureNotUsed( oid );
        } finally {
            read.unlock();
        }

    }

    /**
     *
     */
    @Override
    public Set<Policy> findUsages( final long oid ) {
        ensureCacheValid();

        return findUsagesInternal( oid );
    }

    /**
     *
     */
    @Override
    public boolean remove( final long oid ) {
        ensureCacheValid();

        return removeInternal( oid );
    }

    /**
     *
     */
    @ManagedOperation(description="Rebuild Policy Cache")
    public void initializePolicyCache() {
        logAndAudit( MessageProcessingMessages.POLICY_CACHE_BUILD );

        List<PolicyCacheEvent> events = new ArrayList<PolicyCacheEvent>();
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
            guidToOidMap.clear();

            // rebuild
            for( Policy policy : policyManager.findAll() ) {
               findDependentPoliciesIfDirty( policy, null, new HashSet<Long>(), new HashMap<Long, Integer>(), events );
            }

            // rebuild registered
            for ( RegisteredPolicy policy : guidToPolicyMap.values() ) {
                findDependentPoliciesIfDirty( policy, null, new HashSet<Long>(), new HashMap<Long, Integer>(), events );
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
    }

    /**
     *
     */
    @Override
    public void onApplicationEvent( final ApplicationEvent applicationEvent ) {
        if( applicationEvent instanceof LicenseEvent || applicationEvent instanceof AssertionModuleRegistrationEvent ) {
            transactionIfAvailable( new Runnable() {
                @Override
                public void run() {
                    ensureCacheValid();
                    resetUnlicensed();
                }
            });
        } else if( applicationEvent instanceof AssertionModuleUnregistrationEvent ) {
            transactionIfAvailable( new Runnable() {
                @Override
                public void run() {
                    ensureCacheValid();
                    resetAllForModuleUnload();
                }
            });
        } else if( applicationEvent instanceof EntityInvalidationEvent ) {
            EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;

            if( !Policy.class.isAssignableFrom( event.getEntityClass() ) ) return;

            ensureCacheValid();
            long policyOid = -1;
            try {
                for( long oid : event.getEntityIds() ) {
                    policyOid = oid;
                    Policy policy = policyManager.findByPrimaryKey( oid );
                    if( policy == null ) {
                        notifyDelete( oid );
                    } else {
                        notifyUpdate( new Policy(policy, true) );
                    }
                }
            } catch( FindException fe ) {
                markDirty();
                logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { Long.toString(policyOid) }, fe );
            }
            publishReload();
        } else if ( applicationEvent instanceof Started ) {
            transactionIfAvailable( new Runnable() {
                @Override
                public void run() {
                    initializePolicyCache();
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
    public Set<Long> getPolicies() {
        lock.readLock().lock();
        try {
            return new TreeSet<Long>(policyCache.keySet());
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
    public Set<Long> getUnlicensedPolicies() {
        lock.readLock().lock();
        try {
            return new TreeSet<Long>(policiesThatAreUnlicensed);
        } finally {
            lock.readLock().unlock();
        }
    }

    //- PROTECTED

    protected void setTraceLevel(final Level level) {
        TRACE_LEVEL = level;
    }

    /**
     *
     */
    protected boolean isInCache( final Long policyOid ) {
        boolean cached = false;

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( policyOid );
            cached = pce != null;
        } finally {
            read.unlock();
        }

        return cached;
    }

    /**
     *
     */
    protected void setLicenseStatus( final Long policyOid, final boolean licensed ) {
        if ( licensed ) {
            policiesThatAreUnlicensed.remove( policyOid );
        } else {
            policiesThatAreUnlicensed.add( policyOid );
        }
    }

    /**
     * Caller is responsible for ensuring close of the returned server assertion
     */
    protected ServerAssertion buildServerPolicy( final Policy policy )
            throws ServerPolicyInstantiationException, ServerPolicyException, InvalidPolicyException {
        ServerAssertion serverRootAssertion;

        // build policy
        Assertion ass;
        try {
            ass = policy.getAssertion();
        } catch( IOException ioe ) {
            throw new InvalidPolicyException( "Invalid policy with id " + policy.getOid() + ": " + ExceptionUtils.getMessage( ioe ), ioe );
        }

        // build server policy, create dummy if unlicensed        
        try {
            serverRootAssertion = policyFactory.compilePolicy( ass, true );
            setLicenseStatus( policy.getOid(), true );
        } catch( LicenseException le ) {
            setLicenseStatus( policy.getOid(), false );
            logAndAudit( MessageProcessingMessages.POLICY_CACHE_UNLICENSED, policy.getName(), Long.toString( policy.getOid() ), ExceptionUtils.getMessage( le ));
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

    private static final Logger logger = Logger.getLogger( PolicyCacheImpl.class.getName() );

    private Level TRACE_LEVEL = Level.FINEST;
    private final PlatformTransactionManager transactionManager;
    private Auditor auditor;
    private ApplicationEventPublisher eventSink;
    private PolicyManager policyManager;
    private final ServerPolicyFactory policyFactory;


    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private boolean cacheIsInvalid = true;
    private boolean initialized = false;
    private final Set<Long> policiesThatAreUnlicensed = new HashSet<Long>();
    private final Map<Long, PolicyCacheEntry> policyCache = new HashMap<Long, PolicyCacheEntry>(); // policy cache entries must be closed on removal
    private final Map<String, Long> guidToOidMap = new HashMap<String, Long>();
    private final Map<String, RegisteredPolicy> guidToPolicyMap = new HashMap<String,RegisteredPolicy>();

    private void notifyUpdate( final Policy policy ) {
          updateInternal( policy );
    }

    private void notifyDelete( final long oid ) {
        logger.log( Level.FINE, MessageFormat.format( "Policy #{0} has been deleted", oid ) );
        removeInternal( oid );
    }

    private Set<Policy> findUsagesInternal( final Long oid ) {
        Set<Policy> policies = new HashSet<Policy>();

        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyCacheEntry pce = cacheGet( oid );
            if( pce != null ) {
                final Set<Long> usedByOids = pce.usedBy;
                for( Long aLong : usedByOids ) {
                    PolicyCacheEntry userpce = cacheGet( aLong );
                    if( userpce != null ) {
                        policies.add( userpce.policy );
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
    private void findAllUsages( final Long oid, final Map<Long, Policy> usingPolicies ) {
        final Lock read = lock.readLock();
        read.lock();
        try {
            for( Policy policy : findUsagesInternal( oid ) ) {
                if( !usingPolicies.containsKey( policy.getOid() )) {
                    usingPolicies.put( policy.getOid(), policy );
                    findAllUsages( policy.getOid(), usingPolicies );
                }
            }
        } finally {
            read.unlock();
        }
    }

    /**
     * Caller must hold lock
     */
    private void updateUsage( final Long usingPolicyId, final boolean used, final Set<Long> policyIds ) {
        for( Long useeOid : policyIds ) {
            PolicyCacheEntry useePdi = cacheGet( useeOid );
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
        PolicyCacheEntry replaced = policyCache.put( pce.policyId, pce );
        if ( replaced != null ) {
            ResourceUtils.closeQuietly( replaced );
            guidToOidMap.remove(replaced.policy.getGuid());
            pce.usedBy.addAll( replaced.usedBy );
            updateUsage( pce.policyId, false, replaced.getUsedPolicyIds());
        }
        guidToOidMap.put(pce.policy.getGuid(), pce.policyId);
        updateUsage( pce.policyId, true, pce.getUsedPolicyIds());
    }

    /**
     * Remove an item from the cache, update the usage structure (usedBy)
     *
     * If a child is invalid and has no remaining users, it is removed from the
     * cache. No event is fired in this case since it is not a state change,
     * just book keeping.
     */
    private PolicyCacheEntry cacheRemove( final Long policyId ) {
        PolicyCacheEntry removed = policyCache.remove( policyId );
        if ( removed != null ) {
            ResourceUtils.closeQuietly( removed );
            guidToOidMap.remove(removed.policy.getGuid());
            updateUsage( policyId, false, removed.getUsedPolicyIds());

            Set<Long> policiesToRemove = new HashSet<Long>();
            for ( Long usedPolicyId : removed.getUsedPolicyIds() ) {
                PolicyCacheEntry entry = cacheGet( usedPolicyId );
                if ( entry != null && !entry.isValid() && entry.usedBy.isEmpty() ) {
                    policiesToRemove.add( usedPolicyId );      
                }
            }

            for ( Long removePolicyId : policiesToRemove ) {
                cacheRemove( removePolicyId );    
            }
        }
        return removed;
    }

    /**
     * Get an item from the cache
     */
    private PolicyCacheEntry cacheGet( final Long policyId ) {
        return policyCache.get( policyId );
    }

    /**
     * Policy should be read only
     */
    private void updateInternal( final Policy policy ) {
        if ( logger.isLoggable( Level.FINE )) {
            logger.log( Level.FINE,
                    "Policy #{0} ({1}) has been created or updated; updating caches",
                    new Object[]{ policy.getOid(), policy.getName() } );
        }

        List<PolicyCacheEvent> events = new ArrayList<PolicyCacheEvent>();
        final Lock write = lock.writeLock();
        write.lock();
        try {
            final Map<Long, Policy> usingPolicies = new HashMap<Long,Policy>();
            findAllUsages( policy.getOid(), usingPolicies );

            // mark self and users as dirty
            markDirty( Collections.singleton( policy.getOid() ) );
            markDirty( usingPolicies.keySet() );

            // rebuild self
            findDependentPolicies( policy, null, new HashSet<Long>(), new HashMap<Long, Integer>(), events );

            // rebuild parents
            for( Policy usingPolicy : usingPolicies.values() ) {
                findDependentPoliciesIfDirty( usingPolicy, null, new HashSet<Long>(), new HashMap<Long, Integer>(), events );
                trace( cacheGet( usingPolicy.getOid() ) );
            }

            trace( cacheGet( policy.getOid() ) );
        } finally {
            write.unlock();
        }

        for ( PolicyCacheEvent event : events ) {
            publishEvent( event );
        }
    }

    private boolean removeInternal( final long oid ) {
        boolean removedPolicy = false;
        final Policy removed;
        final Set<Long> invalidated = new HashSet<Long>();
        final Lock write = lock.writeLock();
        write.lock();
        try {
            PolicyCacheEntry pce = cacheGet( oid );
            if ( pce == null ) {
                removed = null;
                logger.log( Level.FINE, "No entry found for Policy #{0}, ignoring", oid );
            } else {
                removedPolicy = true;
                final Policy deletedPolicy = pce.policy;
                final Map<Long,Policy> usingPolicies = new HashMap<Long,Policy>();
                findAllUsages( oid, usingPolicies );
                markDirty( usingPolicies.keySet() );

                logger.log( Level.FINE, "Policy #{0} has ben deleted; removing from cache", oid );
                if ( usingPolicies.isEmpty() ) {
                    cacheRemove( oid );
                } else {
                    // cache as invalid, just in case the OID re-appears somehow
                    logger.log( Level.FINE, "Caching Policy #{0} as invalid for users: {1}", new Object[]{oid, usingPolicies.keySet()} );

                    // update users
                    recursiveInvalidate( deletedPolicy, null , usingPolicies.keySet(), invalidated );
                }

                removed = deletedPolicy;

                trace( cacheGet( oid ));
            }
        } finally {
            write.unlock();
        }

        if ( removed != null )
            publishEvent( new PolicyCacheEvent.Deleted(this, removed) );

        for ( Long policyId : invalidated ) {
            publishEvent( new PolicyCacheEvent.Invalid(this, policyId, null) );
        }

        return removedPolicy;
    }

    /**
     * Recursively invalidate the parents of the given policy.
     */
    private void recursiveInvalidate( Policy policy, Long usedPolicyId, Set<Long> policiesToInvalidate, Set<Long> invalidated ) {
        // invalidate the given policy
        Long policyId = policy.getOid();
        PolicyCacheEntry entry = cacheGet( policyId );
        if ( entry != null ) {
            invalidated.add( policyId );
            cacheReplace( new PolicyCacheEntry( entry.policy, usedPolicyId ) );
        }

        // invalidate parents
        Set<Policy> parents = findUsagesInternal( policyId );
        for( Policy parentPolicy : parents ) {
            Long parentPolicyId = parentPolicy.getOid();
            if ( policiesToInvalidate.contains( parentPolicyId )) {
                policiesToInvalidate.remove( parentPolicyId );
                recursiveInvalidate( parentPolicy, policyId, policiesToInvalidate, invalidated );
            }
        }
    }

    /**
     * Rebuild deps only if dirty.
     *
     * Caller must hold lock.
     *
     * WARNING: This will not populate the params unless the policy is dirty!
     */
    private void findDependentPoliciesIfDirty( final Policy thisPolicy,
                                               final Policy parentPolicy,
                                               final Set<Long> seenOids,
                                               final Map<Long, Integer> dependentVersions,
                                               final List<PolicyCacheEvent> events ) {
        PolicyCacheEntry pce = cacheGet( thisPolicy.getOid() );
        if ( pce == null || pce.isDirty() ) {
            findDependentPolicies(thisPolicy, parentPolicy, seenOids, dependentVersions, events );
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
     * @param seenOids          the Policy OIDs seen thus far in this policy stack, to detect cycles
     *                          (always pass a new, mutable <code>{@link Set}&lt;{@link Long}&gt;)
     * @param dependentVersions the OIDs and versions of policies seen thus far in this path through the policy tree, to
     *                          track dependencies. (always pass a new, mutable <code>{@link java.util.Map}&lt;Long, Integer&gt;)
     * @return the set of policies thisPolicy uses
     * //@throws InvalidPolicyException if this policy, or one of its descendants, could not be parsed
     * //@throws FindException          if one of this policy's descendants could not be loaded
     */
    private PolicyCacheEntry findDependentPolicies( final Policy thisPolicy,
                                                    final Policy parentPolicy,
                                                    final Set<Long> seenOids,
                                                    final Map<Long, Integer> dependentVersions,
                                                    final List<PolicyCacheEvent> events ) {
        Long thisPolicyId = thisPolicy.getOid();
        dependentVersions.put( thisPolicyId, thisPolicy.getVersion() );
        Set<Long> descendentPolicies = new HashSet<Long>();
        if ( logger.isLoggable( Level.FINE ) )
            logger.log( Level.FINE, "Processing Policy #{0} ({1})", new Object[]{ thisPolicyId, thisPolicy.getName() } );

        Assertion assertion = null;
        ServerAssertion serverAssertion = null;
        Exception exception = null;
        Long usedInvalidPolicyId = null;
        try {
            try {
                // validate / parse policy
                if( !seenOids.add( thisPolicyId ) ) {
                    throw new CircularPolicyException( parentPolicy, thisPolicy.getGuid(), thisPolicy.getName() );
                }

                try {
                    assertion = thisPolicy.getAssertion();
                } catch( IOException ioe ) {
                    throw new InvalidPolicyException( "Invalid policy with id " + thisPolicyId + ": " + ExceptionUtils.getMessage( ioe ), ioe );
                }

                // process included policies
                final Iterator assit = assertion.preorderIterator();
                while( assit.hasNext() ) {
                    final Assertion ass = (Assertion) assit.next();
                    if( !( ass instanceof Include ) ) continue;

                    final Include include = (Include) ass;
                    final String includedGuid = include.getPolicyGuid();
                    if ( logger.isLoggable( Level.FINE ) )
                        logger.log( Level.FINE, "Policy #{0} ({1}) includes Policy #{2} ({3})",
                                new Object[]{ thisPolicyId, thisPolicy.getName(), includedGuid, include.getPolicyName() } );
                    if( includedGuid == null )
                        throw new InvalidPolicyException( "Found Include assertion with no PolicyGUID in Policy #" + thisPolicy.getGuid() );
                    final Long includedOid;

                    // Get cached info for include, rebuild if required
                    PolicyCacheEntry includedInfo = null;
                    if(guidToOidMap.containsKey(includedGuid)) {
                        includedInfo = cacheGet( guidToOidMap.get(includedGuid) );
                    }
                    if( includedInfo == null || includedInfo.isDirty() ) {
                        if ( logger.isLoggable( Level.FINE ) )
                            logger.log( Level.FINE, "Creating new dependency info for #{0} ({1})",
                                    new Object[]{ includedGuid, include.getPolicyName() } );
                        final Policy  includedPolicy = policyManager.findByGuid( includedGuid );
                        if( includedPolicy == null ) {
                            throw new ServerPolicyInstantiationException("Include assertion in Policy #" + thisPolicy.getOid() + " refers to Policy #" + includedGuid + ", which does not exist");
                        }
                        includedOid = includedPolicy.getOid();
                        descendentPolicies.add( includedOid);
                        includedInfo = findDependentPolicies( new Policy(includedPolicy, true), thisPolicy, seenOids, dependentVersions, events );
                    } else {
                        includedOid = includedInfo.policyId;
                        descendentPolicies.add( includedOid );
                    }

                    if ( includedInfo.isValid() ) {
                        if ( logger.isLoggable( Level.FINE ) )
                            logger.log( Level.FINE, "Found dependency info for #{0} ({1})",
                                    new Object[]{ Long.toString( includedOid ), include.getPolicyName() } );
                        dependentVersions.putAll( includedInfo.getMetadata().getDependentVersions( true ));
                        descendentPolicies.addAll( includedInfo.getMetadata().getUsedPolicyIds( false ) );
                        for ( Long policyOid : includedInfo.getMetadata().getUsedPolicyIds( false ) ) {
                            if ( seenOids.contains( policyOid )) {
                                throw new CircularPolicyException( parentPolicy, thisPolicy.getGuid(), thisPolicy.getName() );
                            }
                        }
                    } else {
                        if ( logger.isLoggable( Level.FINE ) )
                            logger.log( Level.FINE, "Found dependency info for #{0} ({1}) [invalid policy]",
                                    new Object[]{ Long.toString( includedOid ), include.getPolicyName() } );
                        // Add usedBy so we are notified when this policy is updated
                        includedInfo.usedBy.add( thisPolicyId );
                        usedInvalidPolicyId = includedInfo.policyId;
                        throw new ServerPolicyInstantiationException("Include assertion in Policy #" + thisPolicy.getOid() + " refers to Policy #'"+includedOid+"' which is invalid");
                    }
                }

                // construct server policy
                serverAssertion = buildServerPolicy( thisPolicy );
            } catch ( ServerPolicyException spe ) {
                boolean alwaysAuditException = true;
                if (ExceptionUtils.causedBy(spe, PolicyAssertionException.class)) {
                    alwaysAuditException = false;
                }
                auditInvalidPolicy( thisPolicy, spe, alwaysAuditException );
                exception = spe;
            } catch ( ServerPolicyInstantiationException spie ) {
                auditInvalidPolicy( thisPolicy, spie, false );
                exception = spie;
            } catch ( CircularPolicyException cpe ) {
                auditInvalidPolicy( thisPolicy, cpe, false );
                exception = cpe;
            } catch ( InvalidPolicyException ipe ) {
                auditInvalidPolicy( thisPolicy, ipe, false );
                exception = ipe;
            } catch ( FindException fe ) {
                markDirty();
                logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { Long.toString(thisPolicyId) }, fe );
                exception = fe;
            }

            // update cache structure (even if policy is invalid)
            seenOids.remove( thisPolicyId );

            PolicyCacheEntry pce;
            if ( serverAssertion != null ) {
                ServerPolicy ServerPolicy = new ServerPolicy( thisPolicy, collectMetadata( assertion, descendentPolicies ), descendentPolicies, dependentVersions, serverAssertion );
                pce = new PolicyCacheEntry( thisPolicy, ServerPolicy, null );
            } else {
                pce = new PolicyCacheEntry( thisPolicy, usedInvalidPolicyId );
            }

            cacheReplace(pce);
            serverAssertion = null; // null here since serverAssertion now in the cache, and will be closed on removal

            if ( pce.isValid() ) {
                events.add( new PolicyCacheEvent.Updated( this, thisPolicy ) );
            } else {
                events.add( new PolicyCacheEvent.Invalid( this, thisPolicyId, exception ) );
            }

            return pce;
        } finally {
            ResourceUtils.closeQuietly( serverAssertion );
        }
    }

    private void auditInvalidPolicy( final Policy policy, final Exception exception, final boolean alwaysAuditException ) {
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
    private void markDirty( final Set<Long> policyIds ) {
        for ( Long policyId : policyIds ) {
            PolicyCacheEntry entry = cacheGet( policyId );
            if ( entry != null ) {
                entry.markDirty();
            }
        }
    }

    private void ensureNotUsed( final long oid ) throws PolicyDeletionForbiddenException {
        PolicyCacheEntry pce = cacheGet( oid );
        if( pce != null ) {
            if( !pce.usedBy.isEmpty() ) {
                for ( Long id : pce.usedBy ) {
                    final PolicyCacheEntry entry = cacheGet( id );
                    if ( entry != null ) {
                        throw new PolicyDeletionForbiddenException( pce.policy, EntityType.POLICY, entry.policy );
                    }
                }
            }
       }
    }

    private Set<String> toPolicyGuids( final Set<Policy> policies ) {
        Set<String> guids = new HashSet<String>();

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
    private PolicyMetadata collectMetadata( final Assertion rootAssertion, final Set<Long> usedPolicyOids ) {
        boolean tarariWanted = false;
        boolean wssInPolicy = false;
        boolean multipartInPolicy = false;
        Iterator i = rootAssertion.preorderIterator();
        while( i.hasNext() ) {
            Assertion ass = (Assertion) i.next();
            if( Assertion.isHardwareAccelerated( ass ) ) {
                tarariWanted = true;
            } else if( Assertion.isRequest( ass ) && Assertion.isWSSecurity( ass ) ) {
                wssInPolicy = true;
            } else if ( Assertion.isMultipart( ass ) && Assertion.isRequest( ass ) ) {
                multipartInPolicy = true;
            }
        }

        if( !( tarariWanted && wssInPolicy && multipartInPolicy ) ) {
            // need to check included policies also (which must have been cached before calling)
            for( Long usedPolicyOid : usedPolicyOids ) {
                PolicyCacheEntry entry = cacheGet( usedPolicyOid );
                if( entry != null && entry.isValid() ) {
                    PolicyMetadata metadata = entry.handle.getPolicyMetadata();
                    tarariWanted = tarariWanted || metadata.isTarariWanted();
                    wssInPolicy = wssInPolicy || metadata.isWssInPolicy();
                    multipartInPolicy = multipartInPolicy || metadata.isMultipart();
                }
            }
        }

        final boolean metaTarariWanted = tarariWanted;
        final boolean metaWssInPolicy = wssInPolicy;
        final boolean metaMultipartInPolicy = multipartInPolicy;
        return new PolicyMetadata() {
            @Override
            public boolean isTarariWanted() {
                return metaTarariWanted;
            }

            @Override
            public boolean isWssInPolicy() {
                return metaWssInPolicy;
            }

            @Override
            public boolean isMultipart() {
                return metaMultipartInPolicy;
            }
        };
    }

    private void resetUnlicensed() {
        Set<Long> unlicensed;

        final Lock read = lock.readLock();
        read.lock();
        try {
            unlicensed = new HashSet<Long>(policiesThatAreUnlicensed);
        } finally {
            read.unlock();
        }

        int numUnlicensed = unlicensed.size();
        if (numUnlicensed < 1) return;

        logAndAudit( MessageProcessingMessages.POLICY_CACHE_RESETTING_POLICIES, Integer.toString(numUnlicensed) );

        reset( unlicensed );
    }

    private void resetAllForModuleUnload() {
        Set<Long> forReset;

        final Lock read = lock.readLock();
        read.lock();
        try {
            forReset = new HashSet<Long>( policyCache.keySet() );
        } finally {
            read.unlock();
        }

        logAndAudit( MessageProcessingMessages.POLICY_CACHE_MODULE_UNLOAD );

        reset( forReset );
    }

    private void reset( final Set<Long> policyIds ) {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            markDirty( policyIds );
            for ( Long oid : policyIds ) {
                PolicyCacheEntry pce = cacheGet(oid);
                if (pce == null) continue; // no longer relevant

                Policy policy = null;
                try {
                    policy = policyManager.findByPrimaryKey( oid );
                } catch ( FindException fe ) {
                    markDirty();
                    logAndAudit( MessageProcessingMessages.POLICY_CACHE_STORAGE_ERROR, new String[] { oid.toString() }, fe );
                }

                if ( policy != null ) {
                    updateInternal( new Policy(policy, true) );
                } else {
                    removeInternal( oid );
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
        private final Long policyId;
        private final Policy policy;

        // one of these is two fields is required
        private final ServerPolicyHandle handle;
        private final ServerPolicy serverPolicy;
        private final Long usesPolicyId;

        private final Set<Long> usedBy;
        private boolean dirty = false;

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
                                  final Set<Long> usedBy ) {
            if ( policy == null ) throw new IllegalArgumentException("policy must not be null");
            if ( serverPolicy == null ) throw new IllegalArgumentException("serverPolicy must not be null");
            this.policyId = policy.getOid();
            this.policy = policy;
            this.handle = serverPolicy.ref();
            this.serverPolicy = serverPolicy;
            this.usesPolicyId = null;
            this.usedBy = new HashSet<Long>();
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
                                  final Long usesPolicyId ) {
            if ( policy == null ) throw new IllegalArgumentException("policy must not be null");
            this.policyId = policy.getOid();
            this.policy = policy;
            this.handle = null;
            this.serverPolicy = null;
            this.usesPolicyId = usesPolicyId;
            this.usedBy = new HashSet<Long>();
        }

        private ServerPolicyMetadata getMetadata() {
            if ( handle == null ) {
                return null;
            } else {
                return handle.getMetadata();
            }
        }

        private Set<Long> getUsedPolicyIds() {
            Set<Long> policyIds;

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

        public boolean isDirty() {
            return dirty;
        }

        public void markDirty() {
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
    }

    private static final class RegisteredPolicy extends Policy {
        private static final AtomicLong registeredOidCounter = new AtomicLong(-100000);

        RegisteredPolicy( final String name,
                          final PolicyType type,
                          final String xml ){
            if ( name==null || name.trim().isEmpty() ) throw new IllegalArgumentException("name is required.");
            if ( type==null ) throw new IllegalArgumentException("type is required.");
            if ( xml==null || xml.trim().isEmpty() ) throw new IllegalArgumentException("xml is required.");

            setOid( registeredOidCounter.getAndDecrement() );
            setGuid( UUID.randomUUID().toString() );
            setName( name );
            setType( type );
            setXml( xml );
            lock();
        }
    }
}
