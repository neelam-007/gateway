package com.l7tech.message;

import com.l7tech.util.Pair;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class HeadersKnobSupportTest {
    private HeadersKnobSupport knob;

    @Before
    public void setup() {
        knob = new HeadersKnobSupport();
    }

    @Test
    public void addHeader() {
        knob.addHeader("1", "one");
        assertEquals(1, knob.getHeaderValues("1").length);
    }

    @Test
    public void addHeaderNullValue() {
        knob.addHeader("1", null);
        final String[] values = knob.getHeaderValues("1");
        assertEquals(1, values.length);
        assertNull(values[0]);
    }

    @Test
    public void addHeaderNameAlreadyExists() {
        knob.addHeader("1", "one");
        knob.addHeader("1", "anotherOne");
        final String[] values = knob.getHeaderValues("1");
        assertEquals(2, values.length);
        final List<String> valuesList = Arrays.asList(values);
        assertTrue(valuesList.contains("one"));
        assertTrue(valuesList.contains("anotherOne"));
    }

    @Test
    public void addHeaderNameAndValueAlreadyExist() {
        knob.addHeader("1", "one");
        knob.addHeader("1", "one");
        final String[] values = knob.getHeaderValues("1");
        assertEquals(2, values.length);
        for (final String value : values) {
            assertEquals("one", value);
        }
    }

    @Test
    public void setHeader() {
        knob.setHeader("1", "one");
        final String[] values = knob.getHeaderValues("1");
        assertEquals(1, values.length);
        assertEquals("one", values[0]);
    }

    @Test
    public void setHeaderNullValue() {
        knob.setHeader("1", null);
        final String[] values = knob.getHeaderValues("1");
        assertEquals(1, values.length);
        assertNull(values[0]);
    }

    @Test
    public void setHeaderReplacesExistingValues() {
        knob.addHeader("1", "one");
        knob.addHeader("1", "anotherOne");
        knob.setHeader("1", "superiorOne");
        final String[] values = knob.getHeaderValues("1");
        assertEquals(1, values.length);
        assertEquals("superiorOne", values[0]);
    }

    @Test
    public void setHeaderIgnoresCase() {
        knob.addHeader("foo", "bar");
        knob.addHeader("foo", "anotherOne");
        knob.setHeader("FOO", "superiorOne");
        final String[] values = knob.getHeaderValues("FOO");
        assertEquals(1, values.length);
        assertEquals("superiorOne", values[0]);
    }

    @Test
    public void getHeaderValues() {
        knob.addHeader("1", "one");
        knob.addHeader("foo", "bar");
        final String[] values = knob.getHeaderValues("1");
        assertEquals(1, values.length);
        assertEquals("one", values[0]);
    }

    @Test
    public void getHeaderValuesCaseInsensitive() {
        knob.addHeader("foo", "bar");
        final String[] values = knob.getHeaderValues("FOO");
        assertEquals(1, values.length);
        assertEquals("bar", values[0]);
    }

    @Test
    public void getHeaderValuesNotString() {
        knob.addHeader("1", new Integer(1));
        final String[] values = knob.getHeaderValues("1");
        assertEquals(1, values.length);
        assertEquals("1", values[0]);
    }

    @Test
    public void getHeaderValuesNone() {
        assertEquals(0, knob.getHeaderValues("doesnotexist").length);
    }

    @Test
    public void getHeaderNames() {
        knob.addHeader("1", "one");
        final String[] names = knob.getHeaderNames();
        assertEquals(1, names.length);
        assertEquals("1", names[0]);
    }

    @Test
    public void getHeaderNamesNone() {
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void getHeaderNamesDuplicate() {
        knob.addHeader("1", "one");
        knob.addHeader("1", "anotherOne");
        final String[] names = knob.getHeaderNames();
        assertEquals(1, names.length);
        assertEquals("1", names[0]);
    }

    @Test
    public void containsHeaderTrue() {
        knob.addHeader("1", "one");
        assertTrue(knob.containsHeader("1"));
    }

    @Test
    public void containsHeaderIgnoresCase() {
        knob.addHeader("foo", "bar");
        assertTrue(knob.containsHeader("FOO"));
    }


    @Test
    public void containsHeaderFalse() {
        assertFalse(knob.containsHeader("doesnotexist"));
    }

    @Test
    public void removeHeader() {
        knob.addHeader("foo", "bar");
        knob.addHeader("foo", "bar2");
        knob.removeHeader("foo");
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void removeHeaderIgnoresCase() {
        knob.addHeader("foo", "bar");
        knob.addHeader("foo", "bar2");
        knob.removeHeader("FOO");
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void removeHeaderWithValue() {
        knob.addHeader("foo", "bar");
        knob.addHeader("foo", "bar2");
        knob.removeHeader("foo", "bar2");
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void removeHeaderWithValueIgnoresCase() {
        knob.addHeader("foo", "bar");
        knob.addHeader("foo", "bar2");
        knob.removeHeader("FOO", "bar2");
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void removeHeaderWithNullValue() {
        knob.addHeader("foo", "bar");
        knob.addHeader("foo", null);
        knob.removeHeader("foo", null);
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void removeHeaderNotFound() {
        knob.addHeader("foo", "bar");
        knob.removeHeader("notFound");
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void removeHeaderWithValueNotFound() {
        knob.addHeader("foo", "bar");
        knob.removeHeader("notFound", "notFound");
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void getHeaders() {
        knob.addHeader("foo", "bar");
        final Collection<Pair<String, Object>> headers = knob.getHeaders();
        assertEquals(1, headers.size());
        final Pair<String, Object> header = headers.iterator().next();
        assertEquals("foo", header.getKey());
        assertEquals("bar", header.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeadersUnmodifiable() {
        knob.getHeaders().add(new Pair<String, Object>("key", "value"));
    }
}
