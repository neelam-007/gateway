package com.l7tech.identity;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.io.IOException;
import java.util.Collection;

/**
 * @author alex
 */
public interface IdentityProvider {
    IdentityProviderConfig getConfig();
    UserManager getUserManager();
    GroupManager getGroupManager();

    /**
     * @return a {@link User}. Will never be null.
     */
    User authenticate( LoginCredentials pc ) throws AuthenticationException, FindException, IOException;

    /**
     * If true, the save, update and delete methods wont be supported on the usermanager and groupmanager objects
     */
    boolean isReadOnly();

    /**
     * searches for users and groups whose name match the pattern described in searchString
     * pattern may include wildcard such as * character
     */
    Collection search(EntityType[] types, String searchString) throws FindException;

    String getAuthRealm();

    void test() throws InvalidIdProviderCfgException;
}
