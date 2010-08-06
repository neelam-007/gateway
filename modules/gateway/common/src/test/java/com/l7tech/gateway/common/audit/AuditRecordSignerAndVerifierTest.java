package com.l7tech.gateway.common.audit;

import com.l7tech.common.TestKeys;
import com.l7tech.security.cert.KeyUsageException;
import com.l7tech.util.Pair;
import org.junit.Test;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.*;

/**
 * Unit test for AuditRecordSigner and AuditRecordVerifier.
 */
public class AuditRecordSignerAndVerifierTest {

    @Test(expected = NullPointerException.class)
    public void testSignerWithNoKey() {
        new AuditRecordSigner(null);
    }

    @Test(expected = NullPointerException.class)
    public void testVerifierWithNoKey() {
        new AuditRecordVerifier(null);
    }


    @Test
    public void testSignatureRsa2048() throws Exception {
        doTestSignature(TestKeys.getCertAndKey("RSA_2048"));
    }

    @Test
    public void testSignatureRsa1024() throws Exception {
        doTestSignature(TestKeys.getCertAndKey("RSA_1024"));
    }

    @Test
    public void testSignatureEcSecp384r1() throws Exception {
        doTestSignature(TestKeys.getCertAndKey("EC_secp384r1"));
    }

    @Test
    public void testSignatureEcSecp256r1() throws Exception {
        doTestSignature(TestKeys.getCertAndKey("EC_secp256r1"));
    }

    void doTestSignature(Pair<X509Certificate, PrivateKey> k) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException, KeyUsageException, CertificateParsingException {
        AuditRecord rec = AuditRecordTest.makeMessageAuditRecord();
        rec.setMessage("Orig Message");

        AuditRecordSigner signer = new AuditRecordSigner(k.right);
        AuditRecordVerifier verifier = new AuditRecordVerifier(k.left);

        signer.signAuditRecord(rec);
        assertNotNull(rec.getSignature());

        assertTrue("Signature must be valid after it is created",
                verifier.verifySignatureOfDigest(rec.getSignature(), rec.computeSignatureDigest()));

        rec.setMessage("Altered Message");

        assertFalse("Signature must be invalid after the record is modified",
                verifier.verifySignatureOfDigest(rec.getSignature(), rec.computeSignatureDigest()));

        rec.setMessage("Orig Message");

        assertTrue("Signature must be valid after it is restored",
                verifier.verifySignatureOfDigest(rec.getSignature(), rec.computeSignatureDigest()));
    }
}
