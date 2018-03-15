package com.l7tech.server;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.Header;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpCookiesKnob;
import com.l7tech.message.JmsKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.audit.AuditContextFactory;
import com.l7tech.server.audit.MessageSummaryAuditFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugId;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SoapMessageProcessingServletTest {
    private SoapMessageProcessingServlet servlet;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private SsgConnector connector;
    @Mock
    private LicenseManager licenseManager;
    @Mock
    private Config config;
    @Mock
    private StashManagerFactory stashManagerFactory;
    @Mock
    private StashManager stashManager;
    @Mock
    private MessageProcessor messageProcessor;
    @Mock
    private MessageSummaryAuditFactory messageSummaryAuditFactory;
    @Mock
    private AuditContextFactory auditContextFactory;

    private static final String ALLOW_GZIP_COMPRESSED_REQUEST = "request.compress.gzip.allow";

    @Before
    public void setup() throws Exception {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        servlet = new TestableSoapMessageProcessingServlet();
        servlet.setLicenseManager(licenseManager);
        servlet.setConfig(config);
        servlet.setStashManagerFactory(stashManagerFactory);
        servlet.setMessageProcessor(messageProcessor);
        servlet.auditContextFactory = auditContextFactory;
        servlet.messageSummaryAuditFactory = messageSummaryAuditFactory;
        connector = new SsgConnector();
        connector.setEndpoints(SsgConnector.Endpoint.MESSAGE_INPUT.name());
        when(stashManagerFactory.createStashManager()).thenReturn(stashManager);
        when(auditContextFactory.doWithNewAuditContext(Matchers.<Callable>any(), Matchers.<Functions.Nullary>any())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return ((Callable) invocationOnMock.getArguments()[0]).call();
            }
        });
        doReturn(false).when(messageProcessor).isRelayGatewayMetricsEnable();
    }

    @Test
    public void requestHeadersAddedToContext() throws Exception {
        request.addHeader("test", "test");
        request.addHeader("foo", "bar");
        request.addHeader("foo", "bar2");
        request.setContent("test".getBytes());
        // using doAnswer to verify headers because ArgumentCaptor does not capture the state of the PEC at the time of method execution
        // instead the ArgumentCaptor captures a reference to the PEC which is mutated after method execution and cannot be used to verify headers
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HeadersKnob headersKnob = context.getRequest().getHeadersKnob();
                final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames(HEADER_TYPE_HTTP));
                assertEquals(2, headerNames.size());
                assertTrue(headerNames.contains("test"));
                assertTrue(headerNames.contains("foo"));
                assertEquals(1, headersKnob.getHeaderValues("test", HEADER_TYPE_HTTP).length);
                assertEquals("test", headersKnob.getHeaderValues("test", HEADER_TYPE_HTTP)[0]);
                final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP));
                assertEquals(2, fooValues.size());
                assertTrue(fooValues.contains("bar"));
                assertTrue(fooValues.contains("bar2"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @Test
    public void requestHeadersAddedToContextFiltersHeaders() throws Exception {
        for (final String headerNotToForward : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            request.addHeader(headerNotToForward, "testValue");
        }
        request.addHeader("foo", "bar");
        request.setContent("test".getBytes());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final HeadersKnob headersKnob = ((PolicyEnforcementContext) invocationOnMock.getArguments()[0]).getRequest().getHeadersKnob();
                final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames(HEADER_TYPE_HTTP));
                assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size() + 1, headerNames.size());
                assertTrue(headerNames.contains("foo"));
                assertEquals(1, headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
                assertEquals("bar", headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
                for (final String headerNotToForward : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
                    assertTrue(headerNames.contains(headerNotToForward));
                }
                for (final Header header : headersKnob.getHeaders()) {
                    if (!header.getKey().equals("foo")) {
                        assertFalse(header.isPassThrough());
                    } else {
                        assertTrue(header.isPassThrough());
                    }
                }
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @Test
    public void requestHeadersAddedToContextFiltersHeadersIgnoresCase() throws Exception {
        for (final String headerNotToForward : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            request.addHeader(headerNotToForward.toUpperCase(), "testValue");
        }
        request.addHeader("foo", "bar");
        request.setContent("test".getBytes());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final HeadersKnob headersKnob = ((PolicyEnforcementContext) invocationOnMock.getArguments()[0]).getRequest().getHeadersKnob();
                final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames(HEADER_TYPE_HTTP));
                assertEquals(HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD.size() + 1, headerNames.size());
                assertTrue(headerNames.contains("foo"));
                assertEquals(1, headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
                assertEquals("bar", headersKnob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
                for (final String headerNotToForward : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
                    assertTrue(headerNames.contains(headerNotToForward.toUpperCase()));
                }
                for (final Header header : headersKnob.getHeaders()) {
                    if (!header.getKey().equals("foo")) {
                        assertFalse(header.isPassThrough());
                    } else {
                        assertTrue(header.isPassThrough());
                    }
                }
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @Test
    public void contextResponseHeadersAddedToResponse() throws Exception {
        request.setContent("test".getBytes());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final HeadersKnob responseHeadersKnob = ((PolicyEnforcementContext) invocationOnMock.getArguments()[0]).getResponse().getHeadersKnob();
                responseHeadersKnob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
                responseHeadersKnob.addHeader("foo", "bar2", JmsKnob.HEADER_TYPE_JMS_PROPERTY); // test only HTTP headers are passed through
                responseHeadersKnob.addHeader("doNotPassThrough", "doNotPassThrough", HEADER_TYPE_HTTP, false);
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        assertEquals(1, response.getHeaderNames().size());
        final List<Object> headerValues = response.getHeaders("foo");
        assertEquals(1, headerValues.size());
        assertEquals("bar", headerValues.get(0));
    }

    @Test
    public void requestCookiesAddedToContext() throws Exception {
        request.addHeader("Cookie", "1=a; 2=b");
        request.setContent("test".getBytes());
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HttpCookiesKnob cookiesKnob = context.getRequest().getHttpCookiesKnob();
                final Set<HttpCookie> cookies = cookiesKnob.getCookies();
                final Map<String, String> cookiesMap = new HashMap<>();
                for (final HttpCookie cookie : cookies) {
                    cookiesMap.put(cookie.getCookieName(), cookie.getCookieValue());
                }
                assertEquals(2, cookiesMap.size());
                assertEquals("a", cookiesMap.get("1"));
                assertEquals("b", cookiesMap.get("2"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @Test
    public void contextResponseCookiesAddedToResponse() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/shouldBeOverwritten", "shouldBeOverwritten", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/shouldBeOverwritten", "shouldBeOverwritten", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=test.l7tech.com; Path=/test; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=test.l7tech.com; Path=/test; Comment=test; Max-Age=60"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @Test
    public void contextResponseCookiesAddedToResponseWithSubPath() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test/foo/bar");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/shouldBeOverwritten", "shouldBeOverwritten", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/shouldBeOverwritten", "shouldBeOverwritten", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=test.l7tech.com; Path=/test/foo; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=test.l7tech.com; Path=/test/foo; Comment=test; Max-Age=60"));
    }

    @BugId("SSG-8033")
    @Test
    public void contextResponseCookiesAddedToResponseDoNotOverwriteDomain() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                context.setOverwriteResponseCookieDomain(false);
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=original; Path=/test; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=original; Path=/test; Comment=test; Max-Age=60"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @BugId("SSG-8033")
    @Test
    public void contextResponseCookiesAddedToResponseDoNotOverwritePath() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                context.setOverwriteResponseCookiePath(false);
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=test.l7tech.com; Path=/original; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=test.l7tech.com; Path=/original; Comment=test; Max-Age=60"));
    }

    @BugId("SSG-8884")
    @Test
    public void contextResponseCookiesAddedToResponseDoNotOverwriteSubPath() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                context.setOverwriteResponseCookiePath(false);
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original/with/sub/path", "original", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original/with/sub/path", "original", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=test.l7tech.com; Path=/original/with/sub/path; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=test.l7tech.com; Path=/original/with/sub/path; Comment=test; Max-Age=60"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @BugId("SSG-8033")
    @Test
    public void contextResponseCookiesAddedToResponseDoNotOverwriteDomainOrPath() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                context.setOverwriteResponseCookiePath(false);
                context.setOverwriteResponseCookieDomain(false);
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=original; Path=/original; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=original; Path=/original; Comment=test; Max-Age=60"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @Test
    public void contextResponseInvalidSetCookieHeaderAddedToResponse() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false));
                context.getResponse().getHeadersKnob().addHeader("Set-Cookie", "invalidSetCookieHeaderValue", HEADER_TYPE_HTTP);
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("foo=bar"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("invalidSetCookieHeaderValue"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @BugId("SSG-8486")
    @Test
    public void contextResponseSetCookieHeaderWithSpecialCharacterAddedToResponse() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("foo", "bar", 0, null, null, -1, false, null, false));
                context.getResponse().getHeadersKnob().addHeader("Set-Cookie", "LSID=DQAAAK…Eaem_vYg; domain=test.l7tech.com; path=/test", HEADER_TYPE_HTTP);
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("foo=bar"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("LSID=DQAAAK…Eaem_vYg; domain=test.l7tech.com; path=/test"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @BugId("SSG-8490")
    @Test
    public void contextResponseCookiesDuplicateNameDomainAndPath() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HeadersKnob headersknob = context.getResponse().getHeadersKnob();
                headersknob.addHeader("Set-Cookie", "test=value1; domain=test.l7tech.com; path=/test", HeadersKnob.HEADER_TYPE_HTTP);
                headersknob.addHeader("Set-Cookie", "test=value2; domain=test.l7tech.com; path=/test", HeadersKnob.HEADER_TYPE_HTTP);
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        List cookieHeaders = response.getHeaders("Set-Cookie");
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("test=value1; domain=test.l7tech.com; path=/test"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("test=value2; domain=test.l7tech.com; path=/test"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    @Test
    public void noResponseContentTypeHeaderUsesOuterContentType() throws Exception {
        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setRequestURI("/test");
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final Document emptyDocument = XmlUtil.createEmptyDocument();
                context.getResponse().initialize(emptyDocument, ContentTypeHeader.XML_DEFAULT);
                assertFalse(context.getResponse().getHeadersKnob().containsHeader(HttpConstants.HEADER_CONTENT_TYPE, HEADER_TYPE_HTTP));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        assertEquals(ContentTypeHeader.XML_DEFAULT.getFullValue(), response.getContentType());
    }

    @BugId("SSG-8528")
    @Test
    public void gzipRequestZeroContentLength() throws Exception {
        request.addHeader(HttpConstants.HEADER_CONTENT_ENCODING, "gzip");
        request.addHeader(HttpConstants.HEADER_CONTENT_TYPE, ContentTypeHeader.TEXT_DEFAULT.getFullValue());
        request.addHeader(HttpConstants.HEADER_CONTENT_LENGTH, "0");
        request.setContent("".getBytes());
        when(config.getBooleanProperty(ALLOW_GZIP_COMPRESSED_REQUEST, true)).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                assertFalse(context.isRequestWasCompressed());
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @Test
    public void gzipRequest() throws Exception {
        request.addHeader(HttpConstants.HEADER_CONTENT_ENCODING, "gzip");
        request.addHeader(HttpConstants.HEADER_CONTENT_TYPE, ContentTypeHeader.TEXT_DEFAULT.getFullValue());
        request.setContent(IOUtils.compressGzip("test".getBytes()));
        when(config.getBooleanProperty(ALLOW_GZIP_COMPRESSED_REQUEST, true)).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                assertTrue(context.isRequestWasCompressed());
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @BugId("DE218036")
    @Test
    public void gzipGetRequestNoContent() throws Exception {
        final String TEST_REQUEST_URL="http://testURL/test";
        final HttpServletRequest mockRequest = mock(HttpServletRequest.class);
        when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        });
        when(mockRequest.getMethod()).thenReturn(HttpConstants.METHOD_GET);
        when(mockRequest.getContentLength()).thenReturn(-1);

        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpConstants.HEADER_CONTENT_ENCODING, "gzip");

        // create an Enumeration over the header keys
        final Iterator<String> iterator = headers.keySet().iterator();
        final Enumeration headerNames = new Enumeration<String>() {
            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
        when(mockRequest.getHeader(HttpConstants.HEADER_CONTENT_ENCODING)).thenReturn("gzip");
        when(mockRequest.getHeaderNames()).thenReturn(headerNames);

        final Vector<String> headerValues = new Vector<>();
        headerValues.add("gzip");
        final Enumeration<String> headerValuesEnum = headerValues.elements();
        when(mockRequest.getHeaders(HttpConstants.HEADER_CONTENT_ENCODING)).thenReturn(headerValuesEnum);
        when(mockRequest.getRequestURL()).thenReturn(new StringBuffer(TEST_REQUEST_URL));

        when(config.getBooleanProperty(ALLOW_GZIP_COMPRESSED_REQUEST, true)).thenReturn(true);
        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HeadersKnob responseHeadersKnob = context.getResponse().getHeadersKnob();
                responseHeadersKnob.addHeader(HttpConstants.HEADER_CONTENT_TYPE, ContentTypeHeader.TEXT_DEFAULT.getFullValue(), HEADER_TYPE_HTTP);
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
        servlet.service(mockRequest, response);
        assertEquals(response.getHeader(HttpConstants.HEADER_CONTENT_TYPE), ContentTypeHeader.TEXT_DEFAULT.getFullValue());
        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));
    }

    @BugId("DE347523")
    @Test
    public void testUnencodeRequestURIWithoutFailure() throws Exception {
        // Set request URL containing some un-encoding characters such as '{' and '}'.
        final String testingUnencodedUri = "/test{}";

        request.setContent("test".getBytes());
        request.setServerName("test.l7tech.com");
        request.setServerPort(8080);
        request.setRequestURI(testingUnencodedUri);

        doAnswer(new Answer() {
            @Override
            public Object answer(final InvocationOnMock invocationOnMock) throws Throwable {
                final PolicyEnforcementContext context = (PolicyEnforcementContext) invocationOnMock.getArguments()[0];
                final HttpCookiesKnob cookiesKnob = context.getResponse().getHttpCookiesKnob();
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test", false));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test", false));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));

        // The service method has been successfully executed without any exceptions and getPassThroughHeaders() has
        // successfully processed retrieving domain and path from reqeust URL.
        try {
            servlet.service(request, response);
        } catch (Exception e) {
            fail("should not fail on this method call, service");
        }

        verify(messageProcessor).processMessageNoAudit(any(PolicyEnforcementContext.class));

        List cookieHeaders = response.getHeaders("Set-Cookie");

        // Verify domain and path are correctly overwritten in cookie headers, even though the request URL contains unwise characters.
        assertEquals(2, cookieHeaders.size());
        assertTrue("Checking first Set-Cookie header", cookieHeaders.contains("1=a; Version=1; Domain=test.l7tech.com; Path=" + testingUnencodedUri + "; Comment=test; Max-Age=60"));
        assertTrue("Checking second Set-Cookie header", cookieHeaders.contains("2=b; Version=1; Domain=test.l7tech.com; Path=" + testingUnencodedUri + "; Comment=test; Max-Age=60"));
        assertTrue("Empty cookies", response.getCookies().length == 0);
    }

    private class TestableSoapMessageProcessingServlet extends SoapMessageProcessingServlet {
        @Override
        SsgConnector getConnector(final HttpServletRequest request) {
            return connector;
        }
    }

}
