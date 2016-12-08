package com.l7tech.external.assertions.portalupgrade.server;

import com.l7tech.common.TestKeys;
import com.l7tech.util.HexUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for PinChecker class.
 */
@RunWith(MockitoJUnitRunner.class)
public class PinCheckerTest {

    X509Certificate cert1024 = TestKeys.getCertAndKey( "RSA_1024" ).left;
    X509Certificate cert2048 = TestKeys.getCertAndKey( "RSA_2048" ).left;
    PortalUpgradeManager.PinChecker pinChecker = new PortalUpgradeManager.PinChecker( HexUtils.getSha256Digest( cert1024.getPublicKey().getEncoded() ) );
    @Mock
    SSLSession cert1024Session;
    @Mock
    SSLSession cert2048Session;

    @Before
    public void init() throws Exception {
        when( cert1024Session.getPeerCertificates() ).thenReturn( new Certificate[] { cert1024 } );
        when( cert2048Session.getPeerCertificates() ).thenReturn( new Certificate[] { cert2048 } );
    }

    @Test
    public void testCheckServerTrustedMatchingPin() throws Exception {
        pinChecker.checkServerTrusted( new X509Certificate[] { cert1024 }, "RSA" );
    }

    @Test( expected = CertificateException.class )
    public void testCheckServerTrustedBrokenPin() throws Exception {
        pinChecker.checkServerTrusted( new X509Certificate[] { cert2048 }, "RSA" );
    }

    @Test
    public void testVerifyHostnameMatchingPin() throws Exception {
        assertTrue( pinChecker.verify( "blah.nomatch.example.com", cert1024Session ) );
    }

    @Test
    public void testVerifyHostnameBrokenPin() throws Exception {
        assertFalse( pinChecker.verify( "blah.nomatch.example.com", cert2048Session ) );
    }
}
