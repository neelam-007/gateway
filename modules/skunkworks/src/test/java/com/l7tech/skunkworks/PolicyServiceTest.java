/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.skunkworks;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.identity.UserBean;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.proxy.datamodel.Policy;
import com.l7tech.proxy.datamodel.exceptions.BadCredentialsException;
import com.l7tech.proxy.util.PolicyServiceClient;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.security.xml.SecurityTokenResolver;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.TestDefaultKey;
import com.l7tech.server.identity.TestIdentityProvider;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyService;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.filter.FilterManager;
import com.l7tech.server.secureconversation.InboundSecureConversationContextManager;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.util.MockConfig;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.xpath.XpathExpression;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit tests for PolicyService.
 */
public class PolicyServiceTest {
    private static final Logger log = Logger.getLogger(PolicyServiceTest.class.getName());
    private static ApplicationContext applicationContext;

    private static final String TESTUSER_UID = "666";
    private static final String TESTUSER_LOGIN = "franco";
    private static final String TESTUSER_PASSWD = "password";
    private static final String BAD_PASSWD = "drowssap";
    private static final SpecificUser TESTUSER_IDASSERTION = new SpecificUser(TestIdentityProvider.PROVIDER_ID,
                                                                 TESTUSER_LOGIN,
                                                                 TESTUSER_UID,
                                                                 TESTUSER_LOGIN);
    private static final LoginCredentials NONE = LoginCredentials.makeLoginCredentials(new HttpBasicToken("", new char[0]), HttpBasic.class);

    @BeforeClass
    public static void init() {
        // Set to true to ensure that all "addressing" elements in the request are signed
        SyspropUtil.setProperty( "com.l7tech.server.policy.policyServiceFullSecurityChecks", "true" );

        applicationContext =  ApplicationContexts.getTestApplicationContext();

        UserBean francoBean = new UserBean();
        francoBean.setName(TESTUSER_LOGIN);
        francoBean.setLogin(TESTUSER_LOGIN);
        francoBean.setUniqueIdentifier(TESTUSER_UID);
        francoBean.setProviderId(TestIdentityProvider.PROVIDER_ID);
        TestIdentityProvider.addUser(francoBean, TESTUSER_LOGIN, TESTUSER_PASSWD.toCharArray());

        UserBean johnSmith = new UserBean();
        johnSmith.setName("John Smith");
        johnSmith.setLogin("John Smith");
        johnSmith.setUniqueIdentifier("112313418");
        johnSmith.setProviderId(TestIdentityProvider.PROVIDER_ID);
        TestIdentityProvider.addUser(johnSmith, "John Smith", TESTUSER_PASSWD.toCharArray(), "C=US, ST=California, L=Cupertino, O=IBM, OU=Java Technology Center, CN=John Smith");
    }

    @AfterClass
    public static void cleanupSystemProperties() {
        SyspropUtil.clearProperties(
            "com.l7tech.server.policy.policyServiceFullSecurityChecks"
        );
    }

    private PolicyEnforcementContext getPolicyRequestContext( final LoginCredentials loginCredentials ) throws GeneralSecurityException, IOException {
        Document requestDoc;
        Message request = new Message();
        Message response = new Message();

        final MockHttpServletRequest hrequest = new MockHttpServletRequest();
        hrequest.setMethod("POST");
        if ( loginCredentials != null ) hrequest.setSecure( true );
        final MockHttpServletResponse hresponse = new MockHttpServletResponse();
        final HttpServletRequestKnob httpRequestKnob = new HttpServletRequestKnob(hrequest);
        final HttpServletResponseKnob httpResponseKnob = new HttpServletResponseKnob(hresponse);
        request.attachHttpRequestKnob(httpRequestKnob);
        request.attachHttpResponseKnob(httpResponseKnob);
        response.attachHttpRequestKnob(httpRequestKnob);
        response.attachHttpResponseKnob(httpResponseKnob);

        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response, true);
        if (loginCredentials != null) {
            if (loginCredentials != NONE ) context.getDefaultAuthenticationContext().addCredentials(loginCredentials);
            requestDoc = PolicyServiceClient.createGetPolicyRequest("123");
        }
        else {
            requestDoc = PolicyServiceClient.createDecoratedGetPolicyRequest("123", null, TestDocuments.getEttkClientCertificate(), TestDocuments.getEttkClientPrivateKey(), null);
        }
        assertNotNull(requestDoc);
        log.info("Request (pretty-printed): " + XmlUtil.nodeToFormattedString(requestDoc));

        request.initialize(requestDoc);
        return context;
    }

    private Document getPolicyResponse(final Assertion policyToTest, PolicyEnforcementContext context) throws Exception {
        PolicyService ps = new PolicyService(
                new MockConfig(new Properties()),
                new TestDefaultKey(),
                (ServerPolicyFactory) applicationContext.getBean("policyFactory"),
                (FilterManager) applicationContext.getBean("policyFilterManager"),
                (SecurityTokenResolver) applicationContext.getBean("securityTokenResolver"),
                (PolicyPathBuilderFactory) applicationContext.getBean("policyPathBuilderFactory"),
                applicationContext.getBean("inboundSecureConversationContextManager", InboundSecureConversationContextManager.class) );
        ps.setApplicationContext(applicationContext);
        PolicyService.PolicyGetter policyGetter = new PolicyService.PolicyGetter() {
            @Override
            public PolicyService.ServiceInfo getPolicy(String serviceId) {
                return new PolicyService.ServiceInfo() {

                    @Override
                    public Assertion getPolicy() {
                        return policyToTest;
                    }

                    @Override
                    public String getVersion() {
                        return "1";
                    }
                };
            }
        };

        ps.respondToPolicyDownloadRequest(context, true, policyGetter);
        if (context.getPolicyResult() == AssertionStatus.AUTH_FAILED) {
            throw new BadCredentialsException(context.getPolicyResult().toString());
        } else {
            Document response = context.getResponse().getXmlKnob().getDocumentReadOnly();
            assertNotNull(response);
            log.info("Response (pretty-printed):" + XmlUtil.nodeToFormattedString(response));
            return response;
        }
    }

    private void testPolicy(final Assertion policyToTest, final LoginCredentials loginCredentials) throws Exception {
        final PolicyEnforcementContext context = getPolicyRequestContext(loginCredentials);
        Message request = context.getRequest();
        Document response = getPolicyResponse(policyToTest, context);
        Policy policy = parsePolicyResponse(request.getXmlKnob().getDocumentReadOnly(), response);
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
        LoginCredentials francoCreds = LoginCredentials.makeLoginCredentials(
                new HttpBasicToken(TESTUSER_LOGIN, TESTUSER_PASSWD.toCharArray()), HttpBasic.class);
        testPolicy(policyToTest, francoCreds);
    }

    private void testWithInvalidCredentials(final Assertion policyToTest) throws Exception {
        LoginCredentials francoCreds = LoginCredentials.makeLoginCredentials(
                new HttpBasicToken(TESTUSER_LOGIN, BAD_PASSWD.toCharArray()), HttpBasic.class);
        testPolicy(policyToTest, francoCreds);

    }

    @Test
    public void testSimplePolicyService() throws Exception {
        testPolicy(new TrueAssertion(), NONE);
    }

    @Test
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

    @Test
    public void testWithIdentitiesSigned() throws Exception {
        AllAssertion root = new AllAssertion();
        root.addChild(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);
        or.addChild(TESTUSER_IDASSERTION);
        or.addChild(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "John Smith", "112313418", "John Smith"));
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testPolicy(root, null);
    }

    @Test
    public void testWithTwoBranchForTwoIdentities() throws Exception {
        AllAssertion root = new AllAssertion();
        root.addChild(new HttpBasic());
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);

        AllAssertion branch1 = new AllAssertion();
        branch1.addChild(TESTUSER_IDASSERTION);
        branch1.addChild(new RequireWssSignedElement(new XpathExpression("/pathForFranco")));
        branch1.setChildren(branch1.getChildren());

        AllAssertion branch2 = new AllAssertion();
        branch2.addChild(new SpecificUser(TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        branch2.addChild(new RequireWssSignedElement(new XpathExpression("/pathForMike")));
        branch2.setChildren(branch2.getChildren());

        or.addChild(branch1);
        or.addChild(branch2);
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testWithValidCredentials(root);
    }

    @Test
    public void testAnonymousBranch() throws Exception {
        AllAssertion root = new AllAssertion();
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);

        AllAssertion branch1 = new AllAssertion();
        branch1.addChild(new RequestXpathAssertion(new XpathExpression("/anonymousPath")));
        branch1.addChild(new WssSignElement());
        branch1.setChildren(branch1.getChildren());

        AllAssertion branch2 = new AllAssertion();
        branch2.addChild(new HttpBasic());
        branch2.addChild(new SpecificUser( TestIdentityProvider.PROVIDER_ID, "mike", "111", "mike"));
        branch2.addChild(new RequestXpathAssertion(new XpathExpression("/pathForMikeOnly")));
        branch2.addChild(new WssSignElement());
        branch2.setChildren(branch2.getChildren());

        or.addChild(branch1);
        or.addChild(branch2);
        
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testPolicy(root, NONE);
    }

    @Test
    public void testFullAnonymous() throws Exception {
        AllAssertion root = new AllAssertion();
        OneOrMoreAssertion or = new OneOrMoreAssertion();
        root.addChild(or);

        AllAssertion branch1 = new AllAssertion();
        branch1.addChild(new RequestXpathAssertion(new XpathExpression("/anonymousPath")));
        branch1.addChild(new WssSignElement());
        branch1.setChildren(branch1.getChildren());

        or.addChild(branch1);
        
        root.addChild(new HttpRoutingAssertion("http://soap.spacecrocodile.com"));
        root.setChildren(root.getChildren());
        or.setChildren(or.getChildren());
        log.info(root.toString());

        testPolicy(root, NONE);
    }

    @Test
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

    @Test
    public void testReplayedPolicyResponse() throws Exception {
        final PolicyEnforcementContext context = getPolicyRequestContext(null);
        Document firstResponse = getPolicyResponse(new TrueAssertion(), context);
        Policy firstPolicy = parsePolicyResponse(context.getRequest().getXmlKnob().getDocumentReadOnly(), firstResponse);
        assertNotNull(firstPolicy);

        // So far so good.  Now let's try a replay
        Message secondRequest = getPolicyRequestContext(null).getRequest();
        try {
            parsePolicyResponse(secondRequest.getXmlKnob().getDocumentReadOnly(), firstResponse);
            fail("The replayed policy response should have been rejected by the client.");
        } catch (InvalidDocumentFormatException e) {
            // Ok
            log.info("The correct exception was thrown: " + e);
        }
    }
}
