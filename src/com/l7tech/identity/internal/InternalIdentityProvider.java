package com.l7tech.identity.internal;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.objectmodel.PersistenceManager;

/**
 * @author alex
 */
public class InternalIdentityProvider implements IdentityProvider {
    public InternalIdentityProvider() {
    }

    public void initialize( IdentityProviderConfig config ) {
        _identityProviderConfig = config;
        _userManager = PersistenceManager.getEntityManager( User.class );
        _groupManager = PersistenceManager.getEntityManager( Group.class );
        _userManager.setIdentityProviderOid( config.getOid() );
        _groupManager.setIdentityProviderOid( config.getOid() );
    }

    public UserManager getUserManager() {
        return _userManager;
    }

    public GroupManager getGroupManager() {
        return _groupManager;
    }

    public boolean authenticate( User user ) {
        return false;
    }

    private IdentityProviderConfig _identityProviderConfig;
    private UserManager _userManager;
    private GroupManager _groupManager;
}
