package com.l7tech.identity.rmi;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.*;
import com.l7tech.jini.export.RemoteService;
import com.sun.jini.start.LifeCycle;

import java.rmi.RemoteException;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;

import net.jini.config.ConfigurationException;

/**
 * Layer 7 Technologies, inc.
 * User: flascelles
 * Date: May 26, 2003
 *
 * RMI implementation of the IdentityAdmin
 */
public class IdentityAdminImpl extends RemoteService implements IdentityAdmin {
    public IdentityAdminImpl(String[] configOptions, LifeCycle lc)
      throws ConfigurationException, IOException {
        super(configOptions, lc);
        delegate = new com.l7tech.identity.ws.IdentityAdminImpl();
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
    public EntityHeader[] findAllIdentityProviderConfig() throws RemoteException, FindException {
        return delegate.findAllIdentityProviderConfig();
    }
    /**
     *
     * @return Array of entity headers for all existing id provider config
     * @throws RemoteException
     */
    public EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize)
            throws RemoteException, FindException {
        return delegate.findAllIdentityProviderConfigByOffset(offset, windowSize);
    }
    /**
     *
     * @return An identity provider config object
     * @throws RemoteException
     */
    public IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid)
            throws RemoteException, FindException {
        return delegate.findIdentityProviderConfigByPrimaryKey(oid);
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig cfg)
            throws RemoteException, SaveException, UpdateException {
        return delegate.saveIdentityProviderConfig(cfg);
    }

    public void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException {
        delegate.deleteIdentityProviderConfig(oid);
    }

    public EntityHeader[] findAllUsers(long idProvCfgId) throws RemoteException, FindException {
        return delegate.findAllUsers(idProvCfgId);
    }

    public EntityHeader[] findAllUsersByOffset(long idProvCfgId, int offset, int windowSize)
            throws RemoteException, FindException {
        return delegate.findAllUsersByOffset(idProvCfgId, offset, windowSize);
    }

    public EntityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern)
            throws RemoteException, FindException {
        return delegate.searchIdentities(idProvCfgId, types, pattern);
    }

    public User findUserByPrimaryKey(long idProvCfgId, String userId) throws RemoteException, FindException {
        return delegate.findUserByPrimaryKey(idProvCfgId, userId);
    }

    public void deleteUser(long idProvCfgId, String userId) throws RemoteException, DeleteException {
        delegate.deleteUser(idProvCfgId, userId);
    }

    public long saveUser(long idProvCfgId, User user) throws RemoteException, SaveException, UpdateException {
        return delegate.saveUser(idProvCfgId, user);
    }


    public EntityHeader[] findAllGroups(long idProvCfgId) throws RemoteException, FindException {
        return delegate.findAllGroups(idProvCfgId);
    }

    public EntityHeader[] findAllGroupsByOffset(long idProvCfgId, int offset, int windowSize)
            throws RemoteException, FindException {
        return delegate.findAllGroupsByOffset(idProvCfgId, offset, windowSize);
    }

    public Group findGroupByPrimaryKey(long idProvCfgId, String groupId) throws RemoteException, FindException {
        return delegate.findGroupByPrimaryKey(idProvCfgId, groupId);
    }

    public void deleteGroup(long idProvCfgId, String groupId) throws RemoteException, DeleteException {
        delegate.deleteGroup(idProvCfgId, groupId);
    }

    public long saveGroup(long idProvCfgId, Group group) throws RemoteException, SaveException, UpdateException {
        return delegate.saveGroup(idProvCfgId, group);
    }

    public String getUserCert(long idProvCfgId, String userId) throws RemoteException,
                                            FindException, CertificateEncodingException {
        return delegate.getUserCert(idProvCfgId, userId);
    }

    public void revokeCert(long idProvCfgId, String userId) throws RemoteException, UpdateException {
        delegate.revokeCert(idProvCfgId, userId);
    }

    public void testIdProviderConfig(IdentityProviderConfig cfg) throws RemoteException,
                                            InvalidIdProviderCfgException {
        delegate.testIdProviderConfig(cfg);
    }

    // ************************************************
    // PRIVATES
    // ************************************************
    private IdentityAdmin delegate = null;
}
