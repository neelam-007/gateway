package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.TimeOfDay;
import com.l7tech.policy.assertion.TimeRange;
import com.l7tech.server.message.PolicyEnforcementContext;

import java.io.IOException;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Server side processing of a TimeRange assertion.
 */
public class ServerTimeRange extends AbstractServerAssertion<TimeRange> implements ServerAssertion<TimeRange> {


    public ServerTimeRange(TimeRange assertion) {
        super(assertion);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (!assertion.isControlDay() && !assertion.isControlTime()) {
            logAndAudit( AssertionMessages.TIME_RANGE_NOTHING_TO_CHECK );
            return AssertionStatus.NONE;
        }
        Calendar nowCal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        // check day of week first because it is cheaper
        if ( assertion.isControlDay()) {
            if (!checkDay(nowCal)) {
                logAndAudit( AssertionMessages.TIME_RANGE_DOW_OUTSIDE_RANGE );

                return AssertionStatus.FAILED;
            }
        }
        if ( assertion.isControlTime()) {
            if (!checkTimeOfDay(nowCal)) {
                logAndAudit( AssertionMessages.TIME_RANGE_TOD_OUTSIDE_RANGE );

                return AssertionStatus.FAILED;
            }
        }
        logAndAudit( AssertionMessages.TIME_RAGNE_WITHIN_RANGE );
        return AssertionStatus.NONE;
    }

    private boolean checkDay(Calendar nowCal) {
        int today = nowCal.get(Calendar.DAY_OF_WEEK);
        // is across week?
        if ( assertion.getEndDayOfWeek() < assertion.getStartDayOfWeek()) {
            if (today >= assertion.getStartDayOfWeek() && today <= Calendar.SATURDAY) return true;
            if (today <= assertion.getEndDayOfWeek()) return true;
            return false;
        } else {
            if (today >= assertion.getStartDayOfWeek() && today <= assertion.getEndDayOfWeek()) return true;
            return false;
        }
    }

    private boolean checkTimeOfDay(Calendar nowCal) {
        TimeOfDay now = new TimeOfDay(nowCal.get(Calendar.HOUR_OF_DAY), nowCal.get(Calendar.MINUTE),
                                      nowCal.get(Calendar.SECOND));
        TimeOfDay start = assertion.getTimeRange().getFrom();
        TimeOfDay end = assertion.getTimeRange().getTo();
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

    private final static TimeOfDay LAST_SEC_OF_DAY = new TimeOfDay(23,59,59);
}
