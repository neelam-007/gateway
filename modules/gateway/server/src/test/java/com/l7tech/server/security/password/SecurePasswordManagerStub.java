package com.l7tech.server.security.password;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.EntityManagerStub;

import java.text.ParseException;

/**
 * A test SecurePasswordManager that "encrypts" passwords by changing them into all uppercase, and "decrypts"
 * them by changing them to all lowercase.
 */
public class SecurePasswordManagerStub extends EntityManagerStub<SecurePassword,EntityHeader> implements SecurePasswordManager {
    public SecurePasswordManagerStub() {
    }

    public SecurePasswordManagerStub(SecurePassword... entitiesIn) {
        super(entitiesIn);
    }

    @Override
    public String encryptPassword(char[] plaintext) throws FindException {
        return new String(plaintext).toUpperCase();
    }

    @Override
    public char[] decryptPassword(String encodedPassword) throws FindException, ParseException {
        return encodedPassword.toLowerCase().toCharArray();
    }

    @Override
    public Class<? extends Entity> getImpClass() {
        return SecurePassword.class;
    }
}
