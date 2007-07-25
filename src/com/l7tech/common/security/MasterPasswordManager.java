package com.l7tech.common.security;

import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SyspropUtil;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.security.Key;

/**
 * Provides password encryption/decryption services.
 */
public class MasterPasswordManager {
    protected static final Logger logger = Logger.getLogger(MasterPasswordManager.class.getName());
    public static final String PROP_FINDER = "com.l7tech.masterPasswordFinder";
    private static final String PROP_FINDER_DEFAULT = DefaultMasterPasswordFinder.class.getName();

    private static final MasterPasswordManager INSTANCE = new MasterPasswordManager();


    public static MasterPasswordManager getInstance() {
        return INSTANCE;
    }

    public MasterPasswordManager() {
    }

    private char[] getMasterPassword() {
        try {
            return getFinder().findMasterPassword();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Unable to find master password -- assuming unecnrypted passwords", e);
            return null;
        }
    }

    private Key getKey() {
        // TODO convert master password into key
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public String encryptPassword(char[] plaintextPassword) {
        // TODO encrypt with key, add prefix
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public char[] decryptPassword(String encryptedPassword) {
        // TODO check for prefix, decrypt with key
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private MasterPasswordFinder getFinder() {
        String finderClassname = SyspropUtil.getString(PROP_FINDER, PROP_FINDER_DEFAULT);
        try {
            Class finderClass = Class.forName(finderClassname);
            if (finderClass == null || !MasterPasswordFinder.class.isAssignableFrom(finderClass))
                throw new IllegalStateException("Class does not implement MasterPasswordFinder: " + finderClassname);
            return (MasterPasswordFinder)finderClass.newInstance();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Unable to find master password finder class: " + ExceptionUtils.getMessage(e), e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to instantiate master password finder class: " + ExceptionUtils.getMessage(e), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Unable to instantiate master password finder class: " + ExceptionUtils.getMessage(e), e);
        }
    }
}

