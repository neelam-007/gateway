package com.l7tech.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

@SuppressWarnings({"JavaDoc"})
public class DateConfigUtilsTest {
    @Before
    public void setUp() throws Exception {

        // configure supported date formats
        List<Pair<String, Pattern>> autoFormats = getAutoFormats();

        dateConfigUtils = new DateTimeConfigUtils();
        dateConfigUtils.setAutoDateFormats(autoFormats);

    }

    @Test
    public void testParseAutoFormat() throws Exception {
        final Date date = dateConfigUtils.parseDateFromString("2012-05-07T23:12:24.567Z");
        validateDate(date, true);
    }

    @Test
    public void testParseMilliSecondTimestamp() throws Exception {
        //timestamp for 2012-05-07T23:12:24.567Z
        final Date date = dateConfigUtils.parseDateFromString("1336432344567");
        validateDate(date, true);
    }

    @Test
    public void testParseSecondTimestamp() throws Exception {
        //timestamp for 2012-05-07T23:12:24.567Z
        final Date date = dateConfigUtils.parseDateFromString("1336432344");
        validateDate(date, false);
    }

    /**
     * No built in format matched the date string
     */
    @Test(expected = DateTimeConfigUtils.UnknownDateFormatException.class)
    public void testParseAutoFormatNotMatched() throws Exception {
        dateConfigUtils.parseDateFromString("Sun, 06--Nov--94 08:49:37 GMT");
    }

    // - PRIVATE

    private DateTimeConfigUtils dateConfigUtils;

    /**
     * Validates the date represents 2012-05-07T23:12:24.567Z
     * @param date to validate
     * @param validateMillis true if milliseconds should be validated.
     */
    private void validateDate(Date date, boolean validateMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("Z"));
        cal.setTime(date);
        assertNotNull(date);

        assertEquals("Incorrect year", 2012, cal.get(Calendar.YEAR));
        assertEquals("Incorrect month", 4, cal.get(Calendar.MONTH));
        assertEquals("Incorrect day", 7, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("Incorrect hour", 23, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Incorrect minute", 12, cal.get(Calendar.MINUTE));
        assertEquals("Incorrect second", 24, cal.get(Calendar.SECOND));
        if (validateMillis) {
            assertEquals("Incorrect millisecond", 567, cal.get(Calendar.MILLISECOND));
        }
    }

    private List<Pair<String, Pattern>> getAutoFormats(){
        final List<Pair<String, Pattern>> autoFormats = new ArrayList<Pair<String, Pattern>>();
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SSSX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SSX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>(" yyyy-MM-dd'T'HH:mm:ssX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mmX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$ yyyy-MM ^\\d{4}-\\d{2}$ yyyy ^\\d{4}$")));
        autoFormats.add(new Pair<String, Pattern>("EEE, dd MMM yyyy HH:mm:ss z", Pattern.compile("^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$")));
        autoFormats.add(new Pair<String, Pattern>("EEE, dd MMM yy HH:mm:ss Z", Pattern.compile("^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$")));
        autoFormats.add(new Pair<String, Pattern>("EEE, dd-MMM-yy HH:mm:ss z", Pattern.compile("^(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s\\d{2}-[a-zA-Z]{3}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$")));
        autoFormats.add(new Pair<String, Pattern>("EEE MMM dd HH:mm:ss yyyy", Pattern.compile("^[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s(\\d{2}|\\s\\d)\\s\\d{2}:\\d{2}:\\d{2}\\s\\d{4}$")));
        return autoFormats;
    }
}
