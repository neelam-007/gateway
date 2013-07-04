package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.message.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.ext.*;
import com.l7tech.policy.assertion.ext.message.*;
import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetable;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.policy.custom.CustomAssertionsPolicyTestBase;
import com.l7tech.util.HexUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.security.AccessControlException;
import java.security.GeneralSecurityException;
import java.util.*;

import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.w3c.dom.Document;

import javax.security.auth.login.FailedLoginException;
import javax.xml.parsers.ParserConfigurationException;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Test ServerCustomAssertionHolder
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerCustomAssertionHolderTest extends CustomAssertionsPolicyTestBase
{
    static private final String SAMPLE_XML_INPUT_MESSAGE = "<?xml version=\"1.0\" encoding=\"utf-8\"?><a><b>1</b><c>2</c></a>";
    static private final String SAMPLE_XML_OUT_MESSAGE = "<test>output message</test>";

    @Before
    public void setUp() throws Exception
    {
        // call base init
        doInit();

        // mock getBean to return appropriate mock classes for policyFactory
        when(mockApplicationContext.getBean("policyFactory", ServerPolicyFactory.class)).thenReturn(serverPolicyFactory);
        when(mockApplicationContext.getBean("policyFactory")).thenReturn(serverPolicyFactory);

        // mock getBean to return appropriate stashManagerFactory used for HardcodedResponseAssertion
        final StashManagerFactory stashManagerFactory = new StashManagerFactory() {
            @Override
            public StashManager createStashManager() {
                return new ByteArrayStashManager();
            }
        };
        when(mockApplicationContext.getBean("stashManagerFactory")).thenReturn(stashManagerFactory);
        when(mockApplicationContext.getBean("stashManagerFactory", StashManagerFactory.class)).thenReturn(stashManagerFactory);

        // Register needed assertions here
        assertionRegistry.registerAssertion(SetVariableAssertion.class);
        assertionRegistry.registerAssertion(HardcodedResponseAssertion.class);
    }

    @Test
    public void testNullStatusReturn() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = (ServerCustomAssertionHolder)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        doReturn(null).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());
        final AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_onRequest_For_IOException_And_GeneralSecurityException() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(customAssertionHolder, responseAssertion));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final CustomAssertion customAssertion = serviceInvocation.getCustomAssertion();
        assertFalse("CustomAssertion is of type CustomMessageTargetable", customAssertion instanceof CustomMessageTargetable);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when policy is after routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        //noinspection deprecation
        doThrow(IOException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);

        //noinspection deprecation
        doThrow(GeneralSecurityException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(FailedLoginException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.AUTH_FAILED, status);

        //noinspection deprecation
        doThrow(AccessControlException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(ParserConfigurationException.class).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any()); // throw some other exception
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void test_onResponse_For_IOException_And_GeneralSecurityException() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.ACCESS_CONTROL);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(responseAssertion, customAssertionHolder));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final CustomAssertion customAssertion = serviceInvocation.getCustomAssertion();
        assertFalse("CustomAssertion is of type CustomMessageTargetable", customAssertion instanceof CustomMessageTargetable);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when policy is after routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());

        //noinspection deprecation
        doThrow(IOException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);

        //noinspection deprecation
        doThrow(GeneralSecurityException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(FailedLoginException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.AUTH_FAILED, status);

        //noinspection deprecation
        doThrow(AccessControlException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.UNAUTHORIZED, status);

        //noinspection deprecation
        doThrow(ParserConfigurationException.class).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any()); // throw some other exception
        status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, status);
    }

    @Test
    public void testLegacyBeforeRoute() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_OUT_MESSAGE.getBytes()));

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(customAssertionHolder, responseAssertion));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final CustomAssertion customAssertion = serviceInvocation.getCustomAssertion();
        assertFalse("CustomAssertion is of type CustomMessageTargetable", customAssertion instanceof CustomMessageTargetable);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest)param1;

                final Document inputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                inputDoc.normalizeDocument();
                final Document reqDoc = request.getDocument();
                assertNotNull("Document not NULL", reqDoc);
                reqDoc.normalizeDocument();
                assertTrue("Source Target Message Document is same as TargetMessage document", inputDoc.isEqualNode(reqDoc));

                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when policy is before routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE)), new Message());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        // just to be persistent make sure response is properly set
        final Document outDoc = XmlUtil.stringToDocument(SAMPLE_XML_OUT_MESSAGE);
        outDoc.normalizeDocument();
        final Document resDoc = context.getResponse().getXmlKnob().getDocumentReadOnly();
        assertNotNull("Response is not NULL", resDoc);
        resDoc.normalizeDocument();
        assertTrue("Source Target Message Document is same as TargetMessage document", outDoc.isEqualNode(resDoc));
    }

    @Test
    public void testLegacyAfterRoute() throws Exception {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_INPUT_MESSAGE.getBytes()));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategory(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final Assertion ass = makePolicy(Arrays.<Assertion>asList(responseAssertion, customAssertionHolder));
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertNotNull("CustomAssertionHolder cannot be null.", customAssertionHolder);
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        final CustomAssertion customAssertion = serviceInvocation.getCustomAssertion();
        assertFalse("CustomAssertion is of type CustomMessageTargetable", customAssertion instanceof CustomMessageTargetable);

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when policy is after routing assertion!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onResponse", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceResponse", param1 instanceof ServiceResponse);
                ServiceResponse response = (ServiceResponse)param1;

                final Document inputDoc = XmlUtil.stringToDocument(SAMPLE_XML_INPUT_MESSAGE);
                inputDoc.normalizeDocument();
                final Document resDoc = response.getDocument();
                assertNotNull("Document not NULL", resDoc);
                resDoc.normalizeDocument();
                assertTrue("Source Target Message Document is same as TargetMessage document", inputDoc.isEqualNode(resDoc));

                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        final PolicyEnforcementContext context = makeContext(new Message(), new Message());
        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

}