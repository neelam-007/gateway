/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import java.security.cert.X509Certificate;

/**
 * @author mike
 */
public interface X509SecurityToken extends SecurityToken {
    X509Certificate asX509Certificate();
    boolean isPossessionProved();
}
