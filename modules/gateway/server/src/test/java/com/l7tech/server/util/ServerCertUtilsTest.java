package com.l7tech.server.util;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.cert.TestCertificateGenerator;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.junit.Test;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Unit tests for ServerCertUtils
 *
 * @author Steve Jones
 */
public class ServerCertUtilsTest {

    @Test
    public void testCRLURL() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(GOOGLE_PEM);

        String[] crlUrls = ServerCertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Empty CRL urls", crlUrls.length > 0);
        assertEquals("CRL url missing or invalid", "http://crl.thawte.com/ThawteSGCCA.crl", crlUrls[0]);

    }

    @Test
    @BugNumber(9347)
    public void testCRLURL_distPointMulti() throws Exception {
        X509Certificate certificate = CertUtils.decodeCert(HexUtils.decodeBase64(BUG_9347_CRL_DIST_MULTI_URL));

        String[] crlUrls = ServerCertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "ldap:///CN=Layer%207%20Support,CN=supad1,CN=CDP,CN=Public%20Key%20Services,CN=Services,CN=Configuration,DC=test2003,DC=com?certificateRevocationList?base?objectClass=cRLDistributionPoint", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "http://supad1.test2003.com/CertEnroll/Layer%207%20Support.crl", crlUrls[1]);
    }

    @Test
    @BugNumber(9347)
    public void testCRLURL_oneDistPoint_twoUrls() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator().keySize(512);
        gen.getCertGenParams().setIncludeCrlDistributionPoints(true);
        gen.getCertGenParams().setCrlDistributionPointsUrls(Arrays.asList(Arrays.asList("ldap://ldapone.example.com", "http://httpone.example.com")));

        X509Certificate certificate = gen.generate();

        String[] crlUrls = ServerCertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "ldap://ldapone.example.com", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "http://httpone.example.com", crlUrls[1]);
    }

    @Test
    @BugNumber(9347)
    public void testCRLURL_twoDistPoints_oneUrlEach() throws Exception {
        TestCertificateGenerator gen = new TestCertificateGenerator().keySize(512);
        gen.getCertGenParams().setIncludeCrlDistributionPoints(true);
        gen.getCertGenParams().setCrlDistributionPointsUrls(Arrays.asList(Arrays.asList("ldap://ldapone.example.com"), Arrays.asList("http://httpone.example.com")));

        X509Certificate certificate = gen.generate();

        String[] crlUrls = ServerCertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "ldap://ldapone.example.com", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "http://httpone.example.com", crlUrls[1]);
    }

    @Test
    public void testAuthorityInformationAccessUris() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(GOOGLE_PEM);

        String[] ocspUrls = ServerCertUtils.getAuthorityInformationAccessUris(certificate, "1.3.6.1.5.5.7.48.1");
        assertNotNull("No OCSP urls", ocspUrls);
        assertEquals("OCSP url not found.", "http://ocsp.thawte.com", ocspUrls[0]);

        String[] crtUrls = ServerCertUtils.getAuthorityInformationAccessUris(certificate, "1.3.6.1.5.5.7.48.2");
        assertNotNull("No CRT urls", crtUrls);
        assertEquals("CRT url not found", "http://www.thawte.com/repository/Thawte_SGC_CA.crt", crtUrls[0]);

    }

    @Test
    public void testAuthorityKeyIdentifierIssuerAndSerial() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(REDHAT_PEM);

        AuthorityKeyIdentifierStructure aki = ServerCertUtils.getAKIStructure(certificate);
        BigInteger serial = ServerCertUtils.getAKIAuthorityCertSerialNumber(aki);
        String issuerDn = ServerCertUtils.getAKIAuthorityCertIssuer(aki);

        assertEquals("Serial number not correctly processed", BigInteger.valueOf(0), serial);
        assertEquals("Issuer DN not correctly processed", "1.2.840.113549.1.9.1=#160f72686e73407265646861742e636f6d,cn=rhns certificate authority,ou=red hat network services,o=red hat\\, inc.,l=research triangle park,st=north carolina,c=us", issuerDn);
    }

    @Test
    public void testAuthorityKeyIdentifierKeyIdentifier() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(REDHAT_PEM);

        AuthorityKeyIdentifierStructure aki = ServerCertUtils.getAKIStructure(certificate);
        String base64KI = ServerCertUtils.getAKIKeyIdentifier(aki);

        assertEquals("KeyIdentifier not correctly processed", "VBXNnyz37A0f0qi+TAesiD77mwo=", base64KI);
    }

    /**
     * Test certificate with CRL and OCSP URLS and a CRT URL
     */
    private static final String GOOGLE_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIDITCCAoqgAwIBAgIQaHZkOD1Jbi714xmYQuB87jANBgkqhkiG9w0BAQUFADBMMQswCQYDVQQG\n" +
            "EwJaQTElMCMGA1UEChMcVGhhd3RlIENvbnN1bHRpbmcgKFB0eSkgTHRkLjEWMBQGA1UEAxMNVGhh\n" +
            "d3RlIFNHQyBDQTAeFw0wNzA1MDMxNTM0NThaFw0wODA1MTQyMzE4MTFaMGgxCzAJBgNVBAYTAlVT\n" +
            "MRMwEQYDVQQIEwpDYWxpZm9ybmlhMRYwFAYDVQQHEw1Nb3VudGFpbiBWaWV3MRMwEQYDVQQKEwpH\n" +
            "b29nbGUgSW5jMRcwFQYDVQQDEw53d3cuZ29vZ2xlLmNvbTCBnzANBgkqhkiG9w0BAQEFAAOBjQAw\n" +
            "gYkCgYEA5sXGjc0LowME3K7MyUa+vcydvHM0SP7TdWTQycl2J3IPqZYaO4HzFPaukFbnGdJzaKeF\n" +
            "pK7KJBQwALroNl2BczpxBY+xrxGH2lzxPr9TUYRvRA636CbXL7Jv8vJd36fPjKXpHm8wSJQhCwGt\n" +
            "ug5xAQ0Q77/uLNON/lSo/tOXj8sCAwEAAaOB5zCB5DAoBgNVHSUEITAfBggrBgEFBQcDAQYIKwYB\n" +
            "BQUHAwIGCWCGSAGG+EIEATA2BgNVHR8ELzAtMCugKaAnhiVodHRwOi8vY3JsLnRoYXd0ZS5jb20v\n" +
            "VGhhd3RlU0dDQ0EuY3JsMHIGCCsGAQUFBwEBBGYwZDAiBggrBgEFBQcwAYYWaHR0cDovL29jc3Au\n" +
            "dGhhd3RlLmNvbTA+BggrBgEFBQcwAoYyaHR0cDovL3d3dy50aGF3dGUuY29tL3JlcG9zaXRvcnkv\n" +
            "VGhhd3RlX1NHQ19DQS5jcnQwDAYDVR0TAQH/BAIwADANBgkqhkiG9w0BAQUFAAOBgQCTpI4FnX2K\n" +
            "8/gy0DucIc7S6FX9gLW71StUeiWsr3MYCvm3eplcFiNGV/wxGVuL8gR5c+60slZr39f32FbVt6rN\n" +
            "6JzImfN2S2QHreqaKyCS5pKbMoR8gmJ3mhWg1yGtyNmMuzGCmxCGqUF6EuABVgkG2GOaUO5Erd51\n" +
            "QQF6aVNJig==\n" +
            "-----END CERTIFICATE-----";

    /**
     * Test certificate with an X509v3 Authority Key Identifier that contains
     * an issuer/serial and a key identifier.
     */
    private static final String REDHAT_PEM =
            "-----BEGIN CERTIFICATE-----\n" +
            "MIIEMDCCA5mgAwIBAgIBADANBgkqhkiG9w0BAQQFADCBxzELMAkGA1UEBhMCVVMx\n" +
            "FzAVBgNVBAgTDk5vcnRoIENhcm9saW5hMR8wHQYDVQQHExZSZXNlYXJjaCBUcmlh\n" +
            "bmdsZSBQYXJrMRYwFAYDVQQKEw1SZWQgSGF0LCBJbmMuMSEwHwYDVQQLExhSZWQg\n" +
            "SGF0IE5ldHdvcmsgU2VydmljZXMxIzAhBgNVBAMTGlJITlMgQ2VydGlmaWNhdGUg\n" +
            "QXV0aG9yaXR5MR4wHAYJKoZIhvcNAQkBFg9yaG5zQHJlZGhhdC5jb20wHhcNMDAw\n" +
            "ODIzMjI0NTU1WhcNMDMwODI4MjI0NTU1WjCBxzELMAkGA1UEBhMCVVMxFzAVBgNV\n" +
            "BAgTDk5vcnRoIENhcm9saW5hMR8wHQYDVQQHExZSZXNlYXJjaCBUcmlhbmdsZSBQ\n" +
            "YXJrMRYwFAYDVQQKEw1SZWQgSGF0LCBJbmMuMSEwHwYDVQQLExhSZWQgSGF0IE5l\n" +
            "dHdvcmsgU2VydmljZXMxIzAhBgNVBAMTGlJITlMgQ2VydGlmaWNhdGUgQXV0aG9y\n" +
            "aXR5MR4wHAYJKoZIhvcNAQkBFg9yaG5zQHJlZGhhdC5jb20wgZ8wDQYJKoZIhvcN\n" +
            "AQEBBQADgY0AMIGJAoGBAMBoKxIw4iEtIsZycVu/F6CTEOmb48mNOy2sxLuVO+DK\n" +
            "VTLclcIQswSyUfvohWEWNKW0HWdcp3f08JLatIuvlZNi82YprsCIt2SEDkiQYPhg\n" +
            "PgB/VN0XpqwY4ELefL6Qgff0BYUKCMzV8p/8JIt3pT3pSKnvDztjo/6mg0zo3At3\n" +
            "AgMBAAGjggEoMIIBJDAdBgNVHQ4EFgQUVBXNnyz37A0f0qi+TAesiD77mwowgfQG\n" +
            "A1UdIwSB7DCB6YAUVBXNnyz37A0f0qi+TAesiD77mwqhgc2kgcowgccxCzAJBgNV\n" +
            "BAYTAlVTMRcwFQYDVQQIEw5Ob3J0aCBDYXJvbGluYTEfMB0GA1UEBxMWUmVzZWFy\n" +
            "Y2ggVHJpYW5nbGUgUGFyazEWMBQGA1UEChMNUmVkIEhhdCwgSW5jLjEhMB8GA1UE\n" +
            "CxMYUmVkIEhhdCBOZXR3b3JrIFNlcnZpY2VzMSMwIQYDVQQDExpSSE5TIENlcnRp\n" +
            "ZmljYXRlIEF1dGhvcml0eTEeMBwGCSqGSIb3DQEJARYPcmhuc0ByZWRoYXQuY29t\n" +
            "ggEAMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQEEBQADgYEAkwGIiGdnkYye0BIU\n" +
            "kHESh1UK8lIbrfLTBx2vcJm7sM2AI8ntK3PpY7HQs4xgxUJkpsGVVpDFNQYDWPWO\n" +
            "K9n5qaAQqZn3FUKSpVDXEQfxAtXgcORVbirOJfhdzQsvEGH49iBCzMOJ+IpPgiQS\n" +
            "zzl/IagsjVKXUsX3X0KlhwlmsMw=\n" +
            "-----END CERTIFICATE-----";

    private static final String BUG_9347_CRL_DIST_MULTI_URL =
            "MIIFUDCCBDigAwIBAgIKLi6BKAAAAAAANjANBgkqhkiG9w0BAQUFADBJMRMwEQYK\n" +
            "CZImiZPyLGQBGRYDY29tMRgwFgYKCZImiZPyLGQBGRYIdGVzdDIwMDMxGDAWBgNV\n" +
            "BAMTD0xheWVyIDcgU3VwcG9ydDAeFw0xMDExMDUwMTM5MDJaFw0xMTExMDUwMTQ5\n" +
            "MDJaMBQxEjAQBgNVBAMTCXRlc3RKYXNvbjCBnzANBgkqhkiG9w0BAQEFAAOBjQAw\n" +
            "gYkCgYEA03ryUeb4W3fc588UG7wmbJVLi12F+LJiA3+01fVYPBiPhlWKL1NrSqzK\n" +
            "ny8P/pn+a42pPY3HGg9SaZcG5dYs40qf7uWf2lkqcs9CJCYs37R45HBaJb1/ngqg\n" +
            "dWtkmQ7pEs+k2iDz9hAW2bueebhldkD6a7Ll1bw4wD2cd2h8pFsCAwEAAaOCAvEw\n" +
            "ggLtMA4GA1UdDwEB/wQEAwIE8DBEBgkqhkiG9w0BCQ8ENzA1MA4GCCqGSIb3DQMC\n" +
            "AgIAgDAOBggqhkiG9w0DBAICAIAwBwYFKw4DAgcwCgYIKoZIhvcNAwcwHQYDVR0O\n" +
            "BBYEFGFx+gMOleTBhksNYhunjvv6VQu/MBMGA1UdJQQMMAoGCCsGAQUFBwMCMB8G\n" +
            "A1UdIwQYMBaAFCooOCWPsc5SzpS7T5dc5UPfwysKMIIBEwYDVR0fBIIBCjCCAQYw\n" +
            "ggECoIH/oIH8hoG6bGRhcDovLy9DTj1MYXllciUyMDclMjBTdXBwb3J0LENOPXN1\n" +
            "cGFkMSxDTj1DRFAsQ049UHVibGljJTIwS2V5JTIwU2VydmljZXMsQ049U2Vydmlj\n" +
            "ZXMsQ049Q29uZmlndXJhdGlvbixEQz10ZXN0MjAwMyxEQz1jb20/Y2VydGlmaWNh\n" +
            "dGVSZXZvY2F0aW9uTGlzdD9iYXNlP29iamVjdENsYXNzPWNSTERpc3RyaWJ1dGlv\n" +
            "blBvaW50hj1odHRwOi8vc3VwYWQxLnRlc3QyMDAzLmNvbS9DZXJ0RW5yb2xsL0xh\n" +
            "eWVyJTIwNyUyMFN1cHBvcnQuY3JsMIIBJwYIKwYBBQUHAQEEggEZMIIBFTCBswYI\n" +
            "KwYBBQUHMAKGgaZsZGFwOi8vL0NOPUxheWVyJTIwNyUyMFN1cHBvcnQsQ049QUlB\n" +
            "LENOPVB1YmxpYyUyMEtleSUyMFNlcnZpY2VzLENOPVNlcnZpY2VzLENOPUNvbmZp\n" +
            "Z3VyYXRpb24sREM9dGVzdDIwMDMsREM9Y29tP2NBQ2VydGlmaWNhdGU/YmFzZT9v\n" +
            "YmplY3RDbGFzcz1jZXJ0aWZpY2F0aW9uQXV0aG9yaXR5MF0GCCsGAQUFBzAChlFo\n" +
            "dHRwOi8vc3VwYWQxLnRlc3QyMDAzLmNvbS9DZXJ0RW5yb2xsL3N1cGFkMS50ZXN0\n" +
            "MjAwMy5jb21fTGF5ZXIlMjA3JTIwU3VwcG9ydC5jcnQwDQYJKoZIhvcNAQEFBQAD\n" +
            "ggEBAAsBMmExw0W1QroxjjkSSWtlU2wFjL8R5T29aiTb4kVxiSn4Z+Cmew84uZSt\n" +
            "O2eNgWd+N/UgQ9LhyivsoBIP9X/wBA7QldDR5fpO4a3GspYJ0IttCI+B0aST/FW8\n" +
            "AgDWIHgRtS8/c+zZ7RtXDrUmXQpDwzZBV8rpsGr91crBZkRQ7T9Z2+lp6DGxASMI\n" +
            "zN76wxuXKtHYyZvd6bnpGUZJWHWUHJN7aOJlJv3MWF0zs3Aqula8safTInG/5QX9\n" +
            "yU/OOxityjqITM5ITy4BKUWPSNYS10F3303bzyQ9LS+ScOha0CIWST8InBV8iCL7\n" +
            "UATtteuIVGjcXy5b/C9a5m4IET4=";
}
