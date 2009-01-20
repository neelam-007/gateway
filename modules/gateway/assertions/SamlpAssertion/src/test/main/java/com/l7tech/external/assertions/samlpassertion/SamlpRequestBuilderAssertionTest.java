package com.l7tech.external.assertions.samlpassertion;

/**
 * User: vchan
 */
public class SamlpRequestBuilderAssertionTest extends BaseAssertionTest<SamlpRequestBuilderAssertion> {


    public void testParseAssertion() {

        final String policyXml =
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "      <L7p:SamlpRequestBuilder>\n" +
                "        <L7p:AuthenticationStatement samlAuthenticationInfo=\"included\">\n" +
                "          <L7p:AuthenticationMethods stringArrayValue=\"included\"/>\n" +
                "        </L7p:AuthenticationStatement>\n" +
                "        <L7p:NameQualifier stringValue=\"\"/>\n" +
                "        <L7p:Version boxedIntegerValue=\"1\"/>\n" +
                "      </L7p:SamlpRequestBuilder>" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

//                "        <L7p:SamlpRequestBuilder>\n" +
//                "            <L7p:AuthorizationStatement samlAuthorizationInfo=\"included\">\n" +
//                "                <L7p:Action stringValue=\"Invoke\"/>\n" +
//                "                <L7p:ActionNamespace stringValue=\"namespace\"/>\n" +
//                "                <L7p:Resource stringValue=\"http://localhost:8081/targetService\"/>\n" +
//                "            </L7p:AuthorizationStatement>\n" +
//                "            <L7p:ConsentAttribute stringValue=\"http://consent\"/>\n" +
//                "            <L7p:DestinationAttribute stringValue=\"http://dest\"/>\n" +
//                "            <L7p:Evidence boxedIntegerValue=\"1\"/>\n" +
//                "            <L7p:EvidenceVariable stringValue=\"evid.enc.e\"/>\n" +
//                "            <L7p:NameIdentifierFormat stringValue=\"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\"/>\n" +
//                "            <L7p:NameIdentifierType nameIdentifierType=\"SPECIFIED\"/>\n" +
//                "            <L7p:NameIdentifierValue stringValue=\"${name.id.template}\"/>\n" +
//                "            <L7p:NameQualifier stringValue=\"${qualifer.name}\"/>\n" +
//                "            <L7p:OtherTargetMessageVariable stringValue=\"${samlpRequest.message}\"/>\n" +
//                "            <L7p:RequestId boxedIntegerValue=\"1\"/>\n" +
//                "            <L7p:RequestIdVariable stringValue=\"${request.id}\"/>\n" +
//                "            <L7p:SamlVersion boxedIntegerValue=\"1\"/>\n" +
//                "            <L7p:SoapVersion boxedIntegerValue=\"1\"/>\n" +
//                "            <L7p:SubjectConfirmationKeyInfoType subjectConfirmationKeyInfoType=\"NONE\"/>\n" +
//                "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
//                "            <L7p:Target target=\"OTHER\"/>\n" +
//                "        </L7p:SamlpRequestBuilder>";
        try {
            Object obj = parseAssertionFromXml(policyXml);
            assertNotNull(obj);
            assertTrue(isAssertionClass(obj));

        } catch (Exception ex) {
            fail("Unexpected error encountered -- " + ex);
        }
    }


    protected boolean isAssertionClass(Object obj) {
        return (obj instanceof SamlpRequestBuilderAssertion);
    }

    protected SamlpRequestBuilderAssertion castAssertionClass(Object obj) {
        return SamlpRequestBuilderAssertion.class.cast(obj);
    }

    public void testFeatureNames() throws Exception {
        assertEquals("assertion:SamlpRequestBuilder", new SamlpRequestBuilderAssertion().getFeatureSetName());
    }

}
