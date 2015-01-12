package com.l7tech.server.security.password;

import com.l7tech.server.security.sharedkey.SharedKeyManagerStub;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class SecurePasswordManagerTest {
    SharedKeyManagerStub skm = new SharedKeyManagerStub("7layer".toCharArray());

    @Test
    public void testEncryptAndDecryptPassword() throws Exception {
        SecurePasswordManagerImpl spm = new SecurePasswordManagerImpl(skm);
        String encrypted = spm.encryptPassword("foobarbazblat".toCharArray());
        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("$L7C"));

        assertEquals("foobarbazblat", new String(spm.decryptPassword(encrypted)));
    }
}
