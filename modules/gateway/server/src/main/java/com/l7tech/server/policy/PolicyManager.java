/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.folder.FolderedEntityManager;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.policy.PolicyType;

import java.util.Collection;
import java.util.Set;

/**
 * @author alex
 */
public interface PolicyManager extends FolderedEntityManager<Policy, PolicyHeader>, GuidBasedEntityManager<Policy>, PropertySearchableEntityManager<PolicyHeader>, RoleAwareEntityManager<Policy> {

    /**
     * Find headers for policies of the given type.
     *
     * @param type The type of policy to access.
     * @return The collection of headers
     * @throws FindException if an error occurs
     */
    Collection<PolicyHeader> findHeadersByType( PolicyType type) throws FindException;

    /**
     * Create and save an RBAC {@link com.l7tech.gateway.common.security.rbac.Role} granting permission to read, update and delete the policy.
     *
     * Note that this method should not be invoked for {@link com.l7tech.policy.PolicyType#PRIVATE_SERVICE} policies; such policies
     * are covered under the appropriate "Manage Service" role.
     *
     * @param policy the policy being saved
     * @throws com.l7tech.objectmodel.SaveException if the Role cannot be saved
     */
    void addManagePolicyRole( Policy policy) throws SaveException;

    /**
     * Find the policy that has the specified GUID
     * 
     * @param guid The GUID of the policy to retrieve
     * @return The policy that has the specified GUID
     * @throws FindException If no policy can be found for the specified GUID
     */
    @Override
    Policy findByGuid(String guid) throws FindException;

    Collection<PolicyHeader> findHeadersWithTypes(Set<PolicyType> types) throws FindException;

    Collection<PolicyHeader> findHeadersWithTypes(Set<PolicyType> types, boolean includeAliases) throws FindException;

    /**
     * This will delete without validating the policy usages in the policy cache.
     * This is used in the bundle imported because the cache while within a transaction.
     * DO NOT USE unless you have a good reason for it.
     *
     * @param policy The policy to delete.
     * @throws DeleteException
     */
    void deleteWithoutValidation(Policy policy) throws DeleteException;
}
