/*
 * Copyright (C) 2003-2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.common.util;

import java.security.KeyStore;

/**
 * This class encapsulates Keystore properties.
 * @author emil
 * @version Nov 29, 2004
 */
public class KeystoreInfo {
    private final String storeFile;
    private final String storePassword;
    private final String storeType;

    /**
     * Construct the keystore info with the default type.
     *
     * @param storeFile the store file path
     * @param password the store password
     */
    public KeystoreInfo(String storeFile, String password) {
        this(storeFile, password, KeyStore.getDefaultType());
    }

    /**
     * Construct the keystore info with the store file path, store password
     * and store type
     *
     * @param storeFile the store file path
     * @param password  the store password
     * @param storeType the store type
     */
    public KeystoreInfo(String storeFile, String password, String storeType) {
        this.storeFile = storeFile;
        this.storePassword = password;
        this.storeType = storeType;
    }

    /**
     * @return the keystore file path
     */
    public String getStoreFile() {
        return storeFile;
    }

    /**
     * @return the keystore password
     */
    public String getStorePassword() {
        return storePassword;
    }

    /**
     * @return the keystore type
     */
    public String getStoreType() {
        return storeType;
    }
}