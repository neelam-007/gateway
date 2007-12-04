/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.*;
import com.l7tech.common.security.rbac.MethodStereotype;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.util.BeanUtils;
import com.l7tech.common.util.Functions.Unary;
import static com.l7tech.common.util.Functions.map;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.FalseAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.event.PolicyCheckpointEvent;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author alex
 */
public class PolicyAdminImpl implements PolicyAdmin, ApplicationContextAware {
    private final PolicyManager policyManager;
    private final PolicyCache policyCache;
    private final PolicyVersionManager policyVersionManager;
    private final RoleManager roleManager;

    private ApplicationContext applicationContext;

    public PolicyAdminImpl(PolicyManager policyManager, PolicyCache policyCache, PolicyVersionManager policyVersionManager, RoleManager roleManager) {
        this.policyManager = policyManager;
        this.policyCache = policyCache;
        this.policyVersionManager = policyVersionManager;
        this.roleManager = roleManager;
    }

    public Policy findPolicyByPrimaryKey(long oid) throws FindException {
        return policyManager.findByPrimaryKey(oid);
    }

    public Collection<EntityHeader> findPolicyHeadersByType(PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    public void deletePolicy(long oid) throws PolicyDeletionForbiddenException, DeleteException, FindException {
        policyManager.delete(oid);
        roleManager.deleteEntitySpecificRole(com.l7tech.common.security.rbac.EntityType.POLICY, oid);
    }

    public long savePolicy(Policy policy) throws SaveException {
        return savePolicy(policy, true);
    }

    public long savePolicy(Policy policy, boolean activateAsWell) throws SaveException {
        if (!activateAsWell)
            throw new SaveException("TODO Saving a Policy without activating it is not yet implemented"); // TODO implement this

        if (policy.getOid() == Policy.DEFAULT_OID) {
            final long oid = policyManager.save(policy);
            // Checkpoint after save since it requires a valid OID
            applicationContext.publishEvent(new PolicyCheckpointEvent(this, policy));
            policyManager.addManagePolicyRole(policy);
            return oid;
        } else {
            try {
                // Checkpoint before update so it can preserve old policy XML
                applicationContext.publishEvent(new PolicyCheckpointEvent(this, policy));
                policyManager.update(policy);
                return policy.getOid();
            } catch (UpdateException e) {
                throw new SaveException("Couldn't update policy", e.getCause());
            }
        }
    }

    @Secured(stereotype=MethodStereotype.FIND_ENTITIES)
    public Set<Policy> findUsages(long oid) throws FindException {
        return policyCache.findUsages(oid);
    }

    public PolicyVersion findPolicyVersionByPrimaryKey(long policyOid, long versionOid) throws FindException {
        return policyVersionManager.findByPrimaryKey(policyOid, versionOid);
    }

    public List<PolicyVersion> findPolicyVersionHeadersByPolicy(long policyOid) throws FindException {
        final Set<PropertyDescriptor> allButXml = BeanUtils.omitProperties(BeanUtils.getProperties(PolicyVersion.class), "xml");

        return map(policyVersionManager.findAllForPolicy(policyOid), new Unary<PolicyVersion, PolicyVersion>() {
            public PolicyVersion call(PolicyVersion version) {
                PolicyVersion ret = new PolicyVersion();
                try {
                    BeanUtils.copyProperties(version, ret, allButXml);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return ret;
            }
        });
    }

    public void setPolicyVersionComment(long policyOid, long versionOid, String comment) throws FindException, UpdateException {
        PolicyVersion ver = policyVersionManager.findByPrimaryKey(policyOid, versionOid);
        if (ver == null) throw new FindException("No PolicyVersion found with policyOid=" + policyOid + " and oid=" + versionOid);
        ver.setName(comment);
        policyVersionManager.update(ver);
    }

    public void setActivePolicyVersion(long policyOid, long versionOid) throws FindException, UpdateException {
        PolicyVersion ver = policyVersionManager.findByPrimaryKey(policyOid, versionOid);
        if (ver == null) throw new FindException("No PolicyVersion found with policyOid=" + policyOid + " and oid=" + versionOid);

        Policy policy = policyManager.findByPrimaryKey(policyOid);
        if (policy == null) throw new FindException("No Policy found with policyOid=" + policyOid); // shouldn't be possible

        policy.setXml(ver.getXml());
        ver.setActive(true);
        policyManager.update(policy);
        policyVersionManager.update(ver);
        policyVersionManager.deactivateVersions(policyOid, versionOid);
    }

    public void clearActivePolicyVersion(long policyOid) throws FindException, UpdateException {
        Policy policy = policyManager.findByPrimaryKey(policyOid);
        if (policy == null) throw new FindException("No Policy found with policyOid=" + policyOid);

        policy.setXml(WspWriter.getPolicyXml(new FalseAssertion())); // TODO better way to deactivate policies
        policyManager.update(policy);
        policyVersionManager.deactivateVersions(policyOid, PolicyVersion.DEFAULT_OID);
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
