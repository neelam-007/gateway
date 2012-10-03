package com.l7tech.portal.reports.parameter;

import org.jetbrains.annotations.Nullable;

/**
 * Parameters required for a ranking report.
 */
public class RankingReportParameters extends DefaultReportParameters {
    static final Integer DEFAULT_LIMIT = 10;

    /**
     * The number of results to limit the report to. Can be null if there is no limit.
     */
    private Integer limit = DEFAULT_LIMIT;

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(@Nullable final Integer limit) {
        this.limit = limit;
    }
}
