package com.l7tech.server.policy.variable;

import com.l7tech.util.DateUtils;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * @see ExpandVariablesTest for test coverage of supported suffixes
 * @author darmstrong
 */
@SuppressWarnings({"JavaDoc"})
public class DateTimeSelectorTest {

    @Test
    public void testGetPattern_Identity() throws Exception {
        testGetPattern(DateUtils.ISO8601_PATTERN);
    }

    @Test
    public void testGetPattern_NoEscapes() throws Exception {
        testGetPattern("yyyy-MM-ddHH:mm:ss.SSSXXX");
    }

    @Test
    public void testGetPattern_DoubleEscapes() throws Exception {
        testGetPattern("yyyy-MM-dd'T'HH:mm:ss.'T'SSSXXX");
    }

    @Test
    public void testGetPattern_NoNotEscapedPatternChars() throws Exception {
        testGetPattern("'yyyy-MM-ddHH:mm:ss.SSSXXX'");
    }

    @Test
    public void testGetPattern_NoPatternChars() throws Exception {
        testGetPattern("+6");
    }

    @Test
    public void testPatternSuffixes() throws Exception {
        final Map<String,String> formats = DateTimeSelector.builtInSuffixFormats;
        DateTimeSelector sel = new DateTimeSelector();
        for (Map.Entry<String, String> entry : formats.entrySet()) {
            assertEquals(entry.getValue(), sel.getPattern(entry.getKey()));
        }
    }

    // - PRIVATE

    private void testGetPattern(final String pattern) {
        DateTimeSelector sel = new DateTimeSelector();
        final String output = sel.getPattern(pattern);
        assertEquals("Output should be the same as input", pattern, output);
    }
}
