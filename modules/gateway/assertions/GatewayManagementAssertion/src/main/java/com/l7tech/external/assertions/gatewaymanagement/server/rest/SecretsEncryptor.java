package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.io.File;
import java.io.FileNotFoundException;
import java.security.GeneralSecurityException;
import java.text.ParseException;

/**
 * Utility to encrypt secrets in a resource
 */
public class SecretsEncryptor {

    MasterPasswordManager secretsEncryptor;
    private byte[] bundlePassphraseKeyBytes;
    private boolean encryptingInitialized = false;
    private String wrappedBundleKey;

    public SecretsEncryptor(byte[] passphrase) throws GeneralSecurityException {

        // Convert bundle passphrase to bundle passphrase key bytes
        SecretKeyFactory skf = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede");
        SecretKey secretKey = skf.generateSecret( new PBEKeySpec( new String(passphrase, Charsets.UTF8).toCharArray()));
        bundlePassphraseKeyBytes = secretKey.getEncoded();
    }

    /**
     *
     * @param base64encodedKeyPassphrase The base-64 encoded passphrase to use for the encryption key when encrypting passwords, if null uses cluster passphrase by default.
     * @return
     * @throws FileNotFoundException
     * @throws GeneralSecurityException
     */
    @Nullable
    public static SecretsEncryptor createSecretsEncryptor(@Nullable final String base64encodedKeyPassphrase) throws FileNotFoundException, GeneralSecurityException {
        if (base64encodedKeyPassphrase != null) {
            return new SecretsEncryptor(HexUtils.decodeBase64(base64encodedKeyPassphrase));
        } else {
            final String confDir = SyspropUtil.getString("com.l7tech.config.path", "../node/default/etc/conf");
            final File ompDat = new File(new File(confDir), "omp.dat");
            if (ompDat.exists()) {
                return new SecretsEncryptor(new DefaultMasterPasswordFinder(ompDat).findMasterPasswordBytes());
            } else {
                throw new FileNotFoundException("Cannot encrypt because " + ompDat.getAbsolutePath() + " does not exist");
            }
        }
    }

    /**
     *
     * @param secret
     * @return encrypts secret with generated key
     */
    public String encryptSecret(String secret){
        if(!encryptingInitialized){
            setupEncrypting();
        }
        return secretsEncryptor.encryptPassword(secret.toCharArray());
    }

    /**
     * Stored in the resource
     * @return key encrypted by passphrase
     */
    public String getWrappedBundleKey(){
        if(!encryptingInitialized){
            setupEncrypting();
        }
        return wrappedBundleKey;
    }

    private void setupEncrypting(){
        // lazily create encrypting objects, no needed for decrypting

        // Create a new random bundle key
        byte[] bundleKeyBytes = new byte[32];
        RandomUtil.nextBytes(bundleKeyBytes);

        // setup secrets encryptor
        secretsEncryptor = new MasterPasswordManager(bundleKeyBytes);

        // save wrapped key
        String bundleKeyBase64 = HexUtils.encodeBase64(bundleKeyBytes);
        MasterPasswordManager encryptor = new MasterPasswordManager( bundlePassphraseKeyBytes );
        wrappedBundleKey = encryptor.encryptPassword( bundleKeyBase64.toCharArray() );

        encryptingInitialized = true;
    }

    /**
     * Decrypts secret with key
     * @param secret
     * @param encryptedKey
     * @return
     */
    public String decryptSecret(@Nullable final String secret,@Nullable final String encryptedKey) throws ParseException {
        // decrypt key
        // decrypt secret
        MasterPasswordManager keyDecryptor = new MasterPasswordManager( bundlePassphraseKeyBytes );
        MasterPasswordManager secretsDecryptor = new MasterPasswordManager(HexUtils.decodeBase64(new String(keyDecryptor.decryptPassword(encryptedKey))));

        return  new String(secretsDecryptor.decryptPassword(secret));
    }

}
