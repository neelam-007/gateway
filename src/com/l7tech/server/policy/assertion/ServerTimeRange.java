package com.l7tech.server.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TimeOfDay;
import com.l7tech.policy.assertion.TimeRange;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.logging.Logger;

/**
 * Server side processing of a TimeRange assertion.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 20, 2004<br/>
 * $Id$<br/>
 *
 */
public class ServerTimeRange implements ServerAssertion {
    public ServerTimeRange(TimeRange assertion) {
        if (assertion == null) throw new IllegalArgumentException("must provide assertion");
        subject = assertion;
    }

    public AssertionStatus checkRequest(Request req, Response res) throws IOException, PolicyAssertionException {
        if (!subject.isControlDay() && !subject.isControlTime()) {
            logger.finest("Nothing to check.");
            return AssertionStatus.NONE;
        }
        Calendar nowCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        // check day of week first because it is cheaper
        if (subject.isControlDay()) {
            if (!checkDay(nowCal)) {
                logger.info("Day of week outside allowed range. Returning failure.");
                return AssertionStatus.FAILED;
            }
        }
        if (subject.isControlTime()) {
            if (!checkTimeOfDay(nowCal)) {
                logger.info("Time of day outside allowed range. Returning failure.");
                return AssertionStatus.FAILED;
            }
        }
        logger.finest("Request within TimeRange.");
        return AssertionStatus.NONE;
    }

    private boolean checkDay(Calendar nowCal) {
        int today = nowCal.get(Calendar.DAY_OF_WEEK);
        // is across week?
        if (subject.getEndDayOfWeek() < subject.getStartDayOfWeek()) {
            if (today >= subject.getStartDayOfWeek() && today <= Calendar.SATURDAY) return true;
            if (today <= subject.getEndDayOfWeek()) return true;
            return false;
        } else {
            if (today >= subject.getStartDayOfWeek() && today <= subject.getEndDayOfWeek()) return true;
            return false;
        }
    }

    private boolean checkTimeOfDay(Calendar nowCal) {
        TimeOfDay now = new TimeOfDay(nowCal.get(Calendar.HOUR_OF_DAY), nowCal.get(Calendar.MINUTE),
                                      nowCal.get(Calendar.SECOND));
        TimeOfDay start = subject.getTimeRange().getFrom();
        TimeOfDay end = subject.getTimeRange().getTo();
        // over midnight range
        if (start.compareTo(end) > 0) {
            // does now comes after start but before end of day?
            if (now.compareTo(start) >= 0 && now.compareTo(LAST_SEC_OF_DAY) <= 0) return true;
            // does now comes before end?
            if (now.compareTo(end) <= 0) return true;
            return false;
        } else {
            if (now.compareTo(end) <= 0 && now.compareTo(start) >= 0) return true;
            return false;
        }
    }


    private TimeRange subject;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final static TimeOfDay LAST_SEC_OF_DAY = new TimeOfDay(23,59,59);
}
