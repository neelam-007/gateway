package com.l7tech.server.config;

import com.l7tech.common.security.MasterPasswordManager;

import java.util.Properties;
import java.util.Map;
import java.util.Set;
import java.text.ParseException;

/**
 * A class that knows how to encrypt, reencrypt, and decrypt password properties.
 */
public class PasswordPropertyCrypto {
    private final MasterPasswordManager encryptor;
    private final MasterPasswordManager decryptor;
    private String[] passwordPropertyNames;

    /**
     * Create a properties decryptor that will encrypt password properties using the specified MasterPasswordManager.
     *
     * @param encryptor a MasterPasswordManager bean to use to encrypt any passwords we see, or null to avoid encrypting any passwords.
     * @param decryptor a MasterPasswordManager bean to use to decrypt any existing encrypted passwords before re-encrypting them.
     *                  If null, we won't try to decrypt any encrypted passwords before re-encrypting them.
     */
    public PasswordPropertyCrypto(MasterPasswordManager encryptor, MasterPasswordManager decryptor) {
        this.encryptor = encryptor;
        this.decryptor = decryptor;
    }

    /**
     * Set the list of property names whose values should be checked to see if they look like passwords
     * that should be (re)encrypted.
     *
     * @param passwordPropertyNames an array of property names; if empty or null, no passwords will be encrypted.
     */
    public void setPasswordProperties(String[] passwordPropertyNames) {
        this.passwordPropertyNames = passwordPropertyNames;
    }

    private boolean decryptAndMaybeEncryptPasswords(MasterPasswordManager encryptor, MasterPasswordManager decryptor, Properties props) throws ParseException {
        boolean changesMade = false;
        Set<Map.Entry<Object,Object>> entries = props.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            if (isPasswordPropertyName(entry.getKey()) && entry.getValue() != null) {
                Object oldValue = entry.getValue();
                String newValue = reencrypt(encryptor, decryptor, oldValue);
                if (!newValue.equals(oldValue)) {
                    changesMade = true;
                    entry.setValue(newValue);
                }
            }
        }
        return changesMade;
    }

    /**
     * Encrypt in-place any properties whose names are matched by {@link #isPasswordPropertyName(Object)}.
     *
     *
     * @param props a Properties instance to edit in-place.  Required.
     *              Any properties that look like password properties will be run through the MasterPasswordManager.
     * @throws java.text.ParseException if a decryptor was provided, and an already-encrypted password was encountered
     *                                  that could not be decrypted using the provided decryptor.
     * @return true if any properties were altered.
     */
    public boolean encryptPasswords(Properties props) throws ParseException {
        return decryptAndMaybeEncryptPasswords(encryptor, decryptor, props);
    }


    /**
     * Decrypt in-place any properties whose names are matched by {@link #isPasswordPropertyName(Object)}.
     * Takes no action if no decryptor is provided.
     *
     * @param props a Properties instance to edit in-place.  Required.
     *              Any properties that look like password properties will be run through the MasterPasswordManager.
     * @return true if any properties were altered.
     * @throws java.text.ParseException if a decryptor was provided, and an already-encrypted password was encountered
     *                                  that could not be decrypted using the provided decryptor.
     */
    public boolean decryptPasswords(Properties props) throws ParseException {
        return decryptAndMaybeEncryptPasswords(null, decryptor, props);
    }

    /**
     * Reencrypt the specified password with the current encryptor.
     * If the password is already encrypted and a decryptor is set, it will first be decrypted.
     * Then it will be encrypted with the encryptor.
     * If there is no encryptor, returns the input string unchanged.
     *
     * @param maybeEncrypted a property value that may already be encrypted
     * @return the property value decrypted by the decryptor (if necessary) and then reencrypted with the current encryptor
     * @throws java.text.ParseException if a decryptor was provided, and an already-encrypted password was encountered
     *                                  that could not be decrypted using the provided decryptor.
     */
    public String reencrypt(Object maybeEncrypted) throws ParseException {
        return reencrypt(encryptor, decryptor, maybeEncrypted);
    }

    private String reencrypt(MasterPasswordManager encryptor, MasterPasswordManager decryptor, Object maybeEncrypted) throws ParseException {
        String plaintext = maybeEncrypted.toString();
        if (encryptor == null)
            return plaintext;
        if (decryptor != null && decryptor.looksLikeEncryptedPassword(plaintext))
            plaintext = new String(decryptor.decryptPassword(plaintext));
        return encryptor.encryptPassword(plaintext.toCharArray());
    }

    /**
     * Decrypt the specified password with the current decryptor.
     *
     * @param maybeEncrypted a property value that may already be encrypted
     * @return the password decrypted by the current decryptor, if possible, otherwise the input value unchanged.
     * @throws ParseException if the password appears to be encrypted but cannot be decrypted with the current decryptor
     */
    public String decryptIfEncrypted(Object maybeEncrypted) throws ParseException {
        return reencrypt(null, decryptor, maybeEncrypted);
    }

    /**
     * See if the specified properties key looks like the name of a password property.
     * <p/>
     * This method returns true if and only if the name matches a property name on the whitelist
     * set by {@link #setPasswordProperties(String[])}.
     *
     * @param key the property name to check.  Required.
     * @return true if this looks like it might be the name of a password property.
     */
    public boolean isPasswordPropertyName(Object key) {
        if (passwordPropertyNames == null)
            return false;

        for (String name : passwordPropertyNames) {
            if (name.equals(key))
                return true;
        }
        return false;
    }
}
