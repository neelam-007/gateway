package com.l7tech.identity.internal.imp;

import com.l7tech.identity.*;
import com.l7tech.objectmodel.PersistenceManager;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 14, 2003
 *
 */
public class InternalIdentityProviderImp implements IdentityProvider {
    public InternalIdentityProviderImp() {
    }

    public void initialize( IdentityProviderConfig config ) {
        _identityProviderConfig = config;
        _userManager = (UserManager)PersistenceManager.getEntityManager( User.class );
        _groupManager = (GroupManager)PersistenceManager.getEntityManager( Group.class );
        _userManager.setIdentityProviderOid( config.getOid() );
        _groupManager.setIdentityProviderOid( config.getOid() );
    }

    public UserManager getUserManager() {
        return _userManager;
    }

    public GroupManager getGroupManager() {
        return _groupManager;
    }

    public boolean authenticate(User user, Object credential) {
        return false;
    }

    private IdentityProviderConfig _identityProviderConfig;
    private UserManager _userManager;
    private GroupManager _groupManager;
}
