/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id: PhaosRsaSignerEngine.java,v 1.1 2003/12/08 20:02:40 mike Exp $
 */

package com.l7tech.common.security.prov.ncipher;

import com.l7tech.common.security.RsaSignerEngine;
import com.l7tech.common.security.prov.bc.BouncyCastleRsaSignerEngine;

import java.security.cert.Certificate;

/**
 *
 * @author mike
 * @version 1.0
 */
public class NcipherRsaSignerEngine implements RsaSignerEngine {
    private BouncyCastleRsaSignerEngine bcengine;

    public NcipherRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass) {
        bcengine = new BouncyCastleRsaSignerEngine( keyStorePath, storePass, privateKeyAlias, privateKeyPass );
    }

    /**
     * Create a certificate from the given PKCS10 Certificate Request.
     *
     * @param pkcs10req  the PKCS10 certificate signing request, expressed in binary form.
     * @return a signed X509 client certificate
     * @throws Exception if something bad happens
     */
    public Certificate createCertificate(byte[] pkcs10req) throws Exception {
        return bcengine.createCertificate( pkcs10req );
    }
}
