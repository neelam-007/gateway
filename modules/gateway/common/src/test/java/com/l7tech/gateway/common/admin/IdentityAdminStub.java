package com.l7tech.gateway.common.admin;

import com.l7tech.common.protocol.SecureSpanConstants;
import com.l7tech.identity.LogonInfo;
import com.l7tech.gateway.common.StubDataStore;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalGroupMembership;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.*;

/**
 * @author alex
 */
public class IdentityAdminStub implements IdentityAdmin {
    @Override
    public String echoVersion() {
        return SecureSpanConstants.ADMIN_PROTOCOL_VERSION;
    }

    @Override
    public EntityHeader[] findAllIdentityProviderConfig() throws FindException {
        Map<Goid, IdentityProviderConfig> configs = StubDataStore.defaultStore().getIdentityProviderConfigs();
        List<EntityHeader> headers = new ArrayList<EntityHeader>();
        for (Goid key : configs.keySet()) {
            IdentityProviderConfig config = configs.get(key);
            headers.add(fromIdentityProviderConfig(config));
        }
        return headers.toArray(new EntityHeader[0]);
    }

    @Override
    public IdentityProviderConfig findIdentityProviderConfigByID(Goid oid) throws FindException {
        return StubDataStore.defaultStore().getIdentityProviderConfigs().get(oid);
    }

    @Override
    public LdapIdentityProviderConfig[] getLdapTemplates() throws FindException {
        return new LdapIdentityProviderConfig[0];
    }

    @Override
    public Goid saveIdentityProviderConfig(IdentityProviderConfig config) throws SaveException, UpdateException {
        Goid oid = config.getGoid();
        if (oid == null || Goid.isDefault(oid)) {
            oid = StubDataStore.defaultStore().nextGoid();
        }
        config.setGoid(oid);
        StubDataStore.defaultStore().getIdentityProviderConfigs().put(oid, config);
        return oid;
    }

    @Override
    public void deleteIdentityProviderConfig(Goid oid) throws DeleteException {
        StubDataStore.defaultStore().getIdentityProviderConfigs().remove(oid);
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllUsers(Goid idProvCfgId) throws FindException {
        Map<String, PersistentUser> users = StubDataStore.defaultStore().getUsers();
        EntityHeaderSet<IdentityHeader> results = new EntityHeaderSet<IdentityHeader>();
        for (String s : users.keySet()) {
            User u = users.get(s);
            results.add(fromUser(u));
        }
        return results;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> searchIdentities(Goid idProvCfgId, EntityType[] types, String pattern) throws FindException {
        EntityHeaderSet<IdentityHeader> results = new EntityHeaderSet<IdentityHeader>();
        for (EntityType type : types) {
            if (type == EntityType.USER)
                results.addAll(findAllUsers(idProvCfgId));
            else if (type == EntityType.GROUP)
                results.addAll(findAllGroups(idProvCfgId));
        }
        return results;
    }

    @Override
    public User findUserByID(Goid idProvCfgId, String userId) throws FindException {
        return StubDataStore.defaultStore().getUsers().get(userId);
    }

    @Override
    public User findUserByLogin(Goid idProvCfgId, String login) throws FindException {
        if (login == null) return null;
        Map<String, PersistentUser> users = StubDataStore.defaultStore().getUsers();
        for (String uid : users.keySet()) {
            User u = users.get(uid);
            if (login.equals(u.getLogin())) return u;
        }
        return null;
    }

    @Override
    public void deleteUser(Goid idProvCfgId, String userId) throws DeleteException, ObjectNotFoundException {
        Map users = StubDataStore.defaultStore().getUsers();
        users.remove(userId);
    }

    @Override
    public String saveUser(Goid idProvCfgId, User user, Set<IdentityHeader> groupHeaders ) throws SaveException, UpdateException, ObjectNotFoundException {
        if (!(user instanceof PersistentUser)) throw new IllegalArgumentException("IdentityAdminStub only supports Internal and Federated users");
        PersistentUser pu = (PersistentUser) user;
        final StubDataStore store = StubDataStore.defaultStore();
        if (pu.getId() == null || Goid.isDefault(pu.getGoid()))
            pu.setGoid(store.nextGoid());
        pu.setProviderId(idProvCfgId);
        store.getUsers().put(pu.getId(), pu);
        if (groupHeaders != null) {
            // Clear existing memberships
            for (Iterator i = store.getGroupMemberships().iterator(); i.hasNext();) {
                GroupMembership gm = (GroupMembership)i.next();
                if (Goid.parseGoid(pu.getId()).equals(gm.getMemberUserId())) i.remove();
            }

            // Set new memberships
            for (IdentityHeader header : groupHeaders) {
                GroupMembership mem = InternalGroupMembership.newInternalMembership(header.getGoid(), pu.getGoid());
                store.getGroupMemberships().add(mem);
            }
        }
        return pu.getId();
    }

    @Override
    public String saveUser(Goid idProvCfgId, User user, Set<IdentityHeader> groupHeaders, String clearTextPassword) throws SaveException, UpdateException, ObjectNotFoundException, InvalidPasswordException {
        return null;
    }

    @Override
    public void changeUsersPassword(User user, String newClearTextPassword) throws FindException, UpdateException, InvalidPasswordException {

    }

    @Override
    public boolean currentUsersPasswordCanBeChanged() throws AuthenticationException, FindException {
        return false;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> findAllGroups(Goid idProvCfgId) throws FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map<String, PersistentGroup> groups = store.getGroups();
        EntityHeaderSet<IdentityHeader> results = new EntityHeaderSet<IdentityHeader>();
        for (String gid : groups.keySet()) {
            Group g = groups.get(gid);
            results.add(fromGroup(g));
        }
        return results;
    }

    @Override
    public Group findGroupByID(Goid idProvCfgId, String groupId) throws FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = store.getGroups();
        return (Group)groups.get(groupId);
    }

    @Override
    public Group findGroupByName(Goid idProvCfgId, String name) throws FindException {
        return null;
    }

    @Override
    public void deleteGroup(Goid idProvCfgId, String groupId) throws DeleteException, ObjectNotFoundException {
        final StubDataStore store = StubDataStore.defaultStore();
        Map groups = store.getGroups();
        groups.remove(groupId);
    }

    @Override
    public String saveGroup(Goid idProvCfgId, Group group, Set userHeaders) throws SaveException, UpdateException, ObjectNotFoundException {
        if (!(group instanceof PersistentGroup)) throw new IllegalArgumentException("IdentityAdminStub only supports Internal and Federated groups");
        PersistentGroup pg = (PersistentGroup) group;
        final StubDataStore store = StubDataStore.defaultStore();
        Map<String, PersistentGroup> groups = store.getGroups();
        if (pg.getId() == null || Goid.isDefault(pg.getGoid())) {
            pg.setGoid(store.nextGoid());
        }
        groups.put(pg.getId(), pg);
        return pg.getId();
    }

    @Override
    public String getUserCert(User user) throws FindException, CertificateEncodingException {
        return null; // TODO ?
    }

    @Override
    public boolean doesCurrentUserHaveCert() throws FindException {
        return false;
    }

    @Override
    public void revokeCert(User user) throws UpdateException, ObjectNotFoundException {
    }

    @Override
    public int revokeCertificates() throws UpdateException {
        return 0;
    }

    @Override
    public void recordNewUserCert(User user, Certificate cert) throws UpdateException {
    }

    @Override
    public void testIdProviderConfig(IdentityProviderConfig cfg, String testUser, char[] testPassword) throws InvalidIdProviderCfgException {
    }

    @Override
    public void testNtlmConfig(Map<String, String> props) throws InvalidIdProviderCfgException {
    }

    @Override
    public Set<IdentityHeader> getGroupHeaders(Goid providerId, String userId) throws FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Set<GroupMembership> memberships = store.getGroupMemberships();
        Set<IdentityHeader> results = new HashSet<IdentityHeader>();
        for (GroupMembership gm : memberships) {
            if (userId.equals(String.valueOf(gm.getMemberUserId()))) {
                Group g = store.getGroups().get(String.valueOf(gm.getThisGroupId()));
                results.add(fromGroup(g));
            }
        }
        return results;
    }

    @Override
    public Set<IdentityHeader> getGroupHeadersForGroup(Goid providerId, String groupId) throws FindException {
        return Collections.emptySet();
    }

    @Override
    public Set<IdentityHeader> getUserHeaders(Goid providerId, String groupId) throws FindException {
        final StubDataStore store = StubDataStore.defaultStore();
        Set<GroupMembership> memberships = store.getGroupMemberships();
        Set<IdentityHeader> results = new HashSet<IdentityHeader>();
        for (GroupMembership gm : memberships) {
            if (groupId.equals(String.valueOf(gm.getThisGroupId()))) {
                User u = store.getUsers().get(String.valueOf(gm.getMemberUserId()));
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
        return new IdentityHeader(u.getProviderId(), u.getId(), EntityType.USER, u.getLogin(), null, u.getName(), null);
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
        return new IdentityHeader(g.getProviderId(), g.getId(), EntityType.GROUP, g.getName(), g.getDescription(), g.getName(), null);
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
        return new EntityHeader(Goid.toString(s.getGoid()), EntityType.SERVICE, s.getName(), "");
    }

    static EntityHeader fromIdentityProviderConfig(IdentityProviderConfig config) {
        EntityHeader out = new EntityHeader();
        out.setDescription(config.getDescription());
        out.setName(config.getName());
        out.setGoid(config.getGoid());
        out.setType(EntityType.ID_PROVIDER_CONFIG);
        return out;
    }

    @Override
    public IdentityProviderPasswordPolicy getPasswordPolicyForIdentityProvider(Goid providerId) throws FindException {
        return null; // TODO ?
    }

    @Override
    public String getPasswordPolicyDescriptionForIdentityProvider() throws FindException {
        return null; // TODO ?        
    }

    @Override
    public AccountMinimums getAccountMinimums() {
        return null;
    }

    @Override
    public Map<String, AccountMinimums> getAccountMinimumsMap() {
        return null;
    }

    @Override
    public Map<String, IdentityProviderPasswordPolicy> getPasswordPolicyMinimums() {
        return null;
    }

    @Override
    public String updatePasswordPolicy(Goid providerId, IdentityProviderPasswordPolicy policy) throws SaveException, UpdateException, ObjectNotFoundException {
        return null; // TODO ?
    }

    @Override
    public void forceAdminUsersResetPassword(Goid identityProviderConfigId) throws FindException, SaveException, UpdateException {
        // TODO ?
    }

    @Override
    public void activateUser(User user) throws FindException, UpdateException {
        // TODO?
    }

    @Override
    public LogonInfo.State getLogonState(User user) throws FindException {
        return null; // TODO?
    }
}
