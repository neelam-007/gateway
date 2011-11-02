/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.security.prov.bc;

import com.l7tech.security.prov.JceProvider;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jetbrains.annotations.NotNull;

import javax.crypto.spec.IvParameterSpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;

/**
 * BouncyCastle-specific JCE provider engine.
 */
public class BouncyCastleJceProviderEngine extends JceProvider {
    protected final Provider PROVIDER = new BouncyCastleProvider();

    // A GCM IV full of zero bytes, for sanity checking IVs
    private static final byte[] ZERO_IV = new byte[12];

    public BouncyCastleJceProviderEngine() {
        Security.addProvider(PROVIDER);
    }

    @Override
    public String getDisplayName() {
        return PROVIDER.toString();
    }

    @Override
    public AlgorithmParameterSpec generateAesGcmParameterSpec(int authTagLenBytes, @NotNull byte[] iv) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException {
        // This IvParameterSpec will work generically with any provider that will initialize AES-GCM using a plain old IvParameterSpec,
        // and which in that situation defaults to GCM with a 16-byte auth tag and zero bytes of additional authenticated data.

        if (authTagLenBytes != 16)
            throw new InvalidAlgorithmParameterException("GCM auth tag length must be 16 bytes when using this crypto provider");
        if (iv.length != 12)
            throw new InvalidAlgorithmParameterException("GCM IV must be exactly 12 bytes long");
        if (Arrays.equals(ZERO_IV, iv))
            throw new InvalidAlgorithmParameterException("GCM IV is entirely zero octets");

        IvParameterSpec ret = new IvParameterSpec(iv);

        // Sanity check IV before returning
        // TODO remove these sanity checks after adequate testing has been done
        final byte[] gotIV = ret.getIV();
        if (gotIV.length != 12)
            throw new InvalidAlgorithmParameterException("IV in GCM spec is the wrong length: expected 12 bytes, found " + gotIV.length);
        if (!Arrays.equals(iv, gotIV))
            throw new InvalidAlgorithmParameterException("IV in GCM spec does not match requested IV");

        return ret;
    }
}