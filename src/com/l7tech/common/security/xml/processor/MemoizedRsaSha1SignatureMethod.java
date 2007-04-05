/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.security.xml.processor;

import com.ibm.xml.dsig.SignatureMethod;
import com.l7tech.common.security.JceProvider;
import com.l7tech.common.util.WhirlycacheFactory;
import com.l7tech.common.util.SyspropUtil;
import com.whirlycott.cache.Cache;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.x509.DigestInfo;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * A wrapper for xss4j rsa-sha1 impl that notices if it is verifying the exact same signature this process
 * has already verified once before recently, and if so, skips the RSA step.
 */
public class MemoizedRsaSha1SignatureMethod extends SignatureMethod {
    private static final Logger logger = Logger.getLogger(MemoizedRsaSha1SignatureMethod.class.getName());
    private static final String PROPBASE = MemoizedRsaSha1SignatureMethod.class.getName();
    private static final int SIG_VERIFY_CACHE_MAX = SyspropUtil.getInteger(PROPBASE + ".sigVerifyCacheSize", 1000).intValue();

    private static final ThreadLocal tlSha1 = new ThreadLocal() {
        protected Object initialValue() {
            try {
                return MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e); // can't happen, misconfigured VM
            }
        }
    };

    //private static final LRUMap sigCache = new LRUMap(SIG_VERIFY_CACHE_MAX);
    private static final Cache sigCache = WhirlycacheFactory.createCache("sigCache", SIG_VERIFY_CACHE_MAX,
                                                                         131, WhirlycacheFactory.POLICY_LRU);

    private static class CachedSignature {
        final byte[] digestBytes;
        final byte[] publicKeyBytes;
        final int hashCode;

        public CachedSignature(byte[] digestBytes, byte[] publicKeyBytes) {
            this.digestBytes = digestBytes;
            this.publicKeyBytes = publicKeyBytes;
            this.hashCode = makeHashCode();
        }

        private int makeHashCode() {
            // are the first 4 bytes of the sha1 digest hashy enough for ya, punk?
            final byte[] b = digestBytes;
            return b[0] +
                   b[1] * 256 +
                   b[2] * 256 * 256 +
                   b[3] * 256 * 256 * 256 +
                   Arrays.hashCode(publicKeyBytes) * 17;
        }

        public int hashCode() {
            return hashCode;
        }

        /** @noinspection RedundantIfStatement*/
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final CachedSignature that = (CachedSignature)o;

            if (!Arrays.equals(digestBytes, that.digestBytes)) return false;
            if (!Arrays.equals(publicKeyBytes, that.publicKeyBytes)) return false;

            return true;
        }

        /** @return true if this signature hash has already been verified with this public key. */
        public boolean isVerified() {
            final Object got;
            got = sigCache.retrieve(this);
            return got instanceof Boolean && ((Boolean)got).booleanValue();
        }

        /** Report that this signature hash was successfully verified with its public key. */
        public void onVerified() {
            sigCache.store(this, Boolean.TRUE);
        }
    }

    private final SignatureMethod delegate;
    private boolean signing = false;
    private RSAPublicKey verifyKey;
    private MessageDigest sha1 = null;


    public MemoizedRsaSha1SignatureMethod(SignatureMethod delegate) {
        this.delegate = delegate;
    }

    public SignatureMethod getDelegate() {
        return delegate;
    }

    public String getURI() {
        return delegate.getURI();
    }

    public void initSign(Key key) throws InvalidKeyException {
        delegate.initSign(key);
        signing = true;
        sha1 = null;
    }

    public void initVerify(Key key) throws InvalidKeyException {
        signing = false;
        sha1 = (MessageDigest)tlSha1.get();
        sha1.reset();
        if (key == null) throw new InvalidKeyException("Verification key must be provided");
        if (!(key instanceof RSAPublicKey))
            throw new InvalidKeyException("Verifying an rsa-sha1 signature requires an RSA public key (provided=" + key.getClass().getName() + ")");

        // Defer creating the cipher until we find out whether we miss the sig cache or not
        verifyKey = (RSAPublicKey)key;
    }

    public void update(byte[] bytes) throws SignatureException {
        if (signing) {
            delegate.update(bytes);
            return;
        }

        assert sha1 != null;
        sha1.update(bytes);
    }

    public byte[] sign() throws SignatureException {
        return delegate.sign();
    }

    public boolean verify(byte[] bytes) throws SignatureException {
        byte[]  hash = sha1.digest();

        CachedSignature cached = new CachedSignature(hash, verifyKey.getEncoded());
        if (cached.isVerified()) {
            return true; // cache hit
        }

        // Oh well, we'll have to do the work
        try {
            Cipher rsa = JceProvider.getRsaPkcs1PaddingCipher();
            rsa.init(Cipher.DECRYPT_MODE, verifyKey);
            byte[] result = rsa.doFinal(bytes);

            ByteArrayInputStream    bIn = new ByteArrayInputStream(result);
            DERInputStream          dIn = new DERInputStream(bIn);

            DigestInfo digInfo = new DigestInfo((ASN1Sequence)dIn.readObject());

            // TODO fix this comparison
//            if (!digInfo.getAlgorithmId().equals(X509ObjectIdentifiers.id_SHA1))
//                throw new SignatureException("Decoded digest algorithm identifier was not rsa-sha1 (1.3.14.3.2.26)");

            byte[] sigHash = digInfo.getDigest();

            if (hash.length != sigHash.length)
                throw new SignatureException("Signature value length mismatch (expected " + hash.length + "; got " + sigHash.length);

            boolean validated = Arrays.equals(hash, sigHash);
            if (validated) cached.onVerified();
            return validated;

        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        } catch (NoSuchPaddingException e) {
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            throw new SignatureException(e);
        } catch (IOException e) {
            throw new SignatureException(e);
        } catch (BadPaddingException e) {
            throw new SignatureException(e);
        } catch (IllegalBlockSizeException e) {
            throw new SignatureException(e);
        } catch (NoSuchProviderException e) {
            throw new SignatureException(e);
        }
    }
}
