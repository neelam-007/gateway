package com.l7tech.identity;

import com.l7tech.identity.internal.*;

/**
 * @author alex
 */
public interface IdentityProvider {
    void initialize( IdentityProviderConfig config );
    UserManager getUserManager();
    GroupManager getGroupManager();
    boolean authenticate( User user, Object credential );
}
