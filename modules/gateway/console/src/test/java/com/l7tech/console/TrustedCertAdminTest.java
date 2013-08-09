package com.l7tech.console;

import com.l7tech.common.io.CertUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.util.HexUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * This test is pretty fragile and probably shouldn't be in the nightly run because:
 * <ul>
 * <li>Some tests rely on http://mail.l7tech.com/ and https://mail.l7tech.com/
 * being up and using a self-signed cert with an incorrect hostname
 * <li>Other tests rely on the last SSG used by the Console on the machine being up and running
 * </ul>
 */
@Ignore("Developer test")
public class TrustedCertAdminTest {
    private static Registry registry;

    @BeforeClass
    public static void init() {
        try {
            new SsgAdminSession();
            registry = Registry.getDefault();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testRetrieveCertIgnoreHostname() throws Exception {
        X509Certificate[] chain = registry.getTrustedCertManager().retrieveCertFromUrl("https://mail.l7tech.com/", true);
        assertNotNull(chain);
        for (X509Certificate cert : chain) {
            System.out.println("Found cert with dn " + cert.getSubjectDN().getName());
        }
    }

    @Test
    public void testRetrieveCertWrongHostname() throws Exception {
        try {
            registry.getTrustedCertManager().retrieveCertFromUrl("https://mail.l7tech.com/", false);
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    @Test
    public void testRetrieveCertHttpUrl() throws Exception {
        try {
            registry.getTrustedCertManager().retrieveCertFromUrl("http://mail.l7tech.com:8080/");
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    @Test
    public void testRetrieveCertWrongPort() throws Exception {
        try {
            registry.getTrustedCertManager().retrieveCertFromUrl("https://mail.l7tech.com:80/", true);
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    @Test
    public void testRetrieveCertUnknownHost() throws Exception {
        try {
            registry.getTrustedCertManager().retrieveCertFromUrl("https://fiveearthmoneysperyear.l7tech.com:8443/");
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    @Test
    public void testUpdateCert() throws Exception {
        final TrustedCert tc = getTrustedCert();
        final Goid oid =
                registry.getTrustedCertManager().saveCert( tc );
        System.out.println("Saved " + oid);

        tc.setSubjectDn("The other one");
        tc.setTrustedForSsl(true);
        tc.setGoid(oid);

        Goid saved = registry.getTrustedCertManager().saveCert(tc);
        System.out.println("Updated " + oid + " " + saved);

        TrustedCert tc2 = registry.getTrustedCertManager().findCertByPrimaryKey( oid );

        assertEquals(tc, tc2);

        registry.getTrustedCertManager().deleteCert( oid );
        System.out.println("Deleted " + oid);
    }

    @Test
    public void testFindAllCerts() throws Exception {
        final TrustedCert tc = getTrustedCert();

        Set<Goid> oids = new HashSet<Goid>();
        oids.add( registry.getTrustedCertManager().saveCert( tc ) );
        oids.add( registry.getTrustedCertManager().saveCert( tc ) );

        System.out.println("Saved " + oids);

        List<TrustedCert> all = registry.getTrustedCertManager().findAllCerts();

        Set<Goid> foundOids = new HashSet<Goid>();
        for (TrustedCert tc2 : all) {
            foundOids.add( tc2.getGoid() );
        }

        System.out.println("Found " + foundOids);

        assertTrue(foundOids.containsAll(oids));

        final Set<Goid> deletedOids = new HashSet<Goid>();
        for (Goid oid : oids) {
            registry.getTrustedCertManager().deleteCert( oid );
            deletedOids.add(oid);
        }
        System.out.println("Deleted " + deletedOids);
    }

    private TrustedCert getTrustedCert() throws CertificateException, IOException {
        X509Certificate cert = getCert();
        TrustedCert tc = new TrustedCert();
        tc.setCertificate(cert);
        tc.setName("Oh no! It's a cert!");
        tc.setSubjectDn(cert.getSubjectDN().getName());
        return tc;
    }

    @Test
    public void testSaveCert() throws Exception {
        final TrustedCert tc = getTrustedCert();
        tc.setTrustedForSigningClientCerts(true);
        tc.setTrustedAsSamlIssuer(true);
        tc.setTrustedForSigningServerCerts(true);
        tc.setTrustedForSsl(true);
        X509Certificate cert = tc.getCertificate();
        System.out.println("Saving cert with dn '" + cert.getSubjectDN().getName() + "' and usage '" + tc.getUsageDescription() + "'");

        final Goid oid = registry.getTrustedCertManager().saveCert( tc );
        System.out.println("Saved cert " + oid);


        TrustedCert tc2 = registry.getTrustedCertManager().findCertByPrimaryKey( oid );
        assertNotNull(tc2);
        System.out.println("Loaded TrustedCert " + tc2);
        System.out.println("DN is " + tc2.getCertificate().getSubjectDN().getName());

        assertEquals(tc2.getSubjectDn(), tc.getSubjectDn());
        assertEquals(tc2.getCertificate(), tc.getCertificate());

        registry.getTrustedCertManager().deleteCert( oid );
        System.out.println("Deleted TrustedCert " + oid);

        Object gone = registry.getTrustedCertManager().findCertByPrimaryKey( oid );
        assertNull(gone);
    }

    private X509Certificate getCert() throws CertificateException, IOException {
        byte[] certBytes = HexUtils.decodeBase64(CERT_BASE64);
        return CertUtils.decodeCert(certBytes);
    }

    private static final String CERT_BASE64 =
      "MIICZjCCAc8CBD2+61QwDQYJKoZIhvcNAQEEBQAwejELMAkGA1UEBhMCVVMxEzARBgNV\n" +
      "BAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJNMR8w\n" +
      "HQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNtaXRo\n" +
      "MB4XDTAyMTAyOTIwMTEwMFoXDTA3MTAwMzIwMTEwMFowejELMAkGA1UEBhMCVVMxEzAR\n" +
      "BgNVBAgTCkNhbGlmb3JuaWExEjAQBgNVBAcTCUN1cGVydGlubzEMMAoGA1UEChMDSUJN\n" +
      "MR8wHQYDVQQLExZKYXZhIFRlY2hub2xvZ3kgQ2VudGVyMRMwEQYDVQQDEwpKb2huIFNt\n" +
      "aXRoMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCtitis5FONvv236Xw1CKOvKAOQ\n" +
      "0NYlvRd/FKwuf+T1XCFadHMrtvHhq/+Z/Dlcn2YQYOCp9auS+WkBcL0AUrJJPUbwZIB2\n" +
      "CyBZJjnS7+jdb37RKYQUsNRNlgdIcoZM8bvCZldBSfnat4xDPyQOJB7ExDrMmI9tP0NY\n" +
      "9GN0npfnwwIDAQABMA0GCSqGSIb3DQEBBAUAA4GBAFLNrEP8Y0xwUVIl4XigEiDM6jAd\n" +
      "DJFCI+m8EA07nAsYWmV/Ic8kkqDzXaWyLkIBJQ0gElRlWHYe+W/K/pT9CNEWRFViKbZG\n" +
      "evivBKek7GQXhuEgo+pWalpEtg4nA741+46iKeKpEQILL6OYEj7aHcreIxaQ1WH2v1iM\n" +
      "ig33Q0+S";
}