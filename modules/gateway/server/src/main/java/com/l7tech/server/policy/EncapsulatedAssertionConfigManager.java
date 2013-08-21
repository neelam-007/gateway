package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager for EncapsulatedAssertionConfig.
 */
public interface EncapsulatedAssertionConfigManager extends EntityManager<EncapsulatedAssertionConfig,GuidEntityHeader>,GuidBasedEntityManager<EncapsulatedAssertionConfig> {
    /**
     * Find all active/enabled encapsulated assertion configs that reference the specified policy OID as the
     * backing policy.
     *
     * @param policyGoid the policy GOID to check.
     * @return a collection of encapsulated assertion configs that reference this policy OID.  May be empty but never null.
     * @throws FindException if there is a problem accessing the database
     */
    @NotNull
    Collection<EncapsulatedAssertionConfig> findByPolicyGoid(Goid policyGoid) throws FindException;
}
