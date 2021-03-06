package com.l7tech.common.io;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.TestKeys;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.ArrayUtils;
import com.l7tech.util.HexUtils;
import com.l7tech.util.NameValuePair;
import com.l7tech.util.Pair;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigInteger;
import java.net.InetAddress;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        assertTrue("Canonical match", CertUtils.isEqualDNCanonical("ou=blah+cn=abc", "cn=ABC+OU=Blah"));
        assertTrue( "Canonical match naming", CertUtils.isEqualDNCanonical("email=test@test.com+s=ca+dnq=test.com", "EMAILADDRESS=test@test.com+ST=ca+DNQUALIFIER=test.com") );
        assertFalse("Basic mismatch", CertUtils.isEqualDNCanonical("cn=a", "cn=b"));
    }

    @Test
    public void testValidDN() {
        assertTrue( "Valid DN", CertUtils.isValidDN("CN=bug5722_child, O=OASIS, ST=NJ, DNQ=org, EMAILADDRESS=support@simpson.org") );
        assertFalse( "Invalid DN", CertUtils.isValidDN("CN=bug5722_child, O=OASIS, S=NJ, DNQUALIFIER=org, EMAIL=support@simpson.org") );
        assertTrue("Valid DN with explicit known OIDs", CertUtils.isValidDN("cn=bug5722_child,o=oasis,st=nj,2.5.4.46=#13036f7267,1.2.840.113549.1.9.1=#1613737570706f72744073696d70736f6e2e6f7267"));
        assertTrue("Valid DN with explicit unknown OIDs", CertUtils.isValidDN("cn=bug5722_child,2.5.4.46342342=#1613737570706f72744073696d70736f6e2e6f7267"));
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

        assertEquals("DNQ DN", "cn=bug5722_child,o=oasis,st=nj,2.5.4.46=org,1.2.840.113549.1.9.1=support@simpson.org", CertUtils.formatDN("cn=bug5722_child,o=oasis,st=nj,2.5.4.46=#13036f7267,1.2.840.113549.1.9.1=#1613737570706f72744073696d70736f6e2e6f7267"));
        assertEquals( "Formatted Subject DN 1", CertUtils.formatDN( "CN=URN:BAE:TEST:3:LAYER7, OU=APL_GIG_TESTBED, O=JHU-APL, ST=MD, C=US" ), CertUtils.getSubjectDN(cert) );
        assertEquals( "Formatted Subject DN 2", CertUtils.formatDN( "CN=www.thawte.com, L=Mountain View, ST=California, C=US, SERIALNUMBER=3898261, O=Thawte Inc, OID.2.5.4.15=\"V1.0, Clause 5.(b)\", OID.1.3.6.1.4.1.311.60.2.1.2=Delaware, OID.1.3.6.1.4.1.311.60.2.1.3=US" ), CertUtils.getSubjectDN(cert2) );
        // The following test is not generally applicable, but is true for this test data
        assertEquals("Subject DN 1", cert.getSubjectDN().getName().toLowerCase().replaceAll(" ", ""), CertUtils.getSubjectDN(cert));
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
        System.out.println((double) i / t * 1000 + " iterations per second");
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

        final X509Certificate certificate = CertUtils.decodeFromPEM(certificatePem);
        final String fingerprint = CertUtils.getCertificateFingerprint(certificate, "SHA1").substring(5);
        assertEquals("Fingerprint only", "F6:F6:1A:8B:28:A2:06:1F:5B:62:8C:C8:22:CC:6C:64:D2:5A:D2:79", fingerprint);
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
    public void testNetScapeCRLURL() throws Exception {
        String NETSCAPE_CRL_PEM = "-----BEGIN CERTIFICATE-----\n" +
                "MIIHPTCCBSWgAwIBAgIBADANBgkqhkiG9w0BAQQFADB5MRAwDgYDVQQKEwdSb290\n" +
                "IENBMR4wHAYDVQQLExVodHRwOi8vd3d3LmNhY2VydC5vcmcxIjAgBgNVBAMTGUNB\n" +
                "IENlcnQgU2lnbmluZyBBdXRob3JpdHkxITAfBgkqhkiG9w0BCQEWEnN1cHBvcnRA\n" +
                "Y2FjZXJ0Lm9yZzAeFw0wMzAzMzAxMjI5NDlaFw0zMzAzMjkxMjI5NDlaMHkxEDAO\n" +
                "BgNVBAoTB1Jvb3QgQ0ExHjAcBgNVBAsTFWh0dHA6Ly93d3cuY2FjZXJ0Lm9yZzEi\n" +
                "MCAGA1UEAxMZQ0EgQ2VydCBTaWduaW5nIEF1dGhvcml0eTEhMB8GCSqGSIb3DQEJ\n" +
                "ARYSc3VwcG9ydEBjYWNlcnQub3JnMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIIC\n" +
                "CgKCAgEAziLA4kZ97DYoB1CW8qAzQIxL8TtmPzHlawI229Z89vGIj053NgVBlfkJ\n" +
                "8BLPRoZzYLdufujAWGSuzbCtRRcMY/pnCujW0r8+55jE8Ez64AO7NV1sId6eINm6\n" +
                "zWYyN3L69wj1x81YyY7nDl7qPv4coRQKFWyGhFtkZip6qUtTefWIonvuLwphK42y\n" +
                "fk1WpRPs6tqSnqxEQR5YYGUFZvjARL3LlPdCfgv3ZWiYUQXw8wWRBB0bF4LsyFe7\n" +
                "w2t6iPGwcswlWyCR7BYCEo8y6RcYSNDHBS4CMEK4JZwFaz+qOqfrU0j36NK2B5jc\n" +
                "G8Y0f3/JHIJ6BVgrCFvzOKKrF11myZjXnhCLotLddJr3cQxyYN/Nb5gznZY0dj4k\n" +
                "epKwDpUeb+agRThHqtdB7Uq3EvbXG4OKDy7YCbZZ16oE/9KTfWgu3YtLq1i6L43q\n" +
                "laegw1SJpfvbi1EinbLDvhG+LJGGi5Z4rSDTii8aP8bQUWWHIbEZAWV/RRyH9XzQ\n" +
                "QUxPKZgh/TMfdQwEUfoZd9vUFBzugcMd9Zi3aQaRIt0AUMyBMawSB3s42mhb5ivU\n" +
                "fslfrejrckzzAeVLIL+aplfKkQABi6F1ITe1Yw1nPkZPcCBnzsXWWdsC4PDSy826\n" +
                "YreQQejdIOQpvGQpQsgi3Hia/0PsmBsJUUtaWsJx8cTLc6nloQsCAwEAAaOCAc4w\n" +
                "ggHKMB0GA1UdDgQWBBQWtTIb1Mfz4OaO873SsDrusjkY0TCBowYDVR0jBIGbMIGY\n" +
                "gBQWtTIb1Mfz4OaO873SsDrusjkY0aF9pHsweTEQMA4GA1UEChMHUm9vdCBDQTEe\n" +
                "MBwGA1UECxMVaHR0cDovL3d3dy5jYWNlcnQub3JnMSIwIAYDVQQDExlDQSBDZXJ0\n" +
                "IFNpZ25pbmcgQXV0aG9yaXR5MSEwHwYJKoZIhvcNAQkBFhJzdXBwb3J0QGNhY2Vy\n" +
                "dC5vcmeCAQAwDwYDVR0TAQH/BAUwAwEB/zAyBgNVHR8EKzApMCegJaAjhiFodHRw\n" +
                "czovL3d3dy5jYWNlcnQub3JnL3Jldm9rZS5jcmwwMAYJYIZIAYb4QgEEBCMWIWh0\n" +
                "dHBzOi8vd3d3LmNhY2VydC5vcmcvcmV2b2tlLmNybDA0BglghkgBhvhCAQgEJxYl\n" +
                "aHR0cDovL3d3dy5jYWNlcnQub3JnL2luZGV4LnBocD9pZD0xMDBWBglghkgBhvhC\n" +
                "AQ0ESRZHVG8gZ2V0IHlvdXIgb3duIGNlcnRpZmljYXRlIGZvciBGUkVFIGhlYWQg\n" +
                "b3ZlciB0byBodHRwOi8vd3d3LmNhY2VydC5vcmcwDQYJKoZIhvcNAQEEBQADggIB\n" +
                "ACjH7pyCArpcgBLKNQodgW+JapnM8mgPf6fhjViVPr3yBsOQWqy1YPaZQwGjiHCc\n" +
                "nWKdpIevZ1gNMDY75q1I08t0AoZxPuIrA2jxNGJARjtT6ij0rPtmlVOKTV39O9lg\n" +
                "18p5aTuxZZKmxoGCXJzN600BiqXfEVWqFcofN8CCmHBh22p8lqOOLlQ+TyGpkO/c\n" +
                "gr/c6EWtTZBzCDyUZbAEmXZ/4rzCahWqlwQ3JNgelE5tDlG+1sSPypZt90Pf6DBl\n" +
                "Jzt7u0NDY8RD97LsaMzhGY4i+5jhe1o+ATc7iwiwovOVThrLm82asduycPAtStvY\n" +
                "sONvRUgzEv/+PDIqVPfE94rwiCPCR/5kenHA0R6mY7AHfqQv0wGP3J8rtsYIqQ+T\n" +
                "SCX8Ev2fQtzzxD72V7DX3WnRBnc0CkvSyqD/HMaMyRa+xMwyN2hzXwj7UfdJUzYF\n" +
                "CpUCTPJ5GhD22Dp1nPMd8aINcGeGG7MW9S/lpOt5hvk9C8JzC6WZrG/8Z7jlLwum\n" +
                "GCSNe9FINSkYQKyTYOGWhlC0elnYjyELn8+CkcY7v2vcB5G5l1YjqrZslMZIBjzk\n" +
                "zk6q5PYvCdxTby78dOs6Y5nCpqyJvKeyRKANihDjbPIky/qbn3BHLt4Ui9SyIAmW\n" +
                "omTxJBzcoTWcFbLUvFUufQb1nA5V9FrWk9p2rSVzTMVD\n" +
                "-----END CERTIFICATE-----";

        X509Certificate certificate = CertUtils.decodeFromPEM(NETSCAPE_CRL_PEM);

        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Empty CRL urls", crlUrls.length > 0);
        assertEquals("CRL url missing or invalid", "https://www.cacert.org/revoke.crl", crlUrls[0]);
    }

    @Test
    public void testCrlRelName() throws Exception {
        /**
         * Test cert with relative names in CRL
         */
        final String CRL_RELNAME_PEM =
                "-----BEGIN CERTIFICATE-----\n" +
                "MIIFtzCCA5+gAwIBAgIJANIdGu5YNrI1MA0GCSqGSIb3DQEBBQUAMIGKMQswCQYD\n" +
                "VQQGEwJDQTEZMBcGA1UECAwQQnJpdGlzaCBDb2x1bWJpYTESMBAGA1UEBwwJVmFu\n" +
                "Y291dmVyMRAwDgYDVQQKDAdDYSB0ZWNoMQ0wCwYDVQQLDARBUElNMQ8wDQYDVQQD\n" +
                "DAZjYS5jb20xGjAYBgkqhkiG9w0BCQEWC3l2ZXNAY2EuY29tMB4XDTE4MDgwOTE3\n" +
                "MzMzMVoXDTE4MDkwODE3MzMzMVowgYoxCzAJBgNVBAYTAkNBMRkwFwYDVQQIDBBC\n" +
                "cml0aXNoIENvbHVtYmlhMRIwEAYDVQQHDAlWYW5jb3V2ZXIxEDAOBgNVBAoMB0Nh\n" +
                "IHRlY2gxDTALBgNVBAsMBEFQSU0xDzANBgNVBAMMBmNhLmNvbTEaMBgGCSqGSIb3\n" +
                "DQEJARYLeXZlc0BjYS5jb20wggIiMA0GCSqGSIb3DQEBAQUAA4ICDwAwggIKAoIC\n" +
                "AQCytUFfPB4BcKclyGnzORx/t1jGoBAE0JHC92+MSpcyleGVB/kwgljDl1+9iYez\n" +
                "x0eTlp8iJ7xXrdVByOFo7jXV2dx+TBjBzcHUXcbr88ER78AZNaOIFjWbA+m4dQVy\n" +
                "W0gPy+EOb+voQDTXY6TtKph3DUbSJAEJR9SUza74Fnf0Pqz4+LgxMGz3Q+KSVCN6\n" +
                "8CINC2TfvIIxUxaG2jhpOG1u7VW/VPP+FlcPigpEqAZ9Upgc0DfGFIu60iIeKTGo\n" +
                "wfsvGpuX4rzxizhV24TYTH+iTFES2HQtbNChNUUT9AUW62BTB3JdJZTbmmkH1n7l\n" +
                "tmCt9s9soxFu+xXxSf8NQ3ilkuJog/WrTdVuSTeyXIekTpv44xwD6R/xyl9pkPrp\n" +
                "Yo3vJDTeTrUk4KIqrIH8w2UaIO7bpkaVDLt6nnCxqPBpWLA9q5vDf4+Wpbmp9Xe3\n" +
                "KoGFGEo9vvrdYlNXjudj60533QSy+SlePXjf3xKkJZyNWAnOY9tpE+f2UCHXq2H/\n" +
                "BDZS60IRjFHrir4S2WVrQtq5sN4q7F0KtHJxJNc09kRYxstejOdGb/XXROmoyI81\n" +
                "HA+4kVL0N6QptDFjIKn8IyESfSLKSOmUjVPQvkVqmQYW/i+bCPhNyI0gmnWttAhv\n" +
                "gzUAopIJkiLgVUFembIP9Y3OclOQgMuuQ1NQDTVSzBxRHwIDAQABox4wHDAaBgNV\n" +
                "HR8EEzARMA+gDaELMAkGA1UEBhMCVVMwDQYJKoZIhvcNAQEFBQADggIBAFq6Vh0P\n" +
                "6rfBI026G9ftrMq9Ym4QQmf52/PgK7Lo9uz0aEtz8wzcFK7lE/wycoRGCOSkFeLp\n" +
                "qpOvBfQyciTyUxU1+azbiH9WE4EdAFKumEG+FR6rj9S848TpQTa8llpp+mUEt2lE\n" +
                "71YqXLxg4X2FPIEsCeNXQpfXohnfbm8QQvUue7+2SbuQmrtau8/cddSSv2HhiBG7\n" +
                "+/JCcfZqVWJbcqPSYJSMgVODdL8AQWZBiybjMa2+DVgxmc1g8nhOX8jrrnxPrqXI\n" +
                "n69xcK+6g+8Mgpjbd2r7zYZyg9KRyQmuTG58Rgocfl7VfSgWqx5cFy/rS/YFNRLg\n" +
                "rHOXaXXtM6Rz1gClWpTeNSOcJ/rAXqfcmFAXXkHwQ9VljLyA3hIHv80W6hvYP7eF\n" +
                "CtANQWbjIuOqHFyClyxo42Jn93UbPyPOe07XS9oWa5iuFoOej7mUA7R07VCveQJf\n" +
                "FLKJgLxWqFM+p+CqgdjddL7EB/rGuCpV7DQSBh6f9nx5/bduzBKTY560C686Frrs\n" +
                "dyMWjywPTIC48uQmaXQ8qiCJT2EqxPnntavcoXaYWdm5iE0TB1oChVJm7NEKG9WY\n" +
                "ffA+SxBRpL+sh01VpaOIbAPHVNfRhZJqqChoZy0FSFFWS5qEZrgm2bKPRDEUGHG6\n" +
                "RTSMjV26J+Kewd+B+/AMDP2xRierT4jlIlgA\n" +
                "-----END CERTIFICATE-----\n";

        X509Certificate certificate = CertUtils.decodeFromPEM(CRL_RELNAME_PEM);
        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertTrue("Empty CRL urls, since relative name is not supported", crlUrls.length == 0);
    }

    @Test
    @BugNumber(10713)
    public void testCRLURLAlice() throws Exception {
        X509Certificate certificate = TestDocuments.getWssInteropAliceCert();
        String[] crlUrls = CertUtils.getCrlUrls(certificate);
        assertNotNull("Null CRL urls", crlUrls);
        assertEquals("CRL urls", 0L, (long) crlUrls.length);
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

        AuthorityKeyIdentifier aki = CertUtils.getAKI(certificate);
        BigInteger serial = CertUtils.getAKIAuthorityCertSerialNumber(aki);
        String issuerDn = CertUtils.getAKIAuthorityCertIssuer(aki);

        assertEquals("Serial number not correctly processed", BigInteger.valueOf(0), serial);
        assertEquals("Issuer DN not correctly processed", "1.2.840.113549.1.9.1=#160f72686e73407265646861742e636f6d,cn=rhns certificate authority,ou=red hat network services,o=red hat\\, inc.,l=research triangle park,st=north carolina,c=us", issuerDn);
    }

    @Test
    public void testAuthorityKeyIdentifierKeyIdentifier() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(REDHAT_PEM);

        AuthorityKeyIdentifier aki = CertUtils.getAKI(certificate);
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
    @BugNumber(13589)
    public void testMatchUsesCanonicalForm() throws Exception {
        // The match should ignore the case of the DN attribute names as well as formatting details (whitespace around commas, etc)
        // Non-regex matches should canonicalize the DN pattern.  Regex matches should compile the regex in case_insensitive mode.
        assertTrue(CertUtils.dnMatchesPattern("cn=tester1.xml-gw.mastercard.com,ou=xml gw client1,o=layer7,st=missouri,c=us", "CN=tester*.xml-gw.mastercard.com, OU=XML GW client*", false));
        assertTrue(CertUtils.dnMatchesPattern("cn=tester1.xml-gw.mastercard.com,ou=xml gw client1,o=layer7,st=missouri,c=us", "CN=tester.*.xml-gw.mastercard.com, OU=XML GW client.*", true));
        assertFalse(CertUtils.dnMatchesPattern("cn=tester1.xml-gw.mastercard.com,ou=xml gw client1,o=layer7,st=missouri,c=us", "CN=tester*.xml-gw.mastercard.com, OU=XML GW client*", true));
        assertFalse(CertUtils.dnMatchesPattern("cn=tester1.xml-gw.mastercard.com,ou=xml gw client1,o=layer7,st=missouri,c=us", "CN=tester.*.xml-gw.mastercard.com, OU=XML GW client.*", false));
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

    @Test
    @BugNumber(13337)
    public void testSANWithPropertiesName() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(BUG_13337_WITH_SAN);
        List<Pair<String, String>> attrs = CertUtils.getCertProperties(certificate);
        //SSG-8286 - The order of the returned values changed in JDK 1.7.0_u51.
        // These only appear to be used for display so their order is not required.
        // Making sure all properties are found.
        Collection<String> expectedSANProperties = Arrays.asList(("URL=http://petrov.ca/\n" +
                "IP Address=192.168.0.2\n" +
                "IP Address=192.168.0.1\n" +
                "DNS Name=petrov.ca\n" +
                "RFC822 Name=stoyan.petrov@gmail.com\n" +
                "Directory Name=1.2.840.113549.1.9.1=stoyan@petrov.ca,cn=fish,ou=petrovs,o=fish corp.,c=bg\n" +
                "DNS Name=yahoo.com\n").split("\n"));
        boolean found = false;
        for (Pair<String, String> pair: attrs) {
            if (pair.getKey().equals(CertUtils.CERT_PROP_SAN)) {
                found = true;
                Assert.assertNotNull("SAN properties must not be null", pair.getValue());
                String[] foundProperties = pair.getValue().split("\n");
                Assert.assertEquals("The number of expected properties is not equal to the number of found properties.", expectedSANProperties.size(), foundProperties.length);
                for(String property : foundProperties) {
                    Assert.assertTrue("Found unexpected SAN property: " + property, expectedSANProperties.contains(property));
                }
            }
        }

        Assert.assertTrue("Could not find SAN property", found);
    }

    @Test
    @BugNumber(13422)
    public void testSANWithByteArrayPropertiesName() throws Exception {
        X509Certificate certificate = CertUtils.decodeFromPEM(BUG_13422_CERT_WITH_BYTE_ARRAY_SAN);
        List<Pair<String, String>> attrs = CertUtils.getCertProperties(certificate);
        String expected = "DNS Name=tacoma.seattle.local\n";
        for (Pair<String, String> pair: attrs) {
            if (pair.getKey().equals(CertUtils.CERT_PROP_SAN)) {
                assertEquals(expected, pair.getValue());
            }
        }
    }

    @Test
    public void testParsePemCertNoLineBreaks() throws Exception {
        String nolf = GOOGLE_PEM.replace("\n", "");
        byte[] got = CertUtils.decodeCertBytesFromPEM(nolf, true);
        final X509Certificate cert = CertUtils.decodeCert(got);
        assertNotNull(cert);
    }

    @Test
    public void testGetCertProperties() throws Exception {
        String propertiesTestCert = "-----BEGIN CERTIFICATE-----\n" +
            "MIICJDCCAY2gAwIBAgIJANtlD/ubhZo0MA0GCSqGSIb3DQEBDAUAMBAxDjAMBgNVBAMTBWJsb3Jn\n" +
            "MB4XDTE0MTAyMTE3MTg1NloXDTM0MTAxNjE3MTg1NlowEDEOMAwGA1UEAxMFYmxvcmcwgZ8wDQYJ\n" +
            "KoZIhvcNAQEBBQADgY0AMIGJAoGBAI/0hxogzZp4RlYflk1iCYES8w5KlwCPHzdoB3CKbbosGgpD\n" +
            "lhtZrsDqGkd1EPb78DOYp41xxLob2hG9g6NIiV2+NHQLuklWzqBRIwDlmMe+wcp5Fur+EqH0di3k\n" +
            "IG5pfWpLZGtUNeJJXtZB0fT8GzcSuQaxyA0XxSMkOLcePfWdAgMBAAGjgYUwgYIwEgYDVR0TAQH/\n" +
            "BAgwBgEB/wIBBDAOBgNVHQ8BAf8EBAMCASIwHAYDVR0lAQH/BBIwEAYEVR0lAAYIKwYBBQUHAwMw\n" +
            "HQYDVR0OBBYEFOfnKT3VxNomMMsKMYWysmxxKOb3MB8GA1UdIwQYMBaAFOfnKT3VxNomMMsKMYWy\n" +
            "smxxKOb3MA0GCSqGSIb3DQEBDAUAA4GBAEgf+SmjwsixO2A+oGVxQve63MJvZMvISge4l9lG6QcU\n" +
            "q1upQQ8JEid223g/Qy1H2E50aKj9KvJyykSFpdeDuEJyWuj/YNPgTCL+pteUHoUUnotapfp0aKVq\n" +
            "f+pWFbcdrZnlEnksKTTn7MJ88PHOI6uiyANfeMHzgMIr90T/17rD\n" +
            "-----END CERTIFICATE-----";

        X509Certificate cert = TestKeys.getCert(propertiesTestCert);

        List<Pair<String,String>> props = CertUtils.getCertProperties(cert);

        assertTrue(props.contains(new Pair<>(CertUtils.CERT_PROP_KEY_USAGE, "Key Encipherment, CRL Signing")));
        assertTrue(props.contains(new Pair<>(CertUtils.CERT_PROP_EXT_KEY_USAGE, "any, id-kp-codeSigning")));
        assertTrue(props.contains(new Pair<>(CertUtils.CERT_PROP_ISSUED_TO, "CN=blorg")));
    }

    @Test
    public void testCheckForMismatchingKey_NoMismatch() throws Exception {
        Pair<X509Certificate, PrivateKey> certAndKeyA = TestKeys.getCertAndKey("RSA_512");

        CertUtils.checkForMismatchingKey(certAndKeyA.left, certAndKeyA.right);
    }

    @Test
    public void testCheckForMismatchingKey_KeyIsNotRSA_CertificateExceptionThrown() throws Exception {
        X509Certificate rsaCert = TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64);
        PrivateKey dsaKey = TestKeys.getKey("DSA", TestKeys.DSA_1024_KEY_PKCS8_B64);

        try {
            CertUtils.checkForMismatchingKey(rsaCert, dsaKey);
            fail("Should have failed due to detectable mismatching key");
        } catch (CertificateException e) {
            assertEquals("The specified key does not belong to the specified " +
                    "certificate [cn=test_rsa_1024].", e.getMessage());
        }
    }

    @Test
    public void testCheckForMismatchingKey_CertIsNotRSA_CertificateExceptionThrown() throws Exception {
        X509Certificate dsaCert = TestKeys.getCert(TestKeys.DSA_1024_CERT_X509_B64);
        PrivateKey rsaKey = TestKeys.getKey("RSA", TestKeys.RSA_1024_KEY_PKCS8_B64);

        try {
            CertUtils.checkForMismatchingKey(dsaCert, rsaKey);
            fail("Should have failed due to detectable mismatching key");
        } catch (CertificateException e) {
            assertEquals("The specified key does not belong to the specified " +
                    "certificate [cn=test_dsa_1024].", e.getMessage());
        }
    }

    @Test
    public void testCheckForMismatchingKey_RSAModulusDiffers_CertificateExceptionThrown() throws Exception {
        X509Certificate rsa1024Cert = TestKeys.getCert(TestKeys.RSA_1024_CERT_X509_B64);
        PrivateKey rsa2048Key = TestKeys.getKey("RSA", TestKeys.RSA_2048_KEY_PKCS8_B64);

        try {
            CertUtils.checkForMismatchingKey(rsa1024Cert, rsa2048Key);
            fail("Should have failed due to detectable mismatching key");
        } catch (CertificateException e) {
            assertEquals("The specified key's RSA modulus differs from that of the public key " +
                    "in the certificate [cn=test_rsa_1024].", e.getMessage());
        }
    }

    @Test
    public void testExtractSingleCommonNameFromCertificate_NoSubjectDN_IAEThrown() throws Exception {
        X509Certificate mockCert = mock(X509Certificate.class);

        when(mockCert.getSubjectDN()).thenReturn(null);

        try {
            CertUtils.extractSingleCommonNameFromCertificate(mockCert);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Certificate [] contains no subject DN", e.getMessage());
        }
    }

    @Test
    public void testExtractFirstCommonNameFromCertificate_NoSubjectDN_IAEThrown() throws Exception {
        X509Certificate mockCert = mock(X509Certificate.class);

        when(mockCert.getSubjectDN()).thenReturn(null);

        try {
            CertUtils.extractFirstCommonNameFromCertificate(mockCert);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Certificate [] contains no subject DN", e.getMessage());
        }
    }

    @Test
    public void testExtractCommonNamesFromCertificate_NoSubjectDN_IAEThrown() throws Exception {
        X509Certificate mockCert = mock(X509Certificate.class);

        when(mockCert.getSubjectDN()).thenReturn(null);

        try {
            CertUtils.extractCommonNamesFromCertificate(mockCert);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Certificate [] contains no subject DN", e.getMessage());
        }
    }

    @Test
    public void testExtractSingleIssuerNameFromCertificate_NoSubjectDN_IAEThrown() throws Exception {
        X509Certificate mockCert = mock(X509Certificate.class);

        when(mockCert.getIssuerDN()).thenReturn(null);

        try {
            CertUtils.extractSingleIssuerNameFromCertificate(mockCert);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Certificate [] contains no issuer DN", e.getMessage());
        }
    }

    @Test
    public void testExtractFirstIssuerNameFromCertificate_NoSubjectDN_IAEThrown() throws Exception {
        X509Certificate mockCert = mock(X509Certificate.class);

        when(mockCert.getIssuerDN()).thenReturn(null);

        try {
            CertUtils.extractFirstIssuerNameFromCertificate(mockCert);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Certificate [] contains no issuer DN", e.getMessage());
        }
    }

    @Test
    public void testExtractIssuerNamesFromCertificate_NoSubjectDN_IAEThrown() throws Exception {
        X509Certificate mockCert = mock(X509Certificate.class);

        when(mockCert.getIssuerDN()).thenReturn(null);

        try {
            CertUtils.extractIssuerNamesFromCertificate(mockCert);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertEquals("Certificate [] contains no issuer DN", e.getMessage());
        }
    }

    @Test
    @BugId( "SSG-11353" )
    public void testExtractCurveNameFromEcCert() throws Exception {
        PublicKey publicKey = TestKeys.getCert( TestKeys.EC_secp256r1_CERT_X509_B64 ).getPublicKey();
        String curveName = CertUtils.guessEcCurveName( publicKey );
        assertEquals( "secp256r1", curveName );
    }

    @Test
    @BugId( "SSG-11353" )
    public void testExtractCurveNameFromEcCert_nonSunImpl() throws Exception {
        final PublicKey publicKey = TestKeys.getCert( TestKeys.EC_secp256r1_CERT_X509_B64 ).getPublicKey();
        PublicKey nonSunPublicKey = new PublicKey() {
            @Override
            public String getAlgorithm() {
                return "EC";
            }

            @Override
            public String getFormat() {
                return "X.509";
            }

            @Override
            public byte[] getEncoded() {
                return publicKey.getEncoded();
            }
        };
        String curveName = CertUtils.guessEcCurveName( nonSunPublicKey );
        assertEquals( "secp256r1", curveName );
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_Empty() throws Exception {
        assertNull(CertUtils.convertToX509GeneralName(new NameValuePair()));
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_NullValue() throws Exception {
        assertNull(CertUtils.convertToX509GeneralName(new NameValuePair("DNS Name",null)));
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_NullKey() throws Exception {
        assertNull(CertUtils.convertToX509GeneralName(new NameValuePair(null, "test")));
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_Dns() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("DNS Name", "test.ca.com"));
        assertEquals(X509GeneralName.Type.dNSName, generalName.getType());
        assertEquals("test.ca.com", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_WildcardDns() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("DNS Name","*.ca.com"));
        assertEquals(X509GeneralName.Type.dNSName, generalName.getType());
        assertEquals("*.ca.com", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_Ip() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("IP Address", "111.222.33.44"));
        assertEquals(X509GeneralName.Type.iPAddress, generalName.getType());
        assertEquals("111.222.33.44", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_Dn() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("Directory Name", "CN=test,OU=people"));
        assertEquals(X509GeneralName.Type.directoryName, generalName.getType());
        assertEquals("CN=test,OU=people", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_Email() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("Email", "test@ca.com"));
        assertEquals(X509GeneralName.Type.rfc822Name, generalName.getType());
        assertEquals("test@ca.com", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_Http() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "http://test.ca.com?test=test"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("http://test.ca.com?test=test", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_HttpSvn() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "http+svn://test.ca.com/svn/root"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("http+svn://test.ca.com/svn/root", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_UriCustomScheme1() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "custom-scheme:test.ca.com/custom"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("custom-scheme:test.ca.com/custom", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_UriCustomScheme2() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "custom.scheme://test.ca.com/custom"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("custom.scheme://test.ca.com/custom", generalName.getStringVal());
    }

    @Test
    public void testConvertToX509GeneralNameFromNameValuePair_UriCustomScheme3() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "s://test.ca.com/custom"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("s://test.ca.com/custom", generalName.getStringVal());
    }

    @BugId("DE347819")
    @Test
    public void testConvertToX509GeneralName_InvalidIPv4Addres() throws Exception {
        int errorCount = 0;
        try {
            CertUtils.convertToX509GeneralName(new NameValuePair("IP Address", "111.222.333.44"));
        } catch (IllegalArgumentException e) {
            errorCount++;
        }
        try {
            CertUtils.convertToX509GeneralName(new NameValuePair("IP Address", "1111.555.3333.44"));
        } catch (IllegalArgumentException e) {
            errorCount++;
        }
        try {
            CertUtils.convertToX509GeneralName(new NameValuePair("IP Address", "0000.0000.0000.0000"));
        } catch (IllegalArgumentException e) {
            errorCount++;
        }
        assertEquals(3, errorCount);
    }

    @Test
    @BugId("DE346973")
    public void testConvertToX509GeneralName_UpperCaseURI() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "http://appserver:6394/wa/r/myApp"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("http://appserver:6394/wa/r/myApp", generalName.getStringVal());
        generalName = CertUtils.convertToX509GeneralName(new NameValuePair("URI", "urn:ISSN:1535-3613"));
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalName.getType());
        assertEquals("urn:ISSN:1535-3613", generalName.getStringVal());
    }

    @Test(expected = IllegalArgumentException.class)
    @BugId("DE348135")
    public void testConvertToX509GeneralName_InvalidUriSchemeWithDot() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("URI", ".scheme:appserver:6394/wa/r/myApp"));
    }

    @Test(expected = IllegalArgumentException.class)
    @BugId("DE348135")
    public void testConvertToX509GeneralName_InvalidUriSchemeWithPlus() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("URI", "+scheme:appserver:6394/wa/r/myApp"));
    }

    @Test(expected = IllegalArgumentException.class)
    @BugId("DE348135")
    public void testConvertToX509GeneralName_InvalidUriSchemeWithNumber() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("URI", "9scheme:appserver:6394/wa/r/myApp"));
    }

    @Test(expected = IllegalArgumentException.class)
    @BugId("DE348135")
    public void testConvertToX509GeneralName_InvalidUriSchemeWithDash() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("URI", "-scheme:appserver:6394/wa/r/myApp"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertToX509GeneralName_InvalidDnsNameFormat() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("DNS Name","bla$"));
    }

    @Test(expected = UnsupportedX509GeneralNameException.class)
    public void testConvertToX509GeneralName_UnspecifiedType() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("",""));
    }

    @Test(expected = UnsupportedX509GeneralNameException.class)
    public void testConvertToX509GeneralName_UnsupportedType1() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("blah", "blah"));
    }

    @Test(expected = UnsupportedX509GeneralNameException.class)
    public void testConvertToX509GeneralName_UnsupportedType2() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("Other Name", "123"));
    }

    @Test
    public void testConvertFromX509GeneralNameToNameValuePair() throws Exception {
        NameValuePair actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.dNSName, "test.ca.com"));
        assertEquals("DNS Name", actual.left);
        assertEquals("test.ca.com", actual.right);
        actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.uniformResourceIdentifier, "https://test.ca.com"));
        assertEquals("URI", actual.left);
        assertEquals("https://test.ca.com", actual.right);
        actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.directoryName, "CN=test,OU=people"));
        assertEquals("Directory Name", actual.left);
        assertEquals("CN=test,OU=people", actual.right);
        actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.rfc822Name, "test@ca.com"));
        assertEquals("Email", actual.left);
        assertEquals("test@ca.com", actual.right);
        actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.iPAddress, new byte[] {4,4,111,-122,33,44}));
        assertEquals("IP Address", actual.left);
        assertEquals("111.134.33.44", actual.right);
        actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.iPAddress, "111.134.33.44"));
        assertEquals("111.134.33.44", actual.right);
    }

    @Test
    public void testConvertFromX509GeneralNameToNameValuePair_Other() throws Exception {
        byte[] expectedBytes = new byte[] {0,127,126,3};
        NameValuePair actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.otherName, expectedBytes));
        assertEquals("Other Name", actual.left);
        assertArrayEquals(expectedBytes, Base64.getDecoder().decode(actual.right));
    }

    @Test
    public void testConvertFromX509GeneralNameToNameValuePair_EdiParty() throws Exception {
        byte[] expectedBytes = new byte[] {5,1,1};
        NameValuePair actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.ediPartyName, expectedBytes));
        assertEquals("EDI Party Name", actual.left);
        assertArrayEquals(expectedBytes, Base64.getDecoder().decode(actual.right));
    }

    @Test
    public void testConvertFromX509GeneralNameToNameValuePair_X400Address() throws Exception {
        byte[] expectedBytes = new byte[] {3,4,1,2,3,4};
        NameValuePair actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.x400Address, expectedBytes));
        assertEquals("X400 Address", actual.left);
        assertArrayEquals(expectedBytes, Base64.getDecoder().decode(actual.right));
    }

    @Test
    public void testConvertFromX509GeneralNameToNameValuePair_RegisteredId() throws Exception {
        String expected = "1.2.3.4.5";
        NameValuePair actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.registeredID, expected));
        assertEquals("Registered ID", actual.left);
        assertEquals(expected, actual.right);
    }

    @Test
    public void testConvertFromX509GeneralNameToNameValuePair_ipv6() throws Exception {
        InetAddress a = InetAddress.getByName("2001:0DB8:AC10:FE01:0000:0000:0000:0000");
        byte[] bytes = a.getAddress();
        NameValuePair actual = CertUtils.convertFromX509GeneralName(new X509GeneralName(X509GeneralName.Type.iPAddress, ArrayUtils.concat(new byte[]{4,16}, bytes)));
        assertEquals("2001:db8:ac10:fe01:0:0:0:0", actual.right);
    }

    @Test
    public void testExtractX509GeneralNamesFromList() throws Exception {
        String[] sans = {"DNS Name:test.ca.com", "Email:test@ca.com", "IP Address:111.222.33.44", "URI:http://test.ca.com?test=test", "Directory Name:CN=test,OU=people"};
        List<X509GeneralName> generalNames = CertUtils.extractX509GeneralNamesFromList(Arrays.asList(sans));
        assertEquals(5,generalNames.size());
        assertEquals(X509GeneralName.Type.dNSName, generalNames.get(0).getType());
        assertEquals("test.ca.com", generalNames.get(0).getStringVal());
        assertEquals(X509GeneralName.Type.rfc822Name, generalNames.get(1).getType());
        assertEquals("test@ca.com", generalNames.get(1).getStringVal());
        assertEquals(X509GeneralName.Type.iPAddress, generalNames.get(2).getType());
        assertEquals("111.222.33.44", generalNames.get(2).getStringVal());
        assertEquals(X509GeneralName.Type.uniformResourceIdentifier, generalNames.get(3).getType());
        assertEquals("http://test.ca.com?test=test", generalNames.get(3).getStringVal());
        assertEquals(X509GeneralName.Type.directoryName, generalNames.get(4).getType());
        assertEquals("CN=test,OU=people", generalNames.get(4).getStringVal());
    }

    @Test
    public void testExtractX509GeneralNamesFromList_isNull() throws Exception {
        String[] sans = {"test.ca.com","IP Address"};
        assertNull(CertUtils.extractX509GeneralNamesFromList(Arrays.asList(sans)));
        assertNull(CertUtils.extractX509GeneralNamesFromList(Collections.EMPTY_LIST));
        assertNull(CertUtils.extractX509GeneralNamesFromList(null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractX509GeneralNamesFromList_wrongStringFormat() throws Exception {
        String[] sans = {"DNS Name:test.ca.com:test1.ca.com"};
        assertNull(CertUtils.extractX509GeneralNamesFromList(Arrays.asList(sans)));
    }

    @Test
    public void testX509GeneralNameGetUserFriendlyNames() throws Exception {
        String[] userFriendlyNames = Arrays.stream(X509GeneralName.Type.values()).map(X509GeneralName.Type::getUserFriendlyName).collect(Collectors.toList()).toArray(new String[0]);
        assertEquals(9, userFriendlyNames.length);
    }

    @Test
    public void testSupportedSubjectAlternativeNameTypes() {
        Set supportedTypes = Arrays.stream(X509GeneralName.Type.values()).filter(CertUtils::isSubjectAlternativeNameTypeSupported).collect(Collectors.toSet());
        assertEquals(5, supportedTypes.size());
        assertTrue(supportedTypes.contains(X509GeneralName.Type.iPAddress));
        assertTrue(supportedTypes.contains(X509GeneralName.Type.dNSName));
        assertTrue(supportedTypes.contains(X509GeneralName.Type.directoryName));
        assertTrue(supportedTypes.contains(X509GeneralName.Type.rfc822Name));
        assertTrue(supportedTypes.contains(X509GeneralName.Type.uniformResourceIdentifier));
    }

    @BugId("DE347819")
    @Test
    public void testConvertToX509GeneralNameIpv6Addresses() throws Exception {
        X509GeneralName generalName = CertUtils.convertToX509GeneralName(new NameValuePair("IP Address","2001:0db8:85a3:08d3:1319:8a2e:0370:7348"));
        assertEquals(X509GeneralName.Type.iPAddress, generalName.getType());
        assertEquals("2001:0db8:85a3:08d3:1319:8a2e:0370:7348", generalName.getStringVal());
        generalName = CertUtils.convertToX509GeneralName(new NameValuePair("IP Address","abf3:FF2:0::00:23"));
        assertEquals(X509GeneralName.Type.iPAddress, generalName.getType());
        assertEquals("abf3:FF2:0::00:23", generalName.getStringVal());
        generalName = CertUtils.convertToX509GeneralName(new NameValuePair("IP Address","2001:db8::1"));
        assertEquals(X509GeneralName.Type.iPAddress, generalName.getType());
        assertEquals("2001:db8::1", generalName.getStringVal());
    }

    @BugId("DE347819")
    @Test(expected = IllegalArgumentException.class)
    public void testConvertToX509GeneralNameIpv6Address_InvalidFormat1() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("IP Address","2001:db8:::1"));
    }

    @BugId("DE347819")
    @Test(expected = IllegalArgumentException.class)
    public void testConvertToX509GeneralNameIpv6Address_InvalidFormat2() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("IP Address","cafe:babe:0000::4343:1.2.3.4"));
    }

    @BugId("DE347819")
    @Test(expected = IllegalArgumentException.class)
    public void testConvertToX509GeneralNameIpv6Address_InvalidFormat3() throws Exception {
        CertUtils.convertToX509GeneralName(new NameValuePair("IP Address","2001:1111"));
    }

    @BugId("DE356626")
    @Test
    public void testGetThumbprintSHA1_beforeFix_base64() throws Exception {
        X509Certificate testCert = TestKeys.getCert( TestKeys.DSA_1024_CERT_X509_B64 );
        String thumbprintBase64 = "oMQ3f/0E/byjDFXMYQ8aGaRFoqA=";
        assertEquals(thumbprintBase64, CertUtils.getThumbprintSHA1(testCert));//Existing method
    }

    @BugId("DE356626")
    @Test
    public void testGetThumbprintSHA1_afterFix_base64url() throws Exception {
        X509Certificate testCert = TestKeys.getCert( TestKeys.DSA_1024_CERT_X509_B64 );
        String thumbprintBase64url = "oMQ3f_0E_byjDFXMYQ8aGaRFoqA";
        assertEquals(thumbprintBase64url, CertUtils.getThumbprintSHA1(testCert, CertUtils.FINGERPRINT_BASE64URL));//New method
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

    private static final String BUG_13337_WITH_SAN =
            "MIIElTCCA32gAwIBAgIBAjANBgkqhkiG9w0BAQUFADCBpjELMAkGA1UEBhMCQ0ExGTAXBgNVBAgM\n" +
            "EEJyaXRpc2ggQ29sdW1iaWExEjAQBgNVBAcMCVZhbmNvdXZlcjEdMBsGA1UECgwUTGF5ZXIgNyBU\n" +
            "ZWNobm9sb2dpZXMxEDAOBgNVBAsMB1Rlc3RpbmcxFjAUBgNVBAMMDVN0b3lhbiBQZXRyb3YxHzAd\n" +
            "BgkqhkiG9w0BCQEWEHN0b3lhbkBwZXRyb3YuY2EwHhcNMTIxMDIwMDAxMzA4WhcNMTMxMDIwMDAx\n" +
            "MzA4WjCBkjELMAkGA1UEBhMCQ0ExGTAXBgNVBAgMEEJyaXRpc2ggQ29sdW1iaWExHTAbBgNVBAoM\n" +
            "FExheWVyIDcgVGVjaG5vbG9naWVzMRAwDgYDVQQLDAdUZXN0aW5nMRYwFAYDVQQDDA1TdG95YW4g\n" +
            "UGV0cm92MR8wHQYJKoZIhvcNAQkBFhBzdG95YW5AcGV0cm92LmNhMIIBIjANBgkqhkiG9w0BAQEF\n" +
            "AAOCAQ8AMIIBCgKCAQEAxujDgUEBGI14uCLsNESyU/0j2vBQ2bgohvwYYwxqsGCBvwJJ+IhO/k9I\n" +
            "b+RcVRhPoHWmBlZuNUDMuMOgRaO8Y28ETeGik/hLKXxe3VpizpMTEqXtqheYhPWN6JNL0ttgCbjZ\n" +
            "NGdiOLMdGaXeGsJgiMiRwGrxFJmXzFWFBLpTXBu2FJ326qGczBgEr05tGOUuOK9lVdtnbGH8lmYH\n" +
            "uOgK7hyLLCUNNULctI7d8ip2azVlSyoI31ifXtxkLX1E10dJMvdTWwEcx9+Q1pLgOSK1lcED5RU0\n" +
            "zffCG5WgRrGNwjxqEkKFNsttMAA0zJygi4LcNMojri19qHWewi3y7UAQZwIDAQABo4HfMIHcMAkG\n" +
            "A1UdEwQCMAAwCwYDVR0PBAQDAgXgMIHBBgNVHREEgbkwgbaBF3N0b3lhbi5wZXRyb3ZAZ21haWwu\n" +
            "Y29tgglwZXRyb3YuY2GCCXlhaG9vLmNvbYYRaHR0cDovL3BldHJvdi5jYS+HBMCoAAGHBMCoAAKk\n" +
            "ZjBkMQswCQYDVQQGEwJCRzETMBEGA1UEChMKRmlzaCBDb3JwLjEQMA4GA1UECxMHUGV0cm92czEN\n" +
            "MAsGA1UEAxMERmlzaDEfMB0GCSqGSIb3DQEJARYQc3RveWFuQHBldHJvdi5jYTANBgkqhkiG9w0B\n" +
            "AQUFAAOCAQEAFBZWh30WCaC4HPgfGpofL7aeJFh+pSul5QQBH1kko77SXDHoKEWVq//xi6BdRL88\n" +
            "DeWnzTt1iPQcB5db9HlwRM/z0orbCZ58wxzAAma/4yUgXNyrXz/DtMWhKujELsfqL6AUpMK7Q2Q3\n" +
            "hofuAxHk/wYnhwN1isPyrhU+2dnrd5bnkiMGWyqUAakkYwyx7xL6I87xAPZA0zjqIL9gt57q0luu\n" +
            "HjUcXjprvF3YHosODKXDo/kUGeCXoZKQ4yL/ascXK5OCHO8F6i3zYxl+IHmR4H8yZKMuFv+KbUL4\n" +
            "xjPxGSqYeiVKi6yvaxOvpwqvhJx0FZSnFcbkzLJuCnP+XKpklQ==";

    private static final String BUG_13422_CERT_WITH_BYTE_ARRAY_SAN =
            "MIIGBDCCBOygAwIBAgIKSHorMAAAAAAABjANBgkqhkiG9w0BAQUFADBUMRUwEwYKCZImiZPyLGQB\n" +
            "GRYFbG9jYWwxFzAVBgoJkiaJk/IsZAEZFgdzZWF0dGxlMSIwIAYDVQQDExlyb290LnRhY29tYS5z\n" +
            "ZWF0dGxlLmxvY2FsMB4XDTEyMDYxMTE4MzkyNVoXDTEzMDYxMTE4MzkyNVowHzEdMBsGA1UEAxMU\n" +
            "dGFjb21hLnNlYXR0bGUubG9jYWwwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCp/LYO\n" +
            "QpQ2sjkvYBZ8OURjY/x529IRq2Ew2kYL5bkWoG0oJi3x7JEAHU0VLDLNUBn2Kprb98uFkvvsxgzf\n" +
            "NnS7KnToCo9OLLvxdg0NwgpuswQlFS2c7vw7gTo6knx8ydmPkTTqxp8svjT/raEcbfKjDdV3Ab/y\n" +
            "MYQ65u+Rb6yd9GL7obSDd9syGnCqzbPK+fNiiVx3hXhtMgbv4D8EfIBSg07WpqVpoWTmCUozXWH9\n" +
            "fJbtISWv+/kYnfSedgDN9pmpWgYKeyZSRvH6GjC/FdfwQ8s51gTA8oXmTQHNPuAYalqftyG0PjAm\n" +
            "dFLAySU41Jo0ckAWeTzHjXnr6e5nmBkBAgMBAAGjggMLMIIDBzAvBgkrBgEEAYI3FAIEIh4gAEQA\n" +
            "bwBtAGEAaQBuAEMAbwBuAHQAcgBvAGwAbABlAHIwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUF\n" +
            "BwMBMA4GA1UdDwEB/wQEAwIFoDB4BgkqhkiG9w0BCQ8EazBpMA4GCCqGSIb3DQMCAgIAgDAOBggq\n" +
            "hkiG9w0DBAICAIAwCwYJYIZIAWUDBAEqMAsGCWCGSAFlAwQBLTALBglghkgBZQMEAQIwCwYJYIZI\n" +
            "AWUDBAEFMAcGBSsOAwIHMAoGCCqGSIb3DQMHMB0GA1UdDgQWBBTQObtNFLopBdbANO9s7rFmcL26\n" +
            "WzAfBgNVHSMEGDAWgBQW7BkEtU7osTDoxdYESQrkCYGq1zCB2AYDVR0fBIHQMIHNMIHKoIHHoIHE\n" +
            "hoHBbGRhcDovLy9DTj1yb290LnRhY29tYS5zZWF0dGxlLmxvY2FsLENOPXRhY29tYSxDTj1DRFAs\n" +
            "Q049UHVibGljJTIwS2V5JTIwU2VydmljZXMsQ049U2VydmljZXMsQ049Q29uZmlndXJhdGlvbixE\n" +
            "Qz1zZWF0dGxlLERDPWxvY2FsP2NlcnRpZmljYXRlUmV2b2NhdGlvbkxpc3Q/YmFzZT9vYmplY3RD\n" +
            "bGFzcz1jUkxEaXN0cmlidXRpb25Qb2ludDCBzQYIKwYBBQUHAQEEgcAwgb0wgboGCCsGAQUFBzAC\n" +
            "hoGtbGRhcDovLy9DTj1yb290LnRhY29tYS5zZWF0dGxlLmxvY2FsLENOPUFJQSxDTj1QdWJsaWMl\n" +
            "MjBLZXklMjBTZXJ2aWNlcyxDTj1TZXJ2aWNlcyxDTj1Db25maWd1cmF0aW9uLERDPXNlYXR0bGUs\n" +
            "REM9bG9jYWw/Y0FDZXJ0aWZpY2F0ZT9iYXNlP29iamVjdENsYXNzPWNlcnRpZmljYXRpb25BdXRo\n" +
            "b3JpdHkwQAYDVR0RBDkwN6AfBgkrBgEEAYI3GQGgEgQQ6BOhb85ttECBPdrCize8G4IUdGFjb21h\n" +
            "LnNlYXR0bGUubG9jYWwwDQYJKoZIhvcNAQEFBQADggEBADd9Uv5QsPZ9tVjE1ADZf9UEhQ6SaEkE\n" +
            "vEy/iEhNGn23ORDwLr+3bEb9SW7dWNimuybe5Td5V/HTnNUvReQpVWlRZ/CbF1cw98mxZENH6Pw0\n" +
            "PIq2H2m753C7NG6EtY7oQUlRaiebElIzRUkNUM+HIX1yVD+iuk621jvoU+JfvggiuISingm1Kh8p\n" +
            "QAYuoPtFToXUeHxAyGtMbzWNMPp3+Ty/LWjXvAVVmArXbKMUgpUWh1Hxs8Hl3w750xf9nJzbPwpi\n" +
            "yxVMOlbs8IyLyLOP5K/WcM216/C+C7f6gkh2uM4pVVFknhofXt/5RWigam6gt8LdVrcGrkdjap9d\n" +
            "NKX+L5E=";
}
