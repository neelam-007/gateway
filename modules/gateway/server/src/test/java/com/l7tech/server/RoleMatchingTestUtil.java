package com.l7tech.server;

import com.l7tech.gateway.common.security.rbac.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import org.mockito.ArgumentMatcher;

import java.util.Set;

/**
 * Convenience class for unit tests that require argument matching for Roles.
 */
public final class RoleMatchingTestUtil {
    public static ArgumentMatcher<Role> canReadAllAssertions() {
        return new CanReadAllAssertions();
    }

    public static ArgumentMatcher<Role> canDebugPolicy(Goid policyGoid) {
        return new CanDebugPolicy(policyGoid);
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

    private static class CanDebugPolicy extends ArgumentMatcher<Role> {
        private final Goid policyGoid;

        public CanDebugPolicy(Goid policyGoid) {
            this.policyGoid = policyGoid;
        }

        @Override
        public boolean matches(final Object o) {
            boolean match = false;
            if (o instanceof Role) {
                final Role role = (Role) o;
                for (final Permission permission : role.getPermissions()) {
                    if (permission.getOperation() == OperationType.OTHER &&
                        permission.getEntityType() == EntityType.POLICY &&
                        isScopeMatching(permission.getScope())) {
                        match = true;
                    }
                }
            }
            return match;
        }

        private boolean isScopeMatching(Set<ScopePredicate> scope) {
            boolean match = false;

            if (scope.size() == 1) {
                ScopePredicate theScope = scope.iterator().next();
                if (theScope instanceof ObjectIdentityPredicate) {
                    match = ((ObjectIdentityPredicate) theScope).getTargetEntityId().equals(policyGoid.toString());
                }
            }
            return match;
        }
    }
}
