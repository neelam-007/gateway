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
import com.l7tech.common.util.Pair;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author alex
 */
public class PolicyAdminImpl implements PolicyAdmin {
    private final PolicyManager policyManager;
    private final PolicyCache policyCache;
    private final PolicyVersionManager policyVersionManager;
    private final RoleManager roleManager;

    private static final Set<PropertyDescriptor> OMIT_VERSION_AND_XML = BeanUtils.omitProperties(BeanUtils.getProperties(Policy.class), "version", "xml");
    private static final Set<PropertyDescriptor> OMIT_XML = BeanUtils.omitProperties(BeanUtils.getProperties(Policy.class), "xml");

    public PolicyAdminImpl(PolicyManager policyManager, PolicyCache policyCache, PolicyVersionManager policyVersionManager, RoleManager roleManager) {
        this.policyManager = policyManager;
        this.policyCache = policyCache;
        this.policyVersionManager = policyVersionManager;
        this.roleManager = roleManager;
    }

    public Policy findPolicyByPrimaryKey(long oid) throws FindException {
        Policy policy = policyManager.findByPrimaryKey(oid);
        if (policy == null) return null;
        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(oid);
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }
        return policy;
    }

    public Collection<EntityHeader> findPolicyHeadersByType(PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    public void deletePolicy(long oid) throws PolicyDeletionForbiddenException, DeleteException, FindException {
        policyManager.delete(oid);
        roleManager.deleteEntitySpecificRole(com.l7tech.common.security.rbac.EntityType.POLICY, oid);
    }

    public long savePolicy(Policy policy) throws SaveException {
        return savePolicy(policy, true).left;
    }

    public Pair<Long,Long> savePolicy(Policy policy, boolean activateAsWell) throws SaveException {
        try {
            if (!activateAsWell)
                return saveWithoutActivating(policy);

            if (policy.getOid() == Policy.DEFAULT_OID) {
                final long oid = policyManager.save(policy);
                long versionOrdinal = checkpointPolicy(policy, activateAsWell, true).getOrdinal();
                policyManager.addManagePolicyRole(policy);
                return new Pair<Long,Long>(oid, versionOrdinal);
            } else {
                policyManager.update(policy);
                long versionOrdinal = checkpointPolicy(policy, true, false).getOrdinal();
                return new Pair<Long,Long>(policy.getOid(), versionOrdinal);
            }
        } catch (SaveException e) {
            throw e;
        } catch (ObjectModelException e) {
            throw new SaveException("Couldn't update policy", e.getCause());
        }
    }

    private Pair<Long,Long> saveWithoutActivating(Policy policy) throws ObjectModelException {
        long policyOid = policy.getOid();
        if (policyOid == Policy.DEFAULT_OID) {
            // Save new policy without activating it
            String revisionXml = policy.getXml();
            policy.disable();
            long oid = policyManager.save(policy);
            policyManager.addManagePolicyRole(policy);
            Policy toCheckpoint = makeCopyWithDifferentXml(policy, revisionXml);
            toCheckpoint.setVersion(toCheckpoint.getVersion() - 1);
            long versionOrdinal = checkpointPolicy(toCheckpoint, false, true).getOrdinal();
            return new Pair<Long,Long>(oid, versionOrdinal);
        }

        try {
            // Save updated policy without activating it or changing the enable/disable state of the currently-in-effect policy
            String revisionXml = policy.getXml();
            Policy curPolicy = policyManager.findByPrimaryKey(policyOid);
            if (curPolicy == null)
                throw new SaveException("No existing policy found with OID=" + policyOid);
            String curXml = curPolicy.getXml();
            BeanUtils.copyProperties(policy, curPolicy, OMIT_VERSION_AND_XML);
            curPolicy.setXml(curXml + ' '); // leave policy semantics unchanged but bump the version number
            policyManager.update(curPolicy);
            long versionOrdinal = checkpointPolicy(makeCopyWithDifferentXml(curPolicy, revisionXml), false, false).getOrdinal();
            return new Pair<Long,Long>(policyOid, versionOrdinal);
        } catch (InvocationTargetException e) {
            throw new SaveException(e);
        } catch (IllegalAccessException e) {
            throw new SaveException(e);
        }
    }

    private static Policy makeCopyWithDifferentXml(Policy policy, String revisionXml) throws SaveException {
        try {
            Policy toCheckpoint = new Policy(policy.getType(), policy.getName(), revisionXml, policy.isSoap());
            BeanUtils.copyProperties(policy, toCheckpoint, OMIT_XML);
            return toCheckpoint;
        } catch (InvocationTargetException e) {
            throw new SaveException("Unable to copy Policy properties", e); // can't happen
        } catch (IllegalAccessException e) {
            throw new SaveException("Unable to copy Policy properties", e); // can't happen
        }
    }

    private PolicyVersion checkpointPolicy(Policy toCheckpoint, boolean activate, boolean newEntity) throws ObjectModelException {
        return policyVersionManager.checkpointPolicy(toCheckpoint, activate, newEntity);
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

    public PolicyVersion findActivePolicyVersionForPolicy(long policyOid) throws FindException {
        return policyVersionManager.findActiveVersionForPolicy(policyOid);
    }

    public void clearActivePolicyVersion(long policyOid) throws FindException, UpdateException {
        Policy policy = policyManager.findByPrimaryKey(policyOid);
        if (policy == null) throw new FindException("No Policy found with policyOid=" + policyOid);

        policy.disable();
        policyManager.update(policy);
        policyVersionManager.deactivateVersions(policyOid, PolicyVersion.DEFAULT_OID);
    }
}
