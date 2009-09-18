package com.l7tech.server.security.cert;

import org.junit.Test;
import org.junit.Assert;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.OCSPRespGenerator;
import org.bouncycastle.ocsp.BasicOCSPRespGenerator;
import org.bouncycastle.ocsp.RespID;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.CertificateStatus;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import com.l7tech.security.MockGenericHttpClient;
import com.l7tech.security.types.CertificateValidationResult;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.TestDocuments;

import java.security.cert.X509Certificate;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.Date;
import java.util.Vector;

/**
 * Unit test for OCSPClient
 */
public class OCSPClientTest {

    @Test
    public void testOcspClientBasic() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = bulidClientWithMockResponse( subjectCert, null, null );
        OCSPClient.OCSPStatus status = client.getRevocationStatus( subjectCert, false, true );
        Assert.assertEquals( "Not revoked", CertificateValidationResult.OK, status.getResult() );
    }

    @Test
    public void testOcspClientNonce() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = bulidClientWithMockResponse( subjectCert, new byte[]{ 1,2,3,4,5,6,7,8 }, new byte[]{ 1,2,3,4,5,6,7,8 } );
        OCSPClient.OCSPStatus status = client.getRevocationStatus( subjectCert, true, true );
        Assert.assertEquals( "Not revoked", CertificateValidationResult.OK, status.getResult() );
    }

    @Test( expected = OCSPClient.OCSPClientException.class )
    public void testOcspClientNonceFailure() throws Exception {
        final X509Certificate subjectCert = TestDocuments.getWssInteropAliceCert();

        OCSPClient client = bulidClientWithMockResponse( subjectCert, new byte[]{ 1,2,3,4,5,6,7,8 }, new byte[]{ 1,2,3,4,5,6,7,9 } );
        client.getRevocationStatus( subjectCert, true, true );
        Assert.fail( "Expected nonce mismatch exception." );
    }

    private OCSPClient bulidClientWithMockResponse( final X509Certificate subjectCert,
                                                    final byte[] responseNonceBytes,
                                                    final byte[] requestNonceBytes ) throws Exception {
        final PrivateKey issuerKey = TestDocuments.getWssInteropBobKey();
        final X509Certificate issuerCert = TestDocuments.getWssInteropBobCert();

        String sha1rsaprovider = Signature.getInstance("SHA1WithRSA").getProvider().getName();
        BasicOCSPRespGenerator responseGenerator = new BasicOCSPRespGenerator( new RespID( issuerCert.getSubjectX500Principal() ) );
        CertificateID certId = new CertificateID( OCSPClient.buildCertID( issuerCert, subjectCert.getSerialNumber() ) );
        responseGenerator.addResponse( certId, CertificateStatus.GOOD );
        if ( responseNonceBytes != null ) {
            final Vector<DERObjectIdentifier> oids = new Vector<DERObjectIdentifier>();
            final Vector<X509Extension> values = new Vector<X509Extension>();
            oids.add(OCSPObjectIdentifiers.id_pkix_ocsp_nonce);
            values.add(new X509Extension(false, new DEROctetString(responseNonceBytes)));
            responseGenerator.setResponseExtensions(new X509Extensions(oids, values));
        }
        BasicOCSPResp response = responseGenerator.generate( "SHA1WithRSA", issuerKey, new X509Certificate[]{ issuerCert }, new Date(), sha1rsaprovider );
        OCSPRespGenerator respGen = new OCSPRespGenerator();
        OCSPResp resp = respGen.generate( 0, response );
        byte[] body = resp.getEncoded();

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
            };
        } else {
            return new OCSPClient( mockClient, "http://mocked/ocspresponder", issuerCert, authorizer );
        }
    }
}
