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
public interface X509SecurityToken extends SigningSecurityToken {
    X509Certificate asX509Certificate();

    /**
     * @return the certificate that was used to sign one or more elements of the message that contained this token
     */
    X509Certificate getMessageSigningCertificate();
}
