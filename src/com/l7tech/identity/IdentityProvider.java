package com.l7tech.identity;

import com.l7tech.credential.PrincipalCredentials;

/**
 * @author alex
 */
public interface IdentityProvider {
    void initialize( IdentityProviderConfig config );
    IdentityProviderConfig getConfig();
    UserManager getUserManager();
    GroupManager getGroupManager();
    boolean authenticate( PrincipalCredentials pc );
    /**
     * If true, the save, update and delete methods wont be supported on the usermanager and groupmanager objects
     */
    boolean isReadOnly();
}
