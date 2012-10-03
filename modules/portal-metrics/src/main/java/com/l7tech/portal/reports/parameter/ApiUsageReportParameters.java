package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.List;

/**
 * ReportParameters specific to an API usage report.
 */
public class ApiUsageReportParameters extends DefaultReportParameters {
    /**
     * @deprecated In favour of UUIDs
     *             API/service ids that are relevant to the usage report.
     */
    private final List<Long> apis;

    /**
     * Optional list of API keys that are relevant to the usage report. If null/empty the data will not be filtered by API key.
     */
    private final List<String> apiKeys;

    /**
     * List of UUID which uniquely identifies a service. Each UUID is unique across multiple implementation of the Portal in reference to a particular API.
     */
    private final ArrayList<String> uuids;

    @Deprecated
    public ApiUsageReportParameters(final long startTime, final long endTime, final int binResolution, final Format format, final List<Long> apis, final List<String> apiKeys) {
        throw new UnsupportedOperationException("Parameters with api ids currently not supported.");
//        super(startTime, endTime, binResolution, format);
//        Validate.notEmpty(apis, "Apis must not be null or empty.");
//        this.apis = apis;
//        this.apiKeys = apiKeys;
//        this.uuids = null;
    }

    public ApiUsageReportParameters(final long startTime, final long endTime, final int binResolution, final Format format, final ArrayList<String> uuids, final List<String> apiKeys) {
        super(startTime, endTime, binResolution, format);
        Validate.notEmpty(uuids, "Apis must not be null or empty.");
        this.apis = null;
        this.apiKeys = apiKeys;
        this.uuids = uuids;
    }

    @Deprecated
    public ApiUsageReportParameters(final long startTime, final long endTime, final int binResolution, final List<Long> apis, final List<String> apiKeys) {
        throw new UnsupportedOperationException("Parameters with api ids currently not supported.");
//        super(startTime, endTime, binResolution);
//        Validate.notEmpty(apis, "Apis must not be null or empty.");
//        this.apis = apis;
//        this.apiKeys = apiKeys;
//        this.uuids = null;
    }


    public ApiUsageReportParameters(final long startTime, final long endTime, final int binResolution, final ArrayList<String> uuids, final List<String> apiKeys) {
        super(startTime, endTime, binResolution);
        Validate.notEmpty(uuids, "Apis must not be null or empty.");
        this.apis = null;
        this.apiKeys = apiKeys;
        this.uuids = uuids;
    }


    @Deprecated
    public ApiUsageReportParameters(final long startTime, final long endTime, final List<Long> apis, final List<String> apiKeys) {
        throw new UnsupportedOperationException("Parameters with api ids currently not supported.");
//        super(startTime, endTime);
//        Validate.notEmpty(apis, "Apis must not be null or empty.");
//        this.apis = apis;
//        this.apiKeys = apiKeys;
//        this.uuids = null;
    }

    public ApiUsageReportParameters(final long startTime, final long endTime, final ArrayList<String> uuids, final List<String> apiKeys) {
        super(startTime, endTime);
        Validate.notEmpty(uuids, "Apis must not be null or empty.");
        this.uuids = uuids;
        this.apiKeys = apiKeys;
        this.apis = null;
    }

    public List<Long> getApis() {
        return apis;
    }

    public List<String> getApiKeys() {
        return apiKeys;
    }

    public ArrayList<String> getUuids() {
        return uuids;

    }
}
