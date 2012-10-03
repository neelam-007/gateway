package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.ApiQuotaUsageReportParameters;
import com.l7tech.portal.reports.parameter.DefaultReportParameters;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.List;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.COMMON_WHERE;

/**
 * Defines an API/service usage report.
 */
public class ApiQuotaUsageReportDefinition extends ReportDefinition<ApiQuotaUsageReportParameters> {
    /**
     * Filter results to a list of UUIDs
     */
    public static String SQL_UUID_TEST = "and a.uuid in (:uuids) ";

    /**
     * Filter results to a an api key
     */
    public static String SQL_APIKEY_TEST = "and api_key = ? ";

    /**
     * Common Group by clause for the SQL.
     */
    public static String SQL_GROUP_BY = "group by api_key, uuid";

    /**
     * Gets the latest bin_start_time given a resolution and time range.
     */
    private static String GET_LATEST_START_TIME = "(select bin_start_time from api_key_or_method_usage_view " + COMMON_WHERE + "order by bin_start_time desc limit 1)";

    /**
     * Note :
     * THe body of the select statement.
     * The Bin Resolution will be determined by value of the Quota Range
     */
    private static final String SQL_TEMPLATE_HOURS_MONTHS = "select api_key, " +
            "uuid, " +
            "sum(hits_total) as hits from api_key_or_method_usage_view a " + COMMON_WHERE + SQL_UUID_TEST + SQL_APIKEY_TEST + SQL_GROUP_BY;


    private static final String SQL_TEMPLATE_MINS_SECS = "select api_key, a.uuid, " +
            "(sum(hits_total) / (interval_size / (1000*60))) per_min_avg, " +
            "(sum(hits_total) / (interval_size / (1000*1))) per_sec_avg " +
            "from api_key_or_method_usage_view a join service_metrics b on a.metric_id = b.objectid " +
            "where a.resolution = ? and bin_start_time = " +
            GET_LATEST_START_TIME + SQL_UUID_TEST + SQL_APIKEY_TEST + SQL_GROUP_BY + ", bin_start_time, interval_size";


    /**
     * @param params @see ApiQuotaUsageReportParameters
     */
    public ApiQuotaUsageReportDefinition(final ApiQuotaUsageReportParameters params) {
        super(params);

    }

    public String generateSQLQuery(@NotNull final List<String> uuids, final @NotNull DefaultReportParameters.QuotaRange range) {
        String query = null;
        switch (range) {
            case SECOND:
            case MINUTE:
                query = replaceStringWithQuestionMarks(SQL_TEMPLATE_MINS_SECS, ":uuids", uuids.size());
                break;
            case HOUR:
            case DAY:
            case MONTH:
                query = replaceStringWithQuestionMarks(SQL_TEMPLATE_HOURS_MONTHS, ":uuids", uuids.size());
                break;
        }
        return query;
    }

    @Override
    public String generateSQLQuery() {
        throw new UnsupportedOperationException("Use generateSQLQuery(List<String> uuids) method instead");
    }

    @Override
    public void setSQLParams(final PreparedStatement statement) {
        throw new UnsupportedOperationException("Use setSQLParams(PreparedStatement statement, List<String> uuids, DefaultReportParameters.QuotaRange range) method instead");
    }

    public void setSQLParams(@NotNull final PreparedStatement statement, @NotNull final List<String> uuids, @NotNull final DefaultReportParameters.QuotaRange range) {
        final String apiKey = params.getApiKey();
        long startTime = params.getStartTime(range);
        int i = 0;
        try {
            if (range.equals(DefaultReportParameters.QuotaRange.SECOND) || range.equals(DefaultReportParameters.QuotaRange.MINUTE)) {
                statement.setInt(++i, getBinResolution(range));
            }
            statement.setLong(++i, startTime);
            statement.setLong(++i, Calendar.getInstance().getTimeInMillis());
            statement.setInt(++i, getBinResolution(range));
            if (uuids != null && !uuids.isEmpty()) {
                for (final String uuid : uuids) {
                    statement.setString(++i, uuid);
                }
            }
            statement.setString(++i, apiKey);
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }

    private final int getBinResolution(DefaultReportParameters.QuotaRange range) {
        int res = DefaultReportParameters.BIN_RESOLUTION_HOURLY;
        switch (range) {
            case SECOND:
            case MINUTE:
            case HOUR:
                res = DefaultReportParameters.BIN_RESOLUTION_CUSTOM;
                break;
            case DAY:
            case MONTH:
                res = DefaultReportParameters.BIN_RESOLUTION_HOURLY;
                break;
        }
        return res;
    }

}
