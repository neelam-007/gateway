package com.l7tech.portal.reports.format;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;

/**
 * Abstract parent for all ResultSet formatters.
 */
public abstract class AbstractResultSetFormatter implements ResultSetFormatter {
    private static final Logger LOGGER = Logger.getLogger(AbstractResultSetFormatter.class);
    private static final int MINIMUM_INTERVAL = 1;

    /**
     * @return the value that should be used if the grouping column has a null value.
     */
    abstract String getNullValueForGroupingColumn();

    /**
     * Converts a grouped result set map to a string.
     *
     * @param map     the result set as a map to convert to a string.
     * @param options the ResultSetFormatOptions that determine how the string will be formatted.
     * @return the grouped result set map as a string.
     * @throws Exception if an error occurs converting the map to a string.
     */
    abstract String mapToString(final Map<String, List<Map<String, Object>>> map, final ResultSetFormatOptions options);

    /**
     * Converts a result set list to a string.
     *
     * @param list    the result set as a list to convert to a string.
     * @param options the ResultSetFormatOptions that determine how the string will be formatted.
     * @return the list as a string.
     * @throws Exception if an error occurs converting the list to a string.
     */
    abstract String listToString(final List<Map<String, Object>> list, final ResultSetFormatOptions options);

    /**
     * Formats result set data into a String.
     *
     * @param resultSet     the ResultSet to format.
     * @param formatOptions the formatting options.
     */
    public String format(final ResultSet resultSet, final ResultSetFormatOptions formatOptions) throws FormatException {
        Validate.notNull(resultSet, "Result set cannot be null.");
        String formattedResultSet = "";
        try {
            if (formatOptions == null || StringUtils.isBlank(formatOptions.getGroupingColumnName())) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Formatting without grouping.");
                }
                final List<Map<String, Object>> rows = getResultSetData(resultSet, formatOptions);
                formattedResultSet = listToString(rows, formatOptions);
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Formatting with grouping.");
                }
                final Map<String, List<Map<String, Object>>> groupedRows = getGroupedResultSetData(resultSet, formatOptions);
                formattedResultSet = mapToString(groupedRows, formatOptions);
            }
        } catch (final Exception e) {
            throw new FormatException("Error formatting result set: " + e.getMessage(), e);
        }
        return formattedResultSet;
    }

    private boolean columnExists(final ResultSetMetaData metaData, final String columnName) throws SQLException {
        final int numColumns = metaData.getColumnCount();
        for (int i = 1; i <= numColumns; i++) {
            if (columnName.equalsIgnoreCase(metaData.getColumnLabel(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean shouldAddMissingData(final ResultSetFormatOptions formatOptions, final ResultSetMetaData metaData) throws SQLException {
        boolean addMissingData = true;
        if (formatOptions == null || StringUtils.isBlank(formatOptions.getNumericColumnName())
                || !columnExists(metaData, formatOptions.getNumericColumnName())
                || formatOptions.getExpectedInterval() == null
                || formatOptions.getExpectedInterval() < MINIMUM_INTERVAL) {
            addMissingData = false;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Missing data will not be given default values.");
            }
        }
        return addMissingData;
    }

    /**
     * Converts the result set into a list of map where key = column label and value = column value.
     */
    private List<Map<String, Object>> getResultSetData(final ResultSet resultSet, final ResultSetFormatOptions options) throws SQLException {
        final List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        if (resultSet != null) {
            final ResultSetMetaData metaData = resultSet.getMetaData();
            final boolean shouldAddMissingData = shouldAddMissingData(options, metaData);
            final int numColumns = metaData.getColumnCount();
            while (resultSet.next()) {
                final Map<String, Object> currentRow = new HashMap<String, Object>();
                for (int i = 1; i <= numColumns; i++) {
                    // use column label in case the sql query renames columns
                    // use lower case because some implementations return all upper case
                    final String columnName = metaData.getColumnLabel(i)
                            .toLowerCase();
                    final Object value = resultSet.getObject(i);
                    currentRow.put(columnName, value);
                }

                //add missing data to middle
                if (shouldAddMissingData) {
                    addMissingDataBetweenLastAndCurrentRow(options, rows, currentRow);
                }
                rows.add(currentRow);
            }

            if (shouldAddMissingData) {
                addMissingDataBetweenStartAndFirstRow(options, rows);
                addMissingDataBetweenLastRowAndEnd(options, rows);
            }
        }
        return rows;
    }

    /**
     * Groups the result set data into a map.
     * <p/>
     * Key = value stored in the column identified by groupingColumnName.
     * <p/>
     * Value = list of maps where each map is a result set row (sub map key = column name, sub map value = column value)
     */
    private Map<String, List<Map<String, Object>>> getGroupedResultSetData(final ResultSet resultSet, final ResultSetFormatOptions options) throws SQLException {
        final Map<String, List<Map<String, Object>>> groupedRows = new HashMap<String, List<Map<String, Object>>>();
        boolean shouldAddMissingData = false;
        if (resultSet != null) {
            final ResultSetMetaData metaData = resultSet.getMetaData();
            shouldAddMissingData = shouldAddMissingData(options, metaData);
            final String groupingColumnName = options.getGroupingColumnName();
            final int numColumns = metaData.getColumnCount();
            if (numColumns > 0 && columnExists(metaData, groupingColumnName)) {
                while (resultSet.next()) {
                    final Map<String, Object> currentRow = new HashMap<String, Object>();
                    String groupingColumnValue = null;
                    for (int i = 1; i <= numColumns; i++) {
                        // must use column label in case the sql query renames columns
                        final String columnName = metaData.getColumnLabel(i)
                                .toLowerCase();
                        final Object value = resultSet.getObject(i);
                        if (columnName.equals(groupingColumnName)) {
                            groupingColumnValue = (value == null ? getNullValueForGroupingColumn() : value.toString());
                        } else {
                            currentRow.put(columnName, value);
                        }
                    }
                    if (!groupedRows.containsKey(groupingColumnValue)) {
                        groupedRows.put(groupingColumnValue, new ArrayList<Map<String, Object>>());
                    }
                    final List<Map<String, Object>> group = groupedRows.get(groupingColumnValue);
                    if (shouldAddMissingData) {
                        addMissingDataBetweenLastAndCurrentRow(options, group, currentRow);
                    }
                    group.add(currentRow);
                }

                if (shouldAddMissingData) {
                    for (final List<Map<String, Object>> group : groupedRows.values()) {
                        addMissingDataBetweenStartAndFirstRow(options, group);
                        addMissingDataBetweenLastRowAndEnd(options, group);
                    }
                }

            } else if (numColumns > 0) {
                throw new FormatException("Grouping column does not exist in result set: " + groupingColumnName);
            }
        }

        // add any missing groups
        if (options.getExpectedGroups() != null) {
            for (final String group : options.getExpectedGroups()) {
                if (!groupedRows.containsKey(group)) {
                    if (shouldAddMissingData && options.getExpectedStartValue() != null && options.getExpectedEndValue() != null) {
                        // cannot add missing data for this group without expected start value and end value
                        final List<Map<String, Object>> missingData = createMissingData(options, options.getExpectedStartValue(), true, options.getExpectedEndValue(), true);
                        groupedRows.put(group, missingData);
                    } else {
                        groupedRows.put(group, Collections.<Map<String, Object>>emptyList());
                    }
                }
            }
        }

        return groupedRows;
    }

    /**
     * Creates list of missing 'rows' of data between the range given as a list of map where key = column and value = column value.
     */
    private List<Map<String, Object>> createMissingData(final ResultSetFormatOptions options, final Long start, final boolean startInclusive, final Long end, final boolean endInclusive) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating missing data between " + start + " and " + end + ". Start inclusive: " + startInclusive + ", end inclusive: " + endInclusive);
        }
        final List<Map<String, Object>> missingData = new ArrayList<Map<String, Object>>();
        Long currentValue = null;
        if (end > start) {
            if (startInclusive) {
                currentValue = start;
            } else {
                currentValue = start + options.getExpectedInterval();
            }
            if (endInclusive) {
                while (currentValue <= end) {
                    missingData.add(createMissingRow(options, currentValue));
                    currentValue = currentValue + options.getExpectedInterval();
                }
            } else {
                while (currentValue < end) {
                    missingData.add(createMissingRow(options, currentValue));
                    currentValue = currentValue + options.getExpectedInterval();
                }
            }
        } else if (start > end) {
            if (startInclusive) {
                currentValue = start;
            } else {
                currentValue = start - options.getExpectedInterval();
            }
            if (endInclusive) {
                while (currentValue >= end) {
                    missingData.add(createMissingRow(options, currentValue));
                    currentValue = currentValue - options.getExpectedInterval();
                }
            } else {
                while (currentValue > end) {
                    missingData.add(createMissingRow(options, currentValue));
                    currentValue = currentValue - options.getExpectedInterval();
                }
            }
        }
        return missingData;
    }

    /**
     * Creates a single row of data as a map where key = column and value = column value.
     */
    private Map<String, Object> createMissingRow(final ResultSetFormatOptions options, final Long valueToInsert) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Creating missing row with value: " + valueToInsert);
        }
        final Map<String, Object> missingRow = new HashMap<String, Object>();
        if (options.getDefaultColumnValues() != null) {
            for (final Map.Entry<String, Object> entry : options.getDefaultColumnValues().entrySet()) {
                missingRow.put(entry.getKey().toLowerCase(), entry.getValue());
            }
        }
        missingRow.put(options.getNumericColumnName(), valueToInsert);
        return missingRow;
    }

    private void addMissingDataBetweenLastAndCurrentRow(final ResultSetFormatOptions options, final List<Map<String, Object>> existingRows, final Map<String, Object> currentRow) {
        if (!existingRows.isEmpty()) {
            try {
                final String numericColumnName = options.getNumericColumnName();
                final Map<String, Object> lastRow = existingRows.get(existingRows.size() - 1);
                final Long currentValue = Long.valueOf(String.valueOf(currentRow.get(numericColumnName)));
                final Long previousValue = Long.valueOf(String.valueOf(lastRow.get(numericColumnName)));
                existingRows.addAll(createMissingData(options, previousValue, false, currentValue, false));
            } catch (final NumberFormatException e) {
                LOGGER.error("Failed to add missing data between rows. Encountered invalid value for column expected have an integer. Error: " + e.getMessage(), e);
            }
        }
    }

    private void addMissingDataBetweenLastRowAndEnd(final ResultSetFormatOptions options, final List<Map<String, Object>> rows) {
        final Long expectedEndValue = options.getExpectedEndValue();
        if (expectedEndValue != null && !rows.isEmpty()) {
            final Map<String, Object> lastRow = rows.get(rows.size() - 1);
            try {
                final Long lastValue = Long.valueOf(String.valueOf(lastRow.get(options.getNumericColumnName())));
                if (!expectedEndValue.equals(lastValue)) {
                    final List<Map<String, Object>> missingEndData = createMissingData(options, lastValue, false, expectedEndValue, true);
                    if (!missingEndData.isEmpty()) {
                        rows.addAll(missingEndData);
                    }
                }
            } catch (final NumberFormatException e) {
                LOGGER.error("Failed to add missing data between last row and expected end. Encountered invalid value for column expected have an integer. Error: " + e.getMessage(), e);
            }
        }
    }

    private void addMissingDataBetweenStartAndFirstRow(final ResultSetFormatOptions options, final List<Map<String, Object>> rows) {
        final Long expectedStartValue = options.getExpectedStartValue();
        if (expectedStartValue != null && !rows.isEmpty()) {
            final Map<String, Object> firstRow = rows.get(0);
            try {
                final Long firstValue = Long.valueOf(String.valueOf(firstRow.get(options.getNumericColumnName())));
                if (!expectedStartValue.equals(firstValue)) {
                    final List<Map<String, Object>> missingStartData = createMissingData(options, expectedStartValue, true, firstValue, false);
                    if (!missingStartData.isEmpty()) {
                        rows.addAll(0, missingStartData);
                    }
                }
            } catch (final NumberFormatException e) {
                LOGGER.error("Failed to add missing data between expected start and first row. Encountered invalid value for column expected have an integer. Error: " + e.getMessage(), e);
            }
        }
    }
}
