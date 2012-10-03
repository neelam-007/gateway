package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines an API usage comparison report that compares usage for multiple APIs (services).
 */
public class ApiUsageComparisonReportDefinition extends ReportDefinition<ApiUsageReportParameters> {
//    private static final String SQL_TEMPLATE = "select sum(hits_total) sum_hits_total, api_id, api_name " +
//            "from api_usage_view " +
//            ReportQueryConstants.COMMON_WHERE + "and api_id in (:apiOids)" +
//            "group by api_id, api_name";

    private static final String SQL_TEMPLATE = "select sum(hits_total) sum_hits_total, uuid, api_name " +
            "from api_usage_view " +
            ReportQueryConstants.COMMON_WHERE + "and uuid in (:apiOids)" +
            "group by uuid, api_name";


//    public static String SQL_TEMPLATE_FILTER_BY_API_KEY = "select sum(hits_total) sum_hits_total, api_id, api_name  " +
//            "from api_key_or_method_usage_view " +
//            ReportQueryConstants.COMMON_WHERE + " and api_id in (:apiOids) and api_key in (:apiKeys) " +
//            "group by api_id, api_name";

    public static String SQL_TEMPLATE_FILTER_BY_API_KEY = "select sum(hits_total) sum_hits_total, uuid, api_name  " +
            "from api_key_or_method_usage_view " +
            ReportQueryConstants.COMMON_WHERE + " and uuid in (:apiOids) and api_key in (:apiKeys) " +
            "group by uuid, api_name";


    public ApiUsageComparisonReportDefinition(final ApiUsageReportParameters params) {
        super(params);
    }

    @Override
    public String generateSQLQuery() {
        String query = null;
        int numApiIds = 0;
        if(params.getApis() != null)
            numApiIds = params.getApis().size();
        else
            numApiIds = params.getUuids().size();
        final List<String> apiKeys = params.getApiKeys();
        if (apiKeys == null || apiKeys.isEmpty()) {
            query = replaceStringWithQuestionMarks(SQL_TEMPLATE, ":apiOids", numApiIds);
        } else {
            final String apiIdsReplaced = replaceStringWithQuestionMarks(SQL_TEMPLATE_FILTER_BY_API_KEY, ":apiOids", numApiIds);
            query = replaceStringWithQuestionMarks(apiIdsReplaced, ":apiKeys", apiKeys.size());
        }
        return query;
    }

    @Override
    /* public void setSQLParams(PreparedStatement statement) {
        final List<Long> apis = params.getApis();
        final List<String> apiKeys = params.getApiKeys();
        int i = 0;
        try {
            statement.setLong(++i, params.getStartTime());
            statement.setLong(++i, params.getEndTime());
            statement.setInt(++i, params.getBinResolution());
            for (final Long api : apis) {
                statement.setLong(++i, api);
            }
            if (apiKeys != null && !apiKeys.isEmpty()) {
                for (final String key : apiKeys) {
                    statement.setString(++i, key);
                }
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }*/

    public void setSQLParams(PreparedStatement statement) {
        final ArrayList<String> uuids = params.getUuids();
        final List<String> apiKeys = params.getApiKeys();
        int i = 0;
        try {
            statement.setLong(++i, params.getStartTime());
            statement.setLong(++i, params.getEndTime());
            statement.setInt(++i, params.getBinResolution());
            for (final String api : uuids) {
                statement.setString(++i, api);
            }
            if (apiKeys != null && !apiKeys.isEmpty()) {
                for (final String key : apiKeys) {
                    statement.setString(++i, key);
                }
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }
}
