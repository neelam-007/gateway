/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.luna;

import com.chrysalisits.cryptox.LunaJCEProvider;
import com.l7tech.security.prov.CertificateRequest;
import com.l7tech.security.prov.JceProviderEngine;
import com.l7tech.security.prov.RsaSignerEngine;

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
        return new LunaRsaSignerEngine(privateKeyAlias);
    }

    public KeyPair generateRsaKeyPair(int len) {
        KeyPairGenerator kpg = null;
        try {
            kpg = KeyPairGenerator.getInstance("RSA", PROVNAME);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Luna JCE provider misconfigured: " + e.getMessage(), e);
        } catch (NoSuchProviderException e) {
            throw new RuntimeException("Luna JCE provider misconfigured: " + e.getMessage(), e);
        }
        kpg.initialize(len);
        return kpg.generateKeyPair();
    }

    public KeyPair generateRsaKeyPair() {
        return generateRsaKeyPair(RSA_KEY_LENGTH);
    }

    public CertificateRequest makeCsr(String username, KeyPair keyPair) throws InvalidKeyException, SignatureException {
        throw new UnsupportedOperationException("LunaJceProviderEngine is unable to create new Certificate Signing Request using Luna KeyPair: Unsupported operation");
    }

    public Cipher getRsaNoPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/NoPadding", PROVNAME);
    }

    public Cipher getRsaOaepPaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/OAEPPadding", PROVIDER.getName());
    }

    public Cipher getRsaPkcs1PaddingCipher() throws NoSuchProviderException, NoSuchAlgorithmException, NoSuchPaddingException {
        return Cipher.getInstance("RSA/NONE/PKCS1Padding", PROVIDER.getName());
    }
}
