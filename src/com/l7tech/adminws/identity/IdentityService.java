package com.l7tech.adminws.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.identity.IdentityProviderConfig;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * Class IdentityService.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public interface IdentityService  extends Remote {
    String echoVersion() throws RemoteException;

    EntityHeader[] findAllIdentityProviderConfig() throws RemoteException;

    EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws RemoteException;

    IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws RemoteException;

    long saveIdentityProviderConfig(IdentityProviderConfig identityProviderConfig) throws RemoteException;

    void deleteIdentityProviderConfig(long oid) throws RemoteException;

    EntityHeader[] findAllUsers(long identityProviderConfigId) throws RemoteException;

    EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws RemoteException;

    EntityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern) throws RemoteException;

    com.l7tech.identity.User findUserByPrimaryKey(long identityProviderConfigId, String userId) throws RemoteException;

    void deleteUser(long identityProviderConfigId, String userId) throws RemoteException;

    long saveUser(long identityProviderConfigId, com.l7tech.identity.User user) throws RemoteException;

    EntityHeader[] findAllGroups(long identityProviderConfigId) throws RemoteException;

    EntityHeader[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws RemoteException;

    com.l7tech.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId) throws RemoteException;

    void deleteGroup(long identityProviderConfigId, String groupId) throws RemoteException;

    long saveGroup(long identityProviderConfigId, com.l7tech.identity.Group group) throws RemoteException;

    String getUserCert(long identityProviderConfigId, String userId) throws RemoteException;

    void revokeCert(long identityProviderConfigId, String userId) throws RemoteException;

    void testIdProviderConfig(IdentityProviderConfig identityProviderConfig) throws RemoteException;
}
