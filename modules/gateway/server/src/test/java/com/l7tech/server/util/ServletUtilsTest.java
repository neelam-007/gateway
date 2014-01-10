package com.l7tech.server.util;

import com.l7tech.message.HeadersKnob;
import com.l7tech.message.HeadersKnobSupport;
import com.l7tech.policy.assertion.HttpPassthroughRuleSet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ServletUtilsTest {
    private MockHttpServletRequest request;
    private HeadersKnob knob;

    @Before
    public void setup() {
        request = new MockHttpServletRequest();
        knob = new HeadersKnobSupport();
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
}
