package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.junit.Test;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static org.junit.Assert.*;

public class DefaultReportParametersTest {
    private DefaultReportParameters parameters;

    @Test(expected = IllegalArgumentException.class)
    public void nullFormat() {
        parameters = new DefaultReportParameters(0, 2, 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void startTimeAfterEndTime() {
        parameters = new DefaultReportParameters(2, 1, 1, Format.XML);
    }

    @Test
    public void defaultFormat() {
        parameters = new DefaultReportParameters(0, 1, 1);
        assertEquals(Format.JSON, parameters.getFormat());
    }

    @Test
    public void defaultFormatAndResolution() {
        parameters = new DefaultReportParameters(0, 1);
        assertEquals(Format.JSON, parameters.getFormat());
        assertEquals(DefaultReportParameters.BIN_RESOLUTION_HOURLY, parameters.getBinResolution());
    }

    @Test
    public void getStartTimeMinute() {
        final Calendar calendar = new GregorianCalendar(2012, Calendar.JANUARY, 10, 10, 10, 10);
        parameters = new StubDefaultReportParameters(calendar.getTimeInMillis());

        final long startTime = parameters.getStartTime(DefaultReportParameters.QuotaRange.MINUTE);

        calendar.add(Calendar.HOUR, -1);
        assertEquals(calendar.getTimeInMillis(), startTime);
    }

    @Test
    public void getStartTimeSecond() {
        final Calendar calendar = new GregorianCalendar(2012, Calendar.JANUARY, 10, 10, 10, 10);
        parameters = new StubDefaultReportParameters(calendar.getTimeInMillis());

        final long startTime = parameters.getStartTime(DefaultReportParameters.QuotaRange.SECOND);

        calendar.add(Calendar.HOUR, -1);
        assertEquals(calendar.getTimeInMillis(), startTime);
    }

    private class StubDefaultReportParameters extends DefaultReportParameters {
        private final long time;

        StubDefaultReportParameters(final long time) {
            this.time = time;
        }

        @Override
        Calendar getCalendar() {
            final GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTimeInMillis(time);
            return calendar;
        }
    }
}
