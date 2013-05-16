package com.l7tech.console.policy;

import com.l7tech.gateway.common.export.ExternalReferenceFactory;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.*;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.util.Pair;

import java.util.*;

/**
 * PolicyAdmin wrapper that fires invalidation events for the active policy.
 *
 * @author steve
 */
public class InvalidatingPolicyAdmin implements PolicyAdmin {

    //- PUBLIC

    public InvalidatingPolicyAdmin( final PolicyAdmin delegate,
                                    final EntityInvalidationListener listener ) {
        this.delegate = delegate;
        this.listener = listener;
    }

    @Override
    public Policy findPolicyByPrimaryKey( long oid ) throws FindException {
        return delegate.findPolicyByPrimaryKey( oid );
    }

    @Override
    public Policy findPolicyByUniqueName(String name) throws FindException {
        return delegate.findPolicyByUniqueName(name);
    }

    @Override
    public Policy findPolicyByGuid(String guid) throws FindException {
        return delegate.findPolicyByGuid(guid);
    }

    @Override
    public Collection<PolicyHeader> findPolicyHeadersByType( PolicyType type ) throws FindException {
        return delegate.findPolicyHeadersByType( type );
    }

    @Override
    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types) throws FindException{
        return delegate.findPolicyHeadersWithTypes(types);
    }

    @Override
    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types, boolean includeAliases)
            throws FindException{
        return delegate.findPolicyHeadersWithTypes(types, includeAliases);
    }

    @Override
    public PolicyAlias findAliasByEntityAndFolder(Long entityOid, Long folderOid) throws FindException {
        return delegate.findAliasByEntityAndFolder(entityOid, folderOid);
    }


    @Override
    public void deleteEntityAlias(String oid) throws DeleteException {
        delegate.deleteEntityAlias(oid);
    }

    @Override
    public long saveAlias(PolicyAlias policyAlias) throws SaveException {
        return delegate.saveAlias(policyAlias);
    }

    @Override
    public void deletePolicy( long policyOid ) throws PolicyDeletionForbiddenException, DeleteException, FindException, ConstraintViolationException {
        delegate.deletePolicy( policyOid );
        fireEntityDelete( policyOid );
    }

    @Override
    public Pair<Long,String> savePolicy( Policy policy ) throws PolicyAssertionException, SaveException {
        Pair<Long,String> policyOidGuid = delegate.savePolicy( policy );
        fireEntityUpdate( policyOidGuid.left );
        return policyOidGuid;
    }

    @Override
    public PolicyCheckpointState savePolicy( Policy policy, boolean activateAsWell ) throws PolicyAssertionException, SaveException {
        PolicyCheckpointState result = delegate.savePolicy( policy, activateAsWell );
        fireEntityUpdate( result.getPolicyOid() );
        return result;
    }

    @Override
    public SavePolicyWithFragmentsResult savePolicy(Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws PolicyAssertionException, SaveException {
        SavePolicyWithFragmentsResult result = delegate.savePolicy(policy, activateAsWell, fragments);
        fireEntityUpdate( result.policyCheckpointState.getPolicyOid() );
        return result;
    }

    @Override
    public Set<Policy> findUsages( long oid ) throws FindException {
        return delegate.findUsages( oid );
    }

    @Override
    public PolicyVersion findPolicyVersionByPrimaryKey( long policyOid, long versionOid ) throws FindException {
        return delegate.findPolicyVersionByPrimaryKey( policyOid, versionOid );
    }

    @Override
    public List<PolicyVersion> findPolicyVersionHeadersByPolicy( long policyOid ) throws FindException {
        return delegate.findPolicyVersionHeadersByPolicy( policyOid );
    }

    @Override
    public void setPolicyVersionComment( long policyOid, long versionOid, String comment ) throws FindException, UpdateException {
        delegate.setPolicyVersionComment( policyOid, versionOid, comment );
    }

    @Override
    public void setActivePolicyVersion( long policyOid, long versionOid ) throws FindException, UpdateException {
        delegate.setActivePolicyVersion( policyOid, versionOid );
        fireEntityUpdate( policyOid );
    }

    @Override
    public PolicyVersion findActivePolicyVersionForPolicy( long policyOid ) throws FindException {
        return delegate.findActivePolicyVersionForPolicy( policyOid );
    }

    @Override
    public void clearActivePolicyVersion( long policyOid ) throws FindException, UpdateException {
        delegate.clearActivePolicyVersion( policyOid );
        fireEntityUpdate( policyOid );
    }

    @Override
    public String getDefaultPolicyXml(PolicyType type, String internalTag) {
        return delegate.getDefaultPolicyXml(type, internalTag);
    }

    @Override
    public long getXmlMaxBytes(){
        return delegate.getXmlMaxBytes();
    }

    @Override
    public Set<ExternalReferenceFactory> findAllExternalReferenceFactories() {
        return delegate.findAllExternalReferenceFactories();
    }

    @Override
    public PolicyVersion findLatestRevisionForPolicy(final long policyOid) {
        return delegate.findLatestRevisionForPolicy(policyOid);
    }

    @Override
    public Collection<Policy> findBySecurityZoneOid(long securityZoneOid) {
        return delegate.findBySecurityZoneOid(securityZoneOid);
    }

    //- PRIVATE

    private final PolicyAdmin delegate;
    private final EntityInvalidationListener listener;

    /**
     * 
     */
    private void fireEntityDelete( long policyOid ) {
        if ( listener != null ) {
            listener.notifyDelete( new EntityHeader( Long.toString(policyOid), EntityType.POLICY, "", "") );
        }
    }

    /**
     *
     */
    private void fireEntityUpdate( long policyOid ) {
        if ( listener != null ) {
            listener.notifyUpdate( new EntityHeader( Long.toString(policyOid), EntityType.POLICY, "", "") );
        }
    }

}
