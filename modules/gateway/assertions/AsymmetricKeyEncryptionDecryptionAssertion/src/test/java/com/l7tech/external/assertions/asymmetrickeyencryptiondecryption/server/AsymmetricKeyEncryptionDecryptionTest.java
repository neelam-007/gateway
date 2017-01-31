package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server;

import com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.AsymmetricKeyEncryptionDecryptionAssertion;
import com.l7tech.policy.AllAssertionsTest;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * AsymmetricKeyEncryptionDecryptionTest
 */
public class AsymmetricKeyEncryptionDecryptionTest {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new AsymmetricKeyEncryptionDecryptionAssertion());
    }

    @Test
    public void testBackwardsCompatible_NO_MODE_NO_PADDING() throws Exception {
        final String assertionXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "            <L7p:InputVariable stringValue=\"input\"/>\n" +
                "            <L7p:KeyGoid goidValue=\"00000000000000000000000000000002\"/>\n" +
                "            <L7p:KeyName stringValue=\"abc\"/>\n" +
                "            <L7p:Mode intValue=\"2\"/>\n" +
                "            <L7p:ModePaddingOption rsaModePaddingOption=\"NO_MODE_NO_PADDING\"/>\n" +
                "            <L7p:OutputVariable stringValue=\"decrypted64\"/>\n" +
                "        </L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "</wsp:Policy>\n";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(AsymmetricKeyEncryptionDecryptionAssertion.class);
        WspReader wspReader = new WspReader(registry);

        AsymmetricKeyEncryptionDecryptionAssertion assertion = (AsymmetricKeyEncryptionDecryptionAssertion) wspReader.parseStrictly(assertionXML, WspReader.INCLUDE_DISABLED);
        assertNotNull(assertion);

        // Make sure NO_MODE_NO_PADDING to RSA/ECB/PCKS1Padding
        assertNull(assertion.getModePaddingOption());
        assertEquals(BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_PKCS1_PADDING), assertion.getAlgorithm());
    }

    @Test
    public void testBackwardsCompatible_ECB_NO_PADDING() throws Exception {
        final String assertionXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "            <L7p:InputVariable stringValue=\"input\"/>\n" +
                "            <L7p:KeyGoid goidValue=\"00000000000000000000000000000002\"/>\n" +
                "            <L7p:KeyName stringValue=\"abc\"/>\n" +
                "            <L7p:Mode intValue=\"2\"/>\n" +
                "            <L7p:ModePaddingOption rsaModePaddingOption=\"ECB_NO_PADDING\"/>\n" +
                "            <L7p:OutputVariable stringValue=\"decrypted64\"/>\n" +
                "        </L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "</wsp:Policy>\n";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(AsymmetricKeyEncryptionDecryptionAssertion.class);
        WspReader wspReader = new WspReader(registry);

        AsymmetricKeyEncryptionDecryptionAssertion assertion = (AsymmetricKeyEncryptionDecryptionAssertion) wspReader.parseStrictly(assertionXML, WspReader.INCLUDE_DISABLED);
        assertNotNull(assertion);

        // Make sure NO_MODE_NO_PADDING to RSA/ECB/PCKS1Padding
        assertNull(assertion.getModePaddingOption());
        assertEquals(BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_NO_PADDING), assertion.getAlgorithm());
    }

    @Test
    public void testBackwardsCompatible_ECB_PKCS1_PADDING() throws Exception {
        final String assertionXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "            <L7p:InputVariable stringValue=\"input\"/>\n" +
                "            <L7p:KeyGoid goidValue=\"00000000000000000000000000000002\"/>\n" +
                "            <L7p:KeyName stringValue=\"abc\"/>\n" +
                "            <L7p:Mode intValue=\"2\"/>\n" +
                "            <L7p:ModePaddingOption rsaModePaddingOption=\"ECB_PKCS1_PADDING\"/>\n" +
                "            <L7p:OutputVariable stringValue=\"decrypted64\"/>\n" +
                "        </L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "</wsp:Policy>\n";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(AsymmetricKeyEncryptionDecryptionAssertion.class);
        WspReader wspReader = new WspReader(registry);

        AsymmetricKeyEncryptionDecryptionAssertion assertion = (AsymmetricKeyEncryptionDecryptionAssertion) wspReader.parseStrictly(assertionXML, WspReader.INCLUDE_DISABLED);
        assertNotNull(assertion);

        // Make sure NO_MODE_NO_PADDING to RSA/ECB/PCKS1Padding
        assertNull(assertion.getModePaddingOption());
        assertEquals(BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_PKCS1_PADDING), assertion.getAlgorithm());
    }

    @Test
    public void testBackwardsCompatible_ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING() throws Exception {
        final String assertionXML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "            <L7p:InputVariable stringValue=\"input\"/>\n" +
                "            <L7p:KeyGoid goidValue=\"00000000000000000000000000000002\"/>\n" +
                "            <L7p:KeyName stringValue=\"abc\"/>\n" +
                "            <L7p:Mode intValue=\"2\"/>\n" +
                "            <L7p:ModePaddingOption rsaModePaddingOption=\"ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING\"/>\n" +
                "            <L7p:OutputVariable stringValue=\"decrypted64\"/>\n" +
                "        </L7p:AsymmetricKeyEncryptionDecryption>\n" +
                "</wsp:Policy>\n";

        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(AsymmetricKeyEncryptionDecryptionAssertion.class);
        WspReader wspReader = new WspReader(registry);

        AsymmetricKeyEncryptionDecryptionAssertion assertion = (AsymmetricKeyEncryptionDecryptionAssertion) wspReader.parseStrictly(assertionXML, WspReader.INCLUDE_DISABLED);
        assertNotNull(assertion);

        // Make sure NO_MODE_NO_PADDING to RSA/ECB/PCKS1Padding
        assertNull(assertion.getModePaddingOption());
        assertEquals(BlockAsymmetricAlgorithm.getAlgorithm(BlockAsymmetricAlgorithm.NAME_RSA, BlockAsymmetricAlgorithm.MODE_ECB, BlockAsymmetricAlgorithm.PADDING_OAEP_WITH_SHA1_AND_MGF1_PADDING), assertion.getAlgorithm());
    }
}
