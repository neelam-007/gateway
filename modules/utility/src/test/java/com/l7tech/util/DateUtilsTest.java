package com.l7tech.util;

import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;

import static org.junit.Assert.*;

/**
 * @author darmstrong
 */
public class DateUtilsTest {
    @Test
    public void testGeTimeZone() throws Exception {

        assertNotNull(DateUtils.getTimeZone("utc"));
        assertNotNull(DateUtils.getTimeZone("local"));
        assertNotNull(DateUtils.getTimeZone("Pacific/Midway"));
        assertNotNull(DateUtils.getTimeZone("+00"));
        assertNotNull(DateUtils.getTimeZone("-00"));
        assertNotNull(DateUtils.getTimeZone("+1000"));
        assertNotNull(DateUtils.getTimeZone("-1000"));
        // 5 hours nad 30 minutes is a valid timezone offset from GMT.
        assertNotNull(DateUtils.getTimeZone("+05:30"));
        assertNotNull(DateUtils.getTimeZone("+0530"));

        assertNotNull(DateUtils.getTimeZone("-02:30"));
        assertNotNull(DateUtils.getTimeZone("-0230"));

        Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})");
    }

}
