package com.l7tech.server.security.cert;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeUnit;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.ocsp.*;
import static org.junit.Assert.*;
import org.junit.Test;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Vector;

/**
 * Unit test for OCSPClient
 */
public class OCSPClientTest {

    @Test
    public void testOcspClientBasic() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = buildClientWithMockResponse( subjectCert, null, null, false );
        OCSPClient.OCSPStatus status = client.getRevocationStatus( subjectCert, false, true );
        assertEquals( "Not revoked", CertificateValidationResult.OK, status.getResult() );
    }

    @Test(expected = OCSPClient.OCSPClientException.class)
    public void testOcspClientInvalidResponseSignature() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = buildClientWithMockResponse( subjectCert, null, null, true );
        OCSPClient.OCSPStatus status = client.getRevocationStatus( subjectCert, false, true );
        assertEquals( "Not revoked", CertificateValidationResult.OK, status.getResult() );
    }

    @Test
    public void testOcspClientNonce() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = buildClientWithMockResponse( subjectCert, new byte[]{ (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8 }, new byte[]{ (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8 }, false );
        OCSPClient.OCSPStatus status = client.getRevocationStatus( subjectCert, true, true );
        assertEquals( "Not revoked", CertificateValidationResult.OK, status.getResult() );
    }

    @Test( expected = OCSPClient.OCSPClientException.class )
    public void testOcspClientNonceFailure() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = buildClientWithMockResponse( subjectCert, new byte[]{ (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 8 }, new byte[]{ (byte) 1, (byte) 2, (byte) 3, (byte) 4, (byte) 5, (byte) 6, (byte) 7, (byte) 9 }, false );
        client.getRevocationStatus( subjectCert, true, true );
        fail( "Expected nonce mismatch exception." );
    }

    @Test( expected = OCSPClient.OCSPClientException.class )
    public void testOcspClientStaleResponse() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        final long timeNow = System.currentTimeMillis();
        OCSPClient client = buildClientWithMockResponse( subjectCert, null, null, false, new Date(timeNow), new Date(timeNow - TimeUnit.DAYS.toMillis( 40L )), null );
        client.getRevocationStatus( subjectCert, false, true );
        fail( "Expected stale response exception." );
    }

    @BugNumber(10576)
    @Test( expected = OCSPClient.OCSPClientException.class )
    public void testOcspClientStaleResponseNextUpdateDueExact() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        final long timeNow = System.currentTimeMillis();
        OCSPClient client = buildClientWithMockResponse( subjectCert, null, null, false, new Date(timeNow), new Date(timeNow - TimeUnit.DAYS.toMillis( 1L )), new Date(timeNow) );
        client.getRevocationStatus( subjectCert, false, true );
        fail( "Expected stale response exception." );
    }

    @BugNumber(10576)
    @Test( expected = OCSPClient.OCSPClientException.class )
    public void testOcspClientStaleResponseNextUpdatePast() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        final long timeNow = System.currentTimeMillis();
        OCSPClient client = buildClientWithMockResponse( subjectCert, null, null, false, new Date(timeNow), new Date(timeNow - TimeUnit.DAYS.toMillis( 1L )), new Date(timeNow - 60000L) );
        client.getRevocationStatus( subjectCert, false, true );
        fail( "Expected stale response exception." );
    }

    @BugNumber(10576)
    @Test
    public void testOcspClientStaleResponseNextUpdateFuture() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        final long timeNow = System.currentTimeMillis();
        OCSPClient client = buildClientWithMockResponse( subjectCert, null, null, false, new Date(timeNow), new Date(timeNow - TimeUnit.DAYS.toMillis( 1L )), new Date(timeNow + 10000L) );
        OCSPClient.OCSPStatus status = client.getRevocationStatus( subjectCert, false, true );
        assertEquals( "Not revoked", CertificateValidationResult.OK, status.getResult() );
    }

    private OCSPClient buildClientWithMockResponse( final X509Certificate subjectCert,
                                                    final byte[] responseNonceBytes,
                                                    final byte[] requestNonceBytes,
                                                    final boolean breakResponseSignature ) throws Exception
    {
        return buildClientWithMockResponse( subjectCert, responseNonceBytes, requestNonceBytes, breakResponseSignature, null, new Date( ), null );
    }

    private OCSPClient buildClientWithMockResponse( final X509Certificate subjectCert,
                                                    final byte[] responseNonceBytes,
                                                    final byte[] requestNonceBytes,
                                                    final boolean breakResponseSignature,
                                                    final Date timeNow,
                                                    final Date thisUpdate,
                                                    final Date nextUpdate ) throws Exception
    {
        Pair<X509Certificate, PrivateKey> ik = makeIssuerCert();
        final PrivateKey issuerKey = ik.right;
        final X509Certificate issuerCert = ik.left;

        String sha1rsaprovider = Signature.getInstance("SHA1WithRSA").getProvider().getName();
        BasicOCSPRespGenerator responseGenerator = new BasicOCSPRespGenerator( new RespID( issuerCert.getSubjectX500Principal() ) );
        CertificateID certId = new CertificateID( OCSPClient.buildCertID( issuerCert, subjectCert.getSerialNumber() ) );
        responseGenerator.addResponse( certId, CertificateStatus.GOOD, thisUpdate, nextUpdate, null );
        if ( responseNonceBytes != null ) {
            final Vector<DERObjectIdentifier> oids = new Vector<DERObjectIdentifier>();
            final Vector<X509Extension> values = new Vector<X509Extension>();
            oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            values.add(new X509Extension(false, new DEROctetString(responseNonceBytes)));
            responseGenerator.setResponseExtensions(new X509Extensions(oids, values));
        }
        BasicOCSPResp response = responseGenerator.generate( "SHA1WithRSA", issuerKey, new X509Certificate[]{ issuerCert }, thisUpdate, sha1rsaprovider );
        OCSPRespGenerator respGen = new OCSPRespGenerator();
        OCSPResp resp = respGen.generate( 0, response );
        byte[] body = resp.getEncoded();
        if (breakResponseSignature)
            body[body.length / 3]++;

        HttpHeader[] responseHeaders = new HttpHeader[]{
                new GenericHttpHeader("Content-Type", "application/octet-stream"),
        };
        GenericHttpHeaders headers = new GenericHttpHeaders(responseHeaders);
        MockGenericHttpClient mockClient =  new MockGenericHttpClient(200,
                                     headers,
                                     ContentTypeHeader.parseValue( "application/octet-stream" ),
                                     (long)body.length,
                                     body);

        OCSPClient.OCSPCertificateAuthorizer authorizer = new OCSPClient.OCSPCertificateAuthorizer(){
            @Override
            public X509Certificate getAuthorizedSigner( final OCSPClient client, final X509Certificate[] certificates ) {
                return issuerCert;
            }
        };

        if ( requestNonceBytes != null ) {
            return new OCSPClient( mockClient, "http://mocked/ocspresponder", issuerCert, authorizer ){
                @Override
                byte[] generateNonce() {
                    return requestNonceBytes;
                }

                @Override
                long currentTimeMillis() {
                    return timeNow==null ? System.currentTimeMillis() : timeNow.getTime();
                }
            };
        } else {
            return new OCSPClient( mockClient, "http://mocked/ocspresponder", issuerCert, authorizer );
        }
    }

    private Pair<X509Certificate, PrivateKey> makeIssuerCert() throws GeneralSecurityException {
        return new TestCertificateGenerator().extKeyUsage(true, Arrays.asList(KeyPurposeId.id_kp_OCSPSigning.getId())).generateWithKey();
    }
}
