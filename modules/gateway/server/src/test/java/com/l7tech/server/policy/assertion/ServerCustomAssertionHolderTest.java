package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
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
import com.l7tech.server.policy.ServiceFinderImpl;
import com.l7tech.server.policy.assertion.composite.ServerAllAssertion;
import com.l7tech.server.policy.custom.CustomAssertionsPolicyTestBase;
import com.l7tech.server.policy.custom.CustomAssertionsSampleContents;
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
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import javax.security.auth.login.FailedLoginException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponseWrapper;
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
        customAssertionHolder.setCategories(Category.ACCESS_CONTROL);
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
        customAssertionHolder.setCategories(Category.ACCESS_CONTROL);
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
        customAssertionHolder.setCategories(Category.ACCESS_CONTROL);
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
        customAssertionHolder.setCategories(Category.AUDIT_ALERT);
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
        customAssertionHolder.setCategories(Category.AUDIT_ALERT);
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

    /**
     * Simple utility function to check if two cookies of type {@link Cookie} or {@link HttpCookie} are equal.
     * The function only compares against the cookie name, value, version, path and domain,
     * since these are the only values we set in the unit-test
     *
     * @param inCookie1 the first cookie
     * @param inCookie2 the second cookie
     * @return true if <tt>cookie1</tt> equals <tt>cookie2</tt>
     */
    private <T> boolean compareCookie(T inCookie1, T inCookie2)
    {
        assertTrue("verify that input cookie pair is of type Cookie or HttpCookie",
                (inCookie1 instanceof Cookie && inCookie2 instanceof Cookie) ||
                (inCookie1 instanceof HttpCookie && inCookie2 instanceof HttpCookie)
        );

        if (inCookie1 instanceof Cookie)
        {
            Cookie cookie1 = (Cookie)inCookie1;
            Cookie cookie2 = (Cookie)inCookie2;
            //noinspection StringEquality
            return ((cookie1.getName() == cookie2.getName()) || cookie1.getName().equals(cookie2.getName())) &&
                   ((cookie1.getValue() == cookie2.getValue()) || cookie1.getValue().equals(cookie2.getValue())) &&
                    cookie1.getVersion() == cookie2.getVersion() &&
                   ((cookie1.getPath() == cookie2.getPath()) || cookie1.getPath().equals(cookie2.getPath())) &&
                   ((cookie1.getDomain() == cookie2.getDomain()) || cookie1.getDomain().equals(cookie2.getDomain()));
        }
        else
        {
            HttpCookie cookie1 = (HttpCookie)inCookie1;
            HttpCookie cookie2 = (HttpCookie)inCookie2;
            //noinspection StringEquality
            return ((cookie1.getCookieName() == cookie2.getCookieName()) || cookie1.getCookieName().equals(cookie2.getCookieName())) &&
                   ((cookie1.getCookieValue() == cookie2.getCookieValue()) || cookie1.getCookieValue().equals(cookie2.getCookieValue())) &&
                    cookie1.getVersion() == cookie2.getVersion() &&
                   ((cookie1.getPath() == cookie2.getPath()) || cookie1.getPath().equals(cookie2.getPath())) &&
                   ((cookie1.getDomain() == cookie2.getDomain()) || cookie1.getDomain().equals(cookie2.getDomain()));

        }
    }

    /**
     * Creates a sample before routing policy having a custom assertion and hardcoded response, in that order.
     * @return AllAssertion object having a custom assertion and hardcoded response as child's.
     */
    private Assertion createBeforeRoutingPolicy() {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategories(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType("text/xml; charset=UTF-8");
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(SAMPLE_XML_OUT_MESSAGE.getBytes()));

        return makePolicy(Arrays.<Assertion>asList(customAssertionHolder, responseAssertion));
    }

    /**
     * Creates a sample after routing policy having a hardcoded response and a custom assertion, in that order.
     * @return AllAssertion object having a hardcoded response and a custom assertion as child's.
     */
    private Assertion createAfterRoutingPolicy() {
        final HardcodedResponseAssertion responseAssertion = new HardcodedResponseAssertion();
        responseAssertion.setResponseContentType(CustomAssertionsSampleContents.MULTIPART_FIRST_PART_APP_OCTET_CONTENT_TYPE);
        responseAssertion.setResponseStatus(HardcodedResponseAssertion.DEFAULT_STATUS);
        responseAssertion.setBase64ResponseBody(HexUtils.encodeBase64(CustomAssertionsSampleContents.MULTIPART_FIRST_PART_APP_OCTET_CONTENT.getBytes()));

        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategories(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        return makePolicy(Arrays.<Assertion>asList(responseAssertion, customAssertionHolder));
    }

    // test http cookies
    private final HttpCookie[] testHttpCookies = {
            new HttpCookie("cookie1", "cookie1_value", 1, "path1", "domain1"),
            new HttpCookie("cookie2", "cookie2_value", 2, "path2", "domain2")
    };

    // test cookies to add to updatedCookies context map vector
    private final Cookie[] newCookies = {
            new Cookie("newCookie1", "newCookie1_value"),
            new Cookie("newCookie2", "newCookie2_value"),
            new Cookie("newCookie3", "newCookie3_value"),
            new Cookie("newCookie4", "newCookie4_value"),
            new Cookie("newCookie5", "newCookie5_value")
    };

    // test cookies to remove from the policy context
    private final String[] deleteCookieNames = {"cookie2", "newCookie1", "newCookie5"};

    // test HttpServletRequest
    private MockHttpServletRequest httpServletRequest = null;

    // test HttpServletResponse
    private MockHttpServletResponse httpServletResponse = null;

    /**
     * Creates a PolicyEnforcementContext with empty request and empty response,
     * with attached HttpRequestKnob and HttpResponseKnob respectively.
     *
     * @return the PolicyEnforcementContext object
     */
    private PolicyEnforcementContext createPolicyContext() throws Exception {
        // build the request
        final Message request = new Message();
        httpServletRequest = new MockHttpServletRequest();
        httpServletRequest.setMethod("GET");
        httpServletRequest.addHeader("request_header1", "request_header1_value");
        httpServletRequest.addHeader("request_header2", new String[] {"request_header2_value1", "request_header2_value2"});
        request.attachHttpRequestKnob(new HttpServletRequestKnob(httpServletRequest));

        // build the response
        final Message response = new Message();
        httpServletResponse = new MockHttpServletResponse();
        httpServletResponse.addHeader("response_header1", "response_header1_value");
        httpServletResponse.addHeader("response_header2", "response_header2_value1");
        response.attachHttpResponseKnob(new HttpServletResponseKnob(httpServletResponse));

        // build the context
        final PolicyEnforcementContext context = makeContext(request, response);
        context.addCookie(testHttpCookies[0]);
        context.addCookie(testHttpCookies[1]);

        return context;
    }

    /**
     * Do the actual test for updatedCookies and customAssertionsCookiesToOmit context-map
     */
    private void doTestUpdateAndDeletedCookies(final PolicyEnforcementContext context) {
        Object[] cookies = context.getCookies().toArray();

        final int totalNumCookies = testHttpCookies.length + newCookies.length - deleteCookieNames.length;
        assertSame(4, totalNumCookies);
        assertSame(totalNumCookies, cookies.length);

        compareCookie(cookies[0], testHttpCookies[0]);
        compareCookie(cookies[1], CookieUtils.fromServletCookie(newCookies[1], false));
        compareCookie(cookies[2], CookieUtils.fromServletCookie(newCookies[2], false));
        compareCookie(cookies[3], CookieUtils.fromServletCookie(newCookies[3], false));
    }

    /**
     * Do the actual context map tests for legacy
     */
    private void doTestLegacyContextMap(Map<String, Object> contextMap) {

        //noinspection unchecked
        Vector<Cookie> updatedCookies = (Vector<Cookie>)contextMap.get("updatedCookies"); // let it throw if its not of type Vector<Cookie>
        //noinspection unchecked
        Collection<Cookie> originalCookies = (Collection<Cookie>)contextMap.get("originalCookies"); // let it throw if its not of type Collection<Cookie>

        // verify the cookies are properly set
        assertTrue(compareCookie(updatedCookies.get(0), CookieUtils.toServletCookie(testHttpCookies[0])));
        assertTrue(compareCookie(updatedCookies.get(1), CookieUtils.toServletCookie(testHttpCookies[1])));
        // originalCookies should be the same as updatedCookies
        assertTrue(Arrays.equals(updatedCookies.toArray(), originalCookies.toArray()));

        // verify messageParts
        assertTrue(contextMap.get("messageParts") instanceof Object[][]);
        Object[][] messageParts = (Object[][])contextMap.get("messageParts");

        assertSame(4, messageParts.length);

        assertTrue(messageParts[0][0] instanceof String);
        assertTrue(messageParts[0][1] instanceof byte[]);
        assertEquals(messageParts[0][0], CustomAssertionsSampleContents.MULTIPART_APP_OCTET_PART_CONTENT_TYPE);
        assertTrue(Arrays.equals((byte[])messageParts[0][1], CustomAssertionsSampleContents.MULTIPART_APP_OCTET_PART_CONTENT.getBytes()));

        assertTrue(messageParts[1][0] instanceof String);
        assertTrue(messageParts[1][1] instanceof byte[]);
        assertEquals(messageParts[1][0], CustomAssertionsSampleContents.MULTIPART_XML_PART_CONTENT_TYPE);
        assertTrue(Arrays.equals((byte[])messageParts[1][1], CustomAssertionsSampleContents.MULTIPART_XML_PART_CONTENT.getBytes()));

        assertTrue(messageParts[2][0] instanceof String);
        assertTrue(messageParts[2][1] instanceof byte[]);
        assertEquals(messageParts[2][0], CustomAssertionsSampleContents.MULTIPART_JSON_PART_CONTENT_TYPE);
        assertTrue(Arrays.equals((byte[])messageParts[2][1], CustomAssertionsSampleContents.MULTIPART_JSON_PART_CONTENT.getBytes()));

        assertTrue(messageParts[3][0] instanceof String);
        assertTrue(messageParts[3][1] instanceof byte[]);
        assertEquals(messageParts[3][0], CustomAssertionsSampleContents.MULTIPART_SOAP_PART_CONTENT_TYPE);
        assertTrue(Arrays.equals((byte[])messageParts[3][1], CustomAssertionsSampleContents.MULTIPART_SOAP_PART_CONTENT.getBytes()));

        // verify serviceFinder
        assertTrue(contextMap.get("serviceFinder") instanceof ServiceFinderImpl);

        // verify servlet knobs
        assertTrue(Arrays.equals((String[])contextMap.get("request.http.headerValues.request_header1"), new String[] {"request_header1_value"}));
        assertTrue(Arrays.equals((String[])contextMap.get("request.http.headerValues.request_header2"), new String[] {"request_header2_value1", "request_header2_value2"}));
        assertEquals(contextMap.get("httpRequest"), httpServletRequest);
        assertNull("no response http headers", contextMap.get("request.http.headerValues.response_header1"));
        assertNull("no response http headers", contextMap.get("request.http.headerValues.response_header2"));
        assertEquals(((HttpServletResponseWrapper)contextMap.get("httpResponse")).getResponse(), httpServletResponse);
    }

    @Test
    public void testLegacyContextMapBeforeRouting() throws Exception {
        final Assertion ass = createBeforeRoutingPolicy();
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertTrue(allAssertion.getChildren().get(0) instanceof CustomAssertionHolder);
        serviceInvocation.setCustomAssertion(((CustomAssertionHolder) allAssertion.getChildren().get(0)).getCustomAssertion());

        // build the context (creates empty request and response)
        final PolicyEnforcementContext context = createPolicyContext();
        // initialize the request
        context.getRequest().initialize(
                ContentTypeHeader.parseValue(CustomAssertionsSampleContents.MULTIPART_FIRST_PART_APP_OCTET_CONTENT_TYPE),
                CustomAssertionsSampleContents.MULTIPART_FIRST_PART_APP_OCTET_CONTENT.getBytes()
        );

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest) param1;

                //noinspection unchecked
                doTestLegacyContextMap(request.getContext()); // let it throw if its not of type Map<String, Object>

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
    public void testLegacyContextMapAfterRouting() throws Exception {
        final Assertion ass = createAfterRoutingPolicy();
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertTrue(allAssertion.getChildren().get(1) instanceof CustomAssertionHolder);
        serviceInvocation.setCustomAssertion(((CustomAssertionHolder) allAssertion.getChildren().get(1)).getCustomAssertion());

        // build the context
        final PolicyEnforcementContext context = createPolicyContext();

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onResponse", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceResponse", param1 instanceof ServiceResponse);
                ServiceResponse response = (ServiceResponse)param1;

                //noinspection unchecked
                doTestLegacyContextMap(response.getContext()); // let it throw if its not of type Map<String, Object>

                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

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

        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testContextMapBeforeRouting() throws Exception {
        final Assertion ass = createBeforeRoutingPolicy();
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertTrue(allAssertion.getChildren().get(0) instanceof CustomAssertionHolder);
        serviceInvocation.setCustomAssertion(((CustomAssertionHolder) allAssertion.getChildren().get(0)).getCustomAssertion());

        // build the context (creates empty request and response)
        final PolicyEnforcementContext context = createPolicyContext();
        // initialize the request
        context.getRequest().initialize(
                ContentTypeHeader.parseValue(CustomAssertionsSampleContents.MULTIPART_FIRST_PART_APP_OCTET_CONTENT_TYPE),
                CustomAssertionsSampleContents.MULTIPART_FIRST_PART_APP_OCTET_CONTENT.getBytes()
        );

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                final Object param1 = invocation.getArguments()[0];
                assertTrue("Param is CustomPolicyContext", param1 instanceof CustomPolicyContext);
                final CustomPolicyContext policyContext = (CustomPolicyContext) param1;

                //noinspection unchecked
                doTestLegacyContextMap(policyContext.getContext()); // let it throw if its not of type Map<String, Object>

                assertNotNull(policyContext.getContext().get("defaultRequest"));
                assertNull(policyContext.getContext().get("defaultResponse"));

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

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
    public void testContextMapAfterRouting() throws Exception {
        final Assertion ass = createAfterRoutingPolicy();
        assertTrue("Is AllAssertion", ass instanceof AllAssertion);
        final AllAssertion allAssertion = (AllAssertion)ass;

        assertTrue(allAssertion.getChildren().get(1) instanceof CustomAssertionHolder);
        serviceInvocation.setCustomAssertion(((CustomAssertionHolder) allAssertion.getChildren().get(1)).getCustomAssertion());

        // build the context
        final PolicyEnforcementContext context = createPolicyContext();

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                final Object param1 = invocation.getArguments()[0];
                assertTrue("Param is CustomPolicyContext", param1 instanceof CustomPolicyContext);
                final CustomPolicyContext policyContext = (CustomPolicyContext) param1;

                //noinspection unchecked
                doTestLegacyContextMap(policyContext.getContext()); // let it throw if its not of type Map<String, Object>

                assertNull(policyContext.getContext().get("defaultRequest"));
                assertNotNull(policyContext.getContext().get("defaultResponse"));

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(allAssertion, false);
        assertTrue("Is instance of ServerAllAssertion", serverAssertion instanceof ServerAllAssertion);
        final ServerAllAssertion serverAllAssertion = (ServerAllAssertion)serverAssertion;

        AssertionStatus status = serverAllAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);
    }

    @Test
    public void testUpdateAndDeleteCookies() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategories(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        assertNotNull("CustomAssertion cannot be null.", customAssertionHolder.getCustomAssertion());
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        // build the context
        final PolicyEnforcementContext context = createPolicyContext();

        doAnswer(new Answer<CustomAssertionStatus>() {
            @Override
            public CustomAssertionStatus answer(InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                final Object param1 = invocation.getArguments()[0];
                assertTrue("Param is CustomPolicyContext", param1 instanceof CustomPolicyContext);
                final CustomPolicyContext policyContext = (CustomPolicyContext) param1;

                //noinspection unchecked
                Map<String, Object> contextMap = policyContext.getContext(); // let it throw if not of type Map<String, Object>
                assertNotNull("context-map cannot be null", contextMap);

                //noinspection unchecked
                Vector<Cookie> updatedCookies = (Vector<Cookie>)contextMap.get("updatedCookies"); // let it throw if its not of type Vector<Cookie>

                // add new cookies
                Collections.addAll(updatedCookies, newCookies);

                // add delete cookies
                contextMap.put("customAssertionsCookiesToOmit", deleteCookieNames);

                return CustomAssertionStatus.NONE;
            }
        }).when(serviceInvocation).checkRequest(Matchers.<CustomPolicyContext>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onRequest should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = (ServerCustomAssertionHolder)serverAssertion;

        AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        doTestUpdateAndDeletedCookies(context);
    }


    @Test
    public void testLegacyUpdateAndDeleteCookies() throws Exception {
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder();
        customAssertionHolder.setCategories(Category.AUDIT_ALERT);
        customAssertionHolder.setDescriptionText("Test Custom Assertion");
        customAssertionHolder.setCustomAssertion(new TestLegacyCustomAssertion());

        assertNotNull("CustomAssertion cannot be null.", customAssertionHolder.getCustomAssertion());
        serviceInvocation.setCustomAssertion(customAssertionHolder.getCustomAssertion());

        // build the context
        final PolicyEnforcementContext context = createPolicyContext();

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertTrue("there is only one parameter for onRequest", invocation.getArguments().length == 1);

                Object param1 = invocation.getArguments()[0];
                assertTrue("Param is ServiceRequest", param1 instanceof ServiceRequest);
                ServiceRequest request = (ServiceRequest) param1;

                //noinspection unchecked
                Map<String, Object> contextMap = request.getContext(); // let it throw if not of type Map<String, Object>
                assertNotNull("context-map cannot be null", contextMap);

                //noinspection unchecked
                Vector<Cookie> updatedCookies = (Vector<Cookie>)contextMap.get("updatedCookies"); // let it throw if its not of type Vector<Cookie>

                // add new cookies
                Collections.addAll(updatedCookies, newCookies);

                // add delete cookies
                contextMap.put("customAssertionsCookiesToOmit", deleteCookieNames);

                return null;
            }
        }).when(serviceInvocation).onRequest(Matchers.<ServiceRequest>any());

        //noinspection deprecation
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                fail("onResponse should not be called when checkRequest is implemented!");
                return null;
            }
        }).when(serviceInvocation).onResponse(Matchers.<ServiceResponse>any());

        final ServerAssertion serverAssertion = serverPolicyFactory.compilePolicy(customAssertionHolder, false);
        assertTrue("Is instance of ServerCustomAssertionHolder", serverAssertion instanceof ServerCustomAssertionHolder);
        final ServerCustomAssertionHolder serverCustomAssertionHolder = (ServerCustomAssertionHolder)serverAssertion;

        AssertionStatus status = serverCustomAssertionHolder.checkRequest(context);
        assertEquals(AssertionStatus.NONE, status);

        doTestUpdateAndDeletedCookies(context);
    }
}