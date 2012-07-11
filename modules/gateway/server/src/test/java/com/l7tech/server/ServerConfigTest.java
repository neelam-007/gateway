package com.l7tech.server;

import static java.util.Calendar.*;
import static org.junit.Assert.*;

import com.l7tech.gateway.common.audit.AuditFactory;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.DateTimeConfigUtils;
import com.l7tech.util.Pair;
import org.junit.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
@SuppressWarnings({"JavaDoc"})
public class ServerConfigTest {
    @Test
    public void checkPropertyDescriptions() {
        ServerConfig sc = ServerConfig.getInstance();

        Map<String,String> descToName = new HashMap<String,String>();
        Map<String,String> namesAndDescs = sc.getClusterPropertyNames();

        for (Map.Entry<String, String> entry : namesAndDescs.entrySet()) {
            String name = entry.getKey();
            String desc = entry.getValue();
            assertNotNull("Cluster property name must be non-null", name);
            final String trimmedName = name.trim().toLowerCase();
            assertTrue("Cluster property name must be non-empty", trimmedName.length() > 0);
            assertNotNull("Cluster property description for " + name + " must be non-null", desc);
            final String trimmedDesc = desc.trim().toLowerCase();
            assertTrue("Cluster property description for " + name + " must be non-empty", trimmedDesc.length() > 0);

            final String prevName = descToName.put(trimmedDesc, trimmedName);
            if (prevName != null)
                fail("Cluster property description for " + name + " duplicates description for " + prevName);
        }
    }

    @Test
    public void testDefaultCustomFormats() throws Exception {
        final ServerConfig sc = ServerConfig.getInstance();

        // Use SimplePropertyChangeHandler to obtain the values from ServerConfig. This is not strictly needed, but
        // it allows this code to be reused. We care about the actual values defined in serverconfig.properties and their
        // actual shipped values.
        SimplePropertyChangeHandler spch = new SimplePropertyChangeHandler();
        TestAudit testAudit = new TestAudit();
        final AuditFactory factory = testAudit.factory();

        ApplicationContexts.inject(spch, CollectionUtils.<String, Object>mapBuilder()
                .put("serverConfig", sc)
                .put("auditFactory", factory)
                .put("dateParser", new DateTimeConfigUtils())
                .unmodifiableMap()
        );

        final Set<String> customDateFormatSet = new HashSet<String>(spch.getCustomDateFormatsStrings());

        assertEquals(12, customDateFormatSet.size());

        assertTrue(customDateFormatSet.contains("yyyy-MM-dd'T'HH:mm:ss.SSSXXX"));
        assertTrue(customDateFormatSet.contains("yyyy-MM-dd'T'HH:mm:ss.SSXXX"));
        assertTrue(customDateFormatSet.contains("yyyy-MM-dd'T'HH:mm:ss.SXXX"));
        assertTrue(customDateFormatSet.contains("yyyy-MM-dd'T'HH:mm:ssXXX"));
        assertTrue(customDateFormatSet.contains("yyyy-MM-dd'T'HH:mmXXX"));
        assertTrue(customDateFormatSet.contains("yyyy-MM-dd"));
        assertTrue(customDateFormatSet.contains("yyyy-MM"));
        assertTrue(customDateFormatSet.contains("yyyy"));
        assertTrue(customDateFormatSet.contains("EEE, dd MMM yyyy HH:mm:ss z"));
        assertTrue(customDateFormatSet.contains("EEE, dd MMM yy HH:mm:ss Z"));
        assertTrue(customDateFormatSet.contains("EEE, dd-MMM-yy HH:mm:ss z"));
        assertTrue(customDateFormatSet.contains("EEE MMM dd HH:mm:ss yyyy"));
    }

    /**
     * If this test fails it's likely that datetime.autoFormats has been modified.
     * This test validates the hardcoded list of formats and patterns in this property.
     * Any new additions should have a test here to validate the correctness of the pairing of format to pattern, in
     * addition to the correct interpretation of the date parsed by the format.
     */
    @Test
    public void testVerifyDateAutoFormats() throws Exception {
        final ServerConfig sc = ServerConfig.getInstance();

        // Use SimplePropertyChangeHandler to obtain the values from ServerConfig. This is not strictly needed, but
        // it allows this code to be reused. We care about the actual values defined in serverconfig.properties and their
        // actual shipped values.
        SimplePropertyChangeHandler spch = new SimplePropertyChangeHandler();
        TestAudit testAudit = new TestAudit();
        final AuditFactory factory = testAudit.factory();

        ApplicationContexts.inject(spch, CollectionUtils.<String, Object>mapBuilder()
                .put("serverConfig", sc)
                .put("auditFactory", factory)
                .put("dateParser", new DateTimeConfigUtils())
                .unmodifiableMap()
        );

        final List<Pair<String,Pattern>> pairList = spch.getAutoDateFormatsStrings();
        final Map<String, Pattern> formatToPattern = new HashMap<String, Pattern>();
        for (Pair<String, Pattern> pair : pairList) {
            System.out.println(pair.left + " - " + pair.right);
            formatToPattern.put(pair.left, pair.right);
            //autoFormats.add(new Pair<String, Pattern>("yyyy-MM-dd'T'HH:mm:ss.SSSX", Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:?+(?:\\d{2})?+)$")));
            //
//            System.out.println("autoFormats.add(new Pair<String, Pattern>(\"" + pair.left + "\", Pattern.compile(\"^" + pair.right.toString().replace("\\", "\\\\") + "$\")));");
        }

        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "2012-05-29T19:46:30.123Z", 1338320790123L, MILLISECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ss.SSXXX", "2012-05-29T19:46:30.12Z", 1338320790012L, MILLISECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ss.SXXX", "2012-05-29T19:46:30.1Z", 1338320790001L, MILLISECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ssXXX", "2012-05-29T19:46:30Z", 1338320790839L, SECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mmXXX", "2012-05-29T19:46Z", 1338320790839L, MINUTE, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd", "2012-05-29", 1338320790839L, DAY_OF_MONTH, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM", "2012-05", 1338320790839L, MONTH, formatToPattern);
        validateMatchAndExclusivity("yyyy", "2012", 1338320790839L, YEAR, formatToPattern);
        validateMatchAndExclusivity("EEE, dd MMM yyyy HH:mm:ss z", "Wed, 02 Oct 2002 13:46:30 EST", 1033584390222L, SECOND, formatToPattern);
        validateMatchAndExclusivity("EEE, dd MMM yy HH:mm:ss Z", "Wed, 02 Oct 02 13:46:30 EST", 1033584390222L, SECOND, formatToPattern);
        validateMatchAndExclusivity("EEE, dd-MMM-yy HH:mm:ss z", "Wednesday, 02-Oct-02 13:46:30 EST", 1033584390222L, SECOND, formatToPattern);
        validateMatchAndExclusivity("EEE MMM dd HH:mm:ss yyyy", "Wed Oct  2 13:46:30 2002", "20021002T13:46:30", SECOND, formatToPattern);

        // validate ISO 8601 with 8601 style timezone
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", "2012-05-29T19:46:30.123+01:00", 1338317190123L, MILLISECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ss.SSXXX", "2012-05-29T19:46:30.12-01:00", 1338324390012L, MILLISECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ss.SXXX", "2012-05-29T19:46:30.1+01:00", 1338317190001L, MILLISECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mm:ssXXX", "2012-05-29T19:46:30-01:00", 1338324390012L, SECOND, formatToPattern);
        validateMatchAndExclusivity("yyyy-MM-dd'T'HH:mmXXX", "2012-05-29T19:46+01:00", 1338317190001L, MINUTE, formatToPattern);

        // validate rfc822 / 1123 with numeric timezones
        validateMatchAndExclusivity("EEE, dd MMM yyyy HH:mm:ss z", "Wed, 02 Oct 2002 13:46:30 +0200", 1033559190625L, SECOND, formatToPattern);
        validateMatchAndExclusivity("EEE, dd MMM yy HH:mm:ss Z", "Wed, 02 Oct 02 13:46:30 -1000", 1033602390448L, SECOND, formatToPattern);
    }

    private void validateMatchAndExclusivity(final String formatToTest, final String exampleDate, Object exampleMillis, int maximumCalField, Map<String, Pattern> allPatterns) throws ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(formatToTest);
        dateFormat.setLenient(false);
        Calendar actualCal = Calendar.getInstance();
        actualCal.setTime(dateFormat.parse(exampleDate));
        Calendar expectedCal = Calendar.getInstance();
        if (exampleMillis instanceof Long) {
            expectedCal.setTimeInMillis((Long) exampleMillis);
        } else {
            final Date expectedDate = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss").parse(exampleMillis.toString());
            expectedCal.setTime(expectedDate);
        }

        //reset fields for which we parsed no precision.
        //this is based on the internals of Calendar - fields go in order up to 14, which is milliseconds
        for (int i = maximumCalField + 1; i <= 14; i++) {
            actualCal.set(i, 0);
            expectedCal.set(i, 0);
        }

        assertEquals("Date " + exampleDate+" incorrectly interpreted with format: " + formatToTest, expectedCal.getTimeInMillis(), actualCal.getTimeInMillis());
        Pattern pattern = allPatterns.get(formatToTest);
        assertNotNull("Pattern for Format not found: " + formatToTest, pattern);
        Matcher matcher = pattern.matcher(exampleDate);
        assertTrue("Format: " + formatToTest+" configured with pattern: " + pattern+" did not match example date: " + exampleDate,
                matcher.matches());
        validateNoOtherMatches(formatToTest, exampleDate, allPatterns);
    }

    private void validateNoOtherMatches(final String formatToIgnore, final String exampleDate, Map<String, Pattern> allPatterns){
        for (Map.Entry<String, Pattern> entry : allPatterns.entrySet()) {
            if (formatToIgnore.equals(entry.getKey())) {
                continue;
            }

            assertFalse("Pattern for format: " + entry.getKey() + " matched pattern:" + entry.getValue(),
                    entry.getValue().matcher(exampleDate).matches());
        }

    }

    /**
     * Use to generate the timestamps used in {@link #testVerifyDateAutoFormats()} for ISO8601 dates
     */
    private void testGetTime() throws Exception {
        final Calendar cal = Calendar.getInstance();
        final TimeZone gmt = TimeZone.getTimeZone("GMT");
        cal.setTimeZone(gmt);
        cal.set(Calendar.YEAR, 2012);
        cal.set(Calendar.MONTH, 5 - 1);
        cal.set(Calendar.DAY_OF_MONTH, 29);
//        cal.set(Calendar.HOUR_OF_DAY, 19);
        cal.set(Calendar.HOUR_OF_DAY, 18);
//        cal.set(Calendar.HOUR_OF_DAY, 20);
        cal.set(Calendar.MINUTE, 46);
        cal.set(Calendar.SECOND, 30);
//        cal.set(Calendar.MILLISECOND, 123);
//        cal.set(Calendar.MILLISECOND, 12);
        cal.set(Calendar.MILLISECOND, 1);
        final Date time = cal.getTime();
//        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        dateFormat.setTimeZone(gmt);
        System.out.println(dateFormat.format(time));
        System.out.println(time);
        System.out.println(cal.getTimeInMillis());
    }

    /**
     * Use to generate the timestamps used in {@link #testVerifyDateAutoFormats()} for rfc dates
     */
    public void testGetTimeRFC() throws Exception {
        final Calendar cal = Calendar.getInstance();
//        final TimeZone timeZone = TimeZone.getTimeZone("EST");
        final TimeZone timeZone = TimeZone.getTimeZone("GMT");
        cal.setTimeZone(timeZone);
        cal.set(Calendar.YEAR, 2002);
        cal.set(Calendar.MONTH, 10 - 1);
        cal.set(Calendar.DAY_OF_MONTH, 2);
//        cal.set(Calendar.HOUR_OF_DAY, 13);
//        cal.set(Calendar.HOUR_OF_DAY, 11);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 46);
        cal.set(Calendar.SECOND, 30);
//        cal.set(Calendar.MILLISECOND, 123);
//        cal.set(Calendar.MILLISECOND, 12);
//        cal.set(Calendar.MILLISECOND, 1);
        final Date time = cal.getTime();
//        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
        dateFormat.setTimeZone(timeZone);
        System.out.println(dateFormat.format(time));
        System.out.println(time);
        System.out.println(cal.getTimeInMillis());
    }
}
