package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.apache.commons.lang.Validate;

import java.util.Calendar;

/**
 * Default input parameters required to generate a report.
 */
public class DefaultReportParameters {

    public enum QuotaRange {

        /**
         * Queue usage quota for a the last second. Since the bin sizes are 15 mins, an average of the usage will be used
         */
        SECOND,

        /**
         * Queue usage quota for a the last minute. Since the bin sizes are 15 mins, an average of the usage will be used
         */
        MINUTE,

        /**
         * Query usage quota for this HOUR ((now milsec - (mins * 1000*60)) to now)
         */
        HOUR,

        /**
         * Query usage quota for the past 24 hours  ( now  milsec - [ hrs * (1000*60*60) + mins *(1000*60)] ) to now
         */
        DAY,

        /**
         * Query usage quota for the past Month ( now  milsec - [ daysInMonth * (1000*60*60*24) + hrs * (1000*60*60) + mins *(1000*60)] ) to now
         */
        MONTH

    }

    ;

    /**
     * Commonly-used bin sizes.
     */
    public static final int BIN_RESOLUTION_HOURLY = 1;
    public static final int BIN_RESOLUTION_DAILY = 2;
    public static final int BIN_RESOLUTION_CUSTOM = 3;

    /**
     * Time range start
     */
    private long startTime;

    /**
     * Time range start
     */
    private long endTime;

    /**
     * Desired bin size / resolution (default hourly)
     */
    private int binResolution = BIN_RESOLUTION_HOURLY; // "daily", "hourly", "custom (in minutes)"


    // private List<QuotaRange> quotaRange;
    /**
     * Result format (default is JSON)
     */
    private Format format = Format.JSON;

    public DefaultReportParameters(final long startTime, final long endTime, final int binResolution, final Format format) {
        Validate.notNull(format, "Format must not be null.");
        Validate.isTrue(startTime <= endTime, "Start time must be less than or equal to end time.");
        this.startTime = startTime;
        this.endTime = endTime;
        this.binResolution = binResolution;
        this.format = format;
    }

    public DefaultReportParameters(final long startTime, final long endTime, final int binResolution) {
        this(startTime, endTime, binResolution, Format.JSON);
    }

    public DefaultReportParameters(final long startTime, final long endTime) {
        this(startTime, endTime, BIN_RESOLUTION_HOURLY, Format.JSON);
    }

    public DefaultReportParameters() {
    }


    //    public List<QuotaRange> getQuotaRanges(){
//        return this.quotaRange;
//    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getBinResolution() {
        return binResolution;
    }

    public void setBinResolution(int binResolution) {
        this.binResolution = binResolution;
    }

    public Format getFormat() {
        return format;
    }

    public void setFormat(Format format) {
        this.format = format;
    }

    /**
     * Based on the Quota range selected, calculate the start time for the SQL query.
     * <p/>
     * FIXME this method should not be here! Should be somewhere Quota-specific.
     *
     * @param range
     * @return
     */
    public long getStartTime(QuotaRange range) {
        Calendar cal = getCalendar();
        switch (range) {
            // second and minute use the last hour for the start time
            // as quotas for second and minute are calculated based on an average
            case SECOND:
            case MINUTE:
                cal.add(Calendar.HOUR, -1);
                break;
            case HOUR:
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case DAY:
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                break;
            case MONTH:
                int dom = cal.get(Calendar.DAY_OF_MONTH) - 1;
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                cal.setTimeInMillis(cal.getTimeInMillis() - 1000l * 60 * 60 * 24 * dom);
                break;
        }
        return cal.getTimeInMillis();
    }

    /**
     * Restricted method that can be overridden by tests.
     */
    Calendar getCalendar() {
        return Calendar.getInstance();
    }


}
