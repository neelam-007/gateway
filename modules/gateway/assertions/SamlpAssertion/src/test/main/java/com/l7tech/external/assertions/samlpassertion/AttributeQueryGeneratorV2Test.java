package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp2MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.NameIdentifierResolver;
import com.l7tech.external.assertions.samlpassertion.server.SamlpRequestConstants;
import com.l7tech.external.assertions.samlpassertion.server.v2.AttributeQueryGenerator;
import com.l7tech.server.audit.Auditor;
import saml.v2.assertion.AttributeType;
import saml.v2.assertion.NameIDType;
import saml.v2.protocol.AttributeQueryType;

import javax.xml.bind.JAXBElement;
import java.util.HashMap;

/**
 * User: vchan
 */
public class AttributeQueryGeneratorV2Test extends SamlpMessageGeneratorTest<AttributeQueryType> {

    protected final String ATTR_STATEMENT_EMPTY =
            "<L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
            "  <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
            "  </L7p:Attributes>\n" +
            "</L7p:AttributeStatement>";

    protected final String ATTR_STATEMENT_1 =
            "<L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
            "  <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
            "    <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "        <L7p:FriendlyName stringValue=\"IAMFRIEND\"/>\n" +
            "        <L7p:Name stringValue=\"aaaa\"/>\n" +
            "        <L7p:NameFormat stringValue=\"format\"/>\n" +
            "        <L7p:Namespace stringValue=\"\"/>\n" +
            "        <L7p:Value stringValue=\"Hello\"/>\n" +
            "    </L7p:item>\n" +
            "  </L7p:Attributes>\n" +
            "</L7p:AttributeStatement>";

    protected final String ATTR_STATEMENT_2 =
            "<L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
            "  <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
            "    <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "        <L7p:FriendlyName stringValue=\"IAMFRIEND\"/>\n" +
            "        <L7p:Name stringValue=\"aaaa\"/>\n" +
            "        <L7p:NameFormat stringValue=\"format\"/>\n" +
            "        <L7p:Namespace stringValue=\"\"/>\n" +
            "        <L7p:Value stringValue=\"Hello\"/>\n" +
            "    </L7p:item>\n" +
            "  </L7p:Attributes>\n" +
            "</L7p:AttributeStatement>";


    public void testCreateEmptyAttributeRequest() {

        try {

            AttributeQueryType result = doTest(createAssertionXml(TEST_POLICY_TEMPLATE, ATTR_STATEMENT_EMPTY));
            assertTrue("expected attributeList to be empty", result.getAttribute().isEmpty());

        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }


    public void testCreateAttributeRequest1() {

        try {
            AttributeQueryType result = doTest(createAssertionXml(TEST_POLICY_TEMPLATE, ATTR_STATEMENT_1));
            assertTrue(!result.getAttribute().isEmpty());
            assertEquals("expected attributeList.size = 1", 1, result.getAttribute().size());

            AttributeType at = result.getAttribute().get(0);
            assertNotNull(at);
            assertEquals("aaaa", at.getName());
            assertEquals("format", at.getNameFormat());
            assertEquals("IAMFRIEND", at.getFriendlyName());

            assertEquals(1, at.getAttributeValue().size());
            assertNotNull(at.getAttributeValue().get(0));
            assertTrue(at.getAttributeValue().get(0) instanceof String);

        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }


    public void testDebug() {

        final String policy =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "<L7p:SamlpRequestBuilder>\n" +
            "    <L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
            "        <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
            "            <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "                <L7p:Name stringValue=\"aaaa\"/>\n" +
            "                <L7p:Namespace stringValue=\"\"/>\n" +
            "                <L7p:Value stringValue=\"Hello\"/>\n" +
            "            </L7p:item>\n" +
            "            <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "                <L7p:Name stringValue=\"hello\"/>\n" +
            "                <L7p:NameFormat stringValue=\"aaaa\"/>\n" +
            "                <L7p:Namespace stringValue=\"\"/>\n" +
            "                <L7p:Value stringValue=\"asdfsadf\"/>\n" +
            "            </L7p:item>\n" +
            "            <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "                <L7p:FriendlyName stringValue=\"friendlyTest\"/>\n" +
            "                <L7p:Name stringValue=\"stupid\"/>\n" +
            "                <L7p:NameFormat stringValue=\"urn:oasis:names:tc:SAML:2.0:attrname-format:uri\"/>\n" +
            "                <L7p:Namespace stringValue=\"\"/>\n" +
            "                <L7p:RepeatIfMulti booleanValue=\"true\"/>\n" +
            "                <L7p:Value stringValue=\"asdfsdf\"/>\n" +
            "            </L7p:item>\n" +
            "        </L7p:Attributes>\n" +
            "    </L7p:AttributeStatement>\n" +
            "    <L7p:ConsentAttribute stringValue=\"http://consent\"/>\n" +
            "    <L7p:DestinationAttribute stringValue=\"http://dest\"/>\n" +
            "    <L7p:NameIdentifierType nameIdentifierType=\"FROM_USER\"/>\n" +
            "    <L7p:NameQualifier stringValue=\"\"/>\n" +
            "    <L7p:RequestId boxedIntegerValue=\"0\"/>\n" +
            "    <L7p:SamlVersion boxedIntegerValue=\"2\"/>\n" +
            "    <L7p:SoapVersion boxedIntegerValue=\"1\"/>\n" +
            "</L7p:SamlpRequestBuilder>" +
            "    </wsp:All>" +
            "</wsp:Policy>";

        try {
            AttributeQueryType result = doTest(policy);
            assertTrue(!result.getAttribute().isEmpty());
        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }

    protected int getSamlVersion() {
        return 2;
    }

    protected AbstractSamlp2MessageGenerator<AttributeQueryType> createMessageGenerator(SamlpRequestBuilderAssertion assertion) {

        try {
            Auditor auditor = new Auditor(this, appCtx, null);
            java.util.Map<String, Object> varMap = new HashMap<String, Object>();

            AttributeQueryGenerator gen = new AttributeQueryGenerator(varMap, auditor);
            gen.setNameResolver( new NameIdentifierResolver(assertion) {

                protected void parse() {
                    this.nameValue = "somebody@email-exchange.com";
                    this.nameFormat = SamlConstants.NAMEIDENTIFIER_EMAIL;
                }
            });
            gen.setIssuerNameResolver( new NameIdentifierResolver(assertion) {

                protected void parse() {
                    this.nameValue = "Bob-the-issuer";
                }
            });

            return gen;

        } catch (Exception ex) {
            failUnexpected("createMessageGenerator() failed to create Generator: ", ex);
        }
        return null;
    }

    protected void checkCommonRequestElements(AttributeQueryType request) {
        // ID
        assertNotNull(request.getID());
        assertTrue(request.getID().startsWith("samlp2-"));
        // Version
        assertEquals(SamlpRequestConstants.SAML_VERSION_2_0, request.getVersion());
        // Issue Instant
        assertNotNull(request.getIssueInstant());
        // Optional attributes
        assertEquals("http://consent", request.getConsent());
        assertEquals("http://dest", request.getDestination());
        // Issuer
        assertNotNull(request.getIssuer());
        // Extension
        assertNull(request.getExtensions());
        // Signature
        assertNull(request.getSignature());
    }

    private AttributeQueryType doTest(String policyXml) throws Exception {
        // create Assertion instance
        SamlpRequestBuilderAssertion assertion = getAssertionFromXml( policyXml );

        // create request generator
        AbstractSamlp2MessageGenerator<AttributeQueryType> generator = createMessageGenerator(assertion);
        assertNotNull(generator);

        // generate the request message
        AttributeQueryType result = generator.create(assertion);
        assertNotNull(result);

        // common checks
        doCommonCheck(result);

        // output Message
        outputRequest(generator.createJAXBElement(result));

        return result;
    }

    private void doCommonCheck(AttributeQueryType msg) {

        checkCommonRequestElements(msg);
        assertNotNull(msg.getSubject());
//        assertEquals(2, msg.getSubject().getContent().size());

        // NameIdentifier
        JAXBElement<?> elem1 = msg.getSubject().getContent().get(0);
        assertTrue(elem1.getValue() instanceof NameIDType);
        assertEquals("somebody@email-exchange.com", ((NameIDType) elem1.getValue()).getValue());
        assertEquals(SamlConstants.NAMEIDENTIFIER_EMAIL, ((NameIDType) elem1.getValue()).getFormat());

        // SubjectConfirmation
//        JAXBElement<?> elem2 = msg.getSubject().getContent().get(1);
//        assertTrue(elem2.getValue() instanceof SubjectConfirmationType);
    }

    private void outputRequest(JAXBElement<?> reqElement) {
        System.out.println( toXml(reqElement) );
    }
}