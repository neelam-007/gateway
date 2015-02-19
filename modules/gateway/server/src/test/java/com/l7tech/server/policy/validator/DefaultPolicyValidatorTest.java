package com.l7tech.server.policy.validator;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.PolicyValidatorResult.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.assertion.xmlsec.RequireWssX509Cert;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.wsdl.SerializableWSDLLocator;
import com.l7tech.xml.soap.SoapVersion;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;

/**
 * Test the default policy assertion path validator functionality.
 *
 * @author Emil Marceta
 */
public class DefaultPolicyValidatorTest {
    private ApplicationContext spring;

    @Before
    public void setUp() throws Exception {
        this.spring = ApplicationContexts.getTestApplicationContext();
    }

    private PolicyValidationContext pvc(PolicyType type, SerializableWSDLLocator wsdlLocator, boolean soap, SoapVersion soapVersion) {
        return new PolicyValidationContext(type, null, null, wsdlLocator, soap, soapVersion);
    }

    /**
     * Test public access warning
     *
     * @throws Exception
     */
    @Test
    public void testPublicAccessWarning() throws Exception {
        HttpRoutingAssertion httpRoutingAssertion = new HttpRoutingAssertion();
        httpRoutingAssertion.setProtectedServiceUrl("http://wheel");
        List<Assertion> kids =
          Arrays.asList( new SslAssertion(),
                  new HttpBasic(),
                  httpRoutingAssertion );

        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        final PublishedService service = getBogusService();
        PolicyValidatorResult result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        List<Message> messages = result.messages(httpRoutingAssertion);
        assertTrueWithMessages("Expected errors/warnings for the " + HttpRoutingAssertion.class + " assertion, got 0", !messages.isEmpty(), messages);

        kids =
          Arrays.asList( new SslAssertion(),
                  new HttpBasic(),
                  new SpecificUser(),
                  httpRoutingAssertion );
        aa = new AllAssertion();
        aa.setChildren(kids);
        result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        messages = result.messages(httpRoutingAssertion);
        assertTrueWithMessages("Expected no errors/warnings.", messages.isEmpty(), messages);
    }

    private PolicyValidator getValidator() {
        return (PolicyValidator) spring.getBean("defaultPolicyValidator");
    }

    private PublishedService getBogusService() throws Exception {
        PublishedService output = new PublishedService();
        output.setSoap(true);
        // set wsdl
        Document wsdl = TestDocuments.getTestDocument(TestDocuments.WSDL_DOC_LITERAL3);
        output.setWsdlXml( XmlUtil.nodeToFormattedString(wsdl) );
        return output;
    }

    /**
     * Test credential source missing error/warning tests
     *
     * @throws Exception
     */
    @Test
    public void testCredentialSourceWarning() throws Exception {
        HttpRoutingAssertion httpRoutingAssertion = new HttpRoutingAssertion();
        httpRoutingAssertion.setProtectedServiceUrl("http://wheel");
        SpecificUser specificUser = new SpecificUser();
        List<Assertion> kids =
          Arrays.<Assertion>asList( specificUser,
                  httpRoutingAssertion );

        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        final PublishedService service = getBogusService();
        PolicyValidatorResult result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        List<Message> messages = result.messages(specificUser);
        assertTrue("Expected errors/warnings for the " + HttpRoutingAssertion.class + " assertion, got 0 messages.", !messages.isEmpty());

        RequireWssX509Cert xs = new RequireWssX509Cert();
        kids =
          Arrays.<Assertion>asList( xs,
                  specificUser,
                  httpRoutingAssertion );

        aa = new AllAssertion();
        aa.setChildren(kids);
        dfpv = getValidator();
        result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        messages = result.messages(specificUser);
        assertTrueWithMessages("Expected no errors/warnings.", messages.isEmpty(), messages);
    }

    /**
     * Test partial xml request after route
     *
     * @throws Exception
     */
    @Test
    public void testPartialXmlRequestSecurityAfterRoute() throws Exception {
        RequireWssX509Cert xs = new RequireWssX509Cert();
        final List<Assertion> kids =
          Arrays.asList( new SslAssertion(),
                  new HttpBasic(),
                  new SpecificUser(),
                  new HttpRoutingAssertion(),
                  xs );
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        final PublishedService service = getBogusService();
        PolicyValidatorResult result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        List<Message> messages = result.messages(xs);
        assertTrueWithMessages("Expected errors/warnings for the " + RequireWssSignedElement.class + " assertion, got 0", !messages.isEmpty(), messages);
    }

    /**
     * Test partial xml request before route
     *
     * @throws Exception
     */
    @Test
    public void testPartialXmlRequestSecurity() throws Exception {
        RequireWssSignedElement xs = new RequireWssSignedElement();
        final List<Assertion> kids =
          Arrays.asList( new SslAssertion(),
                  new RequireWssX509Cert(),
                  new SpecificUser(),
                  xs,
                  new HttpRoutingAssertion() );
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        final PublishedService service = getBogusService();
        PolicyValidatorResult result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        List<Message> messages = result.messages(xs);
        assertTrueWithMessages("Expected no errors/warnings", messages.isEmpty(), messages);
    }


    /**
     * Test http client certificste policy combinations
     *
     * @throws Exception
     */
    @Test
    public void testHttpClientCert() throws Exception {
        HttpRoutingAssertion httpRoutingAssertion = new HttpRoutingAssertion();
        httpRoutingAssertion.setProtectedServiceUrl("http://wheel");

        List<Assertion> kids =
          Arrays.asList( new SslAssertion(),
                  new SslAssertion(true),
                  new SpecificUser(),
                  httpRoutingAssertion );
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        final PublishedService service = getBogusService();
        PolicyValidatorResult result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        List<Message> messages = result.getMessages();
        assertTrueWithMessages("Expected no errors/warnings", messages.isEmpty(), messages);

        SslAssertion clientCert = new SslAssertion(true);
        kids =
          Arrays.asList( clientCert,
                  new SpecificUser(),
                  httpRoutingAssertion );
        aa = new AllAssertion();
        aa.setChildren(kids);
        result = dfpv.validate(aa, pvc(PolicyType.PRIVATE_SERVICE, service.wsdlLocator(), service.isSoap(), service.getSoapVersion()), new TestLicenseManager());
        messages = result.messages(clientCert);
        assertTrueWithMessages("Expected no errors/warnings", messages.isEmpty(), messages);
    }

    private static void assertTrueWithMessages(String msg, boolean expression, List<Message> messages) {
        StringBuilder sb = new StringBuilder();
        for ( final Message message : messages ) {
            sb.append( message.getMessage() ).append( "\n" );
        }
        if (sb.length() > 0) {
            msg += "\n" + sb.toString();
        }
        assertTrue(msg, expression);
    }
}
