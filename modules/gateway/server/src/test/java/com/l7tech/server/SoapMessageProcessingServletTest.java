package com.l7tech.server;

import com.l7tech.common.mime.StashManager;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.message.HeadersKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import com.l7tech.server.message.PolicyEnforcementContext;
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

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

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

    private class TestableSoapMessageProcessingServlet extends SoapMessageProcessingServlet {
        @Override
        SsgConnector getConnector(final HttpServletRequest request) {
            return connector;
        }
    }
}
