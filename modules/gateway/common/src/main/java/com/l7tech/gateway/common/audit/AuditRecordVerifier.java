package com.l7tech.gateway.common.audit;

import com.l7tech.security.cert.KeyUsageActivity;
import com.l7tech.security.cert.KeyUsageChecker;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.util.HexUtils;

import java.security.*;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECKey;
import java.util.logging.Logger;

/**
 * Utility class for verifying audit record signatures.
 */
public class AuditRecordVerifier {
    private static Logger logger = Logger.getLogger(AuditRecordVerifier.class.getName());

    private final X509Certificate signerCertificate;

    /**
     * Create a verifier that will expect audit records to have been signed using the specified signer certificate.
     *
     * @param signerCertificate the signer's certificate.  Must allow the verifyXml activity under the currently-in-effect key usage enforcement policy. Required.
     */
    public AuditRecordVerifier(X509Certificate signerCertificate) {
        if (signerCertificate == null)
            throw new NullPointerException("signerCertificate");
        this.signerCertificate = signerCertificate;
    }

    /**
     * Attempt to verify that the specified recently-recomputed AuditRecord digest matches that signed into the
     * specified signatureToVerify using the specified X.509 certificate's public key.
     *
     * @param signatureToVerify   the audit record signature to verify (encoded in Base-64).  Must be non-null and non-empty.
     * @param currentDigestValue  the current digest value of the audit record in question.  Required.
     *                            <p/>
     *                            This shall have been recently computed freshly from the complete audit record data, rather than being
     *                            read out of whatever storage media the signature value came from.  (That is, checking
     *                            a signature against a digest that came from the same database table as the signature is pointless.)
     * @return true if the signature is valid.  False if the signature appears to be in the correct format but cannot be confirmed to be valid.
     * @throws KeyUsageException  if the current key usage enforcement policy does not permit use of the specified certificate for the verifyXml activity.
     * @throws java.security.cert.CertificateParsingException if there is an error parsing a critical Extended Key Usage extension
     * @throws NoSuchAlgorithmException if a required signature algorithm is not available in the current environment.
     * @throws InvalidKeyException if the public key from the specified certificate cannot be used to verify this signature.
     * @throws SignatureException if signature verification fails for some other reason.
     */
    public boolean verifySignatureOfDigest(String signatureToVerify, byte[] currentDigestValue)
            throws KeyUsageException, CertificateParsingException, NoSuchAlgorithmException, InvalidKeyException, SignatureException
    {
        PublicKey pub = signerCertificate.getPublicKey();
        KeyUsageChecker.requireActivity(KeyUsageActivity.verifyXml, signerCertificate);
        byte[] decodedSig = HexUtils.decodeBase64(signatureToVerify);
        boolean isEcc = pub instanceof ECKey || "EC".equals(pub.getAlgorithm());
        Signature sig = Signature.getInstance(isEcc ? "SHA512withECDSA" : "NONEwithRSA");
        sig.initVerify(pub);
        sig.update(currentDigestValue);
        return sig.verify(decodedSig);
    }
}
