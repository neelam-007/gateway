/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.validator;

import com.l7tech.common.security.xml.ElementSecurity;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.http.HttpClientCert;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlResponseSecurity;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import javax.xml.soap.SOAPConstants;
import java.util.*;

/**
 * Test the default policy assertion path validator functionality.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class DefaultPolicyValidatorTest extends TestCase {

    public DefaultPolicyValidatorTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(DefaultPolicyValidatorTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
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
        PolicyValidator dfpv = PolicyValidator.getDefault();
        PolicyValidatorResult result = dfpv.validate(aa);
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
        result = dfpv.validate(aa);
        messages = result.messages(httpRoutingAssertion);
        assertTrue("Expected no errors/warnings.", messages.isEmpty(), messages);
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
        PolicyValidator dfpv = PolicyValidator.getDefault();
        PolicyValidatorResult result = dfpv.validate(aa);
        List messages = result.messages(specificUser);
        assertTrue("Expected errors/warnings for the " + HttpRoutingAssertion.class + " assertion, got 0 messages.", !messages.isEmpty());

        XmlRequestSecurity xs = new XmlRequestSecurity();
        xs.setElements(new ElementSecurity[]{getAuthenticationElementSecurity()});
        kids =
          Arrays.asList(new Assertion[]{
              xs,
              specificUser,
              httpRoutingAssertion
          });

        aa = new AllAssertion();
        aa.setChildren(kids);
        dfpv = PolicyValidator.getDefault();
        result = dfpv.validate(aa);
        messages = result.messages(specificUser);
        assertTrue("Expected no errors/warnings.", messages.isEmpty(), messages);
    }

    /**
     * Test partial xml request after route
     *
     * @throws Exception
     */
    public void testPartialXmlRequestSecurityAfterRoute() throws Exception {
        XmlRequestSecurity xs = new XmlRequestSecurity();
        xs.setElements(getTestPartialElementSecurity());
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
        PolicyValidator dfpv = PolicyValidator.getDefault();
        PolicyValidatorResult result = dfpv.validate(aa);
        List messages = result.messages(xs);
        assertTrue("Expected errors/warnings for the " + XmlRequestSecurity.class + " assertion, got 0", !messages.isEmpty(), messages);
    }

    /**
     * Test partial xml request before route
     *
     * @throws Exception
     */
    public void testPartialXmlRequestSecurity() throws Exception {
        XmlRequestSecurity xs = new XmlRequestSecurity();
        xs.setElements(getTestPartialElementSecurity());
        final List kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new HttpBasic(),
              new SpecificUser(),
              xs,
              new HttpRoutingAssertion()
          });
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = PolicyValidator.getDefault();
        PolicyValidatorResult result = dfpv.validate(aa);
        List messages = result.messages(xs);
        assertTrue("Expected no errors/warnings", messages.isEmpty(), messages);
    }


    /**
     * Test xml response after route
     *
     * @throws Exception
     */
    public void testPartialXmlResponseSecurityBeforeRoute() throws Exception {
        XmlResponseSecurity xs = new XmlResponseSecurity();
        xs.setElements(getTestPartialElementSecurity());
        final List kids =
          Arrays.asList(new Assertion[]{
              new SslAssertion(),
              new HttpBasic(),
              new SpecificUser(),
              xs,
              new HttpRoutingAssertion()
          });
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = PolicyValidator.getDefault();
        PolicyValidatorResult result = dfpv.validate(aa);
        List messages = result.messages(xs);
        assertTrue("Expected errors/warnings for the " + XmlRequestSecurity.class + " assertion, got 0", !messages.isEmpty());
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
              new HttpClientCert(),
              new SpecificUser(),
              httpRoutingAssertion
          });
        AllAssertion aa = new AllAssertion();
        aa.setChildren(kids);
        PolicyValidator dfpv = PolicyValidator.getDefault();
        PolicyValidatorResult result = dfpv.validate(aa);
        List messages = result.getMessages();
        assertTrue("Expected no errors/warnings", messages.isEmpty(), messages);

        HttpClientCert httpClientCert = new HttpClientCert();
        kids =
          Arrays.asList(new Assertion[]{
              httpClientCert,
              new SpecificUser(),
              httpRoutingAssertion
          });
        aa = new AllAssertion();
        aa.setChildren(kids);
        result = dfpv.validate(aa);
        messages = result.messages(httpClientCert);
        assertTrue("Expected errors/warnings for the " + HttpClientCert.class + " assertion, got 0", !messages.isEmpty());
    }


    /**
     * @return test element security
     */
    private ElementSecurity[] getTestPartialElementSecurity() {
        Map nm = new HashMap();
        nm.put("soapenv", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        nm.put("impl", "http://warehouse.acme.com/ws");

        return new ElementSecurity[]{
            new ElementSecurity(new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/accountid", nm),
              new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder", nm),
              false,
              "AES", 128),

            new ElementSecurity(new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/price", nm),
              new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/placeOrder", nm),
              true,
              "AES", 128),

            new ElementSecurity(new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/amount", nm),
              new XpathExpression("/soapenv:Envelope/soapenv:Body/impl:placeOrder/placeOrder", nm),
              true,
              "AES", 128),
        };
    }

    private ElementSecurity getAuthenticationElementSecurity() {
        Map namespaces = new HashMap();
        namespaces.put("soapenv", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        namespaces.put("SOAP-ENV", SOAPConstants.URI_NS_SOAP_ENVELOPE);
        XpathExpression xpathExpression = new XpathExpression(SoapUtil.SOAP_ENVELOPE_XPATH, namespaces);
        return new ElementSecurity(xpathExpression, null, false, ElementSecurity.DEFAULT_CIPHER, ElementSecurity.DEFAULT_KEYBITS);
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