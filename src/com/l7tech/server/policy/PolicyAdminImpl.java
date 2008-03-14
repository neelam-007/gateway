/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.*;
import com.l7tech.common.security.rbac.MethodStereotype;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.util.BeanUtils;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.Functions.Unary;
import static com.l7tech.common.util.Functions.map;
import com.l7tech.objectmodel.*;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class PolicyAdminImpl implements PolicyAdmin, ApplicationContextAware {
    protected static final Logger logger = Logger.getLogger(PolicyAdminImpl.class.getName());

    private final PolicyManager policyManager;
    private final PolicyCache policyCache;
    private final PolicyVersionManager policyVersionManager;
    private final RoleManager roleManager;
    private Auditor auditor;

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

    public Policy findPolicyByUniqueName(String name) throws FindException {
        Policy policy = policyManager.findByUniqueName(name);
        if (policy == null) return null;
        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(policy.getOid());
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }
        return policy;
    }

    public Collection<PolicyHeader> findPolicyHeadersByType(PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    public void deletePolicy(long oid) throws PolicyDeletionForbiddenException, DeleteException, FindException {
        policyManager.delete(oid);
        roleManager.deleteEntitySpecificRole(com.l7tech.common.security.rbac.EntityType.POLICY, oid);
    }

    public long savePolicy(Policy policy) throws SaveException {
        return savePolicy(policy, true).getPolicyOid();
    }

    public PolicyCheckpointState savePolicy(Policy policy, boolean activateAsWell) throws SaveException {
        try {
            if (!activateAsWell)
                return saveWithoutActivating(policy);

            if (policy.getOid() == Policy.DEFAULT_OID) {
                final long oid = policyManager.save(policy);
                final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(policy, activateAsWell, true);
                policyManager.addManagePolicyRole(policy);
                return new PolicyCheckpointState(oid, checkpoint.getOrdinal(), checkpoint.isActive());
            } else {
                policyManager.update(policy);
                final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(policy, true, false);
                long versionOrdinal = checkpoint.getOrdinal();
                return new PolicyCheckpointState(policy.getOid(), versionOrdinal, checkpoint.isActive());
            }
        } catch (SaveException e) {
            throw e;
        } catch (UpdateException ue) {
            DuplicateObjectException doe = ExceptionUtils.getCauseIfCausedBy( ue, DuplicateObjectException.class );
            if ( doe != null ) {
                throw doe;
            } else {
                throw new SaveException("Couldn't update policy", ue);                
            }
        } catch (ObjectModelException e) {
            throw new SaveException("Couldn't update policy", e);
        }
    }

    public SavePolicyWithFragmentsResult savePolicy(Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws SaveException {
        try {
            HashMap<String, Long> fragmentNameOidMap = new HashMap<String, Long>();
            for(Policy fragmentPolicy : fragments.values()) {
                boolean create = false;

                try {
                    Policy p = policyManager.findByUniqueName(fragmentPolicy.getName());
                    if(p == null) {
                        create = true;
                    } else if(p.getOid() != fragmentPolicy.getOid()) {
                        fragmentNameOidMap.put(fragmentPolicy.getName(), p.getOid());
                    }
                } catch(FindException e) {
                    create = true;
                }

                if(create) {
                    fragmentPolicy.setOid(Policy.DEFAULT_OID);
                    long oid = policyManager.save(fragmentPolicy);
                    policyVersionManager.checkpointPolicy(fragmentPolicy, activateAsWell, true);
                    policyManager.addManagePolicyRole(fragmentPolicy);
                    fragmentNameOidMap.put(fragmentPolicy.getName(), oid);
                }
            }

            if(fragmentNameOidMap.size() > 0) {
                Assertion rootAssertion = policy.getAssertion();
                correctIncludeAssertions(rootAssertion, fragmentNameOidMap);
                policy.setXml(WspWriter.getPolicyXml(rootAssertion));
            }

            return new SavePolicyWithFragmentsResult(savePolicy(policy, activateAsWell), fragmentNameOidMap);
        } catch(SaveException e) {
            throw e;
        } catch(Exception e) {
            throw new SaveException("Couldn't update policy", e);
        }
    }

    private void correctIncludeAssertions(Assertion rootAssertion, HashMap<String, Long> fragmentNameOidMap) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                correctIncludeAssertions(child, fragmentNameOidMap);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(fragmentNameOidMap.containsKey(includeAssertion.getPolicyName())) {
                includeAssertion.setPolicyOid(fragmentNameOidMap.get(includeAssertion.getPolicyName()));
            }
        }
    }

    private PolicyCheckpointState saveWithoutActivating(Policy policy) throws ObjectModelException {
        long policyOid = policy.getOid();
        if (policyOid == Policy.DEFAULT_OID) {
            // Save new policy without activating it
            String revisionXml = policy.getXml();
            policy.disable();
            long oid = policyManager.save(policy);
            policyManager.addManagePolicyRole(policy);
            Policy toCheckpoint = makeCopyWithDifferentXml(policy, revisionXml);
            toCheckpoint.setVersion(toCheckpoint.getVersion() - 1);
            final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(toCheckpoint, false, true);
            return new PolicyCheckpointState(oid, checkpoint.getOrdinal(), checkpoint.isActive());
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
            final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(makeCopyWithDifferentXml(curPolicy, revisionXml), false, false);
            return new PolicyCheckpointState(policyOid, checkpoint.getOrdinal(), checkpoint.isActive());
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
    }
}
