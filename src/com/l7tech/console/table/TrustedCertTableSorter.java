package com.l7tech.console.table;

import com.l7tech.console.panels.CertManagerWindow;
import com.l7tech.common.security.TrustedCert;

import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * 
 * @author fpang
 *         $Id$
 */
public class TrustedCertTableSorter extends FilteredDefaultTableModel {
    static Logger logger = Logger.getLogger(TrustedCertTableSorter.class.getName());
    private boolean ascending = true;
    private int columnToSort = 1;
    private Vector data = new Vector();
    private Object[] sortedData = null;

    /**
     * Constructor
     */
    public TrustedCertTableSorter() {
    }

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public TrustedCertTableSorter(DefaultTableModel model) {
        setModel(model);
    }

   /**
     * Set the table model.
     *
     * @param model  The table model to be set.
     */
    public void setModel(DefaultTableModel model) {
        super.setRealModel(model);
    }

    /**
     * Set the data object.
     *
     * @param data  The list of the node status of every gateways in the cluster (unsorted).
     */
    public void setData(Vector data) {
        this.data = data;
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
        Arrays.sort(sorted, new TrustedCertTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
    }

    /**
     * Return the value of the table cell specified with its tbale coordinate.
     *
     * @param row  The row index.
     * @param col  The column index.
     * @return Object  The value at the specified table coordinate.
     */
    public Object getValueAt(int row, int col) {
        switch (col) {
            case CertManagerWindow.CERT_TABLE_CERT_NAME_COLUMN_INDEX:
                return ((TrustedCert) sortedData[row]).getName();
            case CertManagerWindow.CERT_TABLE_CERT_SUBJECT_COLUMN_INDEX:
                return ((TrustedCert) sortedData[row]).getSubjectDn();
            case CertManagerWindow.CERT_TABLE_CERT_USAGE_COLUMN_INDEX:
                //todo:
                //return ((TrustedCert) sortedData[row]).getUsage();
                return "";
            default:
                throw new IllegalArgumentException("Bad Column");
        }
    }

    /**
     * A class for determining the order of two objects by comparing their values.
     */
    public class ColumnSorter implements Comparator {
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
                case CertManagerWindow.CERT_TABLE_CERT_NAME_COLUMN_INDEX:
                    elementA = "";
                    elementB = "";
                    break;
                case CertManagerWindow.CERT_TABLE_CERT_SUBJECT_COLUMN_INDEX:
                    elementA = "";
                    elementB = "";
                    break;
                case CertManagerWindow.CERT_TABLE_CERT_USAGE_COLUMN_INDEX:
                    elementA = "";
                    elementB = "";
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
                    if(elementA instanceof String) {
                        return ((String)elementA).compareToIgnoreCase((String)elementB);
                    } else {
                        // add code here to support other types
                        return 0;
                    }
                } else {
                    if(elementA instanceof String) {
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


