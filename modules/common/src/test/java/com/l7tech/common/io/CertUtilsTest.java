/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import static org.junit.Assert.*;
import org.junit.*;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author mike
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
                               "O=ACME Inc., OU=Widgets, CN=*"));

        assertFalse(CertUtils.dnMatchesPattern("O=ACME Inc., OU=Widgets, CN=joe",
                                "O=ACME Inc., OU=Widgets, CN=bob"));

        assertTrue("Multi-valued attributes, case and whitespace are insignificant",
                   CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise",
                               "dc=layer7-tech, DC=com, UID=*"));

        assertFalse("Group value wildcards are required",
                    CertUtils.dnMatchesPattern("dc=layer7-tech,dc=com, uid=acruise",
                                "dc=layer7-tech, DC=com, cn=*, UID=*"));
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

}
