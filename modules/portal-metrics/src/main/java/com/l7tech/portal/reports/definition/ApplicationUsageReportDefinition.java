package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.ApplicationUsageReportParameters;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.*;

/**
 * Defines an application (api key) usage report.
 */
public class ApplicationUsageReportDefinition extends ReportDefinition<ApplicationUsageReportParameters> {
    // this query will separate data for multiple api keys
    public static String SQL_TEMPLATE_GROUP_BY_KEY = COMMON_SELECT + ", api_key " +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and api_key in (:apiKeys) " +
            COMMON_GROUP_BY + ", api_key " +
            ORDER_BY;

    // this query will group data for multiple api keys together
    public static String SQL_TEMPLATE = COMMON_SELECT +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and api_key in (:apiKeys) " +
            COMMON_GROUP_BY +
            ORDER_BY;

    /**
     * True if data for multiple api keys should be separated; false if they should be grouped together.
     */
    final boolean separateDataByApiKey;

    public boolean isSeparateDataByApiKey() {
        return separateDataByApiKey;
    }

    public ApplicationUsageReportDefinition(final ApplicationUsageReportParameters params, final boolean separateDataByApiKey) {
        super(params);
        this.separateDataByApiKey = separateDataByApiKey;
    }

    @Override
    public String generateSQLQuery() {
        if (separateDataByApiKey) {
            final int size = params.getApplicationKeys().size();
            return replaceStringWithQuestionMarks(SQL_TEMPLATE_GROUP_BY_KEY, ":apiKeys", size);
        } else {
            return replaceStringWithQuestionMarks(SQL_TEMPLATE, ":apiKeys", params.getApplicationKeys().size());
        }
    }

    @Override
    public void setSQLParams(final PreparedStatement statement) {
        final List<String> applicationKeys = params.getApplicationKeys();
        int i = 0;
        try {
            statement.setLong(++i, params.getStartTime());
            statement.setLong(++i, params.getEndTime());
            statement.setInt(++i, params.getBinResolution());
            for (final String key : applicationKeys) {
                statement.setString(++i, key);
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }
}
