/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.internal.GroupMembership;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.*;

/**
 * TODO
 *
 * @author alex
 * @version $Revision$
 */
public class IdentityAdminStub implements IdentityAdmin {
    public String echoVersion() throws RemoteException {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    public EntityHeader[] findAllIdentityProviderConfig() throws RemoteException, FindException {
        Map configs = StubDataStore.defaultStore().getIdentityProviderConfigs();
        List headers = new ArrayList();
        for (Iterator i = configs.keySet().iterator(); i.hasNext();) {
            Object key = i.next();
            IdentityProviderConfig config = (IdentityProviderConfig)configs.get(key);
            headers.add(EntityHeader.fromIdentityProviderConfig(config));
        }
        return (EntityHeader[])headers.toArray(new EntityHeader[0]);
    }

    public EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize) throws RemoteException, FindException {
        return findAllIdentityProviderConfig();
    }

    public IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws RemoteException, FindException {
        return (IdentityProviderConfig)StubDataStore.defaultStore().getIdentityProviderConfigs().get(new Long(oid));
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws RemoteException, FindException {
        return new LdapConfigTemplateManager().getTemplates();
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig config) throws RemoteException, SaveException, UpdateException {
        long oid = config.getOid();
        if (oid == 0 || oid == Entity.DEFAULT_OID) {
            oid = StubDataStore.defaultStore().nextObjectId();
        }
        config.setOid(oid);
        Long key = new Long(oid);
        StubDataStore.defaultStore().getIdentityProviderConfigs().put(key, config);
        return oid;
    }

    public void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException {
        StubDataStore.defaultStore().getIdentityProviderConfigs().remove(new Long(oid));
    }

    public EntityHeader[] findAllUsers(long idProvCfgId) throws RemoteException, FindException {
        Map users = StubDataStore.defaultStore().getUsers();
        ArrayList results = new ArrayList();
        for (Iterator i = users.keySet().iterator(); i.hasNext();) {
            String uid = (String)i.next();
            User u = (User)users.get(uid);
            results.add(EntityHeader.fromUser(u));
        }
        return (EntityHeader[])results.toArray(new EntityHeader[0]);
    }

    public EntityHeader[] findAllUsersByOffset(long idProvCfgId, int offset, int windowSize) throws RemoteException, FindException {
        return findAllUsers(idProvCfgId);
    }

    public EntityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern) throws RemoteException, FindException {
        ArrayList results = new ArrayList();
        for (int i = 0; i < types.length; i++) {
            EntityType type = types[i];
            if (type == EntityType.USER)
                results.addAll(Arrays.asList(findAllUsers(idProvCfgId)));
            else if (type == EntityType.GROUP)
                results.addAll(Arrays.asList(findAllGroups(idProvCfgId)));
        }
        return (EntityHeader[])results.toArray(new EntityHeader[0]);
    }

    public User findUserByPrimaryKey(long idProvCfgId, String userId) throws RemoteException, FindException {
        return (User)StubDataStore.defaultStore().getUsers().get(userId);
    }

    public User findUserByLogin(long idProvCfgId, String login) throws RemoteException, FindException {
        if (login == null) return null;
        Map users = StubDataStore.defaultStore().getUsers();
        for (Iterator i = users.keySet().iterator(); i.hasNext();) {
            String uid = (String)i.next();
            User u = (User)users.get(uid);
            if (login.equals(u.getLogin())) return u;
        }
        return null;
    }

    public void deleteUser(long idProvCfgId, String userId) throws RemoteException, DeleteException, ObjectNotFoundException {
        Map users = StubDataStore.defaultStore().getUsers();
        users.remove(userId);
    }

    public String saveUser(long idProvCfgId, User user, Set groupHeaders) throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        final StubDataStore store = StubDataStore.defaultStore();
        if (user.getUniqueIdentifier() == null)
            user.getUserBean().setUniqueIdentifier(Long.toString(store.nextObjectId()));
        user.setProviderId(idProvCfgId);
        store.getUsers().put(user.getUniqueIdentifier(), user);
        if (groupHeaders != null) {
            // Clear existing memberships
            for (Iterator i = store.getGroupMemberships().iterator(); i.hasNext();) {
                GroupMembership gm = (GroupMembership)i.next();
                if (user.getUniqueIdentifier().equals(Long.toString(gm.getUserOid()))) i.remove();
            }

            // Set new memberships
            for (Iterator i = groupHeaders.iterator(); i.hasNext();) {
                EntityHeader header = (EntityHeader)i.next();
                GroupMembership mem = new GroupMembership(Long.parseLong(user.getUniqueIdentifier()), header.getOid());
                store.getGroupMemberships().add(mem);
            }
        }
        return user.getUniqueIdentifier();
    }

    public EntityHeader[] findAllGroups(long idProvCfgId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = store.getGroups();
        List results = new ArrayList();
        for (Iterator i = groups.keySet().iterator(); i.hasNext();) {
            String gid = (String)i.next();
            Group g = (Group)groups.get(gid);
            results.add(EntityHeader.fromGroup(g));
        }
        return (EntityHeader[])results.toArray(new EntityHeader[0]);
    }

    public EntityHeader[] findAllGroupsByOffset(long idProvCfgId, int offset, int windowSize) throws RemoteException, FindException {
        return findAllGroups(idProvCfgId);
    }

    public Group findGroupByPrimaryKey(long idProvCfgId, String groupId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = store.getGroups();
        return (Group)groups.get(groupId);
    }

    public void deleteGroup(long idProvCfgId, String groupId) throws RemoteException, DeleteException, ObjectNotFoundException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = store.getGroups();
        groups.remove(groupId);
    }

    public String saveGroup(long idProvCfgId, Group group, Set userHeaders) throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = store.getGroups();
        if (group.getUniqueIdentifier() == null || group.getUniqueIdentifier().equals(Long.toString(Entity.DEFAULT_OID))) {
            group.getGroupBean().setUniqueIdentifier(Long.toString(store.nextObjectId()));
        }
        groups.put(group.getUniqueIdentifier(), group);
        return group.getUniqueIdentifier();
    }

    public String getUserCert(User user) throws RemoteException, FindException, CertificateEncodingException {
        return null; // TODO ?
    }

    public void revokeCert(User user) throws RemoteException, UpdateException, ObjectNotFoundException {
    }

    public void recordNewUserCert(User user, Certificate cert) throws RemoteException, UpdateException {
    }

    public void testIdProviderConfig(IdentityProviderConfig cfg) throws RemoteException, InvalidIdProviderCfgException {
    }

    public Set getGroupHeaders(long providerId, String userId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Set memberships = store.getGroupMemberships();
        Set results = new HashSet();
        for (Iterator i = memberships.iterator(); i.hasNext();) {
            GroupMembership gm = (GroupMembership)i.next();
            if (userId.equals(Long.toString(gm.getUserOid()))) {
                Group g = (Group)store.getGroups().get(Long.toString(gm.getGroupOid()));
                results.add(EntityHeader.fromGroup(g));
            }
        }
        return results;
    }

    public Set getUserHeaders(long providerId, String groupId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Set memberships = store.getGroupMemberships();
        Set results = new HashSet();
        for (Iterator i = memberships.iterator(); i.hasNext();) {
            GroupMembership gm = (GroupMembership)i.next();
            if (groupId.equals(Long.toString(gm.getGroupOid()))) {
                User u = (User)store.getUsers().get(Long.toString(gm.getUserOid()));
                results.add(EntityHeader.fromUser(u));
            }
        }
        return results;
    }

    /**
     * Determine the roles for the given subject.
     *
     * @param subject the subject for which to get roles for
     * @return the <code>Set</code> of roles (groups) the subject is memeber of
     * @throws java.rmi.RemoteException on remote invocation error
     * @throws java.security.AccessControlException
     *                                  if the current subject is not allowed to perform the operation.
     *                                  The invocation tests whether the current subject  (the subject carrying out the operaton)
     *                                  has privileges to perform the operation. The operators are not allowed to perform this operation
     *                                  except for themselves.
     */
    public Set getRoles(Subject subject) throws RemoteException, AccessControlException {
        Set roles = new HashSet();
        roles.add(Group.ADMIN_GROUP_NAME);
        roles.add(Group.OPERATOR_GROUP_NAME);
        return roles;
    }
}
