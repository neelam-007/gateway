package com.l7tech.message;

import com.l7tech.common.http.GenericHttpHeader;
import com.l7tech.common.http.GenericHttpHeaders;
import com.l7tech.common.http.HttpHeader;
import com.l7tech.common.http.SimpleHttpHeadersHaver;
import com.l7tech.common.mime.ContentTypeHeader;
import static org.junit.Assert.*;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class HttpInboundRequestFacetTest {
    @Test
    public void testAttachKnob() throws Exception {
        Message mess = new Message();
        assertNull(mess.getKnob(HttpInboundResponseKnob.class));

        HttpInboundResponseFacet facet = new HttpInboundResponseFacet();
        mess.attachKnob(HttpInboundResponseKnob.class, facet);
        assertTrue(mess.getKnob(HttpInboundResponseKnob.class) == facet);

        mess.close();
        assertNull(mess.getKnob(HttpInboundResponseKnob.class));
    }

    @Test
    public void testAddHeaders() throws Exception {
        HttpInboundResponseFacet facet = new HttpInboundResponseFacet();
        HttpHeader[] headers = new HttpHeader[] {
                new GenericHttpHeader("Content-Type", "text/xml"),
                new GenericHttpHeader("Content-Length", "535235"),
                new GenericHttpHeader("Foop-Bloop", ""),
                new GenericHttpHeader("Content-Length", "535235"),
                new GenericHttpHeader("Content-Type", "foo/bar"),
                new GenericHttpHeader("Content-Type", "application/xml+blah"),
                new GenericHttpHeader("Content-Length", "222"),
                ContentTypeHeader.XML_DEFAULT,
        };
        facet.setHeaderSource(new SimpleHttpHeadersHaver(new GenericHttpHeaders(headers)));

        assertTrue(facet.containsHeader("content-type"));
        assertTrue(facet.containsHeader("foop-bloop"));
        assertFalse(facet.containsHeader("poof-poolb"));
        assertFalse(facet.containsHeader("contenttype"));
        assertFalse(facet.containsHeader("content-type "));
        assertTrue(facet.containsHeader("ConTENt-tYPe"));
        assertTrue(facet.containsHeader("Content-length"));
        assertFalse(facet.containsHeader("\tContent-Type"));

        assertHeaderValues(facet, "asdfqwer");
        assertHeaderValues(facet, "content-type", "text/xml", "foo/bar", "application/xml+blah", ContentTypeHeader.XML_DEFAULT.getFullValue());
        assertHeaderValues(facet, "coNTent-lenGth", "535235", "535235", "222");
        assertHeaderValues(facet, "FooP-BLoop", "");
    }

    private void assertHeaderValues(HasHeaders h, String header, String... expectedValueStrings) {
        Set<String> expectedValues = new HashSet<String>(Arrays.asList(expectedValueStrings));
        String[] actualValueStrings = h.getHeaderValues(header);
        assertEquals("Correct number of raw values present", expectedValueStrings.length, actualValueStrings.length);
        Set<String> actualValues = new HashSet<String>(Arrays.asList(actualValueStrings));
        assertEquals("Correct number of unique values present", expectedValues.size(), actualValues.size());
        assertTrue("All expected values were present", actualValues.containsAll(expectedValues));
        assertTrue("All values present were expected", expectedValues.containsAll(actualValues));
    }
}
