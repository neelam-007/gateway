package com.l7tech.adminws.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.*;
import com.l7tech.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;

import java.rmi.RemoteException;
import java.io.IOException;

import net.jini.config.ConfigurationException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 * Admin WS for identities (provider configs, users, groups)
 */
public class IdentityServiceImpl extends RemoteService implements IdentityService {
    public IdentityServiceImpl(String[] configOptions, LifeCycle lc)
      throws ConfigurationException, IOException {
        super(configOptions, lc);
        delegate = new Service();
    }
    /**
     * Returns a version string. This can be compared to version on client-side.
     * @return value to be compared with the client side value of Service.VERSION;
     * @throws RemoteException
     */
    public String echoVersion() throws RemoteException {
        return delegate.echoVersion();
    }
    /**
     *
     * @return Array of entity headers for all existing id provider config
     * @throws RemoteException
     */
    public EntityHeader[] findAllIdentityProviderConfig() throws RemoteException {
        return delegate.findAllIdentityProviderConfig();
    }
    /**
     *
     * @return Array of entity headers for all existing id provider config
     * @throws RemoteException
     */
    public EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws RemoteException {
        return delegate.findAllIdentityProviderConfigByOffset(offset, windowSize);
    }
    /**
     *
     * @return An identity provider config object
     * @throws RemoteException
     */
    public IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws RemoteException {
        return delegate.findIdentityProviderConfigByPrimaryKey(oid);
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig) throws RemoteException {
        return delegate.saveIdentityProviderConfig(identityProviderConfig);
    }

    public void deleteIdentityProviderConfig(long oid) throws RemoteException {
        delegate.deleteIdentityProviderConfig(oid);
    }

    public EntityHeader[] findAllUsers(long identityProviderConfigId) throws RemoteException {
        return delegate.findAllUsers(identityProviderConfigId);
    }

    public EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws RemoteException {
        return delegate.findAllUsersByOffset(identityProviderConfigId, offset, windowSize);
    }

    public EntityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern) throws RemoteException {
        return delegate.searchIdentities(identityProviderConfigId, types, pattern);
    }

    public User findUserByPrimaryKey(long identityProviderConfigId, String userId) throws RemoteException {
        return delegate.findUserByPrimaryKey(identityProviderConfigId, userId);
    }

    public void deleteUser(long identityProviderConfigId, String userId) throws RemoteException {
        delegate.deleteUser(identityProviderConfigId, userId);
    }

    public long saveUser(long identityProviderConfigId, User user) throws RemoteException {
        return delegate.saveUser(identityProviderConfigId, user);
    }


    public EntityHeader[] findAllGroups(long identityProviderConfigId) throws RemoteException {
        return delegate.findAllGroups(identityProviderConfigId);
    }

    public EntityHeader[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws RemoteException {
        return delegate.findAllGroupsByOffset(identityProviderConfigId, offset, windowSize);
    }

    public Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId) throws RemoteException {
        return delegate.findGroupByPrimaryKey(identityProviderConfigId, groupId);
    }

    public void deleteGroup(long identityProviderConfigId, String groupId) throws RemoteException {
        delegate.deleteGroup(identityProviderConfigId, groupId);
    }

    public long saveGroup(long identityProviderConfigId, Group group) throws RemoteException {
        return delegate.saveGroup(identityProviderConfigId, group);
    }

    public String getUserCert(long identityProviderConfigId, String userId) throws RemoteException {
        return delegate.getUserCert(identityProviderConfigId, userId);
    }

    public void revokeCert(long identityProviderConfigId, String userId) throws RemoteException {
        delegate.revokeCert(identityProviderConfigId, userId);
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityService delegate = null;
}
