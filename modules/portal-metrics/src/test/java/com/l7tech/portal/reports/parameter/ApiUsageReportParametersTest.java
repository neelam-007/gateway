package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;

public class ApiUsageReportParametersTest {
    private ArrayList<String> uuids = null;
    private ArrayList<String> empty_list = new ArrayList<String>();
    private ArrayList<String> api_keys = null;

    private ArrayList<Long> oids = null;

    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportNullApiIds() {
        new ApiUsageReportParameters(1l, 2l, 1, Format.XML, uuids, api_keys);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportEmptyApiIds() throws Exception {
        //new ApiUsageReportParameters(0, 2, 1, Format.XML, Collections.<Long>emptyList(), null);
        new ApiUsageReportParameters(0, 2, 1, Format.XML, empty_list, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportNullApiIdsDefaultFormat() {
        new ApiUsageReportParameters(0, 2, 1, uuids, api_keys);
    }

    /**
     * FIXME params with api ids are broken.
     */
    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportEmptyApiIdsDefaultFormat() throws Exception {
        new ApiUsageReportParameters(0, 2, 1, Collections.<Long>emptyList(), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportNullApiIdsDefaultFormatAndResolution() {
        new ApiUsageReportParameters(0, 2, uuids, api_keys);
    }

    /**
     * FIXME params with api ids are broken.
     */
    @Ignore
    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportEmptyApiIdsDefaultFormatAndResolution() throws Exception {
        new ApiUsageReportParameters(0, 2, Collections.<Long>emptyList(), null);
    }
}
