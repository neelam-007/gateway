package com.l7tech.adminws.service;

import com.l7tech.identity.IdentityProviderConfig;

import java.util.Collection;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 12, 2003
 *
 * Admin Web service for all that has to do with identities
 */
public interface Identity {
    // identity provider config
    public Header[] findAlllIdentityProviderConfig();
    public Header[] findAllIdentityProviderConfig(int offset, int windowSize);
    public IdentityProviderConfig findIdentityProviderConfigByPrimaryKey( long oid );
    public long saveIdentityProviderConfig( IdentityProviderConfig identityProviderConfig );
    public void deleteIdentityProviderConfig(long oid);
    // user manager
    public User findUserByPrimaryKey(long identityProviderConfigId, long userId);
    public void deleteUser(long identityProviderConfigId, long userId);
    public long saveUser(long identityProviderConfigId, User user);
    public Header[] findAllUsers(long identityProviderConfigId);
    public Header[] findAllUsers(long identityProviderConfigId, int offset, int windowSize);
    // group manager
    public Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId);
    public void deleteGroup(long identityProviderConfigId, long groupId);
    public long saveGroup(long identityProviderConfigId, Group group);
    public Header[] findAllGroups(long identityProviderConfigId);
    public Header[] findAllGroups(long identityProviderConfigId, int offset, int windowSize);
}
