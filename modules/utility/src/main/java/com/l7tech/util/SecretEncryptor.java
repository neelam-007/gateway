package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import javax.security.auth.Destroyable;
import java.security.InvalidKeyException;
import java.text.ParseException;

/**
 * A password encryption scheme, such as L7C or L7C2.
 * <p/>
 * Use this interface with a key finder to encrypt or decrypt passwords or other secrets that can be stored as "$L7C2$" strings.
 */
public interface SecretEncryptor {

    /**
     * Check if the specified password looks like a password that has been encrypted with this SecretEncryptor's mechanism.
     * Typically this will be done just be checking the prefix.
     * If this method returns true it is still not guaranteed that a subsequent encryption attempt will succeed.
     *
     * @param possiblyEncryptedSecret a string that might be an encrypted password in the format emitted
     *                                  by this SecretEncryptor
     * @return true if the specified string looks like it might be an encrypted password in the format emitted
     *         by this SecretEncryptor
     */
    boolean looksLikeEncryptedSecret( String possiblyEncryptedSecret );

    /**
     * Get the name of this scheme.
     *
     * @return the name of this scheme (not the raw prefix), eg "L7C" or "L7C2".
     */
    @NotNull
    String getName();

    /**
     * Get the prefix string for this scheme.
     *
     * @return the raw prefix string (including any needed dollar sign delimiters or other punctuation), eg "$L7C$" or "$L7C2$".
     */
    @NotNull
    String getPrefix();

    /**
     * Encrypt a password using this scheme.
     * <p/>
     * The input value is password bytes.  If the password was originally a string or other char sequence
     * it will need to be encoded to bytes first.  The encoding is up to the caller but UTF-8 is a good default.
     *
     * @param keyFinder a key finder instance that will be consulted in order to locate a key to use for encryption.  Required.
     * @param bypassKeyDerivation Set to true to skip the expensive KDF for each encryption/decryption operation <b>ONLY IF</b>
     *                            the key finder will always return a secret at least 32 bytes (256 bits) long
     *                            that is <b>HIGH ENTROPY</b> (that is, the output of a secure random number generator, or the
     *                            output of a key derivation function of some kind, and not something like a user-chosen
     *                            password or passphrase).
     *                            <p/>
     *                            Ignored if not supported by this SecretEncryptor implementation.
     *                            <p/>
     *                            When in any doubt, pass <b>false</b> for this parameter.
     * @param secretBytes the secret to be encrypted, expressed as a byte array of arbitrary (possibly zero) length.  Required.
     * @return the encrypted password encoded as a printable string that might look similar to "$scheme$salt$ciphertext"
     *         where scheme is something like L7C or L7C2 and salt and ciphertext are strings of Base 64.
     *         The return value will always start with the prefix returned by {@link #getPrefix()}.
     */
    @NotNull
    String encryptPassword( @NotNull SecretEncryptorKeyFinder keyFinder, boolean bypassKeyDerivation, @NotNull byte[] secretBytes );

    /**
     * Decrypt a password according to this scheme.
     * <p/>
     * The input value is a printable string encoding the encrypted password.  The prefix of the string
     * must match the prefix returned by {@link #getPrefix} or an ParseException will be thrown.
     *
     * @param keyFinder a key finder instance that will be consulted in order to locate a key to use for decryption.  Required.
     * @param encryptedPassword an encoded string that starts with {@link #getPrefix()}, and might look similar to "$scheme$salt$ciphertext"
     *                          where scheme is something like L7C or L7C2 and salt and ciphertext are base64 encoded strings.
     * @return the decrypted plaintext password bytes.  Never null.
     * @throws ParseException if the specified encrypted password is not in the correct format to be decrypted.
     */
    @NotNull
    byte[] decryptPassword( @NotNull SecretEncryptorKeyFinder keyFinder, @NotNull String encryptedPassword ) throws ParseException;
}
