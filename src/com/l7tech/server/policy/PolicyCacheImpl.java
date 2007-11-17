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
    private static final Logger logger = Logger.getLogger(PolicyCacheImpl.class.getName());

    private PolicyManager policyManager;
    private final ServerPolicyFactory policyFactory;

    private final Map<Long, ServerPolicyInfo> serverPolicyCache = new HashMap<Long, ServerPolicyInfo>();
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
            ServerPolicyInfo spi = serverPolicyCache.get(oid);
            if (spi != null) return spi.serverPolicy;

            read.unlock(); read = null;

            // Must not hold either lock here, we're reentrant if an Include assertion is in the policy
            final ServerAssertion assertion = policyFactory.compilePolicy(policy.getAssertion(), true);
            spi = new ServerPolicyInfo(policy, assertion);

            try {
                write.lock();
                serverPolicyCache.put(oid, spi);
                return assertion;
            } finally {
                write.unlock();
            }
        } finally {
            if (read != null) read.unlock();
        }
    }

    public ServerAssertion getServerPolicy(long policyOid)
            throws FindException, IOException, ServerPolicyException, LicenseException
    {
        ServerPolicyInfo spi;
        ServerAssertion assertion;

        Lock read = lock.readLock();
        try {
            read.lock();
            spi = serverPolicyCache.get(policyOid);
            if (spi != null) return spi.serverPolicy;
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
                assertion = policyFactory.compilePolicy(policy.getAssertion(), true);

                write.lock();
                serverPolicyCache.put(policyOid, new ServerPolicyInfo(policy, assertion));
                return assertion;
            }
        } finally {
            write.unlock();
        }
    }

    public void update(Policy policy) throws ServerPolicyException, LicenseException, IOException {
        if (policy.getOid() == Policy.DEFAULT_OID) throw new IllegalArgumentException("Can't update a brand-new policy--it must be saved first");
        logger.log(Level.INFO, "Policy #{0} ({1}) has been created or updated; updating caches", new Object[] { policy.getOid(), policy.getName() });
        getServerPolicy(policy); // Refresh the cache, don't care about return value

        // Prevent reentrant calls from ServerInclude from deadlocking
        ServerAssertion sass = policyFactory.compilePolicy(policy.getAssertion(), true);

        this.lock.writeLock().lock();
        try {
            findDependentPolicies(policy, new HashSet<Long>());
            updateUsedBy();
            serverPolicyCache.remove(policy.getOid());
            serverPolicyCache.put(policy.getOid(), new ServerPolicyInfo(policy, sass));
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
            return Collections.unmodifiableSet(pdi.usedBy);
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
                    throw new PolicyDeletionForbiddenException(deletedPolicy, EntityType.POLICY, pdi.usedBy.iterator().next());
                }

                for (Policy usee : pdi.uses) {
                    PolicyDependencyInfo useePdi = dependencyCache.get(usee.getOid());
                    if (useePdi == null) continue;
                    useePdi.usedBy.remove(deletedPolicy);
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
                findDependentPolicies(policy, new HashSet<Long>());
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
            for (Policy usedPolicy : pdi.uses) {
                PolicyCacheImpl.PolicyDependencyInfo usedPdi = dependencyCache.get(usedPolicy.getOid());
                usedPdi.usedBy.add(pdi.policy);
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
     *        (always pass <code>new {@link HashSet}&lt;{@link Long}&gt;())
     * @throws IOException if this policy, or one of its descendants, could not be parsed
     * @throws FindException if one of this policy's descendants could not be loaded
     */
    private void findDependentPolicies(final Policy thisPolicy,
                                       final Set<Long> seenOids)
            throws IOException, FindException
    {
        logger.log(Level.FINE, "Processing Policy #{0} ({1})", new Object[] { thisPolicy.getOid(), thisPolicy.getName() });
        seenOids.add(thisPolicy.getOid());

        PolicyCacheImpl.PolicyDependencyInfo thisInfo = dependencyCache.get(thisPolicy.getOid());
        if (thisInfo == null) {
            thisInfo = new PolicyCacheImpl.PolicyDependencyInfo(thisPolicy);
            dependencyCache.put(thisPolicy.getOid(), thisInfo);
        }

        final Assertion thisRootAssertion = thisPolicy.getAssertion();
        final Iterator assit = thisRootAssertion.preorderIterator();
        while (assit.hasNext()) {
            Assertion ass = (Assertion) assit.next();
            if (ass instanceof Include) {
                Include include = (Include) ass;
                Long includedOid = include.getPolicyOid();
                logger.log(Level.FINE, "Policy #{0} ({1}) includes Policy #{2} ({3})", new Object[] { thisPolicy.getOid(), thisPolicy.getName(), includedOid, include.getPolicyName() });
                if (includedOid == null) throw new RuntimeException("Found Include assertion with no PolicyOID in Policy #" + thisPolicy.getOid());
                if (seenOids.contains(includedOid)) throw new CircularPolicyException(thisPolicy, includedOid, include.getPolicyName());

                PolicyCacheImpl.PolicyDependencyInfo includedInfo = dependencyCache.get(includedOid);
                Policy includedPolicy;
                if (includedInfo == null) {
                    logger.log(Level.FINE, "Creating new dependency info for #{0} ({1})", new Object[] { Long.toString(includedOid), include.getPolicyName() });
                    includedPolicy = policyManager.findByPrimaryKey(includedOid);
                    if (includedPolicy == null) {
                        logger.info("Include assertion in Policy #" + thisPolicy.getOid() + " refers to Policy #" + includedOid + ", which does not exist");
                        continue;
                    }

                    includedInfo = new PolicyCacheImpl.PolicyDependencyInfo(includedPolicy);
                    dependencyCache.put(includedOid, includedInfo);
                } else {
                    includedPolicy = includedInfo.policy;
                }
                findDependentPolicies(includedPolicy, seenOids);
                seenOids.remove(includedOid);
                thisInfo.uses.add(includedInfo.policy);
                includedInfo.usedBy.add(thisPolicy);
            }
        }
    }

    private static class PolicyDependencyInfo {
        private final Policy policy;
        private final Set<Policy> uses = new HashSet<Policy>();
        private final Set<Policy> usedBy = new HashSet<Policy>();

        private PolicyDependencyInfo(Policy policy) {
            this.policy = policy;
        }
    }

    private static class ServerPolicyInfo {
        private final Policy policy;
        private final ServerAssertion serverPolicy;

        public ServerPolicyInfo(Policy policy, ServerAssertion serverPolicy) {
            this.policy = policy;
            this.serverPolicy = serverPolicy;
        }
    }

    public void setPolicyManager(PolicyManager policyManager) {
        this.policyManager = policyManager;
    }
}
