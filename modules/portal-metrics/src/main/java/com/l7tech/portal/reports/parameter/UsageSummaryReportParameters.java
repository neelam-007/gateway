package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.apache.commons.lang.Validate;

import java.util.List;

/**
 * Report parameters specifically for an application (api key) usage report.
 */
public class UsageSummaryReportParameters extends DefaultReportParameters {
    /**
     * Application keys that are relevant for the application usage report.
     */
    private final List<String> applicationKeys;

    public UsageSummaryReportParameters(final long startTime, final long endTime, final int binResolution, final Format format, final List<String> applicationKeys) {
        super(startTime, endTime, binResolution, format);
        Validate.notEmpty(applicationKeys, "Application keys cannot be null or empty.");
        this.applicationKeys = applicationKeys;
    }

    public UsageSummaryReportParameters(final long startTime, final long endTime, final int binResolution, final List<String> applicationKeys) {
        super(startTime, endTime, binResolution);
        Validate.notEmpty(applicationKeys, "Application keys cannot be null or empty.");
        this.applicationKeys = applicationKeys;
    }

    public UsageSummaryReportParameters(final long startTime, final long endTime, final List<String> applicationKeys) {
        super(startTime, endTime);
        Validate.notEmpty(applicationKeys, "Application keys cannot be null or empty.");
        this.applicationKeys = applicationKeys;
    }

    public List<String> getApplicationKeys() {
        return applicationKeys;
    }
}
