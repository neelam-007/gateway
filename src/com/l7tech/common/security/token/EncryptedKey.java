/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.token;



/**
 * Represents a WS-Security EncryptedKey.
 */
public interface EncryptedKey extends SecretKeyToken, SigningSecurityToken {
    String getEncryptedKeySHA1();
}
