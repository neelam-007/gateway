package com.l7tech.console.table;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.io.IOException;
import javax.swing.table.DefaultTableModel;

import com.l7tech.wsdl.MimePartInfo;
import com.l7tech.common.mime.ContentTypeHeader;

/**
 * Model for extra mime parts table.
 *
 * @author $Author$
 * @version $Revision$
 */
public class ExtraMimePartsTableModel extends FilteredDefaultTableModel {

    //- PUBLIC

    public static final int MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX = 0;
    public static final int MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX = 1;

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public ExtraMimePartsTableModel(DefaultTableModel model) {
        setRealModel(model);
    }

    /**
     * Return all data in the model
     * @return  The data in the model
     */
    public List getData() {
        return Collections.unmodifiableList(rawdata);
    }

    /**
     * Set the data object.
     *
     * @param data  The list of the node status of every gateways in the cluster (unsorted).
     */
    public void setData(Collection data) {
        this.rawdata = new ArrayList(data);
        while (this.rawdata.size() < 1) {
            MimePartInfo mpi = new MimePartInfo();
            mpi.setMaxLength(-1);
            this.rawdata.add(mpi);
        }
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

    /**
     * Add an empty row to the table model.
     */
    public void addEmptyRow() {
        MimePartInfo mpi = new MimePartInfo();
        mpi.setMaxLength(-1);
        this.rawdata.add(mpi);
        List newsorteddata = new ArrayList(Arrays.asList(sortedData));
        newsorteddata.add(mpi);
        this.sortedData = newsorteddata.toArray();
        getRealModel().setRowCount(sortedData.length);
        fireTableDataChanged();
    }

    public boolean isCellEditable(int row, int col) {
         return true;
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
                case ExtraMimePartsTableModel.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX:
                    String data = multipartInfo.retrieveAllContentTypes();
                    if (data.length()==0) return null;
                    return new ContentTypeHeaderModel(data);

                case ExtraMimePartsTableModel.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX:
                    return new NullableIntegerModel(multipartInfo.getMaxLength());

                default:
                    throw new IllegalArgumentException("Accessing a invalid column:" + col);
            }
        } else {
            throw new IllegalArgumentException("Acessging a invalid row:" + row);
        }
    }

    /**
     * Update the data of a row.
     *
     * @param row The row index.
     * @param col The col index.
     * @param aValue The new data to be stored.
     */
    public void setValueAt(Object aValue, int row, int col) {
        if(row < sortedData.length) {
            MimePartInfo multipartInfo = (MimePartInfo) sortedData[row];

            switch (col) {
                case ExtraMimePartsTableModel.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX:
                    if (aValue == null) {
                        multipartInfo.setContentTypes(new String[0]);
                    }
                    else {
                        multipartInfo.setContentTypes(new String[]{((ContentTypeHeaderModel)aValue).toString()});
                    }
                    break;
                case ExtraMimePartsTableModel.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX:
                    if (aValue == null) {
                        multipartInfo.setMaxLength(0);
                    }
                    else {
                        multipartInfo.setMaxLength(((Number)aValue).intValue());
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Accessing a invalid column:" + col);
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
        Class colClass = null;

        switch (columnIndex) {
            case ExtraMimePartsTableModel.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX:
                colClass = NullableIntegerModel.class;
                break;
            case ExtraMimePartsTableModel.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX:
                colClass = ContentTypeHeaderModel.class;
                break;
        }

        return colClass;
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
        Arrays.sort(sorted, new ExtraMimePartsTableModel.ColumnSorter(columnToSort, ascending));
        sortedData = sorted;
        getRealModel().setRowCount(sortedData.length);
        fireTableDataChanged();
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
                case ExtraMimePartsTableModel.MIME_PART_TABLE_CONTENT_TYPE_COLUMN_INDEX:
                    elementA = ((MimePartInfo) a).retrieveAllContentTypes();
                    elementB = ((MimePartInfo) b).retrieveAllContentTypes();
                    break;

                case ExtraMimePartsTableModel.MIME_PART_TABLE_MAX_LENGTH_COLUMN_INDEX:
                    elementA = new Integer(((MimePartInfo) a).getMaxLength());
                    elementB = new Integer(((MimePartInfo) b).getMaxLength());
                    break;

                default:
                    ExtraMimePartsTableModel.logger.warning("Bad Table Column: " + column);
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
                        return ((Integer) elementB).intValue() > ((Integer) elementA).intValue()?1:0;
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

    /**
     * Data class for the content type cells.
     */
    public static class ContentTypeHeaderModel {
        private ContentTypeHeader header;

        public ContentTypeHeaderModel(String fullHeader) {
            try {
                header = ContentTypeHeader.parseValue(fullHeader);
            }
            catch (IOException ioe) {
                throw new IllegalArgumentException("Invalid content type '"+fullHeader+"'.");
            }
        }

        public String toString() {
            return header.getFullValue();
        }
    }

    /**
     * Data class for the 0-x numeric cells.
     */
    public static class NullableIntegerModel extends Number {
        private int value;

        public NullableIntegerModel(String value) throws NumberFormatException {
            int newValue = Integer.parseInt(value);
            if (newValue < 0) {
                throw new IllegalArgumentException("value must be 0 or more");
            }
            this.value = newValue;
        }

        public NullableIntegerModel(int value) {
            this.value = value;
        }

        public double doubleValue() {
            return value;
        }

        public float floatValue() {
            return value;
        }

        public int intValue() {
            return value;
        }

        public long longValue() {
            return value;
        }

        public String toString() {
            String text = null;

            if (value < 0) {
                text = "";
            }
            else {
                text = Integer.toString(value);
            }

            return text;
        }
    }

    //- PRIVATE

    private static Logger logger = Logger.getLogger(ExtraMimePartsTableModel.class.getName());

    private boolean ascending = true;
    private int columnToSort = 1;
    private List rawdata = new ArrayList();
    private Object[] sortedData = new Object[0];

}
