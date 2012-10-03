package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.ReportGenerationException;
import com.l7tech.portal.reports.parameter.RankingReportParameters;
import org.jetbrains.annotations.NotNull;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Abstract parent class for all ranking reports.
 */
public abstract class RankingReportDefinition extends ReportDefinition<RankingReportParameters> {
    public static final String LIMIT = "limit ?";

    protected RankingReportDefinition(@NotNull final RankingReportParameters params) {
        super(params);
    }

    /**
     * @return the SQL query without a limit clause.
     */
    protected abstract String getQueryWithoutLimit();

    @Override
    public String generateSQLQuery() {
        if (params.getLimit() != null) {
            return getQueryWithoutLimit() + LIMIT;
        } else {
            return getQueryWithoutLimit();
        }
    }

    @Override
    public void setSQLParams(@NotNull final PreparedStatement statement) {
        int count = 0;
        try {
            statement.setLong(++count, params.getStartTime());
            statement.setLong(++count, params.getEndTime());
            statement.setInt(++count, params.getBinResolution());
            if (params.getLimit() != null) {
                statement.setInt(++count, params.getLimit());
            }
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error setting sql parameters: " + e.getMessage(), e);
        }
    }
}
