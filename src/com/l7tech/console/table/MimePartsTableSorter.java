package com.l7tech.console.table;

import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.wsdl.MimePartInfo;

import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class MimePartsTableSorter  extends FilteredDefaultTableModel {

    public static final int MIME_PART_TABLE_PARAM_NAME_COLUMN_INDEX = 0;
    public static final int MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX = 1;
    public static final int MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX = 2;

    static Logger logger = Logger.getLogger(MimePartsTableSorter.class.getName());
    private boolean ascending = true;
    private int columnToSort = 1;
    private Vector rawdata = new Vector();
    private Object[] sortedData = new Object[0];

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public MimePartsTableSorter(DefaultTableModel model) {
        setRealModel(model);
    }

    /**
     * Return all data in the model
     * @return  The data in the model
     */
    public Vector getAllData() {
        return rawdata;
    }

    /**
     * Set the data object.
     *
     * @param data  The list of the node status of every gateways in the cluster (unsorted).
     */
    public void setData(Vector data) {
        this.rawdata = data;
        sortData(columnToSort, false);
    }

    /**
     * Add a row to the table model
     *
     * @param rowData The new row to be stored.
     */
    public void addRow(Object rowData) {
        this.rawdata.add(rowData);
        sortData(columnToSort, false);
    }

    public boolean isCellEditable(int row, int col) {
         if(col == MimePartsTableSorter.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX)
             return true;
         else
             return false;
     }

   /**
     * Update the data of a row.
     * @param row  The row index.
     * @param col The col index.
     * @param aValue The new data to be stored.
     */
   public void setValueAt(Object aValue, int row, int col) {

       // only max length column can be modified
       if(col == MimePartsTableSorter.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX) {

           Object o = (Object) getValueAt(row, MIME_PART_TABLE_PARAM_NAME_COLUMN_INDEX);

           if (o instanceof String) {
               String mimePartName = (String) o;

               for (int i = 0; i < rawdata.size(); i++) {
                   MimePartInfo mimePart = (MimePartInfo) rawdata.elementAt(i);
                   if (mimePart != null && mimePart.getName() == mimePartName) {
                       // replace the old one
                       if(aValue instanceof Integer) {
                           mimePart.setMaxLength(((Integer)aValue).intValue());
                       }
                       break;
                   }
               }
               // sort the data
               sortData(columnToSort, false);
           }
       }
   }

    /**
     * Return the column index of the sorted column.
     *
     * @return int  The column index of the sorted column.
     */
    public int getSortedColumn(){
        return columnToSort;
    }

    public Class getColumnClass(int columnIndex) {
        if(columnIndex == MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX) {
            return Integer.class;
        } else {
            return String.class;
        }
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

        if(orderToggle){
            ascending = ascending ? false : true;
        }

        // always sort in ascending order if the user select a new column
        if(column != columnToSort){
            ascending = true;
        }
        // save the column index
        columnToSort = column;

        Object[] sorted = rawdata.toArray();
        Arrays.sort(sorted, new MimePartsTableSorter.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
        getRealModel().setRowCount(sortedData.length);
        fireTableDataChanged();
    }

    /**
     * Return the value of the table cell specified with its tbale coordinate.
     *
     * @param row  The row index.
     * @param col  The column index.
     * @return Object  The value at the specified table coordinate.
     */
    public Object getValueAt(int row, int col) {

        if(row < sortedData.length) {
            MimePartInfo multipartInfo = (MimePartInfo) sortedData[row];

            switch (col) {
                case MIME_PART_TABLE_PARAM_NAME_COLUMN_INDEX:
                    return multipartInfo.getName();

                case MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX:
                    return multipartInfo.getContentType();

                case MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX:
                    return new Integer(multipartInfo.getMaxLength());

                default:
                    throw new IllegalArgumentException("Accessing a invalid column:" + col);
            }
        } else {
            throw new IllegalArgumentException("Acessging a invalid row:" + row);
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
                case MIME_PART_TABLE_PARAM_NAME_COLUMN_INDEX:
                    elementA = ((MimePartInfo) a).getName();
                    elementB = ((MimePartInfo) b).getName();
                    break;

                case MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX:
                    elementA = ((MimePartInfo) a).getContentType();
                    elementB = ((MimePartInfo) b).getContentType();

                    break;

                case MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX:
                        elementA = new Integer(((MimePartInfo) a).getMaxLength());
                        elementB = new Integer(((MimePartInfo) b).getMaxLength());

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
                    if (elementA instanceof Integer) {
                        return ((Integer) elementA).intValue() > ((Integer) elementB).intValue()?1:0;
                    } else if(elementA instanceof String) {
                        return ((String)elementA).compareToIgnoreCase((String)elementB);
                    } else {
                        // add code here to support other types
                        return 0;
                    }
                } else {
                     if (elementA instanceof Integer) {
                        return ((Integer) elementB).intValue() > ((Long) elementA).intValue()?1:0;
                    } else if(elementA instanceof String) {
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