package com.l7tech.common.security;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;

/**
 * The <code>Keys</code> class test.
 */
public class KeysTest extends TestCase {

    public KeysTest(String name) throws Exception {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> containing the KeysTest
     * the policy package.
     * <p/>
     * Add new tests at the bottom of the list.
     */
    public static Test suite() {
        return new TestSuite(KeysTest.class);

    }

    /**
     *
     * @throws Exception
     */
    public void testCreateKeysAndCertificate() throws Exception {
        Keys kc = new Keys();
        Date notBefore = new Date();
        Calendar cal = Calendar.getInstance();
        // clear the time part
        cal.setTime(notBefore);
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        notBefore = cal.getTime();

        cal.roll(Calendar.YEAR, 1);
        Date notAfter = cal.getTime();
        String subject = "CN=fred";

        X509Certificate cert = kc.generateSelfSignedCertificate(notBefore, notAfter, subject);
        assertTrue("Wrong subject", cert.getSubjectDN().getName().equals(subject));
        assertTrue("Wrong issuer", cert.getIssuerDN().getName().equals(subject));

        assertTrue("Wrong not before", cert.getNotBefore().equals(notBefore));
        assertTrue("Wrong not after", cert.getNotAfter().equals(notAfter));
    }

    /**
     * Test <code>KeysTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
