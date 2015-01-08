package com.l7tech.external.assertions.gatewaymanagement.server.rest;

import com.l7tech.util.ArrayUtils;
import com.l7tech.util.EncryptionUtil;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

/**
 * Utility to encrypt secrets in a resource
 */
public class SecretsEncryptor {

    private byte[] key ;
    private byte[] passphrase;

    public SecretsEncryptor(byte[] passphrase) {
        this.passphrase = passphrase;  // todo use cluster passphrase if no passphrase supplied
        init();
    }

    protected void init(){
        SecretKeyFactory skf = null;
        try {
            skf = SecretKeyFactory.getInstance("PBEWithSHA1AndDESede");
            SecretKey secretKey = skf.generateSecret( new PBEKeySpec( new String(passphrase).toCharArray() ) );
            key = secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        }


    }

    /**
     *
     * @param secret
     * @return encrypts secret with generated key
     */
    public byte[] encryptSecret(byte[] secret){
        String encrypted = new String(secret) + passphrase;
        return encrypted.getBytes(); // todo stub
    }

    /**
     * Stored in the resource
     * @return key encrypted by passphrase
     */
    public byte[] getEncryptedKey(){
        String encrypted = new String(key) + passphrase;
        return encrypted.getBytes(); // todo stub
    }

    /**
     * Decrypts secret with key
     * @param secret
     * @param encryptedKey
     * @return
     */
    public byte[] decryptSecret(byte[] secret, byte[] encryptedKey){
        // decrypt key
        // decrypt secret
        String encryptedKeyStr = new String(encryptedKey);
        String decryptedKey = encryptedKeyStr.substring(0,encryptedKeyStr.length()- new String(passphrase).length());
        String secretStr = new String(secret);
        return  secretStr.substring(0,secretStr.length()-decryptedKey.length()).getBytes(); // todo stub
    }

}
