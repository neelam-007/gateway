package com.l7tech.portal.reports.format;

import java.sql.ResultSet;

/**
 * Formatter for open ResultSets.
 */
public interface ResultSetFormatter {

    /**
     * Formats the given ResultSet into a String representation.
     *
     * @param resultSet     the ResultSet to convert to a String. Must not be closed.
     * @param formatOptions the formatting options.
     * @return the ResultSet formatted as a String.
     * @throws FormatException if the ResultSet cannot be formatted to a String.
     */
    String format(final ResultSet resultSet, final ResultSetFormatOptions formatOptions) throws FormatException;
}
