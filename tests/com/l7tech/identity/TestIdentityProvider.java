/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

/**
 * @author mike
 */
public class TestIdentityProvider implements IdentityProvider {
    public static long PROVIDER_ID = 9898;
    private static Logger log = Logger.getLogger(TestIdentityProvider.class.getName());
    private static Map usernameMap = Collections.synchronizedMap(new HashMap());
    private static IdentityProviderConfig config = new IdentityProviderConfig(new IdentityProviderType(9898,
                                                                                                       "TestIdentityProvider",
                                                                                                       TestIdentityProvider.class.getName()));
    {
        config.setOid(PROVIDER_ID);
        config.setName("TestIdentityProvider");
        config.setDescription("ID provider for testing");
        config.setVersion(1);
    }
    private static TestUserManager userman = new TestUserManager();
    private static TestGroupManager groupman = new TestGroupManager();

    private static class TestEntityManager implements EntityManager {
        public Collection findAllHeaders() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection findAllHeaders(int offset, int windowSize) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection findAll() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection findAll(int offset, int windowSize) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Integer getVersion(long oid) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Map findVersionMap() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }

    private static class MyUser {
        private char[] password;
        private UserBean user;
        private String username;

        private MyUser(UserBean user, String username, char[] password) {
            this.user = user;
            this.username = username;
            this.password = password;
        }
    }

    public TestIdentityProvider() {
    }

    public TestIdentityProvider(IdentityProviderConfig cfg) {
    }

    public static void addUser(UserBean user, String username, char[] password) {
        usernameMap.put(username, new MyUser(user, username, password));
    }

    public User authenticate(LoginCredentials pc) throws AuthenticationException, FindException, IOException {
        MyUser mu = (MyUser)usernameMap.get(pc.getLogin());
        if (mu == null) return null;
        if (Arrays.equals(mu.password, pc.getCredentials())) {
            return mu.user;
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

    public Collection search(EntityType[] types, String searchString) throws FindException {
        return Collections.EMPTY_LIST;
    }

    public String getAuthRealm() {
        return "myrealm";
    }

    public void test() throws InvalidIdProviderCfgException {
        return;
    }

    public void preSaveClientCert(User user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
        return;
    }

    private static class TestGroupManager extends TestEntityManager implements GroupManager {
        public Group findByPrimaryKey(String identifier) throws FindException {
            return null;
        }

        public Group findByName(String name) throws FindException {
            return null;
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

        public String save(Group group) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(Group group) throws UpdateException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public String save(Group group, Set userHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(Group group, Set userHeaders) throws UpdateException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Class getImpClass() {
            return getClass();
        }

        public boolean isMember(User user, Group group) throws FindException {
            return false;
        }

        public void addUsers(Group group, Set users) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void removeUsers(Group group, Set users) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void addUser(User user, Set groups) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void removeUser(User user, Set groups) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void addUser(User user, Group group) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void removeUser(User user, Group group) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Set getGroupHeaders(User user) throws FindException {
            return Collections.EMPTY_SET;
        }

        public Set getGroupHeaders(String userId) throws FindException {
            return Collections.EMPTY_SET;
        }

        public void setGroupHeaders(User user, Set groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void setGroupHeaders(String userId, Set groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Set getUserHeaders(Group group) throws FindException {
            return Collections.EMPTY_SET;
        }

        public Set getUserHeaders(String groupId) throws FindException {
            return Collections.EMPTY_SET;
        }

        public void setUserHeaders(Group group, Set groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void setUserHeaders(String groupId, Set groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }

    private static class TestUserManager extends TestEntityManager implements UserManager {
        public User findByPrimaryKey(String identifier) throws FindException {
            MyUser mu = (MyUser)usernameMap.get(identifier);
            return mu == null ? null : mu.user;
        }

        public User findByLogin(String login) throws FindException {
            MyUser mu = (MyUser)usernameMap.get(login);
            return mu == null ? null : mu.user;
        }

        public void delete(User user) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void deleteAll(long ipoid) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public String save(User user) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(User user) throws UpdateException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public String save(User user, Set groupHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public void update(User user, Set groupHeaders) throws UpdateException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Collection search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public EntityHeader userToHeader(User user) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public Class getImpClass() {
            return getClass();
        }
    }
}
