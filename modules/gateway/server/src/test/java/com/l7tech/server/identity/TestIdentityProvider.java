/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity;

import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.*;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SessionSecurityToken;
import com.l7tech.util.GoidUpgradeMapper;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;
import java.util.*;
import java.math.BigInteger;

/**
 * @author mike
 */
public class TestIdentityProvider implements AuthenticatingIdentityProvider<User,Group,UserManager<User>,GroupManager<User,Group>> {
    public static Goid PROVIDER_ID = GoidUpgradeMapper.mapOid(null, 9898L);
    public static int PROVIDER_VERSION = 1;

    private static Map<String, MyUser> usernameMap = Collections.synchronizedMap(new HashMap<String, MyUser>());
    private static Map<String, MyGroup> groupMap = Collections.synchronizedMap(new HashMap<String, MyGroup>());

    public static IdentityProviderConfig TEST_IDENTITY_PROVIDER_CONFIG =
      new IdentityProviderConfig(new IdentityProviderType(9898, "TestIdentityProvider", TestIdentityProvider.class.getName()){});

    public static void clearAllUsers(){
        usernameMap.clear();    
    }

    static {
        TEST_IDENTITY_PROVIDER_CONFIG.setGoid(PROVIDER_ID);
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
        @Override
        public String getClassname() {
            return TestIdentityProvider.class.getName();
        }

        @Override
        public IdentityProvider createIdentityProvider(IdentityProviderConfig configuration, boolean start) throws InvalidIdProviderCfgException {
            return new TestIdentityProvider(configuration);
        }

        @Override
        public void destroyIdentityProvider( final IdentityProvider identityProvider ) {
        }
    }

    private static class MyUser {
        private char[] password;
        private String certDn;
        private UserBean user;
        private long passwordExpiryTime;

        private MyUser(UserBean user, char[] password, long passwordExpiryTime) {
            this.user = user;
            this.password = password;
            this.passwordExpiryTime = passwordExpiryTime;
        }

        private MyUser(UserBean user, char[] password, String certDn, long passwordExpiryTime) {
            this.user = user;
            this.password = password;
            this.passwordExpiryTime = passwordExpiryTime;
            this.certDn = certDn;
            user.setSubjectDn( certDn );
        }
    }

    private static class MyGroup {
        private GroupBean group;
        private UserBean[] users;

        private MyGroup(GroupBean group, UserBean[] users) {
            this.group = group;
            this.users = users;
        }
    }

    public static void addUser(UserBean user, String username, char[] password) {
        MyUser myUser = new MyUser(user, password, user.getPasswordExpiry());
        usernameMap.put(username, myUser);
    }

    public static void addUser(UserBean user, String username, char[] password, String certDn) {
        MyUser myUser = new MyUser(user, password, certDn, user.getPasswordExpiry());
        usernameMap.put(username, myUser);
    }

    public static void addGroup( GroupBean gb1, UserBean... users ) {
        MyGroup myGroup = new MyGroup( gb1, users );
        groupMap.put(gb1.getName().toLowerCase(), myGroup);
    }

    public static void reset(){
        usernameMap.clear();
    }
    
    @Override
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        return authenticate(pc, false);
    }

    @Override
    public AuthenticationResult authenticate(LoginCredentials pc, boolean allowUserUpgrade) throws AuthenticationException {
        MyUser mu = usernameMap.get(pc.getLogin());
        if (mu == null) return null;
        if (mu.password != null && pc.getCredentials()!=null && Arrays.equals(mu.password, pc.getCredentials())) {
            return new AuthenticationResult(mu.user, pc.getSecurityTokens());
        }
        if (mu.certDn != null && pc.getClientCert()!=null && mu.certDn.equals(pc.getClientCert().getSubjectDN().getName())) {
            return new AuthenticationResult(mu.user, pc.getSecurityTokens(), pc.getClientCert(), false);
        }
        if ( pc.getFormat().equals( CredentialFormat.SESSIONTOKEN ) &&
             mu.user.getId().equals( ((SessionSecurityToken)pc.getSecurityToken()).getUserId() ) &&
             config.getGoid().equals(((SessionSecurityToken)pc.getSecurityToken()).getProviderId())) {
            return new AuthenticationResult(mu.user, pc.getSecurityTokens());
        }
        throw new AuthenticationException("Invalid username or password");
    }

    @Override
    public User findUserByCredential(LoginCredentials lc) throws FindException {
        return userman.findByLogin(lc.getLogin());
    }

    @Override
    public X509Certificate findCertByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
        return null;
    }

    @Override
    public X509Certificate findCertBySki( final String ski ) throws FindException {
        return null;
    }

    @Override
    public X509Certificate findCertByThumbprintSHA1( final String thumbprintSHA1 ) throws FindException {
        return null;
    }

    @Override
    public X509Certificate findCertBySubjectDn(X500Principal subjectDn) throws FindException {
        return null;
    }

    @Override
    public IdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public boolean hasClientCert(String login) throws AuthenticationException {
        return false;  
    }

    @Override
    public UserManager<User> getUserManager() {
        return userman;
    }

    @Override
    public GroupManager<User,Group> getGroupManager() {
        return groupman;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        return EntityHeaderSet.empty();
    }

    @Override
    public String getAuthRealm() {
        return "myrealm";
    }

    @Override
    public void test(boolean fast, String testUser, char[] testPassword) throws InvalidIdProviderCfgException {
    }

    @Override
    public void preSaveClientCert( User user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
    }

    @Override
    public void setUserManager(UserManager userManager) {
    }

    @Override
    public void setGroupManager(GroupManager groupManager) {
    }

    @Override
    public void validate(User u) throws ValidationException {
        MyUser myUser = usernameMap.get(u.getLogin());
        if (myUser == null) {
            throw new ValidationException("User " + u.getLogin()+ " does not exist in the TestIdentityProvider");
        }
        if (myUser.passwordExpiryTime > 0 && myUser.passwordExpiryTime <= System.currentTimeMillis()) throw new ValidationException("Account is expired");
    }

    private class TestGroupManager implements GroupManager<User, Group> {
        private Map<User, Set<Group>> userToGroupMap = new HashMap<User, Set<Group>>();
        private Goid providerId;

        public TestGroupManager(Goid providerId){
            this.providerId = providerId;    
        }
        @Override
        public Group findByPrimaryKey(String identifier) throws FindException {
            Group group = null;
            for ( MyGroup myGroup : groupMap.values() ) {
                if ( myGroup.group.getId().equals(identifier )) {
                    group = myGroup.group;
                    break;
                }
            }
            return group;
        }

        @Override
        public Group findByName(String name) throws FindException {
            MyGroup myGroup = groupMap.get( name.toLowerCase() );
            return myGroup==null ? null : myGroup.group;
        }

        @Override
        public void delete(Group group) throws DeleteException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void delete(String identifier) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void deleteAll(Goid ipoid) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void deleteAllVirtual(Goid ipoid) throws DeleteException, ObjectNotFoundException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public String saveGroup(Group group) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void update(Group group) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public String save(Group group, Set<IdentityHeader> userHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public String save(Goid id, Group group, Set<IdentityHeader> userHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void update(Group group, Set<IdentityHeader> userHeaders) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public Collection<IdentityHeader> search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public Class getImpClass() {
            return getClass();
        }

        @Override
        public Group reify(GroupBean bean) {
            return bean;
        }

        @Override
        public boolean isMember(final User user, final Group group) throws FindException {
            boolean member = false;
            outer: for ( MyGroup myGroup : groupMap.values() ) {
                if ( myGroup.group.getId().equals(group.getId() )) {
                    for ( UserBean groupUser : myGroup.users ) {
                        if ( groupUser.getProviderId().equals(user.getProviderId()) &&
                            groupUser.getId().equals(user.getId())) {
                            member = true;
                            break outer;
                        }
                    }
                    break;
                }
            }
            return member;
        }

        @Override
        public void addUsers(Group group, Set<User> users) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void removeUsers(Group group, Set<User> users) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void addUser(User user, Set<Group> groups) throws FindException, UpdateException {
            userToGroupMap.put(user, groups);
        }

        @Override
        public void removeUser(User user, Set<Group> groups) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void addUser(User user, Group group) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void removeUser(User user, Group group) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public Set<IdentityHeader> getGroupHeaders(User user) throws FindException {
            Set<Group> groups = userToGroupMap.get(user);
            if(groups == null) return null;

            Set<IdentityHeader> returnSet = new HashSet<IdentityHeader>();
            for(Group g: groups){
                IdentityHeader iH = new IdentityHeader(providerId,g.getId(), EntityType.GROUP, g.getName(), g.getDescription(), null, null);
                returnSet.add(iH);
            }
            return returnSet;
        }

        @Override
        public Set<IdentityHeader> getGroupHeaders(String userId) throws FindException {
            return Collections.emptySet();
        }

        @Override
        public Set<IdentityHeader> getGroupHeadersForNestedGroup(String groupId) throws FindException {
            return Collections.emptySet();
        }

        @Override
        public void setGroupHeaders(User user, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void setGroupHeaders(String userId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public Set<IdentityHeader> getUserHeaders(Group group) throws FindException {
            return Collections.emptySet();
        }

        @Override
        public Set<IdentityHeader> getUserHeaders(String groupId) throws FindException {
            return Collections.emptySet();
        }

        @Override
        public void setUserHeaders(Group group, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void setUserHeaders(String groupId, Set<IdentityHeader> groupHeaders) throws FindException, UpdateException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public Collection findAll() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }

    private static class TestUserManager implements UserManager<User> {
        @Override
        public User findByPrimaryKey(String identifier) throws FindException {
            MyUser mu = usernameMap.get(identifier);
            return mu == null ? null : mu.user;
        }

        @Override
        public User findByLogin(String login) throws FindException {
            MyUser mu = usernameMap.get(login);
            return mu == null ? null : mu.user;
        }

        @Override
        public void delete(User user) throws DeleteException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void delete(String identifier) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void deleteAll(Goid ipoid) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public void update(User user) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public String save(User user, Set<IdentityHeader> groupHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public String save(Goid id, User user, Set<IdentityHeader> groupHeaders) throws SaveException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public User reify(UserBean bean) {
            return bean;
        }

        @Override
        public void update(User user, Set<IdentityHeader> groupHeaders) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public IdentityHeader userToHeader(User user) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        public User headerToUser(IdentityHeader header) {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public Class getImpClass() {
            return getClass();
        }

        @Override
        public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
            throw new UnsupportedOperationException("not supported for TestIdentityProvider");
        }
    }
}
