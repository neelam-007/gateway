package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.parameter.RankingReportParameters;
import org.jetbrains.annotations.NotNull;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.COMMON_WHERE;

/**
 * Defines an application (API key) ranking report - most requested applications.
 * <p/>
 * Each rank will include the following information:
 * - hits
 * - api_key
 * <p/>
 */
public class ApplicationRankingReportDefinition extends RankingReportDefinition {
    static final String QUERY = "select api_key, sum(hits_total) as hits from api_key_or_method_usage_view " + COMMON_WHERE +
            "group by api_key order by hits desc ";

    public ApplicationRankingReportDefinition(@NotNull final RankingReportParameters params) {
        super(params);
    }

    @Override
    protected String getQueryWithoutLimit() {
        return QUERY;
    }
}
