/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.gateway.common.admin.IdentityAdmin;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.PersistentUser;
import com.l7tech.identity.User;
import com.l7tech.identity.GroupMembership;
import com.l7tech.identity.PersistentGroup;
import com.l7tech.identity.Group;
import com.l7tech.identity.InvalidIdProviderCfgException;
import com.l7tech.objectmodel.*;
import com.l7tech.server.identity.ldap.LdapConfigTemplateManager;
import com.l7tech.gateway.common.service.PublishedService;

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
    public String echoVersion() {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    public EntityHeader[] findAllIdentityProviderConfig() throws FindException {
        Map<Long, IdentityProviderConfig> configs = null;//StubDataStore.defaultStore().getIdentityProviderConfigs();
        List<EntityHeader> headers = new ArrayList<EntityHeader>();
        for (Long key : configs.keySet()) {
            IdentityProviderConfig config = configs.get(key);
            headers.add(fromIdentityProviderConfig(config));
        }
        return headers.toArray(new EntityHeader[0]);
    }

    public IdentityProviderConfig findIdentityProviderConfigByID(long oid) throws FindException {
        return null;//StubDataStore.defaultStore().getIdentityProviderConfigs().get(new Long(oid));
    }

    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        return new LdapConfigTemplateManager().getTemplates();
    }

    public long saveIdentityProviderConfig(IdentityProviderConfig config) throws SaveException, UpdateException {
        long oid = config.getOid();
        if (oid == 0 || oid == PersistentEntity.DEFAULT_OID) {
            oid = 0;//StubDataStore.defaultStore().nextObjectId();
        }
        config.setOid(oid);
        Long key = new Long(oid);
        //StubDataStore.defaultStore().getIdentityProviderConfigs().put(key, config);
        return oid;
    }

    public void deleteIdentityProviderConfig(long oid) throws DeleteException {
        //StubDataStore.defaultStore().getIdentityProviderConfigs().remove(new Long(oid));
    }

    public IdentityHeader[] findAllUsers(long idProvCfgId) throws FindException {
        Map<String, PersistentUser> users = null;//StubDataStore.defaultStore().getUsers();
        ArrayList<IdentityHeader> results = new ArrayList<IdentityHeader>();
        for (String s : users.keySet()) {
            User u = users.get(s);
            results.add(fromUser(u));
        }
        return results.toArray(new IdentityHeader[0]);
    }

    public IdentityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern) throws FindException {
        ArrayList<IdentityHeader> results = new ArrayList<IdentityHeader>();
        for (EntityType type : types) {
            if (type == EntityType.USER)
                results.addAll(Arrays.asList(findAllUsers(idProvCfgId)));
            else if (type == EntityType.GROUP)
                results.addAll(Arrays.asList(findAllGroups(idProvCfgId)));
        }
        return results.toArray(new IdentityHeader[0]);
    }

    public User findUserByID(long idProvCfgId, String userId) throws FindException {
        return null;//StubDataStore.defaultStore().getUsers().get(userId);
    }

    public User findUserByLogin(long idProvCfgId, String login) throws FindException {
        if (login == null) return null;
        Map<String, PersistentUser> users = null;//StubDataStore.defaultStore().getUsers();
        for (String uid : users.keySet()) {
            User u = users.get(uid);
            if (login.equals(u.getLogin())) return u;
        }
        return null;
    }

    public void deleteUser(long idProvCfgId, String userId) throws DeleteException, ObjectNotFoundException {
        Map users = null;//StubDataStore.defaultStore().getUsers();
        users.remove(userId);
    }

    public String saveUser(long idProvCfgId, User user, Set<IdentityHeader> groupHeaders) throws SaveException, UpdateException, ObjectNotFoundException {
        if (!(user instanceof PersistentUser)) throw new IllegalArgumentException("IdentityAdminStub only supports Internal and Federated users");
        PersistentUser pu = (PersistentUser) user;
//        final StubDataStore store = StubDataStore.defaultStore();
//        if (pu.getId() == null)
//            pu.setOid(store.nextObjectId());
        pu.setProviderId(idProvCfgId);
//        store.getUsers().put(pu.getId(), pu);
        if (groupHeaders != null) {
            // Clear existing memberships
//            for (Iterator i = store.getGroupMemberships().iterator(); i.hasNext();) {
//                GroupMembership gm = (GroupMembership)i.next();
//                if (pu.getId().equals(gm.getMemberUserId())) i.remove();
//            }

            // Set new memberships
            for (IdentityHeader header : groupHeaders) {
                GroupMembership mem = InternalGroupMembership.newInternalMembership(header.getOid(), pu.getOid());
//                store.getGroupMemberships().add(mem);
            }
        }
        return pu.getId();
    }

    public IdentityHeader[] findAllGroups(long idProvCfgId) throws FindException {
//        final StubDataStore store = StubDataStore.defaultStore();
        Map<String, PersistentGroup> groups = null;//store.getGroups();
        List<IdentityHeader> results = new ArrayList<IdentityHeader>();
        for (String gid : groups.keySet()) {
            Group g = groups.get(gid);
            results.add(fromGroup(g));
        }
        return results.toArray(new IdentityHeader[0]);
    }

    public Group findGroupByID(long idProvCfgId, String groupId) throws FindException {
//        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = null;//store.getGroups();
        return (Group)groups.get(groupId);
    }

    public Group findGroupByName(long idProvCfgId, String name) throws FindException {
        return null;
    }

    public void deleteGroup(long idProvCfgId, String groupId) throws DeleteException, ObjectNotFoundException {
//        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = null;//store.getGroups();
        groups.remove(groupId);
    }

    public String saveGroup(long idProvCfgId, Group group, Set userHeaders) throws SaveException, UpdateException, ObjectNotFoundException {
        if (!(group instanceof PersistentGroup)) throw new IllegalArgumentException("IdentityAdminStub only supports Internal and Federated groups");
        PersistentGroup pg = (PersistentGroup) group;
//        final StubDataStore store = StubDataStore.defaultStore();
        Map<String, PersistentGroup> groups = null;//store.getGroups();
        if (pg.getId() == null || pg.getId().equals(Long.toString(PersistentEntity.DEFAULT_OID))) {
//            pg.setOid(store.nextObjectId());
        }
        groups.put(pg.getId(), pg);
        return pg.getId();
    }

    public String getUserCert(User user) throws FindException, CertificateEncodingException {
        return null; // TODO ?
    }

    public void revokeCert(User user) throws UpdateException, ObjectNotFoundException {
    }

    public int revokeCertificates() throws UpdateException {
        return 0;
    }

    public void recordNewUserCert(User user, Certificate cert) throws UpdateException {
    }

    public void testIdProviderConfig(IdentityProviderConfig cfg) throws InvalidIdProviderCfgException {
    }

    public Set<IdentityHeader> getGroupHeaders(long providerId, String userId) throws FindException {
//        final StubDataStore store = StubDataStore.defaultStore();
        Set<GroupMembership> memberships = null;//store.getGroupMemberships();
        Set<IdentityHeader> results = new HashSet<IdentityHeader>();
        for (GroupMembership gm : memberships) {
            if (userId.equals(gm.getMemberUserId())) {
                Group g = null;//store.getGroups().get(gm.getThisGroupId());
                results.add(fromGroup(g));
            }
        }
        return results;
    }

    public Set<IdentityHeader> getUserHeaders(long providerId, String groupId) throws FindException {
//        final StubDataStore store = StubDataStore.defaultStore();
        Set<GroupMembership> memberships = null;//store.getGroupMemberships();
        Set<IdentityHeader> results = new HashSet<IdentityHeader>();
        for (GroupMembership gm : memberships) {
            if (groupId.equals(gm.getThisGroupId())) {
                User u = null;//store.getUsers().get(gm.getMemberUserId());
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
