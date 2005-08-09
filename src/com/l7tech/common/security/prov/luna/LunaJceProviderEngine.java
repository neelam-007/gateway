/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.prov.luna;

import com.chrysalisits.cryptox.LunaJCEProvider;
import com.l7tech.common.security.CertificateRequest;
import com.l7tech.common.security.JceProviderEngine;
import com.l7tech.common.security.RsaSignerEngine;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.security.*;

/**
 * @author mike
 */
public class LunaJceProviderEngine implements JceProviderEngine {
    private final Provider PROVIDER = new LunaJCEProvider();
    private final String PROVNAME = PROVIDER.getName();

    public Provider getAsymmetricProvider() {
        return PROVIDER;
    }

    public Provider getSymmetricProvider() {
        return PROVIDER;
    }

    public RsaSignerEngine createRsaSignerEngine(String keyStorePath, String storePass, String privateKeyAlias, String privateKeyPass, String storeType) {
        throw new UnsupportedOperationException("certificate signing support currently not implemented for Luna");
    }

    public KeyPair generateRsaKeyPair() {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA", PROVNAME);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Luna JCE provider misconfigured: " + e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException("Luna JCE provider misconfigured: " + e.getMessage(), e);
        }
        kpg.initialize(RSA_KEY_LENGTH);
        KeyPair kp = kpg.generateKeyPair();
        return kp;
    }

    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        throw new UnsupportedOperationException("certificate signing request support currently not implemented for Luna");
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/NoPadding", PROVNAME);
    }
}
