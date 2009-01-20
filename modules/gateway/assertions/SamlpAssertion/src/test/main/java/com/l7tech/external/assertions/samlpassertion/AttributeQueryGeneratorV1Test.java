package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.common.security.saml.SamlConstants;
import com.l7tech.external.assertions.samlpassertion.server.AbstractSamlp1MessageGenerator;
import com.l7tech.external.assertions.samlpassertion.server.NameIdentifierResolver;
import com.l7tech.external.assertions.samlpassertion.server.v1.AttributeQueryGenerator;
import com.l7tech.server.audit.Auditor;
import saml.v1.assertion.AttributeDesignatorType;
import saml.v1.protocol.AttributeQueryType;
import saml.v1.protocol.RequestType;

import javax.xml.bind.JAXBElement;
import java.math.BigInteger;
import java.util.HashMap;

/**
 * User: vchan
 */
public class AttributeQueryGeneratorV1Test extends SamlpMessageGeneratorTest<RequestType> {

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
            "        <L7p:Namespace stringValue=\"testNs\"/>\n" +
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
            "        <L7p:Namespace stringValue=\"testNs\"/>\n" +
            "        <L7p:Value stringValue=\"Hello\"/>\n" +
            "    </L7p:item>\n" +
            "  </L7p:Attributes>\n" +
            "</L7p:AttributeStatement>";


    public void testCreateEmptyAttributeRequest() {

        try {

            RequestType result = doTest(createAssertionXml(TEST_POLICY_TEMPLATE, ATTR_STATEMENT_EMPTY));
            assertTrue("expected attributeList to be empty", result.getAttributeQuery().getAttributeDesignator().isEmpty());

        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }


    public void testCreateAttributeRequest1() {

        try {
            RequestType req = doTest(createAssertionXml(TEST_POLICY_TEMPLATE, ATTR_STATEMENT_1));
            AttributeQueryType result = req.getAttributeQuery();
            assertTrue(!result.getAttributeDesignator().isEmpty());
            assertEquals("expected attributeList.size = 1", 1, result.getAttributeDesignator().size());

            AttributeDesignatorType at = result.getAttributeDesignator().get(0);
            assertNotNull(at);
            assertEquals("aaaa", at.getAttributeName());
            assertEquals("testNs", at.getAttributeNamespace());

//            assertEquals(1, at.getAttributeValue().size());
//            assertNotNull(at.getAttributeValue().get(0));
//            assertTrue(at.getAttributeValue().get(0) instanceof String);

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
            "                <L7p:Namespace stringValue=\"testNs\"/>\n" +
            "                <L7p:Value stringValue=\"asdfsadf\"/>\n" +
            "            </L7p:item>\n" +
            "            <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "                <L7p:FriendlyName stringValue=\"friendlyTest\"/>\n" +
            "                <L7p:Name stringValue=\"stupid\"/>\n" +
            "                <L7p:Namespace stringValue=\"testNs\"/>\n" +
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
            "    <L7p:SamlVersion boxedIntegerValue=\"1\"/>\n" +
            "    <L7p:SoapVersion boxedIntegerValue=\"1\"/>\n" +
            "</L7p:SamlpRequestBuilder>" +
            "    </wsp:All>" +
            "</wsp:Policy>";

        try {
            RequestType result = doTest(policy);
            assertTrue(!result.getAttributeQuery().getAttributeDesignator().isEmpty());
        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }


    protected AbstractSamlp1MessageGenerator<AttributeQueryType> createMessageGenerator(SamlpRequestBuilderAssertion assertion) {

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

    protected int getSamlVersion() {
        return 1;
    }
    
    private RequestType doTest(String policyXml) throws Exception {
        // create Assertion instance
        SamlpRequestBuilderAssertion assertion = getAssertionFromXml( policyXml );

        // create request generator
        AbstractSamlp1MessageGenerator<AttributeQueryType> generator = createMessageGenerator(assertion);
        assertNotNull(generator);

        // generate the request message
        RequestType result = generator.create(assertion);
        assertNotNull(result);

        // common checks
        doCommonCheck(result);

        // output Message
        outputRequest(generator.createJAXBElement(result));

        return result;
    }

    protected void checkCommonRequestElements(RequestType request) {
        // more to come
        // ID
        assertNotNull(request.getRequestID());
        assertTrue(request.getRequestID().startsWith("samlp-"));
        // Version
        assertEquals(BigInteger.valueOf(1), request.getMajorVersion());
        assertEquals(BigInteger.valueOf(1), request.getMinorVersion());
        // Issue Instant
        assertNotNull(request.getIssueInstant());
        // Signature
        assertNull(request.getSignature());
    }

    private void doCommonCheck(RequestType msg) {

        checkCommonRequestElements(msg);
        assertNotNull(msg.getAttributeQuery());
    }

    private void outputRequest(JAXBElement<?> reqElement) {
        System.out.println( toXml(reqElement) );
    }
}