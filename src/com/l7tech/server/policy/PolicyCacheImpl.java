/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.LicenseException;
import com.l7tech.common.policy.CircularPolicyException;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.Pair;
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
    private static final Logger logger = Logger.getLogger(PolicyCacheImpl.class.getName());

    private PolicyManager policyManager;
    private final ServerPolicyFactory policyFactory;

    private final Map<Long, ServerAssertion> serverPolicyCache = new HashMap<Long, ServerAssertion>();
    private final Map<Long, PolicyDependencyInfo> dependencyCache = new HashMap<Long, PolicyCacheImpl.PolicyDependencyInfo>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public PolicyCacheImpl(ServerPolicyFactory policyFactory) {
        this.policyFactory = policyFactory;
    }

    // TODO check if version and/or XML are different, avoid needless recompiles for non-policy changes
    public ServerAssertion getServerPolicy(Policy policy) throws IOException, ServerPolicyException, LicenseException {
        final long oid = policy.getOid();
        if (oid == Policy.DEFAULT_OID) throw new IllegalArgumentException("Can't compile a brand-new policy--it must be saved first");
        Lock read = lock.readLock();
        Lock write = lock.writeLock();
        try {
            read.lock();
            ServerAssertion sass = serverPolicyCache.get(oid);
            if (sass != null) return sass;
            read.unlock(); read = null;

            // Must not hold either lock here, we're reentrant if an Include assertion is in the policy
            sass = policyFactory.compilePolicy(policy.getAssertion(), true);

            try {
                write.lock();
                serverPolicyCache.put(oid, sass);
                return sass;
            } finally {
                write.unlock();
            }
        } finally {
            if (read != null) read.unlock();
        }
    }

    public Map<Long, Integer> getDependentVersions(long policyOid) {
        Lock read = lock.readLock();
        try {
            read.lock();
            PolicyDependencyInfo pdi = dependencyCache.get(policyOid);
            if (pdi == null) return null;
            return pdi.dependentVersions;
        } finally {
            read.unlock();
        }
    }

    public ServerAssertion getServerPolicy(long policyOid)
            throws FindException, IOException, ServerPolicyException, LicenseException
    {
        ServerAssertion sass;

        Lock read = lock.readLock();
        try {
            read.lock();
            sass = serverPolicyCache.get(policyOid);
            if (sass != null) return sass;
        } finally {
            read.unlock();
        }

        Policy policy = policyManager.findByPrimaryKey(policyOid);

        final Lock write = lock.writeLock();
        try {
            if (policy == null) {
                write.lock();
                serverPolicyCache.remove(policyOid);
                logger.info("Policy #" + policyOid + " has been deleted");
                return null;
            } else {
                sass = policyFactory.compilePolicy(policy.getAssertion(), true);

                write.lock();
                serverPolicyCache.put(policyOid, sass);
                return sass;
            }
        } finally {
            write.unlock();
        }
    }

    public void update(Policy policy) throws ServerPolicyException, LicenseException, IOException {
        if (policy.getOid() == Policy.DEFAULT_OID) throw new IllegalArgumentException("Can't update a brand-new policy--it must be saved first");
        logger.log(Level.FINE, "Policy #{0} ({1}) has been created or updated; updating caches", new Object[] { policy.getOid(), policy.getName() });
        getServerPolicy(policy); // Refresh the cache, don't care about return value

        // Prevent reentrant calls from ServerInclude from deadlocking
        ServerAssertion sass = policyFactory.compilePolicy(policy.getAssertion(), true);

        this.lock.writeLock().lock();
        try {
            final PolicyDependencyInfo pdi = dependencyCache.get(policy.getOid());
            final Set<Long> seenOids = new HashSet<Long>();
            final Map<Long, Integer> dependentVersions = new HashMap<Long, Integer>();
            findDependentPolicies(policy, seenOids, dependentVersions);
            pdi.dependentVersions = Collections.unmodifiableMap(dependentVersions);
            updateUsedBy();
            serverPolicyCache.remove(policy.getOid());
            serverPolicyCache.put(policy.getOid(), sass);
        } catch (FindException e) {
            throw new ServerPolicyException(policy.getAssertion(), "Included policy could not be loaded", e);
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    public Set<Policy> findUsages(long oid) {
        try {
            lock.readLock().lock();
            PolicyDependencyInfo pdi = dependencyCache.get(oid);
            final Set<Long> usedByOids = pdi.usedBy;
            Set<Policy> policies = new HashSet<Policy>();
            for (Long aLong : usedByOids) {
                policies.add(dependencyCache.get(aLong).policy);
            }
            return Collections.unmodifiableSet(policies);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void remove(long oid) throws PolicyDeletionForbiddenException {
        lock.writeLock().lock();
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
                    useePdi.usedBy.remove(deletedPolicy.getOid());
                }
                logger.log(Level.INFO, "Policy #{0} has been deleted; removing from cache", oid);
                dependencyCache.remove(oid);
            }
            serverPolicyCache.remove(oid);
        } finally {
            lock.writeLock().unlock();
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
                    logger.log(Level.INFO, MessageFormat.format("Policy #{0} has been deleted", oid));
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
        lock.writeLock().lock();
        try {
            for (Policy policy : policyManager.findAll()) {
                findDependentPolicies(policy, new HashSet<Long>(), new HashMap<Long, Integer>());
            }
            updateUsedBy();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Caller must hold write lock.
     */
    private void updateUsedBy() {
        // TODO can this be tightened up from O(2n) to O(n)?
        for (PolicyCacheImpl.PolicyDependencyInfo pdi : dependencyCache.values()) {
            pdi.usedBy.clear();
        }

        for (PolicyCacheImpl.PolicyDependencyInfo pdi : dependencyCache.values()) {
            for (Long usedPolicyOid : pdi.uses) {
                PolicyCacheImpl.PolicyDependencyInfo usedPdi = dependencyCache.get(usedPolicyOid);
                usedPdi.usedBy.add(pdi.policy.getOid());
            }
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
     * @throws IOException if this policy, or one of its descendants, could not be parsed
     * @throws FindException if one of this policy's descendants could not be loaded
     */
    private void findDependentPolicies(final Policy thisPolicy,
                                       final Set<Long> seenOids,
                                       final Map<Long, Integer> dependentVersions)
            throws IOException, FindException
    {
        logger.log(Level.FINE, "Processing Policy #{0} ({1})", new Object[] { thisPolicy.getOid(), thisPolicy.getName() });
        seenOids.add(thisPolicy.getOid());
        dependentVersions.put(thisPolicy.getOid(), thisPolicy.getVersion());

        PolicyCacheImpl.PolicyDependencyInfo thisInfo = dependencyCache.get(thisPolicy.getOid());
        if (thisInfo == null) {
            thisInfo = new PolicyCacheImpl.PolicyDependencyInfo(thisPolicy);
            dependencyCache.put(thisPolicy.getOid(), thisInfo);
        } else {
            thisInfo.uses.clear(); // We're about to rebuild it in this and subsequent recursive invocations
        }

        final Assertion thisRootAssertion = thisPolicy.getAssertion();
        final Iterator assit = thisRootAssertion.preorderIterator();
        while (assit.hasNext()) {
            final Assertion ass = (Assertion) assit.next();
            if (!(ass instanceof Include)) continue;

            final Include include = (Include) ass;
            final Long includedOid = include.getPolicyOid();
            logger.log(Level.FINE, "Policy #{0} ({1}) includes Policy #{2} ({3})", new Object[] { thisPolicy.getOid(), thisPolicy.getName(), includedOid, include.getPolicyName() });
            if (includedOid == null) throw new RuntimeException("Found Include assertion with no PolicyOID in Policy #" + thisPolicy.getOid());
            if (seenOids.contains(includedOid)) throw new CircularPolicyException(thisPolicy, includedOid, include.getPolicyName());

            PolicyDependencyInfo includedInfo = dependencyCache.get(includedOid);
            final Policy includedPolicy;
            if (includedInfo == null) {
                logger.log(Level.FINE, "Creating new dependency info for #{0} ({1})", new Object[] { Long.toString(includedOid), include.getPolicyName() });
                includedPolicy = policyManager.findByPrimaryKey(includedOid);
                if (includedPolicy == null) {
                    logger.info("Include assertion in Policy #" + thisPolicy.getOid() + " refers to Policy #" + includedOid + ", which does not exist");
                    continue;
                }

                includedInfo = new PolicyDependencyInfo(includedPolicy);
                dependencyCache.put(includedOid, includedInfo);
            } else {
                includedPolicy = includedInfo.policy;
            }
            findDependentPolicies(includedPolicy, seenOids, dependentVersions);
            seenOids.remove(includedOid);
            thisInfo.uses.add(includedInfo.policy.getOid());
            includedInfo.usedBy.add(thisPolicy.getOid());
            thisInfo.dependentVersions = Collections.unmodifiableMap(dependentVersions);
        }
    }

    private static class PolicyDependencyInfo {
        private final Policy policy;
        private final Set<Long> uses = new HashSet<Long>();
        private final Set<Long> usedBy = new HashSet<Long>();

        private Map<Long, Integer> dependentVersions;

        private PolicyDependencyInfo(Policy policy) {
            this.policy = policy;
        }
    }

    /**
     * Caller must hold read lock at a minimum (write lock is sufficient)
     */
    private void collectVersions(PolicyDependencyInfo info, List<Pair<Long,Integer>> versions) {
        for (Long usedOid : info.uses) {
            PolicyDependencyInfo usedPdi = dependencyCache.get(usedOid);
            versions.add(new Pair<Long, Integer>(usedOid, usedPdi.policy.getVersion()));
            collectVersions(usedPdi, versions);
        }
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }
}
