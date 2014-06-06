package com.l7tech.server.policy.assertion;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.*;
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

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

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
    public void testAddHeader_HttpHeaderType_newmess() throws Exception {
        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");

        assertTrue(mess.getHeadersKnob().getHeaders().isEmpty());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final Collection<Header> headers = headersKnob.getHeaders("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.size());
        for (Header header : headers) {
            assertEquals(HEADER_TYPE_HTTP, header.getType());
            assertEquals("bar", header.getValue());
        }
    }

    @Test
    public void testAddHeader_JmsPropertyHeaderType_newmess() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMetadataType(HEADER_TYPE_JMS_PROPERTY);

        assertTrue(mess.getHeadersKnob().getHeaders().isEmpty());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        assertEquals(1, headersKnob.getHeaders().size());

        final Collection<Header> headers = headersKnob.getHeaders("foo", HEADER_TYPE_JMS_PROPERTY);
        assertEquals(1, headers.size());
        for (Header header : headers) {
            assertEquals(HEADER_TYPE_JMS_PROPERTY, header.getType());
            assertEquals("bar", header.getValue());
        }
    }

    @Test
    public void testAddHeaderWithOneExistingHeaderOfDifferentType_JmsPropertyHeaderType() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);

        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMetadataType(HEADER_TYPE_JMS_PROPERTY);

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        assertEquals(1, headersKnob.getHeaders().size());

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        assertEquals(2, headersKnob.getHeaders().size());
        assertEquals(1, headersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY).size());

        Header newHeader = headersKnob.getHeaders("foo", HEADER_TYPE_JMS_PROPERTY).iterator().next();

        assertEquals("foo", newHeader.getKey());
        assertEquals("bar", newHeader.getValue());
        assertEquals(HEADER_TYPE_JMS_PROPERTY, newHeader.getType());
    }

    @Test
    public void testAddHeader_servletRequest() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setMetadataType(HEADER_TYPE_HTTP);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    @BugNumber(11365)
    public void testAddHeader_servletRequest_noreplace() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setRemoveExisting(false);
        ass.setMetadataType(HEADER_TYPE_HTTP);

        MockServletContext servletContext = new MockServletContext();
        MockHttpServletRequest hrequest = new MockHttpServletRequest(servletContext);
        hrequest.addHeader("foo", "orig");
        mess.attachHttpRequestKnob(new HttpServletRequestKnob(hrequest));
        // request headers are added to headers knob by SoapMessageProcessingServlet
        mess.getHeadersKnob().addHeader("foo", "orig", HEADER_TYPE_HTTP);
        mess.initialize(ContentTypeHeader.TEXT_DEFAULT, "blah".getBytes(Charsets.UTF8));

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));

        final HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
        mess.getHeadersKnob().addHeader("foo", "orig", HEADER_TYPE_HTTP);
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
            String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
            String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
        String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);

        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        headersKnob = mess.getKnob(HeadersKnob.class);
        assertNotNull(headersKnob);
        headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
        String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);

        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        ass.setRemoveExisting(true);
        sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        headersKnob = mess.getKnob(HeadersKnob.class);
        assertNotNull(headersKnob);
        headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.length);
        assertEquals("blat", headers[0]);
    }

    @Test
    public void testAddHeader_replaceExistingWithSameTypeAndIgnoreExistingWithDifferentType() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar1", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_JMS_PROPERTY);
        mess.getHeadersKnob().addHeader("foo", "bar3", HEADER_TYPE_JMS_PROPERTY);

        HeadersKnob headersKnob = mess.getHeadersKnob();
        assertNotNull(headersKnob);
        assertEquals(4, headersKnob.getHeaders().size());
        assertEquals(2, headersKnob.getHeaders(HEADER_TYPE_HTTP).size());
        assertEquals(2, headersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY).size());

        // replace JMS Property headers named "foo"
        ass.setHeaderName("foo");
        ass.setHeaderValue("blat");
        ass.setRemoveExisting(true);
        ass.setMetadataType(HEADER_TYPE_JMS_PROPERTY);

        ServerAddHeaderAssertion sass = new ServerAddHeaderAssertion(ass);
        assertEquals(AssertionStatus.NONE, sass.checkRequest(pec));

        assertEquals(3, headersKnob.getHeaders().size());
        assertEquals(2, headersKnob.getHeaders(HEADER_TYPE_HTTP).size());

        Collection<Header> jmsHeaders = headersKnob.getHeaders(HEADER_TYPE_JMS_PROPERTY);
        assertEquals(1, jmsHeaders.size());
        assertEquals("blat", jmsHeaders.iterator().next().getValue());

        final List<String> values = Arrays.asList(headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP));
        assertEquals(2, values.size());
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("bar1"));
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
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.length);
        assertEquals("bar", headers[0]);
    }

    @Test
    public void addHeaderToHeadersKnob() throws Exception {
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] values = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals("bar", values[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
    }

    @Test
    public void replaceHeaderOnHeadersKnob() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "originalFoo", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("newFoo");
        ass.setRemoveExisting(true);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] values = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals("newFoo", values[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_ADDED));
    }

    @Test
    public void addToHeaderOnHeadersKnob() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "originalFoo", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("newFoo");
        ass.setRemoveExisting(false);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final List<String> values = Arrays.asList(headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP));
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
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
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
        final String[] headers = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, headers.length);
        assertNull(headers[0]);
    }

    @Test
    public void removeHeader() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeader_UseInvalidRegexForNameAndEvaluateAsExpression_AssertionFails() throws Exception {
        String regex = "`~!@#$%^&*()_+-={}|[]\":;'?><,./";

        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);

        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName(regex);
        ass.setHeaderValue("bar");
        ass.setEvaluateNameAsExpression(true);
        ass.setEvaluateValueExpression(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        try {
            serverAssertion.checkRequest(pec);
            fail("Expected a PolicyAssertionException to be thrown.");
        } catch (PolicyAssertionException e) {
            assertEquals("Invalid regular expression: Illegal repetition near index 15\n" +
                    "`~!@#$%^&*()_+-={}|[]\":;'?><,./\n               ^",
                    e.getMessage());
        }

        // header should not have been removed
        assertEquals(1, pec.getRequest().getHeadersKnob().getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeader_UseInvalidRegexForValueAndEvaluateAsExpression_AssertionFails() throws Exception {
        String regex = "`~!@#$%^&*()_+-={}|[]\":;'?><,./";

        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);

        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue(regex);
        ass.setEvaluateNameAsExpression(false);
        ass.setEvaluateValueExpression(true);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        try {
            serverAssertion.checkRequest(pec);
            fail("Expected a PolicyAssertionException to be thrown.");
        } catch (PolicyAssertionException e) {
            assertEquals("Invalid regular expression: Illegal repetition near index 15\n" +
                    "`~!@#$%^&*()_+-={}|[]\":;'?><,./\n               ^",
                    e.getMessage());
        }

        // header should not have been removed
        assertEquals(1, pec.getRequest().getHeadersKnob().getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeader_UseValueSeparatorInValue_MatchingHeaderRemoved() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "ba,r", HEADER_TYPE_HTTP);

        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("ba,r");
        ass.setEvaluateNameAsExpression(false);
        ass.setEvaluateValueExpression(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderByValueWithSeparatorInQuotedSingleValue() throws IOException, PolicyAssertionException {
        mess.getHeadersKnob().addHeader("foo", "\"val, ue2\"", HEADER_TYPE_HTTP);

        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("\"val, ue2\"");
        ass.setEvaluateNameAsExpression(false);
        ass.setEvaluateValueExpression(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderByValueMultivaluedWithSeparatorInQuotedValue() throws IOException, PolicyAssertionException {
        mess.getHeadersKnob().addHeader("foo", "\"val\"\"ue1\",\"val, ue2\",value3", HEADER_TYPE_HTTP);

        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("\"val, ue2\"");
        ass.setEvaluateNameAsExpression(false);
        ass.setEvaluateValueExpression(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertEquals("\"val\"\"ue1\",value3", headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP, false)[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderByValueContainingEscapedQuotesFromMultivaluedHeader() throws IOException, PolicyAssertionException {
        mess.getHeadersKnob().addHeader("foo", "\"val\"\"ue1\",\"val, ue2\",value3", HEADER_TYPE_HTTP);

        ass.setMetadataType(HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("\"val\"\"ue1\"");
        ass.setEvaluateNameAsExpression(false);
        ass.setEvaluateValueExpression(false);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertEquals("\"val, ue2\",value3", headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP, false)[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeader_TypeMatchesSubset_MatchingSubsetRemoved() throws Exception {
        HeadersKnob headersKnob = mess.getHeadersKnob();

        headersKnob.addHeader("foo", "bar1", HEADER_TYPE_HTTP);
        headersKnob.addHeader("foo", "bar1", HEADER_TYPE_HTTP);
        headersKnob.addHeader("foo", "bar2", HEADER_TYPE_JMS_PROPERTY);
        headersKnob.addHeader("foo", "bar2", HEADER_TYPE_JMS_PROPERTY);

        assertEquals(4, headersKnob.getHeaders().size());

        ass.setHeaderName("foo");
        ass.setMetadataType(HEADER_TYPE_JMS_PROPERTY);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals(2, headersKnob.getHeaders().size());

        // ensure the correct headers remain
        for (Header header : headersKnob.getHeaders()) {
            assertEquals(HEADER_TYPE_HTTP, header.getType());
            assertEquals("foo", header.getKey());
            assertEquals("bar1", header.getValue());
        }
    }

    @Test
    public void removeHeaderIgnoresValueIfEmpty() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderWithValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithLiteralExpressionValue() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        // expression should be evaluated literally (no match)
        ass.setHeaderValue("b.*");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("bar", fooValues[0]);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithEmptyValueFromContextVariable() throws Exception {
        pec.setVariable("value", "");
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("${value}");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("bar", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderWithValueMultivalued() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar, bar2", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue("bar");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("bar2", fooValues[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeader_NameDoesNotMatch_NoHeadersRemoved() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        ass.setHeaderName("notFound");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertEquals("bar", headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeader_NameMatchesButTypeDoesNot_NoHeadersRemoved() throws Exception {
        HeadersKnob headersKnob = mess.getHeadersKnob();

        headersKnob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        assertEquals(1, headersKnob.getHeaders().size());

        ass.setHeaderName("foo");
        ass.setMetadataType(HEADER_TYPE_JMS_PROPERTY);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);

        AssertionStatus status = serverAssertion.checkRequest(pec);

        assertEquals(AssertionStatus.NONE, status);
        assertEquals(1, headersKnob.getHeaders().size());
        assertFalse(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test(expected = PolicyAssertionException.class)
    public void nullHeaderName() throws Exception {
        ass.setHeaderName(null);
        serverAssertion.checkRequest(pec);
    }

    @Test
    public void removeHeaderNameExpression() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("Foo", "caseNoMatch", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("doesNotMatch", "shouldNotBeRemoved", HEADER_TYPE_HTTP);
        ass.setHeaderName(STARTS_WITH_F);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] upperCaseFooValues = headersKnob.getHeaderValues("Foo", HEADER_TYPE_HTTP);
        assertEquals(1, upperCaseFooValues.length);
        assertEquals("caseNoMatch", upperCaseFooValues[0]);
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("doesNotMatch", HEADER_TYPE_HTTP)[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderNameExpressionAll() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar, bar2", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("abc", "123", HEADER_TYPE_HTTP);
        ass.setHeaderName(".*");
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME));
    }

    @Test
    public void removeHeaderValueExpression() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "Bar(caseNoMatch)", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "valNoMatch", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("Foo", "barz", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("nameNoMatch", "shouldNotBeRemoved", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue(STARTS_WITH_B);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateValueExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP));
        assertEquals(2, fooValues.size());
        assertTrue(fooValues.contains("valNoMatch"));
        assertTrue(fooValues.contains("Bar(caseNoMatch)"));
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("nameNoMatch", HEADER_TYPE_HTTP)[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderValueExpressionMultivalued() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "Bar(caseNoMatch), bar, valNoMatch", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("Foo", "barz", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("nameNoMatch", "shouldNotBeRemoved", HEADER_TYPE_HTTP);
        ass.setHeaderName("foo");
        ass.setHeaderValue(STARTS_WITH_B);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateValueExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(2, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] fooValues = headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("Bar(caseNoMatch),valNoMatch", fooValues[0]);
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("nameNoMatch", HEADER_TYPE_HTTP)[0]);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.HEADER_REMOVED_BY_NAME_AND_VALUE));
    }

    @Test
    public void removeHeaderNameAndValueExpressions() throws Exception {
        mess.getHeadersKnob().addHeader("foo", "bar", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("foo", "valNoMatch", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("Foo", "caseNoMatch", HEADER_TYPE_HTTP);
        mess.getHeadersKnob().addHeader("doesNotMatch", "shouldNotBeRemoved", HEADER_TYPE_HTTP);
        ass.setHeaderName(STARTS_WITH_F);
        ass.setHeaderValue(STARTS_WITH_B);
        ass.setOperation(AddHeaderAssertion.Operation.REMOVE);
        ass.setEvaluateNameAsExpression(true);
        ass.setEvaluateValueExpression(true);

        assertEquals(AssertionStatus.NONE, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, true));
        assertEquals(3, headerNames.size());
        assertTrue(headerNames.contains("foo"));
        assertTrue(headerNames.contains("Foo"));
        assertTrue(headerNames.contains("doesNotMatch"));
        final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP));
        assertEquals(2, fooValues.size());
        assertTrue(fooValues.contains("valNoMatch"));
        assertTrue(fooValues.contains("caseNoMatch"));
        assertEquals("shouldNotBeRemoved", headersKnob.getHeaderValues("doesNotMatch", HEADER_TYPE_HTTP)[0]);
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
        assertEquals(1, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        final String[] vals = headersKnob.getHeaderValues(STARTS_WITH_F, HEADER_TYPE_HTTP);
        assertEquals(1, vals.length);
        assertEquals(STARTS_WITH_B, vals[0]);
    }

    @Test
    public void addHeaderNameContextVarDoesNotExist() throws Exception {
        ass.setHeaderName("${name}");
        ass.setHeaderValue("test");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_HEADER_NAME));
    }

    @Test
    public void addHeaderNameContextVarResolvesEmpty() throws Exception {
        pec.setVariable("name", "");
        ass.setHeaderName("${name}");
        ass.setHeaderValue("test");

        assertEquals(AssertionStatus.FALSIFIED, serverAssertion.checkRequest(pec));
        final HeadersKnob headersKnob = pec.getRequest().getHeadersKnob();
        assertEquals(0, headersKnob.getHeaderNames(HEADER_TYPE_HTTP, true, false).length);
        assertTrue(testAudit.isAuditPresent(AssertionMessages.EMPTY_HEADER_NAME));
    }

    private void configureServerAssertion(final ServerAddHeaderAssertion serverAssertion) {
        ApplicationContexts.inject(serverAssertion,
                CollectionUtils.MapBuilder.<String, Object>builder()
                        .put("auditFactory", testAudit.factory())
                        .map());
    }
}
