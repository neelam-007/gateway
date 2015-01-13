package com.l7tech.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides password encryption/decryption services.
 */
public class MasterPasswordManager {
    private static final Logger logger = Logger.getLogger(MasterPasswordManager.class.getName());
    private static final String PROP_EMIT_LEGACY_ENCRYPTION = "com.l7tech.util.MasterPasswordManager.emitLegacyEncryption";

    @NotNull
    private final SecretEncryptorKeyFinder finder;

    @NotNull
    private final List<SecretEncryptor> secretEncryptors;

    private final boolean bypassKdf;

    /**
     * @return the master password converted to key bytes, or null if the master password is unavailable.
     */
    byte[] getMasterPasswordBytes() {
        byte[] ret = null;
        Throwable t = null;
        try {
            ret = finder.findMasterPasswordBytes();
        } catch (Exception e) {
            /* FALLTHROUGH and log it */
            t = e;
        }
        if (ret == null) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logger.log(Level.WARNING, "Unable to find master password -- assuming unencrypted passwords: " + ExceptionUtils.getMessage(t), ExceptionUtils.getDebugException(t));
        }
        return ret;
    }

    /**
     * Create a MasterPasswordManager that will use the specified master password finder
     * using the default secret encryptor preference ordering.
     *
     * @param finder the SecretEncryptorKeyFinder to invoke whever key material is required for a decryption operation.
     */
    public MasterPasswordManager( @NotNull final SecretEncryptorKeyFinder finder ) {
        this( finder, false, getDefaultSecretEncryptorsList() );
    }

    /**
     * Create a MasterPasswordManager that will use the specified bytes to produce the key for encrypting
     * and decrypting passwords.
     * <p/>
     * <strong>note:</strong> This constructor causes the key bytes to be kept in memory for the entire
     * lifetime of the MasterPasswordManager instance and should only be used by unit tests or short-lived processes.
     *
     * @param fixedKeyBytes a byte string that will be used to produce a key.  Will not be treated as characters.
     *                      Caller may revoke access to the key at a later time by zeroing the array, causing decryption to fail.
     */
    public MasterPasswordManager(@NotNull final byte[] fixedKeyBytes) {
        this(  new MasterPasswordFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return Arrays.copyOf( fixedKeyBytes, fixedKeyBytes.length );
            }
        }, false, null );
    }

    /**
     * Create a MasterPasswordManager that will use the specified bytes to produce the key for encrypting
     * and decrypting passwords.
     * <p/>
     * <strong>note:</strong> This constructor causes the key bytes to be kept in memory for the entire
     * lifetime of the MasterPasswordManager instance and should only be used by unit tests or short-lived processes.
     *
     * @param fixedKeyBytes a byte string that will be used to produce a key.  Will not be treated as characters.
     *                      Caller may revoke access to the key at a later time by zeroing the array, causing decryption to fail.
     * @param bypassKeyDerivation Set to true to skip the expensive KDF for each encryption/decryption operation <b>ONLY IF</b>
     *                            the key finder will always return a secret at least 32 bytes (256 bits) long
     *                            that is <b>HIGH ENTROPY</b> (that is, the output of a secure random number generator, or the
     *                            output of a key derivation function of some kind, and not something like a user-chosen
     *                            password or passphrase).
     *                            <p/>
     *                            When in any doubt, pass <b>false</b> for this parameter.
     */
    public MasterPasswordManager( @NotNull final byte[] fixedKeyBytes, boolean bypassKeyDerivation ) {
        this(  new MasterPasswordFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return Arrays.copyOf( fixedKeyBytes, fixedKeyBytes.length );
            }
        }, bypassKeyDerivation, null );
    }


    /**
     * Create a MasterPasswordManager that will use the specified master password finder
     * using the specified secret encryptors and preference ordering.
     * <p/>
     * <strong>Important note:</strong> This constructor causes the key bytes to be kept in memory for the entire
     * lifetime of the MasterPasswordManager instance and should only be used by unit tests or short-lived processes.
     *
     * @param finder the SecretEncryptorKeyFinder to invoke whever key material is required for a decryption operation.
     * @param bypassKeyDerivation Set to true to skip the expensive KDF for each encryption/decryption operation <b>ONLY IF</b>
     *                            the key finder will always return a secret at least 32 bytes (256 bits) long
     *                            that is <b>HIGH ENTROPY</b> (that is, the output of a secure random number generator, or the
     *                            output of a key derivation function of some kind, and not something like a user-chosen
     *                            password or passphrase).
     *                            <p/>
     *                            When in any doubt, pass <b>false</b> for this parameter.
     * @param secretEncryptors encryptors to use when decrypting a password.  The first encryptor in the list
     *                         will always be used for encrypting passwords, or null to use the default list.
     *                         If provided, must be nonempty.
     */
    public MasterPasswordManager( @NotNull final SecretEncryptorKeyFinder finder, boolean bypassKeyDerivation, @Nullable final List<SecretEncryptor> secretEncryptors ) {
        this.finder = finder;
        this.bypassKdf = bypassKeyDerivation;
        this.secretEncryptors = secretEncryptors == null ? getDefaultSecretEncryptorsList() : new ArrayList<>( secretEncryptors );
        if ( this.secretEncryptors.isEmpty() )
            throw new IllegalArgumentException( "secretEncryptors may not be empty" );
    }

    /**
     * Create a MasterPassword instance that encrypts and decrypts based on a fixed key (which may or may not
     * be claimed to be high-entropy), that always emits encrypted keys in the new L7C2 format, and that may or may
     * not even be able to accept keys encrypted in the old L7C format.
     *
     * @param fixedKeyBytes  static key bytes.  Required.  If this is at least 32 bytes of high-quality high-entropy key material
     *                        (that is, securely random, or from a KDF, etc. rather than a user-chosen password or passphrase)
     *                        then bypassKeyDerivation may be set to true to improve performance substantially.
     * @param bypassKeyDerivation Set to true to skip the expensive KDF for each encryption/decryption operation <b>ONLY IF</b>
     *                            the key finder will always return a secret at least 32 bytes (256 bits) long
     *                            that is <b>HIGH ENTROPY</b> (that is, the output of a secure random number generator, or the
     *                            output of a key derivation function of some kind, and not something like a user-chosen
     *                            password or passphrase).
     *                            <p/>
     *                            When in any doubt, pass <b>false</b> for this parameter.
     * @param recognizeLegacyPasswords if true, old style L7C passwords will be accepted for decryption (but will not be emitted).
     *                                 if false, old style L7C passowords will not be accepted for decryption.
     *                                 <p/>
     *                                 When in any doubt, pass <b>false</b> for this parameter.
     * @return a new MasterPasswordManager instance, ready to encrypt and decrypt using the provided key material.  Never null.
     * @throws IllegalArgumentException if bypassKeyDerivation=true and staticKeyBytes does not contain at least 32 bytes.
     */
    @NotNull
    public static MasterPasswordManager createMasterPasswordManager( @NotNull final byte[] fixedKeyBytes, boolean bypassKeyDerivation, boolean recognizeLegacyPasswords ) {
        if ( bypassKeyDerivation && fixedKeyBytes.length < 32 ) {
            throw new IllegalArgumentException( "At least 32 bytes of high-entropy key material must be provided in order to bypass key derivation" );
        }
        SecretEncryptorKeyFinder keyFinder = new SecretEncryptorKeyFinder() {
            @Override
            public byte[] findMasterPasswordBytes() {
                return Arrays.copyOf( fixedKeyBytes, fixedKeyBytes.length );
            }
        };
        List<SecretEncryptor> secretEncryptors = new ArrayList<>();
        secretEncryptors.add( new L7C2SecretEncryptor() );
        if ( recognizeLegacyPasswords ) {
            secretEncryptors.add( new L7CSecretEncryptor() );
        }
        return new MasterPasswordManager( keyFinder, bypassKeyDerivation, secretEncryptors );
    }

    private static List<SecretEncryptor> getDefaultSecretEncryptorsList() {
        boolean legacyMode = ConfigFactory.getBooleanProperty( PROP_EMIT_LEGACY_ENCRYPTION, false );
        if ( legacyMode ) {
            // Emit legacy L7C, parse either
            return Arrays.asList(
                    new L7CSecretEncryptor(),
                    new L7C2SecretEncryptor()
            );
        } else {
            // Normal mode: emit new L7C2, parse either
            return Arrays.asList(
                    new L7C2SecretEncryptor(),
                    new L7CSecretEncryptor()
            );
        }
    }

    /**
     * Encrypt a plaintext password using a key derived from the master password and a random salt.
     *
     * @param plaintextPassword the password to encrypt.  Required.
     * @return the encrypted form of this password, in the form "$L7C2$jasdjhfasdkj$asdkajsdhfaskdjfhasdkjfh".  Never null.
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public String encryptPassword( char[] plaintextPassword ) {
        SecretEncryptor encryptor = secretEncryptors.get( 0 );
        return encryptor.encryptPassword( finder, bypassKdf, IOUtils.encodeCharacters( Charsets.UTF8, plaintextPassword ) );
    }

    /**
     * Test if the specified string looks like it might be an encrypted password.
     * <p/>
     * This method considers a string to look like an encrypted password if it starts with one of the
     * recognized encryption prefixes ("$L7C$" or "$L7C2$").
     *
     * @param possiblyEncryptedPassword a password that might be encrypted.  Required.
     * @return true if this password looks like an encrypted password.
     */
    public boolean looksLikeEncryptedPassword(String possiblyEncryptedPassword) {
        for ( SecretEncryptor secretEncryptor : secretEncryptors ) {
            if ( secretEncryptor.looksLikeEncryptedSecret( possiblyEncryptedPassword ) )
                return true;
        }
        return false;
    }

    /**
     * Decrypts a password if it looks like an encrypted password; otherwise returns it unchanged.
     * If errors occur while decrypting the password, this logs the errors at level WARNING but then
     * returns the password unchanged.
     *
     * @param possiblyEncryptedPassword a password that might be encrypted.  Required.
     * @return the decrypted version of the password if it was encrypted and decryption was possible, otherwise
     *         the original password's characters unchanged.  Possibly null if the input string was null.
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public char[] decryptPasswordIfEncrypted(String possiblyEncryptedPassword) {
        for ( SecretEncryptor secretEncryptor : secretEncryptors ) {
            if ( secretEncryptor.looksLikeEncryptedSecret( possiblyEncryptedPassword ) ) {
                try {
                    byte[] bytes = secretEncryptor.decryptPassword( finder, possiblyEncryptedPassword );
                    return IOUtils.decodeCharacters( Charsets.UTF8, bytes );
                } catch ( ParseException e ) {
                    logger.log( Level.WARNING, "Unable to decrypt encrypted password: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
                    return possiblyEncryptedPassword.toCharArray();
                }
            }
        }

        return possiblyEncryptedPassword == null ? null : possiblyEncryptedPassword.toCharArray();
    }

    /**
     * Decrypt an encrypted password.
     *
     * @param encryptedPassword an encrypted password, similar to "$L7C$jasdjhfasdkj$asdkajsdhfaskdjfhasdkjfh".  Required.
     * @return the plaintext of the password.  Never null, but may be empty.
     * @throws ParseException if the encrypted password was invalid and could not be decrypted
     * @throws RuntimeException if a needed algorithm or padding mode is unavailable
     */
    public char[] decryptPassword(String encryptedPassword) throws ParseException {
        for ( SecretEncryptor secretEncryptor : secretEncryptors ) {
            if ( secretEncryptor.looksLikeEncryptedSecret( encryptedPassword ) ) {
                byte[] bytes = secretEncryptor.decryptPassword( finder, encryptedPassword );
                return Charsets.UTF8.decode( ByteBuffer.wrap( bytes ) ).array();
            }
        }
        throw new ParseException("Not an encrypted password", 0);
    }

    /**
     * Interface implemented by strategies for obtaining the master password.
     */
    public static interface MasterPasswordFinder extends SecretEncryptorKeyFinder {
    }
}

