/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.common.io;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.security.cert.Certificate;
import java.util.logging.Logger;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.Iterator;

/**
 * @author mike
 */
public class CertUtilsTest extends TestCase {
    private static Logger log = Logger.getLogger(CertUtilsTest.class.getName());

    public CertUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(CertUtilsTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

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

        Certificate cert = CertUtils.decodeCert(serverCertB64.getBytes());

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

        cert = CertUtils.decodeCert(clientCertB64.getBytes());


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

        cert = CertUtils.decodeCert(otherCertB64.getBytes());

    }

    public void testDnParser() throws Exception {
        doTestDnParse();
        doTestDnPatterns();
    }

    protected void doTestDnParse() throws Exception {
        Map map = CertUtils.dnToAttributeMap("cn=Mike Lyons, ou=Research, o=Layer 7 Technologies");

        Set entries = map.entrySet();
        for (Iterator i = entries.iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry)i.next();
            String key = (String)entry.getKey();
            List values = (List)entry.getValue();
            for (Iterator j = values.iterator(); j.hasNext();) {
                String value = (String)j.next();
                log.info("    key<" + key +">  value<" + value +">");
            }
        }

    }

    protected void doTestDnPatterns() throws Exception {
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
}
