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
    public Policy findPolicyByPrimaryKey( Goid goid ) throws FindException {
        return delegate.findPolicyByPrimaryKey( goid );
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
    public PolicyAlias findAliasByEntityAndFolder(Goid entityGoid, Goid folderGoid) throws FindException {
        return delegate.findAliasByEntityAndFolder(entityGoid, folderGoid);
    }


    @Override
    public void deleteEntityAlias(String oid) throws DeleteException {
        delegate.deleteEntityAlias(oid);
    }

    @Override
    public Goid saveAlias(PolicyAlias policyAlias) throws SaveException {
        return delegate.saveAlias(policyAlias);
    }

    @Override
    public void deletePolicy( Goid policyGoid ) throws PolicyDeletionForbiddenException, DeleteException, FindException, ConstraintViolationException {
        delegate.deletePolicy( policyGoid );
        fireEntityDelete( policyGoid );
    }

    @Override
    public Pair<Goid,String> savePolicy( Policy policy ) throws PolicyAssertionException, SaveException {
        Pair<Goid,String> policyOidGuid = delegate.savePolicy( policy );
        fireEntityUpdate( policyOidGuid.left );
        return policyOidGuid;
    }

    @Override
    public PolicyCheckpointState savePolicy( Policy policy, boolean activateAsWell ) throws PolicyAssertionException, SaveException {
        PolicyCheckpointState result = delegate.savePolicy( policy, activateAsWell );
        fireEntityUpdate( result.getPolicyGoid() );
        return result;
    }

    @Override
    public SavePolicyWithFragmentsResult savePolicy(Policy policy, boolean activateAsWell, HashMap<String, Policy> fragments) throws PolicyAssertionException, SaveException {
        SavePolicyWithFragmentsResult result = delegate.savePolicy(policy, activateAsWell, fragments);
        fireEntityUpdate( result.policyCheckpointState.getPolicyGoid() );
        return result;
    }

    @Override
    public Set<Policy> findUsages( Goid goid ) throws FindException {
        return delegate.findUsages( goid );
    }

    @Override
    public PolicyVersion findPolicyVersionByPrimaryKey( Goid policyGoid, Goid versionGoid ) throws FindException {
        return delegate.findPolicyVersionByPrimaryKey( policyGoid, versionGoid );
    }

    @Override
    public List<PolicyVersion> findPolicyVersionHeadersByPolicy( Goid policyGoid ) throws FindException {
        return delegate.findPolicyVersionHeadersByPolicy( policyGoid );
    }

    @Override
    public void setPolicyVersionComment( Goid policyGoid, Goid versionGoid, String comment ) throws FindException, UpdateException {
        delegate.setPolicyVersionComment( policyGoid, versionGoid, comment );
    }

    @Override
    public void setActivePolicyVersion( Goid policyGoid, Goid versionGoid ) throws FindException, UpdateException {
        delegate.setActivePolicyVersion( policyGoid, versionGoid );
        fireEntityUpdate( policyGoid );
    }

    @Override
    public PolicyVersion findActivePolicyVersionForPolicy( Goid policyGoid ) throws FindException {
        return delegate.findActivePolicyVersionForPolicy( policyGoid );
    }

    @Override
    public PolicyVersion findPolicyVersionForPolicy(Goid policyGoid, long versionOrdinal) throws FindException {
        return delegate.findPolicyVersionForPolicy(policyGoid, versionOrdinal);
    }

    @Override
    public void clearActivePolicyVersion( Goid policyGoid ) throws FindException, UpdateException {
        delegate.clearActivePolicyVersion( policyGoid );
        fireEntityUpdate( policyGoid );
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
    public PolicyVersion findLatestRevisionForPolicy(final Goid policyGoid) {
        return delegate.findLatestRevisionForPolicy(policyGoid);
    }

    @Override
    public Policy findByAlias(Goid aliasGoid) throws FindException {
        return delegate.findByAlias(aliasGoid);
    }

    //- PRIVATE

    private final PolicyAdmin delegate;
    private final EntityInvalidationListener listener;

    /**
     * 
     */
    private void fireEntityDelete( Goid policyGoid ) {
        if ( listener != null ) {
            listener.notifyDelete( new EntityHeader( Goid.toString(policyGoid), EntityType.POLICY, "", "") );
        }
    }

    /**
     *
     */
    private void fireEntityUpdate( Goid policyGoid ) {
        if ( listener != null ) {
            listener.notifyUpdate( new EntityHeader( Goid.toString(policyGoid), EntityType.POLICY, "", "") );
        }
    }
}
