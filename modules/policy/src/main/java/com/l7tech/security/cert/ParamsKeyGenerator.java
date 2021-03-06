package com.l7tech.security.cert;

import com.l7tech.common.io.KeyGenParams;
import com.l7tech.security.prov.JceProvider;

import java.security.*;

/**
 * Parameter driven key pair generator that accepts a KeyGenParams instance.
 * <p/>
 * This class is not threadsafe.
 */
public class ParamsKeyGenerator {
    private final KeyGenParams k;
    private final SecureRandom random;

    public ParamsKeyGenerator(KeyGenParams k) {
        this.k = k;
        this.random = null;
    }

    public ParamsKeyGenerator(KeyGenParams k, SecureRandom random) {
        this.k = k;
        this.random = random;
    }

    public KeyPair generateKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        String alg = k.getAlgorithm();
        if (alg == null)
            alg = getDefaultAlgorithm();
        if ("RSA".equals(alg)) {
            return generateRsaKeyPair();
        } else if ("EC".equals(alg)) {
            return generateEcKeyPair();
        } else
            return generateGenericKeyPair();
    }

    protected KeyPair generateEcKeyPair() throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        String curve = k.getNamedParam();
        if (curve == null)
            curve = getDefaultNamedCurve();

        return JceProvider.getInstance().generateEcKeyPair(curve, random);
    }

    protected KeyPair generateRsaKeyPair() throws NoSuchAlgorithmException {
        int keyBits = k.getKeySize();
        if (keyBits < 1)
            keyBits = getDefaultRsaKeySize();

        KeyPairGenerator kpg = JceProvider.getInstance().getKeyPairGenerator("RSA");
        if (random != null) {
            kpg.initialize(keyBits, random);
        } else {
            kpg.initialize(keyBits);
        }
        return kpg.generateKeyPair();
    }

    protected KeyPair generateGenericKeyPair() throws NoSuchAlgorithmException {
        int keyBits = k.getKeySize();
        if (keyBits < 1)
            keyBits = 1024;

        KeyPairGenerator kpg = JceProvider.getInstance().getKeyPairGenerator(k.getAlgorithm());
        if (random != null) {
            kpg.initialize(keyBits, random);
        } else {
            kpg.initialize(keyBits);
        }
        return kpg.generateKeyPair();
    }

    protected String getDefaultAlgorithm() {
        return "RSA";
    }

    protected String getDefaultNamedCurve() {
        return "secp384r1";
    }

    protected int getDefaultRsaKeySize() {
        return JceProvider.DEFAULT_RSA_KEYSIZE;
    }
}
