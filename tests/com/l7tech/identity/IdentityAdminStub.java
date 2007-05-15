/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.service.PublishedService;

import java.rmi.RemoteException;
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
        Map<Long, IdentityProviderConfig> configs = StubDataStore.defaultStore().getIdentityProviderConfigs();
        List<EntityHeader> headers = new ArrayList<EntityHeader>();
        for (Long key : configs.keySet()) {
            IdentityProviderConfig config = configs.get(key);
            headers.add(fromIdentityProviderConfig(config));
        }
        return headers.toArray(new EntityHeader[0]);
    }

    public IdentityProviderConfig findIdentityProviderConfigByID(long oid) throws RemoteException, FindException {
        return StubDataStore.defaultStore().getIdentityProviderConfigs().get(new Long(oid));
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws RemoteException, FindException {
        return new LdapConfigTemplateManager().getTemplates();
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig config) throws RemoteException, SaveException, UpdateException {
        long oid = config.getOid();
        if (oid == 0 || oid == PersistentEntity.DEFAULT_OID) {
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

    public IdentityHeader[] findAllUsers(long idProvCfgId) throws RemoteException, FindException {
        Map<String, PersistentUser> users = StubDataStore.defaultStore().getUsers();
        ArrayList<IdentityHeader> results = new ArrayList<IdentityHeader>();
        for (String s : users.keySet()) {
            User u = users.get(s);
            results.add(fromUser(u));
        }
        return results.toArray(new IdentityHeader[0]);
    }

    public IdentityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern) throws RemoteException, FindException {
        ArrayList<IdentityHeader> results = new ArrayList<IdentityHeader>();
        for (EntityType type : types) {
            if (type == EntityType.USER)
                results.addAll(Arrays.asList(findAllUsers(idProvCfgId)));
            else if (type == EntityType.GROUP)
                results.addAll(Arrays.asList(findAllGroups(idProvCfgId)));
        }
        return results.toArray(new IdentityHeader[0]);
    }

    public User findUserByID(long idProvCfgId, String userId) throws RemoteException, FindException {
        return StubDataStore.defaultStore().getUsers().get(userId);
    }

    public User findUserByLogin(long idProvCfgId, String login) throws RemoteException, FindException {
        if (login == null) return null;
        Map<String, PersistentUser> users = StubDataStore.defaultStore().getUsers();
        for (String uid : users.keySet()) {
            User u = users.get(uid);
            if (login.equals(u.getLogin())) return u;
        }
        return null;
    }

    public void deleteUser(long idProvCfgId, String userId) throws RemoteException, DeleteException, ObjectNotFoundException {
        Map users = StubDataStore.defaultStore().getUsers();
        users.remove(userId);
    }

    public String saveUser(long idProvCfgId, User user, Set<IdentityHeader> groupHeaders) throws RemoteException, SaveException, UpdateException, ObjectNotFoundException {
        if (!(user instanceof PersistentUser)) throw new IllegalArgumentException("IdentityAdminStub only supports Internal and Federated users");
        PersistentUser pu = (PersistentUser) user;
        final StubDataStore store = StubDataStore.defaultStore();
        if (pu.getId() == null)
            pu.setOid(store.nextObjectId());
        pu.setProviderId(idProvCfgId);
        store.getUsers().put(pu.getId(), pu);
        if (groupHeaders != null) {
            // Clear existing memberships
            for (Iterator i = store.getGroupMemberships().iterator(); i.hasNext();) {
                GroupMembership gm = (GroupMembership)i.next();
                if (pu.getId().equals(gm.getMemberUserId())) i.remove();
            }

            // Set new memberships
            for (IdentityHeader header : groupHeaders) {
                GroupMembership mem = InternalGroupMembership.newInternalMembership(header.getOid(), pu.getOid());
                store.getGroupMemberships().add(mem);
            }
        }
        return pu.getId();
    }

    public IdentityHeader[] findAllGroups(long idProvCfgId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map<String, PersistentGroup> groups = store.getGroups();
        List<IdentityHeader> results = new ArrayList<IdentityHeader>();
        for (String gid : groups.keySet()) {
            Group g = groups.get(gid);
            results.add(fromGroup(g));
        }
        return (IdentityHeader[])results.toArray(new EntityHeader[0]);
    }

    public Group findGroupByID(long idProvCfgId, String groupId) throws RemoteException, FindException {
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
        if (!(group instanceof PersistentGroup)) throw new IllegalArgumentException("IdentityAdminStub only supports Internal and Federated groups");
        PersistentGroup pg = (PersistentGroup) group;
        final StubDataStore store = StubDataStore.defaultStore();
        Map<String, PersistentGroup> groups = store.getGroups();
        if (pg.getId() == null || pg.getId().equals(Long.toString(PersistentEntity.DEFAULT_OID))) {
            pg.setOid(store.nextObjectId());
        }
        groups.put(pg.getId(), pg);
        return pg.getId();
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

    public Set<IdentityHeader> getGroupHeaders(long providerId, String userId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Set<GroupMembership> memberships = store.getGroupMemberships();
        Set<IdentityHeader> results = new HashSet<IdentityHeader>();
        for (GroupMembership gm : memberships) {
            if (userId.equals(gm.getMemberUserId())) {
                Group g = store.getGroups().get(gm.getThisGroupId());
                results.add(fromGroup(g));
            }
        }
        return results;
    }

    public Set<IdentityHeader> getUserHeaders(long providerId, String groupId) throws RemoteException, FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Set<GroupMembership> memberships = store.getGroupMemberships();
        Set<IdentityHeader> results = new HashSet<IdentityHeader>();
        for (GroupMembership gm : memberships) {
            if (groupId.equals(gm.getThisGroupId())) {
                User u = store.getUsers().get(gm.getMemberUserId());
                results.add(fromUser(u));
            }
        }
        return results;
    }


    /**
     * User to header
     *
     * @param u the user to get the header for
     * @return the corresponding header
     */
    public static IdentityHeader fromUser(User u) {
        if (u == null) {
            throw new IllegalArgumentException();
        }
        return new IdentityHeader(u.getProviderId(), u.getId(), EntityType.USER, u.getLogin(), u.getName());
    }

    /**
     * Group to header
     *
     * @param g the group to get the header for
     * @return the corresponding header
     */
    static IdentityHeader fromGroup(Group g) {
        if (g == null) {
            throw new IllegalArgumentException();
        }
        return new IdentityHeader(g.getProviderId(), g.getId(), EntityType.GROUP, g.getName(), g.getDescription());
    }

    /**
     * Service to header
     *
     * @param s the service to get the header for
     * @return the corresponding header
     */
    static EntityHeader fromService(PublishedService s) {
        if (s == null) {
            throw new IllegalArgumentException();
        }
        return new EntityHeader(Long.toString(s.getOid()), EntityType.SERVICE, s.getName(), "");
    }

    static EntityHeader fromIdentityProviderConfig(IdentityProviderConfig config) {
        EntityHeader out = new EntityHeader();
        out.setDescription(config.getDescription());
        out.setName(config.getName());
        out.setOid(config.getOid());
        out.setType(EntityType.ID_PROVIDER_CONFIG);
        return out;
    }

}
