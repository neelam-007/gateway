/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */

package com.l7tech.server.policy.validator;

import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.TestDocuments;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Test the default policy assertion path validator functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyValidatorTest extends TestCase {
    private ApplicationContext spring;

    public DefaultPolicyValidatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultPolicyValidatorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    @Override
    protected void setUp() throws Exception {
        this.spring = ApplicationContexts.getTestApplicationContext();
    }

    /**
     * Test public access warning
     *
     * @throws Exception
     */
    public void testPublicAccessWarning() throws Exception {
        HttpRoutingAssertion httpRoutingAssertion = new HttpRoutingAssertion();
        httpRoutingAssertion.setProtectedServiceUrl("http://wheel");
        List kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new HttpBasic(),
              httpRoutingAssertion
          });

        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        PolicyValidatorResult result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        List messages = result.messages(httpRoutingAssertion);
        assertTrue("Expected errors/warnings for the " + HttpRoutingAssertion.class + " assertion, got 0", !messages.isEmpty(), messages);

        kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new HttpBasic(),
              new SpecificUser(),
              httpRoutingAssertion
          });
        aa = new AllAssertion();
        aa.setChildren(kids);
        result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        messages = result.messages(httpRoutingAssertion);
        assertTrue("Expected no errors/warnings.", messages.isEmpty(), messages);
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
    public void testCredentialSourceWarning() throws Exception {
        HttpRoutingAssertion httpRoutingAssertion = new HttpRoutingAssertion();
        httpRoutingAssertion.setProtectedServiceUrl("http://wheel");
        SpecificUser specificUser = new SpecificUser();
        List kids =
          Arrays.asList(new Assertion[]{
              specificUser,
              httpRoutingAssertion
          });

        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        PolicyValidatorResult result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        List messages = result.messages(specificUser);
        assertTrue("Expected errors/warnings for the " + HttpRoutingAssertion.class + " assertion, got 0 messages.", !messages.isEmpty());

        RequestWssX509Cert xs = new RequestWssX509Cert();
        kids =
          Arrays.asList(new Assertion[]{
              xs,
              specificUser,
              httpRoutingAssertion
          });

        aa = new AllAssertion();
        aa.setChildren(kids);
        dfpv = getValidator();
        result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        messages = result.messages(specificUser);
        assertTrue("Expected no errors/warnings.", messages.isEmpty(), messages);
    }

    /**
     * Test partial xml request after route
     *
     * @throws Exception
     */
    public void testPartialXmlRequestSecurityAfterRoute() throws Exception {
        RequestWssX509Cert xs = new RequestWssX509Cert();
        final List kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new HttpBasic(),
              new SpecificUser(),
              new HttpRoutingAssertion(),
              xs
          });
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        PolicyValidatorResult result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        List messages = result.messages(xs);
        assertTrue("Expected errors/warnings for the " + RequestWssIntegrity.class + " assertion, got 0", !messages.isEmpty(), messages);
    }

    /**
     * Test partial xml request before route
     *
     * @throws Exception
     */
    public void testPartialXmlRequestSecurity() throws Exception {
        RequestWssIntegrity xs = new RequestWssIntegrity();
        final List kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new RequestWssX509Cert(),
              new SpecificUser(),
              xs,
              new HttpRoutingAssertion()
          });
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        PolicyValidatorResult result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        List messages = result.messages(xs);
        assertTrue("Expected no errors/warnings", messages.isEmpty(), messages);
    }


    /**
     * Test http client certificste policy combinations
     *
     * @throws Exception
     */
    public void testHttpClientCert() throws Exception {
        HttpRoutingAssertion httpRoutingAssertion = new HttpRoutingAssertion();
        httpRoutingAssertion.setProtectedServiceUrl("http://wheel");

        List kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new SslAssertion(true),
              new SpecificUser(),
              httpRoutingAssertion
          });
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = getValidator();
        PolicyValidatorResult result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        List messages = result.getMessages();
        assertTrue("Expected no errors/warnings", messages.isEmpty(), messages);

        SslAssertion clientCert = new SslAssertion(true);
        kids =
          Arrays.asList(new Assertion[]{
              clientCert,
              new SpecificUser(),
              httpRoutingAssertion
          });
        aa = new AllAssertion();
        aa.setChildren(kids);
        result = dfpv.validate(aa, PolicyType.PRIVATE_SERVICE, getBogusService().parsedWsdl(), getBogusService().isSoap(), new TestLicenseManager());
        messages = result.messages(clientCert);
        assertTrue("Expected no errors/warnings", messages.isEmpty(), messages);
    }

    private static void assertTrue(String msg, boolean expression, List messages) {
        StringBuffer sb = new StringBuffer();
        for (Iterator iterator = messages.iterator(); iterator.hasNext();) {
            PolicyValidatorResult.Message message = (PolicyValidatorResult.Message)iterator.next();
            sb.append(message.getMessage()).append("\n");
        }
        if (sb.length() > 0) {
            msg += "\n" + sb.toString();
        }
        assertTrue(msg, expression);
    }
}
