/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.identity.TestIdentityProvider;
import com.l7tech.identity.UserBean;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.SoapResponse;
import com.l7tech.message.TestSoapRequest;
import com.l7tech.message.TestSoapResponse;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.util.PolicyServiceClient;
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
        UserBean francoBean = new UserBean();
        francoBean.setName(TESTUSER_LOGIN);
        francoBean.setLogin(TESTUSER_LOGIN);
        francoBean.setUniqueIdentifier(TESTUSER_UID);
        francoBean.setProviderId(TestIdentityProvider.PROVIDER_ID);
        TestIdentityProvider.addUser(francoBean, TESTUSER_LOGIN, TESTUSER_PASSWD.toCharArray());
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
            request = PolicyServiceClient.createSignedGetPolicyRequest("123",
                                                                 TestDocuments.getEttkClientCertificate(),
                                                                 TestDocuments.getEttkClientPrivateKey());
        }
        assertNotNull(request);
        log.info("Request (pretty-printed): " + XmlUtil.nodeToFormattedString(request));

        SoapRequest soapReq = new TestSoapRequest(request);
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
                        return "1";
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

    private void testwithValidIdentity(final Assertion policyToTest) throws Exception {

        LoginCredentials francoCreds = new LoginCredentials(TESTUSER_LOGIN, TESTUSER_PASSWD.toCharArray(),
                                                            CredentialFormat.CLEARTEXT,
                                                            HttpBasic.class);
        testPolicy(policyToTest, francoCreds);
    }

    public void testSimplePolicyService() throws Exception {
        testPolicy(new TrueAssertion(), null);
    }

    public void testWithIdentities() throws Exception {
        AllAssertion root = new AllAssertion();
        root.getChildren().add(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.getChildren().add(or);
        or.getChildren().add(TESTUSER_IDASSERTION);
        or.getChildren().add(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        root.getChildren().add(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testwithValidIdentity(root);
    }

    private static final String TESTUSER_UID = "666";
    private static final String TESTUSER_LOGIN = "franco";
    private static final String TESTUSER_PASSWD = "password";
    private SpecificUser TESTUSER_IDASSERTION = new SpecificUser(TestIdentityProvider.PROVIDER_ID,
                                                                 TESTUSER_LOGIN,
                                                                 TESTUSER_UID,
                                                                 TESTUSER_LOGIN);
}
