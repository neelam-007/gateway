package com.l7tech.server;

import com.l7tech.common.security.TrustedCertAdmin;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.net.UnknownHostException;
import java.security.cert.X509Certificate;

/**
 * This test is pretty fragile because it relies on http://mail.l7tech.com/ and https://mail.l7tech.com/
 * being up and using a self-signed cert with an incorrect hostname.
 *
 * @author alex
 * @version $Revision$
 */
public class TrustedCertAdminTest extends TestCase {
    /**
     * test <code>TrustedCertAdminTest</code> constructor
     */
    public TrustedCertAdminTest( String name ) {
        super( name );
    }

    /**
     * create the <code>TestSuite</code> for the TrustedCertAdminTest <code>TestCase</code>
     */
    public static Test suite() {
        TestSuite suite = new TestSuite( TrustedCertAdminTest.class );
        return suite;
    }

    public void setUp() throws Exception {
        certAdmin = new TrustedCertAdminImpl(null,null);
    }

    public void tearDown() throws Exception {
        certAdmin = null;
    }

    public void testRetrieveCertIgnoreHostname() throws Exception {
        X509Certificate[] chain = certAdmin.retrieveCertFromUrl("https://mail.l7tech.com/", true);
        assertNotNull(chain);
        for ( int i = 0; i < chain.length; i++ ) {
            X509Certificate cert = chain[i];
            System.out.println("Found cert with dn " + cert.getSubjectDN().getName() );
        }
    }

    public void testRetrieveCertWrongHostname() throws Exception {
        try {
            certAdmin.retrieveCertFromUrl("https://mail.l7tech.com/", false);
            fail("Should have thrown");
        } catch ( IOException e ) {
            // OK
        }
    }

    public void testRetrieveCertHttpUrl() throws Exception {
        try {
            certAdmin.retrieveCertFromUrl("http://mail.l7tech.com:8080/");
            fail("Should have thrown");
        } catch ( IllegalArgumentException e ) {
            // OK
        }
    }

    public void testRetrieveCertWrongPort() throws Exception {
        try {
            certAdmin.retrieveCertFromUrl("https://mail.l7tech.com:80/", true);
            fail("Should have thrown");
        } catch ( SSLException e ) {
            // OK
        }
    }

    public void testRetrieveCertUnknownHost() throws Exception {
        try {
            certAdmin.retrieveCertFromUrl("https://fiveearthmoneysperyear.l7tech.com:8443/");
            fail("Should have thrown");
        } catch ( UnknownHostException e ) {
            // OK
        }
    }

    /**
     * Test <code>TrustedCertAdminTest</code> main.
     */
    public static void main( String[] args ) throws
                                             Throwable {
        junit.textui.TestRunner.run( suite() );
    }

    private TrustedCertAdmin certAdmin;
}