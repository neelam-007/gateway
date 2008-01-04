package com.l7tech.console.policy;

import com.l7tech.common.policy.*;
import com.l7tech.common.util.Pair;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.PolicyAssertionException;

import java.util.Collection;
import java.util.Set;
import java.util.List;

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

    public Collection<EntityHeader> findPolicyHeadersByType( PolicyType type ) throws FindException {
        return delegate.findPolicyHeadersByType( type );
    }

    public void deletePolicy( long policyOid ) throws PolicyDeletionForbiddenException, DeleteException, FindException {
        delegate.deletePolicy( policyOid );
        fireEntityInvalidated( policyOid );
    }

    public long savePolicy( Policy policy ) throws PolicyAssertionException, SaveException {
        return delegate.savePolicy( policy );
    }

    public Pair<Long, Long> savePolicy( Policy policy, boolean activateAsWell ) throws PolicyAssertionException, SaveException {
        Pair<Long, Long> result = delegate.savePolicy( policy, activateAsWell );
        fireEntityInvalidated( result.left );
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
        fireEntityInvalidated( policyOid );
    }

    public PolicyVersion findActivePolicyVersionForPolicy( long policyOid ) throws FindException {
        return delegate.findActivePolicyVersionForPolicy( policyOid );
    }

    public void clearActivePolicyVersion( long policyOid ) throws FindException, UpdateException {
        delegate.clearActivePolicyVersion( policyOid );
        fireEntityInvalidated( policyOid );
    }

    //- PRIVATE

    private final PolicyAdmin delegate;
    private final EntityInvalidationListener listener;

    /**
     * 
     */
    private void fireEntityInvalidated( long policyOid ) {
        if ( listener != null ) {
            listener.invalidate( new EntityHeader( Long.toString(policyOid), EntityType.POLICY, "", "") );
        }
    }
}
