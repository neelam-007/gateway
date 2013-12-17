package com.l7tech.server.policy;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.identity.external.PolicyBackedIdentityProviderConfig;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.external.PolicyBackedIdentityProvider;
import com.l7tech.server.policy.export.PolicyExporterImporterManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.*;
import com.l7tech.util.Functions.Unary;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.inject.Named;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.cert.CertificateEncodingException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.util.Functions.map;
import static com.l7tech.util.Pair.pair;

/**
 * @author alex
 */
public class PolicyAdminImpl implements PolicyAdmin {
    protected static final Logger logger = Logger.getLogger(PolicyAdminImpl.class.getName());

    private final PolicyManager policyManager;
    private final PolicyCache policyCache;
    private final PolicyVersionManager policyVersionManager;
    private final ServiceManager serviceManager;
    private final ClusterPropertyManager clusterPropertyManager;
    private final PolicyExporterImporterManager policyExporterImporterManager;
    private final RbacServices rbacServices;

    @Inject
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;

    @Inject
    private PolicyAssertionRbacChecker policyChecker;

    @Inject
    @Named("identityProviderFactory")
    private IdentityProviderFactory identityProviderFactory;

    private static final Set<PropertyDescriptor> OMIT_VERSION_AND_XML = BeanUtils.omitProperties(BeanUtils.getProperties(Policy.class), "version", "xml");
    private static final Set<PropertyDescriptor> OMIT_XML = BeanUtils.omitProperties(BeanUtils.getProperties(Policy.class), "xml");
    private final PolicyAliasManager policyAliasManager;

    private Config config;
    private LicenseManager licenseManager;
    private DefaultKey defaultKey;


    public PolicyAdminImpl(final PolicyManager policyManager,
                           final PolicyAliasManager policyAliasManager,
                           final PolicyCache policyCache,
                           final PolicyVersionManager policyVersionManager,
                           final ServiceManager serviceManager,
                           final ClusterPropertyManager clusterPropertyManager,
                           final PolicyExporterImporterManager policyExporterImporterManager,
                           final RbacServices rbacServices,
                           final LicenseManager licenseManager,
                           final DefaultKey defaultKey)
    {
        this.policyManager = policyManager;
        this.policyAliasManager = policyAliasManager;
        this.policyCache = policyCache;
        this.policyVersionManager = policyVersionManager;
        this.serviceManager = serviceManager;
        this.clusterPropertyManager = clusterPropertyManager;
        this.policyExporterImporterManager = policyExporterImporterManager;
        this.rbacServices = rbacServices;
        this.licenseManager = licenseManager;
        this.defaultKey = defaultKey;
    }

    @Override
    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types) throws FindException{
        return policyManager.findHeadersWithTypes(types);
    }

    @Override
    public PolicyAlias findAliasByEntityAndFolder(Goid entityGoid, Goid folderGoid) throws FindException {
        return policyAliasManager.findAliasByEntityAndFolder(entityGoid, folderGoid);
    }

    @Override
    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types, boolean includeAliases)
            throws FindException{
        return policyManager.findHeadersWithTypes(types, includeAliases);
    }

    @Override
    public Policy findPolicyByPrimaryKey(Goid goid) throws FindException {
        Policy policy = policyManager.findByPrimaryKey(goid);
        if (policy == null) return null;
        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(goid);
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }
        return policy;
    }

    @Override
    public Policy findPolicyByUniqueName(String name) throws FindException {
        Policy policy = policyManager.findByUniqueName(name);
        if (policy == null) return null;
        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(policy.getGoid());
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }
        return policy;
    }

    @Override
    public Policy findPolicyByGuid(String guid) throws FindException {
        Policy policy = policyManager.findByGuid(guid);
        if (policy == null) return null;
        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy(policy.getGoid());
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }
        return policy;
    }

    @Override
    public Collection<PolicyHeader> findPolicyHeadersByType( PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    @Override
    public void deletePolicy(Goid goid) throws PolicyDeletionForbiddenException, DeleteException, FindException, ConstraintViolationException {
        checkForPolicyInUseAsSpecialPolicy(goid);
        policyManager.delete(goid);
    }

    private void checkForPolicyInUseAsSpecialPolicy(Goid goid) throws FindException, PolicyDeletionForbiddenException {
        if ( config == null)
            return;
        Policy policy = policyManager.findByPrimaryKey(goid);
        if (policy == null)
            return;

        final Collection<EncapsulatedAssertionConfig> configsWhichReferencePolicy = encapsulatedAssertionConfigManager.findByPolicyGoid(policy.getGoid());
        if (encapsulatedAssertionConfigManager != null && configsWhichReferencePolicy.size() > 0) {
            throw  new PolicyDeletionForbiddenException(policy, EntityType.ENCAPSULATED_ASSERTION, configsWhichReferencePolicy.iterator().next());
        }

        if (PolicyType.IDENTITY_PROVIDER_POLICY.equals(policy.getType())) {
            // See if it is in use
            Collection<IdentityProvider> providers = identityProviderFactory.findAllIdentityProviders();
            for (IdentityProvider provider : providers) {
                if (provider instanceof PolicyBackedIdentityProvider) {
                    PolicyBackedIdentityProviderConfig config = (PolicyBackedIdentityProviderConfig) provider.getConfig();
                    if (Goid.equals(policy.getGoid(), config.getPolicyId()))
                        throw new PolicyDeletionForbiddenException(policy, EntityType.ID_PROVIDER_CONFIG, config);
                }
            }
        }

        if (!(PolicyType.INTERNAL.equals(policy.getType())))
            return;
        String guid = policy.getGuid();

        String sinkGuid = config.getProperty( ServerConfigParams.PARAM_AUDIT_SINK_POLICY_GUID );
        if (sinkGuid != null && sinkGuid.trim().equals(guid))
            throw new PolicyDeletionForbiddenException(policy, EntityType.CLUSTER_PROPERTY, "it is currently in use as the global audit sink policy");

        String lookupGuid = config.getProperty( ServerConfigParams.PARAM_AUDIT_LOOKUP_POLICY_GUID );
        if (lookupGuid != null && lookupGuid.trim().equals(guid))
            throw new PolicyDeletionForbiddenException(policy, EntityType.CLUSTER_PROPERTY, "it is currently in use as the global audit lookup policy");

        String traceGuid = config.getProperty( ServerConfigParams.PARAM_TRACE_POLICY_GUID );
        if (traceGuid != null && traceGuid.trim().equals(guid)) {
            if (atLeastOneServiceHasTracingEnabled())
                throw new PolicyDeletionForbiddenException(policy, EntityType.CLUSTER_PROPERTY, "it is currently in use as the global debug trace policy");
            cleanupTraceClusterProperty();
        }
    }

    private boolean atLeastOneServiceHasTracingEnabled() throws FindException {
        if (serviceManager == null)
            return true;
        Collection<ServiceHeader> serviceHeaders = serviceManager.findAllHeaders();
        for (ServiceHeader serviceHeader : serviceHeaders) {
            if (serviceHeader.isTracingEnabled())
                return true;
        }
        return false;
    }

    private void cleanupTraceClusterProperty() {
        if (clusterPropertyManager == null)
            return;
        try {
            ClusterProperty traceProp = clusterPropertyManager.findByUniqueName( ServerConfigParams.PARAM_TRACE_POLICY_GUID);
            if (traceProp == null)
                return;

            clusterPropertyManager.delete(traceProp);
        } catch (FindException e) {
            logger.log(Level.INFO, "Unable to look up trace.policy.guid cluster property: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } catch (DeleteException e) {
            logger.log(Level.INFO, "Unable to delete no-longer-needed trace.policy.guid cluster property: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
    }

    @Override
    public Pair<Goid,String> savePolicy(Policy policy) throws SaveException {
        final PolicyCheckpointState policyCheckpointState = savePolicy(policy, true);
        return pair( policyCheckpointState.getPolicyGoid(), policyCheckpointState.getPolicyGuid() );
    }

    @Override
    public void deleteEntityAlias(String aliasGoid) throws DeleteException {
        final PolicyAlias alias;
        try {
            Goid goid = Goid.parseGoid(aliasGoid);
            alias = policyAliasManager.findByPrimaryKey(goid);
            policyAliasManager.delete(alias);
            logger.info("Deleted PolicyAlias: " + goid);
        } catch (FindException e) {
            throw new DeleteException("Could not find object to delete.", e);
        }
    }

    @Override
    public Goid saveAlias(PolicyAlias pa) throws SaveException {
        Goid goid;
        try {
            if(!Goid.isDefault(pa.getGoid()) ){
                goid = pa.getGoid();
                logger.fine("Updating PolicyAlias: " + goid);
                policyAliasManager.update(pa);
            }else{
                logger.fine("Saving new PolicyAlias");
                goid = policyAliasManager.save(pa);
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

        return goid;
    }

    @Override
    public PolicyCheckpointState savePolicy(Policy policy, boolean activateAsWell) throws SaveException {
        try {
            if (!activateAsWell)
                return saveWithoutActivating(policy);

            if (Goid.isDefault(policy.getGoid()) ) {
                ensureGuid( policy );
                policyChecker.checkPolicy(policy);
                final Goid goid = policyManager.save(policy);
                final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(policy, activateAsWell, true);
                policyManager.createRoles(policy);
                return new PolicyCheckpointState(goid, policy.getGuid(), checkpoint.getOrdinal(), checkpoint.isActive());
            } else {
                final Policy existing = policyManager.findByPrimaryKey(policy.getGoid());
                if (!existing.getXml().equals(policy.getXml())) {
                    // only check rbac for assertions if the policy xml has changed
                    policyChecker.checkPolicy(policy);
                }
                policyManager.update(policy);
                final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(policy, true, false);
                long versionOrdinal = checkpoint.getOrdinal();
                return new PolicyCheckpointState(policy.getGoid(), policy.getGuid(), versionOrdinal, checkpoint.isActive());
            }
        } catch (SaveException e) {
            /* RETHROW to screen of ObjectModelException catch block below */
            throw e;
        } catch (UpdateException ue) {
            DuplicateObjectException doe = ExceptionUtils.getCauseIfCausedBy( ue, DuplicateObjectException.class );
            if ( doe != null ) {
                throw doe;
            } else {
                throw new SaveException("Couldn't update policy", ue);                
            }
        } catch (ObjectModelException | IOException e) {
            throw new SaveException("Couldn't update policy", e);
        }
    }

    private void ensureGuid( final Policy policy ) {
        if(policy.getGuid() == null) {
            UUID guid = UUID.randomUUID();
            policy.setGuid(guid.toString());
        }
    }

    private static enum FragmentImportAction {
        IGNORE,
        CREATE,
        UPDATE
    }
    
    @Override
    public SavePolicyWithFragmentsResult savePolicy( Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws SaveException {
        try {
            HashMap<String, String> fragmentNameGuidMap = new HashMap<String, String>();
            savePolicyFragments(activateAsWell, fragments, fragmentNameGuidMap);

            if(fragmentNameGuidMap.size() > 0) {
                Assertion rootAssertion = WspReader.getDefault().parsePermissively(policy.getXml(), WspReader.INCLUDE_DISABLED);
                policy.setXml(WspWriter.getPolicyXml(rootAssertion));
            }

            return new SavePolicyWithFragmentsResult(savePolicy(policy, activateAsWell), fragmentNameGuidMap);
        } catch(SaveException e) {
            throw e;
        } catch (ObjectModelException e) {
            throw new SaveException("Couldn't update policy", e);
        } catch(IOException e) {
            throw new SaveException("Couldn't update policy", e);
        }
    }

    private static class PolicyDependencyTreeNode {
        Policy policy;
        boolean visited = false;
        Set<PolicyDependencyTreeNode> dependants = new HashSet<PolicyDependencyTreeNode>();

        private PolicyDependencyTreeNode(Policy policy) {
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
    private void savePolicyFragments(boolean activateAsWell, HashMap<String, Policy> fragments, Map<String, String> fragmentNameGuidMap)
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
                    } else if (!Goid.isDefault(dependencyNode.policy.getGoid()) ) {
                        fragmentNameGuidMap.put(dependencyNode.policy.getName(), p.getGuid());
                    }
                } catch (FindException e) {
                    action = FragmentImportAction.CREATE;
                }

                if (action == FragmentImportAction.CREATE) {
                    checkPermitted( OperationType.CREATE, EntityType.POLICY );

                    dependencyNode.policy.setGoid(Policy.DEFAULT_GOID);
                    policyChecker.checkPolicy(dependencyNode.policy);
                    policyManager.save(dependencyNode.policy);
                    policyVersionManager.checkpointPolicy(dependencyNode.policy, activateAsWell, true);
                    policyManager.createRoles(dependencyNode.policy);
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
     * Ensure the user is permitted to perform the specified action. 
     */
    private void checkPermitted( final OperationType operationType, final EntityType entityType ) throws ObjectModelException {
        final User user = JaasUtils.getCurrentUser();
        if ( user == null || !rbacServices.isPermittedForAnyEntityOfType( user, operationType, entityType ) ) {
            throw new PermissionDeniedException(operationType, entityType);
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
        Map<String, PolicyDependencyTreeNode> dependencyNodes = new HashMap<String, PolicyDependencyTreeNode>();
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
    private void updatePolicyDependencyTree(@Nullable Assertion rootAssertion, Set<String> requiredPolicies, HashMap<String, Policy> policies) {
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
        Goid policyGoid = policy.getGoid();
        if (Goid.isDefault(policyGoid)) {
            // Save new policy without activating it
            String revisionXml = policy.getXml();
            policy.disable();
            ensureGuid( policy );
            Goid goid = policyManager.save(policy);
            policyManager.createRoles(policy);
            try {
                policyChecker.checkPolicy(policy);
            } catch (IOException e) {
                throw new SaveException(e);
            }
            Policy toCheckpoint = makeCopyWithDifferentXml(policy, revisionXml);
            toCheckpoint.setVersion(toCheckpoint.getVersion() - 1);
            final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(toCheckpoint, false, true);
            return new PolicyCheckpointState(goid, policy.getGuid(), checkpoint.getOrdinal(), checkpoint.isActive());
        }

        try {
            // Save updated policy without activating it or changing the enable/disable state of the currently-in-effect policy
            String revisionXml = policy.getXml();
            Policy curPolicy = policyManager.findByPrimaryKey(policyGoid);
            if (curPolicy == null)
                throw new SaveException("No existing policy found with GOID=" + policyGoid);
            String curXml = curPolicy.getXml();
            BeanUtils.copyProperties(policy, curPolicy, OMIT_VERSION_AND_XML);
            curPolicy.setXml(curXml + ' '); // leave policy semantics unchanged but bump the version number
            policyManager.update(curPolicy);
            policyChecker.checkPolicy(curPolicy);
            final PolicyVersion checkpoint = policyVersionManager.checkpointPolicy(makeCopyWithDifferentXml(curPolicy, revisionXml), false, false);
            return new PolicyCheckpointState(policyGoid, policy.getGuid(), checkpoint.getOrdinal(), checkpoint.isActive());
        } catch (InvocationTargetException | IOException | IllegalAccessException e) {
            throw new SaveException(e);
        }
    }

    private static Policy makeCopyWithDifferentXml(Policy policy, String revisionXml) throws SaveException {
        try {
            Policy toCheckpoint = new Policy(policy.getType(), policy.getName(), revisionXml, policy.isSoap());
            BeanUtils.copyProperties(policy, toCheckpoint, OMIT_XML);
            return toCheckpoint;
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new SaveException("Unable to copy Policy properties", e); // can't happen
        }
    }

    @Override
    public Set<Policy> findUsages(Goid goid) throws FindException {
        return policyCache.findUsages(goid);
    }

    @Override
    public PolicyVersion findPolicyVersionByPrimaryKey(Goid policyGoid, Goid versionGoid) throws FindException {
        return policyVersionManager.findByPrimaryKey(policyGoid, versionGoid);
    }

    @Override
    public PolicyVersion findLatestRevisionForPolicy(final Goid policyGoid) {
        return policyVersionManager.findLatestRevisionForPolicy(policyGoid);
    }

    @Override
    public Policy findByAlias(final Goid aliasGoid) throws FindException {
        Policy found = null;
        final PolicyAlias alias = policyAliasManager.findByPrimaryKey(aliasGoid);
        if (alias != null) {
            found = findPolicyByPrimaryKey(alias.getEntityGoid());
        }
        return found;
    }

    @Override
    public List<PolicyVersion> findPolicyVersionHeadersByPolicy(Goid policyGoid) throws FindException {
        final Set<PropertyDescriptor> allButXml = BeanUtils.omitProperties(BeanUtils.getProperties(PolicyVersion.class), "xml");

        return map(policyVersionManager.findAllForPolicy(policyGoid), new Unary<PolicyVersion, PolicyVersion>() {
            @Override
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

    @Override
    public void setPolicyVersionComment(Goid policyGoid, Goid versionGoid, String comment) throws FindException, UpdateException {
        PolicyVersion ver = policyVersionManager.findByPrimaryKey(policyGoid, versionGoid);
        if (ver == null) throw new FindException("No PolicyVersion found with policyGoid=" + policyGoid + " and goid=" + versionGoid);
        ver.setName(comment);
        policyVersionManager.update(ver);
    }

    @Override
    public void setActivePolicyVersion(Goid policyGoid, Goid versionGoid) throws FindException, UpdateException {
        PolicyVersion ver = policyVersionManager.findByPrimaryKey(policyGoid, versionGoid);
        if (ver == null) throw new FindException("No PolicyVersion found with policyGoid=" + policyGoid + " and goid=" + versionGoid);

        Policy policy = policyManager.findByPrimaryKey(policyGoid);
        if (policy == null) throw new FindException("No Policy found with policyGoid=" + policyGoid); // shouldn't be possible

        policy.setXml(ver.getXml());
        ver.setActive(true);
        policyManager.update(policy);
        policyVersionManager.update(ver);
        policyVersionManager.deactivateVersions(policyGoid, versionGoid);
    }

    @Override
    public PolicyVersion findActivePolicyVersionForPolicy(Goid policyGoid) throws FindException {
        return policyVersionManager.findActiveVersionForPolicy(policyGoid);
    }

    @Override
    public PolicyVersion findPolicyVersionForPolicy(Goid policyGoid, long versionOrdinal) throws FindException {
        return policyVersionManager.findPolicyVersionForPolicy(policyGoid, versionOrdinal);
    }

    @Override
    public void clearActivePolicyVersion(Goid policyGoid) throws FindException, UpdateException {
        Policy policy = policyManager.findByPrimaryKey(policyGoid);
        if (policy == null) throw new FindException("No Policy found with policyGoid=" + policyGoid);
        if (isAuditSinkPolicy(policy)) throw new UpdateException("Not allowed to clear active version for the audit sink policy");

        policy.disable();
        policyManager.update(policy);
        policyVersionManager.deactivateVersions(policyGoid, PolicyVersion.DEFAULT_GOID);
    }

    public void setServerConfig(Config config ) {
        this.config = config;
    }

    @Override
    public long getXmlMaxBytes(){
        return Message.getMaxBytes();
    }

    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() {
        return policyExporterImporterManager.findAllExternalReferenceFactories();
    }

    @Override
    public String getDefaultPolicyXml(PolicyType type, String internalTag) {

        if(type == null || internalTag == null || internalTag.trim().isEmpty()){
            throw new IllegalArgumentException("type cannot be null. internalTag cannot be null or empty.");
        }
        
        if(type == PolicyType.INTERNAL){
            if( PolicyType.TAG_AUDIT_MESSAGE_FILTER.equals(internalTag)){
                return getAuditMessageFilterDefaultPolicy();
            } else if (PolicyType.TAG_AUDIT_VIEWER.equals(internalTag)){
                return getAuditViewerDefaultPolicy();
            }
        }
        return null;
    }

    private String getAuditMessageFilterDefaultPolicy(){
        //By using XML, which should always be backwards compatible, we don't need to add dependencies for
        //modular assertions

        String auditViewerCertB64 = null;
        if (defaultKey != null) {
            SsgKeyEntry avInfo = defaultKey.getAuditViewerInfo();
            if (avInfo != null && avInfo.getCertificate() != null) {
                try {
                    auditViewerCertB64 = HexUtils.encodeBase64(avInfo.getCertificate().getEncoded(), true);
                } catch (CertificateEncodingException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        return getDefaultXmlBasedOnLicense(getDefaultAuditMessageFilterPolicyXml(auditViewerCertB64),
                FALLBACK_AUDIT_MESSAGE_FILTER_POLICY_XML, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
    }

    private String getAuditViewerDefaultPolicy(){
        return getDefaultXmlBasedOnLicense(DEFAULT_AUDIT_VIEWER_POLICY_XML,
                FALLBACK_AUDIT_VIEWER_POLICY_XML, PolicyType.TAG_AUDIT_VIEWER);

    }

    /**
     * Choose between the default xml and the fallback xml.
     * <p/>
     * Protected visibility for test cases.
     *
     * @param defaultXml        desired default xml. Validated to be valid.
     * @param fallbackXml       fallback xml, not validated.
     * @param policyInternalTag internal tag string used for logging warning.
     * @return defaultXml if it contains no unlicensed assertions and is valid policy XML, otherwise the fallbackXml.
     */
    protected String getDefaultXmlBasedOnLicense(final String defaultXml,
                                                 final String fallbackXml,
                                                 final String policyInternalTag) {
        try {
            final Assertion assertion = WspReader.getDefault().parsePermissively(
                    defaultXml, WspReader.INCLUDE_DISABLED);

            if (assertion instanceof CompositeAssertion) {
                CompositeAssertion root = (CompositeAssertion) assertion;
                final boolean defaultContainsUnlicensedAssertion = xmlContainsUnlicensedAssertion(root);
                if (defaultContainsUnlicensedAssertion) {
                    return fallbackXml;
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not parse default " + policyInternalTag + " policy xml.");
            //should not happen
            //fall through
        }

        return defaultXml;
    }

    private boolean xmlContainsUnlicensedAssertion(CompositeAssertion parent){
        final List<Assertion> kids = parent.getChildren();
        if (kids.isEmpty()) return false;

        for (Assertion kid : kids) {
            if (kid instanceof CompositeAssertion){
                if(xmlContainsUnlicensedAssertion((CompositeAssertion) kid)){
                    return true;
                }
            }
            final String featureSetName = kid.getFeatureSetName();
            if (!licenseManager.isFeatureEnabled(featureSetName)){
                return true;
            }
        }

        return false;
    }

    private boolean isAuditSinkPolicy(Policy policy) {
        return policy != null && PolicyType.INTERNAL.equals(policy.getType()) && "audit-sink".equals(policy.getInternalTag());
    }

    private static final String AMF_COMMENT_FRAGMENT =
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Add policy logic to scrub / protect the request or response messages before they are audited.\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Policy is invoked by the audit sub system post service and global policy processing.\"/>\n" +
            "        </L7p:CommentAssertion>\n";

    public static String getDefaultAuditMessageFilterPolicyXml(String recipientCertBase64) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AMF_COMMENT_FRAGMENT +
            "        <L7p:EncodeDecode>\n" +
            "            <L7p:SourceVariableName stringValue=\"request.mainpart\"/>\n" +
            "            <L7p:TargetContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:TargetDataType variableDataType=\"message\"/>\n" +
            "            <L7p:TargetVariableName stringValue=\"request\"/>\n" +
            "            <L7p:TransformType transformType=\"BASE64_ENCODE\"/>\n" +
            "        </L7p:EncodeDecode>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PHNhdmVkbWVzc2FnZSB4bWxucz0iaHR0cDovL2xheWVyN3RlY2guY29tL25zL2F1ZGl0Ij4NCiR7cmVxdWVzdC5tYWlucGFydH0NCjwvc2F2ZWRtZXNzYWdlPg==\"/>\n" +
            "            <L7p:ContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:DataType variableDataType=\"message\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"request\"/>\n" +
            "        </L7p:SetVariable>\n" + (recipientCertBase64 != null ? "" :
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"Configure cert to use here. Should match the Audit Viewer Private Key if defined.\"/>\n" +
            "        </L7p:CommentAssertion>\n") +
            "        <L7p:NonSoapEncryptElement>\n" + (recipientCertBase64 == null ? ""
                      : "<L7p:RecipientCertificateBase64 stringValueReference=\"inline\"><![CDATA[" + recipientCertBase64 + "]]></L7p:RecipientCertificateBase64>") +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "            <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                <L7p:Expression stringValue=\"/*\"/>\n" +
            "                <L7p:Namespaces mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"xenc\"/>\n" +
            "                        <L7p:value stringValue=\"http://www.w3.org/2001/04/xmlenc#\"/>\n" +
            "                    </L7p:entry>\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"ds\"/>\n" +
            "                        <L7p:value stringValue=\"http://www.w3.org/2000/09/xmldsig#\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Namespaces>\n" +
            "            </L7p:XpathExpression>\n" +
            "        </L7p:NonSoapEncryptElement>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
    }

    public static final String FALLBACK_AUDIT_MESSAGE_FILTER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AMF_COMMENT_FRAGMENT +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final String AV_COMMENT_FRAGMENT =
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Add logic to transform audited messages and details.\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"// Policy is invoked from the audit viewer.\"/>\n" +
            "        </L7p:CommentAssertion>\n";

    public static final String DEFAULT_AUDIT_VIEWER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AV_COMMENT_FRAGMENT +
            "        <L7p:NonSoapDecryptElement/>\n" +
            "        <L7p:RequestXpathAssertion>\n" +
            "            <L7p:VariablePrefix stringValue=\"output\"/>\n" +
            "            <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                <L7p:Expression stringValue=\"/ns:savedmessage\"/>\n" +
            "                <L7p:Namespaces mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"ns\"/>\n" +
            "                        <L7p:value stringValue=\"http://layer7tech.com/ns/audit\"/>\n" +
            "                    </L7p:entry>\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"s\"/>\n" +
            "                        <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Namespaces>\n" +
            "            </L7p:XpathExpression>\n" +
            "        </L7p:RequestXpathAssertion>\n" +
            "        <L7p:EncodeDecode>\n" +
            "            <L7p:CharacterEncoding stringValueNull=\"null\"/>\n" +
            "            <L7p:SourceVariableName stringValue=\"output.result\"/>\n" +
            "            <L7p:TargetContentType stringValue=\"text/xml; charset=utf-8\"/>\n" +
            "            <L7p:TargetDataType variableDataType=\"message\"/>\n" +
            "            <L7p:TargetVariableName stringValue=\"request\"/>\n" +
            "            <L7p:TransformType transformType=\"BASE64_DECODE\"/>\n" +
            "        </L7p:EncodeDecode>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    public static final String FALLBACK_AUDIT_VIEWER_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            AV_COMMENT_FRAGMENT +
            "    </wsp:All>\n" +
            "</wsp:Policy>";
}
