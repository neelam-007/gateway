package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.apache.commons.lang.Validate;
import org.bouncycastle.crypto.engines.CAST5Engine;

import java.util.HashMap;
import java.util.List;

/**
 * ReportParameters specific to the Quota usage report
 */
public class ApiQuotaUsageReportParameters extends DefaultReportParameters {



    /**
     * Optional list of API keys that are relevant to the usage report. If null/empty the data will not be filtered by API key.
     */
    private final String apiKey;

    private final HashMap<QuotaRange, List<String>> api_ranges;

    public ApiQuotaUsageReportParameters(final String apiKey, final HashMap<QuotaRange, List<String>> api_ranges){
        Validate.notEmpty(apiKey, "API key must not be null or empty.");
        Validate.notEmpty(api_ranges, "UUID list cannot be null or empty.");
        this.setFormat(Format.JSON);
        this.apiKey = apiKey;
        this.api_ranges = api_ranges;

    }
    public ApiQuotaUsageReportParameters(final String apiKey, final HashMap<QuotaRange, List<String>> api_ranges, final Format format) {
        Validate.notEmpty(apiKey, "API key must not be null or empty.");
        Validate.notEmpty(api_ranges, "UUID list cannot be null or empty.");
        this.setFormat(format);
        this.apiKey = apiKey;
        this.api_ranges = api_ranges;
    }


    public String getApiKey() {
        return apiKey;
    }

    public HashMap<QuotaRange, List<String>> getApiRanges(){
        return api_ranges;
    }


}
