package com.l7tech.security.cert;

import com.l7tech.util.ConfigFactory;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.common.io.WhirlycacheFactory;
import com.l7tech.common.io.CertUtils;
import com.whirlycott.cache.Cache;

import java.security.cert.X509Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.*;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class CertVerifier {
    private static final Logger logger = Logger.getLogger(CertVerifier.class.getName());

    private static final String PROPBASE = CertUtils.class.getName();
    public static final int CERT_VERIFY_CACHE_MAX = ConfigFactory.getIntProperty( PROPBASE + ".certVerifyCacheSize", 500 );// Map of VerifiedCert => Boolean.TRUE

    public static final Cache certVerifyCache =
            WhirlycacheFactory.createCache("certCache",
                    CERT_VERIFY_CACHE_MAX,
                    127, WhirlycacheFactory.POLICY_LRU
            );

    /**
     * Test if the specified certificate is verifiable with the specified public key, without throwing
     * any checked exceptions.
     * <p/>
     * This makes use of the CertUtils certificate verification cache.
     *
     * @param cert  the certificate to check.  Required.
     * @param signingcert the CA cert that is expected to have signed the subject cert.  Required.
     * @return  true iff. the specified cert verifies successfully with the specified public key
     */
    public static boolean isVerified(X509Certificate cert, X509Certificate signingcert) {
        try {
            cachedVerify(cert, signingcert);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Similar behavior to X509Certificate.verify(publicKey), except takes a certificate instead of just the public key, checks key usage, and memoizes the result.
     *
     * @param signedcert  the subject cert to verify.  Required.
     * @param signingcert the CA cert that is expected to have signed the subject cert.  Required.
     * @throws java.security.NoSuchProviderException   if there's no default provider
     * @throws java.security.NoSuchAlgorithmException  on unsupported signature algorithm
     * @throws java.security.SignatureException    on signature error
     * @throws java.security.InvalidKeyException   on incorrect key
     * @throws java.security.cert.CertificateException  on certificate parse error
     */
    public static void cachedVerify(X509Certificate signedcert, X509Certificate signingcert) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        cachedVerify(signedcert, signingcert, null);
    }

    /**
     * Similar behavior to X509Certificate.verify(publicKey), except takes a certificate instead of just the public key, checks key usage, and memoizes the result.
     *
     * @param cert        the subject cert to verify.  Required.
     * @param signingcert the CA cert that is expected to have signed the subject cert.  Required.
     * @param sigProvider JCE provider to use for the verify operation, or null to use the default provider.
     * @throws java.security.NoSuchProviderException   if there's no default provider, or if the specified provider isn't found
     * @throws java.security.NoSuchAlgorithmException  on unsupported signature algorithm
     * @throws java.security.SignatureException    on signature error
     * @throws java.security.InvalidKeyException   on incorrect key
     * @throws java.security.cert.CertificateException  on certificate parse error
     */
    public static void cachedVerify(X509Certificate cert, X509Certificate signingcert, String sigProvider) throws NoSuchProviderException, NoSuchAlgorithmException, SignatureException, InvalidKeyException, CertificateException {
        VerifiedCert vc = new VerifiedCert(cert, signingcert);
        if (vc.isVerified()) {
            if (logger.isLoggable(Level.FINER)) logger.finer("Verified cert signature (cached): " + cert.getSubjectDN().toString());
            return; // cache hit
        }

        KeyUsageChecker.requireActivity(KeyUsageActivity.verifyClientCert, signingcert);
        if (sigProvider == null)
            cert.verify(signingcert.getPublicKey());
        else
            cert.verify(signingcert.getPublicKey(), sigProvider);
        vc.onVerified();
    }

    /**
     * Verifies that each cert in the specified certificate chain is signed by the next certificate
     * in the chain, and that at least one is <em>signed by or identical to</em> trustedCert.
     *
     * @param chain An array of one or more {@link java.security.cert.X509Certificate}s to check
     * @param trustedCert A trusted {@link java.security.cert.X509Certificate} to check the chain against
     * @throws com.l7tech.common.io.CertUtils.CertificateUntrustedException if the chain could not be validated with the specified
     *                                                       trusted certificate, but the chain otherwise appears to
     *                                                       be internally consistent and might validate later if a
     *                                                       different trusted certificate is used.
     * @throws java.security.cert.CertificateExpiredException if one of the certs in the chain has expired
     * @throws java.security.cert.CertificateException if the chain is seriously invalid and cannot be trusted
     */
    public static void verifyCertificateChain(X509Certificate[] chain, X509Certificate trustedCert)
            throws CertificateException, CertUtils.CertificateUntrustedException
    {
        if (chain == null || chain.length < 1)
            throw new CertificateException("Couldn't find trusted certificate [" +
                    CertUtils.getCertIdentifyingInformation(trustedCert) + "] in peer's certificate chain: " +
                    "certificate chain is null or empty");

        if (CertUtils.certsAreEqual(chain[0], trustedCert))
            return; // success

        for (int i = 1; i < chain.length; ++i) {
            X509Certificate cert = chain[i - 1];
            X509Certificate caCert = chain[i];

            boolean haveConstraints = caCert.getExtensionValue(CertUtils.X509_OID_BASIC_CONSTRAINTS) != null;
            int pathlen = haveConstraints ? caCert.getBasicConstraints() : CertUtils.DEFAULT_X509V1_MAX_PATH_LENGTH;

            if (pathlen < 0)
                throw new CertificateException("CA certificate [" + CertUtils.getCertIdentifyingInformation(caCert) + "] " +
                        "at position " + i + " in certificate path contains basic constraints disallowing " +
                        "use as a CA certificate");

            int numIntermediateCaCerts = i - 1;
            if (numIntermediateCaCerts > pathlen)
                throw new CertificateException("Path length constraint exceeded: CA certificate [" +
                        CertUtils.getCertIdentifyingInformation(caCert) + "] at position " + i + " in certificate " +
                        "path contains basic constraints disallowing use with more than " + pathlen +
                        " intermediate CA certificates");

            try {
                cachedVerify(cert, caCert);
            } catch (GeneralSecurityException e) {
                throw new CertificateException("Unable to verify certificate [" +
                        CertUtils.getCertIdentifyingInformation(cert) +
                        "] signature in peer certificate chain: " + ExceptionUtils.getMessage(e), e);
            }

            if (CertUtils.certsAreEqual(caCert, trustedCert))
                return; // Success
        }

        // We probably just haven't talked to this Ssg before.  Trigger a reimport of the certificate.
        throw new CertUtils.CertificateUntrustedException("Couldn't find trusted certificate [" +
                CertUtils.getCertIdentifyingInformation(trustedCert) + "] in peer's certificate chain");
    }

    public static class VerifiedCert {
        final byte[] certBytes;
        final byte[] caCertBytes;
        final int hashCode;

        public VerifiedCert(X509Certificate cert, X509Certificate caCert) throws CertificateEncodingException {
            this(cert.getEncoded(), caCert.getEncoded());
        }

        public VerifiedCert(byte[] certBytes, byte[] caCertBytes) {
            this.certBytes = certBytes;
            this.caCertBytes = caCertBytes;
            this.hashCode = makeHashCode();
        }

        /** @noinspection RedundantIfStatement*/
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final VerifiedCert that = (VerifiedCert)o;

            if (!Arrays.equals(certBytes, that.certBytes)) return false;
            if (!Arrays.equals(caCertBytes, that.caCertBytes)) return false;

            return true;
        }

        private int makeHashCode() {
            int c = 7;
            c += 17 * Arrays.hashCode(certBytes);
            c += 29 * Arrays.hashCode(caCertBytes);
            return c;
        }

        public int hashCode() {
            return hashCode;
        }

        /** @return true if this cert has already been verified with this public key. */
        public boolean isVerified() {
            final Object got;
            got = certVerifyCache.retrieve(this);
            return got instanceof Boolean && (Boolean)got;
        }

        /** Report that this cert was successfully verified with its public key. */
        public void onVerified() {
            certVerifyCache.store(this, Boolean.TRUE);
        }
    }
}
