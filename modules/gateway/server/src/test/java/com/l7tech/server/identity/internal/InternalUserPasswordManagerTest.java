package com.l7tech.server.identity.internal;

import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.PasswordChangeRecord;
import com.l7tech.util.Charsets;
import com.l7tech.util.Config;
import com.l7tech.util.HexUtils;
import com.l7tech.util.MockConfig;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * Provides test coverage for every condition which can cause a true value to return from
 * {@link InternalUserPasswordManager#configureUserPasswordHashes(InternalUser, String)}.
 *
 * @author darmstrong
 */
public class InternalUserPasswordManagerTest {
    private final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();
    private InternalUserPasswordManager userPasswordManager;
    private static final String PASSWORD = "password";
    private final Properties props = new Properties();
    private static final String HTTP__DIGEST__ENABLE = "httpDigest.enable";

    @Before
    public void setUp() {
        props.clear();
        Config testConfig = new MockConfig(props);
        userPasswordManager = new InternalUserPasswordManagerImpl(testConfig, passwordHasher);
    }

    @Test
    public void testInitialAndSubsequentCheckingOfSamePassword(){
        InternalUser iUser = new InternalUser();
        iUser.setLogin("admin");

        //ensure password is set and is a change is reported.
        assertNull( iUser.getHashedPassword() );
        boolean updateRequired = userPasswordManager.configureUserPasswordHashes(iUser, PASSWORD);
        assertTrue( "User should need update", updateRequired );
        assertNotNull( iUser.getHashedPassword() );

        final String cachedPassword = iUser.getHashedPassword();
        updateRequired = userPasswordManager.configureUserPasswordHashes(iUser, PASSWORD);
        assertFalse( "User should not need an update", updateRequired );
        assertEquals( "hashedPassword property should not have been modified", cachedPassword, iUser.getHashedPassword() );
    }

    @Test
    public void testDigestEnabledAndChanged(){
        InternalUser iUser = new InternalUser();
        iUser.setLogin("admin");
        iUser.setHashedPassword(passwordHasher.hashPassword(PASSWORD.getBytes(Charsets.UTF8)));

        props.setProperty(HTTP__DIGEST__ENABLE, "true");

        final String cachedPassword = iUser.getHashedPassword();
        final String calculatedDigest = HexUtils.encodePasswd("admin", PASSWORD, HexUtils.REALM);
        assertNull( iUser.getHttpDigest() );
        boolean updateRequired = userPasswordManager.configureUserPasswordHashes(iUser, PASSWORD);
        assertTrue( "User should need update", updateRequired );
        assertEquals( "httpDigest property should have been set.", calculatedDigest, iUser.getHttpDigest() );
        //hashedPassword should have have been changed
        assertEquals( "hashedPassword property should not have been changed.", cachedPassword, iUser.getHashedPassword() );

        //changed hashedPassword - should cause change to digest
        iUser.setPasswordChangesHistory(new ArrayList<PasswordChangeRecord>());//avoid NPE as no hibernate in use.
        
        updateRequired = userPasswordManager.configureUserPasswordHashes(iUser, "newpassword");
        assertTrue( "User should need update", updateRequired );
        assertNotSame( "hashedPassword property shoud have been changed.", cachedPassword, iUser.getHashedPassword() );
        assertNotSame( "httpDigest should have been changed.", calculatedDigest, iUser.getHttpDigest() );
    }

    @Test
    public void testDigestDisabled(){
        InternalUser iUser = new InternalUser();
        iUser.setLogin("admin");
        final String calculatedHash = passwordHasher.hashPassword(PASSWORD.getBytes(Charsets.UTF8));
        iUser.setHashedPassword(calculatedHash);
        final String calculatedDigest = HexUtils.encodePasswd("admin", PASSWORD, HexUtils.REALM);
        iUser.setHttpDigest(calculatedDigest);

        boolean updateRequired = userPasswordManager.configureUserPasswordHashes(iUser, PASSWORD);
        assertTrue( "Update required as digest shoud have been cleared.", updateRequired );
        assertNull( iUser.getHttpDigest() );
        assertSame( "No change was needed for hashedPassword", calculatedHash, iUser.getHashedPassword() );
    }

    @Test
    public void testPasswordChangesTruncated() {
        // InternalUserPasswordManagerImpl.manageHistory truncates password
        // history by modifying the list in place. This test ensures this
        // approach keeps working
        final InternalUser user = new InternalUser();
        user.setPasswordChangesHistory( new ArrayList<PasswordChangeRecord>( Arrays.asList(
                new PasswordChangeRecord( user, 1L, "ABCD" ),
                new PasswordChangeRecord( user, 2L, "EFGH"),
                new PasswordChangeRecord( user, 3L, "IJKL")
        ) ) );
        final List<PasswordChangeRecord> passwordChangeList = user.getPasswordChangesHistory();
        assertNotNull( "password history", passwordChangeList );
        assertEquals( "password history size", 3L, (long)passwordChangeList.size() );
        passwordChangeList.remove( 2 );
        passwordChangeList.remove( 1 );
        assertEquals( "password history size", 1L, (long)passwordChangeList.size() );
        assertEquals( "user password history size", 1L, (long)user.getPasswordChangesHistory().size() );
    }
}
