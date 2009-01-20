package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.server.SamlpMessageGenerator;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * User: vchan
 */
public abstract class SamlpMessageGeneratorTest<SAMLP_MSG>
    extends BaseAssertionTest<SamlpRequestBuilderAssertion>
{
    protected static boolean PRINT_STACK = false;

    protected final String TEST_POLICY_TEMPLATE =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SamlpRequestBuilder>\n" +
            "            {0}" +
            "            <L7p:ConsentAttribute stringValue=\"http://consent\"/>\n" +
            "            <L7p:DestinationAttribute stringValue=\"http://dest\"/>\n" +
            "            <L7p:Evidence boxedIntegerValue=\"0\"/>\n" +
            "            <L7p:NameIdentifierFormat stringValue=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\"/>\n" +
            "            <L7p:NameIdentifierType nameIdentifierType=\"SPECIFIED\"/>\n" +
            "            <L7p:NameIdentifierValue stringValue=\"testNameIdentifier\"/>\n" +
            "            <L7p:NameQualifier stringValue=\"nameQualifierValue\"/>\n" +
//            "            <L7p:OtherTargetMessageVariable stringValue=\"${samlpRequest.message}\"/>\n" +
            "            <L7p:RequestId boxedIntegerValue=\"0\"/>\n" +
            "            <L7p:SamlVersion boxedIntegerValue=\"1\"/>\n" +
            "            <L7p:SoapVersion boxedIntegerValue=\"1\"/>\n" +
            "            <L7p:SubjectConfirmationKeyInfoType subjectConfirmationKeyInfoType=\"NONE\"/>\n" +
            "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:sender-vouches\"/>\n" +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "        </L7p:SamlpRequestBuilder>\n" +
            "    </wsp:All>" +
            "</wsp:Policy>";


    protected final String TEST_POLICY_SIMPLE =
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SamlpRequestBuilder>\n" +
            "            <L7p:AuthorizationStatement samlpAuthorizationInfo=\"included\">\n" +
            "                <L7p:Actions stringArrayValue=\"included\">\n" +
            "                    <L7p:item stringValue=\"bbb||ccc\"/>\n" +
            "                </L7p:Actions>\n" +
            "                <L7p:Resource stringValue=\"aaa\"/>\n" +
            "            </L7p:AuthorizationStatement>" +
//            "            <L7p:AuthorizationStatement samlAuthorizationInfo=\"included\">\n" +
//            "                <L7p:Action stringValue=\"Invoke\"/>\n" +
//            "                <L7p:ActionNamespace stringValue=\"namespace\"/>\n" +
//            "                <L7p:Resource stringValue=\"http://localhost:8081/targetService\"/>\n" +
//            "            </L7p:AuthorizationStatement>\n" +
            "            <L7p:ConsentAttribute stringValue=\"http://consent\"/>\n" +
            "            <L7p:DestinationAttribute stringValue=\"http://dest\"/>\n" +
            "            <L7p:Evidence boxedIntegerValue=\"0\"/>\n" +
//            "            <L7p:EvidenceVariable stringValue=\"evid.enc.e\"/>\n" +
            "            <L7p:NameIdentifierFormat stringValue=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\"/>\n" +
            "            <L7p:NameIdentifierType nameIdentifierType=\"SPECIFIED\"/>\n" +
            "            <L7p:NameIdentifierValue stringValue=\"testNameIdentifier\"/>\n" +
            "            <L7p:NameQualifier stringValue=\"nameQualifierValue\"/>\n" +
            "            <L7p:OtherTargetMessageVariable stringValue=\"${samlpRequest.message}\"/>\n" +
            "            <L7p:RequestId boxedIntegerValue=\"0\"/>\n" +
//            "            <L7p:RequestIdVariable stringValue=\"${request.id}\"/>\n" +
            "            <L7p:SamlVersion boxedIntegerValue=\"1\"/>\n" +
            "            <L7p:SoapVersion boxedIntegerValue=\"2\"/>\n" +
            "            <L7p:SubjectConfirmationKeyInfoType subjectConfirmationKeyInfoType=\"NONE\"/>\n" +
//            "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:sender-vouches\"/>\n" +
            "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
            "            <L7p:Target target=\"OTHER\"/>\n" +
            "        </L7p:SamlpRequestBuilder>\n" +
            "    </wsp:All>" +
            "</wsp:Policy>";

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void failUnexpected(Exception ex) {
        failUnexpected("Unexpected Exception encountered: ", ex);
    }

    protected void failUnexpected(String msg, Exception ex) {
        if (PRINT_STACK)
            ex.printStackTrace();

        if (msg != null) {
            fail(msg + ex);
        }
        fail ("Unexpected Exception encountered: " + ex);
    }

    protected boolean isAssertionClass(Object obj) {
        return (obj instanceof SamlpRequestBuilderAssertion);
    }

    protected SamlpRequestBuilderAssertion castAssertionClass(Object obj) {
        return SamlpRequestBuilderAssertion.class.cast(obj);
    }

    protected SamlpRequestBuilderAssertion getAssertionFromXml(String policyXml) throws Exception {
        Object obj = parseAssertionFromXml(policyXml);
        assertNotNull(obj);
        assertTrue(isAssertionClass(obj));

        SamlpRequestBuilderAssertion assertion = SamlpRequestBuilderAssertion.class.cast(obj);
        assertion.setSamlVersion(getSamlVersion());
        return assertion;
    }

    protected SamlpMessageGenerator createMessageGenerator(String policyXml) throws Exception {
        // parse assertion
        SamlpRequestBuilderAssertion assertion = getAssertionFromXml(policyXml);

        // create generator
        return createMessageGenerator(assertion);
    }

    protected abstract int getSamlVersion();

    protected abstract SamlpMessageGenerator<SamlpRequestBuilderAssertion, SAMLP_MSG> createMessageGenerator(SamlpRequestBuilderAssertion assertion);

    public void testCreateEmptyRequest() {

        try {
            SamlpRequestBuilderAssertion assertion = getAssertionFromXml(TEST_POLICY_SIMPLE);
            SamlpMessageGenerator<SamlpRequestBuilderAssertion, SAMLP_MSG> generator = createMessageGenerator(assertion);
            assertNotNull(generator);

            SAMLP_MSG result = generator.create(assertion);
            assertNotNull(result);

            checkCommonRequestElements(result);

            JAXBElement<?> reqElement = generator.createJAXBElement(result);
            System.out.println(toXml( reqElement ));
        } catch (Exception ex) {
            failUnexpected(ex);
        }
    }


    protected abstract void checkCommonRequestElements(SAMLP_MSG request);
    /*
    {
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
    */

    protected String createAssertionXml(String templatePolicy, String statementBlock){

        if (statementBlock == null)
            return templatePolicy;

        return MessageFormat.format(templatePolicy, statementBlock);
    }



    protected String toXml(Object request) {

        try {
            JAXBContext jxbContext;
            if (getSamlVersion() == 1)
                jxbContext = JAXBContext.newInstance("saml.v1.protocol:saml.v1.assertion");
            else
                jxbContext = JAXBContext.newInstance("saml.v2.protocol:saml.v2.assertion");

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.newDocument();

//            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller m = jxbContext.createMarshaller();
            m.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            m.marshal(request, doc);

//            SOAPMessage soap = SoapUtil.asSOAPMessage(doc);
//            soap.writeTo(System.out);

            byte[] strValue = XmlUtil.toByteArray(doc);

            return new String(strValue);

//        } catch (SOAPException soapEx) {
//            failUnexpected(soapEx);
        } catch (IOException ioex) {
            failUnexpected(ioex);
        } catch (ParserConfigurationException pex) {
            failUnexpected(pex);
        } catch (JAXBException jex) {
            failUnexpected(jex);
        }
        return null;
    }

}
