package com.l7tech.policy.validator;

import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;

import junit.framework.TestCase;

import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import java.io.IOException;

/**
 * User: vchan
 */
public class RequestWssConfidentialityValidatorTest  extends TestCase {

    private ApplicationContext appCtx;
    private WspReader policyReader;

    static final String ERROR_MSG_STRING = RequestWssConfidentialityValidator.WARN_WSS_RECIPIENT_NOT_LOCAL;

    // test policy instances
    static final String TEST_WITH_CERT = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:RequestWssConfidentiality><L7p:RecipientContext xmlSecurityRecipientContext=\"included\"><L7p:Actor stringValue=\"Test-Repro\"/><L7p:Base64edX509Certificate stringValue=\"MIIDODCCAqGgAwIBAgIDCMO3MA0GCSqGSIb3DQEBBQUAME4xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFcXVpZmF4MS0wKwYDVQQLEyRFcXVpZmF4IFNlY3VyZSBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMDgwMjIxMDE1MDA1WhcNMTAwMzIzMDA1MDA1WjCBwjELMAkGA1UEBhMCQ0ExGDAWBgNVBAoTD21haWwubDd0ZWNoLmNvbTETMBEGA1UECxMKR1Q1NDYxNTgyMDExMC8GA1UECxMoU2VlIHd3dy5nZW90cnVzdC5jb20vcmVzb3VyY2VzL2NwcyAoYykwODE3MDUGA1UECxMuRG9tYWluIENvbnRyb2wgVmFsaWRhdGVkIC0gUXVpY2tTU0wgUHJlbWl1bShSKTEYMBYGA1UEAxMPbWFpbC5sN3RlY2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCl+Px+nfW+OrjaQG3jvqlRXoX6NzPpuu4MM/EmmSvoEvKjjnDiGH7coerAbp+FR7LK6KEbjA+iyQWj6GBU0dYBZXqi4+MvIsSKhev0ISomMMY6/Q29w8Yvj6GXQ0R3jpqCWvZvrP/cmUFFCqDTpd5VsCNDUmXLNN9C8aLGFl7kYQIDAQABo4GuMIGrMA4GA1UdDwEB/wQEAwIE8DAdBgNVHQ4EFgQUAmP3Vf+/Rt9A2uJMa47D06vQMBkwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL2NybC5nZW90cnVzdC5jb20vY3Jscy9zZWN1cmVjYS5jcmwwHwYDVR0jBBgwFoAUSOZo+SvSspXXR9gjIBBPM5iQn9QwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA0GCSqGSIb3DQEBBQUAA4GBACseYF6/XHAXX2/2VhmnhhP31QkGE5JV5iqxnaY63jH8l+9K42EWBd983VjJgbWFh8KSdlS+NWsObfmuCp1M/1TeIUitHBphTlBkwMqePj1wnaln94KmEOgX6UDEOpv4DpE2ZucC99GyijfjDCjhWQv7QFxzYykNIXBUuCb3TH/E\"/></L7p:RecipientContext><L7p:XEncAlgorithmList stringListValue=\"included\"><L7p:item stringValue=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/></L7p:XEncAlgorithmList><L7p:XpathExpression xpathExpressionValue=\"included\"><L7p:Expression stringValue=\"/soapenv:Envelope/soapenv:Body/tns:getQuote/symbol\"/><L7p:Namespaces mapValue=\"included\"><L7p:entry><L7p:key stringValue=\"soapenv\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"SOAP-ENV\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"ns1\"/><L7p:value stringValue=\"urn:xmltoday-delayed-quotes2\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"wsp\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"tns\"/><L7p:value stringValue=\"urn:xmltoday-delayed-quotes\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"L7p\"/><L7p:value stringValue=\"http://www.layer7tech.com/ws/policy\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"xsd\"/><L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"xsi\"/><L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/></L7p:entry></L7p:Namespaces></L7p:XpathExpression></L7p:RequestWssConfidentiality><L7p:HttpRoutingAssertion><L7p:ProtectedServiceUrl stringValue=\"http://bones.l7tech.com:80/result.xml\"/><L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\"><L7p:Rules httpPassthroughRules=\"included\"><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"Cookie\"/></L7p:item><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"SOAPAction\"/></L7p:item></L7p:Rules></L7p:RequestHeaderRules><L7p:RequestParamRules httpPassthroughRuleSet=\"included\"><L7p:ForwardAll booleanValue=\"true\"/><L7p:Rules httpPassthroughRules=\"included\"/></L7p:RequestParamRules><L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\"><L7p:Rules httpPassthroughRules=\"included\"><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"Set-Cookie\"/></L7p:item></L7p:Rules></L7p:ResponseHeaderRules></L7p:HttpRoutingAssertion></wsp:All></wsp:Policy>";
    static final String TEST_WITH_CERT2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:RequestWssConfidentiality><L7p:RecipientContext xmlSecurityRecipientContext=\"included\"><L7p:Actor stringValue=\"SomeSSL\"/><L7p:Base64edX509Certificate stringValue=\"MIIDODCCAqGgAwIBAgIDCMO3MA0GCSqGSIb3DQEBBQUAME4xCzAJBgNVBAYTAlVTMRAwDgYDVQQKEwdFcXVpZmF4MS0wKwYDVQQLEyRFcXVpZmF4IFNlY3VyZSBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwHhcNMDgwMjIxMDE1MDA1WhcNMTAwMzIzMDA1MDA1WjCBwjELMAkGA1UEBhMCQ0ExGDAWBgNVBAoTD21haWwubDd0ZWNoLmNvbTETMBEGA1UECxMKR1Q1NDYxNTgyMDExMC8GA1UECxMoU2VlIHd3dy5nZW90cnVzdC5jb20vcmVzb3VyY2VzL2NwcyAoYykwODE3MDUGA1UECxMuRG9tYWluIENvbnRyb2wgVmFsaWRhdGVkIC0gUXVpY2tTU0wgUHJlbWl1bShSKTEYMBYGA1UEAxMPbWFpbC5sN3RlY2guY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCl+Px+nfW+OrjaQG3jvqlRXoX6NzPpuu4MM/EmmSvoEvKjjnDiGH7coerAbp+FR7LK6KEbjA+iyQWj6GBU0dYBZXqi4+MvIsSKhev0ISomMMY6/Q29w8Yvj6GXQ0R3jpqCWvZvrP/cmUFFCqDTpd5VsCNDUmXLNN9C8aLGFl7kYQIDAQABo4GuMIGrMA4GA1UdDwEB/wQEAwIE8DAdBgNVHQ4EFgQUAmP3Vf+/Rt9A2uJMa47D06vQMBkwOgYDVR0fBDMwMTAvoC2gK4YpaHR0cDovL2NybC5nZW90cnVzdC5jb20vY3Jscy9zZWN1cmVjYS5jcmwwHwYDVR0jBBgwFoAUSOZo+SvSspXXR9gjIBBPM5iQn9QwHQYDVR0lBBYwFAYIKwYBBQUHAwEGCCsGAQUFBwMCMA0GCSqGSIb3DQEBBQUAA4GBACseYF6/XHAXX2/2VhmnhhP31QkGE5JV5iqxnaY63jH8l+9K42EWBd983VjJgbWFh8KSdlS+NWsObfmuCp1M/1TeIUitHBphTlBkwMqePj1wnaln94KmEOgX6UDEOpv4DpE2ZucC99GyijfjDCjhWQv7QFxzYykNIXBUuCb3TH/E\"/></L7p:RecipientContext><L7p:XEncAlgorithmList stringListValue=\"included\"><L7p:item stringValue=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/></L7p:XEncAlgorithmList><L7p:XpathExpression xpathExpressionValue=\"included\"><L7p:Expression stringValue=\"/soapenv:Envelope/soapenv:Body/tns:getQuote/symbol\"/><L7p:Namespaces mapValue=\"included\"><L7p:entry><L7p:key stringValue=\"soapenv\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"SOAP-ENV\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"ns1\"/><L7p:value stringValue=\"urn:xmltoday-delayed-quotes2\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"wsp\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"tns\"/><L7p:value stringValue=\"urn:xmltoday-delayed-quotes\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"L7p\"/><L7p:value stringValue=\"http://www.layer7tech.com/ws/policy\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"xsd\"/><L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"xsi\"/><L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/></L7p:entry></L7p:Namespaces></L7p:XpathExpression></L7p:RequestWssConfidentiality><L7p:HttpRoutingAssertion><L7p:ProtectedServiceUrl stringValue=\"http://bones.l7tech.com:80/result.xml\"/><L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\"><L7p:Rules httpPassthroughRules=\"included\"><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"Cookie\"/></L7p:item><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"SOAPAction\"/></L7p:item></L7p:Rules></L7p:RequestHeaderRules><L7p:RequestParamRules httpPassthroughRuleSet=\"included\"><L7p:ForwardAll booleanValue=\"true\"/><L7p:Rules httpPassthroughRules=\"included\"/></L7p:RequestParamRules><L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\"><L7p:Rules httpPassthroughRules=\"included\"><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"Set-Cookie\"/></L7p:item></L7p:Rules></L7p:ResponseHeaderRules></L7p:HttpRoutingAssertion></wsp:All></wsp:Policy>";
    static final String TEST_DEFAULT = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"><wsp:All wsp:Usage=\"Required\"><L7p:RequestWssConfidentiality><L7p:XEncAlgorithmList stringListValue=\"included\"><L7p:item stringValue=\"http://www.w3.org/2001/04/xmlenc#aes128-cbc\"/></L7p:XEncAlgorithmList><L7p:XpathExpression xpathExpressionValue=\"included\"><L7p:Expression stringValue=\"/soapenv:Envelope/soapenv:Body/tns:getQuote/symbol\"/><L7p:Namespaces mapValue=\"included\"><L7p:entry><L7p:key stringValue=\"soapenv\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"SOAP-ENV\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"ns1\"/><L7p:value stringValue=\"urn:xmltoday-delayed-quotes2\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"wsp\"/><L7p:value stringValue=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"tns\"/><L7p:value stringValue=\"urn:xmltoday-delayed-quotes\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"L7p\"/><L7p:value stringValue=\"http://www.layer7tech.com/ws/policy\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"xsd\"/><L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema\"/></L7p:entry><L7p:entry><L7p:key stringValue=\"xsi\"/><L7p:value stringValue=\"http://www.w3.org/2001/XMLSchema-instance\"/></L7p:entry></L7p:Namespaces></L7p:XpathExpression></L7p:RequestWssConfidentiality><L7p:HttpRoutingAssertion><L7p:ProtectedServiceUrl stringValue=\"http://bones.l7tech.com:80/result.xml\"/><L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\"><L7p:Rules httpPassthroughRules=\"included\"><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"Cookie\"/></L7p:item><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"SOAPAction\"/></L7p:item></L7p:Rules></L7p:RequestHeaderRules><L7p:RequestParamRules httpPassthroughRuleSet=\"included\"><L7p:ForwardAll booleanValue=\"true\"/><L7p:Rules httpPassthroughRules=\"included\"/></L7p:RequestParamRules><L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\"><L7p:Rules httpPassthroughRules=\"included\"><L7p:item httpPassthroughRule=\"included\"><L7p:Name stringValue=\"Set-Cookie\"/></L7p:item></L7p:Rules></L7p:ResponseHeaderRules></L7p:HttpRoutingAssertion></wsp:All></wsp:Policy>";

    protected void setUp() throws Exception {

        // get the spring app context
        if (appCtx == null) {

            appCtx = ApplicationContexts.getTestApplicationContext();
        }

        // grab the wspReader bean from spring
        if (policyReader == null) {

            policyReader = (WspReader) appCtx.getBean("wspReader", WspReader.class);
            if (policyReader == null) {

                throw new Exception("Fail - Unable to obtain the WspReader bean from the application context.");
            }
        }

    }

    /**
     * Validate a RequestWssConfidentiality assertion with the WSSRecipient set to "Default".
     * No errors/warning messages should be returned by the validator.
     */
    public void testWithLocalRecipient() {

        RequestWssConfidentiality wssConfAssert = null;
        try {
            wssConfAssert = parseAssertion(TEST_DEFAULT);
            assertNotNull(wssConfAssert);

            // run validator
            RequestWssConfidentialityValidator val = new RequestWssConfidentialityValidator(wssConfAssert);
            PolicyValidatorResult result = new PolicyValidatorResult();
            val.validate(new AssertionPath(wssConfAssert), testWsdl(), true, result);

            // no error/warnings are expected
            assertEquals(0, result.getErrorCount());
            assertEquals(0, result.getWarningCount());

        } catch (Exception ex) {
            fail("Unexpected failure during test (testAssertionWithoutCert). " + ex);
        }
    }

    /**
     * Validate a RequestWssConfidentiality assertion with the WSSRecipient set to another source.
     * A warning should be returned by the validator.
     */
    public void testWithOtherRecipient() {

        RequestWssConfidentiality wssConfAssert = null;
        try {
            wssConfAssert = parseAssertion(TEST_WITH_CERT2);
            assertNotNull(wssConfAssert);

            // run validator
            RequestWssConfidentialityValidator val = new RequestWssConfidentialityValidator(wssConfAssert);
            PolicyValidatorResult result = new PolicyValidatorResult();
            val.validate(new AssertionPath(wssConfAssert), testWsdl(), true, result);

            // no error/warnings are expected
            assertEquals(0, result.getErrorCount());
            assertEquals(1, result.getWarningCount());
            assertEquals(ERROR_MSG_STRING, result.getWarnings().get(0).getMessage());

        } catch (Exception ex) {
            fail("Unexpected failure during test (testAssertionWithCert). " + ex);
        }
    }


    private RequestWssConfidentiality parseAssertion(String policyXml) throws IOException {

        Assertion as = policyReader.parsePermissively(policyXml);
        assertNotNull(as);
        assertTrue(as instanceof AllAssertion);
        AllAssertion all = (AllAssertion) as;

        for (Object obj : all.getChildren()) {

            if (obj instanceof RequestWssConfidentiality) {
                return RequestWssConfidentiality.class.cast(obj);
            }
        }
        return null;
    }


    private Wsdl testWsdl() throws IOException, SAXException, WSDLException {

        PublishedService output = new PublishedService();
        output.setSoap(true);
        // set wsdl
        Document wsdl = TestDocuments.getTestDocument(TestDocuments.WSDL_STOCK_QUOTE);
        output.setWsdlXml(XmlUtil.nodeToFormattedString(wsdl));
        return output.parsedWsdl();
    }
}
