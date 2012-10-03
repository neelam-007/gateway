package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.MethodUsageReportParameters;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static com.l7tech.portal.reports.definition.ReportQueryConstants.*;

/**
 * Defines an API method usage report.
 */
public class MethodUsageReportDefinition extends ReportDefinition<MethodUsageReportParameters> {
    // this query will group data for multiple api methods together
    public static String SQL_TEMPLATE = COMMON_SELECT +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and api_method in (:apiMethods) " +
            COMMON_GROUP_BY +
            ORDER_BY;

    // this query will separate data for multiple api methods
    public static String SQL_TEMPLATE_GROUP_BY_METHOD = COMMON_SELECT + ", api_method " +
            "from api_key_or_method_usage_view " +
            COMMON_WHERE + "and api_method in (:apiMethods) " +
            COMMON_GROUP_BY + ", api_method " +
            ORDER_BY;

    private final boolean separateDataByApiMethod;

    public boolean isSeparateDataByApiMethod() {
        return separateDataByApiMethod;
    }

    public MethodUsageReportDefinition(final MethodUsageReportParameters params, final boolean separateDataByApiMethod) {
        super(params);
        this.separateDataByApiMethod = separateDataByApiMethod;
    }

    @Override
    public String generateSQLQuery() {
        if (separateDataByApiMethod) {
            return replaceStringWithQuestionMarks(SQL_TEMPLATE_GROUP_BY_METHOD, ":apiMethods", params.getApiMethods().size());
        } else {
            return replaceStringWithQuestionMarks(SQL_TEMPLATE, ":apiMethods", params.getApiMethods().size());

        }
    }

    @Override
    public void setSQLParams(final PreparedStatement statement) {
        final List<String> apiMethods = params.getApiMethods();
        int i = 0;
        try {
            statement.setLong(++i, params.getStartTime());
            statement.setLong(++i, params.getEndTime());
            statement.setInt(++i, params.getBinResolution());
            for (final String method : apiMethods) {
                statement.setString(++i, method);
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }
}
