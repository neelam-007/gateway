package com.l7tech.adminws.identity;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;

import java.rmi.RemoteException;
import java.rmi.Remote;

/**
 * Class IdentityService.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a> 
 */
public interface IdentityService  extends Remote {
    String echoVersion() throws java.rmi.RemoteException;

    EntityHeader[] findAllIdentityProviderConfig() throws java.rmi.RemoteException;

    EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException;

    com.l7tech.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException;

    long saveIdentityProviderConfig(com.l7tech.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException;

    void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException;

    EntityHeader[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException;

    EntityHeader[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException;

    EntityHeader[] searchIdentities(long identityProviderConfigId, EntityType[] types, String pattern) throws java.rmi.RemoteException;

    com.l7tech.identity.User findUserByPrimaryKey(long identityProviderConfigId, String userId) throws java.rmi.RemoteException;

    void deleteUser(long identityProviderConfigId, String userId) throws java.rmi.RemoteException;

    long saveUser(long identityProviderConfigId, com.l7tech.identity.User user) throws java.rmi.RemoteException;

    EntityHeader[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException;

    EntityHeader[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException;

    com.l7tech.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException;

    void deleteGroup(long identityProviderConfigId, String groupId) throws java.rmi.RemoteException;

    long saveGroup(long identityProviderConfigId, com.l7tech.identity.Group group) throws RemoteException;

    String getUserCert(long identityProviderConfigId, String userId) throws RemoteException;

    void revokeCert(long identityProviderConfigId, String userId) throws RemoteException;
}
