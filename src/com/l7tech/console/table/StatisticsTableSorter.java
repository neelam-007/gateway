package com.l7tech.console.table;

/*
 * This class encapsulates the data model for statistics with sorting capability
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

import com.l7tech.logging.StatisticsRecord;

import java.util.*;
import java.util.logging.Logger;
import javax.swing.table.*;

public class StatisticsTableSorter extends FilteredDefaultTableModel {
    static Logger logger = Logger.getLogger(StatisticsTableSorter.class.getName());
    boolean ascending = true;
    int columnToSort = 0;
    int compares;
    private Vector data;
    private Object[] sortedData = null;

    public StatisticsTableSorter() {
    }

    public StatisticsTableSorter(DefaultTableModel model) {
        setModel(model);
    }

    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    public void setData(Vector data) {
        this.data = data;
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
            ascending = ascending ? false : true;
        }

        // always sort in ascending order if the user select a new column
        if(column != columnToSort){
            ascending = true;
        }
        // save the column index
        columnToSort = column;

        Object[] sorted = data.toArray();
        Arrays.sort(sorted, new ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
    }

    public Object getValueAt(int row, int col) {
        switch (col) {
            case 0:
                return ((StatisticsRecord) sortedData[row]).getServiceName();
            case 1:
                return new Long(((StatisticsRecord) sortedData[row]).getAttemptedCount());
            case 2:
                return new Long(((StatisticsRecord) sortedData[row]).getAuthorizedCount());
            case 3:
                return new Long(((StatisticsRecord) sortedData[row]).getCompletedCount());
            case 4:
                return new Long(((StatisticsRecord) sortedData[row]).getCompletedCountLastMinute());
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    public class ColumnSorter implements Comparator {
        private boolean ascending;
        private int column;

        ColumnSorter(int column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;
        }

        public int compare(Object a, Object b) {

            Object elementA = null;
            Object elementB = null;

            switch (column) {
                case 0:
                    elementA = ((StatisticsRecord) a).getServiceName();
                    elementB = ((StatisticsRecord) b).getServiceName();
                    break;
                case 1:
                    elementA = new Long(((StatisticsRecord) a).getAttemptedCount());
                    elementB = new Long(((StatisticsRecord) b).getAttemptedCount());
                    break;
                case 2:
                    elementA = new Long(((StatisticsRecord) a).getAuthorizedCount());
                    elementB = new Long(((StatisticsRecord) b).getAuthorizedCount());
                    break;
                case 3:
                    elementA = new Long(((StatisticsRecord) a).getCompletedCount());
                    elementB = new Long(((StatisticsRecord) b).getCompletedCount());
                    break;
                case 4:
                    elementA = new Long(((StatisticsRecord) a).getCompletedCountLastMinute());
                    elementB = new Long(((StatisticsRecord) b).getCompletedCountLastMinute());
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

