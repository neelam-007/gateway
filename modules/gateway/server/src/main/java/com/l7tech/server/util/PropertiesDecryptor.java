package com.l7tech.server.util;

import com.l7tech.server.security.MasterPasswordManager;

import java.util.Properties;
import java.util.Map;
import java.util.Set;

/**
 * Utility bean that knows how to decrypt property values whose keys are identified as being password properties.
 */
public class PropertiesDecryptor {
    private final MasterPasswordManager masterPasswordManager;
    private String[] passwordPropertyNames;

    /**
     * Create a properties decryptor that will decrypt password properties using the specified MasterPasswordManager.
     *
     * @param masterPasswordManager a MasterPasswordManager bean.  Required.
     */
    public PropertiesDecryptor(MasterPasswordManager masterPasswordManager) {
        this.masterPasswordManager = masterPasswordManager;
    }

    /**
     * Set the list of property names whose values should be checked to see if they look like encrypted
     * passwords that should be decrypted.
     *
     * @param passwordPropertyNames an array of property names; if empty, no passwords will be decrypted.
     *                              If null, all property values will be run through the decryptor
     *                              if they resemble encrypted passwords.
     */
    public void setPasswordProperties(String[] passwordPropertyNames) {
        this.passwordPropertyNames = passwordPropertyNames;
    }

    /**
     * Decrypt in-place any properties whose names are matched by {@link #isPasswordPropertyName(Object)}.
     * Matching properties that cannot be decrypted for whatever reason will be left unchanged.
     *
     * @param props a Properties instance to edit in-place.  Required.
     *              Any properties that look like password properties will be run through the MasterPasswordManager.
     */
    public void decryptEncryptedPasswords(Properties props) {
        Set<Map.Entry<Object,Object>> entries = props.entrySet();
        for (Map.Entry<Object, Object> entry : entries) {
            if (isPasswordPropertyName(entry.getKey()) && entry.getValue() != null) {
                Object maybeEncrypted = entry.getValue();
                String decrypted = new String(masterPasswordManager.decryptPasswordIfEncrypted(maybeEncrypted.toString()));
                entry.setValue(decrypted);
            }
        }
    }

    /**
     * See if the specified properties key looks like the name of a password property.
     * <p/>
     * This method returns true if the name matches a property name on the whitelist
     * set by {@link #setPasswordProperties(String[])}, or if the whitelist is null.
     *
     * @param key the property name to check.  Required.
     * @return true if this looks like it might be the name of a password property.
     */
    public boolean isPasswordPropertyName(Object key) {
        if (passwordPropertyNames == null)
            return true;

        for (String name : passwordPropertyNames) {
            if (name.equals(key))
                return true;
        }
        return false;
    }
}
