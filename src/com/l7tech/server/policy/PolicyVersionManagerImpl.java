/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyVersion;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.PolicyCheckpointEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.event.system.ServiceCacheEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway's production implementation of {@link PolicyVersionManager}.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class PolicyVersionManagerImpl extends HibernateEntityManager<PolicyVersion, EntityHeader> implements PolicyVersionManager, ApplicationContextAware {
    private static final Logger logger = Logger.getLogger(PolicyVersionManagerImpl.class.getName());   

    private ApplicationContext applicationContext;

    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getImpClass() {
        return PolicyVersion.class;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getInterfaceClass() {
        return PolicyVersion.class;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public String getTableName() {
        return "policy_verison";
    }

    @Transactional(readOnly=true)
    public PolicyVersion findByPrimaryKey(long policyOid, long policyVersionOid) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("oid", policyVersionOid);
        map.put("policyOid", policyOid);
        List<PolicyVersion> found = findMatching(map);
        if (found == null || found.isEmpty())
            return null;
        if (found.size() > 1)
            throw new FindException("Found more than one PolicyVersion with oid=" + policyVersionOid + " and policy_oid=" + policyOid); // can't happen
        return found.iterator().next();
    }

    @Transactional(readOnly=true)
    public List<PolicyVersion> findAllForPolicy(long policyOid) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("policyOid", policyOid);
        return findMatching(map);
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public void checkpointPolicy(Policy newPolicy) throws FindException, SaveException {
        long policyOid = newPolicy.getOid();
        if (policyOid == Policy.DEFAULT_OID) {
            // A new one.  Take no action; its revisions will be created when it is changed.
            return;
        }

        AdminInfo adminInfo = AdminInfo.find();

        List<PolicyVersion> got = findAllForPolicy(policyOid);
        if (got.isEmpty()) {
            // Preserve initial revision
            Policy oldPolicy = ((PolicyManager)applicationContext.getBean("policyManager")).findByPrimaryKey(policyOid);
            PolicyVersion oldVer = snapshot(oldPolicy, adminInfo);  // TODO bug: this saves the wrong admin info for the first revision
            save(oldVer);
        }

        PolicyVersion ver = snapshot(newPolicy, adminInfo);
        save(ver);
    }

    private static PolicyVersion snapshot(Policy policy, AdminInfo adminInfo) {
        long policyOid = policy.getOid();
        PolicyVersion ver = new PolicyVersion();
        ver.setPolicyOid(policyOid);
        ver.setActive(true);
        ver.setOrdinal(policy.getVersion());
        ver.setTime(System.currentTimeMillis());
        ver.setUserLogin(adminInfo.login);
        ver.setUserProviderOid(adminInfo.identityProviderOid);
        ver.setXml(policy.getXml());
        return ver;
    }

    /**
     * Find the Spring-wrapped instance of ourself, if possible.
     *
     * @return a Spring-wrapped instance of this class, if possible.  If one can't be found, just returns this.
     */
    private PolicyVersionManager getPolicyVersionManager() {
        if (applicationContext != null) {
            PolicyVersionManager pvm = (PolicyVersionManager)applicationContext.getBean("policyVersionManager");
            if (pvm != null)
                return pvm;
        }
        return this;
    }

    private ApplicationListener listener = new ApplicationListener() {
        public void onApplicationEvent(ApplicationEvent event) {
            //noinspection ChainOfInstanceofChecks
            if (event instanceof PolicyCheckpointEvent) {
                // Redispatch to ourself through Spring if possible, for transactional correctness, even
                // though this should never be necessary in practice (since if you are mutating a policy
                // you should already be in a transaction)
                Policy policy = ((PolicyCheckpointEvent)event).getPolicyBeingSaved();
                try {
                    getPolicyVersionManager().checkpointPolicy(policy);
                } catch (ObjectModelException e) {
                    logger.log(Level.WARNING, "Unable to checkpoint policy oid " + policy.getOid() + ": " + ExceptionUtils.getMessage(e), e);
                    throw new RuntimeException(e); // Must rethrow to ensure transaction rolled back
                }
            } else if (event instanceof ServiceCacheEvent.Updated) {
                ServiceCacheEvent.Updated updated = (ServiceCacheEvent.Updated)event;
                // TODO do we need to do anything at this point?
            } else if (event instanceof Created) {
                Created created = (Created)event;
                if (created.getEntity() != null && Policy.class.isAssignableFrom(created.getEntity().getClass())) {
                    Policy policy = (Policy)created.getEntity();
                    try {
                        getPolicyVersionManager().checkpointPolicy(policy);
                    } catch (ObjectModelException e) {
                        logger.log(Level.WARNING, "Unable to checkpoint policy oid " + policy.getOid() + ": " + ExceptionUtils.getMessage(e), e);
                        throw new RuntimeException(e); // Must rethrow to ensure transaction rolled back
                    }
                }
            }
        }
    };

    public void setApplicationEventProxy(ApplicationEventProxy proxy) {
        proxy.addApplicationListener(listener);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}