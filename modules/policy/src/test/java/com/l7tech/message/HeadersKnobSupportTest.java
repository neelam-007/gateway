package com.l7tech.message;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
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
        knob.addHeader("1", "one", false);
        final Collection<Header> headers = knob.getHeaders();
        assertEquals(1, headers.size());
        final Header header = headers.iterator().next();
        assertEquals("1", header.getKey());
        assertEquals("one", header.getValue());
        assertFalse(header.isPassThrough());
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
        knob.setHeader("1", "one", false);
        final Collection<Header> headers = knob.getHeaders();
        assertEquals(1, headers.size());
        final Header header = headers.iterator().next();
        assertEquals("1", header.getKey());
        assertEquals("one", header.getValue());
        assertFalse(header.isPassThrough());
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
    public void getHeaderValuesFilterNonPassThrough() {
        knob.addHeader("1", "one");
        knob.addHeader("1", "anotherOne");
        knob.addHeader("1", "anotherAnotherOne", false);
        final String[] values = knob.getHeaderValues("1", false);
        assertEquals(2, values.length);
        assertEquals("one", values[0]);
        assertEquals("anotherOne", values[1]);
    }

    @Test
    public void getHeaderValuesCaseInsensitive() {
        knob.addHeader("foo", "bar");
        knob.addHeader("FOO", "BAR");
        final String[] values = knob.getHeaderValues("FOO");
        assertEquals(2, values.length);
        assertEquals("bar", values[0]);
        assertEquals("BAR", values[1]);
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
    public void getHeaderNamesIgnoresCase() {
        knob.addHeader("foo", "lowerCase");
        knob.addHeader("FOO", "upperCase");
        knob.addHeader("1", "one");
        knob.addHeader("1", "anotherOne");
        final List<String> names = Arrays.asList(knob.getHeaderNames());
        assertEquals(2, names.size());
        assertTrue(names.contains("foo"));
        assertTrue(names.contains("1"));
    }

    @Test
    public void getHeaderNamesFilterNonPassThrough() {
        knob.addHeader("foo", "bar");
        knob.addHeader("1", "one", false);
        final List<String> names = Arrays.asList(knob.getHeaderNames(false, true));
        assertEquals(1, names.size());
        assertTrue(names.contains("foo"));
    }

    @Test
    public void containsHeaderTrue() {
        knob.addHeader("foo", "bar");
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
    public void removeHeaderWithValueCaseSensitive() {
        knob.addHeader("foo", "bar");
        knob.removeHeader("foo", "BAR");
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void removeHeaderWithValueIgnoresNameCase() {
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
        knob.addHeader("FOO", "BAR", false);
        final Collection<Header> headers = knob.getHeaders();
        assertEquals(2, headers.size());
        final Iterator<Header> iterator = headers.iterator();
        final Header header1 = iterator.next();
        assertEquals("foo", header1.getKey());
        assertEquals("bar", header1.getValue());
        final Header header2 = iterator.next();
        assertEquals("FOO", header2.getKey());
        assertEquals("BAR", header2.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeadersUnmodifiable() {
        knob.getHeaders().add(new Header("key", "value"));
    }

    @Test
    public void getHeadersFilterNonPassThrough() {
        knob.addHeader("foo", "bar");
        knob.addHeader("FOO", "BAR", false);
        final Collection<Header> headers = knob.getHeaders(false);
        assertEquals(1, headers.size());
        final Header header = headers.iterator().next();
        assertEquals("foo", header.getKey());
        assertEquals("bar", header.getValue());
    }

    @Test
    public void removeHeaderCaseSensitive() {
        knob.addHeader("foo", "bar");
        knob.removeHeader("Foo", true);
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void getHeadersByName() {
        knob.addHeader("foo", "bar");
        knob.addHeader("FOO", "BAR", false);
        knob.addHeader("notFoo", "notFoo");
        final Collection<Header> headers = knob.getHeaders("foo");
        assertEquals(2, headers.size());
        final Iterator<Header> iterator = headers.iterator();
        final Header first = iterator.next();
        assertEquals("foo", first.getKey());
        assertEquals("bar", first.getValue());
        assertTrue(first.isPassThrough());
        final Header second = iterator.next();
        assertEquals("FOO", second.getKey());
        assertEquals("BAR", second.getValue());
        assertFalse(second.isPassThrough());
    }

    @Test
    public void getHeadersByNameFilterNonPassThrough() {
        knob.addHeader("foo", "bar");
        knob.addHeader("doNotPass", "doNotPass", false);
        final Collection<Header> headers = knob.getHeaders("foo", false);
        assertEquals(1, headers.size());
        final Header header = headers.iterator().next();
        assertEquals("foo", header.getKey());
        assertEquals("bar", header.getValue());
    }

    @Test
    public void removeHeaderByValueCaseSensitive() {
        knob.addHeader("foo", "bar");
        knob.removeHeader("Foo", "bar", true);
        assertEquals(1, knob.getHeaderValues("foo").length);
        assertEquals("bar", knob.getHeaderValues("foo")[0]);
    }

    @Test
    public void removeHeaderByValueMultivalued() {
        knob.addHeader("foo", "value1,value2,value3");
        knob.removeHeader("foo", "value2");
        final String[] fooValues = knob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("value1,value3", fooValues[0]);
    }

    @Test
    public void removeHeaderByValueMultivaluedTrimsWhitespace() {
        // whitespace after the comma separator
        knob.addHeader("foo", "value1, value2, value3");
        knob.removeHeader("foo", "value2");
        final String[] fooValues = knob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("value1,value3", fooValues[0]);
    }

    @Test
    public void removeHeaderByValueMultivaluedNotFound() {
        knob.addHeader("foo", "value1, value2");
        knob.removeHeader("foo", "doesNotExist");
        final String[] fooValues = knob.getHeaderValues("foo");
        assertEquals(1, fooValues.length);
        assertEquals("value1,value2", fooValues[0]);
    }
}
