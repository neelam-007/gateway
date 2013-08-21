package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.AllAssertionsTest;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * User: vchan
 */
public class SamlpRequestBuilderAssertionTest extends BaseAssertionTestCase<SamlpRequestBuilderAssertion> {

    @Test
    public void testCloneIsDeepCopy() throws Exception {
        AllAssertionsTest.checkCloneIsDeepCopy(new SamlpRequestBuilderAssertion());
    }

    @Test
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
            assertFalse("Legacy assertion should parse as having OAEP disabled", (castAssertionClass(obj).getXmlEncryptConfig().isUseOaep()));

        } catch (Exception ex) {
            fail("Unexpected error encountered -- " + ex);
        }
    }


    @Override
    protected boolean isAssertionClass(Object obj) {
        return (obj instanceof SamlpRequestBuilderAssertion);
    }

    @Override
    protected SamlpRequestBuilderAssertion castAssertionClass(Object obj) {
        return SamlpRequestBuilderAssertion.class.cast(obj);
    }

    @Test
    public void testFeatureNames() throws Exception {
        assertEquals("assertion:SamlpRequestBuilder", new SamlpRequestBuilderAssertion().getFeatureSetName());
    }

    @Test
    public void testParsePreEscolarSp1PolicyXml() throws Exception {
        try {
            Object obj = parseAssertionFromXml(POLICY_PRE_ESCOLAR_SP1_SIGN);
            assertNotNull(obj);
            assertTrue(isAssertionClass(obj));
            SamlpRequestBuilderAssertion requestBuilder = (SamlpRequestBuilderAssertion) obj;
            // validate old serialized property was set and assigned correctly
            assertTrue(requestBuilder.isSignAssertion());
            assertEquals(new Integer(2), requestBuilder.getVersion());

        } catch (Exception ex) {
            fail("Unexpected error encountered -- " + ex);
        }

        try {
            Object obj = parseAssertionFromXml(POLICY_PRE_ESCOLAR_SP1_NO_SIGN);
            assertNotNull(obj);
            assertTrue(isAssertionClass(obj));
            SamlpRequestBuilderAssertion requestBuilder = (SamlpRequestBuilderAssertion) obj;
            // validate old serialized property was set and assigned correctly
            assertFalse(requestBuilder.isSignAssertion());
            assertFalse(requestBuilder.getXmlEncryptConfig().isUseOaep());

        } catch (Exception ex) {
            fail("Unexpected error encountered -- " + ex);
        }

        try {
            Object obj = parseAssertionFromXml(POLICY_PRE_ESCOLAR_SP1_NO_SIGN_VERSION_1_1);
            assertNotNull(obj);
            assertTrue(isAssertionClass(obj));
            SamlpRequestBuilderAssertion requestBuilder = (SamlpRequestBuilderAssertion) obj;
            // validate old serialized property was set and assigned correctly
            assertFalse(requestBuilder.isSignAssertion());
            assertEquals(new Integer(1), requestBuilder.getVersion());
            assertFalse(requestBuilder.getXmlEncryptConfig().isUseOaep());

        } catch (Exception ex) {
            fail("Unexpected error encountered -- " + ex);
        }

    }

    private static final String POLICY_PRE_ESCOLAR_SP1_SIGN = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SamlpRequestBuilder>\n" +
            "            <L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
            "                <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
            "                    <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "                        <L7p:FriendlyName stringValue=\"\"/>\n" +
            "                        <L7p:Name stringValue=\"single\"/>\n" +
            "                        <L7p:Namespace stringValue=\"\"/>\n" +
            "                        <L7p:Value stringValue=\"\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Attributes>\n" +
            "            </L7p:AttributeStatement>\n" +
            "            <L7p:OtherTargetMessageVariable stringValueNull=\"null\"/>\n" +
            "            <L7p:RequestId boxedIntegerValue=\"0\"/>\n" +
            "            <L7p:SamlVersion boxedIntegerValue=\"2\"/>\n" +
            "            <L7p:SoapVersion boxedIntegerValue=\"0\"/>\n" +
            "            <L7p:SubjectConfirmationKeyInfoType subjectConfirmationKeyInfoType=\"STR_THUMBPRINT\"/>\n" +
            "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "            <L7p:XmlEncryptConfig xmlElementEncryptionConfig=\"included\"/>\n" +
            "        </L7p:SamlpRequestBuilder>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    private static final String POLICY_PRE_ESCOLAR_SP1_NO_SIGN = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SamlpRequestBuilder>\n" +
            "            <L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
            "                <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
            "                    <L7p:item samlAttributeElementInfo=\"included\">\n" +
            "                        <L7p:FriendlyName stringValue=\"\"/>\n" +
            "                        <L7p:Name stringValue=\"single\"/>\n" +
            "                        <L7p:Namespace stringValue=\"\"/>\n" +
            "                        <L7p:Value stringValue=\"\"/>\n" +
            "                    </L7p:item>\n" +
            "                </L7p:Attributes>\n" +
            "            </L7p:AttributeStatement>\n" +
            "            <L7p:OtherTargetMessageVariable stringValueNull=\"null\"/>\n" +
            "            <L7p:RequestId boxedIntegerValue=\"0\"/>\n" +
            "            <L7p:SamlVersion boxedIntegerValue=\"2\"/>\n" +
            "            <L7p:SignRequest booleanValue=\"false\"/>\n" +
            "            <L7p:SoapVersion boxedIntegerValue=\"0\"/>\n" +
            "            <L7p:SubjectConfirmationKeyInfoType subjectConfirmationKeyInfoType=\"STR_THUMBPRINT\"/>\n" +
            "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
            "            <L7p:Target target=\"REQUEST\"/>\n" +
            "            <L7p:XmlEncryptConfig xmlElementEncryptionConfig=\"included\"/>\n" +
            "        </L7p:SamlpRequestBuilder>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

        private static final String POLICY_PRE_ESCOLAR_SP1_NO_SIGN_VERSION_1_1 = "" +
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:SamlpRequestBuilder>\n" +
                "            <L7p:AttributeStatement samlAttributeInfo=\"included\">\n" +
                "                <L7p:Attributes samlAttributeElementInfoArray=\"included\">\n" +
                "                    <L7p:item samlAttributeElementInfo=\"included\">\n" +
                "                        <L7p:Name stringValue=\"single\"/>\n" +
                "                        <L7p:Namespace stringValue=\"\"/>\n" +
                "                        <L7p:Value stringValue=\"\"/>\n" +
                "                    </L7p:item>\n" +
                "                </L7p:Attributes>\n" +
                "            </L7p:AttributeStatement>\n" +
                "            <L7p:OtherTargetMessageVariable stringValueNull=\"null\"/>\n" +
                "            <L7p:RequestId boxedIntegerValue=\"0\"/>\n" +
                "            <L7p:SamlVersion boxedIntegerValue=\"1\"/>\n" +
                "            <L7p:SignRequest booleanValue=\"false\"/>\n" +
                "            <L7p:SoapVersion boxedIntegerValue=\"0\"/>\n" +
                "            <L7p:SubjectConfirmationKeyInfoType subjectConfirmationKeyInfoType=\"STR_THUMBPRINT\"/>\n" +
                "            <L7p:SubjectConfirmationMethodUri stringValue=\"urn:oasis:names:tc:SAML:1.0:cm:holder-of-key\"/>\n" +
                "            <L7p:Target target=\"REQUEST\"/>\n" +
                "            <L7p:XmlEncryptConfig xmlElementEncryptionConfig=\"included\"/>\n" +
                "        </L7p:SamlpRequestBuilder>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";

}
