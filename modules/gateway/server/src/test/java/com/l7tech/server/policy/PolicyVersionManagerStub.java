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

    public PolicyVersionManagerStub(){
        super();
    }

    public PolicyVersionManagerStub(PolicyVersion... entitiesIn){
        super(entitiesIn);
    }

    @Override
    public PolicyVersion checkpointPolicy( final Policy newPolicy, final boolean activated, final boolean newEntity ) throws ObjectModelException {
        return checkpointPolicy(newPolicy, activated, null, newEntity);
    }

    @Override
    public PolicyVersion checkpointPolicy(Policy newPolicy, boolean activated, String comment, boolean newEntity) throws ObjectModelException {
        PolicyVersion version = new PolicyVersion();
        version.setPolicyGoid(newPolicy.getGoid());
        version.setActive(activated);
        version.setName(comment);
        long ordinal = (long) (newPolicy.getVersion() + 1);
        if (!newEntity) ordinal++;
        version.setOrdinal(ordinal);
        version.setTime(System.currentTimeMillis());
        version.setXml(newPolicy.getXml());



        if(activated && !newEntity){
            for(PolicyVersion oldVersion: findAllForPolicy(newPolicy.getGoid())){
                oldVersion.setActive(false);
            }
        }
        save(version);
        return version;
    }

    @Override
    public PolicyVersion findByPrimaryKey( final Goid policyOid, final Goid policyVersionOid ) throws FindException {
        for(PolicyVersion policyVersion : entities.values()){
            if(Goid.equals(policyVersion.getPolicyGoid(), policyOid) && Goid.equals(policyVersion.getGoid(), policyVersionOid)){
                return policyVersion;
            }
        }
        return null;
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
        for(PolicyVersion policyVersion : entities.values()){
            if(Goid.equals(policyVersion.getPolicyGoid(), policyOid) && policyVersion.isActive()){
                return policyVersion;
            }
        }
        return null;
    }

    @Override
    public PolicyVersion findPolicyVersionForPolicy(Goid policyGoid, long versionOrdinal) throws FindException {
        for(PolicyVersion policyVersion : entities.values()){
            if(Goid.equals(policyVersion.getPolicyGoid(), policyGoid) && policyVersion.getOrdinal() == versionOrdinal){
                return policyVersion;
            }
        }
        return null;
    }

    @Override
    public PolicyVersion findLatestRevisionForPolicy( final Goid policyOid ) {
        throw new UnsupportedOperationException("Not Implemented");
    }
}