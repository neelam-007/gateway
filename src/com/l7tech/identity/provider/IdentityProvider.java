package com.l7tech.identity.provider;

import com.l7tech.identity.provider.IdentityProviderConfig;
import com.l7tech.identity.provider.internal.*;

/**
 * @author alex
 */
public interface IdentityProvider {
    void initialize( IdentityProviderConfig config );
    UserManager getUserManager();
    GroupManager getGroupManager();
    boolean authenticate( User user );
}
