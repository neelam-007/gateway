/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.common.policy;

import static com.l7tech.common.security.rbac.EntityType.POLICY;
import com.l7tech.common.security.rbac.MethodStereotype;
import static com.l7tech.common.security.rbac.MethodStereotype.*;
import com.l7tech.common.security.rbac.Secured;
import com.l7tech.common.security.rbac.RbacAdmin;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.PolicyAssertionException;
import static org.springframework.transaction.annotation.Propagation.REQUIRED;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Set;

/**
 * Remote admin interface for managing {@link Policy} instances on the Gateway.
 * @author alex
 */
@Transactional(propagation=REQUIRED, rollbackFor=Throwable.class)
@Secured(types=POLICY)
public interface PolicyAdmin {
    String ROLE_NAME_TYPE_SUFFIX = "Policy";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    /**
     * Finds a particular {@link Policy} with the specified OID, or null if no such policy can be found.
     * @param oid the OID of the Policy to retrieve
     * @return the Policy with the specified OID, or null if no such policy can be found.
     */
    @Secured(stereotype=FIND_BY_PRIMARY_KEY)
    @Transactional(readOnly=true)
    Policy findPolicyByPrimaryKey(long oid) throws FindException;

    /**
     * Finds all policies in the system with the given type.
     * @param type the type of policies to find; pass <code>null</code> for policies of any type
     * @return the policies with the specified type
     */
    @Secured(stereotype=FIND_ENTITIES)
    @Transactional(readOnly=true)
    Collection<EntityHeader> findPolicyHeadersByType(PolicyType type) throws FindException;

    /**
     * Deletes the policy with the specified OID.  An impact analysis will be performed prior to deleting the policy,
     * and if any other entity references the policy, {@link PolicyDeletionForbiddenException} will be thrown.
     * @param oid the OID of the policy to be deleted.
     * @throws PolicyDeletionForbiddenException if the Policy with the specified OID cannot be deleted because it is
     *         referenced by another entity (e.g. a {@link com.l7tech.service.PublishedService})
     */
    @Secured(stereotype=DELETE_BY_ID)
    void deletePolicy(long oid) throws PolicyDeletionForbiddenException, DeleteException, FindException;

    /**
     * Saves or updates the specified policy.
     * @param policy the policy to be saved.
     * @return the OID of the policy that was saved.
     * @throws PolicyAssertionException if there is a problem with the policy
     */
    @Secured(stereotype=SAVE_OR_UPDATE)
    long savePolicy(Policy policy) throws PolicyAssertionException, SaveException;

    @Secured(stereotype = MethodStereotype.FIND_HEADERS)
    Set<Policy> findUsages(long oid) throws FindException;
}
