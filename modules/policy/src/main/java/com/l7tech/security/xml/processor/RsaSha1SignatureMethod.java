package com.l7tech.security.xml.processor;

import com.ibm.xml.dsig.SignatureMethod;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;

/**
 * Provides a means of creating SignatureMethod instances for RSA with SHA-1 using a Signature from an arbitrary provider.
 */
public class RsaSha1SignatureMethod extends SignatureMethod {
    private final Signature signature;
    private final String uri;

    RsaSha1SignatureMethod(Signature signature, String uri) {
        this.signature = signature;
        this.uri = uri;
    }

    @Override
    public String getURI() {
        return uri;
    }

    @Override
    public void initSign(Key privateKey) throws InvalidKeyException {
        signature.initSign((PrivateKey) privateKey);
    }

    @Override
    public void initVerify(Key publicKey) throws InvalidKeyException {
        signature.initVerify((PublicKey) publicKey);
    }

    @Override
    public void update(byte[] data) throws SignatureException {
        signature.update(data);
    }

    @Override
    public byte[] sign() throws SignatureException {
        return signature.sign();
    }

    @Override
    public boolean verify(byte[] signature) throws SignatureException {
        return this.signature.verify(signature);
    }
}
