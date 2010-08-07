package com.l7tech.gateway.common.audit;

import com.l7tech.security.prov.JceProvider;
import com.l7tech.util.HexUtils;

import java.security.*;
import java.security.interfaces.ECKey;

/**
 * Utility class for generating audit record signatures.
 */
public class AuditRecordSigner {

    private final PrivateKey privateKey;

    /**
     * Create a signer that will sign audit records with the specified private key.
     *
     * @param privateKey the private key to use to sign audit records. May be either an RSA or an EC key.  Required.
     */
    public AuditRecordSigner(PrivateKey privateKey) {
        if (privateKey == null)
            throw new NullPointerException("privateKey");
        this.privateKey = privateKey;
    }

    /**
     * Attempt to sign the specified audit record using the specified private key.
     * <p/>
     * If this method returns normally, the signature was created successfully and the signature field
     * of the audit record has been replaced by the newly computed value (Base-64 encoded).
     *
     * @param auditRecord  the audit record to sign. Required.
     * @throws NoSuchAlgorithmException if the required Signature implementation is currently available for keys of the specified type.
     * @throws InvalidKeyException if the specified key cannot be used for signing this record.
     * @throws SignatureException if signing fails for some other reason.
     */
    public void signAuditRecord(AuditRecord auditRecord) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
        byte[] digest = auditRecord.computeSignatureDigest();
        boolean isEcc = privateKey instanceof ECKey || "EC".equals(privateKey.getAlgorithm());
        Signature sig = JceProvider.getInstance().getSignature(isEcc ? "SHA512withECDSA" : "NONEwithRSA");
        sig.initSign(privateKey);
        sig.update(digest);
        String signature = HexUtils.encodeBase64(sig.sign(), true);
        auditRecord.setSignature(signature);
    }
}
