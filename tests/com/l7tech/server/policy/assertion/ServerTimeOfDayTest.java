package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TimeOfDay;
import com.l7tech.policy.assertion.TimeOfDayRange;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Tests for the TimeRange assertion.
 * Visual inspection style of testing because expected results depends on time when you run the tests.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 16, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerTimeOfDayTest {
    // calendar and timezones experimentation
    public static void main(String[] args) throws Exception {
        Calendar myCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        int hourOfDay = myCalendar.get(Calendar.HOUR_OF_DAY);
        int minutesOfDay = myCalendar.get(Calendar.MINUTE);
        System.out.println("Current UTC time is " + hourOfDay + ":" + minutesOfDay);
        int dayOfWeek = myCalendar.get(Calendar.DAY_OF_WEEK);
        System.out.println("\n\nCurrent UTC day of week is " + dayOfWeek);
        System.out.println("\n\n==============================\n\n");

        ServerTimeOfDayTest me = new ServerTimeOfDayTest();
        me.testTimeRange();
    }

    private void testTimeRange() throws Exception {
        System.out.println("Empty condition:");
        testCondition();

        condition.setControlDay(true);
        condition.setStartDayOfWeek(Calendar.MONDAY);
        condition.setEndDayOfWeek(Calendar.MONDAY);
        System.out.println("Has to be monday:");
        testCondition();

        condition.setStartDayOfWeek(Calendar.THURSDAY);
        condition.setEndDayOfWeek(Calendar.MONDAY);
        System.out.println("Has to be between thursday and monday:");
        testCondition();

        condition.setStartDayOfWeek(Calendar.FRIDAY);
        condition.setEndDayOfWeek(Calendar.FRIDAY);
        System.out.println("Has to be friday:");
        testCondition();

        condition.setStartDayOfWeek(Calendar.SATURDAY);
        condition.setEndDayOfWeek(Calendar.MONDAY);
        System.out.println("Has to be between saturday & monday:");
        testCondition();

        condition.setControlDay(false);
        condition.setControlTime(true);
        condition.setTimeRange(new TimeOfDayRange(new TimeOfDay(8,59,59), new TimeOfDay(14,0,0)));
        System.out.println("Has to be between 0900 and 1400:");
        testCondition();

        condition.setTimeRange(new TimeOfDayRange(new TimeOfDay(20,59,59), new TimeOfDay(8,0,0)));
        System.out.println("Has to be between 2100 and 0800:");
        testCondition();

        condition.setTimeRange(new TimeOfDayRange(new TimeOfDay(20,59,59), new TimeOfDay(22,0,0)));
        System.out.println("Has to be between 2100 and 2200:");
        testCondition();
    }

    private void testCondition() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message(), null, null);
        boolean result = ((new ServerTimeRange(condition)).checkRequest(context) == AssertionStatus.NONE);
        if (result) System.out.println("passed");
        else System.out.println("failed");
    }

    private TimeRange condition = new TimeRange();
}
