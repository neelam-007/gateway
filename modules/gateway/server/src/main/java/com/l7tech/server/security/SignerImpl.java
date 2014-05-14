package com.l7tech.server.security;

import com.l7tech.policy.assertion.ext.security.Signer;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.security.*;

public class SignerImpl implements Signer {
    private static final String DEFAULT_HASH_ALGORITHM_NAME = "SHA-1";

    private final PrivateKey privateKey;

    public SignerImpl(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public byte[] createSignature(String hashAlgorithmName, InputStream dataToSign) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, IOException {
        // Compute signature algorithm.
        //
        String sigAlg = this.computeSignatureAlgorithm(hashAlgorithmName);

        // Create Signature.
        // The following method calls will throw appropriate exceptions if signature algorithm is invalid.
        //
        Signature sig = JceProvider.getInstance().getSignature(sigAlg);
        sig.initSign(privateKey);
        sig.update(IOUtils.slurpStream(dataToSign));

        return sig.sign();
    }

    /**
     * Compute signature algorithm using the given hash algorithm and this signer's key.
     *
     * @param hashAlgorithmName the hash algorithm
     * @return the signature algorithm
     */
    private String computeSignatureAlgorithm(String hashAlgorithmName) {
        String hashAlg;
        if (hashAlgorithmName == null || hashAlgorithmName.trim().isEmpty()) {
            hashAlg = DEFAULT_HASH_ALGORITHM_NAME;
        } else {
            hashAlg = hashAlgorithmName;
        }

        String keyAlg = privateKey.getAlgorithm();
        if ("EC".equalsIgnoreCase(keyAlg) ||
            "ECC".equalsIgnoreCase(keyAlg)) {
            keyAlg = "ECDSA";
        }

        String sigAlg = hashAlg + "with" + keyAlg;
        sigAlg = sigAlg.replaceAll("\\-", "");

        return sigAlg;
    }
}