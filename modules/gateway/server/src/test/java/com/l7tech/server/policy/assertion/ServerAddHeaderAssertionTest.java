package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ServerAddHeaderAssertion}.
 */
public class ServerAddHeaderAssertionTest {
    private static final String STARTS_WITH_F = "f[a-zA-Z0-9_]*";
    private static final String STARTS_WITH_B = "b[a-zA-Z0-9_]*";
    private AddHeaderAssertion ass = new AddHeaderAssertion();
    private Message mess = new Message();
    private PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());
    private TestAudit testAudit;
    private ServerAddHeaderAssertion serverAssertion;

    @Before
    public void setup() throws Exception {
        mess.initialize(XmlUtil.parse("<xml/>"));
        testAudit = new TestAudit();
        serverAssertion = new ServerAddHeaderAssertion(ass);
        configureServerAssertion(serverAssertion);
    }

    @Test
    public void testAddHeader_newmess() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");

        assertTrue(mess.getHeadersKnob().getHeaders().isEmpty());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void testAddHeader_servletRequest() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    @BugNumber(11365)
    public void testAddHeader_servletRequest_noreplace() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setRemoveExisting(false);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("foo", "orig");
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        // request headers are added to headers knob by SoapMessageProcessingServlet
        mess.getHeadersKnob().addHeader("foo", "orig");
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(2, headers.length);
        assertEquals("orig", headers[0]);
        assertEquals("bar", headers[1]);
    }

    @Test
    @BugNumber(11365)
    public void testAddHeader_servletRequest_replace_then_noreplace() throws Exception {
        // Original incoming request contained "foo: orig"
        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("foo", "orig");
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        // request headers are added to headers knob by SoapMessageProcessingServlet
        mess.getHeadersKnob().addHeader("foo", "orig");
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));


        // We now add a "foo: bar", replacing existing
        {
            ass.setHeaderName("foo");
            ass.setHeaderValue("bar");
            ass.setRemoveExisting(true);
            ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

            assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));
            final HeadersKnob headersKnob = mess.getHeadersKnob();
            assertNotNull(headersKnob);
            String[] headers = headersKnob.getHeaderValues("foo");
            assertEquals(1, headers.length);
            assertEquals("bar", headers[0]);
        }


        // We now add a "foo: bar2", keeping existing
        {
            ass.setHeaderName("foo");
            ass.setHeaderValue("bar2");
            ass.setRemoveExisting(false);
            ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

            assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));
            final HeadersKnob headersKnob = mess.getHeadersKnob();
            assertNotNull(headersKnob);
            String[] headers = headersKnob.getHeaderValues("foo");
            assertEquals(2, headers.length);
            assertEquals("bar", headers[0]);
            assertEquals("bar2", headers[1]);
        }


    }

    @Test
    public void testAddHeader_servletResponse() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setTarget(TargetMessageType.RESPONSE);

        MockHttpServletResponse hresponse = new MockHttpServletResponse();
        mess.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);

        HeadersKnob existingKnob = mess.getKnob(HeadersKnob.class);
        assertNotNull(existingKnob);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void testAddHeader_addNewToExisting() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);

        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        headersKnob = mess.getKnob(HeadersKnob.class);
        assertNotNull(headersKnob);
        headers = headersKnob.getHeaderValues("foo");
        assertEquals(2, headers.length);
        assertEquals("bar", headers[0]);
        assertEquals("blat", headers[1]);
    }

    @Test
    public void testAddHeader_replaceExisting() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);

        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        ass.setRemoveExisting(true);
        sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        headersKnob = mess.getKnob(HeadersKnob.class);
        assertNotNull(headersKnob);
        headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("blat", headers[0]);
    }

    @Test
    public void testAddHeader_contextVarName() throws Exception {
        ass.setHeaderName("${hname}");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        pec.setVariable("hname", "foo");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void testAddHeader_contextVarValue() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("${hvalue}");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        pec.setVariable("hvalue", "bar");
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void addHeaderToHeadersKnob() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] values = headersKnob.getHeaderValues("foo");
        assertEquals(1, values.length);
        assertEquals("bar", values[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
    }

    @Test
    public void replaceHeaderOnHeadersKnob() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "originalFoo");
        ass.setHeaderName("foo");
        ass.setHeaderValue("newFoo");
        ass.setRemoveExisting(true);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] values = headersKnob.getHeaderValues("foo");
        assertEquals(1, values.length);
        assertEquals("newFoo", values[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
    }

    @Test
    public void addToHeaderOnHeadersKnob() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "originalFoo");
        ass.setHeaderName("foo");
        ass.setHeaderValue("newFoo");
        ass.setRemoveExisting(false);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final List<String> values = Arrays.asList(headersKnob.getHeaderValues("foo"));
        assertEquals(2, values.size());
        assertTrue(values.contains("originalFoo"));
        assertTrue(values.contains("newFoo"));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
    }

    @Test
    public void addHeaderEmptyValue() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("");
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertEquals(StringUtils.EMPTY, headers[0]);
    }

    @Test
    public void addHeaderNullValue() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue(null);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo");
        assertEquals(1, headers.length);
        assertNull(headers[0]);
    }

    @Test
    public void removeHeader() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames().length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderIgnoresValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("shouldBeIgnored");
        ass.setMatchValueForRemoval(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames().length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderWithValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithEmptyValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "");
        ass.setHeaderName("foo");
        ass.setHeaderValue("");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithValueMultivalued() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar, bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderNotFound() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        ass.setHeaderName("notFound");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        assertEquals("bar", headersKnob.getHeaderValues("foo")[0]);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullHeaderName() throws Exception {
        ass.setHeaderName(null);
        serverAssertion.checkRequest(pec);
    }

    @Test(expected = IllegalStateException.class)
    public void messageNotInitialized() throws Exception {
        serverAssertion.checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message()));
    }

    @Test
    public void removeHeaderNameExpression() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        mess.getHeadersKnob().addHeader("Foo", "caseNoMatch");
        mess.getHeadersKnob().addHeader("doesNotMatch", "shouldNotBeRemoved");
        ass.setHeaderName(STARTS_WITH_F);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames().length);
        final String[] upperCaseFooValues = headersKnob.getHeaderValues("Foo");
        assertEquals(1, upperCaseFooValues.length);
        assertEquals("caseNoMatch", upperCaseFooValues[0]);
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("doesNotMatch")[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderNameExpressionAll() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar, bar2");
        mess.getHeadersKnob().addHeader("abc", "123");
        ass.setHeaderName(".*");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames().length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderValueExpression() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "Bar(caseNoMatch)");
        mess.getHeadersKnob().addHeader("foo", "valNoMatch");
        mess.getHeadersKnob().addHeader("Foo", "barz");
        mess.getHeadersKnob().addHeader("nameNoMatch", "shouldNotBeRemoved");
        ass.setHeaderName("foo");
        ass.setHeaderValue(STARTS_WITH_B);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateValueExpression(true);
        ass.setMatchValueForRemoval(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames().length);
        final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo"));
        assertEquals(2, fooValues.size());
        assertTrue(fooValues.contains("valNoMatch"));
        assertTrue(fooValues.contains("Bar(caseNoMatch)"));
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("nameNoMatch")[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderValueExpressionMultivalued() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "Bar(caseNoMatch), bar, valNoMatch");
        mess.getHeadersKnob().addHeader("Foo", "barz");
        mess.getHeadersKnob().addHeader("nameNoMatch", "shouldNotBeRemoved");
        ass.setHeaderName("foo");
        ass.setHeaderValue(STARTS_WITH_B);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateValueExpression(true);
        ass.setMatchValueForRemoval(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("Bar(caseNoMatch),valNoMatch", fooValues[0]);
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("nameNoMatch")[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderNameAndValueExpressions() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        mess.getHeadersKnob().addHeader("foo", "valNoMatch");
        mess.getHeadersKnob().addHeader("Foo", "caseNoMatch");
        mess.getHeadersKnob().addHeader("doesNotMatch", "shouldNotBeRemoved");
        ass.setHeaderName(STARTS_WITH_F);
        ass.setHeaderValue(STARTS_WITH_B);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);
        ass.setEvaluateValueExpression(true);
        ass.setMatchValueForRemoval(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames());
        assertEquals(3, headerNames.size());
        assertTrue(headerNames.contains("foo"));
        assertTrue(headerNames.contains("Foo"));
        assertTrue(headerNames.contains("doesNotMatch"));
        final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo"));
        assertEquals(2, fooValues.size());
        assertTrue(fooValues.contains("valNoMatch"));
        assertTrue(fooValues.contains("caseNoMatch"));
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("doesNotMatch")[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void addHeaderIgnoresExpressions() throws Exception {
        ass.setHeaderName(STARTS_WITH_F);
        ass.setHeaderValue(STARTS_WITH_B);
        // following flags should be ignored when performing an add
        ass.setEvaluateNameAsExpression(true);
        ass.setEvaluateValueExpression(true);
        ass.setMatchValueForRemoval(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] vals = headersKnob.getHeaderValues(STARTS_WITH_F);
        assertEquals(1, vals.length);
        assertEquals(STARTS_WITH_B, vals[0]);
    }

    @Test
    public void addSimpleCookie() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertFalse(cookie.isNew());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
    }

    @Test
    public void addCookieReplaceExisting() throws Exception {
        pec.addCookie(new HttpCookie("shouldBeReplaced", "shouldBeReplaced", 1, "/", "localhost"));
        mess.getHeadersKnob().addHeader("Cookie", "shouldBeRemoved=shouldBeRemoved");
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar");
        ass.setRemoveExisting(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithDomain() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;domain=localhost");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("localhost", cookie.getDomain());
        assertNull(cookie.getPath());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithPath() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;path=/foo");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("/foo", cookie.getPath());
        assertNull(cookie.getDomain());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithComment() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;comment=test");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("test", cookie.getComment());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithSecure() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;secure");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(cookie.isSecure());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithExpires() throws Exception {
        final Calendar calendar = new GregorianCalendar(2013, Calendar.NOVEMBER, 15, 0, 0, 0);
        ass.setHeaderName("Cookie");
        final SimpleDateFormat format = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        final String headerValue = "foo=bar;expires=" + format.format(calendar.getTime());
        ass.setHeaderValue(headerValue);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(cookie.hasExpiry());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithMaxAge() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;max-age=60");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(60, cookie.getMaxAge());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieWithVersion() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;version=0");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addSetCookieToRequest() throws Exception {
        ass.setHeaderName("Set-Cookie");
        ass.setHeaderValue("foo=bar");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(1, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals("foo=bar", mess.getHeadersKnob().getHeaderValues("Set-Cookie")[0]);
        assertTrue(pec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addSetCookieToResponse() throws Exception {
        ass.setTarget(TargetMessageType.RESPONSE);
        ass.setHeaderName("Set-Cookie");
        ass.setHeaderValue("foo=bar;domain=localhost;path=/;secure;max-age=60;version=1");
        final PolicyEnforcementContext responsePec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(responsePec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, responsePec.getCookies().size());
        final HttpCookie cookie = responsePec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("localhost", cookie.getDomain());
        assertEquals("/", cookie.getPath());
        assertEquals(60, cookie.getMaxAge());
        assertEquals(1, cookie.getVersion());
        assertTrue(cookie.isNew());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieToResponse() throws Exception {
        ass.setTarget(TargetMessageType.RESPONSE);
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar");
        final PolicyEnforcementContext responsePec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(responsePec));
        assertEquals(1, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals("foo=bar", mess.getHeadersKnob().getHeaderValues("Cookie")[0]);
        assertTrue(responsePec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addSetCookieToNonRequestOrResponse() throws Exception {
        final Message otherMessage = new Message();
        otherMessage.initialize(XmlUtil.createEmptyDocument());
        pec.setVariable("otherMessageVar", otherMessage);
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("otherMessageVar");
        ass.setHeaderName("Set-Cookie");
        ass.setHeaderValue("foo=bar");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(1, otherMessage.getHeadersKnob().getHeaderNames().length);
        assertEquals("foo=bar", otherMessage.getHeadersKnob().getHeaderValues("Set-Cookie")[0]);
        // cookie should not be added to context if target is not request/response
        assertTrue(pec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addCookieToNonRequestOrResponse() throws Exception {
        final Message otherMessage = new Message();
        otherMessage.initialize(XmlUtil.createEmptyDocument());
        pec.setVariable("otherMessageVar", otherMessage);
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("otherMessageVar");
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(1, otherMessage.getHeadersKnob().getHeaderNames().length);
        assertEquals("foo=bar", otherMessage.getHeadersKnob().getHeaderValues("Cookie")[0]);
        // cookie should not be added to context if target is not request/response
        assertTrue(pec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void addInvalidCookie() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("");
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertTrue(pec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HTTPROUTE_INVALIDCOOKIE));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_ADDED));
    }

    @Test
    public void removeCookie() throws Exception {
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        mess.getHeadersKnob().addHeader("Cookie", "foo=bar");
        mess.getHeadersKnob().addHeader("Cookie", "key=value");
        ass.setHeaderName("Cookie");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertTrue(pec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByNameExpression() throws Exception {
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        mess.getHeadersKnob().addHeader("Cookie", "foo=bar");
        mess.getHeadersKnob().addHeader("Cookie", "key=value");
        ass.setHeaderName("^C.*");
        ass.setEvaluateNameAsExpression(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertTrue(pec.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByValue() throws Exception {
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        pec.addCookie(new HttpCookie((String) null, null, "key=value"));
        mess.getHeadersKnob().addHeader("Cookie", "foo=bar");
        mess.getHeadersKnob().addHeader("Cookie", "key=value");
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(1, mess.getHeadersKnob().getHeaderNames().length);
        final String[] cookieValues = mess.getHeadersKnob().getHeaderValues("Cookie");
        assertEquals(1, cookieValues.length);
        assertEquals("key=value", cookieValues[0]);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("key", cookie.getCookieName());
        assertEquals("value", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByValueExpression() throws Exception {
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        pec.addCookie(new HttpCookie((String) null, null, "key=value"));
        mess.getHeadersKnob().addHeader("Cookie", "foo=bar");
        mess.getHeadersKnob().addHeader("Cookie", "key=value");
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("^f.*");
        ass.setMatchValueForRemoval(true);
        ass.setEvaluateValueExpression(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(1, mess.getHeadersKnob().getHeaderNames().length);
        final String[] cookieValues = mess.getHeadersKnob().getHeaderValues("Cookie");
        assertEquals(1, cookieValues.length);
        assertEquals("key=value", cookieValues[0]);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("key", cookie.getCookieName());
        assertEquals("value", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieByValueInvalidCookie() throws Exception {
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        mess.getHeadersKnob().addHeader("Cookie", "");
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        // invalid cookie header should be removed
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        // context cookies were not changed
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeSetCookieFromResponse() throws Exception {
        final PolicyEnforcementContext responseContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);
        responseContext.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        mess.getHeadersKnob().addHeader("Set-Cookie", "foo=bar");
        ass.setTarget(TargetMessageType.RESPONSE);
        ass.setHeaderName("Set-Cookie");
        ass.setMatchValueForRemoval(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(responseContext));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertTrue(responseContext.getCookies().isEmpty());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    /**
     * Cookies should not be removed from context if the target is response and the header name is not 'Set-Cookie'.
     */
    @Test
    public void removeCookieFromResponse() throws Exception {
        final PolicyEnforcementContext responseContext = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);
        responseContext.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        mess.getHeadersKnob().addHeader("Cookie", "foo=bar");
        ass.setTarget(TargetMessageType.RESPONSE);
        ass.setHeaderName("Cookie");
        ass.setMatchValueForRemoval(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(responseContext));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, responseContext.getCookies().size());
        final HttpCookie cookie = responseContext.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    /**
     * Cookies should not be removed from context if the target is request and the header name is not 'Cookie'.
     */
    @Test
    public void removeSetCookie() throws Exception {
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        mess.getHeadersKnob().addHeader("Set-Cookie", "foo=bar");
        ass.setHeaderName("Set-Cookie");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, mess.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeCookieFromNonRequestOrResponse() throws Exception {
        final Message otherMessage = new Message();
        otherMessage.initialize(XmlUtil.createEmptyDocument());
        pec.setVariable("otherMessageVar", otherMessage);
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        otherMessage.getHeadersKnob().addHeader("Cookie", "foo=bar");
        ass.setHeaderName("Cookie");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("otherMessageVar");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, otherMessage.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    @Test
    public void removeSetCookieFromNonRequestOrResponse() throws Exception {
        final Message otherMessage = new Message();
        otherMessage.initialize(XmlUtil.createEmptyDocument());
        pec.setVariable("otherMessageVar", otherMessage);
        pec.addCookie(new HttpCookie((String) null, null, "foo=bar"));
        otherMessage.getHeadersKnob().addHeader("Set-Cookie", "foo=bar");
        ass.setHeaderName("Set-Cookie");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setTarget(TargetMessageType.OTHER);
        ass.setOtherTargetMessageVariable("otherMessageVar");

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        assertEquals(0, otherMessage.getHeadersKnob().getHeaderNames().length);
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(testAudit.isAuditPresent(AssertionMessages.INVALID_HEADER_FOR_TARGET));
        assertFalse(testAudit.isAuditPresent(AssertionMessages.COOKIE_REMOVED));
    }

    private void configureServerAssertion(final ServerAddHeaderAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
    }
}
