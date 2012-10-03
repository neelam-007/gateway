package com.l7tech.portal.reports.definition;

/**
 * Convenience class for holding SQL query constants for reports.
 */
public final class ReportQueryConstants {
    public static final String COMMON_SELECT = "select sum(hits_total) sum_hits_total, sum(hits_success) sum_hits_success, " +
            "sum(hits_total_errors) sum_hits_total_errors, if(sum(hits_total)>0,(sum(front_sum)/sum(hits_total)),0) sum_latency, " +
            "if(sum(hits_total)>0,((sum(front_sum) - sum(back_sum)) / sum(hits_total)),0) sum_gateway_latency, " +
            "if(sum(hits_total)>0,(sum(back_sum) / sum(hits_total)),0) sum_back_latency, bin_start_time ";

    public static final String COMMON_WHERE = "where bin_start_time >= ? and bin_end_time < ? and resolution = ? ";
    public static final String COMMON_GROUP_BY = "group by bin_start_time ";
    public static final String ORDER_BY = "order by bin_start_time asc";

    private ReportQueryConstants() {
        // do not construct
    }
}
