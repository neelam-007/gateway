package com.l7tech.gateway.config.manager;

import com.l7tech.util.MasterPasswordManager;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.util.*;

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
     * @param passwordPropertyNames an array of property names; if empty or null, no passwords will be decrypted or encrypted.
     */
    public void setPasswordProperties(String[] passwordPropertyNames) {
        this.passwordPropertyNames = passwordPropertyNames;
    }

    /**
     * Get the list of property names whose values should be checked to see if they look like passwords
     * that should be (re)encrypted.
     *
     * @return an array of property names, or empty or null if no passwords will be decrypted or encrypted.
     */
    public String[] getPasswordProperties() {
        return passwordPropertyNames;
    }

    private boolean decryptAndMaybeEncryptPasswords(MasterPasswordManager encryptor, MasterPasswordManager decryptor, Properties props) {
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
     * @return true if any properties were altered.
     */
    public boolean encryptPasswords(Properties props) {
        return decryptAndMaybeEncryptPasswords(encryptor, decryptor, props);
    }

    public boolean encryptPasswords(PropertiesConfiguration props) {
        return decryptAndMaybeEncryptPasswords(encryptor, decryptor, props);
    }

    private boolean decryptAndMaybeEncryptPasswords(MasterPasswordManager encryptor, MasterPasswordManager decryptor, PropertiesConfiguration props) {
        boolean changesMade = false;
        Map<String, String> mutator = new HashMap<String, String>();

        Iterator keys = props.getKeys();
        while (keys.hasNext()) {
            String propName = (String) keys.next();
            if (isPasswordPropertyName(propName)) {
                Object oldValue = props.getProperty(propName);
                String newValue = reencrypt(encryptor, decryptor, oldValue);
                if (!newValue.equals(oldValue)) {
                    mutator.put(propName, newValue);
                }
            }
        }
        for (Map.Entry<String, String> entry : mutator.entrySet()) {
            props.setProperty(entry.getKey(), entry.getValue());
            changesMade = true;
        }
        return changesMade;
    }


    /**
     * Decrypt in-place any properties whose names are matched by {@link #isPasswordPropertyName(Object)}.
     * Takes no action if no decryptor is provided.
     *
     * @param props a Properties instance to edit in-place.  Required.
     *              Any properties that look like password properties will be run through the MasterPasswordManager.
     * @return true if any properties were altered.
     */
    public boolean decryptPasswords(Properties props) {
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
     */
    public String reencrypt(Object maybeEncrypted) {
        return reencrypt(encryptor, decryptor, maybeEncrypted);
    }

    private String reencrypt(MasterPasswordManager encryptor, MasterPasswordManager decryptor, Object maybeEncrypted) {
        String plaintext = maybeEncrypted.toString();
        if (decryptor != null)
            plaintext = new String(decryptor.decryptPasswordIfEncrypted(plaintext));
        return encryptor == null ? plaintext : encryptor.encryptPassword(plaintext.toCharArray());
    }

    /**
     * Decrypt the specified password with the current decryptor.
     *
     * @param maybeEncrypted a property value that may already be encrypted
     * @return the password decrypted by the current decryptor, if possible, otherwise the input value unchanged.
     */
    public String decryptIfEncrypted(Object maybeEncrypted) {
        return reencrypt(null, decryptor, maybeEncrypted);
    }

    /**
     * @return the encryptor, or null if no encryption is being performed.
     */
    public MasterPasswordManager getEncryptor() {
        return encryptor;
    }

    /**
     * @return the decryptor, or null if no decryption is possible.
     */
    public MasterPasswordManager getDecryptor() {
        return decryptor;
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
