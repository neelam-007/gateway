package com.l7tech.server.policy;

import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.Policy;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.Set;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;

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
        return policyManager.findByGuid(guid);
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
    public Policy findByPrimaryKey(long oid) throws FindException {
        return policyManager.findByPrimaryKey(oid);
    }

    @Override
    public Policy findByHeader(EntityHeader header) throws FindException {
        return policyManager.findByHeader(header);
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
        return policyManager.findAll();
    }

    @Override
    public long save(Policy policy) throws SaveException {
        long oid = policyManager.save(policy);
        try {
            policyVersionManager.checkpointPolicy(policy, true, true);
        } catch ( ObjectModelException ome ) {
            throw new SaveException("Unable to save policy version.", ome);
        }
        return oid;
    }

    @Override
    public Integer getVersion(long oid) throws FindException {
        return policyManager.getVersion(oid);
    }

    @Override
    public Map<Long, Integer> findVersionMap() throws FindException {
        return policyManager.findVersionMap();
    }

    @Override
    public void delete(Policy entity) throws DeleteException {
        policyManager.delete(entity);
    }

    @Override
    public Policy getCachedEntity(long o, int maxAge) throws FindException, CacheVeto {
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
    public void delete(long oid) throws DeleteException, FindException {
        policyManager.delete(oid);
    }

    @Override
    public void update(Policy policy) throws UpdateException {
        policyManager.update(policy);
        try {
            policyVersionManager.checkpointPolicy(policy, true, true);
        } catch ( ObjectModelException ome ) {
            throw new UpdateException("Unable to save policy version.", ome);
        }
    }

    @Override
    public Collection<PolicyHeader> findHeaders(int offset, int windowSize, String filter) throws FindException {
        return policyManager.findHeaders(offset, windowSize, filter);
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return policyManager.getImpClass();
    }

    //- PRIVATE

    private final PolicyManager policyManager;
    private final PolicyVersionManager policyVersionManager;
}
