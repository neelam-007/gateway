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
        assertTrue(dreq.isIncludeTimestamp());
        assertTrue(dreq.isSignTimestamp());
        assertTrue(dreq.isProtectTokens());
        assertFalse(dreq.isUseDerivedKeys());
        assertTrue(dreq.getSenderMessageSigningCertificate() == defaultKey.getSslInfo().getCertificate());
        assertTrue(dreq.getSenderMessageSigningPrivateKey() == defaultKey.getSslInfo().getPrivateKey());
        assertNull(dreq.getSignatureMessageDigest());
        assertEquals(dreq.getEncryptionAlgorithm(), "http://www.w3.org/2001/04/xmlenc#aes128-cbc");
        assertEquals(dreq.getEncryptionKeyInfoInclusionType(), KeyInfoInclusionType.STR_SKI);
        assertNull(dreq.getKerberosTicket());
        assertNull(dreq.getKerberosTicketId());
        assertNull(dreq.getEncryptedKey());
        assertNull(dreq.getEncryptedKeyReferenceInfo());
        assertTrue(dreq.getElementsToEncrypt().isEmpty());
        assertTrue(dreq.getElementsToSign().isEmpty());
    }

    @Test
    public void testOverrideSomeSettings() throws Exception {
        WssConfigurationAssertion ass = new WssConfigurationAssertion();
        ass.setAddTimestamp(false);
        ass.setSignTimestamp(false);
        ass.setProtectTokens(false);
        ass.setUseDerivedKeys(true);
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
        assertFalse(dreq.isIncludeTimestamp());
        assertFalse(dreq.isSignTimestamp());
        assertFalse(dreq.isProtectTokens());
        assertTrue(dreq.isUseDerivedKeys());
        assertTrue(dreq.getSenderMessageSigningCertificate() == defaultKey.getSslInfo().getCertificate());
        assertTrue(dreq.getSenderMessageSigningPrivateKey() == defaultKey.getSslInfo().getPrivateKey());
        assertEquals(dreq.getSignatureMessageDigest(), "you dig it");
        assertEquals(dreq.getEncryptionAlgorithm(), "you enc it");
        assertEquals(dreq.getEncryptionKeyInfoInclusionType(), KeyInfoInclusionType.STR_SKI);
        assertNull(dreq.getKerberosTicket());
        assertNull(dreq.getKerberosTicketId());
        assertNull(dreq.getEncryptedKey());
        assertNull(dreq.getEncryptedKeyReferenceInfo());
        assertTrue(dreq.getElementsToEncrypt().isEmpty());
        assertTrue(dreq.getElementsToSign().isEmpty());
    }


    private static PolicyEnforcementContext context(Document doc) {
        final Message request = new Message(doc, 0);
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
    }
}
