package com.l7tech.identity;

import com.l7tech.credential.PrincipalCredentials;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import java.util.Collection;

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
    /**
     * searches for users and groups whose name match the pattern described in searchString
     * pattern may include wildcard such as * character
     */
    Collection search(EntityType[] types, String searchString) throws FindException;
}
