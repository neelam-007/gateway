package com.l7tech.message;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static com.l7tech.message.HeadersKnob.HEADER_TYPE_HTTP;
import static com.l7tech.message.JmsKnob.HEADER_TYPE_JMS_PROPERTY;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class HeadersKnobSupportTest {
    private HeadersKnobSupport knob;

    @Before
    public void setup() {
        knob = new HeadersKnobSupport();
    }

    @Test
    public void addHeader() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP, false);

        final Collection<Header> allHeaders = knob.getHeaders();
        final Collection<Header> httpHeaders = knob.getHeaders(HEADER_TYPE_HTTP);

        assertEquals(1, httpHeaders.size());
        assertEquals(httpHeaders.size(), allHeaders.size());

        final Header header = httpHeaders.iterator().next();
        assertEquals(HEADER_TYPE_HTTP, header.getType());
        assertEquals("1", header.getKey());
        assertEquals("one", header.getValue());
        assertFalse(header.isPassThrough());
    }

    @Test
    public void addHeaderNullValue() {
        knob.addHeader("1", null, HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertNull(values[0]);
    }

    @Test
    public void addHeader_HeaderTypeHttp_NameAlreadyExistsForSameType() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("1", "anotherOne", HEADER_TYPE_HTTP);

        final Collection<Header> httpHeaders = knob.getHeaders(HEADER_TYPE_HTTP);
        assertEquals(2, httpHeaders.size());

        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(2, values.length);
        final List<String> valuesList = Arrays.asList(values);
        assertTrue(valuesList.contains("one"));
        assertTrue(valuesList.contains("anotherOne"));
    }

    @Test
    public void addHeader_HeaderTypeHttp_NameAlreadyExistsForDifferentType() {
        knob.addHeader("1", "one", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("1", "anotherOne", HEADER_TYPE_HTTP);

        final Collection<Header> allHeaders = knob.getHeaders();
        final Collection<Header> httpHeaders = knob.getHeaders(HEADER_TYPE_HTTP);
        assertEquals(2, allHeaders.size());
        assertEquals(1, httpHeaders.size());

        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals(values[0], "anotherOne");
    }

    @Test
    public void addHeader_HeaderTypeJmsProperty_NameAndValueAlreadyExistForSameType() {
        knob.addHeader("1", "one", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("1", "one", HEADER_TYPE_JMS_PROPERTY);

        final Collection<Header> jmsPropertyHeaders = knob.getHeaders(HEADER_TYPE_JMS_PROPERTY);
        assertEquals(2, jmsPropertyHeaders.size());

        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_JMS_PROPERTY);
        assertEquals(2, values.length);
        for (final String value : values) {
            assertEquals("one", value);
        }
    }

    @Test
    public void addHeader_HeaderTypeJmsProperty_NameAndValueAlreadyExistForDifferentType() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("1", "one", HEADER_TYPE_JMS_PROPERTY);

        final Collection<Header> allHeaders = knob.getHeaders();
        final Collection<Header> jmsPropertyHeaders = knob.getHeaders(HEADER_TYPE_JMS_PROPERTY);
        assertEquals(2, allHeaders.size());
        assertEquals(1, jmsPropertyHeaders.size());

        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_JMS_PROPERTY);
        assertEquals(1, values.length);
        assertEquals(values[0], "one");
    }

    @Test
    public void setHeader() {
        knob.setHeader("1", "one", HEADER_TYPE_HTTP, false);

        final Collection<Header> headers = knob.getHeaders();
        assertEquals(1, headers.size());

        final Header header = headers.iterator().next();
        assertEquals(HEADER_TYPE_HTTP, header.getType());
        assertEquals("1", header.getKey());
        assertEquals("one", header.getValue());
        assertFalse(header.isPassThrough());
    }

    @Test
    public void setHeaderNullValue() {
        knob.setHeader("1", null, HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertNull(values[0]);
    }

    @Test
    public void setHeaderReplacesExistingValues() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("1", "anotherOne", HEADER_TYPE_HTTP);
        knob.setHeader("1", "superiorOne", HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals("superiorOne", values[0]);
    }

    @Test
    public void setHeader_NameMatchesAllAndTypeMatchesSubset_ReplacesExistingValuesForSubset() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("1", "anotherOne", HEADER_TYPE_HTTP);
        knob.addHeader("1", "foo", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("1", "anotherFoo", HEADER_TYPE_JMS_PROPERTY);

        assertEquals(4, knob.getHeaders().size());
        assertEquals(4, knob.getHeaderValues("1").length);

        knob.setHeader("1", "superiorFoo", HEADER_TYPE_JMS_PROPERTY);

        assertEquals(3, knob.getHeaders().size());
        assertEquals(2, knob.getHeaders(HEADER_TYPE_HTTP).size());
        assertEquals(1, knob.getHeaders(HEADER_TYPE_JMS_PROPERTY).size());

        final List<String> values = Arrays.asList(knob.getHeaderValues("1"));

        assertEquals(3, values.size());
        assertTrue(values.contains("one"));
        assertTrue(values.contains("anotherOne"));
        assertTrue(values.contains("superiorFoo"));
    }

    @Test
    public void setHeaderIgnoresCase() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "anotherOne", HEADER_TYPE_HTTP);
        knob.setHeader("FOO", "superiorOne", HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("FOO", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals("superiorOne", values[0]);
    }

    @Test
    public void getHeaderValues_TypeMatchesAllAndNameMatchesSubset_MatchingSubsetReturned() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals("one", values[0]);
    }

    @Test
    public void getHeaderValues_NameMatchesAllAndTypeMatchesSubset_MatchingSubsetReturned() {
        knob.addHeader("foo", "one", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "two", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);

        final List<String> values = Arrays.asList(knob.getHeaderValues("foo", HEADER_TYPE_HTTP));

        assertEquals(2, values.size());
        assertTrue(values.contains("one"));
        assertTrue(values.contains("two"));
    }

    @Test
    public void getHeaderValues_NameMatchesMultiTypeSubsetAndTypeParameterNull_MatchingMultiTypeSubsetReturned() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "one", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("foo2", "two", HEADER_TYPE_JMS_PROPERTY);

        final List<String> values = Arrays.asList(knob.getHeaderValues("foo", null));

        assertEquals(2, values.size());
        assertTrue(values.contains("bar"));
        assertTrue(values.contains("one"));
    }

    @Test
    public void getHeaderValuesFilterNonPassThrough() {
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("1", "anotherOne", HEADER_TYPE_HTTP);
        knob.addHeader("1", "anotherAnotherOne", HEADER_TYPE_HTTP, false);
        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP, false);
        assertEquals(2, values.length);
        assertEquals("one", values[0]);
        assertEquals("anotherOne", values[1]);
    }

    @Test
    public void getHeaderValuesCaseInsensitive() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("FOO", "BAR", HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("FOO", HEADER_TYPE_HTTP);
        assertEquals(2, values.length);
        assertEquals("bar", values[0]);
        assertEquals("BAR", values[1]);
    }

    @Test
    public void getHeaderValuesNotString() {
        knob.addHeader("1", new Integer(1), HEADER_TYPE_HTTP);
        final String[] values = knob.getHeaderValues("1", HEADER_TYPE_HTTP);
        assertEquals(1, values.length);
        assertEquals("1", values[0]);
    }

    @Test
    public void getHeaderValuesNone() {
        assertEquals(0, knob.getHeaderValues("doesNotExist", HEADER_TYPE_HTTP).length);
    }

    @Test
    public void getHeaderNames_NoParameters_AllNamesReturned() {
        knob.addHeader("name1", "val", HEADER_TYPE_HTTP);
        knob.addHeader("name2", "val", HEADER_TYPE_JMS_PROPERTY);

        final List<String> names = Arrays.asList(knob.getHeaderNames());

        assertEquals(2, names.size());
        assertTrue(names.contains("name1"));
        assertTrue(names.contains("name2"));
    }

    @Test
    public void getHeaderNames_NoHeadersPresent_NoNamesReturned() {
        assertEquals(0, knob.getHeaders().size());
        assertEquals(0, knob.getHeaderNames().length);
    }

    @Test
    public void getHeaderNames_TypeMatchesSubset_SubsetNamesReturned() {
        knob.addHeader("name1", "val", HEADER_TYPE_HTTP);
        knob.addHeader("name2", "val", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("name3", "val", HEADER_TYPE_JMS_PROPERTY);

        final String[] names = knob.getHeaderNames(HEADER_TYPE_HTTP);
        assertEquals(1, names.length);
        assertEquals("name1", names[0]);
    }

    @Test
    public void getHeaderNames_TypeDoesNotMatch_NoNamesReturned() {
        knob.addHeader("name1", "val", HEADER_TYPE_HTTP);
        knob.addHeader("name2", "val", HEADER_TYPE_HTTP);
        knob.addHeader("name3", "val", HEADER_TYPE_HTTP);

        assertEquals(0, knob.getHeaderNames(HEADER_TYPE_JMS_PROPERTY).length);
    }

    @Test
    public void getHeaderNamesIgnoresCase() {
        knob.addHeader("foo", "lowerCase", HEADER_TYPE_HTTP);
        knob.addHeader("FOO", "upperCase", HEADER_TYPE_HTTP);
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        knob.addHeader("1", "anotherOne", HEADER_TYPE_HTTP);
        final List<String> names = Arrays.asList(knob.getHeaderNames(HEADER_TYPE_HTTP));
        assertEquals(2, names.size());
        assertTrue(names.contains("foo"));
        assertTrue(names.contains("1"));
    }

    @Test
    public void getHeaderNamesFilterNonPassThrough() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("1", "one", HEADER_TYPE_HTTP, false);
        final List<String> names = Arrays.asList(knob.getHeaderNames(HEADER_TYPE_HTTP, false, true));
        assertEquals(1, names.size());
        assertTrue(names.contains("foo"));
    }

    @Test
    public void containsHeader_NameAndTypeGivenMatch_ReturnTrue() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("1", "one", HEADER_TYPE_HTTP);
        assertTrue(knob.containsHeader("1", HEADER_TYPE_HTTP));
    }

    @Test
    public void containsHeader_NameAndTypeGivenMatchButNameCaseDifferent_ReturnTrue() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        assertTrue(knob.containsHeader("FOO", HEADER_TYPE_HTTP));
    }

    @Test
    public void containsHeader_NameGivenMatchesButTypeGivenDoesNot_ReturnFalse() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        assertFalse(knob.containsHeader("foo", HEADER_TYPE_JMS_PROPERTY));
    }

    @Test
    public void containsHeader_TypeGivenMatchesButNameGivenDoesNot_ReturnFalse() {
        knob.addHeader("foo", "bar", HEADER_TYPE_JMS_PROPERTY);
        assertFalse(knob.containsHeader("doesNotExist", HEADER_TYPE_JMS_PROPERTY));
    }

    @Test
    public void removeHeader_NameMatchesCaseInsensitiveAndTypeMatches_AllHeadersRemoved() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("FOO", "bar2", HEADER_TYPE_HTTP);
        knob.removeHeader("Foo", HEADER_TYPE_HTTP);
        assertEquals(0, knob.getHeaders(HEADER_TYPE_HTTP).size());
    }

    @Test
    public void removeHeader_NameMatchesButTypeDoesNot_NoHeadersRemoved() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaders().size());

        knob.removeHeader("foo", HEADER_TYPE_JMS_PROPERTY);
        assertEquals(1, knob.getHeaders().size());
    }

    @Test
    public void removeHeader_NameMatchesAllAndTypeMatchesSubset_MatchingTypeSubsetRemoved() {
        knob.addHeader("foo", "bar1", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "bar1", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "bar2", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("foo", "bar2", HEADER_TYPE_JMS_PROPERTY);
        assertEquals(4, knob.getHeaders().size());

        knob.removeHeader("foo", HEADER_TYPE_JMS_PROPERTY);
        assertEquals(2, knob.getHeaders().size());

        // ensure the correct headers remain
        for (Header header : knob.getHeaders()) {
            assertEquals(HEADER_TYPE_HTTP, header.getType());
            assertEquals("foo", header.getKey());
            assertEquals("bar1", header.getValue());
        }
    }

    @Test
    public void removeHeaderWithValue() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        knob.removeHeader("foo", "bar2", HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void removeHeaderByNameAndValue_NameAndTypeMatchButValueCaseDoesNot_NoHeadersRemoved() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.removeHeader("foo", "BAR", HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void removeHeader_NameMatchesAllAndValueMatchesMultiTypeSubsetAndTypeMatchesSubset_MatchingSubsetRemoved() {
        knob.addHeader("foo", "val1", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "val2", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "val2", HEADER_TYPE_JMS_PROPERTY);
        knob.addHeader("foo", "val2", HEADER_TYPE_JMS_PROPERTY);

        assertEquals(4, knob.getHeaders().size());
        assertEquals(2, knob.getHeaders(HEADER_TYPE_HTTP).size());
        assertEquals(2, knob.getHeaders(HEADER_TYPE_JMS_PROPERTY).size());

        knob.removeHeader("foo", "val2", HEADER_TYPE_HTTP);

        assertEquals(3, knob.getHeaders().size());
        assertEquals(1, knob.getHeaders(HEADER_TYPE_HTTP).size());
        assertEquals(2, knob.getHeaders(HEADER_TYPE_JMS_PROPERTY).size());

        // ensure the correct HTTP header remains
        Header header = knob.getHeaders(HEADER_TYPE_HTTP).iterator().next();

        assertEquals(HEADER_TYPE_HTTP, header.getType());
        assertEquals("foo", header.getKey());
        assertEquals("val1", header.getValue());
    }

    @Test
    public void removeHeaderWithValueIgnoresNameCase() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("foo", "bar2", HEADER_TYPE_HTTP);
        knob.removeHeader("FOO", "bar2", HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void removeHeaderWithNullValue() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("foo", null, HEADER_TYPE_HTTP);
        knob.removeHeader("foo", null, HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void removeHeaderNotFound() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.removeHeader("notFound", HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void removeHeaderWithValueNotFound() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.removeHeader("notFound", "notFound", HEADER_TYPE_HTTP);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void getHeaders_HeadersWithMultipleTypesPresent_AllReturned() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("FOO", "BAR", HEADER_TYPE_JMS_PROPERTY, false);
        final Collection<Header> headers = knob.getHeaders();
        assertEquals(2, headers.size());
        final Iterator<Header> iterator = headers.iterator();
        final Header header1 = iterator.next();
        assertEquals(HEADER_TYPE_HTTP, header1.getType());
        assertEquals("foo", header1.getKey());
        assertEquals("bar", header1.getValue());
        final Header header2 = iterator.next();
        assertEquals(HEADER_TYPE_JMS_PROPERTY, header2.getType());
        assertEquals("FOO", header2.getKey());
        assertEquals("BAR", header2.getValue());
    }

    @Test
    public void getHeaders_HeaderTypeHttp_HttpHeadersReturned() {
        // add headers of different types
        knob.addHeader("http1", "value", HEADER_TYPE_HTTP);
        knob.addHeader("jms1", "value", HEADER_TYPE_JMS_PROPERTY);

        // get all headers, get subset of headers by type
        final Collection<Header> allHeaders = knob.getHeaders();
        final Collection<Header> httpHeaders = knob.getHeaders(HEADER_TYPE_HTTP);

        // check for expected number of headers
        assertEquals(2, allHeaders.size());
        assertEquals(1, httpHeaders.size());

        // check correct header returned
        final Header httpHeader = httpHeaders.iterator().next();
        assertEquals(HEADER_TYPE_HTTP, httpHeader.getType());
        assertEquals("http1", httpHeader.getKey());
        assertEquals("value", httpHeader.getValue());
    }

    @Test
    public void getHeaders_HeaderTypeJmsProperty_JmsPropertyHeadersReturned() {
        // add headers of different types
        knob.addHeader("http1", "value", HEADER_TYPE_HTTP);
        knob.addHeader("jms1", "value", HEADER_TYPE_JMS_PROPERTY);

        // get all headers, get subset of headers by type
        final Collection<Header> allHeaders = knob.getHeaders();
        final Collection<Header> jmsPropertyHeaders = knob.getHeaders(HEADER_TYPE_JMS_PROPERTY);

        // check for expected number of headers
        assertEquals(2, allHeaders.size());
        assertEquals(1, jmsPropertyHeaders.size());

        // check correct header returned
        final Header jmsHeader = jmsPropertyHeaders.iterator().next();
        assertEquals(HEADER_TYPE_JMS_PROPERTY, jmsHeader.getType());
        assertEquals("jms1", jmsHeader.getKey());
        assertEquals("value", jmsHeader.getValue());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getHeadersUnmodifiable() {
        knob.getHeaders().add(new Header("key", "value", HEADER_TYPE_HTTP));
    }

    @Test
    public void getHeadersFilterNonPassThrough() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("FOO", "BAR", HEADER_TYPE_HTTP, false);
        final Collection<Header> headers = knob.getHeaders(HEADER_TYPE_HTTP, false);
        assertEquals(1, headers.size());
        final Header header = headers.iterator().next();
        assertEquals("foo", header.getKey());
        assertEquals("bar", header.getValue());
    }

    @Test
    public void removeHeaderCaseSensitive() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.removeHeader("Foo", HEADER_TYPE_HTTP, true);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void getHeadersByName() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("FOO", "BAR", HEADER_TYPE_HTTP, false);
        knob.addHeader("notFoo", "notFoo", HEADER_TYPE_HTTP);
        final Collection<Header> headers = knob.getHeaders("foo", HEADER_TYPE_HTTP);
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
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.addHeader("doNotPass", "doNotPass", HEADER_TYPE_HTTP, false);
        final Collection<Header> headers = knob.getHeaders("foo", HEADER_TYPE_HTTP, false);
        assertEquals(1, headers.size());
        final Header header = headers.iterator().next();
        assertEquals("foo", header.getKey());
        assertEquals("bar", header.getValue());
    }

    @Test
    public void removeHeaderByValueCaseSensitive() {
        knob.addHeader("foo", "bar", HEADER_TYPE_HTTP);
        knob.removeHeader("Foo", "bar", HEADER_TYPE_HTTP, true);
        assertEquals(1, knob.getHeaderValues("foo", HEADER_TYPE_HTTP).length);
        assertEquals("bar", knob.getHeaderValues("foo", HEADER_TYPE_HTTP)[0]);
    }

    @Test
    public void removeHeaderByValueMultivalued() {
        knob.addHeader("foo", "value1,value2,value3", HEADER_TYPE_HTTP);
        knob.removeHeader("foo", "value2", HEADER_TYPE_HTTP);
        final String[] fooValues = knob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("value1,value3", fooValues[0]);
    }

    @Test
    public void removeHeaderByValueMultivaluedTrimsWhitespace() {
        // whitespace after the comma separator
        knob.addHeader("foo", "value1, value2, value3", HEADER_TYPE_HTTP);
        knob.removeHeader("foo", "value2", HEADER_TYPE_HTTP);
        final String[] fooValues = knob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("value1,value3", fooValues[0]);
    }

    @Test
    public void removeHeaderByValueMultivaluedNotFound() {
        knob.addHeader("foo", "value1, value2", HEADER_TYPE_HTTP);
        knob.removeHeader("foo", "doesNotExist", HEADER_TYPE_HTTP);
        final String[] fooValues = knob.getHeaderValues("foo", HEADER_TYPE_HTTP);
        assertEquals(1, fooValues.length);
        assertEquals("value1,value2", fooValues[0]);
    }
}
