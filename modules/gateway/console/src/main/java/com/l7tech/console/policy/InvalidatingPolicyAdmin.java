package com.l7tech.console.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.*;
import com.l7tech.gateway.common.admin.PolicyAdmin;

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

    public Policy findPolicyByPrimaryKey( long oid ) throws FindException {
        return delegate.findPolicyByPrimaryKey( oid );
    }

    public Policy findPolicyByUniqueName(String name) throws FindException {
        return delegate.findPolicyByUniqueName(name);
    }

    public Policy findPolicyByGuid(String guid) throws FindException {
        return delegate.findPolicyByGuid(guid);
    }

    public Collection<PolicyHeader> findPolicyHeadersByType( PolicyType type ) throws FindException {
        return delegate.findPolicyHeadersByType( type );
    }

    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types) throws FindException{
        return delegate.findPolicyHeadersWithTypes(types);
    }

    public Collection<PolicyHeader> findPolicyHeadersWithTypes(EnumSet<PolicyType> types, boolean includeAliases)
            throws FindException{
        return delegate.findPolicyHeadersWithTypes(types, includeAliases);
    }

    public PolicyAlias findAliasByEntityAndFolder(Long entityOid, Long folderOid) throws FindException {
        return delegate.findAliasByEntityAndFolder(entityOid, folderOid);
    }


    public void deleteEntityAlias(String oid) throws DeleteException {
        delegate.deleteEntityAlias(oid);
    }

    public long saveAlias(PolicyAlias policyAlias) throws SaveException {
        return delegate.saveAlias(policyAlias);
    }

    public void deletePolicy( long policyOid ) throws PolicyDeletionForbiddenException, DeleteException, FindException {
        delegate.deletePolicy( policyOid );
        fireEntityDelete( policyOid );
    }

    public long savePolicy( Policy policy ) throws PolicyAssertionException, SaveException {
        long policyOid = delegate.savePolicy( policy );
        fireEntityUpdate( policyOid );
        return policyOid;
    }

    public PolicyCheckpointState savePolicy( Policy policy, boolean activateAsWell ) throws PolicyAssertionException, SaveException {
        PolicyCheckpointState result = delegate.savePolicy( policy, activateAsWell );
        fireEntityUpdate( result.getPolicyOid() );
        return result;
    }

    public SavePolicyWithFragmentsResult savePolicy(Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws PolicyAssertionException, SaveException {
        SavePolicyWithFragmentsResult result = delegate.savePolicy(policy, activateAsWell, fragments);
        fireEntityUpdate( result.policyCheckpointState.getPolicyOid() );
        return result;
    }

    public Set<Policy> findUsages( long oid ) throws FindException {
        return delegate.findUsages( oid );
    }

    public PolicyVersion findPolicyVersionByPrimaryKey( long policyOid, long versionOid ) throws FindException {
        return delegate.findPolicyVersionByPrimaryKey( policyOid, versionOid );
    }

    public List<PolicyVersion> findPolicyVersionHeadersByPolicy( long policyOid ) throws FindException {
        return delegate.findPolicyVersionHeadersByPolicy( policyOid );
    }

    public void setPolicyVersionComment( long policyOid, long versionOid, String comment ) throws FindException, UpdateException {
        delegate.setPolicyVersionComment( policyOid, versionOid, comment );
    }

    public void setActivePolicyVersion( long policyOid, long versionOid ) throws FindException, UpdateException {
        delegate.setActivePolicyVersion( policyOid, versionOid );
        fireEntityUpdate( policyOid );
    }

    public PolicyVersion findActivePolicyVersionForPolicy( long policyOid ) throws FindException {
        return delegate.findActivePolicyVersionForPolicy( policyOid );
    }

    public void clearActivePolicyVersion( long policyOid ) throws FindException, UpdateException {
        delegate.clearActivePolicyVersion( policyOid );
        fireEntityUpdate( policyOid );
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
