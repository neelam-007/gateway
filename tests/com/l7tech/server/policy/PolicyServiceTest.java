/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.message.TestSoapRequest;
import com.l7tech.message.TestSoapResponse;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.proxy.util.PolicyServiceClient;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.identity.TestIdentityProvider;
import com.l7tech.identity.UserBean;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.w3c.dom.Document;

import java.util.logging.Logger;

/**
 * Unit tests for PolicyService.
 */
public class PolicyServiceTest extends TestCase {
    private static Logger log = Logger.getLogger(PolicyServiceTest.class.getName());

    public PolicyServiceTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(PolicyServiceTest.class);
    }

    protected void setUp() throws Exception {
        System.setProperty("com.l7tech.common.locator", "com.l7tech.common.locator.TestLocator");
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    private void testPolicy(final Assertion policyToTest, LoginCredentials loginCredentials) throws Exception {
        Document request = null;
        if (loginCredentials != null) {
            request = PolicyServiceClient.createGetPolicyRequest("123");
        }
        else {
            request = PolicyServiceClient.createGetPolicyRequest("123",
                                                                 TestDocuments.getEttkClientCertificate(),
                                                                 TestDocuments.getEttkClientPrivateKey());
        }
        assertNotNull(request);
        log.info("Request (pretty-printed): " + XmlUtil.nodeToFormattedString(request));

        SoapRequest soapReq = new TestSoapRequest(request);
        // todo
        // Mike, you can't do this because those credentials will simply be overwritten by
        // the assertion that processes the signature you are creating in PolicyServiceClient.createGetPolicyRequest
        //
        if (loginCredentials != null) {
            soapReq.setPrincipalCredentials(loginCredentials);
        }
        SoapResponse soapRes = new TestSoapResponse();

        PolicyService ps = new PolicyService(TestDocuments.getDotNetServerPrivateKey(),
                                             TestDocuments.getDotNetServerCertificate());
        PolicyService.PolicyGetter policyGetter = new PolicyService.PolicyGetter() {
            public PolicyService.ServiceInfo getPolicy(String serviceId) {
                return new PolicyService.ServiceInfo() {

                    public Assertion getPolicy() {
                        return policyToTest;
                    }

                    public String getVersion() {
                        return "666";
                    }
                };
            }
        };

        ps.respondToPolicyDownloadRequest(soapReq, soapRes, policyGetter);
        Document response = soapRes.getDocument();
        assertNotNull(response);
        log.info("Response (pretty-printed:" + XmlUtil.nodeToFormattedString(response));

        Policy policy = PolicyServiceClient.parseGetPolicyResponse(response, TestDocuments.getDotNetServerCertificate());
        assertNotNull(policy);

        log.info("Returned policy version: " + policy.getVersion());
        log.info("Returned policy: " + policy.getAssertion());
    }

    public void testSimplePolicyService() throws Exception {
        testPolicy(new TrueAssertion(), null);
    }

    public void testWithIdentities() throws Exception {
        UserBean francoBean = new UserBean();
        francoBean.setName("franco");
        francoBean.setLogin("franco");
        francoBean.setUniqueIdentifier("666");
        francoBean.setProviderId(TestIdentityProvider.PROVIDER_ID);
        TestIdentityProvider.addUser(francoBean, "franco", "password".toCharArray());

        AllAssertion root = new AllAssertion();
        root.getChildren().add(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.getChildren().add(or);
        or.getChildren().add(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "franco", "666", "franco"));
        root.getChildren().add(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        LoginCredentials francoCreds = new LoginCredentials("franco", "password".toCharArray(),
                                                            CredentialFormat.CLEARTEXT,
                                                            HttpBasic.class);
        testPolicy(root, francoCreds);
    }
}
