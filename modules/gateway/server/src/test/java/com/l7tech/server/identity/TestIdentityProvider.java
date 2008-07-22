/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactorySpi;

import java.security.cert.X509Certificate;
import java.util.*;

/**
 * @author mike
 */
public class TestIdentityProvider implements AuthenticatingIdentityProvider {
    public static long PROVIDER_ID = 9898;
    public static int PROVIDER_VERSION = 1;

    private static Map<String, MyUser> usernameMap = Collections.synchronizedMap(new HashMap<String, MyUser>());

    public static IdentityProviderConfig TEST_IDENTITY_PROVIDER_CONFIG =
      new IdentityProviderConfig(new IdentityProviderType(9898, "TestIdentityProvider", TestIdentityProvider.class.getName()){});

    public static void clearAllUsers(){
        usernameMap.clear();    
    }

    static {
        TEST_IDENTITY_PROVIDER_CONFIG.setOid(PROVIDER_ID);
        TEST_IDENTITY_PROVIDER_CONFIG.setName("TestIdentityProvider");
        TEST_IDENTITY_PROVIDER_CONFIG.setDescription("ID provider for testing");
        TEST_IDENTITY_PROVIDER_CONFIG.setVersion(PROVIDER_VERSION);
    }

    private TestUserManager userman = new TestUserManager();
    private TestGroupManager groupman = new TestGroupManager();
    private IdentityProviderConfig config;

    public TestIdentityProvider(IdentityProviderConfig config) {
        this.config = config;
    }

    public static class Factory implements IdentityProviderFactorySpi {
        public String getClassname() {
            return TestIdentityProvider.class.getName();
        }

        public IdentityProvider createIdentityProvider(IdentityProviderConfig configuration) throws InvalidIdProviderCfgException {
            return new TestIdentityProvider(configuration);
        }
    }

    private static class MyUser {
        private char[] password;
        private UserBean user;

        private MyUser(UserBean user, char[] password) {
            this.user = user;
            this.password = password;
        }
    }

    public static void addUser(UserBean user, String username, char[] password) {
        usernameMap.put(username, new MyUser(user, password));
    }

    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        MyUser mu = usernameMap.get(pc.getLogin());
        if (mu == null) return null;
        if (Arrays.equals(mu.password, pc.getCredentials())) {
            return new AuthenticationResult(mu.user);
        }
        throw new AuthenticationException("Invalid username or password");
    }

    public IdentityProviderConfig getConfig() {
        return config;
    }

    public UserManager getUserManager() {
        return userman;
    }

    public GroupManager getGroupManager() {
        return groupman;
    }

    public Collection<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        return Collections.emptyList();
    }

    public Collection<IdentityHeader> search(boolean users, boolean groups, IdentityMapping mapping, Object value) throws FindException {
        return Collections.emptyList();
    }

    public String getAuthRealm() {
        return "myrealm";
    }

    public void test(boolean fast) throws InvalidIdProviderCfgException {
    }

    public void preSaveClientCert( User user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
    }

    public void setUserManager(UserManager userManager) {
    }

    public void setGroupManager(GroupManager groupManager) {
    }

    private class TestGroupManager implements GroupManager<User, Group> {
        public Group findByPrimaryKey(String identifier) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Group findByName(String name) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void delete(Group group) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void deleteAllVirtual(long ipoid) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public String saveGroup(Group group) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(Group group) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public String save(Group group, Set<IdentityHeader> userHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(Group group, Set<IdentityHeader> userHeaders) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection<IdentityHeader> search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Class getImpClass() {
            return getClass();
        }

        public Group reify(GroupBean bean) {
            return bean;
        }

        public boolean isMember(User user, Group group) throws FindException {
            return false;
        }

        public void addUsers(Group group, Set<User> users) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void removeUsers(Group group, Set<User> users) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void addUser(User user, Set<Group> groups) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void removeUser(User user, Set<Group> groups) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void addUser(User user, Group group) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void removeUser(User user, Group group) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Set<IdentityHeader> getGroupHeaders(User user) throws FindException {
            return Collections.emptySet();
        }

        public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
            return Collections.emptySet();
        }

        public void setGroupHeaders(User user, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Set<IdentityHeader> getUserHeaders(Group group) throws FindException {
            return Collections.emptySet();
        }

        public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
            return Collections.emptySet();
        }

        public void setUserHeaders(Group group, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void setUserHeaders(String groupId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection<IdentityHeader> findAllHeaders() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection findAll() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }

    private static class TestUserManager implements UserManager<User> {
        public User findByPrimaryKey(String identifier) throws FindException {
            MyUser mu = usernameMap.get(identifier);
            return mu == null ? null : mu.user;
        }

        public User findByLogin(String login) throws FindException {
            MyUser mu = usernameMap.get(login);
            return mu == null ? null : mu.user;
        }

        public void delete(User user) throws DeleteException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void delete(String identifier) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void deleteAll(long ipoid) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(User user) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public String save(User user, Set<IdentityHeader> groupHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public User reify(UserBean bean) {
            return bean;
        }

        public void update(User user, Set<IdentityHeader> groupHeaders) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection<IdentityHeader> search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public IdentityHeader userToHeader(User user) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public User headerToUser(IdentityHeader header) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Class getImpClass() {
            return getClass();
        }

        public Collection<IdentityHeader> findAllHeaders() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }
}
