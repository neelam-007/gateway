package com.l7tech.server.policy;

import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Interface implemented by utility that can check policies to see if any assertion is used for which the current admin user
 * does not have CREATE permission on the corresponding AssertionAccess virtual entity.
 */
public interface PolicyAssertionRbacChecker {
    /**
     * Check if the current admin user is permitted to save the specified policy.
     * <p/>
     * If this method returns normally, then no assertions are present in the policy that the specified admin
     * user is not permitted to use.
     *
     * @param policy the policy to examine.  If null, this method takes no action.
     * @throws IOException if the policy contains nonempty policy XML that cannot be parsed.
     * @throws com.l7tech.objectmodel.FindException if there is a problem loading permissions from the database.
     * @throws com.l7tech.gateway.common.security.rbac.PermissionDeniedException if at least one assertion is present in the policy for which the specified
     *                                   admin user lacks CREATE permission for the corresponding AssertionAccess virtual entity.
     */
    void checkPolicy(@Nullable Policy policy) throws FindException, PermissionDeniedException, IOException;
}
