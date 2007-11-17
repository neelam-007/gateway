/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.LicenseException;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.objectmodel.*;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author alex
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class PolicyManagerImpl extends HibernateEntityManager<Policy, EntityHeader> implements PolicyManager {
    private PolicyCache policyCache;

    @Transactional(propagation=Propagation.SUPPORTS)
    public void setPolicyCache(PolicyCache policyCache) {
        this.policyCache = policyCache;
    }

    @Override
    public long save(Policy entity) throws SaveException {
        try {
            long oid = super.save(entity);
            policyCache.update(entity);
            return oid;
        } catch (ServerPolicyException e) {
            throw new SaveException("Policy could not be saved; it contained an assertion failed to initialize", e);
        } catch (LicenseException e) {
            throw new SaveException("Policy could not be saved; it contained an assertion that is unlicensed on the gateway", e);
        } catch (IOException e) {
            throw new SaveException("Policy could not be saved; it could not be parsed", e);
        }
    }

    @Override
    public void update(Policy entity) throws UpdateException {
        try {
            policyCache.update(entity);
            super.update(entity);
        } catch (ServerPolicyException e) {
            throw new UpdateException("Policy could not be saved; it contained an assertion failed to initialize", e);
        } catch (LicenseException e) {
            throw new UpdateException("Policy could not be saved; it contained an assertion that is unlicensed on the gateway", e);
        } catch (IOException e) {
            throw new UpdateException("Policy could not be saved; it could not be parsed", e);
        }
    }

    @Override
    public void delete(long oid) throws DeleteException, FindException {
        // TODO check for users
        super.delete(oid);
        try {
            policyCache.remove(oid);
        } catch (PolicyDeletionForbiddenException e) {
            throw new DeleteException("Couldn't delete Policy: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void delete(Policy policy) throws DeleteException {
        // TODO check for users
        super.delete(policy);
        try {
            policyCache.remove(policy.getOid());
        } catch (PolicyDeletionForbiddenException e) {
            throw new DeleteException("Couldn't delete Policy: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Transactional(readOnly=true)
    public Collection<EntityHeader> findHeadersByType(final PolicyType type) throws FindException {
        //noinspection unchecked
        List<Policy> policies = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Policy.class);
                crit.add(Restrictions.eq("type", type));
                return crit.list();
            }
        });
        List<EntityHeader> hs = new ArrayList<EntityHeader>(policies.size());
        for (Policy policy : policies) {
            hs.add(new EntityHeader(policy.getId(), EntityType.POLICY, policy.getName(), null));
        }
        return hs;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getImpClass() {
        return Policy.class;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public Class<? extends Entity> getInterfaceClass() {
        return Policy.class;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public String getTableName() {
        return "policy";
    }
}
