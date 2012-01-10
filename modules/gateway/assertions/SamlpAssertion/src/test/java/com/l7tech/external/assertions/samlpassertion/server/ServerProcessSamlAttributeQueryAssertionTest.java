package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.SimpleSecurityTokenResolver;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.*;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.l7tech.external.assertions.samlpassertion.ProcessSamlAttributeQueryRequestAssertion.*;
import static org.junit.Assert.*;

@SuppressWarnings({"JavaDoc"})
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
        final List<String> map = getSetVariableNames(variablesSet);
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

    private List<String> getSetVariableNames(VariableMetadata[] variablesSet) {
        return Functions.map(Arrays.asList(variablesSet), new Functions.Unary<String, VariableMetadata>() {
            @Override
            public String call(VariableMetadata variableMetadata) {
                return variableMetadata.getName();
            }
        });
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

    @Test(expected = AssertionStatusException.class)
    public void testErrorCase_DuplicateAttributes() throws Exception {
        Message request = new Message(XmlUtil.parse(requestWithDuplicateAttributes));

        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setVerifyAttributesAreUnique(true);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        serverAssertion.checkRequest(context);
    }

    @Test
    public void testSuccessCase_DecryptEncryptedNameIdentifier() throws Exception{
        final Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
        final String nameIdValue = "Protected Name Identifier Value";
        //Generate an AttributeQuery with an encrypted name id
        final Message samlpRequest;
        {
            final SamlpRequestBuilderAssertion requestBuilderAssertion = new SamlpRequestBuilderAssertion();
            requestBuilderAssertion.setSamlVersion(2);
            requestBuilderAssertion.setSoapVersion(1);
            requestBuilderAssertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
            requestBuilderAssertion.setNameIdentifierValue(nameIdValue);
            requestBuilderAssertion.setAttributeStatement(new SamlAttributeStatement());
            requestBuilderAssertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);
            requestBuilderAssertion.setEncryptNameIdentifier(true);
            final XmlElementEncryptionConfig config = requestBuilderAssertion.getXmlEncryptConfig();
            config.setRecipientCertificateBase64(HexUtils.encodeBase64(k.left.getEncoded(), true));
            ServerSamlpRequestBuilderAssertion requestBuilderServerAssertion =
                    new ServerSamlpRequestBuilderAssertion(requestBuilderAssertion, ApplicationContexts.getTestApplicationContext());

            final PolicyEnforcementContext createRequestContext = getContext(null);
            final AssertionStatus assertionStatus = requestBuilderServerAssertion.checkRequest(createRequestContext);
            Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);
            samlpRequest = (Message) createRequestContext.getVariable(requestBuilderAssertion.getOtherTargetMessageVariable());
            final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();
            System.out.println(XmlUtil.nodeToFormattedString(documentElement));
        }

        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setSoapEncapsulated(true);
        assertion.setAllowEncryptedId(true);
        assertion.setDecryptEncryptedId(true);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        SimpleSecurityTokenResolver simpleResolver = new SimpleSecurityTokenResolver(k.left, k.right);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("securityTokenResolver", simpleResolver)
                .put( "auditFactory", new TestAudit().factory() )
                .unmodifiableMap()
        );

        final PolicyEnforcementContext processAttributeQueryContext = getContext(samlpRequest);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(processAttributeQueryContext);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message processAttributeQueryMessage = (Message) processAttributeQueryContext.getVariable(assertion.getTargetName());
        final Element documentElement = processAttributeQueryMessage.getXmlKnob().getDocumentReadOnly().getDocumentElement();
        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        // Validate element was decrypted correctly and that the EncryptedID has been replaced by a NameID

        final String nameIdString = "saml2:NameID";
        final String baseXpath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Subject/ ";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(baseXpath + nameIdString, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        // Verify NameID found where expected
        final Element nameID = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        Assert.assertEquals("Wrong element found", "NameID", nameID.getLocalName());
        final String textValue = DomUtils.getTextValue(nameID);
        Assert.assertEquals("Invalid value found for NameID post decryption", nameIdValue, textValue );

        // Verify the EncryptedID element was not found.
        final String encryptedIdString = "saml2:EncryptedID";
        xpathResult = cursor.getXpathResult(new XpathExpression(baseXpath + encryptedIdString, prefixToNamespace).compile());
        xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        // Verify NameID found where expected
        Assert.assertFalse("EncryptedID element should not have been found", xpathResultSetIterator.hasNext());
    }

    /**
     * If no NameID or EncryptedID were found, then the assertion will fail.
     */
    @Test
    public void testErrorCase_NoSubjectElementAvailable() throws Exception {
        // Missing Subject NameID and Encrypted ID
        Message request = new Message(XmlUtil.parse(invalidRequestMissingSubjectNameIdentifier));
        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setRequireIssuer(false);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("securityTokenResolver", new SimpleSecurityTokenResolver())
                .put( "auditFactory", testAudit.factory() )
                .unmodifiableMap()
        );

        try {
            serverAssertion.checkRequest(context);
            fail("Assertion should have thrown");
        } catch (AssertionStatusException e) {
            //success
        }

        // valid audits
        Assert.assertTrue(testAudit.isAuditPresent(AssertionMessages.SAMLP_ATTRIBUTE_QUERY_INVALID));
        Assert.assertTrue(testAudit.isAuditPresentContaining("Missing Subject NameID or EncryptedID"));
    }

    /**
     * If an EncryptedID was found and decryption was not configured, then all the subject context variables will be
     * null and the assertion will succeed.
     */
    @Test
    public void testSuccessCase_EncryptedIdNotDecrypted_SubjectVariablesSet() throws Exception {
        // Missing Subject NameID and Encrypted ID
        Message request = new Message(XmlUtil.parse(validRequestWithEncryptedId));
        final PolicyEnforcementContext context = getContext(request);
        final ProcessSamlAttributeQueryRequestAssertion assertion = new ProcessSamlAttributeQueryRequestAssertion();
        assertion.setRequireIssuer(false);
        assertion.setAllowEncryptedId(true);
        assertion.setDecryptEncryptedId(false);
        assertion.setAttributeNameFormats(SamlConstants.ATTRIBUTE_NAME_FORMAT_BASIC);

        final ServerProcessSamlAttributeQueryRequestAssertion serverAssertion =
                new ServerProcessSamlAttributeQueryRequestAssertion(assertion);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final VariableMetadata[] variablesSet = assertion.getVariablesSet();
        final List<String> map = getSetVariableNames(variablesSet);
        final Map<String,Object> variableMap = context.getVariableMap(map.toArray(new String[map.size()]), new TestAudit());

        // validate all subject variables were set
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_NAME_QUALIFIER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_NAME_QUALIFIER)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_FORMAT)));
        assertTrue(variableMap.containsKey(prefix(ProtocolRequestUtilities.SUFFIX_SUBJECT_SP_PROVIDED_ID)));
    }

    private String prefix(@NotNull final String variableName) {
        return DEFAULT_PREFIX + "." + variableName;
    }

    private PolicyEnforcementContext getContext(Message request) throws IOException {
        Message response = new Message();
        if (request == null) {
            request = new Message();
        }

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

    private final static String requestWithDuplicateAttributes = "<samlp:AttributeQuery\n" +
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
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     >\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonGivenName\"\n" +
            "      NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\">\n" +
            "     >\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonMiddleName\"\n" +
            "     >\n" +
            "  </saml:Attribute>\n" +
            "  <saml:Attribute Name=\"nc:PersonSurName\"\n" +
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

    private final static String invalidRequestMissingSubjectNameIdentifier = "<samlp:AttributeQuery\n" +
            "  xmlns:saml=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "  xmlns:samlp=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "  ID=\"aaf23196-1773-2113-474a-fe114412ab72\"\n" +
            "  Version=\"2.0\"\n" +
            "  IssueInstant=\"2006-07-17T22:26:40Z\">\n" +
            "  <saml:Subject>\n" +
            "  </saml:Subject>\n" +
            "</samlp:AttributeQuery>";

    private final static String validRequestWithEncryptedId = "<samlp2:AttributeQuery ID=\"samlp2-bcfa816253f573f1d6ae4b123d553f96\" IssueInstant=\"2012-01-06T15:06:44.702-08:00\"\n" +
            "                       Version=\"2.0\" xmlns:ac=\"urn:oasis:names:tc:SAML:2.0:ac\"\n" +
            "                       xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\" xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\"\n" +
            "                       xmlns:samlp2=\"urn:oasis:names:tc:SAML:2.0:protocol\"\n" +
            "                       xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\" +\n" +
            "    \"<saml2:Issuer>CN=irishman2.l7tech.local</saml2:Issuer>\" +\n" +
            "    \"\n" +
            "    <saml2:Subject>\n" +
            "        <saml2:EncryptedID>\n" +
            "            <xenc:EncryptedData xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "                <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/>\n" +
            "                <ds:KeyInfo>\n" +
            "                    <xenc:EncryptedKey xmlns:xenc=\"http://www.w3.org/2001/04/xmlenc#\">\n" +
            "                        <xenc:EncryptionMethod Algorithm=\"http://www.w3.org/2001/04/xmlenc#rsa-1_5\"/>\n" +
            "                        <dsig:KeyInfo xmlns:dsig=\"http://www.w3.org/2000/09/xmldsig#\">\n" +
            "                            <dsig:X509Data>\n" +
            "                                <dsig:X509IssuerSerial>\n" +
            "                                    <dsig:X509IssuerName>CN=irishman2.l7tech.local</dsig:X509IssuerName>\n" +
            "                                    <dsig:X509SerialNumber>10141945054223567498</dsig:X509SerialNumber>\n" +
            "                                </dsig:X509IssuerSerial>\n" +
            "                            </dsig:X509Data>\n" +
            "                        </dsig:KeyInfo>\n" +
            "                        <xenc:CipherData>\n" +
            "                            <xenc:CipherValue>\n" +
            "                                I/lYPBF2A1nOWixDoIm0aDpC5KeTjJr1bCfbywwr0TqwsRSeSi4pLurz0dU4a4SN2jALpmfFG6Pv8wfbdsOx0urtW7KVrg8Ttq+dp0oAAG0WGvOxIJulzLE+CuwVva6rER0FFPoPje/W7MViGyZ9sC9J+NzDnhwj5fzMykRkU9tskTo+UUUsheMVTGM5KCnaSlZXP5376kQ7KKebufs7mPYEEXDcdeFDb3iD/1RwrWJtp0nD38uaBKu6gtCheXgwUU+NIkI925PZ8+K3VAoasz/lR3XbyGZh5dp65o8gJMxctbY5Nw42iEEWs3Yl0er9bDAdQDRhK2m06hxqdhdj7w==\n" +
            "                            </xenc:CipherValue>\n" +
            "                        </xenc:CipherData>\n" +
            "                    </xenc:EncryptedKey>\n" +
            "                </ds:KeyInfo>\n" +
            "                <xenc:CipherData>\n" +
            "                    <xenc:CipherValue>\n" +
            "                        jdm9bZdyJivg/OvqoDgWUL5Oc/9/sYQXcThCbihWb+TqCe+MlNI+3ULBZ3cmWds+T9on9Vn1046Pnj64YDTRRdPuGp0FEWC91+vzhkzba0ywRAmag9yFRcwwdlK13AXl6A4i7jKKNpDTP8rOeh6zFg==\n" +
            "                    </xenc:CipherValue>\n" +
            "                </xenc:CipherData>\n" +
            "            </xenc:EncryptedData>\n" +
            "        </saml2:EncryptedID>\n" +
            "    </saml2:Subject>\n" +
            "    <saml2:Attribute Name=\"givenName\" NameFormat=\"urn:oasis:names:tc:SAML:2.0:attrname-format:basic\"/>\n" +
            "</samlp2:AttributeQuery>";

}
