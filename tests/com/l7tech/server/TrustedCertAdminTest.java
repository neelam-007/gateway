package com.l7tech.server;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.admin.AdminContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This test is pretty fragile and probably shouldn't be in the nightly run because:
 * <ul>
 * <li>Some tests rely on http://mail.l7tech.com/ and https://mail.l7tech.com/
 * being up and using a self-signed cert with an incorrect hostname
 * <li>Other tests rely on the last SSG used by the Console on the machine being up and running
 * </ul>
 *
 * @author alex
 * @version $Revision$
 */
public class TrustedCertAdminTest extends TestCase {
    private static SsgAdminSession ssgAdminSession;
    private static AdminContext adminContext;

    /**
     * test <code>TrustedCertAdminTest</code> constructor
     */
    public TrustedCertAdminTest( String name ) throws Exception {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the TrustedCertAdminTest <code>TestCase</code>
     */
    public static Test suite() {
        try {
            ssgAdminSession = new SsgAdminSession(new String[] {"quark:2124"});
            adminContext = ssgAdminSession.getAdminContext();
            return new TestSuite( TrustedCertAdminTest.class );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void testRetrieveCertIgnoreHostname() throws Exception {
        X509Certificate[] chain = adminContext.getTrustedCertAdmin().retrieveCertFromUrl("https://mail.l7tech.com/", true);
        assertNotNull(chain);
        for ( int i = 0; i < chain.length; i++ ) {
            X509Certificate cert = chain[i];
            System.out.println("Found cert with dn " + cert.getSubjectDN().getName() );
        }
    }

    public void testRetrieveCertWrongHostname() throws Exception {
        try {
           adminContext.getTrustedCertAdmin().retrieveCertFromUrl("https://mail.l7tech.com/", false);
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    public void testRetrieveCertHttpUrl() throws Exception {
        try {
                    adminContext.getTrustedCertAdmin().retrieveCertFromUrl("http://mail.l7tech.com:8080/");
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    public void testRetrieveCertWrongPort() throws Exception {
        try {
                   adminContext.getTrustedCertAdmin().retrieveCertFromUrl("https://mail.l7tech.com:80/", true);
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    public void testRetrieveCertUnknownHost() throws Exception {
        try {
                    adminContext.getTrustedCertAdmin().retrieveCertFromUrl("https://fiveearthmoneysperyear.l7tech.com:8443/");
            fail("Should have thrown");
        } catch (Exception e) {
            // OK
        }
    }

    public void testUpdateCert() throws Exception {
        final TrustedCert tc = getTrustedCert();
        final Long oid =
                new Long(adminContext.getTrustedCertAdmin().saveCert(tc));
        System.out.println("Saved " + oid );

        tc.setSubjectDn("The other one");
        tc.setTrustedForSsl(true);
        tc.setOid(oid.longValue());

        new Long(adminContext.getTrustedCertAdmin().saveCert(tc));
        System.out.println("Updated " + oid );

        TrustedCert tc2 = adminContext.getTrustedCertAdmin().findCertByPrimaryKey(oid.longValue());

        assertEquals(tc,tc2);

                adminContext.getTrustedCertAdmin().deleteCert(oid.longValue());
        System.out.println("Deleted " + oid );
    }

    public void testFindAllCerts() throws Exception {
        final TrustedCert tc = getTrustedCert();

                Set oids = new HashSet();
                oids.add(new Long(adminContext.getTrustedCertAdmin().saveCert(tc)));
                oids.add(new Long(adminContext.getTrustedCertAdmin().saveCert(tc)));

        System.out.println("Saved " + oids);

        List all = adminContext.getTrustedCertAdmin().findAllCerts();

        Set foundOids = new HashSet();
        for ( Iterator iterator = all.iterator(); iterator.hasNext(); ) {
            TrustedCert tc2 = (TrustedCert) iterator.next();
            foundOids.add(new Long(tc2.getOid()));
        }

        System.out.println("Found " + foundOids);

        assertTrue(foundOids.containsAll(oids));

        final Set deletedOids = new HashSet();
                for ( Iterator i = oids.iterator(); i.hasNext(); ) {
                    Long oid = (Long) i.next();
                    adminContext.getTrustedCertAdmin().deleteCert(oid.longValue());
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

    public void testSaveCert() throws Exception {
        final TrustedCert tc = getTrustedCert();
        tc.setTrustedForSigningClientCerts(true);
        tc.setTrustedForSigningSamlTokens(true);
        tc.setTrustedForSigningServerCerts(true);
        tc.setTrustedForSsl(true);
        X509Certificate cert = tc.getCertificate();
        System.out.println("Saving cert with dn '" + cert.getSubjectDN().getName() + "' and usage '" + tc.getUsageDescription() + "'" );

        final Long oid = new Long(adminContext.getTrustedCertAdmin().saveCert(tc));
        System.out.println("Saved cert " + oid);


        TrustedCert tc2 = adminContext.getTrustedCertAdmin().findCertByPrimaryKey(oid.longValue());
        assertNotNull(tc2);
        System.out.println("Loaded TrustedCert " + tc2);
        System.out.println("DN is " + tc2.getCertificate().getSubjectDN().getName() );

        assertEquals(tc2.getSubjectDn(), tc.getSubjectDn());
        assertEquals(tc2.getCertificate(), tc.getCertificate());

        adminContext.getTrustedCertAdmin().deleteCert(oid.longValue());
        System.out.println("Deleted TrustedCert " + oid);

        Object gone = adminContext.getTrustedCertAdmin().findCertByPrimaryKey(oid.longValue());
        assertNull(gone);
    }

    private X509Certificate getCert() throws CertificateException, IOException {
        byte[] certBytes = HexUtils.decodeBase64(CERT_BASE64);
        return CertUtils.decodeCert(certBytes);
    }

    /**
     * Test <code>TrustedCertAdminTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
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