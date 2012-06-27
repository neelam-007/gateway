package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.util.DateUtils;
import com.l7tech.util.TimeUnit;
import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

/**
 * @author darmstrong
 */
@SuppressWarnings({"JavaDoc"})
public class DateTimeSelectorTest {

    @Before
    public void setUp() throws Exception {
        testAudit = new TestAudit();
        utcTimeZone = TimeZone.getTimeZone("UTC");
        localTimeZone = TimeZone.getDefault();
    }

    @Test
    public void testDefaultFormatting() throws Exception {
        testFormattedDate("mydate", "${mydate}", DateUtils.ISO8601_PATTERN, utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc() throws Exception {
        testFormattedDate("mydate", "${mydate.utc}", DateUtils.ISO8601_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.uTC}", DateUtils.ISO8601_PATTERN, utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Local() throws Exception {
        testFormattedDate("mydate", "${mydate.local}", DateUtils.ISO8601_PATTERN, localTimeZone);
        testFormattedDate("mydate", "${mydate.LocaL}", DateUtils.ISO8601_PATTERN, localTimeZone);
    }

    @Test
    public void testDefaultFormatting_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_CustomFormatSuffixWithPeriod() throws Exception {
        testFormattedDate("mydate", "${mydate.yyyyMMdd'T'HH:mm:ss.SSS}", "yyyyMMdd'T'HH:mm:ss.SSS", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.utc.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc_CustomFormatSuffixWithPeriod() throws Exception {
        testFormattedDate("mydate", "${mydate.utc.yyyyMMdd'T'HH:mm:ss.SSS}", "yyyyMMdd'T'HH:mm:ss.SSS", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Local_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.local.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", localTimeZone);
    }

    @Test
    public void testMillis() throws Exception {
        testTimestampDate("mydate", "${mydate.millis}", false);
    }

    @Test
    public void testSeconds() throws Exception {
        testTimestampDate("mydate", "${mydate.seconds}", true);
    }

    @Test
    public void testBuiltInSuffixes_ISO8601() throws Exception {
        testFormattedDate("mydate", "${mydate.iSo8601}", DateUtils.ISO8601_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.local.iso8601}", DateUtils.ISO8601_PATTERN, localTimeZone);
    }

    @Test
    public void testBuiltInSuffixes_RFC1123() throws Exception {
        testFormattedDate("mydate", "${mydate.RFc1123}", DateUtils.RFC1123_DEFAULT_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.utc.rfc1123}", DateUtils.RFC1123_DEFAULT_PATTERN, utcTimeZone);
    }

    @Test
    public void testBuiltInSuffixes_RFC850() throws Exception {
        testFormattedDate("mydate", "${mydate.RFc850}", DateUtils.RFC850_DEFAULT_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.lOCAL.rfc850}", DateUtils.RFC850_DEFAULT_PATTERN, localTimeZone);
    }

    @Test
    public void testBuiltInSuffixes_asctime() throws Exception {
        testFormattedDate("mydate", "${mydate.asctime}", DateUtils.ASCTIME_DEFAULT_PATTERN, utcTimeZone);
        testFormattedDate("mydate", "${mydate.lOCAL.asctime}", DateUtils.ASCTIME_DEFAULT_PATTERN, localTimeZone);
    }

    @Test
    public void testDateSelector_local() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        //doesn't matter what the variable is called - added in .test to illustrate that all that matters is the
        // longest part of the variable reference which matches a defined variable.
        vars.put("mydate.test", date);
        final String actual = ExpandVariables.process("${mydate.test.local}", vars, testAudit);

        //format according to local
        final String expected = DateUtils.getFormattedString(date, DateUtils.getTimeZone("local"), null);
        assertEquals(expected, actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testNamedTimeZoneSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.Australia/Victoria}", DateUtils.ISO8601_PATTERN, TimeZone.getTimeZone("Australia/Victoria"));
        testFormattedDate("mydate", "${mydate.Australia/Victoria.rfc1123}", DateUtils.RFC1123_DEFAULT_PATTERN, TimeZone.getTimeZone("Australia/Victoria"));
    }

    @Test
    public void testOffsetTimeZoneNegative() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-07:00}", "2012-06-26T18:17:59.000-07:00");
    }

    @Test
    public void testOffsetTimeZoneNegative_Short() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-07}", "2012-06-26T18:17:59.000-07:00");
    }

    @Test
    public void testOffsetTimeZonePositive() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+07:00}", "2012-06-27T08:17:59.000+07:00");
    }

    @Test
    public void testOffsetTimeZonePositive_Short() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+07:00}", "2012-06-27T08:17:59.000+07:00");
    }

    @Test
    public void testOffsetTimeZoneNegative_NoColon() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-0700}", "2012-06-26T18:17:59.000-07:00");
    }

    @Test
    public void testOffsetTimeZonePositive_NoColon() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+0700}", "2012-06-27T08:17:59.000+07:00");
    }

    @Test
    public void testOffsetTimeZoneNegativeWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-04:30}", "2012-06-26T20:47:59.000-04:30");
    }

    @Test
    public void testOffsetTimeZonePositiveWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+05:30}", "2012-06-27T06:47:59.000+05:30");
    }

    @Test
    public void testOffsetTimeZoneNegative_NoColonWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.-0230}", "2012-06-26T22:47:59.000-02:30");
    }

    @Test
    public void testOffsetTimeZonePositive_NoColonWithMinutes() throws Exception {
        testFormattedDate(date, "mydate", "${mydate.+0530}", "2012-06-27T06:47:59.000+05:30");
    }

    // - PRIVATE

    private TestAudit testAudit;
    private TimeZone utcTimeZone;
    private TimeZone localTimeZone;
    /**
     * Wed, 27 Jun 2012 01:17:59 GMT
     */
    private final Date date = new Date(1340759879000L);

    private void testFormattedDate(final Date date, final String dateVarNameNoSuffixes, final String dateExpression, final String expectedOutput) {
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(dateVarNameNoSuffixes, date);
        final String actual = ExpandVariables.process(dateExpression, vars, testAudit);
        System.out.println(actual);

        assertEquals(expectedOutput, actual);
        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    private void testFormattedDate(final String dateVarNameNoSuffixes, final String dateExpression, final String expectedFormat, final TimeZone expectedTimeZone) {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(dateVarNameNoSuffixes, date);
        final String actual = ExpandVariables.process(dateExpression, vars, testAudit);
        System.out.println(actual);

        final SimpleDateFormat format = new SimpleDateFormat(expectedFormat);
        format.setTimeZone(expectedTimeZone);
        format.setLenient(false);
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(date.getTime());
        assertEquals(format.format(cal.getTime()), actual);
        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    private void testTimestampDate(final String dateVarNameNoSuffixes, final String dateExpression, final boolean seconds) {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put(dateVarNameNoSuffixes, date);
        final String process = ExpandVariables.process(dateExpression, vars, testAudit);
        if (seconds) {
            assertEquals(Long.valueOf(date.getTime() / 1000L), Long.valueOf(process));
        } else {
            assertEquals(Long.valueOf(date.getTime()), Long.valueOf(process));
        }

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

}
