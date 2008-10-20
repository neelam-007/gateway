/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.math.BigInteger;

/**
 * @author mike
 */
public class TestIdentityProvider implements AuthenticatingIdentityProvider<User,Group,UserManager<User>,GroupManager<User,Group>> {
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
    private TestGroupManager groupman = new TestGroupManager(PROVIDER_ID);
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

    public static void reset(){
        usernameMap.clear();
    }
    
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        MyUser mu = usernameMap.get(pc.getLogin());
        if (mu == null) return null;
        if (Arrays.equals(mu.password, pc.getCredentials())) {
            return new AuthenticationResult(mu.user);
        }
        throw new AuthenticationException("Invalid username or password");
    }

    public X509Certificate findCertByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        return null;
    }

    public IdentityProviderConfig getConfig() {
        return config;
    }

    public boolean updateFailedLogonAttempt(LoginCredentials lc) {
        return false;
    }

    public boolean hasClientCert(LoginCredentials lc) throws AuthenticationException {
        return false;  
    }

    public UserManager getUserManager() {
        return userman;
    }

    public GroupManager<User,Group> getGroupManager() {
        return groupman;
    }

    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        return EntityHeaderSet.empty();
    }

    public EntityHeaderSet<IdentityHeader> search(boolean users, boolean groups, IdentityMapping mapping, Object value) throws FindException {
        return EntityHeaderSet.empty();
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

    public void validate(User u) throws ValidationException {
        if(!usernameMap.containsKey(u.getLogin())){
            throw new ValidationException("User " + u.getLogin()+ " does not exist in the TestIdentityProvider");
        }
    }

    private class TestGroupManager implements GroupManager<User, Group> {
        private Map<User, Set<Group>> userToGroupMap = new HashMap<User, Set<Group>>();
        private long providerId;

        public TestGroupManager(long providerId){
            this.providerId = providerId;    
        }
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
            userToGroupMap.put(user, groups);
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
            Set<Group> groups = userToGroupMap.get(user);
            if(groups == null) return null;

            Set<IdentityHeader> returnSet = new HashSet<IdentityHeader>();
            for(Group g: groups){
                IdentityHeader iH = new IdentityHeader(providerId,g.getId(), EntityType.GROUP, g.getName(), g.getDescription());
                returnSet.add(iH);
            }
            return returnSet;
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

        public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
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

        public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public IdentityHeader userToHeader(User user) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        public User headerToUser(IdentityHeader header) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @SuppressWarnings({"unchecked"})
        public Class getImpClass() {
            return getClass();
        }

        public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }
}
