package com.l7tech.common.io;

import com.l7tech.common.TestDocuments;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import org.bouncycastle.x509.extension.AuthorityKeyIdentifierStructure;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * The current CertUtilsTest is the merge of the original CertUtilsTest and ServerCertUtilsTest (Note: ServerCertUtilsTest has been removed.)
 *
 * @author mike and steve
 */
public class CertUtilsTest {
    private static Logger log = Logger.getLogger(CertUtilsTest.class.getName());

    @Test
    public void testUnparseableCertBug2641() throws Exception {
        String serverCertB64 = "-----BEGIN CERTIFICATE-----\n" +
                "MIIEjTCCA/agAwIBAgIQUlv1nylQ9FxN+RpkLYfa6zANBgkqhkiG9w0BAQUFADCB\n" +
                "ujEfMB0GA1UEChMWVmVyaVNpZ24gVHJ1c3QgTmV0d29yazEXMBUGA1UECxMOVmVy\n" +
                "aVNpZ24sIEluYy4xMzAxBgNVBAsTKlZlcmlTaWduIEludGVybmF0aW9uYWwgU2Vy\n" +
                "dmVyIENBIC0gQ2xhc3MgMzFJMEcGA1UECxNAd3d3LnZlcmlzaWduLmNvbS9DUFMg\n" +
                "SW5jb3JwLmJ5IFJlZi4gTElBQklMSVRZIExURC4oYyk5NyBWZXJpU2lnbjAeFw0w\n" +
                "NjAxMDYwMDAwMDBaFw0wNzAxMDYyMzU5NTlaMIHRMQswCQYDVQQGEwJVUzETMBEG\n" +
                "A1UECBMKQ2FsaWZvcm5pYTEPMA0GA1UEBxQGTm92YXRvMSgwJgYDVQQKFB9GaXJl\n" +
                "bWFucyBGdW5kIEluc3VyYW5jZSBDb21wYW55MRYwFAYDVQQLFA1JVCBEZXBhcnRt\n" +
                "ZW50MTMwMQYDVQQLFCpUZXJtcyBvZiB1c2UgYXQgd3d3LnZlcmlzaWduLmNvbS9y\n" +
                "cGEgKGMpMDAxJTAjBgNVBAMUHHByb2RnYXRld2F5LmZpcmVtYW5zZnVuZC5jb20w\n" +
                "gZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIpM9mkh3OJHn96LrYtfqhdgeP/d\n" +
                "BX6eFo8O54DS+/Q/tsXf7ce3Z4ELYSgYRY4rBfS3Vvn+HlE2S3wz3qaSqjA6sE+Q\n" +
                "+JPEMc+ElwlGLDHjgapbSF5LUL6rbhf+5I8FPqL5NQCciRwWNb1fLcAH7Wm9i6yu\n" +
                "eCona+ME6HsVsZdPAgMBAAGjggF5MIIBdTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF\n" +
                "oDBGBgNVHR8EPzA9MDugOaA3hjVodHRwOi8vY3JsLnZlcmlzaWduLmNvbS9DbGFz\n" +
                "czNJbnRlcm5hdGlvbmFsU2VydmVyLmNybDBEBgNVHSAEPTA7MDkGC2CGSAGG+EUB\n" +
                "BxcDMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8vd3d3LnZlcmlzaWduLmNvbS9ycGEw\n" +
                "KAYDVR0lBCEwHwYJYIZIAYb4QgQBBggrBgEFBQcDAQYIKwYBBQUHAwIwNAYIKwYB\n" +
                "BQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC52ZXJpc2lnbi5jb20w\n" +
                "bQYIKwYBBQUHAQwEYTBfoV2gWzBZMFcwVRYJaW1hZ2UvZ2lmMCEwHzAHBgUrDgMC\n" +
                "GgQUj+XTGoasjY5rw8+AatRIGCx7GS4wJRYjaHR0cDovL2xvZ28udmVyaXNpZ24u\n" +
                "Y29tL3ZzbG9nby5naWYwDQYJKoZIhvcNAQEFBQADgYEAIWhjuj+ZW7QrXjmUzqRg\n" +
                "1CZzb675vNXsqiO4prVM6NBHJ+b9r0PUBvlhCq/YnBcnY0dns04iipmrKpsq969H\n" +
                "dKovy15TKYtw35aw943D895nSfKPgCSZpfi5AUc/5bxCo4YS8WNc5Ltyr/Ji0EWb\n" +
                "XdS5Vqkkzo1HNJ01WGCvkbk=\n" +
                "-----END CERTIFICATE-----";

        CertUtils.decodeCert(serverCertB64.getBytes());

        String clientCertB64 = "-----BEGIN CERTIFICATE-----\n" +
                "MIIEjTCCA/agAwIBAgIQFE6oIpUaQDfJ0xCp9MFuMDANBgkqhkiG9w0BAQUFADCB\n" +
                "ujEfMB0GA1UEChMWVmVyaVNpZ24gVHJ1c3QgTmV0d29yazEXMBUGA1UECxMOVmVy\n" +
                "aVNpZ24sIEluYy4xMzAxBgNVBAsTKlZlcmlTaWduIEludGVybmF0aW9uYWwgU2Vy\n" +
                "dmVyIENBIC0gQ2xhc3MgMzFJMEcGA1UECxNAd3d3LnZlcmlzaWduLmNvbS9DUFMg\n" +
                "SW5jb3JwLmJ5IFJlZi4gTElBQklMSVRZIExURC4oYyk5NyBWZXJpU2lnbjAeFw0w\n" +
                "NTEyMDgwMDAwMDBaFw0wNjEyMDgyMzU5NTlaMIHRMQswCQYDVQQGEwJVUzETMBEG\n" +
                "A1UECBMKQ2FsaWZvcm5pYTEPMA0GA1UEBxQGTm92YXRvMSgwJgYDVQQKFB9GaXJl\n" +
                "bWFucyBGdW5kIEluc3VyYW5jZSBDb21wYW55MRYwFAYDVQQLFA1JVCBEZXBhcnRt\n" +
                "ZW50MTMwMQYDVQQLFCpUZXJtcyBvZiB1c2UgYXQgd3d3LnZlcmlzaWduLmNvbS9y\n" +
                "cGEgKGMpMDAxJTAjBgNVBAMUHGNlcnRnYXRld2F5LmZpcmVtYW5zZnVuZC5jb20w\n" +
                "gZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIwQeBxPHUmOIYm6/Z2D5PKPFwhF\n" +
                "9FIaK7rji0JBsiwHTCm27grG0RBEmHCqXhbRe0qFzgg/pXwQFQtRDrVUgJhxRN2i\n" +
                "dVKxs6ccGsxtB88WUPlIaGSpkIeCChT03WedUVtJWPQlPiXpz1O7mWaKKfJZBON2\n" +
                "4urJcAGU+M4qYD/tAgMBAAGjggF5MIIBdTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF\n" +
                "oDBGBgNVHR8EPzA9MDugOaA3hjVodHRwOi8vY3JsLnZlcmlzaWduLmNvbS9DbGFz\n" +
                "czNJbnRlcm5hdGlvbmFsU2VydmVyLmNybDBEBgNVHSAEPTA7MDkGC2CGSAGG+EUB\n" +
                "BxcDMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8vd3d3LnZlcmlzaWduLmNvbS9ycGEw\n" +
                "KAYDVR0lBCEwHwYJYIZIAYb4QgQBBggrBgEFBQcDAQYIKwYBBQUHAwIwNAYIKwYB\n" +
                "BQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC52ZXJpc2lnbi5jb20w\n" +
                "bQYIKwYBBQUHAQwEYTBfoV2gWzBZMFcwVRYJaW1hZ2UvZ2lmMCEwHzAHBgUrDgMC\n" +
                "GgQUj+XTGoasjY5rw8+AatRIGCx7GS4wJRYjaHR0cDovL2xvZ28udmVyaXNpZ24u\n" +
                "Y29tL3ZzbG9nby5naWYwDQYJKoZIhvcNAQEFBQADgYEAMYjuVnHTybB+fwiO2txG\n" +
                "FOVoHGgzI4OjFbXkpIDrBYDD+DJFVzD3vJeWcBJpOq9bmIPcF47ZlDH0TuQsaBYw\n" +
                "J1mF24UWOCrnWAbgWAHwax64TIy7K1fvegwKAcQaKgekRYqhRff86tX/ZexHM7jT\n" +
                "0RTm3chJ4nLEWA2Sd67fGUM=\n" +
                "-----END CERTIFICATE-----";

        CertUtils.decodeCert(clientCertB64.getBytes());


        String otherCertB64 = "-----BEGIN CERTIFICATE-----\n" +
                "LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tDQpNSUlFalRDQ0EvYWdBd0lCQWdJUVVsdjFueWxR\n" +
                "OUZ4TitScGtMWWZhNnpBTkJna3Foa2lHOXcwQkFRVUZBRENCDQp1akVmTUIwR0ExVUVDaE1XVm1W\n" +
                "eWFWTnBaMjRnVkhKMWMzUWdUbVYwZDI5eWF6RVhNQlVHQTFVRUN4TU9WbVZ5DQphVk5wWjI0c0lF\n" +
                "bHVZeTR4TXpBeEJnTlZCQXNUS2xabGNtbFRhV2R1SUVsdWRHVnlibUYwYVc5dVlXd2dVMlZ5DQpk\n" +
                "bVZ5SUVOQklDMGdRMnhoYzNNZ016RkpNRWNHQTFVRUN4TkFkM2QzTG5abGNtbHphV2R1TG1OdmJT\n" +
                "OURVRk1nDQpTVzVqYjNKd0xtSjVJRkpsWmk0Z1RFbEJRa2xNU1ZSWklFeFVSQzRvWXlrNU55Qlda\n" +
                "WEpwVTJsbmJqQWVGdzB3DQpOakF4TURZd01EQXdNREJhRncwd056QXhNRFl5TXpVNU5UbGFNSUhS\n" +
                "TVFzd0NRWURWUVFHRXdKVlV6RVRNQkVHDQpBMVVFQ0JNS1EyRnNhV1p2Y201cFlURVBNQTBHQTFV\n" +
                "RUJ4UUdUbTkyWVhSdk1TZ3dKZ1lEVlFRS0ZCOUdhWEpsDQpiV0Z1Y3lCR2RXNWtJRWx1YzNWeVlX\n" +
                "NWpaU0JEYjIxd1lXNTVNUll3RkFZRFZRUUxGQTFKVkNCRVpYQmhjblJ0DQpaVzUwTVRNd01RWURW\n" +
                "UVFMRkNwVVpYSnRjeUJ2WmlCMWMyVWdZWFFnZDNkM0xuWmxjbWx6YVdkdUxtTnZiUzl5DQpjR0Vn\n" +
                "S0dNcE1EQXhKVEFqQmdOVkJBTVVISEJ5YjJSbllYUmxkMkY1TG1acGNtVnRZVzV6Wm5WdVpDNWpi\n" +
                "MjB3DQpnWjh3RFFZSktvWklodmNOQVFFQkJRQURnWTBBTUlHSkFvR0JBSXBNOW1raDNPSkhuOTZM\n" +
                "cll0ZnFoZGdlUC9kDQpCWDZlRm84TzU0RFMrL1EvdHNYZjdjZTNaNEVMWVNnWVJZNHJCZlMzVnZu\n" +
                "K0hsRTJTM3d6M3FhU3FqQTZzRStRDQorSlBFTWMrRWx3bEdMREhqZ2FwYlNGNUxVTDZyYmhmKzVJ\n" +
                "OEZQcUw1TlFDY2lSd1dOYjFmTGNBSDdXbTlpNnl1DQplQ29uYStNRTZIc1ZzWmRQQWdNQkFBR2pn\n" +
                "Z0Y1TUlJQmRUQUpCZ05WSFJNRUFqQUFNQXNHQTFVZER3UUVBd0lGDQpvREJHQmdOVkhSOEVQekE5\n" +
                "TUR1Z09hQTNoalZvZEhSd09pOHZZM0pzTG5abGNtbHphV2R1TG1OdmJTOURiR0Z6DQpjek5KYm5S\n" +
                "bGNtNWhkR2x2Ym1Gc1UyVnlkbVZ5TG1OeWJEQkVCZ05WSFNBRVBUQTdNRGtHQzJDR1NBR0crRVVC\n" +
                "DQpCeGNETUNvd0tBWUlLd1lCQlFVSEFnRVdIR2gwZEhCek9pOHZkM2QzTG5abGNtbHphV2R1TG1O\n" +
                "dmJTOXljR0V3DQpLQVlEVlIwbEJDRXdId1lKWUlaSUFZYjRRZ1FCQmdnckJnRUZCUWNEQVFZSUt3\n" +
                "WUJCUVVIQXdJd05BWUlLd1lCDQpCUVVIQVFFRUtEQW1NQ1FHQ0NzR0FRVUZCekFCaGhob2RIUndP\n" +
                "aTh2YjJOemNDNTJaWEpwYzJsbmJpNWpiMjB3DQpiUVlJS3dZQkJRVUhBUXdFWVRCZm9WMmdXekJa\n" +
                "TUZjd1ZSWUphVzFoWjJVdloybG1NQ0V3SHpBSEJnVXJEZ01DDQpHZ1FVaitYVEdvYXNqWTVydzgr\n" +
                "QWF0UklHQ3g3R1M0d0pSWWphSFIwY0RvdkwyeHZaMjh1ZG1WeWFYTnBaMjR1DQpZMjl0TDNaemJH\n" +
                "OW5ieTVuYVdZd0RRWUpLb1pJaHZjTkFRRUZCUUFEZ1lFQUlXaGp1aitaVzdRclhqbVV6cVJnDQox\n" +
                "Q1p6YjY3NXZOWHNxaU80cHJWTTZOQkhKK2I5cjBQVUJ2bGhDcS9ZbkJjblkwZG5zMDRpaXBtcktw\n" +
                "c3E5NjlIDQpkS292eTE1VEtZdHczNWF3OTQzRDg5NW5TZktQZ0NTWnBmaTVBVWMvNWJ4Q280WVM4\n" +
                "V05jNUx0eXIvSmkwRVdiDQpYZFM1VnFra3pvMUhOSjAxV0dDdmtiaz0NCi0tLS0tRU5EIENFUlRJ\n" +
                "RklDQVRFLS0tLS0NCg==\n" +
                "-----END CERTIFICATE-----";

        CertUtils.decodeCert(otherCertB64.getBytes());
    }

    @Test
    public void testDnParser() throws Exception {
        doTestDnParse();
        doTestDnPatterns();
    }

    private void doTestDnParse() throws Exception {
        final Map<String,List<String>> map = CertUtils.dnToAttributeMap("cn=Mike Lyons, ou=Research, o=Layer 7 Technologies");

        final Set<Map.Entry<String,List<String>>> entries = map.entrySet();
        for ( final Map.Entry<String,List<String>> entry : entries ) {
            final String key = entry.getKey();
            final List<String> values = entry.getValue();
            for ( final String value : values ) {
                log.info( "    key<" + key + ">  value<" + value + ">" );
            }
        }

    }

    private void doTestDnPatterns() throws Exception {
        assertTrue(CertUtils.dnMatchesPattern("O=ACME Inc., OU=Widgets, CN=joe",
                               "O=ACME Inc., OU=Widgets, CN=*", false));

        assertFalse(CertUtils.dnMatchesPattern("O=ACME Inc., OU=Widgets, CN=joe",
                                "O=ACME Inc., OU=Widgets, CN=bob", false));

        assertTrue("Multi-valued attributes, case and whitespace are insignificant",
                   CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise",
                               "dc=layer7-tech, DC=com, UID=*", false));

        assertFalse("Group value wildcards are required",
                    CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise",
                                "dc=layer7-tech, DC=com, cn=*, UID=*", false));
    }

    @Test
    public void testWildcardDomainMatch() {
        assertTrue( "Wildcard only match", CertUtils.domainNameMatchesPattern( "host", "*", false ) );
        assertTrue( "Wildcards only match", CertUtils.domainNameMatchesPattern( "host", "******", false ) );
        assertTrue( "Wildcard hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "*.domain.com", false ) );
        assertTrue( "Wildcard trailing hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "h*.domain.com", false ) );
        assertTrue( "Wildcard leading hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "*t.domain.com", false ) );
        assertTrue( "Wildcard embedded hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "h*t.domain.com", false ) );
        assertTrue( "Wildcard multi hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "h**t.domain.com", false ) );
        assertTrue( "Wildcard multi outer hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "*os*.domain.com", false ) );
        assertTrue( "Wildcard multi outer2 hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "*ost*.domain.com", false ) );
        assertTrue( "Wildcard multi outer3 hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "*host*.domain.com", false ) );
        assertTrue( "Wildcard lots multi hostname match", CertUtils.domainNameMatchesPattern( "host.domain.com", "h*****t.domain.com", false ) );
        assertTrue( "Wildcard domain match", CertUtils.domainNameMatchesPattern( "host.sub.domain.com", "host.*.domain.com", false ) );
        assertFalse( "Wildcard mismatch", CertUtils.domainNameMatchesPattern( "host.sub.domain.com", "e*.domain.com", false ) );
        assertFalse( "Wildcard tailing mismatch", CertUtils.domainNameMatchesPattern( "host.sub.domain.com", "*e.domain.com", false ) );
        assertFalse( "Wildcard multi-domain match", CertUtils.domainNameMatchesPattern( "host.sub.domain.com", "*.domain.com", false ) );
        assertFalse( "Wildcard domain match", CertUtils.domainNameMatchesPattern( "host.sub.domain.com", "host.*.domain.com", true ) );
        assertFalse( "Wildcard multi mismatch", CertUtils.domainNameMatchesPattern( "host.domain.com", "h*eer*st.domain.com", false ) );
    }

    @Test
    public void testDNComparison() {
        assertTrue( "Canonical match", CertUtils.isEqualDNCanonical( "ou=blah+cn=abc", "cn=ABC+OU=Blah" ) );
        assertTrue( "Canonical match naming", CertUtils.isEqualDNCanonical( "email=test@test.com+s=ca+dnq=test.com", "EMAILADDRESS=test@test.com+ST=ca+DNQUALIFIER=test.com" ) );
        assertFalse( "Basic mismatch", CertUtils.isEqualDNCanonical( "cn=a", "cn=b" ) );
    }

    @Test
    public void testValidDN() {
        assertTrue( "Valid DN", CertUtils.isValidDN("CN=bug5722_child, O=OASIS, ST=NJ, DNQ=org, EMAILADDRESS=support@simpson.org") );
        assertFalse( "Invalid DN", CertUtils.isValidDN("CN=bug5722_child, O=OASIS, S=NJ, DNQUALIFIER=org, EMAIL=support@simpson.org") );
        assertTrue( "Valid DN with explicit known OIDs", CertUtils.isValidDN("cn=bug5722_child,o=oasis,st=nj,2.5.4.46=#13036f7267,1.2.840.113549.1.9.1=#1613737570706f72744073696d70736f6e2e6f7267") );
        assertTrue( "Valid DN with explicit unknown OIDs", CertUtils.isValidDN("cn=bug5722_child,2.5.4.46342342=#1613737570706f72744073696d70736f6e2e6f7267") );
    }

    @Test
    public void testDNString() throws Exception {
        X509Certificate cert = CertUtils.decodeFromPEM(
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIDQjCCAiqgAwIBAgIBBzANBgkqhkiG9w0BAQUFADBfMQswCQYDVQQGEwJVUzEL\n" +
                "MAkGA1UECBMCTUQxEDAOBgNVBAoTB0pIVS1BUEwxGDAWBgNVBAsUD0FQTF9HSUdf\n" +
                "VEVTVEJFRDEXMBUGA1UEAxQOR0lHX1RFU1RCRURfQ0EwHhcNMTAwMTE1MTk1OTA0\n" +
                "WhcNMTMwMTE0MTk1OTA0WjBmMQswCQYDVQQGEwJVUzELMAkGA1UECBMCTUQxEDAO\n" +
                "BgNVBAoTB0pIVS1BUEwxGDAWBgNVBAsUD0FQTF9HSUdfVEVTVEJFRDEeMBwGA1UE\n" +
                "AxMVVVJOOkJBRTpURVNUOjM6TEFZRVI3MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCB\n" +
                "iQKBgQC22D6MhBOL6izNYF3rNZfvyb+6z4BkrT5udlJs34TmCNENbFOSupnJRUKN\n" +
                "HMMNa3ewdvxzhIJo3B+oHPDCMTM4fQ9t5EsWxVdQEVRZGMiJqk5d/0IoWD9V36yU\n" +
                "OsuG+lXIczrnWL9AojKovps0TKcIZFYg75QpffIs6u4IR9SQkwIDAQABo4GFMIGC\n" +
                "MAkGA1UdEwQCMAAwCwYDVR0PBAQDAgXgMCgGCWCGSAGG+EIBDQQbFhlHSUcgVEVT\n" +
                "VEJFRCBDQSBhdCBKSFUtQVBMMB0GA1UdDgQWBBSQI/FA/QQ2IjLz4htwzmK/Hd7R\n" +
                "+DAfBgNVHSMEGDAWgBQK4j02q3H4XDwG2ke7rZlfp5P6EjANBgkqhkiG9w0BAQUF\n" +
                "AAOCAQEAoBCEEoZUq1s7w5wFG1DQ2q/trJIqdmJN8XNzzNvFZ/vKmStHYZlgsVK8\n" +
                "aAcFproDmJDWa+U1L+0ICxYQzGMzFza/UN2K//Y/KU5qaAdOaUwC0ByGCY0uWYKU\n" +
                "iombk5qTMvs//sHKqCbu/SbOpi5PH9U7I/SGFAUD6HPYERRO9f6MH5cl4Ur2uN70\n" +
                "JOH5tmNIBAl6FgB5EL981jIVFiNno6GNcijVdIis/h60jJVwO5WCWCewZZ8nmKg8\n" +
                "D5IH7rEtrJFxBcWgSn44AUbObonUP8iQLKMfsx2eO0PKZE2Ov7NbFvBFpUqrUdQD\n" +
                "zElRwKBQRxA8wDn+DkLbkHsOA1q2CQ==\n" +
                "-----END CERTIFICATE-----",
                true
        );

        X509Certificate cert2 = CertUtils.decodeFromPEM(
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIEpTCCA42gAwIBAgIQF3YFiJVY7rsA2hDl8POc8DANBgkqhkiG9w0BAQUFADCB\n" +
                "izELMAkGA1UEBhMCVVMxFTATBgNVBAoTDHRoYXd0ZSwgSW5jLjE5MDcGA1UECxMw\n" +
                "VGVybXMgb2YgdXNlIGF0IGh0dHBzOi8vd3d3LnRoYXd0ZS5jb20vY3BzIChjKTA2\n" +
                "MSowKAYDVQQDEyF0aGF3dGUgRXh0ZW5kZWQgVmFsaWRhdGlvbiBTU0wgQ0EwHhcN\n" +
                "MDgxMTE5MDAwMDAwWhcNMTAwMTE3MjM1OTU5WjCBxzETMBEGCysGAQQBgjc8AgED\n" +
                "EwJVUzEZMBcGCysGAQQBgjc8AgECFAhEZWxhd2FyZTEbMBkGA1UEDxMSVjEuMCwg\n" +
                "Q2xhdXNlIDUuKGIpMRMwEQYDVQQKFApUaGF3dGUgSW5jMRAwDgYDVQQFEwczODk4\n" +
                "MjYxMQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTEWMBQGA1UEBxQN\n" +
                "TW91bnRhaW4gVmlldzEXMBUGA1UEAxQOd3d3LnRoYXd0ZS5jb20wgZ8wDQYJKoZI\n" +
                "hvcNAQEBBQADgY0AMIGJAoGBAOeJaLVuHTgZ9i1hwgC6bqtmktaFhy3VqFipenUn\n" +
                "ne2e/gZxcC0hcEw+nLbVXUSStODufApQTA1nmKoBDjejKu/m4BF77rCitDJkpw3a\n" +
                "bBX4xaVaLPzJpjyIiL/fpzjweO2BkykMrserUSFeypXlSFJBthhgBBlvPYAU068j\n" +
                "AxCVAgMBAAGjggFJMIIBRTAMBgNVHRMBAf8EAjAAMDkGA1UdHwQyMDAwLqAsoCqG\n" +
                "KGh0dHA6Ly9jcmwudGhhd3RlLmNvbS9UaGF3dGVFVkNBMjAwNi5jcmwwQgYDVR0g\n" +
                "BDswOTA3BgtghkgBhvhFAQcwATAoMCYGCCsGAQUFBwIBFhpodHRwczovL3d3dy50\n" +
                "aGF3dGUuY29tL2NwczAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwHwYD\n" +
                "VR0jBBgwFoAUzTLi8l0lRwKqj3lLMu4Dmf0wSdEwdgYIKwYBBQUHAQEEajBoMCIG\n" +
                "CCsGAQUFBzABhhZodHRwOi8vb2NzcC50aGF3dGUuY29tMEIGCCsGAQUFBzAChjZo\n" +
                "dHRwOi8vd3d3LnRoYXd0ZS5jb20vcmVwb3NpdG9yeS9UaGF3dGVfRVZfQ0FfMjAw\n" +
                "Ni5jcnQwDQYJKoZIhvcNAQEFBQADggEBALKglt3sBDhrw3qtI0SR5WKMsfacAyEf\n" +
                "7wPZymOy+Ntak8LM8Xxv6w9Re0vntfy8m4dIzFv5yGakQKzpQl3t81MT571uf1BT\n" +
                "ZLOV8UJPNlS0Hn8YNzk7BlvlE9lXvNVo43FfXyv1psKPZ4E6RGOMNvqo7f3XXqKf\n" +
                "sJ1HhvtxYI7I00UZt9rNnupwEIc3EN0sEd/uAiGmdebWn1RyYeZcHm4W9o64/EeA\n" +
                "BUv3LQLuUCbRSAFg3Dyn2+vKi6b/nkddh0D40oLXE2QO1LMpIqfgyM2MTfURISYC\n" +
                "QzOOqT+R1AWXydNCawWZ9hZxZ2XHlt8q11RjJcAo9xzuzYvknTKjgVU=\n" +
                "-----END CERTIFICATE-----",
                true
        );

        assertEquals( "DNQ DN", "cn=bug5722_child,o=oasis,st=nj,2.5.4.46=org,1.2.840.113549.1.9.1=support@simpson.org", CertUtils.formatDN("cn=bug5722_child,o=oasis,st=nj,2.5.4.46=#13036f7267,1.2.840.113549.1.9.1=#1613737570706f72744073696d70736f6e2e6f7267") );
        assertEquals( "Formatted Subject DN 1", CertUtils.formatDN( "CN=URN:BAE:TEST:3:LAYER7, OU=APL_GIG_TESTBED, O=JHU-APL, ST=MD, C=US" ), CertUtils.getSubjectDN(cert) );
        assertEquals( "Formatted Subject DN 2", CertUtils.formatDN( "CN=www.thawte.com, L=Mountain View, ST=California, C=US, SERIALNUMBER=3898261, O=Thawte Inc, OID.2.5.4.15=\"V1.0, Clause 5.(b)\", OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, OID.1.3.6.1.4.1.311.60.2.1.3=US" ), CertUtils.getSubjectDN(cert2) );
        // The following test is not generally applicable, but is true for this test data
        assertEquals( "Subject DN 1", cert.getSubjectDN().getName().toLowerCase().replaceAll(" ", ""), CertUtils.getSubjectDN( cert ));
    }

    @Ignore("Developer test")
    public void testPerformance() throws Exception {
        CertUtils.dnToAttributeMap("dc=layer7-tech,dc=com, uid=acruise");
        long before = System.currentTimeMillis();
        int i = 0;
        for (; i < 250000; i++) {
            CertUtils.dnToAttributeMap("dc=layer7-tech,dc=com, uid=acruise");
        }
        final long t = (System.currentTimeMillis() - before);
        System.out.println( i + " iterations in " + t + "ms.");
        System.out.println((double)i/t *1000 + " iterations per second");
    }

    /**
     * dnToAttributeMap may throw an IllegalArgumentException when an invalid value is passed in.
     * This test validates the current behavior in case it is changed. The
     * bug fix for 13002 will expect this exception during SAML token processing.
     *
     * @throws Exception
     */
    @BugNumber(13002)
    @Test
    public void testInvalidDnThrowsIllegalArgument() throws Exception {
        try {
            CertUtils.dnToAttributeMap("admin");
            fail("Method should have thrown for invalid dn");
        } catch (IllegalArgumentException e) {
            final String message = e.getMessage();
            System.out.println(message);
            assertTrue("Invalid message text found", message.startsWith("Invalid DN"));
        }
    }

    @Test
    public void testFingerprint() throws Exception {
        String certificatePem = "-----BEGIN CERTIFICATE-----\n" +
                "MIIEjTCCA/agAwIBAgIQFE6oIpUaQDfJ0xCp9MFuMDANBgkqhkiG9w0BAQUFADCB\n" +
                "ujEfMB0GA1UEChMWVmVyaVNpZ24gVHJ1c3QgTmV0d29yazEXMBUGA1UECxMOVmVy\n" +
                "aVNpZ24sIEluYy4xMzAxBgNVBAsTKlZlcmlTaWduIEludGVybmF0aW9uYWwgU2Vy\n" +
                "dmVyIENBIC0gQ2xhc3MgMzFJMEcGA1UECxNAd3d3LnZlcmlzaWduLmNvbS9DUFMg\n" +
                "SW5jb3JwLmJ5IFJlZi4gTElBQklMSVRZIExURC4oYyk5NyBWZXJpU2lnbjAeFw0w\n" +
                "NTEyMDgwMDAwMDBaFw0wNjEyMDgyMzU5NTlaMIHRMQswCQYDVQQGEwJVUzETMBEG\n" +
                "A1UECBMKQ2FsaWZvcm5pYTEPMA0GA1UEBxQGTm92YXRvMSgwJgYDVQQKFB9GaXJl\n" +
                "bWFucyBGdW5kIEluc3VyYW5jZSBDb21wYW55MRYwFAYDVQQLFA1JVCBEZXBhcnRt\n" +
                "ZW50MTMwMQYDVQQLFCpUZXJtcyBvZiB1c2UgYXQgd3d3LnZlcmlzaWduLmNvbS9y\n" +
                "cGEgKGMpMDAxJTAjBgNVBAMUHGNlcnRnYXRld2F5LmZpcmVtYW5zZnVuZC5jb20w\n" +
                "gZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIwQeBxPHUmOIYm6/Z2D5PKPFwhF\n" +
                "9FIaK7rji0JBsiwHTCm27grG0RBEmHCqXhbRe0qFzgg/pXwQFQtRDrVUgJhxRN2i\n" +
                "dVKxs6ccGsxtB88WUPlIaGSpkIeCChT03WedUVtJWPQlPiXpz1O7mWaKKfJZBON2\n" +
                "4urJcAGU+M4qYD/tAgMBAAGjggF5MIIBdTAJBgNVHRMEAjAAMAsGA1UdDwQEAwIF\n" +
                "oDBGBgNVHR8EPzA9MDugOaA3hjVodHRwOi8vY3JsLnZlcmlzaWduLmNvbS9DbGFz\n" +
                "czNJbnRlcm5hdGlvbmFsU2VydmVyLmNybDBEBgNVHSAEPTA7MDkGC2CGSAGG+EUB\n" +
                "BxcDMCowKAYIKwYBBQUHAgEWHGh0dHBzOi8vd3d3LnZlcmlzaWduLmNvbS9ycGEw\n" +
                "KAYDVR0lBCEwHwYJYIZIAYb4QgQBBggrBgEFBQcDAQYIKwYBBQUHAwIwNAYIKwYB\n" +
                "BQUHAQEEKDAmMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC52ZXJpc2lnbi5jb20w\n" +
                "bQYIKwYBBQUHAQwEYTBfoV2gWzBZMFcwVRYJaW1hZ2UvZ2lmMCEwHzAHBgUrDgMC\n" +
                "GgQUj+XTGoasjY5rw8+AatRIGCx7GS4wJRYjaHR0cDovL2xvZ28udmVyaXNpZ24u\n" +
                "Y29tL3ZzbG9nby5naWYwDQYJKoZIhvcNAQEFBQADgYEAMYjuVnHTybB+fwiO2txG\n" +
                "FOVoHGgzI4OjFbXkpIDrBYDD+DJFVzD3vJeWcBJpOq9bmIPcF47ZlDH0TuQsaBYw\n" +
                "J1mF24UWOCrnWAbgWAHwax64TIy7K1fvegwKAcQaKgekRYqhRff86tX/ZexHM7jT\n" +
                "0RTm3chJ4nLEWA2Sd67fGUM=\n" +
                "-----END CERTIFICATE-----";

        final X509Certificate certificate = CertUtils.decodeFromPEM( certificatePem );
        final String fingerprint = CertUtils.getCertificateFingerprint(certificate, "SHA1").substring(5);
        assertEquals( "Fingerprint only", "F6:F6:1A:8B:28:A2:06:1F:5B:62:8C:C8:22:CC:6C:64:D2:5A:D2:79", fingerprint );
    }

    @Test
    public void testCRLURL() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(GOOGLE_PEM);

        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Empty CRL urls", crlUrls.length > 0);
        assertEquals("CRL url missing or invalid", "http://crl.thawte.com/ThawteSGCCA.crl", crlUrls[0]);

    }

    @Test
    @BugNumber(10713)
    public void testCRLURLAlice() throws Exception {
        X509Certificate certificate = TestDocuments.getWssInteropAliceCert();
        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull( "Null CRL urls", crlUrls );
        assertEquals( "CRL urls", 0L, (long) crlUrls.length );
    }

    @Test
    @BugNumber(9347)
    public void testCRLURL_distPointMulti() throws Exception {
        X509Certificate certificate = CertUtils.decodeCert(HexUtils.decodeBase64(BUG_9347_CRL_DIST_MULTI_URL));

        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "ldap:///CN=Layer%207%20Support,CN=supad1,CN=CDP,CN=Public%20Key%20Services,CN=Services,CN=Configuration,DC=test2003,DC=com?certificateRevocationList?base?objectClass=cRLDistributionPoint", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "http://supad1.test2003.com/CertEnroll/Layer%207%20Support.crl", crlUrls[1]);
    }

    @Test
    @BugNumber(9539)
    public void testCRLURL_distPointPortal() throws Exception {
        X509Certificate certificate = CertUtils.decodeCert(HexUtils.decodeBase64(BUG_9539_PORTAL_SPACES));

        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "http://crl.disa.mil/getcrl?DOD%20CA-13", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "ldap://crl.gds.disa.mil/cn%3dDOD%20CA-13%2cou%3dPKI%2cou%3dDoD%2co%3dU.S.%20Government%2cc%3dUS?certificaterevocationlist;binary", crlUrls[1]);
    }

    @Test
    @BugNumber(9347)
    public void testCRLURL_oneDistPoint_twoUrls() throws Exception {
        //The certificate generation setting:
        //      Key size: 512
        //      Include CRL Distribution Points: true
        //      Set CRL Distribution Points URLs: {"ldap://ldapone.example.com", "http://httpone.example.com"}
        final String certificatePerm =
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIBxzCCAXGgAwIBAgIISYF19Y2ktXkwDQYJKoZIhvcNAQEFBQAwDzENMAsGA1UEAxMEdGVzdDAe\n" +
                "Fw0xMTA2MTMxNzMzMjFaFw0zMTA2MDgxNzMzMjFaMA8xDTALBgNVBAMTBHRlc3QwXDANBgkqhkiG\n" +
                "9w0BAQEFAANLADBIAkEAjLvUOsDNAc7jYXURG4UM17NsHK2H74G7PLZB9YJWcBdNsDQaSJ88MOVb\n" +
                "UoZq4hzVuxHp5iqfOLP6VQ1nCXoqhwIDAQABo4GwMIGtMA4GA1UdDwEB/wQEAwIF4DASBgNVHSUB\n" +
                "Af8ECDAGBgRVHSUAMB0GA1UdDgQWBBRiGfOcWMpAUC0ndYRnbu2Rbdio+zAfBgNVHSMEGDAWgBRi\n" +
                "GfOcWMpAUC0ndYRnbu2Rbdio+zBHBgNVHR8EQDA+MDygOqA4hhpsZGFwOi8vbGRhcG9uZS5leGFt\n" +
                "cGxlLmNvbYYaaHR0cDovL2h0dHBvbmUuZXhhbXBsZS5jb20wDQYJKoZIhvcNAQEFBQADQQAJb45z\n" +
                "WRT0hwUmfDE3qVwvtHmzc1FiR0OFB1H0mZmMKe5hJGhI2Dy6N9i8XvzhhWY3/SzPrlnpogIyZ3GC\n" +
                "xVdz\n" +
                "-----END CERTIFICATE-----";

        X509Certificate certificate = CertUtils.decodeFromPEM(certificatePerm);
        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "ldap://ldapone.example.com", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "http://httpone.example.com", crlUrls[1]);
    }

    @Test
    @BugNumber(9347)
    public void testCRLURL_twoDistPoints_oneUrlEach() throws Exception {
        //The certificate generation setting:
        //      Key size: 512
        //      Include CRL Distribution Points: true
        //      Set CRL Distribution Points URLs: {"ldap://ldapone.example.com"}, {"http://httpone.example.com"}
        final String certificatePerm =
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIBzjCCAXigAwIBAgIJAL5eJGSd7l+AMA0GCSqGSIb3DQEBBQUAMA8xDTALBgNVBAMTBHRlc3Qw\n" +
                "HhcNMTEwNjEzMTc0MjA4WhcNMzEwNjA4MTc0MjA4WjAPMQ0wCwYDVQQDEwR0ZXN0MFwwDQYJKoZI\n" +
                "hvcNAQEBBQADSwAwSAJBANVL9wbE5Trbcm+0O95CXrGERfjQMcFeLpsFrKffU+3du7RFc0E9z7d/\n" +
                "xs1pgQ55NlAPZdwhafZAX5b6zqWDS6kCAwEAAaOBtjCBszAOBgNVHQ8BAf8EBAMCBeAwEgYDVR0l\n" +
                "AQH/BAgwBgYEVR0lADAdBgNVHQ4EFgQU3B7D0UH8sGGF6lXi2Dzeto9IsLgwHwYDVR0jBBgwFoAU\n" +
                "3B7D0UH8sGGF6lXi2Dzeto9IsLgwTQYDVR0fBEYwRDAgoB6gHIYabGRhcDovL2xkYXBvbmUuZXhh\n" +
                "bXBsZS5jb20wIKAeoByGGmh0dHA6Ly9odHRwb25lLmV4YW1wbGUuY29tMA0GCSqGSIb3DQEBBQUA\n" +
                "A0EAx6lyUdXG4nPv66KYct/AEzAQ6sMaLm/szN4y0X26f+W2mGwqGCw+QlACfLNcsg/jSegNOBcw\n" +
                "5keIg5FgX1nY9w==\n" +
                "-----END CERTIFICATE-----";

        X509Certificate certificate = CertUtils.decodeFromPEM(certificatePerm);
        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Wrong number of CRL urls", crlUrls.length == 2);
        assertEquals("CRL url missing or invalid", "ldap://ldapone.example.com", crlUrls[0]);
        assertEquals("CRL url missing or invalid", "http://httpone.example.com", crlUrls[1]);
    }

    @Test
    public void testAuthorityInformationAccessUris() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(GOOGLE_PEM);

        String[] ocspUrls = CertUtils.getAuthorityInformationAccessUris(certificate, "1.3.6.1.5.5.7.48.1");
        assertNotNull("No OCSP urls", ocspUrls);
        assertEquals("OCSP url not found.", "http://ocsp.thawte.com", ocspUrls[0]);

        String[] crtUrls = CertUtils.getAuthorityInformationAccessUris(certificate, "1.3.6.1.5.5.7.48.2");
        assertNotNull("No CRT urls", crtUrls);
        assertEquals("CRT url not found", "http://www.thawte.com/repository/Thawte_SGC_CA.crt", crtUrls[0]);

    }

    @Test
    public void testAuthorityKeyIdentifierIssuerAndSerial() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(REDHAT_PEM);

        AuthorityKeyIdentifierStructure aki = CertUtils.getAKIStructure(certificate);
        BigInteger serial = CertUtils.getAKIAuthorityCertSerialNumber(aki);
        String issuerDn = CertUtils.getAKIAuthorityCertIssuer(aki);

        assertEquals("Serial number not correctly processed", BigInteger.valueOf(0), serial);
        assertEquals("Issuer DN not correctly processed", "1.2.840.113549.1.9.1=#160f72686e73407265646861742e636f6d,cn=rhns certificate authority,ou=red hat network services,o=red hat\\, inc.,l=research triangle park,st=north carolina,c=us", issuerDn);
    }

    @Test
    public void testAuthorityKeyIdentifierKeyIdentifier() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(REDHAT_PEM);

        AuthorityKeyIdentifierStructure aki = CertUtils.getAKIStructure(certificate);
        String base64KI = CertUtils.getAKIKeyIdentifier(aki);

        assertEquals("KeyIdentifier not correctly processed", "VBXNnyz37A0f0qi+TAesiD77mwo=", base64KI);
    }

    @Test
    @BugNumber(11302)
    public void testMatchingWildCard() throws Exception {
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise", "dc=layer7-*, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise", "dc=*layer7-*, DC=com, UID=*", false));
        assertFalse(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise", "dc=layer7-*.com, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech.com,dc=com, uid=acruise", "dc=layer7-*.com, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech.com,dc=com, uid=acruise", "dc=layer7-tech.com, DC=com, UID=*", false));
        assertFalse(CertUtils.dnMatchesPattern("dc=layer7-tech-com,dc=com, uid=acruise", "dc=layer7-tech.com, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech.com,dc=com, uid=acruise", "dc=layer7*com, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech.com,dc=com, uid=acruise", "dc=*yer7*com, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech.com,dc=com, uid=acruise", "dc=*com, DC=com, UID=*", false));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech.com,dc=com, uid=acruise", "dc=*layer7-tech.com, DC=com", false));
        assertFalse(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com", "dc=layer7.*, DC=com, UID=a.*e", false));
    }

    @Test
    @BugNumber(11302)
    public void testMatchingRegex() throws Exception {
        assertFalse(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise", "dc=layer7*, DC=com", true));
        assertTrue(CertUtils.dnMatchesPattern("dc=LaYer7Tech,dc=com, uid=acruise", "dc=(?i)layer7tech, DC=com", true));
        assertTrue(CertUtils.dnMatchesPattern("dc=Layer7-tech.com,dc=com, uid=acruise", "dc=(?i)layer7-tech\\.(?:com|net), DC=com", true));
        assertTrue(CertUtils.dnMatchesPattern("dc=Layer7-tech.net,dc=com, uid=acruise", "dc=(?i)layer7-tech\\.(?:com|net), DC=com", true));
        assertTrue(CertUtils.dnMatchesPattern("dc=Layer7-tech.net,dc=com, uid=acruise", "DC=com", true));
        assertTrue(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise", "dc=layer7.*, DC=com, UID=a.*e", true));
        assertFalse(CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com", "dc=layer7.*, DC=com, UID=a.*e", true));
    }

    @Test
    @BugNumber(11722)
    public void testSAN() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(BUG_11722_SHOW_SAN);
        List<Pair<String, String>> attrs = CertUtils.getCertProperties(certificate);
        boolean hasSAN = false;
        for (Pair<String, String> pair: attrs) {
            if (pair.getKey().equals(CertUtils.CERT_PROP_SAN)) {
                assertNotNull(pair.getValue());
                hasSAN = true;
            }
        }
        assertTrue(hasSAN);
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

    private static final String BUG_9539_PORTAL_SPACES =
            "MIID3jCCA0egAwIBAgIDAMzBMA0GCSqGSIb3DQEBBQUAMFcxCzAJBgNVBAYTAlVTMRgwFgYDVQQK\n" +
            "Ew9VLlMuIEdvdmVybm1lbnQxDDAKBgNVBAsTA0RvRDEMMAoGA1UECxMDUEtJMRIwEAYDVQQDEwlE\n" +
            "T0QgQ0EtMTMwHhcNMDgwNDA4MTgxNjU0WhcNMTEwNDA5MTgxNjU0WjBwMQswCQYDVQQGEwJVUzEY\n" +
            "MBYGA1UEChMPVS5TLiBHb3Zlcm5tZW50MQwwCgYDVQQLEwNEb0QxDDAKBgNVBAsTA1BLSTENMAsG\n" +
            "A1UECxMERElTQTEcMBoGA1UEAxMTcG9ydGFsLnNvYWYuY2VzLm1pbDCBnzANBgkqhkiG9w0BAQEF\n" +
            "AAOBjQAwgYkCgYEAuysRB2FGiZecD80rNHrhfM0QXnXfXqycqvskYXmR/Af9wOIyvg/65LH8xmmW\n" +
            "2PTfa5EvHCVdH6ytdMPptaC5XVmGJKBGH1Q5xq53pPptBJlhB8IxEGRqU4UfYrPzCb1cs4MIMOXx\n" +
            "9L8KAo1AZJ/RYZikdZmoUVSD/ZObcNxMaOcCAwEAAaOCAZ0wggGZMB8GA1UdIwQYMBaAFGRkQyWk\n" +
            "bOcNIh1lrMDkdTfMBNraMB0GA1UdDgQWBBRvhzt8jTXNjfcRNJKMTbyQe8OHgTAOBgNVHQ8BAf8E\n" +
            "BAMCBaAwgccGA1UdHwSBvzCBvDAtoCugKYYnIGh0dHA6Ly9jcmwuZGlzYS5taWwvZ2V0Y3JsP0RP\n" +
            "RCUyMENBLTEzMIGKoIGHoIGEhoGBIGxkYXA6Ly9jcmwuZ2RzLmRpc2EubWlsL2NuJTNkRE9EJTIw\n" +
            "Q0EtMTMlMmNvdSUzZFBLSSUyY291JTNkRG9EJTJjbyUzZFUuUy4lMjBHb3Zlcm5tZW50JTJjYyUz\n" +
            "ZFVTP2NlcnRpZmljYXRlcmV2b2NhdGlvbmxpc3Q7YmluYXJ5MBYGA1UdIAQPMA0wCwYJYIZIAWUC\n" +
            "AQsFMGUGCCsGAQUFBwEBBFkwVzAzBggrBgEFBQcwAoYnaHR0cDovL2NybC5kaXNhLm1pbC9nZXRz\n" +
            "aWduP0RPRCUyMENBLTEzMCAGCCsGAQUFBzABhhRodHRwOi8vb2NzcC5kaXNhLm1pbDANBgkqhkiG\n" +
            "9w0BAQUFAAOBgQA8oJ2sfVZhrD1sHTtUKZT2YAjY9hsjAKrVAgItVKD8sQorIN8bbc+via0UfXiP\n" +
            "6OCUj1Ues6IDTEJ9z5hDewXYfyEgYEjImNxBFf889ndDzzYdoxyOLXEIwZlG1TxxZUuV+EMPkflZ\n" +
            "ln93k/OYqL8Ux7knj/TERR5js+EhWpBVKg==";

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
    
    private static final String BUG_11722_SHOW_SAN =
            "MIIG3TCCBcWgAwIBAgIQDqQCSM2Tudk9/jiTo8OoQDANBgkqhkiG9w0BAQUFADBmMQswCQYDVQQG\n" +
            "EwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMRkwFwYDVQQLExB3d3cuZGlnaWNlcnQuY29tMSUw\n" +
            "IwYDVQQDExxEaWdpQ2VydCBIaWdoIEFzc3VyYW5jZSBDQS0zMB4XDTExMDUwOTAwMDAwMFoXDTEz\n" +
            "MDUyOTEyMDAwMFowgYYxCzAJBgNVBAYTAlVTMQ4wDAYDVQQIEwVUZXhhczEUMBIGA1UEBxMLU2Fu\n" +
            "IEFudG9uaW8xGzAZBgNVBAoTElJhY2tzcGFjZSBVUywgSW5jLjEVMBMGA1UECxMMRXhjaGFuZ2Ug\n" +
            "T3BzMR0wGwYDVQQDExRtZXgwN2EubWFpbHRydXN0LmNvbTCCASIwDQYJKoZIhvcNAQEBBQADggEP\n" +
            "ADCCAQoCggEBALNRR3z+LWP2v6OPLY519lFLqyr2WwyFtoGEtsc+C9/Bw5WBQx5uArTZF0sLKL2T\n" +
            "5nUqVmCod/i1OoD+yIXcEW7yQUIcXKIjuYLR43vuknxxIXNnHmSQf+PlpSMfCafw9OsYCR56YtJx\n" +
            "cjRmBLxQkTQU/6tBbdJY0267Fq8jdC2jTFIP+B+hTsFPbCArjIiBvqsZW/h0j/PfGgCZVIgbd8Sv\n" +
            "w3spJxIP59r3dYIISFAmzXV9OaUc9IcCcAfJdJcegvwrhYlrTRsl7c5s6WjOVY961934Vc9MBbDu\n" +
            "JpsIjin7JTOluYxYORQ92k40qnR2dHlu/5YCWmzoICN0/Qz6b7kCAwEAAaOCA2QwggNgMB8GA1Ud\n" +
            "IwQYMBaAFFDqc4nbKfsQj57lASDU3nmZSIP3MB0GA1UdDgQWBBQDHcapT/ISvan/duXhy+2YehU1\n" +
            "yjA1BgNVHREELjAsghRtZXgwN2EubWFpbHRydXN0LmNvbYIUbWV4MDdhLmVtYWlsc3J2ci5jb20w\n" +
            "ggHEBgNVHSAEggG7MIIBtzCCAbMGCWCGSAGG/WwBATCCAaQwOgYIKwYBBQUHAgEWLmh0dHA6Ly93\n" +
            "d3cuZGlnaWNlcnQuY29tL3NzbC1jcHMtcmVwb3NpdG9yeS5odG0wggFkBggrBgEFBQcCAjCCAVYe\n" +
            "ggFSAEEAbgB5ACAAdQBzAGUAIABvAGYAIAB0AGgAaQBzACAAQwBlAHIAdABpAGYAaQBjAGEAdABl\n" +
            "ACAAYwBvAG4AcwB0AGkAdAB1AHQAZQBzACAAYQBjAGMAZQBwAHQAYQBuAGMAZQAgAG8AZgAgAHQA\n" +
            "aABlACAARABpAGcAaQBDAGUAcgB0ACAAQwBQAC8AQwBQAFMAIABhAG4AZAAgAHQAaABlACAAUgBl\n" +
            "AGwAeQBpAG4AZwAgAFAAYQByAHQAeQAgAEEAZwByAGUAZQBtAGUAbgB0ACAAdwBoAGkAYwBoACAA\n" +
            "bABpAG0AaQB0ACAAbABpAGEAYgBpAGwAaQB0AHkAIABhAG4AZAAgAGEAcgBlACAAaQBuAGMAbwBy\n" +
            "AHAAbwByAGEAdABlAGQAIABoAGUAcgBlAGkAbgAgAGIAeQAgAHIAZQBmAGUAcgBlAG4AYwBlAC4w\n" +
            "ewYIKwYBBQUHAQEEbzBtMCQGCCsGAQUFBzABhhhodHRwOi8vb2NzcC5kaWdpY2VydC5jb20wRQYI\n" +
            "KwYBBQUHMAKGOWh0dHA6Ly9jYWNlcnRzLmRpZ2ljZXJ0LmNvbS9EaWdpQ2VydEhpZ2hBc3N1cmFu\n" +
            "Y2VDQS0zLmNydDAMBgNVHRMBAf8EAjAAMGUGA1UdHwReMFwwLKAqoCiGJmh0dHA6Ly9jcmwzLmRp\n" +
            "Z2ljZXJ0LmNvbS9jYTMtMjAxMWUuY3JsMCygKqAohiZodHRwOi8vY3JsNC5kaWdpY2VydC5jb20v\n" +
            "Y2EzLTIwMTFlLmNybDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDgYDVR0PAQH/BAQD\n" +
            "AgWgMA0GCSqGSIb3DQEBBQUAA4IBAQA6XvIB5O0bpGeG0fNawKeZqyo8kRwGrTsNxqB6NmqhkY0Q\n" +
            "SQLAFV66RFKDqGC9KgGXGEOvn1gt6PzQKVB+3M62iTlo5c0cTaVThj+8HECIdJc1yqftWMCiRQfR\n" +
            "ly/4ZHByr4VslADEQ9PDu1o45PB8qCZZAvP9PDE/tNBBd2ktTBBvGAn5lNTrNSIaqCr8Y1lmaTs5\n" +
            "gjePYkAkbVGpu/QoiwmbCAEz0GcPpxBF2SQdJnBuARvEJ35rHdP0A1ryKWaT6sUbCJHNDZNpl6kN\n" +
            "vsRR9QwrI4lCj8+qqcNNANvpec86xti4Tx128ptXkGSuta27HAVQqXrS4jK7zpFheEy6";

}
