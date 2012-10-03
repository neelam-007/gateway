package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.*;

/**
 * Defines an API/service usage report.
 */
public class ApiUsageReportDefinition extends ReportDefinition<ApiUsageReportParameters> {
    // this query will group data for multiple apis together

    @Deprecated
    // Deprecated : Group using Object IDs . Please use UUIDs
    public static String SQL_TEMPLATE_OID = COMMON_SELECT +
            "from api_usage_view " +
            COMMON_WHERE + "and api_id in (:apiOids) " +
            COMMON_GROUP_BY +
            ORDER_BY;


    public static String SQL_TEMPLATE = COMMON_SELECT +
            "from api_usage_view " +
            COMMON_WHERE + "and uuid in (:apiOids) " +
            COMMON_GROUP_BY +
            ORDER_BY;



    @Deprecated
    // Deprecated : Using Object IDs.  Please use UUIDs
    public static String SQL_TEMPLATE_GROUP_BY_API_ID_OID = COMMON_SELECT + ", api_name, api_id " +
            "from api_usage_view " +
            COMMON_WHERE + "and api_id in (:apiOids) " +
            COMMON_GROUP_BY + ", api_id " +
            ORDER_BY;

    // this query will separate data for multiple apis
    public static String SQL_TEMPLATE_GROUP_BY_UUID = COMMON_SELECT + ", api_name, uuid " +
            "from api_usage_view " +
            COMMON_WHERE + "and uuid in (:apiOids) " +
            COMMON_GROUP_BY + ", uuid " +
            ORDER_BY;

    @Deprecated
    // Deprecated : Using Object Ids. Please use UUIDs
    public static String SQL_TEMPLATE_GROUP_BY_API_ID_FILTER_BY_API_KEY = COMMON_SELECT + ", api_name, api_id " +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and api_id in (:apiOids) and api_key in (:apiKeys) " +
            COMMON_GROUP_BY + ", api_id " +
            ORDER_BY;

    public static String SQL_TEMPLATE_GROUP_BY_UUID_FILTER_BY_API_KEY = COMMON_SELECT + ", api_name, uuid " +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and uuid in (:apiOids) and api_key in (:apiKeys) " +
            COMMON_GROUP_BY + ", uuid " +
            ORDER_BY;


    @Deprecated
    // Deprecated : Using Object IDs. Please use UUIDs.
    public static String SQL_TEMPLATE_FILTER_BY_API_KEY_OID = COMMON_SELECT +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and api_id in (:apiOids) and api_key in (:apiKeys) " +
            COMMON_GROUP_BY +
            ORDER_BY;

    public static String SQL_TEMPLATE_FILTER_BY_UUID = COMMON_SELECT +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and uuid in (:apiOids) and api_key in (:apiKeys) " +
            COMMON_GROUP_BY +
            ORDER_BY;

    /**
     * True if data for multiple apis should be separated; false if they should be grouped together.
     */
    private final boolean separateDataByApi;

    public boolean isSeparateDataByApi() {
        return separateDataByApi;
    }

    public ApiUsageReportDefinition(final ApiUsageReportParameters params, final boolean separateDataByApi) {
        super(params);
        this.separateDataByApi = separateDataByApi;
    }

    @Override
    public String generateSQLQuery() {
        String query = null;
        int numApis = 0;
        if(params.getUuids() != null)
            numApis = params.getUuids().size();
        else if (params.getApis() != null)
            numApis = params.getApis().size();
        final List<String> apiKeys = params.getApiKeys();
        if (separateDataByApi) {
            if (apiKeys == null || apiKeys.isEmpty()) {
                query = replaceStringWithQuestionMarks(SQL_TEMPLATE_GROUP_BY_UUID, ":apiOids", numApis);
            } else {
                final String apiIdsReplaced = replaceStringWithQuestionMarks(SQL_TEMPLATE_GROUP_BY_UUID_FILTER_BY_API_KEY, ":apiOids", numApis);
                query = replaceStringWithQuestionMarks(apiIdsReplaced, ":apiKeys", apiKeys.size());
            }
        } else {
            if (apiKeys == null || apiKeys.isEmpty()) {
                query = replaceStringWithQuestionMarks(SQL_TEMPLATE, ":apiOids", numApis);
            } else {
                final String apiIdsReplaced = replaceStringWithQuestionMarks(SQL_TEMPLATE_FILTER_BY_UUID, ":apiOids", numApis);
                query = replaceStringWithQuestionMarks(apiIdsReplaced, ":apiKeys", apiKeys.size());
            }
        }
        return query;
    }

    @Override
    public void setSQLParams(final PreparedStatement statement) {
        //final List<Long> apis = params.getApis();
        final List<String> apis = params.getUuids();
        final List<String> apiKeys = params.getApiKeys();
        int i = 0;
        try {
            statement.setLong(++i, params.getStartTime());
            statement.setLong(++i, params.getEndTime());
            statement.setInt(++i, params.getBinResolution());
            for (final String api : apis) {
                statement.setString(++i, api);
            }
            if (apiKeys != null && !apiKeys.isEmpty()) {
                for (final String apiKey : apiKeys) {
                    statement.setString(++i, apiKey);
                }
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }
}
