/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.policy.PolicyDeletionForbiddenException;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.policy.CircularPolicyException;
import static com.l7tech.common.security.rbac.EntityType.POLICY;
import static com.l7tech.common.security.rbac.OperationType.*;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.common.security.rbac.Role;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.JaasUtils;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.transaction.annotation.Propagation;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @author alex
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
public class PolicyManagerImpl extends HibernateEntityManager<Policy, PolicyHeader> implements PolicyManager {
    private static final Logger logger = Logger.getLogger(PolicyManagerImpl.class.getName());
    
    String ROLE_NAME_TYPE_SUFFIX = "Policy";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    private static final Pattern replaceRoleName =
            Pattern.compile(MessageFormat.format(RbacAdmin.RENAME_REGEX_PATTERN, PolicyAdmin.ROLE_NAME_TYPE_SUFFIX));

    private PolicyCache policyCache;
    private final RoleManager roleManager;

    public PolicyManagerImpl(RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    @Transactional(propagation=Propagation.SUPPORTS)
    public void setPolicyCache(PolicyCache policyCache) {
        this.policyCache = policyCache;
    }

    @Override
    public long save(final Policy policy) throws SaveException {
        long oid;

        try {
            policyCache.validate(policy);
        } catch ( CircularPolicyException e ) {
            throw new SaveException("Couldn't save Policy: " + ExceptionUtils.getMessage(e), e);
        }

        oid = super.save(policy);

        return oid;
    }

    @Override
    public void update(final Policy policy) throws UpdateException {
        try {
            policyCache.validate(policy);
        } catch ( CircularPolicyException e ) {
            throw new UpdateException("Couldn't update Policy: " + ExceptionUtils.getMessage(e), e);
        }

        if ( policy.getType() != PolicyType.PRIVATE_SERVICE ) {
            try {
                roleManager.renameEntitySpecificRole(POLICY, policy, replaceRoleName);
            } catch (FindException e) {
                throw new UpdateException("Couldn't find Role to rename", e);
            }
        }

        super.update(policy);
    }

    @Override
    public void delete( long oid ) throws DeleteException, FindException {
        findAndDelete(oid);
    }

    @Override
    public void delete(Policy policy) throws DeleteException {
        try {
            if ( policy != null )
                policyCache.validateRemove( policy.getOid() );
        } catch (PolicyDeletionForbiddenException e) {
            throw new DeleteException("Couldn't delete Policy: " + ExceptionUtils.getMessage(e), e);
        }

        super.delete(policy);
    }

    @Transactional(readOnly=true)
    public Collection<PolicyHeader> findHeadersByType(final PolicyType type) throws FindException {
        //noinspection unchecked
        List<Policy> policies = getHibernateTemplate().executeFind(new ReadOnlyHibernateCallback() {
            @Override
            protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                Criteria crit = session.createCriteria(Policy.class);
                crit.add(Restrictions.eq("type", type));
                return crit.list();
            }
        });
        List<PolicyHeader> hs = new ArrayList<PolicyHeader>(policies.size());
        for (Policy policy : policies) {
            hs.add(newHeader(policy));
        }
        return hs;
    }

    public void addManagePolicyRole(Policy policy) throws SaveException {
        User currentUser = JaasUtils.getCurrentUser();

        // truncate policy name in the role name to avoid going beyond 128 limit
        String pname = policy.getName();
        // cutoff is arbitrarily set to 50
        pname = HexUtils.truncStringMiddle(pname, 50);
        String name = MessageFormat.format(PolicyAdmin.ROLE_NAME_PATTERN, pname, policy.getOid());

        logger.info("Creating new Role: " + name);

        Role newRole = new Role();
        newRole.setName(name);
        newRole.setEntityType(POLICY);
        newRole.setEntityOid(policy.getOid());

        // RUD this policy
        newRole.addPermission(READ, POLICY, policy.getId()); // Read this policy
        newRole.addPermission(UPDATE, POLICY, policy.getId()); // Update this policy
        newRole.addPermission(DELETE, POLICY, policy.getId()); // Delete this policy

        if (currentUser != null) {
            // See if we should give the current user admin permission for this policy
            boolean omnipotent;
            try {
                omnipotent = roleManager.isPermittedForAnyEntityOfType(currentUser, READ, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, UPDATE, POLICY);
                omnipotent &= roleManager.isPermittedForAnyEntityOfType(currentUser, DELETE, POLICY);
            } catch (FindException e) {
                throw new SaveException("Coudln't get existing permissions", e);
            }

            if (!omnipotent) {
                logger.info("Assigning current User to new Role");
                newRole.addAssignedUser(currentUser);
            }
        }
        roleManager.save(newRole);
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

    @Override
    protected PolicyHeader newHeader( final Policy entity ) {
        return new PolicyHeader( entity );
    }

}

