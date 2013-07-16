package com.l7tech.portal.reports.parameter;

import com.l7tech.portal.reports.format.Format;
import org.junit.Test;

import java.util.Collections;

public class UsagesSummaryReportParametersTest {
    @Test(expected = IllegalArgumentException.class)
    public void nullApplicationKeys() {
        new UsageSummaryReportParameters(0, 1, 1, Format.XML, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyApplicationKeys() {
        new UsageSummaryReportParameters(0, 1, 1, Format.XML, Collections.<String>emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullApplicationKeysDefaultFormat() {
        new UsageSummaryReportParameters(0, 1, 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyApplicationKeysDefaultFormat() {
        new UsageSummaryReportParameters(0, 1, 1, Collections.<String>emptyList());
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullApplicationKeysDefaultFormatAndResolution() {
        new UsageSummaryReportParameters(0, 1, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyApplicationKeysDefaultFormatAndResolution() {
        new UsageSummaryReportParameters(0, 1, Collections.<String>emptyList());
    }
}
