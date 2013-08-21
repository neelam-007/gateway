package com.l7tech.server.security.password;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;

import java.text.ParseException;

/**
 * Entity manager for the {@link SecurePassword} entity.
 */
public interface SecurePasswordManager extends EntityManager<SecurePassword, EntityHeader> {

    /**
     * Encrypt the specified plaintext password so to produce a value suitable for use as a SecurePassword's encodedPassword field.
     *
     * @param plaintext the plaintext password.  Required.
     * @return  The encoded password, as a string starting with "$L7C$".  Never null.
     * @throws FindException if there is a problem looking up the cluster shared key.
     */
    String encryptPassword(char[] plaintext) throws FindException;

    /**
     * Decrypt the specified encoded password, which is expected to have come from the encodedPassword field of a SecurePassword instance.
     *
     * @param encodedPassword the encoded password to decrypt.  Required.  Must be a valid encoded password starting with "$L7C$".
     * @return the decrypted password.  Never null but may be empty.
     * @throws FindException if there is a problem looking up the cluster shared key.
     * @throws ParseException if the specified encoded password cannot be decoded.
     */
    char[] decryptPassword(String encodedPassword) throws FindException, ParseException;
}
