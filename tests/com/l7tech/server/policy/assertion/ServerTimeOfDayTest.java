package com.l7tech.server.policy.assertion;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Tests for the TimeOfDay assertion
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 16, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerTimeOfDayTest {

    //todo, tests

    // calendar and timezones experimentation
    public static void main(String[] args) {
        Calendar myCalendar = Calendar.getInstance();
        int hourOfDay = myCalendar.get(Calendar.HOUR_OF_DAY);
        int minutesOfDay = myCalendar.get(Calendar.MINUTE);
        System.out.println("Local time is " + hourOfDay + ":" + minutesOfDay);
        int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
        System.out.println("Local day of week is " + dayOfWeek);
        TimeZone tz = myCalendar.getTimeZone();
        int offset = tz.getRawOffset();
        int hourOffset = offset/(1000*3600);
        System.out.println("Time Zone is " + tz.getDisplayName() + ", offset = " + hourOffset + " hrs");
    }
}
