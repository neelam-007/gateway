/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyHeader;
import com.l7tech.objectmodel.*;

import java.util.Collection;
import java.util.EnumSet;

/**
 * @author alex
 */
public interface PolicyManager extends EntityManager<Policy, PolicyHeader>, GuidBasedEntityManager<Policy> {

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
     * @param guid The GUID of the policy to retrieve
     * @return The policy that has the specified GUID
     * @throws FindException If no policy can be found for the specified GUID
     */
    Policy findByGuid(String guid) throws FindException;

    Collection<PolicyHeader> findHeadersWithTypes(EnumSet<PolicyType> types);
}
