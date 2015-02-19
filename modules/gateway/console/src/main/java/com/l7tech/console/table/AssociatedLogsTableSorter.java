package com.l7tech.console.table;

import com.l7tech.gateway.common.audit.AssociatedLog;

import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;
import java.text.SimpleDateFormat;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class AssociatedLogsTableSorter  extends FilteredDefaultTableModel {
    public static final int ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX = 0;
    public static final int ASSOCIATED_LOG_SECURITY_COLUMN_INDEX = 1;
    public static final int ASSOCIATED_LOG_DETAIL_COLUMN_INDEX = 2;
    public static final int ASSOCIATED_LOG_CODE_COLUMN_INDEX = 3;
    public static final int ASSOCIATED_LOG_MSG_COLUMN_INDEX = 4;


    static Logger logger = Logger.getLogger(AssociatedLogsTableSorter.class.getName());
    private boolean ascending = false;
    private int columnToSort = 0;
    private List<AssociatedLog> rawdata = new ArrayList<AssociatedLog>();
    private Object[] sortedData = new Object[0];

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public AssociatedLogsTableSorter(DefaultTableModel model) {
        setRealModel(model);
    }

    /**
     * Return all data in the model
     * @return  The data in the model
     */
    public List getAllData() {
        return rawdata;
    }

    public void clear() {
        rawdata = new ArrayList<AssociatedLog>();
        sortData(columnToSort, false);
    }

    /**
     * Set the data object.
     *
     * @param data  The list of the node status of every gateways in the cluster (unsorted).
     */
    public void setData(List<AssociatedLog> data) {
        this.rawdata = data;
        sortData(columnToSort, false);
    }

    /**
     * Add a row to the table model
     *
     * @param rowData The new row to be stored.
     */
    public void addRow(AssociatedLog rowData) {
        this.rawdata.add(rowData);
        sortData(columnToSort, false);
    }

    /**
     * Return the column index of the sorted column.
     *
     * @return int  The column index of the sorted column.
     */
    public int getSortedColumn(){
        return columnToSort;
    }

    public Object getData(int index) {
        if(index <= sortedData.length) {
            return sortedData[index];
        } else
            return null;
    }

    /**
     * The sorting order.
     *
     * @return boolean  true if the sorting is in ascending order, false otherwise.
     */
    public boolean isAscending(){
        return ascending;
    }

    /**
     * Perform the data sorting.
     *
     * @param column  The index of the table column to be sorted.
     * @param orderToggle  true if the sorting order is toggled, false otherwise.
     */
    public void sortData(int column, boolean orderToggle) {

        if (orderToggle){
            ascending = !ascending;
        }

        // always sort in ascending order if the user select a new column
        if(column != columnToSort){
            ascending = true;
        }
        // save the column index
        columnToSort = column;

        Object[] sorted = rawdata.toArray();
        Arrays.sort(sorted, new AssociatedLogsTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
        getRealModel().setRowCount(sortedData.length);
        fireTableDataChanged();
    }

    /**
     * Return the value of the table cell specified with its table coordinate.
     *
     * @param row  The row index.
     * @param col  The column index.
     * @return Object  The value at the specified table coordinate.
     */
    public Object getValueAt(int row, int col) {

        if(row > sortedData.length) throw new IllegalArgumentException("Invalid Row: " + row);

        AssociatedLog log = ((AssociatedLog) sortedData[row]);

        switch (col) {
            case ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX:
                final Calendar cal = Calendar.getInstance();

                cal.setTimeInMillis(log.getTimeStamp());
                final SimpleDateFormat sdf = new SimpleDateFormat( "yyyyMMdd HH:mm:ss.SSS" );
                return sdf.format(cal.getTime());

            case ASSOCIATED_LOG_SECURITY_COLUMN_INDEX:
                return log.getSeverity();

            case ASSOCIATED_LOG_DETAIL_COLUMN_INDEX:
                return log.getException();

            case ASSOCIATED_LOG_CODE_COLUMN_INDEX:
                return log.getMessageId();

            case ASSOCIATED_LOG_MSG_COLUMN_INDEX:
                return log.getMessage();

            default:
                throw new IllegalArgumentException("Bad Column: " + col);
        }
    }

    /**
     * A class for determining the order of two objects by comparing their values.
     */
    public class ColumnSorter implements Comparator<Object> {
        private boolean ascending;
        private int column;

        /**
         * Constructor
         *
         * @param column  The index of the table column on which the objects are sorted.
         * @param ascending  true if the sorting order is ascending, false otherwise.
         */
        ColumnSorter(int column, boolean ascending) {
            this.ascending = ascending;
            this.column = column;
        }

        /**
         * Compare the order of the two objects. A negative integer, zero, or a positive integer
         * as the the specified String is greater than, equal to, or less than this String,
         * ignoring case considerations.
         *
         * @param a  One of the two objects to be compared.
         * @param b  The other one of the two objects to be compared.
         * @return   -1 if a > b, 0 if a = b, and 1 if a < b.
         */
        public int compare(Object a, Object b) {

            Object elementA = new Object();
            Object elementB = new Object();

            switch (column) {
                case ASSOCIATED_LOG_TIMESTAMP_COLUMN_INDEX:
                    // Special note: the ordinal plus the time is used to determine the sequence of the logs as the timestamp's resolution is not adequate to resolve the order (ms).

                    if(new Long(((AssociatedLog) a).getTimeStamp()).equals(new Long(((AssociatedLog) b).getTimeStamp()))){
                        elementA = new Integer(((AssociatedLog) a).getOrdinal());
                        elementB = new Integer(((AssociatedLog) b).getOrdinal());
                    }else {
                        elementA = new Long(((AssociatedLog) a).getTimeStamp());
                        elementB = new Long(((AssociatedLog) b).getTimeStamp());
                    }
                    break;

                case ASSOCIATED_LOG_SECURITY_COLUMN_INDEX:
                        elementA = ((AssociatedLog) a).getSeverity();
                        elementB = ((AssociatedLog) b).getSeverity();
                    break;

                case ASSOCIATED_LOG_DETAIL_COLUMN_INDEX:
                    elementA = ((AssociatedLog) a).getException();
                    elementB = ((AssociatedLog) b).getException();
                    break;

                case ASSOCIATED_LOG_CODE_COLUMN_INDEX:
                        elementA = ((AssociatedLog) a).getMessageId();
                        elementB = ((AssociatedLog) b).getMessageId();
                    break;

                case ASSOCIATED_LOG_MSG_COLUMN_INDEX:
                        elementA = ((AssociatedLog) a).getMessage();
                        elementB = ((AssociatedLog) b).getMessage();
                    break;

                default:
                    logger.warning("Bad Table Column: " + column);
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
                    if (elementA instanceof Integer && elementB instanceof Integer) {
                        return ((Integer) elementA).compareTo((Integer) elementB);
                    } else if (elementA instanceof Long && elementB instanceof Long) {
                        return ((Long) elementA).compareTo((Long) elementB);
                    } else if(elementA instanceof String && elementB instanceof String) {
                        return ((String)elementA).compareToIgnoreCase((String)elementB);
                    } else {
                        // add code here to support other types
                        return 0;
                    }
                } else {
                     if (elementA instanceof Integer && elementB instanceof Integer) {
                        return ((Integer) elementB).compareTo((Integer) elementA);
                    } else if (elementA instanceof Long && elementB instanceof Long) {
                        return ((Long) elementB).compareTo((Long) elementA);
                    } else if(elementA instanceof String && elementB instanceof String) {
                        return ((String)elementB).compareToIgnoreCase((String)elementA);
                    } else {
                        // add code here to support other types
                        return 0;
                    }
                }
            }
        }
    }
}



