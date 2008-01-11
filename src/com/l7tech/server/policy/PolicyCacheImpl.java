/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.LicenseException;
import com.l7tech.common.policy.CircularPolicyException;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Caches the compiled {@link ServerAssertion} versions of {@link Policy} objects, and maintains a dependency tree of
 * policies and their {@link Include}d policies, if any.
 */
public class PolicyCacheImpl implements PolicyCache, ApplicationListener, InitializingBean {

    //- PUBLIC

    public PolicyCacheImpl(ServerPolicyFactory policyFactory) {
        this.policyFactory = policyFactory;
    }

    // TODO check if version and/or XML are different, avoid needless recompiles for non-policy changes
    public ServerAssertion getServerPolicy(Policy policy) throws IOException, ServerPolicyException, LicenseException {
        final long oid = policy.getOid();
        if (oid == Policy.DEFAULT_OID) throw new IllegalArgumentException("Can't compile a brand-new policy--it must be saved first");

        final Lock read = lock.readLock();
        read.lock();
        try {
            ServerAssertion sass = serverPolicyCache.get(oid);
            if (sass != null) return sass;
        } finally {
            read.unlock();
        }

        // Must not hold either lock here, we're reentrant if an Include assertion is in the policy
        ServerAssertion sass = policyFactory.compilePolicy(policy.getAssertion(), true);

        final Lock write = lock.writeLock();
        write.lock();
        try {
            serverPolicyCache.put(oid, sass);
            return sass;
        } finally {
            write.unlock();
        }

    }

    public Map<Long, Integer> getDependentVersions(long policyOid) {
        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyDependencyInfo pdi = dependencyCache.get(policyOid);
            if (pdi == null) return null;
            return pdi.getDependentVersions(true);
        } finally {
            read.unlock();
        }
    }

    public String getUniquePolicyVersionIdentifer(long policyOid) {
        final Lock read = lock.readLock();
        read.lock();
        try {
            PolicyDependencyInfo pdi = dependencyCache.get(policyOid);
            if (pdi == null) return null;
            return pdi.policyUVID.getPolicyUniqueIdentifer();
        } finally {
            read.unlock();
        }
    }

    public ServerAssertion getServerPolicy(long policyOid)
            throws FindException, IOException, ServerPolicyException, LicenseException
    {
        ServerAssertion sass;

        final Lock read = lock.readLock();
        read.lock();
        try {
            sass = serverPolicyCache.get(policyOid);
            if (sass != null) return sass;
        } finally {
            read.unlock();
        }

        Policy policy = policyManager.findByPrimaryKey(policyOid);

        final Lock write = lock.writeLock();
        if (policy == null) {
            write.lock();
            try {
                serverPolicyCache.remove(policyOid);
                logger.info("Policy #" + policyOid + " has been deleted");
                return null;
            } finally {
                write.unlock();
            }
        } else {
            sass = policyFactory.compilePolicy(policy.getAssertion(), true);
            write.lock();
            try {
                serverPolicyCache.put(policyOid, sass);
                return sass;
            } finally {
                write.unlock();
            }
        }
    }

    public void update(Policy policy) throws ServerPolicyException, LicenseException, IOException {
        if (policy.getOid() == Policy.DEFAULT_OID) throw new IllegalArgumentException("Can't update a brand-new policy--it must be saved first");
        logger.log(Level.FINE, "Policy #{0} ({1}) has been created or updated; updating caches", new Object[] { policy.getOid(), policy.getName() });
        getServerPolicy(policy); // Refresh the cache, don't care about return value

        // Prevent reentrant calls from ServerInclude from deadlocking
        ServerAssertion sass = buildServerPolicy(policy);

        final Lock write = lock.writeLock();
        write.lock();
        try {
            final Set<Long> usingPolicies = new HashSet<Long>();
            findAllUsages(policy.getOid(), usingPolicies);

            findDependentPolicies(policy, null, new HashSet<Long>(), new HashMap<Long, Integer>());

            serverPolicyCache.remove(policy.getOid());
            serverPolicyCache.put(policy.getOid(), sass);

            usingPolicies.remove( policy.getOid() ); // don't allow policy to invalidate itself
            for ( Long policyOid : usingPolicies) {
                Policy usingPolicy = policyManager.findByPrimaryKey( policyOid );
                if ( usingPolicy != null ) {
                    findDependentPolicies(usingPolicy, null, new HashSet<Long>(), new HashMap<Long, Integer>());
                }
            }

            trace( dependencyCache.get( policy.getOid() ));
        } catch (FindException e) {
            throw new ServerPolicyException(policy.getAssertion(), "Included policy could not be loaded", e);
        } finally {
            write.unlock();
        }
    }

    public Set<Policy> findUsages(long oid) {
        final Lock read = lock.readLock();
        read.lock();
        try {
            Set<Policy> policies = new HashSet<Policy>();
            PolicyDependencyInfo pdi = dependencyCache.get(oid);
            if ( pdi != null ) {
                final Set<Long> usedByOids = pdi.usedBy;
                for (Long aLong : usedByOids) {
                    PolicyDependencyInfo pdiuser = dependencyCache.get(aLong);
                    if ( pdiuser != null ) {
                        policies.add(pdiuser.policy);
                    }
                }
            }
            return Collections.unmodifiableSet(policies);
        } finally {
            read.unlock();
        }
    }

    public void remove(long oid) throws PolicyDeletionForbiddenException {
        final Lock write = lock.writeLock();
        write.lock();
        try {
            PolicyDependencyInfo pdi = dependencyCache.get(oid);
            if (pdi == null) {
                logger.fine(MessageFormat.format("No dependency info found for Policy #{0}, ignoring", oid));
            } else {
                final Policy deletedPolicy = pdi.policy;
                if (!pdi.usedBy.isEmpty()) {
                    final Policy policy = dependencyCache.get(pdi.usedBy.iterator().next()).policy;
                    throw new PolicyDeletionForbiddenException(deletedPolicy, EntityType.POLICY, policy);
                }

                for (Long useeOid : pdi.uses) {
                    PolicyDependencyInfo useePdi = dependencyCache.get(useeOid);
                    if (useePdi == null) continue;
                    dependencyCache.put( useeOid, useePdi.removeUsingPolicy( deletedPolicy.getOid() ) );
                }
                logger.log(Level.FINE, "Policy #{0} has been deleted; removing from cache", oid);
                dependencyCache.remove(oid);
            }
            serverPolicyCache.remove(oid);
        } finally {
            write.unlock();
        }
    }

    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        if (applicationEvent instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;

            if (!Policy.class.isAssignableFrom(event.getEntityClass())) return;

            for (long oid : event.getEntityIds()) {
                Policy policy;
                try {
                    policy = policyManager.findByPrimaryKey(oid);
                } catch (FindException e) {
                    logger.log(Level.SEVERE, MessageFormat.format("Policy #{0} has been deleted or modified but cannot be retrieved from the database", oid), e);
                    continue;
                }

                if (policy == null) {
                    logger.log(Level.FINE, MessageFormat.format("Policy #{0} has been deleted", oid));
                    try {
                        remove(oid);
                    } catch (PolicyDeletionForbiddenException e) {
                        logger.log(Level.SEVERE, "Some other SSG has deleted a policy that is apparently still in use");
                    }
                } else {
                    try {
                        update(policy);
                    } catch (LicenseException e) {
                        logger.log(Level.SEVERE, MessageFormat.format("Policy #{0} ({1}) has been modified but now contains an unlicensed assertion", oid, policy.getName()), e);
                    } catch (ServerPolicyException e) {
                        Assertion ass = e.getAssertion();
                        String assname = ass == null ? null : ass.getClass().getSimpleName();
                        logger.log(Level.SEVERE,
                                MessageFormat.format("Policy #{0} ({1}) has been modified, but a(n) {2} assertion is failing to initialize", 
                                        oid, policy.getName(), assname), e);
                    } catch (IOException e) {
                        logger.log(Level.SEVERE, MessageFormat.format("Policy #{0} ({1}) has been modified, but can no longer be parsed", oid, policy.getName()), e);
                    }
                }
            }
        }
    }

    public void afterPropertiesSet() throws Exception {
        logger.info("Analyzing policy dependencies");
        final Lock write = lock.writeLock();
        write.lock();
        try {
            for (Policy policy : policyManager.findAll()) {
                findDependentPolicies(policy, null, new HashSet<Long>(), new HashMap<Long, Integer>());
            }
        } finally {
            write.unlock();
        }

        trace();
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }

    //- PROTECTED

    protected ServerAssertion buildServerPolicy(final Policy policy) throws IOException, LicenseException, ServerPolicyException {
        return policyFactory.compilePolicy(policy.getAssertion(), true);
    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(PolicyCacheImpl.class.getName());

    private static final Level TRACE_LEVEL = Level.FINEST;

    private PolicyManager policyManager;
    private final ServerPolicyFactory policyFactory;

    private final Map<Long, ServerAssertion> serverPolicyCache = new HashMap<Long, ServerAssertion>();
    private final Map<Long, PolicyDependencyInfo> dependencyCache = new HashMap<Long, PolicyDependencyInfo>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Find all usages of the policy, populate given set. 
     */
    private void findAllUsages( Long oid, Set<Long> usingPolicies ) {
        final Lock read = lock.readLock();
        read.lock();
        try {
            for ( Policy policy : findUsages(oid) ) {
                if ( usingPolicies.add( policy.getOid() )) {
                    findAllUsages( policy.getOid(), usingPolicies );
                }
            }
        } finally {
            read.unlock();
        }
    }

    /**
     * Examine the given policy to find all its dependent policies, namely those reachable via an {@link Include}
     * assertion in the policy, as well as any that can be found recursively in any policies referenced by
     * {@link Include} assertions.  Caller must hold write lock.
     *
     * @param thisPolicy the Policy to run the dependency check on
     * @param seenOids the Policy OIDs seen thus far in this policy stack, to detect cycles
     *        (always pass a new, mutable <code>{@link Set}&lt;{@link Long}&gt;)
     * @param dependentVersions the OIDs and versions of policies seen thus far in this path through the policy tree, to
     *                          track dependencies. (always pass a new, mutable <code>{@link java.util.Map}&lt;Long, Integer&gt;)
     * @return the set of policies thisPolicy uses
     * @throws IOException if this policy, or one of its descendants, could not be parsed
     * @throws FindException if one of this policy's descendants could not be loaded
     */
    private Set<Long> findDependentPolicies(final Policy thisPolicy,
                                            final Policy parentPolicy,
                                            final Set<Long> seenOids,
                                            final Map<Long, Integer> dependentVersions)
            throws IOException, FindException
    {
        Set<Long> descendentPolicies = new HashSet<Long>();
        logger.log(Level.FINE, "Processing Policy #{0} ({1})", new Object[] { thisPolicy.getOid(), thisPolicy.getName() });
        if ( !seenOids.add(thisPolicy.getOid()) ) {
            throw new CircularPolicyException(parentPolicy, thisPolicy.getOid(), thisPolicy.getName());
        }
        dependentVersions.put(thisPolicy.getOid(), thisPolicy.getVersion());

        final Assertion thisRootAssertion = thisPolicy.getAssertion();
        final Iterator assit = thisRootAssertion.preorderIterator();
        while (assit.hasNext()) {
            final Assertion ass = (Assertion) assit.next();
            if (!(ass instanceof Include)) continue;

            final Include include = (Include) ass;
            final Long includedOid = include.getPolicyOid();
            logger.log(Level.FINE, "Policy #{0} ({1}) includes Policy #{2} ({3})", new Object[] { thisPolicy.getOid(), thisPolicy.getName(), includedOid, include.getPolicyName() });
            if (includedOid == null) throw new RuntimeException("Found Include assertion with no PolicyOID in Policy #" + thisPolicy.getOid());

            descendentPolicies.add( includedOid );

            PolicyDependencyInfo includedInfo = dependencyCache.get(includedOid);
            final Policy includedPolicy;
            if (includedInfo == null) {
                logger.log(Level.FINE, "Creating new dependency info for #{0} ({1})", new Object[] { Long.toString(includedOid), include.getPolicyName() });
                includedPolicy = policyManager.findByPrimaryKey(includedOid);
                if (includedPolicy == null) {
                    logger.info("Include assertion in Policy #" + thisPolicy.getOid() + " refers to Policy #" + includedOid + ", which does not exist");
                    continue;
                }
            } else {
                includedPolicy = includedInfo.policy;
            }
            descendentPolicies.addAll( findDependentPolicies(includedPolicy, thisPolicy, seenOids, dependentVersions) );
            dependencyCache.put( includedOid, dependencyCache.get(includedOid).addUsingPolicy( thisPolicy.getOid() ) );
        }

        seenOids.remove(thisPolicy.getOid());
        Set<Long> usedBy = Collections.emptySet();
        Set<Long> oldUse = Collections.emptySet();
        PolicyDependencyInfo oldPDI = dependencyCache.get( thisPolicy.getOid() );
        if ( oldPDI != null ) {
            usedBy = oldPDI.usedBy;
            oldUse = oldPDI.uses;
        }
        dependencyCache.put( thisPolicy.getOid(), new PolicyDependencyInfo(thisPolicy, descendentPolicies, usedBy, dependentVersions) );

        if ( !oldUse.isEmpty() ) {
            Set<Long> noLongerUsed = new HashSet<Long>( oldUse );
            noLongerUsed.removeAll( descendentPolicies );
            for ( Long notIncludedOid : noLongerUsed ) {
                PolicyDependencyInfo pdi = dependencyCache.get(notIncludedOid);
                if ( pdi != null) {
                    dependencyCache.put( notIncludedOid, pdi.removeUsingPolicy( thisPolicy.getOid() ) );
                }
            }
        }

        return descendentPolicies;
    }

    private void trace() {
        if ( logger.isLoggable( TRACE_LEVEL )) {
            Lock read = lock.readLock();
            read.lock();
            try {
                for ( PolicyDependencyInfo dpi : dependencyCache.values() ) {
                    trace(dpi);
                }
            } finally {
                read.unlock();    
            }
        }
    }

    private void trace( final PolicyDependencyInfo pdi ) {
        if ( logger.isLoggable( TRACE_LEVEL )) {
            StringBuilder builder = new StringBuilder();

            builder.append( "Version: " );
            builder.append( pdi.policy.getVersion() );
            builder.append( '\n' );

            builder.append( "Unique Version: " );
            builder.append( pdi.policyUVID.getPolicyUniqueIdentifer() );
            builder.append( '\n' );

            builder.append( "Uses: " );
            builder.append( pdi.uses );
            builder.append( '\n' );

            builder.append( "Used By: " );
            builder.append( pdi.usedBy );
            builder.append( '\n' );

            logger.log( TRACE_LEVEL, "Dependency info for policy " + pdi.policy.getOid() + "\n" + builder.toString());
        }
    }

    private static class PolicyDependencyInfo {
        private final Policy policy;
        private final PolicyUniqueIdentifier policyUVID;
        private final Set<Long> uses;
        private final Set<Long> usedBy;
        private final Map<Long, Integer> dependentVersions;

        private PolicyDependencyInfo(final Policy policy,
                                     final Set<Long> uses,
                                     final Set<Long> usedBy,
                                     final Map<Long, Integer> dependentVersions) {
            this.policy = policy;
            this.uses = Collections.unmodifiableSet( new HashSet<Long>(uses) );
            this.usedBy = Collections.unmodifiableSet( new HashSet<Long>(usedBy) );
            this.dependentVersions = Collections.unmodifiableMap( new HashMap<Long,Integer>(dependentVersions) );
            this.policyUVID = buildPolicyUniqueIdentifier();
        }

        private Map<Long,Integer> getDependentVersions(final boolean includeSelf) {
            return policyUVID.getUsedPoliciesAndVersions( includeSelf );
        }

        private PolicyDependencyInfo addUsingPolicy(final Long policyOid) {
            if ( !usedBy.contains( policyOid )) {
                Set<Long> newUsedBy = new HashSet<Long>( usedBy );
                newUsedBy.add( policyOid );

                return new PolicyDependencyInfo( policy, uses, newUsedBy, dependentVersions );
            } else {
                return this;
            }
        }

        private PolicyDependencyInfo removeUsingPolicy(final Long policyOid) {
            if ( usedBy.contains( policyOid )) {
                Set<Long> newUsedBy = new HashSet<Long>( usedBy );
                newUsedBy.remove( policyOid );

                return new PolicyDependencyInfo( policy, uses, newUsedBy, dependentVersions );
            } else {
                return this;
            }
        }

        private PolicyUniqueIdentifier buildPolicyUniqueIdentifier() {
            Map<Long, Integer> usedPoliciesAndVersions = new HashMap<Long,Integer>();

            for ( Long policyOid : uses ) {
                Integer usedVersion = dependentVersions.get( policyOid );
                if ( usedVersion == null ) {
                    throw new IllegalArgumentException("Missing version for policy with oid " + policyOid);
                }
                usedPoliciesAndVersions.put( policyOid, usedVersion );
            }

            return new PolicyUniqueIdentifier( policy.getOid(), policy.getVersion(), usedPoliciesAndVersions );
        }
    }
}
