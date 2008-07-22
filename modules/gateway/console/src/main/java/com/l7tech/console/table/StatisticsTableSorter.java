/*
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.console.table;

/**
 * This class encapsulates the data model for statistics with sorting capability
 */

import com.l7tech.gateway.common.logging.StatisticsRecord;

import javax.swing.table.DefaultTableModel;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class StatisticsTableSorter extends FilteredDefaultTableModel {
    static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    boolean ascending = true;
    int columnToSort = 0;
    int compares;
    private Pattern serviceNamePattern;
    private Vector<StatisticsRecord> rawData;
    private StatisticsRecord[] displayData;

    public StatisticsTableSorter() {
    }

    public StatisticsTableSorter(DefaultTableModel model) {
        setModel(model);
    }

    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    /**
     * Sets the table data.
     * Filtering will be applied (assigned using {@link #setFilter}.
     * Needs to call {@link #fireTableDataChanged} afterwards to cause repaint.
     *
     * @param data  vector of service statistics
     */
    public void setData(Vector<StatisticsRecord> data) {
        this.rawData = data;
        sortData(columnToSort, false);
    }

    /**
     * Sets the service name filter.
     * Needs to call {@link #fireTableDataChanged} afterwards to cause repaint.
     *
     * @param pattern a regular expression pattern to search within service names;
     *                      empty or null for no filtering
     * @throws PatternSyntaxException if the pattern's syntax is invalid
     */
    public void setFilter(String pattern) {
        if (pattern == null || pattern.length()==0) {
            serviceNamePattern = null;
        } else {
            serviceNamePattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        }
        sortData(columnToSort, false);
    }

    public int getSortedColumn(){
        return columnToSort;
    }

    public boolean isAscending(){
        return ascending;
    }

    public void sortData(int column, boolean orderToggle) {

        if(orderToggle){
            ascending = !ascending;
        }

        // always sort in ascending order if the user select a new column
        if(column != columnToSort){
            ascending = true;
        }
        // save the column index
        columnToSort = column;

        StatisticsRecord[] filtered = null;
        if (serviceNamePattern != null) {
            try {
                List<StatisticsRecord> filteredList = new ArrayList<StatisticsRecord>();
                for (StatisticsRecord sr : rawData) {
                    if (serviceNamePattern.matcher(sr.getServiceName()).find()) {
                        filteredList.add(sr);
                    }
                }
                filtered = filteredList.toArray(new StatisticsRecord[filteredList.size()]);
            } catch (PatternSyntaxException e) {
                // Default to no filtering.
                filtered = rawData.toArray(new StatisticsRecord[rawData.size()]);
            }
        } else {
            // No filtering.
            filtered = rawData.toArray(new StatisticsRecord[rawData.size()]);
        }
        Arrays.sort(filtered, new ColumnSorter(columnToSort, ascending));
        displayData = filtered;
    }

    @Override
    public int getRowCount() {
        return displayData == null ? 0 : displayData.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return displayData[row].getServiceName();
            case 1:
                return new Long(displayData[row].getNumRoutingFailure());
            case 2:
                return new Long(displayData[row].getNumPolicyViolation());
            case 3:
                return new Long(displayData[row].getNumSuccess());
            case 4:
                return new Long(displayData[row].getNumSuccessLastMinute());
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public class ColumnSorter implements Comparator<StatisticsRecord> {
        private boolean ascending;
        private int column;

        ColumnSorter(int column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;
        }

        public int compare(StatisticsRecord a, StatisticsRecord b) {

            Object elementA = null;
            Object elementB = null;

            switch (column) {
                case 0:
                    elementA = a.getServiceName();
                    elementB = b.getServiceName();
                    break;
                case 1:
                    elementA = new Long(a.getNumRoutingFailure());
                    elementB = new Long(b.getNumRoutingFailure());
                    break;
                case 2:
                    elementA = new Long(a.getNumPolicyViolation());
                    elementB = new Long(b.getNumPolicyViolation());
                    break;
                case 3:
                    elementA = new Long(a.getNumSuccess());
                    elementB = new Long(b.getNumSuccess());
                    break;
                case 4:
                    elementA = new Long(a.getNumSuccessLastMinute());
                    elementB = new Long(b.getNumSuccessLastMinute());
                    break;
                default:
                    logger.warning("Bad Statistics Table Column: " + column);
                    break;
            }

            // Treat empty strains like nulls
            if (elementA instanceof String && ((String)elementA).length() == 0) {
                elementA = null;
            }
            if (elementB instanceof String && ((String)elementB).length() == 0) {
                elementB = null;
            }

            // Sort nulls so they appear last, regardless
            // of sort order
            if (elementA == null && elementB == null) {
                return 0;
            } else if (elementA == null) {
                return 1;
            } else if (elementB == null) {
                return -1;
            } else {
                if (ascending) {
                    if (elementA instanceof String) {
                        return ((String) elementA).compareToIgnoreCase((String) elementB);
                    } else if (elementA instanceof Long) {
                        if (((Long) elementA).longValue() == ((Long) elementB).longValue()) {
                            return 0;
                        } else if (((Long) elementA).longValue() > ((Long) elementB).longValue()) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else {
                        logger.warning("Unsupported data type for comparison, sorting is performed.");
                        return 0;
                    }
                } else {
                    if (elementA instanceof String) {
                        return ((String) elementB).compareToIgnoreCase((String) elementA);
                    } else if (elementA instanceof Long) {
                        if (((Long) elementA).longValue() == ((Long) elementB).longValue()) {
                            return 0;
                        } else if (((Long) elementA).longValue() > ((Long) elementB).longValue()) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        logger.warning("Unsupported data type for comparison, sorting is not performed.");
                        return 0;
                    }
                }
            }
        }
    }
}

