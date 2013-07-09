package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.objectmodel.EntityType;
import org.mockito.ArgumentMatcher;

/**
 * Convenience class for unit tests that require argument matching for Roles.
 */
public final class RoleMatchingTestUtil {
    public static ArgumentMatcher<Role> canReadAllAssertions() {
        return new CanReadAllAssertions();
    }

    private static class CanReadAllAssertions extends ArgumentMatcher<Role> {
        @Override
        public boolean matches(final Object o) {
            boolean match = false;
            if (o instanceof Role) {
                final Role role = (Role) o;
                for (final Permission permission : role.getPermissions()) {
                    if (permission.getOperation() == OperationType.READ && permission.getEntityType() == EntityType.ASSERTION_ACCESS && permission.getScope().isEmpty()) {
                        match = true;
                    }
                }
            }
            return match;
        }
    }
}
