package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.TestDocuments;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.xmlsec.WssConfigurationAssertion;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.*;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.util.SoapConstants;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.Document;

import java.util.HashMap;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ServerWssConfigurationAssertion}.
 */
public class ServerWssConfigurationAssertionTest {

    private static BeanFactory beanFactory;
    private static DefaultKey defaultKey;

    @BeforeClass
    public static void setupKeys() throws Exception {
        JceProvider.init();
        defaultKey = new TestDefaultKey();
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("defaultKey", new TestDefaultKey());
            put("securityTokenResolver", new SimpleSecurityTokenResolver(null, new SignerInfo[] { defaultKey.getSslInfo() }));
        }});
    }

    @Test
    public void testSimpleConfig() throws Exception {
        WssConfigurationAssertion ass = new WssConfigurationAssertion();
        ServerWssConfigurationAssertion sass = new ServerWssConfigurationAssertion(ass, beanFactory, null);
        final PolicyEnforcementContext context = context(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getResponse().getSecurityKnob().getOrMakeDecorationRequirements();
        assertTrue("Timestamp included", dreq.isIncludeTimestamp());
        assertTrue("Timestamp signed", dreq.isSignTimestamp());
        assertTrue("Tokens protected", dreq.isProtectTokens());
        assertFalse("Using derived keys", dreq.isUseDerivedKeys());
        assertEquals("WS-SecureConversation Namespace", SoapConstants.WSSC_NAMESPACE, dreq.getNamespaceFactory().getWsscNs());
        assertTrue("Default key is message signing certificate", dreq.getSenderMessageSigningCertificate() == defaultKey.getSslInfo().getCertificate());
        assertTrue("Default key is message private key", dreq.getSenderMessageSigningPrivateKey() == defaultKey.getSslInfo().getPrivateKey());
        assertNull("Null (default) signature message digest", dreq.getSignatureMessageDigest());
        assertEquals("Null (default) encryption algorithm", dreq.getEncryptionAlgorithm(), "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
        assertEquals("SKI encryption key inclusion type", dreq.getEncryptionKeyInfoInclusionType(), KeyInfoInclusionType.STR_SKI);
        assertNull("Kerberos ticket", dreq.getKerberosTicket());
        assertNull("Kerberos ticket id", dreq.getKerberosTicketId());
        assertNull("Encrypted key", dreq.getEncryptedKey());
        assertNull("Encrypted key reference info", dreq.getEncryptedKeyReferenceInfo());
        assertTrue("Empty encrypted elements", dreq.getElementsToEncrypt().isEmpty());
        assertTrue("Empty signed elements", dreq.getElementsToSign().isEmpty());
    }

    @Test
    public void testOverrideSomeSettings() throws Exception {
        WssConfigurationAssertion ass = new WssConfigurationAssertion();
        ass.setAddTimestamp(false);
        ass.setSignTimestamp(false);
        ass.setProtectTokens(false);
        ass.setUseDerivedKeys(true);
        ass.setSecureConversationNamespace( SoapConstants.WSSC_NAMESPACE3 );
        ass.setDigestAlgorithmName("you dig it");
        ass.setEncryptionAlgorithmUri("you enc it");
        ass.setKeyWrappingAlgorithmUri("you enc keys");
        ass.setKeyReference(KeyReference.ISSUER_SERIAL.getName());
        ass.setEncryptionKeyReference(KeyReference.SKI.getName());
        ass.setWssVersion(WsSecurityVersion.WSS11);

        ServerWssConfigurationAssertion sass = new ServerWssConfigurationAssertion(ass, beanFactory, null);
        final PolicyEnforcementContext context = context(TestDocuments.getTestDocument(TestDocuments.PLACEORDER_CLEARTEXT));
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);

        DecorationRequirements dreq = context.getResponse().getSecurityKnob().getOrMakeDecorationRequirements();
        assertFalse("Timestamp included", dreq.isIncludeTimestamp());
        assertFalse("Timestamp signed", dreq.isSignTimestamp());
        assertFalse("Tokens protected", dreq.isProtectTokens());
        assertTrue("Using derived keys", dreq.isUseDerivedKeys());
        assertEquals("WS-SecureConversation Namespace", SoapConstants.WSSC_NAMESPACE3, dreq.getNamespaceFactory().getWsscNs());
        assertTrue("Default key is message signing certificate", dreq.getSenderMessageSigningCertificate() == defaultKey.getSslInfo().getCertificate());
        assertTrue("Default key is message private key", dreq.getSenderMessageSigningPrivateKey() == defaultKey.getSslInfo().getPrivateKey());
        assertEquals("Message signature message digest", dreq.getSignatureMessageDigest(), "you dig it");
        assertEquals("Message encryption algorithm", dreq.getEncryptionAlgorithm(), "you enc it");
        assertEquals("SKI encryption key inclusion type", dreq.getEncryptionKeyInfoInclusionType(), KeyInfoInclusionType.STR_SKI);
        assertNull("Kerberos ticket", dreq.getKerberosTicket());
        assertNull("Kerberos ticket id", dreq.getKerberosTicketId());
        assertNull("Encrypted key", dreq.getEncryptedKey());
        assertNull("Encrypted key reference info", dreq.getEncryptedKeyReferenceInfo());
        assertTrue("Empty encrypted elements", dreq.getElementsToEncrypt().isEmpty());
        assertTrue("Empty signed elements", dreq.getElementsToSign().isEmpty());
    }


    private static PolicyEnforcementContext context(Document doc) {
        final Message request = new Message(doc);
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
    }
}
