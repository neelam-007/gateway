package com.l7tech.server.security.cert;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.http.*;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeUnit;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.ocsp.*;
import static org.junit.Assert.*;

import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

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
@RunWith(MockitoJUnitRunner.class)
public class OCSPClientTest {

    private MockGenericHttpClient mockClient;

    @After
    public void tearDown() throws Exception {
        mockClient = null;
    }

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

    @BugId("DE303090")
    @Test
    public void testOcspCloseConnection() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        final OCSPClient client = buildClientWithMockResponse(subjectCert, null, null, false);

        MockGenericHttpClient.MockGenericHttpRequest spyRequest = Mockito.spy(mockClient.new MockGenericHttpRequest());
        MockGenericHttpClient.CreateRequestListener createRequestListener = (method, params, request) -> spyRequest;
        mockClient.setCreateRequestListener(createRequestListener);

        GenericHttpResponse spyResponse = Mockito.spy(mockClient.new MockGenericHttpResponse());
        Mockito.doAnswer(invocationOnMock -> spyResponse).when(spyRequest).getResponse();

        OCSPClient.OCSPStatus status = client.getRevocationStatus(subjectCert, false, false);
        assertEquals("Not revoked", CertificateValidationResult.OK, status.getResult());
        Mockito.verify(spyRequest, Mockito.times(1)).close();
        Mockito.verify(spyResponse, Mockito.times(1)).close();
    }

    @BugId("DE303090")
    @Test
    public void testOcspCloseConnectionWhenIOException() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        final OCSPClient client = buildClientWithMockResponse(subjectCert, null, null, false);

        MockGenericHttpClient.MockGenericHttpRequest spyRequest = Mockito.spy(mockClient.new MockGenericHttpRequest());
        MockGenericHttpClient.CreateRequestListener createRequestListener = (method, params, request) -> spyRequest;
        mockClient.setCreateRequestListener(createRequestListener);

        GenericHttpResponse spyResponse = Mockito.spy(mockClient.new MockGenericHttpResponse());
        Mockito.doThrow(new GenericHttpException()).when(spyResponse).getInputStream();
        Mockito.doAnswer(invocationOnMock -> spyResponse).when(spyRequest).getResponse();

        OCSPClient.OCSPClientException ocspClientException = null;
        try {
            client.getRevocationStatus(subjectCert, false, false);
        } catch (OCSPClient.OCSPClientException e) {
            ocspClientException = e;
        }
        assertNotNull("Expected an OCSPClientException to be thrown", ocspClientException);
        Mockito.verify(spyRequest, Mockito.times(1)).close();
        Mockito.verify(spyResponse, Mockito.times(1)).close();
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
        final Pair<X509Certificate, PrivateKey> ik = makeIssuerCert();
        final PrivateKey issuerKey = ik.right;
        final X509Certificate issuerCert = ik.left;

        final String sha1rsaprovider = Signature.getInstance("SHA1WithRSA").getProvider().getName();
        final BasicOCSPRespBuilder responseBuilder = new BasicOCSPRespBuilder(
                new RespID(
                        X500Name.getInstance(issuerCert.getSubjectX500Principal().getEncoded())));
        final CertificateID certId = new CertificateID(OCSPClient.buildCertID(issuerCert, subjectCert.getSerialNumber()));
        responseBuilder.addResponse(certId, CertificateStatus.GOOD, thisUpdate, nextUpdate, null);
        if ( responseNonceBytes != null ) {
            final Vector<Extension> values = new Vector<>();
            values.add(new Extension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce, false, new DEROctetString(responseNonceBytes)));
            responseBuilder.setResponseExtensions(new Extensions(values.toArray(new Extension[values.size()])));
        }
        final BasicOCSPResp response = responseBuilder.build(
                new JcaContentSignerBuilder("SHA1WithRSA").setProvider(sha1rsaprovider).build(issuerKey),
                new X509CertificateHolder[]{new X509CertificateHolder(issuerCert.getEncoded())},
                thisUpdate );
        final OCSPRespBuilder respBuilder = new OCSPRespBuilder();
        final OCSPResp resp = respBuilder.build(0, response);
        final byte[] body = resp.getEncoded();
        if (breakResponseSignature)
            body[body.length / 3]++;

        HttpHeader[] responseHeaders = new HttpHeader[]{
                new GenericHttpHeader("Content-Type", "application/octet-stream"),
        };
        GenericHttpHeaders headers = new GenericHttpHeaders(responseHeaders);
        mockClient =  new MockGenericHttpClient(200,
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
