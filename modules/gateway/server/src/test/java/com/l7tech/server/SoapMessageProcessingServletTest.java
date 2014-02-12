package com.l7tech.server;

import com.l7tech.common.http.HttpCookie;
import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HttpCookiesKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.test.BugId;
import com.l7tech.util.Config;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

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

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        servlet = new TestableSoapMessageProcessingServlet();
        servlet.setLicenseManager(licenseManager);
        servlet.setConfig(config);
        servlet.setStashManagerFactory(stashManagerFactory);
        servlet.setMessageProcessor(messageProcessor);
        connector = new SsgConnector();
        connector.setEndpoints(SsgConnector.Endpoint.MESSAGE_INPUT.name());
        when(stashManagerFactory.createStashManager()).thenReturn(stashManager);
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
                final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames());
                assertEquals(2, headerNames.size());
                assertTrue(headerNames.contains("test"));
                assertTrue(headerNames.contains("foo"));
                assertEquals(1, headersKnob.getHeaderValues("test").length);
                assertEquals("test", headersKnob.getHeaderValues("test")[0]);
                final List<String> fooValues = Arrays.asList(headersKnob.getHeaderValues("foo"));
                assertEquals(2, fooValues.size());
                assertTrue(fooValues.contains("bar"));
                assertTrue(fooValues.contains("bar2"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
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
                final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames());
                assertEquals(1, headerNames.size());
                assertTrue(headerNames.contains("foo"));
                assertEquals(1, headersKnob.getHeaderValues("foo").length);
                assertEquals("bar", headersKnob.getHeaderValues("foo")[0]);
                for (final String headerNotToForward : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
                    assertFalse(headerNames.contains(headerNotToForward));
                }
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
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
                final List<String> headerNames = Arrays.asList(headersKnob.getHeaderNames());
                assertEquals(1, headerNames.size());
                assertTrue(headerNames.contains("foo"));
                assertEquals(1, headersKnob.getHeaderValues("foo").length);
                assertEquals("bar", headersKnob.getHeaderValues("foo")[0]);
                for (final String headerNotToForward : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
                    assertFalse(headerNames.contains(headerNotToForward));
                    assertFalse(headerNames.contains(headerNotToForward.toUpperCase()));
                }
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
    }

    @Test
    public void contextResponseHeadersAddedToResponse() throws Exception {
        request.setContent("test".getBytes());
        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                final HeadersKnob responseHeadersKnob = ((PolicyEnforcementContext) invocationOnMock.getArguments()[0]).getResponse().getHeadersKnob();
                responseHeadersKnob.addHeader("foo", "bar");
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        assertEquals(1, response.getHeaderNames().size());
        final List<Object> headerValues = response.getHeaders("foo");
        assertEquals(1, headerValues.size());
        assertEquals("bar", headerValues.get(0));
    }

    @Test
    public void requestCookiesAddedToContext() throws Exception {
        request.setCookies(new Cookie("1", "a"), new Cookie("2", "b"));
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
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
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
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/shouldBeOverwritten", "shouldBeOverwritten", 60, false, "test"));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/shouldBeOverwritten", "shouldBeOverwritten", 60, false, "test"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        final Map<String, String> cookiesMap = new HashMap<>();
        final Cookie[] cookies = response.getCookies();
        for (final Cookie cookie : cookies) {
            cookiesMap.put(cookie.getName(), cookie.getValue());
            // by default the gateway will overwrite the cookie domain and path before sending the response
            assertEquals("test.l7tech.com", cookie.getDomain());
            assertEquals("/test", cookie.getPath());
        }
        assertEquals(2, cookiesMap.size());
        assertEquals("a", cookiesMap.get("1"));
        assertEquals("b", cookiesMap.get("2"));
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
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test"));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        final Map<String, String> cookiesMap = new HashMap<>();
        final Cookie[] cookies = response.getCookies();
        for (final Cookie cookie : cookies) {
            cookiesMap.put(cookie.getName(), cookie.getValue());
            assertEquals("original", cookie.getDomain());
            assertEquals("/test", cookie.getPath());
        }
        assertEquals(2, cookiesMap.size());
        assertEquals("a", cookiesMap.get("1"));
        assertEquals("b", cookiesMap.get("2"));
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
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test"));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        final Map<String, String> cookiesMap = new HashMap<>();
        final Cookie[] cookies = response.getCookies();
        for (final Cookie cookie : cookies) {
            cookiesMap.put(cookie.getName(), cookie.getValue());
            assertEquals("test.l7tech.com", cookie.getDomain());
            assertEquals("/original", cookie.getPath());
        }
        assertEquals(2, cookiesMap.size());
        assertEquals("a", cookiesMap.get("1"));
        assertEquals("b", cookiesMap.get("2"));
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
                cookiesKnob.addCookie(new HttpCookie("1", "a", 1, "/original", "original", 60, false, "test"));
                cookiesKnob.addCookie(new HttpCookie("2", "b", 1, "/original", "original", 60, false, "test"));
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        final Map<String, String> cookiesMap = new HashMap<>();
        final Cookie[] cookies = response.getCookies();
        for (final Cookie cookie : cookies) {
            cookiesMap.put(cookie.getName(), cookie.getValue());
            assertEquals("original", cookie.getDomain());
            assertEquals("/original", cookie.getPath());
        }
        assertEquals(2, cookiesMap.size());
        assertEquals("a", cookiesMap.get("1"));
        assertEquals("b", cookiesMap.get("2"));
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
                cookiesKnob.addCookie(new HttpCookie("foo", "bar", 0, null, null, -1, false, null));
                context.getResponse().getHeadersKnob().addHeader("Set-Cookie", "invalidSetCookieHeaderValue");
                return AssertionStatus.NONE;
            }
        }).when(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        servlet.service(request, response);
        verify(messageProcessor).processMessage(any(PolicyEnforcementContext.class));
        final Cookie[] cookies = response.getCookies();
        assertEquals(1, cookies.length);
        assertEquals("foo", cookies[0].getName());
        assertEquals("bar", cookies[0].getValue());
        assertEquals("invalidSetCookieHeaderValue", response.getHeader("Set-Cookie"));
    }

    private class TestableSoapMessageProcessingServlet extends SoapMessageProcessingServlet {
        @Override
        SsgConnector getConnector(final HttpServletRequest request) {
            return connector;
        }
    }
}
