package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.security.prov.JceProvider;
import com.l7tech.security.xml.SupportedDigestMethods;
import com.l7tech.security.xml.SupportedSignatureMethods;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.BeanFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 *
 */
public class ServerNonSoapSignElementAssertionTest {
    private static final Logger logger = Logger.getLogger(ServerNonSoapSignElementAssertionTest.class.getName());

    private static BeanFactory beanFactory;

    @BeforeClass
    public static void setupKeys() throws Exception {
        JceProvider.init();
        beanFactory = new SimpleSingletonBeanFactory(new HashMap<String,Object>() {{
            put("securityTokenResolver", NonSoapXmlSecurityTestUtils.makeSecurityTokenResolver());
            put("ssgKeyStoreManager", NonSoapXmlSecurityTestUtils.makeSsgKeyStoreManager());
            put("trustedCertCache", NonSoapXmlSecurityTestUtils.makeTrustedCertCache());
        }});
    }

    @Test
    public void testSimpleSignElement() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();

        Element sigElement = applySignature(ass, makeRequest(null));
        assertTrue("default signature type is LAST_CHILD", null == sigElement.getNextSibling());
    }

    @Test
    @BugNumber(10852)
    public void testSignElement_customDigests() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setDigestAlgName("SHA-256");
        ass.setRefDigestAlgName("SHA-384");

        Element sigElement = applySignature(ass, makeRequest(null));
        String sigXml = XmlUtil.nodeToFormattedString(sigElement);

        String wantSigMethod = SupportedSignatureMethods.fromKeyAndMessageDigest(NonSoapXmlSecurityTestUtils.getTestKey().getPublic().getAlgorithm(), ass.getDigestAlgName()).getAlgorithmIdentifier();
        String wantDigMethod = SupportedDigestMethods.fromAlias(ass.getRefDigestAlgName()).getIdentifier();

        // For now we'll just do a very crude substring test, to rule out obvious failures to honor the config
        assertTrue(sigXml.contains(wantSigMethod));
        assertTrue(sigXml.contains(wantDigMethod));
    }

    @Test
    @BugNumber(9602)
    public void testSimpleSignElementDetached() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setDetachedSignatureVariableName("sig");
        ass.setXpathExpression(new XpathExpression("/foo/bar/*"));
        Message request = makeRequest(null);
        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        NodeList sigElements = doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
        assertEquals("Signature elements shall now have been added to the document", 0, sigElements.getLength());
        Object sig = context.getVariable("sig");
        assertNotNull("sig var shall have been set", sig);
        assertTrue(sig instanceof Element);
    }

    @Test
    @BugNumber(9602)
    public void testSignElementDetached_enveloped_withEmptyUriRef() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setDetachedSignatureVariableName("sig");
        ass.setXpathExpression(new XpathExpression("/*"));
        ass.setCustomIdAttributeQname("");
        Message request = makeRequest(null);
        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        NodeList sigElements = doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
        assertEquals("Signature elements shall now have been added to the document", 0, sigElements.getLength());
        Element sig = (Element) context.getVariable("sig");
        assertNotNull("sig var shall have been set", sig);

        Element signedInfo = XmlUtil.findFirstChildElement(sig);
        Element reference = XmlUtil.findExactlyOneChildElementByName(signedInfo, SoapUtil.DIGSIG_URI, "Reference");
        Attr uriAttr = reference.getAttributeNode("URI");
        assertEquals("", uriAttr.getNodeValue());
    }

    @Test
    @BugNumber(7871)
    public void testSimpleSignElementEcdsa() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setKeyAlias(NonSoapXmlSecurityTestUtils.ECDSA_KEY_ALIAS);

        applySignature(ass, makeRequest(null));
    }

    @Test
    @BugNumber(8960)
    public void testSignatureLocation_FirstChild_ElementHasNoChildren() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setSignatureLocation(NonSoapSignElementAssertion.SignatureLocation.FIRST_CHILD);

        Element sigElement = applySignature(ass, makeRequest("<foo><bar/></foo>"));
        assertTrue("With no children, FIRST_CHILD behaves identically to LAST_CHILD", null == sigElement.getNextSibling());
    }

    @Test
    @BugNumber(8960)
    public void testSignatureLocation_FirstChild_ElementHasChildren() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setSignatureLocation(NonSoapSignElementAssertion.SignatureLocation.FIRST_CHILD);

        Element sigElement = applySignature(ass, makeRequest(null));
        assertNotNull("Must be first child", sigElement.getNextSibling());
    }

    @Test
    @BugNumber(8959)
    public void testCustomId_simple() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setCustomIdAttributeQname("customId");

        Element sigElement = applySignature(ass, makeRequest(null));
        Element signed = (Element) sigElement.getParentNode();
        Attr attr = signed.getAttributeNode("customId");
        assertTrue(attr.getValue().length() > 0);
        assertTrue(attr.getNamespaceURI() == null || XMLConstants.NULL_NS_URI.equals(attr.getNamespaceURI()));
        assertEquals("customId", attr.getLocalName());
    }

    @Test
    @BugNumber(8959)
    public void testCustomId_forcedUseOfUndeclaredPrefix() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setCustomIdAttributeQname("pfx:customId");

        Element sigElement = applySignature(ass, makeRequest(null));
        Element signed = (Element) sigElement.getParentNode();
        assertTrue(signed.getAttribute("pfx:customId").length() > 0);
    }

    @Test
    @BugNumber(8959)
    public void testCustomId_customNamespace() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setCustomIdAttributeQname("{urn:blatch}customId");

        Element sigElement = applySignature(ass, makeRequest(null));
        Element signed = (Element) sigElement.getParentNode();
        Attr idAttr = signed.getAttributeNodeNS("urn:blatch", "customId"); // we don't care what prefix it got assigned, just that the NS matches up
        assertNotNull(idAttr);
        assertTrue(idAttr.getValue().length() > 0);
    }

    @Test
    @BugNumber(8959)
    public void testCustomId_customNamespaceWithSuggestedPrefix() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setCustomIdAttributeQname("{urn:blatch}pfx:customId");

        Element sigElement = applySignature(ass, makeRequest(null));
        Element signed = (Element) sigElement.getParentNode();
        Attr idAttr = signed.getAttributeNodeNS("urn:blatch", "customId");
        assertNotNull(idAttr);
        assertTrue(idAttr.getValue().length() > 0);
        assertEquals("pfx:customId", idAttr.getName());
    }

    @Test
    @BugNumber(8959)
    public void testCustomId_customNamespaceWithExistingId() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setCustomIdAttributeQname("{urn:blatch}pfx:customId");

        Element sigElement = applySignature(ass, makeRequest("<foo xmlns:c=\"urn:blatch\"><bar/></foo>"));
        Element signed = (Element) sigElement.getParentNode();
        Attr idAttr = signed.getAttributeNodeNS("urn:blatch", "customId");
        assertNotNull(idAttr);
        assertTrue(idAttr.getValue().length() > 0);
        assertEquals("c:customId", idAttr.getName());
    }

    @Test
    @BugNumber(8959)
    public void testCustomId_customNamespaceWithConflictingPrefix() throws Exception {
        NonSoapSignElementAssertion ass = makeAssertion();
        ass.setCustomIdAttributeQname("{urn:blatch}pfx:customId");

        Element sigElement = applySignature(ass, makeRequest("<foo xmlns:pfx=\"urn:other\"><bar/></foo>"));
        Element signed = (Element) sigElement.getParentNode();
        Attr idAttr = signed.getAttributeNodeNS("urn:blatch", "customId");
        assertNotNull(idAttr);
        assertTrue(idAttr.getValue().length() > 0);
        assertFalse("pfx".equals(idAttr.getPrefix()));
    }

    private NonSoapSignElementAssertion makeAssertion() {
        NonSoapSignElementAssertion ass = new NonSoapSignElementAssertion();
        ass.setKeyAlias("data");
        ass.setUsesDefaultKeyStore(false);
        ass.setNonDefaultKeystoreId(-1);
        ass.setXpathExpression(new XpathExpression("/foo/bar"));
        ass.setTarget(TargetMessageType.REQUEST);
        return ass;
    }

    private static Message makeRequest(String reqXml) {
        return new Message(XmlUtil.stringAsDocument(reqXml != null ? reqXml : "<foo><bar><child1/><child2>foo</child2></bar></foo>"));
    }

    // Returns the ds:Signature element, embedded within the signed Document
    private static Element applySignature(NonSoapSignElementAssertion ass, Message request) throws InvalidXpathException, ParseException, IOException, PolicyAssertionException, SAXException {
        ServerNonSoapSignElementAssertion sass = new ServerNonSoapSignElementAssertion(ass, beanFactory);
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, new Message());
        AssertionStatus result = sass.checkRequest(context);
        assertEquals(AssertionStatus.NONE, result);
        Document doc = request.getXmlKnob().getDocumentReadOnly();
        logger.info("Signed XML (reformatted):\n" + XmlUtil.nodeToFormattedString(doc));
        NodeList sigElements = doc.getElementsByTagNameNS(SoapUtil.DIGSIG_URI, "Signature");
        assertEquals(1, sigElements.getLength());
        return (Element)sigElements.item(0);
    }
}
