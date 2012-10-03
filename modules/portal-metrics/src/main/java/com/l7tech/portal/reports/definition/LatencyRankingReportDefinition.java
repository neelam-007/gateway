package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.parameter.RankingReportParameters;
import org.jetbrains.annotations.NotNull;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.COMMON_WHERE;

/**
 * Defines a latency ranking report - metrics bins with the highest latency - which can be helpful for finding information about latency spikes.
 * <p/>
 * Each rank will include the following information:
 * - latency
 * - api_key
 * - bin_start_time
 * <p/>
 * An application/key can appear on the report more than once if it happens to be linked to multiple bins with high latency.
 */
public class LatencyRankingReportDefinition extends RankingReportDefinition {
    public static String QUERY = "select api_key, bin_start_time, if(hits_total>0,front_sum/hits_total,0) as latency " +
            "from api_key_or_method_usage_view " + COMMON_WHERE +
            "order by latency desc ";

    public LatencyRankingReportDefinition(@NotNull final RankingReportParameters params) {
        super(params);
    }

    @Override
    protected String getQueryWithoutLimit() {
        return QUERY;
    }
}
