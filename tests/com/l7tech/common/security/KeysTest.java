package com.l7tech.common.security;

import com.l7tech.common.util.KeystoreUtils;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

/**
 * The <code>Keys</code> class test.
 */
public class KeysTest extends TestCase {

    public KeysTest(String name) throws Exception {
        super(name);
    }

    /**
     * create the <code>TestSuite</code> containing the KeysTest
     */
    public static Test suite() {
        return new TestSuite(KeysTest.class);
    }

    /**
     * @throws Exception
     */
    public void testCreateKeysAndCertificate() throws Exception {
        final String subject = "CN=fred";
        Keys kc = new Keys(subject);
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

        X509Certificate cert = kc.generateSelfSignedCertificate(notBefore, notAfter);
        assertTrue("Wrong subject", cert.getSubjectDN().getName().equals(subject));
        assertTrue("Wrong issuer", cert.getIssuerDN().getName().equals(subject));

        assertTrue("Wrong not before", cert.getNotBefore().equals(notBefore));
        assertTrue("Wrong not after", cert.getNotAfter().equals(notAfter));
    }


    /**
     * @throws Exception
     */
    public void testWriteRead() throws Exception {
        final String subject = "CN=fred";
        Keys kc = new Keys(subject);
        File f = File.createTempFile("testKeys", null);
        final char[] password = "password".toCharArray();
        final String alias = "testSigner";
        kc.write(f.getAbsolutePath(), alias, password);
        KeyStore ks = KeyStore.getInstance(Keys.KEYSTORE_TYPE);
        FileInputStream fi = new FileInputStream(f);
        ks.load(fi, password);
        fi.close();
        Key key = ks.getKey(alias, password);
        assertTrue(key !=null);
        Certificate cert = ks.getCertificate(alias);
        assertTrue(cert != null);
    }

    public void testSsgKestoreProperties() throws Exception {
        Properties props = Keys.createTestSsgKeystoreProperties();
        KeystoreUtils ku = KeystoreUtils.getInstance();
        assertTrue(Keys.KEYSTORE_TYPE.equals(ku.getKeyStoreType()));
        assertTrue(props.getProperty(KeystoreUtils.ROOT_STOREPASSWD).equals(ku.getRootKeystorePasswd()));
    }

    /**
     * Test <code>KeysTest</code> main.
     */
    public static void main(String[] args) throws Throwable {
        junit.textui.TestRunner.run(suite());
    }
}
