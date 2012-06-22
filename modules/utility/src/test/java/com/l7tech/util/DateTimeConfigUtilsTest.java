package com.l7tech.util;

import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"JavaDoc"})
@RunWith(MockitoJUnitRunner.class)
public class DateTimeConfigUtilsTest {
    @Before
    public void setUp() throws Exception {

        // configure supported date formats
        List<Pair<String, Pattern>> autoFormats = getAutoFormats();

        dateConfigUtils = new DateTimeConfigUtils();
        dateConfigUtils.setAutoDateFormats(autoFormats);

    }

    @Test
    public void testParseAutoFormat() throws Exception {
        final Date date = dateConfigUtils.parseDateFromString("2012-05-07T14:12:24.567Z");
        validateDate(date, true, true);
    }

    @Test
    public void testParseMilliSecondTimestamp() throws Exception {
        //timestamp for 2012-05-07T14:12:24.567Z
        final Date date = dateConfigUtils.parseDateFromString("1336399944567");
        validateDate(date, true, true);
    }

    @Test
    public void testParseSecondTimestamp() throws Exception {
        //timestamp for 2012-05-07T14:12:24.567Z
        final Date date = dateConfigUtils.parseDateFromString("1336399944");
        validateDate(date, false, true);
    }

    /**
     * No built in format matched the date string
     */
    @Test(expected = DateTimeConfigUtils.UnknownDateFormatException.class)
    public void testParseAutoFormatNotMatched() throws Exception {
        dateConfigUtils.parseDateFromString("Sun, 06--Nov--94 08:49:37 GMT");
    }

    @Test
    public void testISOTimeZones() throws Exception {
        Date date = dateConfigUtils.parseDateFromString("2012-05-07T15:12:24.567+01:00");
        validateDate(date, true, true);

        date = dateConfigUtils.parseDateFromString("2012-05-07T15:12:24+01:00");
        validateDate(date, false, true);

        date = dateConfigUtils.parseDateFromString("2012-05-07T15:12+01:00");
        validateDate(date, false, false);

    }

    @BugNumber(12531)
    @Test
    public void testDefaultTimezoneIsZulu() throws Exception {
        // if GMT is not used by default, then this string would be interpreted as 21:12:24 in PST (in June)
        final Date date = dateConfigUtils.parseDateFromString("Mon May 07 14:12:24 2012");
        System.out.println(date);
        validateDate(date, false, true);

    }

    @Test
    public void testLenientAutoDateParsing() throws Exception {
        final ArrayList<Pair<String, Pattern>> autoDateFormats = new ArrayList<Pair<String, Pattern>>();
        autoDateFormats.add(new Pair<String, Pattern>("EEE MMM dd HH:mm:ss yyyy", Pattern.compile(".*")));
        dateConfigUtils.setAutoDateFormats(autoDateFormats);

        when(config.getBooleanProperty(eq("com.l7tech.util.lenientDateFormat"), eq(false))).thenReturn(true);
        dateConfigUtils.setConfig(config);

        final Date date = dateConfigUtils.parseDateFromString("Mon May 07 14:12:24 12");
        validateDate(date, false, true, true);
    }

    // - PRIVATE

    private DateTimeConfigUtils dateConfigUtils;

    @Mock
    private Config config;

    private void validateDate(Date date, boolean validateMillis, boolean validateSeconds) {
        validateDate(date, validateMillis, validateSeconds, false);
    }

    /**
     * Validates the date represents 2012-05-07T23:12:24.567Z
     * @param date to validate
     * @param validateMillis true if milliseconds should be validated.
     * @param validateSeconds
     */
    private void validateDate(Date date, boolean validateMillis, boolean validateSeconds, boolean allowTwoDigitYear) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeZone(TimeZone.getTimeZone("Z"));
        cal.setTime(date);

        if (allowTwoDigitYear) {
            assertEquals("Incorrect year", 12, cal.get(Calendar.YEAR));
        } else {
            assertEquals("Incorrect year", 2012, cal.get(Calendar.YEAR));
        }

        assertEquals("Incorrect month", 4, cal.get(Calendar.MONTH));
        assertEquals("Incorrect day", 7, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals("Incorrect hour", 14, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals("Incorrect minute", 12, cal.get(Calendar.MINUTE));

        if (validateSeconds) {
            assertEquals("Incorrect second", 24, cal.get(Calendar.SECOND));
        }

        if (validateMillis) {
            assertEquals("Incorrect millisecond", 567, cal.get(Calendar.MILLISECOND));
        }
    }

    private List<Pair<String, Pattern>> getAutoFormats(){
        final List<Pair<String, Pattern>> autoFormats = new ArrayList<Pair<String, Pattern>>();
        // If this needs to be recreated after serverconfig.properties has been updated for auto formats, then see
        // ServerConfigTest.testVerifyDateAutoFormats() which can help
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SSXXX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SXXX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{1}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ssXXX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mmXXX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy-MM", Pattern.compile("^\\d{4}-\\d{2}$")));
        autoFormats.add(new Pair<String, Pattern>("yyyy", Pattern.compile("^\\d{4}$")));
        autoFormats.add(new Pair<String, Pattern>("EEE, dd MMM yyyy HH:mm:ss z", Pattern.compile("^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{4}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$")));
        autoFormats.add(new Pair<String, Pattern>("EEE, dd MMM yy HH:mm:ss Z", Pattern.compile("^[a-zA-Z]{3},\\s\\d{2}\\s[a-zA-Z]{3}\\s\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$")));
        autoFormats.add(new Pair<String, Pattern>("EEE, dd-MMM-yy HH:mm:ss z", Pattern.compile("^(?:Monday|Tuesday|Wednesday|Thursday|Friday|Saturday|Sunday),\\s\\d{2}-[a-zA-Z]{3}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s(?:[a-zA-Z]{3}|(?:\\+|-)\\d{4})$")));
        autoFormats.add(new Pair<String, Pattern>("EEE MMM dd HH:mm:ss yyyy", Pattern.compile("^[a-zA-Z]{3}\\s[a-zA-Z]{3}\\s(\\d{2}|\\s\\d)\\s\\d{2}:\\d{2}:\\d{2}\\s\\d{4}$")));
        return autoFormats;
    }
}
