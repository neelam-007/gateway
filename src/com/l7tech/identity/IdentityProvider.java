package com.l7tech.identity;

import com.l7tech.identity.internal.*;

/**
 * @author alex
 */
public interface IdentityProvider {
    void initialize( IdentityProviderConfig config );
    IdentityProviderConfig getConfig();
    UserManager getUserManager();
    GroupManager getGroupManager();
    boolean authenticate( User user, Object credential );
}
