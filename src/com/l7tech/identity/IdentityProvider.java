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
    /**
     * If true, the save, update and delete methods wont be supported on the usermanager and groupmanager objects
     */
    boolean isReadOnly();
}
