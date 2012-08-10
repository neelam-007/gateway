package com.l7tech.server.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.server.EntityManagerStub;

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
    public PolicyVersion findByPrimaryKey( final long policyOid, final long policyVersionOid ) throws FindException {
        throw new FindException("Not Implemented");
    }

    @Override
    public List<PolicyVersion> findAllForPolicy( final long policyOid ) throws FindException {
        throw new FindException("Not Implemented");
    }

    @Override
    public void deactivateVersions( final long policyOid, final long versionOid ) throws UpdateException {
        throw new UpdateException("Not Implemented");
    }

    @Override
    public PolicyVersion findActiveVersionForPolicy( final long policyOid ) throws FindException {
        throw new FindException("Not Implemented");
    }

    @Override
    public PolicyVersion findLatestRevisionForPolicy( final long policyOid ) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}
