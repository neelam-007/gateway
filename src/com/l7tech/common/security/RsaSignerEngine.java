/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.security;

import java.security.cert.Certificate;

/**
 * Provides the guts of an RSA Signer, using the current underlying crypto engine.
 * @author mike
 * @version 1.0
 */
public interface RsaSignerEngine {
    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    Certificate createCertificate(byte[] pkcs10req) throws Exception;
}
