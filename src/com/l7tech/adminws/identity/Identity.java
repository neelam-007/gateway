/**
 * Identity.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis WSDL2Java emitter.
 */

package com.l7tech.adminws.identity;

public interface Identity extends java.rmi.Remote {
    public com.l7tech.adminws.identity.Header[] findAlllIdentityProviderConfig() throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.Header[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws java.rmi.RemoteException;
    public long saveIdentityProviderConfig(com.l7tech.adminws.identity.IdentityProviderConfig identityProviderConfig) throws java.rmi.RemoteException;
    public void deleteIdentityProviderConfig(long oid) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.User findUserByPrimaryKey(long identityProviderConfigId, long userId) throws java.rmi.RemoteException;
    public void deleteUser(long identityProviderConfigId, long userId) throws java.rmi.RemoteException;
    public long saveUser(long identityProviderConfigId, com.l7tech.adminws.identity.User user) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.Header[] findAllUsers(long identityProviderConfigId) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.Header[] findAllUsersByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.Group findGroupByPrimaryKey(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException;
    public void deleteGroup(long identityProviderConfigId, long groupId) throws java.rmi.RemoteException;
    public long saveGroup(long identityProviderConfigId, com.l7tech.adminws.identity.Group group) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.Header[] findAllGroups(long identityProviderConfigId) throws java.rmi.RemoteException;
    public com.l7tech.adminws.identity.Header[] findAllGroupsByOffset(long identityProviderConfigId, int offset, int windowSize) throws java.rmi.RemoteException;
}
