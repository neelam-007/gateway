package com.l7tech.server.policy.assertion;

import com.l7tech.common.http.CookieUtils;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.HttpServletResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AddHeaderAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Charsets;
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
    AddHeaderAssertion ass = new AddHeaderAssertion();
    Message mess = new Message();
    PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(mess, new Message());

    @Before
    public void setup() throws Exception {
        mess.initialize(XmlUtil.parse("<xml/>"));
    }

    @Test
    public void testAddHeader_newmess() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        assertTrue(mess.getHeadersKnob().getHeaders().isEmpty());

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

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
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

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
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("foo", "orig");
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        // request headers are added to headers knob by SoapMessageProcessingServlet
        mess.getHeadersKnob().addHeader("foo", "orig");
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

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
        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);

        MockHttpServletResponse hresponse = new MockHttpServletResponse();
        mess.attachHttpResponseKnob(new HttpServletResponseKnob(hresponse));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), mess);

        HeadersKnob existingKnob = mess.getKnob(HeadersKnob.class);
        assertNotNull(existingKnob);

        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

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
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] values = headersKnob.getHeaderValues("foo");
        assertEquals(1, values.length);
        assertEquals("bar", values[0]);
    }

    @Test
    public void replaceHeaderOnHeadersKnob() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "originalFoo");
        ass.setHeaderName("foo");
        ass.setHeaderValue("newFoo");
        ass.setRemoveExisting(true);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] values = headersKnob.getHeaderValues("foo");
        assertEquals(1, values.length);
        assertEquals("newFoo", values[0]);
    }

    @Test
    public void addToHeaderOnHeadersKnob() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "originalFoo");
        ass.setHeaderName("foo");
        ass.setHeaderValue("newFoo");
        ass.setRemoveExisting(false);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final List<String> values = Arrays.asList(headersKnob.getHeaderValues("foo"));
        assertEquals(2, values.size());
        assertTrue(values.contains("originalFoo"));
        assertTrue(values.contains("newFoo"));
    }

    @Test
    public void addHeaderEmptyValue() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("");
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
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
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
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
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames().length);
    }

    @Test
    public void removeHeaderIgnoresValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("shouldBeIgnored");
        ass.setMatchValueForRemoval(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames().length);
    }

    @Test
    public void removeHeaderWithValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
    }

    @Test
    public void removeHeaderWithEmptyValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "");
        ass.setHeaderName("foo");
        ass.setHeaderValue("");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar", fooValues[0]);
    }

    @Test
    public void removeHeaderWithValueMultivalued() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar, bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMatchValueForRemoval(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
    }

    @Test
    public void removeHeaderNotFound() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        ass.setHeaderName("notFound");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames().length);
        assertEquals("bar", headersKnob.getHeaderValues("foo")[0]);
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullHeaderName() throws Exception {
        ass.setHeaderName(null);
        new ServerAddHeaderAssertion(ass).checkRequest(pec);
    }

    @Test(expected = IllegalStateException.class)
    public void messageNotInitialized() throws Exception {
        new ServerAddHeaderAssertion(ass).checkRequest(PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message()));
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

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames().length);
        final String[] upperCaseFooValues = headersKnob.getHeaderValues("Foo");
        assertEquals(1, upperCaseFooValues.length);
        assertEquals("caseNoMatch", upperCaseFooValues[0]);
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("doesNotMatch")[0]);
    }

    @Test
    public void removeHeaderNameExpressionAll() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar, bar2");
        mess.getHeadersKnob().addHeader("abc", "123");
        ass.setHeaderName(".*");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames().length);
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

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames().length);
        final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo"));
        assertEquals(2, fooValues.size());
        assertTrue(fooValues.contains("valNoMatch"));
        assertTrue(fooValues.contains("Bar(caseNoMatch)"));
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("nameNoMatch")[0]);
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

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames().length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("Bar(caseNoMatch),valNoMatch", fooValues[0]);
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("nameNoMatch")[0]);
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

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
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
    }

    @Test
    public void addHeaderIgnoresExpressions() throws Exception {
        ass.setHeaderName(STARTS_WITH_F);
        ass.setHeaderValue(STARTS_WITH_B);
        // following flags should be ignored when performing an add
        ass.setEvaluateNameAsExpression(true);
        ass.setEvaluateValueExpression(true);
        ass.setMatchValueForRemoval(true);

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
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

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertNull(cookie.getDomain());
        assertNull(cookie.getPath());
    }

    @Test
    public void addCookieWithDomain() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;domain=localhost");

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("localhost", cookie.getDomain());
        assertNull(cookie.getPath());
    }

    @Test
    public void addCookieWithPath() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;path=/foo");

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("/foo", cookie.getPath());
        assertNull(cookie.getDomain());
    }

    @Test
    public void addCookieWithComment() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;comment=test");

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals("test", cookie.getComment());
    }

    @Test
    public void addCookieWithSecure() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;secure");

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(cookie.isSecure());
    }

    @Test
    public void addCookieWithExpires() throws Exception {
        final Calendar calendar = new GregorianCalendar(2013, Calendar.NOVEMBER, 15, 0, 0, 0);
        ass.setHeaderName("Cookie");
        final SimpleDateFormat format = new SimpleDateFormat(CookieUtils.RFC1123_RFC1036_RFC822_DATEFORMAT, Locale.US);
        ass.setHeaderValue("foo=bar;expires=" + format.format(calendar.getTime()));

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertTrue(cookie.hasExpiry());
    }

    @Test
    public void addCookieWithMaxAge() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;max-age=60");

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(60, cookie.getMaxAge());
    }

    @Test
    public void addCookieWithVersion() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("foo=bar;version=0");

        assertEquals(AssertionStatus.NONE, new ServerAddHeaderAssertion(ass).checkRequest(pec));
        assertEquals(1, pec.getCookies().size());
        final HttpCookie cookie = pec.getCookies().iterator().next();
        assertEquals("foo", cookie.getCookieName());
        assertEquals("bar", cookie.getCookieValue());
        assertEquals(0, cookie.getVersion());
    }

    @Test
    public void addInvalidCookie() throws Exception {
        ass.setHeaderName("Cookie");
        ass.setHeaderValue("");
        assertEquals(AssertionStatus.FALSIFIED, new ServerAddHeaderAssertion(ass).checkRequest(pec));
    }
}
