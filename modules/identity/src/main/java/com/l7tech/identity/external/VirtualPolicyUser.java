package com.l7tech.identity.external;

import com.l7tech.identity.UserBean;
import com.l7tech.objectmodel.Goid;

/**
 * Represents a user authenticated by a PolicyBackedIdentityProvider.
 */
public class VirtualPolicyUser extends UserBean {
    public VirtualPolicyUser(Goid providerId, String login) {
        super(providerId, login);
        setUniqueIdentifier(login);
    }
}
