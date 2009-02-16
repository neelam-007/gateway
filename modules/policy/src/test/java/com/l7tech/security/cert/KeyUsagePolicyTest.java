package com.l7tech.security.cert;

import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.jce.X509KeyUsage;
import static org.junit.Assert.*;
import org.junit.*;
import org.xml.sax.SAXException;

import java.util.Arrays;

/**
 *
 */
public class KeyUsagePolicyTest {
    private static final int KU_DSIG = X509KeyUsage.digitalSignature;
    private static final int KU_KENC = X509KeyUsage.keyEncipherment;
    private static final int KU_NONREP = X509KeyUsage.nonRepudiation;
    private static final int KU_CRLSIG = X509KeyUsage.cRLSign;
    private static final int KU_CERTSIG = X509KeyUsage.keyCertSign;

    private static final int KU_DSIG_KENC_NONREP = KU_DSIG | KU_KENC | KU_NONREP;
    private static final int KU_CRLSIG_CERTSIG = KU_CRLSIG | KU_CERTSIG;

    @Test
    public void testParseNullXml() {
        try {
            KeyUsagePolicy.fromXml(null);
            fail("expected exception not thrown");
        } catch (NullPointerException e) {
            // Ok
        } catch (SAXException e) {
            // Ok, we'll accept this as well
        }
    }

    @Test
    public void testParseEmptyXml() {
        try {
            KeyUsagePolicy.fromXml("");
            fail("expected exception not thrown");
        } catch (SAXException e) {
            // Ok
        }
    }

    @Test
    public void testNonWellFormedXml() {
        try {
            KeyUsagePolicy.fromXml("<blah><foo></foop></blah>");
            fail("expected exception not thrown");
        } catch (SAXException e) {
            // Ok
        }
    }

    @Test
    public void testBadRootNsUri() {
        try {
            KeyUsagePolicy.fromXml("<keyusagepolicy xmlns=\"" + KeyUsagePolicy.NAMESPACE_URI + "123" + "\"/>");
            fail("expected exception not thrown");
        } catch (SAXException e) {
            // Ok
        }
    }

    @Test
    public void testBadRootElement() {
        try {
            KeyUsagePolicy.fromXml("<blah xmlns=\"" + KeyUsagePolicy.NAMESPACE_URI + "\"/>");
            fail("expected exception not thrown");
        } catch (SAXException e) {
            // Ok
        }
    }

    @Test
    public void testBadActivityName() {
        try {
            KeyUsagePolicy.fromXml("<keyusagepolicy xmlns=\"http://www.layer7tech.com/ws/keyusage\"><permit action=\"bogus\"/></keyusagepolicy>");
            fail("expected exception not thrown");
        } catch (SAXException e) {
            // Ok
        }
    }

    @Test
    public void testParseXml() throws Exception {
        KeyUsagePolicy kup = KeyUsagePolicy.fromXml(makeTestPolicyXml());
        assertNotNull(kup);
    }

    @Test
    public void testEnforceKeyUsage() throws Exception {
        KeyUsagePolicy kup = KeyUsagePolicy.fromXml(makeTestPolicyXml());

        // Test behavior given a typical client or server cert
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.signXml, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.verifyXml, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.encryptXml, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.decryptXml, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.sslClientRemote, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.sslClientRemote, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.sslServerRemote, KU_DSIG_KENC_NONREP));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.sslServerRemote, KU_DSIG_KENC_NONREP));
        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.verifyCrl, KU_DSIG_KENC_NONREP));
        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.verifyCrl, KU_DSIG_KENC_NONREP));

        // Test behavior given a typical CA cert
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.verifyCrl, KU_CRLSIG_CERTSIG));
        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.sslClientRemote,  KU_CRLSIG_CERTSIG));

        // Blanket permit rules
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.decryptXml, 0));
        assertTrue(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.signXml, 0));
    }

    @Test
    public void testNoBlanketPermits() throws Exception {
        KeyUsagePolicy kup = KeyUsagePolicy.fromXml(makeTestPolicyXmlWithNoBlanketPermits());

        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.decryptXml, 0));
        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.signXml, 0));
    }

    @Test
    public void testEmptyPolicy() throws Exception {
        KeyUsagePolicy kup = KeyUsagePolicy.fromXml(makeEmptyTestPolicy());

        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.decryptXml, 0xFFFFFFFF));
        assertFalse(kup.isKeyUsagePermittedForActivity(KeyUsageActivity.signXml, 0xFFFFFFFF));

        assertFalse("Empty policy doesn't even permit a cert with anyExtendedKeyUsage",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.sslClientRemote,
                Arrays.asList( KeyPurposeId.anyExtendedKeyUsage.getId() )));
    }

    @Test
    public void testEnforceExtendedKeyUsage() throws Exception {
        KeyUsagePolicy kup = KeyUsagePolicy.fromXml(makeTestPolicyXml());

        assertTrue("Blanket permit for activity",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.decryptXml, null));

        assertTrue("Cert that enables anyExtendedKeyUsage",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.verifyCrl,
                Arrays.asList( KeyPurposeId.anyExtendedKeyUsage.getId() )));

        assertTrue("Match first in list, specified in policy by OID string",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.sslClientRemote,
                Arrays.asList( KeyPurposeId.id_kp_clientAuth.getId(), KeyPurposeId.id_kp_serverAuth.getId() )));

        assertTrue("Match second in list, specified in policy by well-known key purpose name",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.sslServerRemote,
                Arrays.asList(  KeyPurposeId.id_kp_clientAuth.getId(), KeyPurposeId.id_kp_serverAuth.getId()  )));

        assertFalse("Fail if no non-wildcard permit rules for the activity and cert doesn't allow anyExtendedKeyUsage",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.verifyCrl,
                Arrays.asList( KeyPurposeId.id_kp_emailProtection.getId(), KeyPurposeId.id_kp_timeStamping.getId() )));

        assertFalse("Fail if none of the cert's OIDs match the list in an extendedKeyUsage permit rule",
                kup.isExtendedKeyUsagePermittedForActivity(KeyUsageActivity.sslServerRemote,
                Arrays.asList( KeyPurposeId.id_kp_emailProtection.getId(), KeyPurposeId.id_kp_timeStamping.getId() )));
    }

    public static String makeTestPolicyXml() {
        return
"<keyusagepolicy xmlns=\"http://www.layer7tech.com/ws/keyusage\">\n" +
"<permit><req>anyExtendedKeyUsage</req></permit>\n" +
"<permit action=\"signXml\"/>\n" +
"<permit action=\"verifyXml\"><req>digitalSignature</req></permit>\n" +
"<permit action=\"verifyXml\"><req>nonRepudiation</req></permit>\n" +
"<permit action=\"encryptXml\"><req>keyEncipherment</req></permit>\n" +
"<permit action=\"decryptXml\"/>\n" +
"<permit action=\"sslServerRemote\"><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslServerRemote\"><req>keyAgreement</req></permit>\n" +
"<permit action=\"sslServerRemote\"><req>id-kp-serverAuth</req></permit>\n" +
"<permit action=\"sslClientRemote\"><req>digitalSignature</req><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslClientRemote\"><req>nonRepudiation</req><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslClientRemote\"><req>1.3.6.1.5.5.7.3.2</req></permit>\n" +
"<permit action=\"verifyClientCert\"><req>keyCertSign</req></permit>\n" +
"<permit action=\"verifyCrl\"><req>cRLSign</req></permit>\n" +
"</keyusagepolicy>";
    }

    public static String makeTestPolicyXmlWithNoBlanketPermits() {
        return
"<keyusagepolicy xmlns=\"http://www.layer7tech.com/ws/keyusage\">\n" +
"<permit><req>anyExtendedKeyUsage</req></permit>\n" +
"<permit action=\"signXml\"><req>digitalSignature</req></permit>\n" +
"<permit action=\"verifyXml\"><req>nonRepudiation</req></permit>\n" +
"<permit action=\"encryptXml\"><req>keyEncipherment</req></permit>\n" +
"<permit action=\"decryptXml\"><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslServerRemote\"><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslServerRemote\"><req>keyAgreement</req></permit>\n" +
"<permit action=\"sslServerRemote\"><req>id-kp-serverAuth</req></permit>\n" +
"<permit action=\"sslClientRemote\"><req>digitalSignature</req><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslClientRemote\"><req>nonRepudiation</req><req>keyEncipherment</req></permit>\n" +
"<permit action=\"sslClientRemote\"><req>1.3.6.1.5.5.7.3.2</req></permit>\n" +
"<permit action=\"verifyClientCert\"><req>keyCertSign</req></permit>\n" +
"<permit action=\"verifyCrl\"><req>cRLSign</req></permit>\n" +
"</keyusagepolicy>";
    }

    public static String makeEmptyTestPolicy() {
        return
"<keyusagepolicy xmlns=\"http://www.layer7tech.com/ws/keyusage\">\n" +
"</keyusagepolicy>";
    }

}
