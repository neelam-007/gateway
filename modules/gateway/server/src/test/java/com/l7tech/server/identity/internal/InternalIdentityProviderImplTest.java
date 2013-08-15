/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.identity.internal;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.identity.BadCredentialsException;
import com.l7tech.identity.User;
import com.l7tech.identity.UserBean;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MockConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Set;

/**
 * Currently simply tests pass authentication, specifically for pre Chinook to Chinook behaviour.
 */
public class InternalIdentityProviderImplTest {
    private final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();
    private final UsernamePasswordSecurityToken userPassToken =
            new UsernamePasswordSecurityToken(SecurityTokenType.HTTP_BASIC, "admin", "password".toCharArray() );
    private final InternalIdentityProviderImpl provider = new InternalIdentityProviderImpl();
    private final LoginCredentials pc = LoginCredentials.makeLoginCredentials(userPassToken, null );
    private Method authMethod;
    private InternalUser user;

    @Before
    public void setUp() throws Exception{
        provider.setPasswordHasher(passwordHasher);

        authMethod = provider.getClass().getDeclaredMethod("authenticatePasswordCredentials",
                LoginCredentials.class,
                InternalUser.class,
                boolean.class);
        authMethod.setAccessible(true);
        user = new InternalUser();
        user.setLogin("admin");
    }

    /**
     * Test the normal password auth case post Chinook - new hashing scheme.
     */
    @Test
    public void testPasswordException() throws Exception {
        final String hashedPassword = passwordHasher.hashPassword("password".getBytes(Charsets.UTF8));
        user.setHashedPassword(hashedPassword);

        final Object result = authMethod.invoke(provider, pc, user, false);//false means no need for a user manager internally
        Assert.assertNotNull(result);
        Assert.assertNull("Users digest should not have been modified", user.getHttpDigest());
    }

    /**
     * Tests that for a user who has not been upgraded, that message traffic authentication works and no update
     * is needed.
     * @throws Exception
     */
    @Test
    public void testAuthPassedMessageTraffic() throws Exception{

        final String oldDigest = HexUtils.encodePasswd("admin", "password", HexUtils.REALM);
        user.setHttpDigest(oldDigest);
        Assert.assertNull("hashedPassword property should not be set", user.getHashedPassword());//protect against test changes

        final Object result = authMethod.invoke(provider, pc, user, false);//false means no need for a user manager internally
        Assert.assertNotNull(result);
    }

    /**
     * Tests that for an upgraded user it is not possible to fall back onto digest authentication.
     */
    @Test(expected = BadCredentialsException.class)
    public void testNoDigestFallback() throws Throwable{
        //configured a hash and a digest
        final String hashedPassword = passwordHasher.hashPassword("password".getBytes(Charsets.UTF8));
        user.setHashedPassword(hashedPassword);
        final String oldDigest = HexUtils.encodePasswd("admin", "password", HexUtils.REALM);
        user.setHttpDigest(oldDigest);

        //try to authenticate with an incorrect password for the user.
        final UsernamePasswordSecurityToken incorrectToken =
                new UsernamePasswordSecurityToken(SecurityTokenType.HTTP_BASIC, "admin", "incorrect".toCharArray() );
        final LoginCredentials incorrectPc = LoginCredentials.makeLoginCredentials(incorrectToken, null );

        try {
            authMethod.invoke(provider, incorrectPc, user, false);//false means no need for a user manager internally
        } catch (Exception e) {
            throw e.getCause();
        }
    }

    /**
     * Test upgrade path for pre Chinook to post Chinook password storage. Following authenticate the user
     * should have a hashedPassword property set.
     */
    @Test
    public void testUpgradeIsNeededAndallowed() throws Exception {
        final boolean [] userUpdated = new boolean[]{false};

        final TestInternalUserManager userManager = new TestInternalUserManager(){
            @Override
            public void update(InternalUser user) throws UpdateException, FindException {
                userUpdated[0] = true;
            }

            @Override
            public InternalUserPasswordManager getUserPasswordManager() {
                return new InternalUserPasswordManagerImpl(new MockConfig(new Properties()), passwordHasher );
            }
        };
        provider.setUserManager(userManager);
        
        final String oldDigest = HexUtils.encodePasswd("admin", "password", HexUtils.REALM);
        user.setHttpDigest(oldDigest);
        Assert.assertNull("hashedPassword property should not be set", user.getHashedPassword());//protect against test changes

        final Object result = authMethod.invoke(provider, pc, user, true);
        Assert.assertNotNull(result);

        //1) user should now have a hashedPassword
        final String hashedPassword = user.getHashedPassword();
        Assert.assertNotNull("User should have had their hashedPassword property set.", hashedPassword);
        Assert.assertTrue("User manager should have been told to update the user.", userUpdated[0]);

        //2) hashed password should verify for the password hasher
        passwordHasher.verifyPassword("password".getBytes(Charsets.UTF8), hashedPassword);//will throw if not valid

        Assert.assertNull("Users digest property should have been removed.", user.getHttpDigest());
    }

    /**
     * Simply implemented every method. Any needed methods can be overridden again by implementors in test case.
     */
    private static class TestInternalUserManager implements InternalUserManager{
        @Override
        public void configure(InternalIdentityProvider provider) {

        }

        @Override
        public InternalUserPasswordManager getUserPasswordManager() {
            return null;
        }

        @Override
        public InternalUser cast(User u) {
            return null;
        }

        @Override
        public InternalUser findByPrimaryKey(String identifier) throws FindException {
            return null;
        }

        @Override
        public InternalUser findByLogin(String login) throws FindException {
            return null;
        }

        @Override
        public void delete(InternalUser user) throws DeleteException {

        }

        @Override
        public void delete(String identifier) throws DeleteException, FindException {

        }

        @Override
        public void deleteAll(Goid ipoid) throws DeleteException, FindException {

        }

        @Override
        public void update(InternalUser user) throws UpdateException, FindException {

        }

        @Override
        public String save(InternalUser user, Set<IdentityHeader> groupHeaders) throws SaveException {
            return null;
        }

        @Override
        public InternalUser reify(UserBean bean) {
            return null;
        }

        @Override
        public void update(InternalUser user, Set<IdentityHeader> groupHeaders) throws UpdateException, FindException {

        }

        @Override
        public EntityHeaderSet<IdentityHeader> search(String searchString) throws FindException {
            return null;
        }

        @Override
        public IdentityHeader userToHeader(InternalUser user) {
            return null;
        }

        @Override
        public InternalUser headerToUser(IdentityHeader header) {
            return null;
        }

        @Override
        public Class<? extends User> getImpClass() {
            return null;
        }

        @Override
        public EntityHeaderSet<IdentityHeader> findAllHeaders() throws FindException {
            return null;
        }
    }
}
