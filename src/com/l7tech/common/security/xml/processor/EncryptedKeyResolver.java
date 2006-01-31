/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.l7tech.common.security.token.EncryptedKey;

/**
 * Interface provided to a WssProcessor to enable it to locate alredy-known EncryptedKey instances by their
 * EncryptedKeySHA1 identifier.
 */
public interface EncryptedKeyResolver {
    /**
     * Look up an EncryptedKey by its EncryptedKeySHA1.
     *
     * @param encryptedKeySha1 the identifier to look up.  Never null or empty.
     * @return the matching EncryptedKey token, or null if no match was found.
     * @see WssProcessorUtil#makeEncryptedKey(javax.crypto.SecretKey,String)
     */
    EncryptedKey getEncryptedKeyBySha1(String encryptedKeySha1);
}
