/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security.token;

import java.security.cert.X509Certificate;

/**
 * Represents a SecurityToken that can be used to sign elements in a message, and which uses an X.509 certificate's
 * private key as the signature method.
 */
public interface X509SigningSecurityToken extends SigningSecurityToken {
    /**
     * @return the certificate that was used to sign one or more elements of the message that contained this token
     */
    X509Certificate getMessageSigningCertificate();
}
