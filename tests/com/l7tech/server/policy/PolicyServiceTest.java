/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.security.Keys;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.xml.TestDocuments;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.identity.TestIdentityProvider;
import com.l7tech.identity.UserBean;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.TrueAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.util.PolicyServiceClient;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.filter.FilterManager;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.logging.Logger;

/**
 * Unit tests for PolicyService.
 */
public class PolicyServiceTest extends TestCase {
    private static Logger log = Logger.getLogger(PolicyServiceTest.class.getName());
    static ApplicationContext applicationContext = null;

    public PolicyServiceTest(String name) {
        super(name);
    }

    public static Test suite() {
         final TestSuite suite = new TestSuite(PolicyServiceTest.class);
         TestSetup wrapper = new TestSetup(suite) {

             protected void setUp() throws Exception {
                 Keys.createTestSsgKeystoreProperties();
                 applicationContext = createApplicationContext();
             }

             protected void tearDown() throws Exception {
                 ;
             }

             private ApplicationContext createApplicationContext() {
                 return ApplicationContexts.getTestApplicationContext();
             }
         };
         return wrapper;
    }

    protected void setUp() throws Exception {
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

    private PolicyEnforcementContext getPolicyRequestContext(LoginCredentials loginCredentials) throws GeneralSecurityException, IOException {
        Document requestDoc = null;
        Message request = new Message();
        Message response = new Message();
        PolicyEnforcementContext context = new PolicyEnforcementContext(request, response);
        context.setCredentials(loginCredentials);
        if (loginCredentials != null) {
            requestDoc = PolicyServiceClient.createGetPolicyRequest("123");
        }
        else {
            requestDoc = PolicyServiceClient.createSignedGetPolicyRequest("123",
                                                                 TestDocuments.getEttkClientCertificate(),
                                                                 TestDocuments.getEttkClientPrivateKey(),
                                                                 null);
        }
        assertNotNull(requestDoc);
        log.info("Request (pretty-printed): " + XmlUtil.nodeToFormattedString(requestDoc));

        request.initialize(requestDoc);
        return context;
    }

    private Document getPolicyResponse(final Assertion policyToTest, PolicyEnforcementContext context) throws Exception {
        PolicyService ps = new PolicyService(TestDocuments.getDotNetServerPrivateKey(),
                                             TestDocuments.getDotNetServerCertificate(),
                                             (ServerPolicyFactory)applicationContext.getBean("policyFactory"),
          (FilterManager)applicationContext.getBean("policyFilterManager"));
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

        ps.respondToPolicyDownloadRequest(context, true, policyGetter);
        Document response = context.getResponse().getXmlKnob().getDocument(false);
        assertNotNull(response);
        log.info("Response (pretty-printed):" + XmlUtil.nodeToFormattedString(response));
        return response;
    }

    private void testPolicy(final Assertion policyToTest, LoginCredentials loginCredentials) throws Exception {
        final PolicyEnforcementContext context = getPolicyRequestContext(loginCredentials);
        Message request = context.getRequest();
        Document response = getPolicyResponse(policyToTest, context);
        Policy policy = parsePolicyResponse(request.getXmlKnob().getDocument(false), response);
        log.info("Returned policy version: " + policy.getVersion());
        log.info("Returned policy: " + policy.getAssertion());
    }

    private Policy parsePolicyResponse(Document originalRequest, Document response) throws Exception {
        Policy policy = PolicyServiceClient.parseGetPolicyResponse(originalRequest,
                                                                   response,
                                                                   TestDocuments.getDotNetServerCertificate(),
                                                                   null,
                                                                   null,
                                                                   false,
                                                                   null,
                                                                   null);
        assertNotNull(policy);
        return policy;
    }

    private void testWithValidCredentials(final Assertion policyToTest) throws Exception {

        LoginCredentials francoCreds = new LoginCredentials(TESTUSER_LOGIN, TESTUSER_PASSWD.toCharArray(),
                                                            CredentialFormat.CLEARTEXT,
                                                            HttpBasic.class);
        testPolicy(policyToTest, francoCreds);
    }

    private void testWithInvalidCredentials(final Assertion policyToTest) throws Exception {
        LoginCredentials francoCreds = new LoginCredentials(TESTUSER_LOGIN, BAD_PASSWD.toCharArray(),
                                                            CredentialFormat.CLEARTEXT,
                                                            HttpBasic.class);
        testPolicy(policyToTest, francoCreds);

    }

    public void testSimplePolicyService() throws Exception {
        testPolicy(new TrueAssertion(), null);
    }

    public void testWithIdentities() throws Exception {
        AllAssertion root = new AllAssertion();
        root.addChild(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);
        or.addChild(TESTUSER_IDASSERTION);
        or.addChild(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testWithValidCredentials(root);
    }

    public void testWithTwoBranchForTwoIdentities() throws Exception {
        AllAssertion root = new AllAssertion();
        root.addChild(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);

        AllAssertion branch1 = new AllAssertion();
        branch1.addChild(TESTUSER_IDASSERTION);
        branch1.addChild(new RequestWssIntegrity(new XpathExpression("/pathForFranco")));
        branch1.setChildren(branch1.getChildren());

        AllAssertion branch2 = new AllAssertion();
        branch2.addChild(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        branch2.addChild(new RequestWssIntegrity(new XpathExpression("/pathForMike")));
        branch2.setChildren(branch2.getChildren());

        or.addChild(branch1);
        or.addChild(branch2);
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testWithValidCredentials(root);
    }

    public void testAnonymousBranch() throws Exception {
        AllAssertion root = new AllAssertion();
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);

        AllAssertion branch1 = new AllAssertion();
        branch1.addChild(new RequestXpathAssertion(new XpathExpression("/anonymousPath")));
        branch1.addChild(new ResponseWssIntegrity());
        branch1.setChildren(branch1.getChildren());

        AllAssertion branch2 = new AllAssertion();
        branch2.addChild(new HttpBasic());
        branch2.addChild(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        branch2.addChild(new RequestXpathAssertion(new XpathExpression("/pathForMikeOnly")));
        branch2.addChild(new ResponseWssIntegrity());
        branch2.setChildren(branch2.getChildren());

        or.addChild(branch1);
        or.addChild(branch2);
        
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testPolicy(root, null);
    }

    public void testFullAnonymous() throws Exception {
        AllAssertion root = new AllAssertion();
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);

        AllAssertion branch1 = new AllAssertion();
        branch1.addChild(new RequestXpathAssertion(new XpathExpression("/anonymousPath")));
        branch1.addChild(new ResponseWssIntegrity());
        branch1.setChildren(branch1.getChildren());

        or.addChild(branch1);
        
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testPolicy(root, null);
    }

    public void testFailedAuthentication() throws Exception {
        AllAssertion root = new AllAssertion();
        root.addChild(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);
        or.addChild(TESTUSER_IDASSERTION);
        or.addChild(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        try {
            testWithInvalidCredentials(root);
            fail("Request should have failed due to invalid credentials.");
        } catch (BadCredentialsException e) {
            log.info("The correct exception was thrown: " + e);
        }
    }

    public void testReplayedPolicyResponse() throws Exception {
        final PolicyEnforcementContext context = getPolicyRequestContext(null);
        Document firstResponse = getPolicyResponse(new TrueAssertion(), context);
        Policy firstPolicy = parsePolicyResponse(context.getRequest().getXmlKnob().getDocument(false), firstResponse);
        assertNotNull(firstPolicy);

        // So far so good.  Now let's try a replay
        Message secondRequest = getPolicyRequestContext(null).getRequest();
        try {
            parsePolicyResponse(secondRequest.getXmlKnob().getDocument(false), firstResponse);
            fail("The replayed policy response should have been rejected by the client.");
        } catch (InvalidDocumentFormatException e) {
            // Ok
            log.info("The correct exception was thrown: " + e);
        }
    }

    private static final String TESTUSER_UID = "666";
    private static final String TESTUSER_LOGIN = "franco";
    private static final String TESTUSER_PASSWD = "password";
    private static final String BAD_PASSWD = "drowssap";
    private SpecificUser TESTUSER_IDASSERTION = new SpecificUser(TestIdentityProvider.PROVIDER_ID,
                                                                 TESTUSER_LOGIN,
                                                                 TESTUSER_UID,
                                                                 TESTUSER_LOGIN);
}
