package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.common.TestKeys;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.samlpassertion.SamlpRequestBuilderAssertion;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.security.saml.NameIdentifierInclusionType;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.security.xml.XmlElementEncryptionConfig;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyException;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Pair;
import com.l7tech.xml.DomElementCursor;
import com.l7tech.xml.ElementCursor;
import com.l7tech.xml.xpath.XpathExpression;
import com.l7tech.xml.xpath.XpathResult;
import com.l7tech.xml.xpath.XpathResultIterator;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.w3c.dom.Element;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"JavaDoc"})
public class ServerSamlpRequestBuilderAssertionTest {

    /**
     * Test that for version 2 we can encrypt the generated NameID. Note Version 1 does not support encrypted elements.
     * @throws Exception
     */
    @Test
    public void testEncryptedID_Version2() throws Exception {
        runEncryptUseCase(new Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext>() {
            @Override
            public Void call(XmlElementEncryptionConfig xmlElementEncryptionConfig, PolicyEnforcementContext policyEnforcementContext) {
                Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
                try {
                    xmlElementEncryptionConfig.setRecipientCertificateBase64(HexUtils.encodeBase64(k.left.getEncoded(), true));
                } catch (CertificateEncodingException e) {
                    Assert.fail("Unexpected Exception: " + e.getMessage());
                }

                return null;
            }
        });
    }

    /**
     * Validate the EncryptedID can be generated via a cert from a context variable
     */
    @BugNumber(11666)
    @Test
    public void testEncryptIdViaCertInContextVariable() throws Exception {
        runEncryptUseCase(new Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext>() {
            @Override
            public Void call(XmlElementEncryptionConfig xmlElementEncryptionConfig, PolicyEnforcementContext policyEnforcementContext) {
                Pair<X509Certificate, PrivateKey> k = TestKeys.getCertAndKey("RSA_1024");
                final String certVarName = "cert";
                xmlElementEncryptionConfig.setRecipientCertContextVariableName(certVarName);

                policyEnforcementContext.setVariable(certVarName, k.left);

                return null;
            }
        });
    }

    private void runEncryptUseCase(Functions.Binary<Void, XmlElementEncryptionConfig, PolicyEnforcementContext> configCallBack) throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        // Absolute minimum configuration to avoid NPE's etc.
        assertion.setSamlVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setNameIdentifierValue("test");
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        // Require encrypted
        assertion.setEncryptNameIdentifier(true);
        final XmlElementEncryptionConfig config = assertion.getXmlEncryptConfig();

        final PolicyEnforcementContext context = getContext();

        configCallBack.call(config, context);

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        Assert.assertEquals("Unexpected status", AssertionStatus.NONE, assertionStatus);

        final Message samlpRequest = (Message) context.getVariable(assertion.getOtherTargetMessageVariable());
        final Element documentElement = samlpRequest.getXmlKnob().getDocumentReadOnly().getDocumentElement();

        System.out.println(XmlUtil.nodeToFormattedString(documentElement));

        String xPath = "/soapenv:Envelope/soapenv:Body/samlp2:AttributeQuery/saml2:Subject/saml2:EncryptedID";

        final Map<String, String> prefixToNamespace = new HashMap<String, String>();
        prefixToNamespace.put(SamlConstants.NS_SAML2_PREFIX, SamlConstants.NS_SAML2);
        prefixToNamespace.put(SamlConstants.NS_SAMLP2_PREFIX, SamlConstants.NS_SAMLP2);
        prefixToNamespace.put("soapenv", "http://schemas.xmlsoap.org/soap/envelope/");

        final ElementCursor cursor = new DomElementCursor(documentElement);

        XpathResult xpathResult = cursor.getXpathResult(new XpathExpression(xPath, prefixToNamespace).compile());
        XpathResultIterator xpathResultSetIterator = xpathResult.getNodeSet().getIterator();

        //first element should be the auth token
        final Element encryptedID = xpathResultSetIterator.nextElementAsCursor().asDomElement();
        Assert.assertEquals("Wrong element found", "EncryptedID", encryptedID.getLocalName());
    }

    /**
     * It's possible to incorrectly configure the encryption settings in the UI - by simply not configuring them.
     *
     * Tests that when no context variable is defined that the server assertion cannot be created.
     */
    @Test(expected = ServerPolicyException.class)
    public void testInvalidEncryptionConfiguration() throws Exception {
        final SamlpRequestBuilderAssertion assertion = new SamlpRequestBuilderAssertion();
        // Absolute minimum configuration to avoid NPE's etc.
        assertion.setSamlVersion(2);
        assertion.setSoapVersion(1);
        assertion.setNameIdentifierType(NameIdentifierInclusionType.SPECIFIED);
        assertion.setNameIdentifierValue("test");
        assertion.setAttributeStatement(new SamlAttributeStatement());
        assertion.setRequestId(SamlpRequestConstants.SAMLP_REQUEST_ID_GENERATE);

        // Require encrypted - do not configure encryption settings for test
        assertion.setEncryptNameIdentifier(true);

        ServerSamlpRequestBuilderAssertion serverAssertion =
                new ServerSamlpRequestBuilderAssertion(assertion, ApplicationContexts.getTestApplicationContext());

        final PolicyEnforcementContext context = getContext();
        serverAssertion.checkRequest(context);
    }

    private PolicyEnforcementContext getContext() throws IOException {

        Message request = new Message();
        Message response = new Message();

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        MockHttpServletResponse hresponse = new MockHttpServletResponse();

        PolicyEnforcementContext policyEnforcementContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);

        request.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        response.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));

        return policyEnforcementContext;

    }

}
