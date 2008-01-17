/**
 * Copyright (C) 2006-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy;

import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.PolicyHeader;

import java.util.Collection;

/**
 * @author alex
 */
public interface PolicyManager extends EntityManager<Policy, PolicyHeader> {

    /**
     * Find headers for policies of the given type.
     *
     * @param type The type of policy to access.
     * @return The collection of headers
     * @throws FindException if an error occurs
     */
    Collection<PolicyHeader> findHeadersByType(PolicyType type) throws FindException;

    /**
     * Create and save an RBAC {@link com.l7tech.common.security.rbac.Role} granting permission to read, update and delete the policy.
     *
     * Note that this method should not be invoked for {@link com.l7tech.common.policy.PolicyType#PRIVATE_SERVICE} policies; such policies
     * are covered under the appropriate "Manage Service" role.
     *
     * @param policy the policy being saved
     * @throws com.l7tech.objectmodel.SaveException if the Role cannot be saved
     */
    void addManagePolicyRole(Policy policy) throws SaveException;
}
