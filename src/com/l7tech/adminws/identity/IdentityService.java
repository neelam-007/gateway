package com.l7tech.adminws.identity;

import com.l7tech.objectmodel.*;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.identity.Group;

import java.rmi.RemoteException;
import java.rmi.Remote;
import java.security.cert.CertificateEncodingException;

/**
 * Class IdentityService.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public interface IdentityService  extends Remote {
    String echoVersion() throws RemoteException;

    EntityHeader[] findAllIdentityProviderConfig() throws RemoteException, FindException;

    EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize)
                                throws RemoteException, FindException;

    IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws RemoteException, FindException;

    long saveIdentityProviderConfig(IdentityProviderConfig cfg)
                                throws RemoteException, SaveException, UpdateException;

    void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException;

    EntityHeader[] findAllUsers(long idProvCfgId) throws RemoteException, FindException;

    EntityHeader[] findAllUsersByOffset(long idProvCfgId, int offset, int windowSize)
                                throws RemoteException, FindException;

    EntityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern)
                                throws RemoteException, FindException;

    User findUserByPrimaryKey(long idProvCfgId, String userId)
                                throws RemoteException, FindException;

    void deleteUser(long idProvCfgId, String userId) throws RemoteException, DeleteException;

    long saveUser(long idProvCfgId, User user) throws RemoteException, SaveException, UpdateException;

    EntityHeader[] findAllGroups(long idProvCfgId) throws RemoteException, FindException;

    EntityHeader[] findAllGroupsByOffset(long idProvCfgId, int offset, int windowSize)
                                throws RemoteException, FindException;

    Group findGroupByPrimaryKey(long idProvCfgId, String groupId)
                                throws RemoteException, FindException;

    void deleteGroup(long idProvCfgId, String groupId) throws RemoteException, DeleteException;

    long saveGroup(long idProvCfgId, Group group)
                                throws RemoteException, SaveException, UpdateException;

    String getUserCert(long idProvCfgId, String userId)
                                throws RemoteException, FindException, CertificateEncodingException;

    void revokeCert(long idProvCfgId, String userId) throws RemoteException, UpdateException;

    void testIdProviderConfig(IdentityProviderConfig cfg)
                                throws RemoteException, InvalidIdProviderCfgException;
}
