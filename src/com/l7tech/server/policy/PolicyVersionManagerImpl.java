/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyVersion;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.PolicyCheckpointEvent;
import com.l7tech.server.event.admin.Created;
import com.l7tech.server.util.ApplicationEventProxy;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
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
    /** @noinspection FieldNameHidesFieldInSuperclass*/
    private static final Logger logger = Logger.getLogger(PolicyVersionManagerImpl.class.getName());

    /** @noinspection FieldCanBeLocal */ // IntelliJ bug
    private final ServerConfig serverConfig;
    private ApplicationContext applicationContext;

    public PolicyVersionManagerImpl(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

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
        final long policyOid = newPolicy.getOid();
        if (policyOid == Policy.DEFAULT_OID) {
            // A new one.  Take no action; its revision will be created when it is actually created.
            return;
        }

        AdminInfo adminInfo = AdminInfo.find();
        PolicyVersion ver = snapshot(newPolicy, adminInfo);
        final long versionOid = save(ver);

        // Delete any uncommented old versions TODO enable this and debug it
        //noinspection ConstantIfStatement
        if (true) {
        int numToKeep = serverConfig.getIntProperty(ServerConfig.PARAM_POLICY_VERSIONING_MAX_REVISIONS, 20);
        final long lowestOrdinal = ver.getOrdinal() - numToKeep;
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.createQuery("delete PolicyVersion where policyOid = :policyOid and oid != :versionOid and name is null and ordinal < :lowestOrdinal")
                        .setLong("policyOid", policyOid)
                        .setLong("versionOid", versionOid)
                        .setLong("lowestOrdinal", lowestOrdinal)
                        .executeUpdate();
                return null;
            }
        });

        // Deactivate all previous versions
        getHibernateTemplate().execute(new HibernateCallback() {
            public Object doInHibernate(Session session) throws HibernateException, SQLException {
                session.createQuery("update versioned PolicyVersion set active = :active where policyOid = :policyOid and oid != :versionOid")
                        .setBoolean("active", false)
                        .setLong("policyOid", policyOid)
                        .setLong("versionOid", versionOid)
                        .executeUpdate();
                return null;
            }
        });
        }
    }

    private static PolicyVersion snapshot(Policy policy, AdminInfo adminInfo) {
        long policyOid = policy.getOid();
        PolicyVersion ver = new PolicyVersion();
        ver.setPolicyOid(policyOid);
        ver.setActive(true); // TODO policy.isEnabled()
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
            } else if (event instanceof Created) {
                Created created = (Created)event;
                if (created.getEntity() != null && Policy.class.isAssignableFrom(created.getEntity().getClass())) {
                    onPolicyCreated((Policy)created.getEntity());
                }
            }
        }
    };

    private void onPolicyCreated(Policy policy) {
        try {
            getPolicyVersionManager().checkpointPolicy(policy);
        } catch (ObjectModelException e) {
            logger.log(Level.WARNING, "Unable to checkpoint policy oid " + policy.getOid() + " after Created event: " + ExceptionUtils.getMessage(e), e);
            throw new RuntimeException(e); // Must rethrow to ensure transaction rolled back
        }
    }

    public void setApplicationEventProxy(ApplicationEventProxy proxy) {
        proxy.addApplicationListener(listener);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}