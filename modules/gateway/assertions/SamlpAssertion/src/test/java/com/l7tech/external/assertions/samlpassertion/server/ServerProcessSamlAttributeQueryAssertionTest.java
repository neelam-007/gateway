package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion.*;
import static org.junit.Assert.*;

public class ServerProcessSamlAttributeQueryAssertionTest {

    @Test
    public void testSuccessCase_SoapEncapsulated() throws Exception {

        final String soap = "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
                "    xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n" +
                "    <s:Header/>\n" +
                "    <s:Body>\n" +
                validRequestWithAllOptionalValuesIncludingAttributes +
                "    </s:Body>\n" +
                "</s:Envelope>";

        System.out.println(soap);
        Message request = new Message(XmlUtil.parse(soap));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setCustomSubjectFormats("urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n");
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        assertion.setSoapEncapsulated(true);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);
    }

    @Test(expected = AssertionStatusException.class)
    public void testErrorCase_NotSoapEncapsulated() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setCustomSubjectFormats("urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n");
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        assertion.setSoapEncapsulated(true);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        serverAssertion.checkRequest(context);
    }

    @Test(expected = AssertionStatusException.class)
    public void testNotXml() throws Exception {
        Message request = new Message();
        request.initialize(ContentTypeHeader.APPLICATION_JSON, "{\"Name\": \"Foo\", \"Id\" : 1234, \"Rank\": 7}".getBytes());

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        serverAssertion.checkRequest(context);
    }

    @Test
    public void testSuccessCase_AllVariablesSet_NoDecryption() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setCustomSubjectFormats("urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n");
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);

        final VariableMetadata[] variablesSet = assertion.getVariablesSet();
        final List<String> map = Functions.map(Arrays.asList(variablesSet), new Functions.Unary<String, VariableMetadata>() {
            @Override
            public String call(VariableMetadata variableMetadata) {
                return variableMetadata.getName();
            }
        });
        final Map<String,Object> variableMap = context.getVariableMap(map.toArray(new String[map.size()]), new TestAudit());

        // validate all variables were set - values may be null.
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ATTRIBUTES)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_NAME_QUALIFIER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_NAME_QUALIFIER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_PROVIDED_ID)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ID)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_VERSION)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ISSUE_INSTANT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_DESTINATION)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_CONSENT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_NAME_QUALIFIER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_SP_NAME_QUALIFIER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_FORMAT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_SP_PROVIDED_ID)));
    }

    @Test
    public void testSuccessCase_ValidateSetVariables() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setCustomSubjectFormats("urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n");
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);

        //note this equals assumes the value was trimmed
        assertEquals("70001234000002110000000000000000", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT)));
        assertEquals("urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT)));
        assertEquals("aaf23196-1773-2113-474a-fe114412ab72", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_ID)));
        assertEquals("2.0", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_VERSION)));
        //note this value is modified from whats in the XML message
        assertEquals("2006-07-17T22:26:40.000Z", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_ISSUE_INSTANT)));
        assertEquals("urn:idmanagement.gov:icam:bae:v2:1:7000:0000", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_DESTINATION)));
        assertEquals("uri:consent", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_CONSENT)));
        assertEquals("urn:idmanagement.gov:icam:bae:v2:1:2100:1700", context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER)));

        final List<Element> attrElemenets = (List<Element>) context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_ATTRIBUTES));
        assertEquals(3, attrElemenets.size());

    }

    @Test
    public void testNoOptionalValues() throws Exception {

        final Message request = new Message(XmlUtil.parse(validRequestWithNoOptionalValues));
        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setRequireIssuer(false);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);

        final VariableMetadata[] variablesSet = assertion.getVariablesSet();
        final List<String> map = Functions.map(Arrays.asList(variablesSet), new Functions.Unary<String, VariableMetadata>() {
            @Override
            public String call(VariableMetadata variableMetadata) {
                return variableMetadata.getName();
            }
        });
        final Map<String,Object> variableMap = context.getVariableMap(map.toArray(new String[map.size()]), new TestAudit());

        // all optional values should be null
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ATTRIBUTES)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_NAME_QUALIFIER)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_NAME_QUALIFIER)));
        //SamlProtocolConstants.SUFFIX_SUBJECT_FORMAT - has a default value so is never null.
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_PROVIDED_ID)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_DESTINATION)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_CONSENT)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_NAME_QUALIFIER)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_SP_NAME_QUALIFIER)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_FORMAT)));
        assertNull("Should be null", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ISSUER_SP_PROVIDED_ID)));

        // all non optional values should be set.
        assertEquals("Invalid value found", "aaf23196-1773-2113-474a-fe114412ab72", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ID)));
        assertEquals("Invalid value found", "2.0", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_VERSION)));
        assertEquals("Invalid value found", SamlConstants.NAMEIDENTIFIER_UNSPECIFIED, variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT)));
        //note time is modified for milli seconds.
        assertEquals("Invalid value found", "2006-07-17T22:26:40.000Z", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_ISSUE_INSTANT)));
        assertEquals("Invalid value found", "70001234000002110000000000000000", variableMap.get(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT)));

    }

    @Test
    public void testSuccessCase_ConfigValueMatched_Destination() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        final String configDestValue = "urn:idmanagement.gov:icam:bae:v2:1:7000:0000";
        assertion.setDestination(configDestValue);
        assertion.setCustomSubjectFormats("urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n");
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);
        assertEquals(configDestValue, context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_DESTINATION)));
    }

    @Test
    public void testErrorCase_ConfigValueNotMatched_Destination() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        final String configDestValue = "http://a.different.destination";
        assertion.setDestination(configDestValue);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        try {
            serverAssertion.checkRequest(context);
            fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            assertEquals("Incorrect status", AssertionStatus.FALSIFIED, e.getAssertionStatus());
        }
    }

    @Test
    public void testSuccessCase_ConfigValueMatched_NameFormat() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        final String configNameFormatValue = "urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n";

        assertion.setCustomSubjectFormats(configNameFormatValue);
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);
        assertEquals(configNameFormatValue, context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT)));
    }

    @Test
    public void testErrorCase_ConfigValueNotMatched_NameFormat() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithAllOptionalValuesIncludingAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        final String configNameFormatValue = "urn:unsupported:nameformat";
        assertion.setCustomSubjectFormats(configNameFormatValue);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        try {
            serverAssertion.checkRequest(context);
            fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            assertEquals("Incorrect status", AssertionStatus.FALSIFIED, e.getAssertionStatus());
        }
    }

    @Test
    public void testSuccessCase_ConfigValueMatched_Subject_Format_NotSupplied() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithMissingOptionalValues));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);
        final Object variable = context.getVariable(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT));
        assertEquals("Default value should be unspecified", SamlConstants.NAMEIDENTIFIER_UNSPECIFIED, variable.toString());

        //set to require NameFormat and assertion should fail
        assertion.setRequireSubjectFormat(true);

        try {
            serverAssertion.checkRequest(context);
            fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            // pass
        }
    }

    @Test
    public void testSuccessCase_ConfigValueMatched_Attribute_NameFormat_NotSupplied() throws Exception {
        Message request = new Message(XmlUtil.parse(validRequestWithMissingOptionalValues));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals("Status should be NONE", AssertionStatus.NONE, assertionStatus);

        //set to require NameFormat and assertion should fail
        assertion.setRequireAttributeNameFormat(true);

        try {
            serverAssertion.checkRequest(context);
            fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            // pass
        }
    }

    private String prefix(@NotNull final String variableName) {
        return DEFAULT_PREFIX + "." + variableName;
    }

    private PolicyEnforcementContext getContext(Message request) throws IOException {
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.setMethod("POST");  //set to make soap encapsulated test work
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private final static String validRequestWithAllOptionalValuesIncludingAttributes = "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Consent=\"uri:consent\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      Format=\"urn:idmanagement.gov:icam:bae:v2:SAML:2.0:nameid-format:fasc-n\">\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "       NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private final static String validRequestWithMissingOptionalValues = "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Destination=\"urn:idmanagement.gov:icam:bae:v2:1:7000:0000\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Issuer>urn:idmanagement.gov:icam:bae:v2:1:2100:1700</saml:Issuer>\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      >\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "     >\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "     >\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
            "     >\n" +
            "  </saml:Attribute>\n" +
            "</samlp:AttributeQuery>";

    private final static String validRequestWithNoOptionalValues = "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Subject>\n" +
            "    <saml:NameID\n" +
            "      >\n" +
            "          70001234000002110000000000000000\n" +
            "    </saml:NameID>\n" +
            "  </saml:Subject>\n" +
            "</samlp:AttributeQuery>";
}
