package com.l7tech.server.identity.external;

import com.l7tech.identity.external.VirtualPolicyGroup;
import com.l7tech.identity.external.VirtualPolicyUser;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.identity.HasDefaultRole;

/**
 * Non-listable identity provider that authenticates users by executing a policy.
 */
public interface PolicyBackedIdentityProvider extends AuthenticatingIdentityProvider<VirtualPolicyUser, VirtualPolicyGroup, PolicyBackedUserManager, PolicyBackedGroupManager>, ConfigurableIdentityProvider, HasDefaultRole {
}
