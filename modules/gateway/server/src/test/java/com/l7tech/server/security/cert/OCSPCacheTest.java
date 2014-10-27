package com.l7tech.server.security.cert;

import com.l7tech.server.util.TestingHttpClientFactory;
import com.l7tech.util.MockConfig;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doThrow;

/**
 * Unit tests for OCSPCache
 *
 * @author Jamie Williams - jamie.williams2@ca.com
 */
@RunWith(MockitoJUnitRunner.class)
public class OCSPCacheTest {

    @Mock
    private X509Certificate poorlyEncodedCertificate;

    @Test
    public void testGetOCSPStatus_GivenPoorlyFormedSubjectCertificate_OCSPClientExceptionThrown() throws Exception {
        OCSPCache cache = new OCSPCache(new TestingHttpClientFactory(), new MockConfig(new Properties()));

        doThrow(new CertificateEncodingException()).when(poorlyEncodedCertificate).getEncoded();

        try {
            cache.getOCSPStatus(null, poorlyEncodedCertificate, null, null, null);
        } catch (OCSPClient.OCSPClientException e) {
            assertEquals("Error processing certificate []", e.getMessage());
        }
    }
}
