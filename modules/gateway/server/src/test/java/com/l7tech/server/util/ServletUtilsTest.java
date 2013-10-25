package com.l7tech.server.util;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.GenericHttpResponse;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HeadersKnobSupport;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ServletUtilsTest {
    private MockHttpServletRequest request;
    private HeadersKnob knob;
    @Mock
    private GenericHttpResponse response;
    private List<GenericHttpHeader> responseHeaders;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        knob = new HeadersKnobSupport();
        responseHeaders = new ArrayList<>();
    }

    @Test
    public void loadRequestHeaders() {
        request.addHeader("foo", "bar");
        ServletUtils.loadHeaders(request, knob);
        assertEquals(1, knob.getHeaderNames().length);
        assertEquals("foo", knob.getHeaderNames()[0]);
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void loadRequestHeadersMultipleValuesForHeader() {
        request.addHeader("foo", "bar");
        request.addHeader("foo", "bar2");
        ServletUtils.loadHeaders(request, knob);
        assertEquals(1, knob.getHeaderNames().length);
        assertEquals("foo", knob.getHeaderNames()[0]);
        assertEquals(2, knob.getHeaderValues("foo").length);
        final List<String> values = Arrays.asList(knob.getHeaderValues("foo"));
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("bar2"));
    }

    @Test
    public void loadRequestHeadersFilters() {
        for (final String shouldNotBeForwarded : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            request.addHeader(shouldNotBeForwarded, "testValue");
        }
        ServletUtils.loadHeaders(request, knob);
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void loadRequestHeadersFiltersCaseInsensitive() {
        for (final String shouldNotBeForwarded : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            request.addHeader(shouldNotBeForwarded.toUpperCase(), "testValue");
        }
        ServletUtils.loadHeaders(request, knob);
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void loadResponseHeaders() {
        responseHeaders.add(new GenericHttpHeader("foo", "bar"));
        when(response.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        ServletUtils.loadHeaders(response, knob);
        assertEquals(1, knob.getHeaderNames().length);
        assertEquals("foo", knob.getHeaderNames()[0]);
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void loadResponseHeadersMultipleValuesForHeader() {
        responseHeaders.add(new GenericHttpHeader("foo", "bar"));
        responseHeaders.add(new GenericHttpHeader("foo", "bar2"));
        when(response.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        ServletUtils.loadHeaders(response, knob);
        assertEquals(1, knob.getHeaderNames().length);
        assertEquals("foo", knob.getHeaderNames()[0]);
        assertEquals(2, knob.getHeaderValues("foo").length);
        final List<String> values = Arrays.asList(knob.getHeaderValues("foo"));
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("bar2"));
    }

    @Test
    public void loadResponseHeadersFilters() {
        for (final String shouldNotBeForwarded : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(shouldNotBeForwarded, "testValue"));
        }
        when(response.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        ServletUtils.loadHeaders(response, knob);
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void loadResponseHeadersFiltersCaseInsensitive() {
        for (final String shouldNotBeForwarded : HttpPassthroughRuleSet.HEADERS_NOT_TO_IMPLICITLY_FORWARD) {
            responseHeaders.add(new GenericHttpHeader(shouldNotBeForwarded.toUpperCase(), "testValue"));
        }
        when(response.getHeaders()).thenReturn(new GenericHttpHeaders(responseHeaders.toArray(new HttpHeader[responseHeaders.size()])));
        ServletUtils.loadHeaders(response, knob);
        assertEquals(0, knob.getHeaderNames().length);
    }
}
