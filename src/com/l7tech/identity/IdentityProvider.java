package com.l7tech.identity;

import java.security.Principal;

/**
 * @author alex
 */
public interface IdentityProvider {
    void initialize( IdentityProviderConfig config );
    IdentityProviderConfig getConfig();
    UserManager getUserManager();
    GroupManager getGroupManager();
    boolean authenticate( Principal user, byte[] credentials );
}
