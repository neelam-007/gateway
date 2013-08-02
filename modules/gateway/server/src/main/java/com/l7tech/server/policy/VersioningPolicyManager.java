package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Delegating PolicyManager that performs versioning of Policies.
 *
 * <p>Creating / updating a policy will activate the policy and create a policy
 * version.</p>
 *
 * TODO this class should probably not exist, versioning should occur whenever a policy is saved.
 */
@Transactional(rollbackFor=Throwable.class)
public class VersioningPolicyManager implements PolicyManager {

    //- PUBLIC

    public VersioningPolicyManager( final PolicyManager policyManager,
                                    final PolicyVersionManager policyVersionManager ) {
        this.policyManager = policyManager;
        this.policyVersionManager = policyVersionManager;
    }

    @Override
    public Collection<PolicyHeader> findHeadersByType(PolicyType type) throws FindException {
        return policyManager.findHeadersByType(type);
    }

    @Override
    public void addManagePolicyRole(Policy policy) throws SaveException {
        policyManager.addManagePolicyRole(policy);
    }

    @Override
    public Policy findByGuid(String guid) throws FindException {
        return processRevision(policyManager.findByGuid(guid));
    }

    @Override
    public Collection<PolicyHeader> findHeadersWithTypes(Set<PolicyType> types) throws FindException {
        return policyManager.findHeadersWithTypes(types);
    }

    @Override
    public Collection<PolicyHeader> findHeadersWithTypes(Set<PolicyType> types, boolean includeAliases) throws FindException {
        return policyManager.findHeadersWithTypes(types, includeAliases);
    }

    @Override
    public Policy findByPrimaryKey(final Goid goid) throws FindException {
        return processRevision( policyManager.findByPrimaryKey(goid) );
    }

    @Override
    public Policy findByPrimaryKey(long oid) throws FindException {
        return processRevision( policyManager.findByPrimaryKey(oid) );
    }

    @Override
    public Policy findByHeader(EntityHeader header) throws FindException {
        return processRevision( policyManager.findByHeader(header) );
    }

    @Override
    public Collection<PolicyHeader> findAllHeaders() throws FindException {
        return policyManager.findAllHeaders();
    }

    @Override
    public Collection<PolicyHeader> findAllHeaders(int offset, int windowSize) throws FindException {
        return policyManager.findAllHeaders(offset, windowSize);
    }

    @Override
    public Collection<Policy> findAll() throws FindException {
        return processRevisions( policyManager.findAll() );
    }

    @Override
    public Goid save(Policy policy) throws SaveException {
        if (policy != null)
            policy.setVersion(0);
        Goid goid = policyManager.save(policy);
        try {
            policyVersionManager.checkpointPolicy(policy, true, true);
        } catch ( ObjectModelException ome ) {
            throw new SaveException("Unable to save policy version.", ome);
        }
        return goid;
    }

    @Override
    public Integer getVersion(Goid goid) throws FindException {
        return policyManager.getVersion(goid);
    }

    @Override
    public Map<Goid, Integer> findVersionMap() throws FindException {
        return policyManager.findVersionMap();
    }

    @Override
    public void delete(Policy entity) throws DeleteException {
        policyManager.delete(entity);
    }

    @Override
    public Policy getCachedEntity(Goid o, int maxAge) throws FindException {
        return policyManager.getCachedEntity(o, maxAge);
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return policyManager.getInterfaceClass();
    }

    @Override
    public EntityType getEntityType() {
        return policyManager.getEntityType();
    }

    @Override
    public String getTableName() {
        return policyManager.getTableName();
    }

    @Override
    public Policy findByUniqueName(String name) throws FindException {
        return policyManager.findByUniqueName(name);
    }

    @Override
    public void delete(Goid goid) throws DeleteException, FindException {
        policyManager.delete(goid);
    }

    @Override
    public void update(Policy policy) throws UpdateException {
        policyManager.update(policy);
        try {
            policyVersionManager.checkpointPolicy(policy, true, false);
        } catch ( ObjectModelException ome ) {
            throw new UpdateException("Unable to save policy version when updating policy.", ome);
        }
    }

    @Override
    public void updateWithFolder(Policy policy) throws UpdateException {
        policyManager.updateWithFolder(policy);
        try {
            policyVersionManager.checkpointPolicy(policy, true, false);
        } catch ( ObjectModelException ome ) {
            throw new UpdateException("Unable to save policy version when updating policy.", ome);
        }
    }

    @Override
    public void updateFolder(Policy entity, Folder folder) throws UpdateException {
        policyManager.updateFolder(entity, folder);
    }

    @Override
    public void updateFolder(Goid entityId, Folder folder) throws UpdateException {
        policyManager.updateFolder(entityId, folder);
    }

    @Override
    public Collection<PolicyHeader> findHeaders(int offset, int windowSize, Map<String,String> filters) throws FindException {
        return policyManager.findHeaders(offset, windowSize, filters);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return policyManager.getImpClass();
    }

    @Override
    public void createRoles(Policy entity) throws SaveException {
        policyManager.createRoles(entity);
    }

    @Override
    public void updateRoles( final Policy entity ) throws UpdateException {
        policyManager.updateRoles( entity );
    }

    @Override
    public void deleteRoles( final Goid entityGoid ) throws DeleteException {
        policyManager.deleteRoles( entityGoid );
    }

    //- PRIVATE

    private final PolicyManager policyManager;
    private final PolicyVersionManager policyVersionManager;

    private Collection<Policy> processRevisions( final Collection<Policy> policies ) throws FindException {
        if ( policies != null ) {
            for ( Policy policy : policies ) {
                processRevision( policy );    
            }
        }

        return policies;
    }

    private Policy processRevision( final Policy policy ) throws FindException {
        if (policy == null) return null;

        PolicyVersion activeVersion = policyVersionManager.findActiveVersionForPolicy( policy.getGoid() );
        if (activeVersion != null) {
            policy.setVersionOrdinal(activeVersion.getOrdinal());
            policy.setVersionActive(true);
        }

        return policy;
    }
}
