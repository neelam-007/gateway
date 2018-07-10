package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.UsageSummaryReportParameters;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.COMMON_WHERE;

/**
 * Usage Summary Report is a report generated by the Portal to sum up the hourly usage for a day for all the api keys
 * provided. Only hourly bins are used so any hits against a key that are only in the 15 minute bins are not included.
 *
 * @author jbagtas
 */
public class UsageSummaryReportDefinition extends ReportDefinition<UsageSummaryReportParameters> {

    // this query will group data for multiple api keys together
    public static String SQL_TEMPLATE = "select sum(hits_total) sum_hits_total, " +
            "bin_start_time as bin_start_date " +
            "from api_key_or_method_usage_view " +
            "where bin_start_time >= ? and bin_end_time < ? and resolution = 1 and api_key in (:apiKeys) " +
            "group by (date(FROM_UNIXTIME(bin_start_time/1000))) " +
            "order by bin_start_date asc";

    public UsageSummaryReportDefinition(final UsageSummaryReportParameters params) {
        super(params);
    }

    @Override
    public String generateSQLQuery() {
        return replaceStringWithQuestionMarks(SQL_TEMPLATE, ":apiKeys", params.getApplicationKeys().size());
    }

    @Override
    public void setSQLParams(final PreparedStatement statement) {
        final List<String> applicationKeys = params.getApplicationKeys();
        int i = 0;
        try {
            statement.setLong(++i, params.getStartTime());
            statement.setLong(++i, params.getEndTime());
            for (final String key : applicationKeys) {
                statement.setString(++i, key);
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }
}
