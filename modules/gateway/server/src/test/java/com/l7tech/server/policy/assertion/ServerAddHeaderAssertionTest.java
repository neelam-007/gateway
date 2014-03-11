package com.l7tech.server.policy.assertion;

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

import java.util.Arrays;
import java.util.List;

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
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
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
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
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
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
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
        assertEquals(0, headersKnob.getHeaderNames(true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderIgnoresValueIfEmpty() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderWithValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "bar2");
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithLiteralExpressionValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar");
        ass.setHeaderName("foo");
        // expression should be evaluated literally (no match)
        ass.setHeaderValue("b.*");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("bar", fooValues[0]);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithEmptyValueFromContextVariable() throws Exception {
        pec.setVariable("value", "");
        mess.getHeadersKnob().addHeader("foo", "bar");
        mess.getHeadersKnob().addHeader("foo", "");
        ass.setHeaderName("foo");
        ass.setHeaderValue("${value}");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
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
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
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
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
        assertEquals("bar", headersKnob.getHeaderValues("foo")[0]);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullHeaderName() throws Exception {
        ass.setHeaderName(null);
        serverAssertion.checkRequest(pec);
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
        assertEquals(2, headersKnob.getHeaderNames(true, false).length);
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
        assertEquals(0, headersKnob.getHeaderNames(true, false).length);
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

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames(true, false).length);
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

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames(true, false).length);
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

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames(true, true));
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

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(true, false).length);
        final String[] vals = headersKnob.getHeaderValues(STARTS_WITH_F);
        assertEquals(1, vals.length);
        assertEquals(STARTS_WITH_B, vals[0]);
    }

    @Test
    public void addHeaderNameContextVarDoesNotExist() throws Exception {
        ass.setHeaderName("${name}");
        ass.setHeaderValue("test");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_HEADER_NAME));
    }

    @Test
    public void addHeaderNameContextVarResolvesEmpty() throws Exception {
        pec.setVariable("name", "");
        ass.setHeaderName("${name}");
        ass.setHeaderValue("test");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_HEADER_NAME));
    }

    private void configureServerAssertion(final ServerAddHeaderAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
    }
}
