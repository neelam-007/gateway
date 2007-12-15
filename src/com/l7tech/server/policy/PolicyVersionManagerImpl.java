/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyVersion;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions;
import com.l7tech.objectmodel.*;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.AdminInfo;
import com.l7tech.server.event.PolicyCheckpointEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gateway's production implementation of {@link PolicyVersionManager}.
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class PolicyVersionManagerImpl extends HibernateEntityManager<PolicyVersion, EntityHeader> implements PolicyVersionManager {
    /** @noinspection FieldNameHidesFieldInSuperclass*/
    private static final Logger logger = Logger.getLogger(PolicyVersionManagerImpl.class.getName());

    /** @noinspection FieldCanBeLocal */ // IntelliJ bug
    private final ServerConfig serverConfig;

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

    /**
     * Examine the specified policy and record a new PolicyVersion if necessary.
     *
     * @param newPolicy a possibly-mutated policy that has not yet been committed to the database.
     * @param activated if true, the newly saved revision should be marked as the active revision for this policy.
     * @param newEntity if true, this is a new Policy entity being created
     * @throws ObjectModelException if there is a problem finding or updating information from the database
     */
    @Transactional(propagation=Propagation.SUPPORTS)
    private void checkpointPolicy(Policy newPolicy, boolean activated, boolean newEntity) throws ObjectModelException {
        final long policyOid = newPolicy.getOid();
        if (policyOid == Policy.DEFAULT_OID)
            throw new IllegalArgumentException("Unable to checkpoint policy without a valid OID");

        AdminInfo adminInfo = AdminInfo.find();
        PolicyVersion ver = snapshot(newPolicy, adminInfo, activated, newEntity);
        final long versionOid = save(ver);
        ver.setOid(versionOid);

        if (activated) {
            // Deactivate all previous versions
            deactivateVersions(policyOid, versionOid);
        }

        deleteStaleRevisions(policyOid, ver);
    }

    private void deleteStaleRevisions(final long policyOid, final PolicyVersion justSaved) throws FindException, DeleteException {
        final long justSavedOid = justSaved.getOid();

        // Delete oldest anonymous revisions if we have exceeded MAX_REVISIONS
        // Revisions that have been assigned a name won't be deleted
        int numToKeep = serverConfig.getIntProperty(ServerConfig.PARAM_POLICY_VERSIONING_MAX_REVISIONS, 20);
        List<PolicyVersion> revisions = new ArrayList<PolicyVersion>(findAllForPolicy(policyOid));

        // Don't count revisions against the limit if they have been assigned names
        revisions = Functions.grep(revisions, new Functions.Unary<Boolean,PolicyVersion>() {
            public Boolean call(PolicyVersion policyVersion) {
                boolean inactive = !policyVersion.isActive();
                boolean notTheOneWeJustSaved = policyVersion.getOid() != justSavedOid;
                boolean belongsToOurPolicy = policyVersion.getPolicyOid() == policyOid;
                boolean nameIsEmpty = isNameEmpty(policyVersion);

                // Candidates for deletion are inactive, anonymous, and not the one we just saved
                return inactive && belongsToOurPolicy && notTheOneWeJustSaved && nameIsEmpty;
            }
        });
        Collections.sort(revisions, new Comparator<PolicyVersion>() {
            public int compare(PolicyVersion o1, PolicyVersion o2) {
                return new Long(o1.getOrdinal()).compareTo(o2.getOrdinal());
            }
        });
        int num = revisions.size();

        if (!justSaved.isActive() && isNameEmpty(justSaved))
            num++; // We won't try to delete the one we just saved, but if it would otherwise be a deletion candidate, it still uses up a slot

        if (num > numToKeep) {
            for (PolicyVersion revision : revisions) {
                if (num > numToKeep && revision.getPolicyOid() == policyOid && revision.getOid() != justSavedOid) {
                    delete(revision);
                    num--;
                    if (num <= numToKeep)
                        break;
                }
            }
        }
    }

    private static boolean isNameEmpty(PolicyVersion policyVersion) {
        return policyVersion.getName() == null || policyVersion.getName().length() < 1;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public void deactivateVersions(final long policyOid, final long versionOid) throws UpdateException {
        try {
            getHibernateTemplate().execute(new HibernateCallback() {
                public Object doInHibernate(Session session) throws HibernateException, SQLException {
                    FlushMode oldFlushMode = session.getFlushMode();
                    try {
                        session.setFlushMode(FlushMode.COMMIT);
                        session.createQuery("update versioned PolicyVersion set active = :active where policyOid = :policyOid and oid != :versionOid")
                                .setBoolean("active", false)
                                .setLong("policyOid", policyOid)
                                .setLong("versionOid", versionOid)
                                .executeUpdate();
                        return null;
                    } finally {
                        session.setFlushMode(oldFlushMode);
                    }
                }
            });
        } catch (DataAccessException e) {
            throw new UpdateException(e);
        }
    }

    private static PolicyVersion snapshot(Policy policy, AdminInfo adminInfo, boolean activated, boolean newEntity) {
        long policyOid = policy.getOid();
        PolicyVersion ver = new PolicyVersion();
        ver.setPolicyOid(policyOid);
        ver.setActive(activated);

        // The entity version numbering for Policy starts at zero, but due to quirks in how save() vs update()
        // behave, both the initial saved Policy and the first update() of an existing policy reach this code
        // with a version number of zero.  So, we increment once to switch to one-based numbering, the increment
        // once more if this isn't the initial save, so we get smooth "1,2,3,4,5" numbering of the ordinals.
        int ordinal = policy.getVersion() + 1;
        if (!newEntity) ordinal++;

        ver.setOrdinal(ordinal);
        ver.setTime(System.currentTimeMillis());
        ver.setUserLogin(adminInfo.login);
        ver.setUserProviderOid(adminInfo.identityProviderOid);
        ver.setXml(policy.getXml());
        return ver;
    }

    // We don't just implement ApplicationListener in the outer class because this causes all
    // calls to onApplicationEvent to go through the transaction interceptor, and onApplicationEvent will be 
    // called several times per message being processed.
    private ApplicationListener listener = new ApplicationListener() {
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof PolicyCheckpointEvent) {
                PolicyCheckpointEvent pce = (PolicyCheckpointEvent)event;
                Policy policy = pce.getPolicyBeingSaved();
                try {
                    checkpointPolicy(policy, pce.isActivated(), pce.isNewEntity());
                } catch (ObjectModelException e) {
                    logger.log(Level.WARNING, "Unable to checkpoint policy oid " + policy.getOid() + ": " + ExceptionUtils.getMessage(e), e);
                    throw new RuntimeException(e); // Must rethrow to ensure transaction rolled back
                }
            }
        }
    };

    public void setApplicationEventProxy(ApplicationEventProxy proxy) {
        proxy.addApplicationListener(listener);
    }
}