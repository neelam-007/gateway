package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.EntityManagerStub;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PolicyVersionManagerStub extends EntityManagerStub<PolicyVersion,EntityHeader> implements PolicyVersionManager {

    @Override
    public PolicyVersion checkpointPolicy( final Policy newPolicy, final boolean activated, final boolean newEntity ) throws ObjectModelException {
        throw new ObjectModelException("Not Implemented");
    }

    @Override
    public PolicyVersion findByPrimaryKey( final Goid policyOid, final Goid policyVersionOid ) throws FindException {
        return entities.get(policyVersionOid);
    }

    @Override
    public List<PolicyVersion> findAllForPolicy( final Goid policyOid ) throws FindException {
        ArrayList<PolicyVersion> policyVersions = new ArrayList<>();
        for(PolicyVersion policyVersion : entities.values()){
            if(Goid.equals(policyVersion.getPolicyGoid(), policyOid)){
                policyVersions.add(policyVersion);
            }
        }
        return policyVersions;
    }

    @Override
    public void deactivateVersions( final Goid policyOid, final Goid versionOid ) throws UpdateException {
        throw new UpdateException("Not Implemented");
    }

    @Override
    public PolicyVersion findActiveVersionForPolicy( final Goid policyOid ) throws FindException {
        throw new FindException("Not Implemented");
    }

    @Override
    public PolicyVersion findPolicyVersionForPolicy(Goid policyGoid, long versionOrdinal) throws FindException {
        throw new FindException("Not Implemented");
    }

    @Override
    public PolicyVersion findLatestRevisionForPolicy( final Goid policyOid ) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}