/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.token;

/**
 * Represents a WS-Security EncryptedKey.
 */
public interface EncryptedKey extends SecretKeyToken, SigningSecurityToken {
    /** @return true if getSecretKey() is guaranteed to return a non-null value without throwing. */
    boolean isUnwrapped();

    /** @return the EncryptedKeySHA1 string for this EncryptedKey.  May be computed lazily.  Never null or empty. */
    String getEncryptedKeySHA1();
}
