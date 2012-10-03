package com.l7tech.portal.reports.definition;

import com.l7tech.portal.reports.parameter.DefaultReportParameters;

import java.sql.PreparedStatement;

/**
 * A ReportDefinition contains the logic to generate a SQL query for a report based on a set of input parameters.
 * Each type of report supported by this API will have a concrete implementation of this base class.
 */
public abstract class ReportDefinition<P extends DefaultReportParameters> {

    protected final P params;

    protected ReportDefinition(final P params) {
        this.params = params;
    }

    public P getParams() {
        return params;
    }

    /**
     * Generate the complete SQL query string based on the specified ReportParameters used to construct
     * this ReportDefinition.
     *
     * @return complete SQL query string that can be used to query the data for the report
     * @see com.l7tech.portal.reports.parameter.DefaultReportParameters
     */
    public abstract String generateSQLQuery();

    /**
     * Sets the SQL parameters on the given PreparedStatement that are relevant for the report.
     *
     * @param statement the PreparedStatement on which to set the SQL parameters.
     */
    public abstract void setSQLParams(final PreparedStatement statement);

    /**
     * Creates a comma-separated string of question marks that can be used in a SQL query.
     *
     * @param numQuestionMarks number of question marks to use.
     * @return a comma-separated string of question marks.
     */
    private String createCommaSeparatedQuestionMarks(final int numQuestionMarks) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < numQuestionMarks; i++) {
            builder.append("?");
            if (i != numQuestionMarks - 1) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    /**
     * Replaces a substring with question marks used for SQL parameters within a string.
     *
     * @param originalString   the string that contains the substring to replace.
     * @param toReplace        the substring to replace with question marks.
     * @param numQuestionMarks the number of question marks to use for replacement.
     * @return a modified version of the original string that has question marks in place of the substring.
     */
    protected String replaceStringWithQuestionMarks(final String originalString, final String toReplace, final int numQuestionMarks) {
        final String questionMarks = createCommaSeparatedQuestionMarks(numQuestionMarks);
        return originalString.replace(toReplace, questionMarks);
    }
}
