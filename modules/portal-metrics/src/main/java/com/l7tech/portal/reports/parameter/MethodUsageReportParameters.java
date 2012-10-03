package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.apache.commons.lang.Validate;

import java.util.List;

/**
 * Report parameters required for an API method usage report.
 */
public class MethodUsageReportParameters extends DefaultReportParameters {
    /**
     * The API methods that are relevant for the report.
     */
    private final List<String> apiMethods;

    public List<String> getApiMethods() {
        return apiMethods;
    }

    public MethodUsageReportParameters(final long startTime, final long endTime, final int binResolution, final Format format, final List<String> apiMethods) {
        super(startTime, endTime, binResolution, format);
        Validate.notEmpty(apiMethods, "API methods cannot be null or empty.");
        this.apiMethods = apiMethods;
    }

    public MethodUsageReportParameters(final long startTime, final long endTime, final int binResolution, final List<String> apiMethods) {
        super(startTime, endTime, binResolution);
        Validate.notEmpty(apiMethods, "API methods cannot be null or empty.");
        this.apiMethods = apiMethods;
    }

    public MethodUsageReportParameters(final long startTime, final long endTime, final List<String> apiMethods) {
        super(startTime, endTime);
        Validate.notEmpty(apiMethods, "API methods cannot be null or empty.");
        this.apiMethods = apiMethods;
    }
}
