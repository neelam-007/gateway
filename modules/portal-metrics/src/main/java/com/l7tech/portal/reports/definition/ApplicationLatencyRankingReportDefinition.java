package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.parameter.RankingReportParameters;
import org.jetbrains.annotations.NotNull;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.COMMON_WHERE;

/**
 * Defines an application (API key) latency ranking report - applications with the most latency.
 * <p/>
 * Each rank will include the following information:
 * - latency
 * - api_key
 * <p/>
 */
public class ApplicationLatencyRankingReportDefinition extends RankingReportDefinition {
    public static String QUERY = "select api_key, if(sum(hits_total)>0,(sum(front_sum)/sum(hits_total)),0) as latency " +
            "from api_key_or_method_usage_view " + COMMON_WHERE +
            "group by api_key order by latency desc ";

    public ApplicationLatencyRankingReportDefinition(@NotNull final RankingReportParameters params) {
        super(params);
    }

    @Override
    protected String getQueryWithoutLimit() {
        return QUERY;
    }
}
