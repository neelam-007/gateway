package com.l7tech.server.policy;

import com.l7tech.common.policy.PolicyVersion;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;

import java.util.List;

/**
 * Provides CRUD services for PolicyVersion instances.
 */
public interface PolicyVersionManager extends EntityManager<PolicyVersion, EntityHeader> {
    /**
     * Find a specific revision of the specified policy by its OID.
     *
     * @param policyOid the policy whose revision to load.
     * @param policyVersionOid the OID of the revision to load.
     * @return the requested revision, or null if no such policy or revision exists.
     * @throws FindException if there is a database problem
     */
    PolicyVersion findByPrimaryKey(long policyOid, long policyVersionOid) throws FindException;

    /**
     * Find all revisions of the specified Policy OID.
     *
     * @param policyOid the policy whose saved revisions to load.
     * @return a List of revisions.  May be empty but never null.
     * @throws FindException if there is a database problem
     */
    List<PolicyVersion> findAllForPolicy(long policyOid) throws FindException;

    /**
     * Clears the 'active' flag for all versions of the specified policy except for the specified version.
     *
     * @param policyOid   the policy whose versions are to have the 'active' flag cleared.  Required.
     * @param versionOid  a version to leave alone (its 'active' flag will not be altered); or,
     *                    {@link PolicyVersion#DEFAULT_OID} to deactivate all versions.    
     */
    void deactivateVersions(long policyOid, long versionOid) throws UpdateException;
}
