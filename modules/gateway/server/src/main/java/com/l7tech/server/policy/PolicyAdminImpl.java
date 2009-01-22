/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.util.BeanUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions.Unary;
import static com.l7tech.util.Functions.map;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.policy.*;
import com.l7tech.gateway.common.admin.PolicyAdmin;

import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * @author alex
 */
public class PolicyAdminImpl implements PolicyAdmin {
    protected static final Logger logger = Logger.getLogger(PolicyAdminImpl.class.getName());

    private final PolicyManager policyManager;
    private final PolicyCache policyCache;
    private final PolicyVersionManager policyVersionManager;
    private final RoleManager roleManager;

    private static final Set<PropertyDescriptor> OMIT_VERSION_AND_XML = BeanUtils.omitProperties(BeanUtils.getProperties(Policy.class), "version", "xml");
    private static final Set<PropertyDescriptor> OMIT_XML = BeanUtils.omitProperties(BeanUtils.getProperties(Policy.class), "xml");
    private final PolicyAliasManager policyAliasManager;

    public PolicyAdminImpl(PolicyManager policyManager,
                           PolicyAliasManager policyAliasManager,
                           PolicyCache policyCache,
                           PolicyVersionManager policyVersionManager,
                           RoleManager roleManager)
    {
        this.policyManager = policyManager;
        this.policyAliasManager = policyAliasManager;
        this.policyCache = policyCache;
        this.policyVersionManager = policyVersionManager;
        this.roleManager = roleManager;
    }

    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types) throws FindException{
        return policyManager.findHeadersWithTypes(types);
    }

    public PolicyAlias findAliasByEntityAndFolder(Long entityOid, Long folderOid) throws FindException {
        return policyAliasManager.findAliasByEntityAndFolder(entityOid, folderOid);
    }

    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types, boolean includeAliases)
            throws FindException{
        return policyManager.findHeadersWithTypes(types, includeAliases);
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

    public Policy findPolicyByGuid(String guid) throws FindException {
        Policy policy = policyManager.findByGuid(guid);
        if (policy == null) return null;
        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(policy.getOid());
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }
        return policy;
    }

    public Collection<PolicyHeader> findPolicyHeadersByType( PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    public void deletePolicy(long oid) throws PolicyDeletionForbiddenException, DeleteException, FindException, ConstraintViolationException {
        policyManager.delete(oid);
        roleManager.deleteEntitySpecificRoles(EntityType.POLICY, oid);
    }

    public long savePolicy(Policy policy) throws SaveException {
        return savePolicy(policy, true).getPolicyOid();
    }

    public void deleteEntityAlias(String policyOid) throws DeleteException {
        final PolicyAlias alias;
        try {
            long oid = Long.parseLong(policyOid);
            alias = policyAliasManager.findByPrimaryKey(oid);
            policyAliasManager.delete(alias);
            logger.info("Deleted PolicyAlias: " + oid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    public long saveAlias(PolicyAlias pa) throws SaveException {
        long oid;
        try {
            if(pa.getOid() > 0){
                oid = pa.getOid();
                logger.fine("Updating PolicyAlias: " + oid);
                policyAliasManager.update(pa);
            }else{
                logger.fine("Saving new PolicyAlias");
                oid = policyAliasManager.save(pa);
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

        return oid;
    }

    public PolicyCheckpointState savePolicy(Policy policy, boolean activateAsWell) throws SaveException {
        try {
            if (!activateAsWell)
                return saveWithoutActivating(policy);

            if (policy.getOid() == Policy.DEFAULT_OID) {
                if(policy.getGuid() == null) {
                    UUID guid = UUID.randomUUID();
                    policy.setGuid(guid.toString());
                }
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

    private static enum FragmentImportAction {
        IGNORE,
        CREATE,
        UPDATE
    }
    
    public SavePolicyWithFragmentsResult savePolicy( Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws SaveException {
        try {
            HashMap<String, String> fragmentNameGuidMap = new HashMap<String, String>();
            savePolicyFragments(activateAsWell, fragments, fragmentNameGuidMap);

            if(fragmentNameGuidMap.size() > 0) {
                Assertion rootAssertion = policy.getAssertion();
                policy.setXml(WspWriter.getPolicyXml(rootAssertion));
            }

            return new SavePolicyWithFragmentsResult(savePolicy(policy, activateAsWell), fragmentNameGuidMap);
        } catch(SaveException e) {
            throw e;
        } catch(Exception e) {
            throw new SaveException("Couldn't update policy", e);
        }
    }

    private static class PolicyDependencyTreeNode {
        Policy policy;
        boolean visited = false;
        Set<PolicyDependencyTreeNode> dependants = new HashSet<PolicyDependencyTreeNode>();

        public PolicyDependencyTreeNode(Policy policy) {
            this.policy = policy;
        }
    }

    /**
     * Saves the provided policy fragments.
     * If there is not an existing policy fragment with the same name, then a new policy fragment is created.
     * If there is an existing policy fragment with the same name but the provided policy fragment has an
     * ID < 0, then the existing policy fragment is not modified.
     * If there is an existing policy fragment with the same name but a different ID, then the existing
     * policy fragment is not modified.
     * If there is an existing policy fragment with the same name and ID, then the existing policy fragment
     * is updated.
     * @param activateAsWell If true, then the policy fragments are activated after they are created
     * @param fragments The policy fragments to save. The keys are the policy fragment names, and the values
     * are the policies.
     * @param fragmentNameGuidMap A map that is updated with the policy IDs. The keys are policy fragment
     * names and the values are policy fragment OIDs.
     * @throws IOException
     * @throws SaveException
     * @throws ObjectModelException
     */
    private void savePolicyFragments(boolean activateAsWell, HashMap<String, Policy> fragments, HashMap<String, String> fragmentNameGuidMap)
    throws IOException, ObjectModelException
    {
        Set<PolicyDependencyTreeNode> dependencyTree = generateIncludeDependencyTree(fragments);

        // The policies are saved by going across the tree, then down. No node will be visited twice.
        while(!dependencyTree.isEmpty()) {
            Set<PolicyDependencyTreeNode> newRoots = new HashSet<PolicyDependencyTreeNode>();

            // Work across the top level of the tree
            for (PolicyDependencyTreeNode dependencyNode : dependencyTree) {
                FragmentImportAction action = FragmentImportAction.IGNORE;

                try {
                    Policy p = policyManager.findByGuid(dependencyNode.policy.getGuid());
                    if (p == null) {
                        action = FragmentImportAction.CREATE;
                    } else if (dependencyNode.policy.getOid() > 0) {
                        fragmentNameGuidMap.put(dependencyNode.policy.getName(), p.getGuid());
                    }
                } catch (FindException e) {
                    action = FragmentImportAction.CREATE;
                }

                if (action == FragmentImportAction.CREATE) {
                    dependencyNode.policy.setOid(Policy.DEFAULT_OID);
                    policyManager.save(dependencyNode.policy);
                    policyVersionManager.checkpointPolicy(dependencyNode.policy, activateAsWell, true);
                    policyManager.addManagePolicyRole(dependencyNode.policy);
                    fragmentNameGuidMap.put(dependencyNode.policy.getName(), dependencyNode.policy.getGuid());
                }

                // Update dependants with the new policy oid for this policy
                for(PolicyDependencyTreeNode child : dependencyNode.dependants) {
                    if(!child.visited) {
                        newRoots.add(child);
                    }
                }
            }

            // Switch to the next level down
            dependencyTree = newRoots;
        }
    }

    /**
     * Generates a policy dependency tree. The root nodes do not depend on other policies. This is used to
     * determine the order to save the policies.
     * @param policies A HashMap of the policies
     * @return A set of roots for the policy dependency tree
     * @throws IOException If the XML for a policy cannot be converted to an Assertion tree
     */
    private Set<PolicyDependencyTreeNode> generateIncludeDependencyTree(HashMap<String, Policy> policies) throws IOException {
        HashMap<String, PolicyDependencyTreeNode> dependencyNodes = new HashMap<String, PolicyDependencyTreeNode>();
        Set<PolicyDependencyTreeNode> treeRoots = new HashSet<PolicyDependencyTreeNode>();

        for(Map.Entry<String, Policy> entry : policies.entrySet()) {
            dependencyNodes.put(entry.getKey(), new PolicyDependencyTreeNode(entry.getValue()));
        }

        for(Map.Entry<String, PolicyDependencyTreeNode> entry : dependencyNodes.entrySet()) {
            Set<String> requiredPolicies = new HashSet<String>();
            updatePolicyDependencyTree(entry.getValue().policy.getAssertion(), requiredPolicies, policies);

            if(requiredPolicies.isEmpty()) {
                treeRoots.add(entry.getValue());
            } else {
                for(String requiredPolicy : requiredPolicies) {
                    if(dependencyNodes.containsKey(requiredPolicy)) {
                        dependencyNodes.get(requiredPolicy).dependants.add(entry.getValue());
                    }
                }
            }
        }

        return treeRoots;
    }

    /**
     * Recursively scans an Assertion tree and adds all policies that the Assertion tree depends on to the
     * provided Set.
     * @param rootAssertion The root of the Assertion tree to scan
     * @param requiredPolicies The set to add required policy names to
     */
    private void updatePolicyDependencyTree(Assertion rootAssertion, Set<String> requiredPolicies, HashMap<String, Policy> policies) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                updatePolicyDependencyTree(child, requiredPolicies, policies);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(policies.containsKey(includeAssertion.getPolicyName())) {
                requiredPolicies.add(includeAssertion.getPolicyName());
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
