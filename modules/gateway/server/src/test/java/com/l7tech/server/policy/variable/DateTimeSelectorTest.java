package com.l7tech.server.policy.variable;

import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.util.DateUtils;
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
        testFormattedDate("mydate", "${mydate}", DateUtils.ISO8601_DEFAULT_PATTERN, utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc() throws Exception {
        testFormattedDate("mydate", "${mydate.utc}", DateUtils.ISO8601_DEFAULT_PATTERN, utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Local() throws Exception {
        testFormattedDate("mydate", "${mydate.local}", DateUtils.ISO8601_DEFAULT_PATTERN, localTimeZone);
    }

    @Test
    public void testDefaultFormatting_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", utcTimeZone);
    }

    @Test
    public void testDefaultFormatting_Utc_CustomFormatSuffix() throws Exception {
        testFormattedDate("mydate", "${mydate.utc.yyyyMMdd'T'HH:mm:ss}", "yyyyMMdd'T'HH:mm:ss", utcTimeZone);
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

    @Test
    public void testDateSelector_Millisecond() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("mydate", date);
        final String process = ExpandVariables.process("${mydate.millis}", vars, testAudit);
        assertEquals(Long.valueOf(date.getTime()), Long.valueOf(process));

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
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
        final String expected = DateUtils.getFormattedString(date, "local");
        assertEquals(expected, actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testDateSelector_Second() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("mydate", date);
        final String process = ExpandVariables.process("${mydate.seconds}", vars, testAudit);
        assertEquals(Long.valueOf(date.getTime() / 1000L), Long.valueOf(process));

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testDateSelector_Formatting_Local() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("mydate", date);
        final String actual = ExpandVariables.process("${mydate.local}", vars, testAudit);

        assertEquals(DateUtils.getFormattedString(date, "local"), actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testDateSelector_Formatting_Local_CustomFormat() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("mydate", date);
        final String actual = ExpandVariables.process("${mydate.local.yyyyMMdd}", vars, testAudit);

        assertEquals(DateUtils.getFormattedString(date, "local", "yyyyMMdd"), actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testDateSelector_Formatting_UTC() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("mydate", date);
        final String actual = ExpandVariables.process("${mydate.utc}", vars, testAudit);

        assertEquals(DateUtils.getFormattedString(date, "utc"), actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());
    }

    @Test
    public void testDateSelector_Formatting_UTC_CustomFormat() throws Exception {
        final Date date = new Date();
        final HashMap<String, Object> vars = new HashMap<String, Object>();
        vars.put("mydate", date);
        final String actual = ExpandVariables.process("${mydate.utc.yyyyMMdd}", vars, testAudit);

        assertEquals(DateUtils.getFormattedString(date, "utc", "yyyyMMdd"), actual);

        // no audits were created
        assertFalse(testAudit.iterator().hasNext());

    }

    // - PRIVATE

    private TestAudit testAudit;
    private TimeZone utcTimeZone;
    private TimeZone localTimeZone;
}
