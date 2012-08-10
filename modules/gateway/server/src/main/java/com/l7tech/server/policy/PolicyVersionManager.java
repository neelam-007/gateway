package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.objectmodel.*;

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
     * @throws UpdateException if there is a database problem or other issue updating the policy
     */
    void deactivateVersions(long policyOid, long versionOid) throws UpdateException;

    /**
     * Find the 'active' version for the specified policy, if any.
     *
     * @param policyOid   the OID of the policy whose active version to find.  Required.
     * @return the version that is marked as active, or null if no active version was found.
     * @throws com.l7tech.objectmodel.FindException if ther eis a problem finding the requested information
     */
    PolicyVersion findActiveVersionForPolicy(long policyOid) throws FindException;

    /**
     * Examine the specified policy and record a new PolicyVersion if necessary.
     *
     * @param newPolicy a possibly-mutated policy that has not yet been committed to the database.
     *                  This policy must already have been assigned a valid OID.
     * @param activated if true, the newly saved revision should be marked as the active revision for this policy.
     * @param newEntity if true, this is a new Policy entity being created
     * @return the new PolicyVersion resulting from this checkpoint.
     * @throws com.l7tech.objectmodel.ObjectModelException if there is a problem finding or updating information from the database
     * @throws IllegalArgumentException if newPolicy does not have a valid OID
     */
    PolicyVersion checkpointPolicy(Policy newPolicy, boolean activated, boolean newEntity) throws ObjectModelException;

    /**
     * Finds the latest PolicyVersion for the given Policy Oid.
     *
     * @param policyOid the OID of the policy whose versions will be searched. Required.
     * @return the latest PolicyVersion.
     */
    PolicyVersion findLatestRevisionForPolicy(long policyOid);
}
