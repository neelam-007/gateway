package com.l7tech.portal.reports.format;

import java.util.List;
import java.util.Map;

/**
 * Options for formatting a result set.
 */
public class ResultSetFormatOptions {
    private String groupingColumnName;
    private String numericColumnName;
    private Long expectedInterval;
    private Long expectedStartValue;
    private Long expectedEndValue;
    private Map<String, Object> defaultColumnValues;
    private List<String> expectedGroups;

    public String getGroupingColumnName() {
        return groupingColumnName;
    }

    /**
     * Optional. If specified, the result set data will be grouped by common values found for the given column.
     *
     * @param groupingColumnName the name of the column to group the result set data by.
     */
    public void setGroupingColumnName(final String groupingColumnName) {
        this.groupingColumnName = groupingColumnName;
    }

    public String getNumericColumnName() {
        return numericColumnName;
    }

    /**
     * Optional (related to expectedInterval and defaultColumnValues). If specified, the values in this column will determine if any rows are 'missing' and a row will be added for any missing row.
     *
     * @param numericColumnName the name of a numeric column that determines if the result set is 'missing' a row.
     */
    public void setNumericColumnName(final String numericColumnName) {
        this.numericColumnName = numericColumnName;
    }

    public Long getExpectedInterval() {
        return expectedInterval;
    }

    /**
     * Optional (related to numericColumnName and defaultColumnValues). Specifies a regular interval that the result set is expected to have data for.
     * If a row is 'missing' then default values will be inserted for the columns identified by the setDefaultColumnValues.
     *
     * @param expectedInterval the regular interval that the result set is expected to have data for.
     */
    public void setExpectedInterval(final Long expectedInterval) {
        this.expectedInterval = expectedInterval;
    }

    public Long getExpectedStartValue() {
        return expectedStartValue;
    }

    /**
     * Optional. The expected starting value of the numericColumnName column.
     *
     * @param expectedStartValue the expected initial value of the numericColumnName column.
     */
    public void setExpectedStartValue(final Long expectedStartValue) {
        this.expectedStartValue = expectedStartValue;
    }

    public Long getExpectedEndValue() {
        return expectedEndValue;
    }

    /**
     * Optional. The expected ending value of the numericColumnName column.
     *
     * @param expectedEndValue the expected ending value of the numericColumnName column.
     */
    public void setExpectedEndValue(final Long expectedEndValue) {
        this.expectedEndValue = expectedEndValue;
    }

    public Map<String, Object> getDefaultColumnValues() {
        return defaultColumnValues;
    }

    /**
     * Optional (related to numericColumnName and expectedInterval). Specifies default values to give to columns when inserting data for missing rows.
     *
     * @param defaultColumnValues a map of default column values (key=column name, value=column value) to give to data inserted for missing rows.
     */
    public void setDefaultColumnValues(final Map<String, Object> defaultColumnValues) {
        this.defaultColumnValues = defaultColumnValues;
    }

    /**
     * Optional (related to groupingColumnName). If specified along with groupingColumnName, any groups that are expected but are not found in the result set
     * will be included in the formatted data.
     *
     * Groups that are in the result set but not in the expected groups will NOT be removed from the formatted data.
     *
     * @return a list of groups that the formatted data is expected to have.
     */
    public List<String> getExpectedGroups() {
        return expectedGroups;
    }

    public void setExpectedGroups(final List<String> expectedGroups) {
        this.expectedGroups = expectedGroups;
    }
}
