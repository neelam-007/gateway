/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.token;

import javax.crypto.SecretKey;

/**
 * @author mike
 */
public interface SecretKeyToken extends SecurityToken {
    SecretKey getSecretKey();
}
