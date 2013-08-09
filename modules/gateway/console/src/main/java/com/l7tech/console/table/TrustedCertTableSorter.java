package com.l7tech.console.table;

import com.l7tech.common.io.CertUtils;
import com.l7tech.security.cert.TrustedCert;

import javax.swing.table.DefaultTableModel;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * 
 * <p> @author fpang </p>
 * $Id$
 */
public class TrustedCertTableSorter extends FilteredDefaultTableModel {

    public static final int CERT_TABLE_CERT_NAME_COLUMN_INDEX = 0;
    public static final int CERT_TABLE_ISSUER_NAME_COLUMN_INDEX = 1;
    public static final int CERT_TABLE_CERT_EXPIRATION_DATE_COLUMN_INDEX = 2;
    public static final int CERT_TABLE_THUMBPRINT_COLUMN_INDEX = 3;
    public static final int CERT_TABLE_CERT_USAGE_COLUMN_INDEX = 4;
    public static final int CERT_TABLE_SUBJECT_DN_COLUMN_INDEX = 5;

    static Logger logger = Logger.getLogger(TrustedCertTableSorter.class.getName());
    private boolean ascending = true;
    private int columnToSort = 1;
    private List<TrustedCert> rawdata = new ArrayList<TrustedCert>();
    private TrustedCert[] sortedData = new TrustedCert[0];

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public TrustedCertTableSorter(DefaultTableModel model) {
        setRealModel(model);
    }

    /**
     * Return all data in the model
     * @return  The data in the model
     */
    public List<TrustedCert> getAllData() {
        return rawdata;
    }

    /**
     * Set the data object.
     *
     * @param data  The list of the node status of every gateways in the cluster (unsorted).
     */
    public void setData(List<TrustedCert> data) {
        this.rawdata = data;
        sortData(columnToSort, false);
    }

    /**
     * Add a row to the table model
     *
     * @param rowData The new row to be stored.
     */
    public void addRow(TrustedCert rowData) {
        this.rawdata.add(rowData);
        sortData(columnToSort, false);
    }

    /**
     * Delete a row from the table model
     * @param rowIndex  The index of the row to be deleted.
     */
    public void deleteRow(int rowIndex) {

        if(rowIndex < sortedData.length) {
            TrustedCert tc = sortedData[rowIndex];

            for (int i = 0; i < rawdata.size(); i++) {
                TrustedCert cert = rawdata.get(i);
                if(cert.equals(tc)) {
                    rawdata.remove(cert);
                    sortData(columnToSort, false);
                    break;
                }
            }
        }
    }

    /**
     * Update the data of a row.
     * @param row  The row index.
     * @param data The new data to be stored.
     */
    public void updateData(int row, Object data) {

        TrustedCert tc;

        if (data instanceof TrustedCert) {
            TrustedCert newtc = (TrustedCert) data;

            for (int i = 0; i < rawdata.size(); i++) {
                tc = rawdata.get(i);
                if (tc != null && tc.getGoid() == newtc.getGoid()) {
                    // replace the old one
                    rawdata.set(i, newtc);
                }
            }
            // sort the data
            sortData(columnToSort, false);
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

    public Object getData(int index) {
        if(index <= sortedData.length) {
            return sortedData[index];
        } else
            return null;
    }

    /**
     * Check if the object already exists in the table model
     * @param tc  The trusted cert
     * @return  true if the trusted cert already exists in the table model, false otherwise.
     */
    public boolean contains(TrustedCert tc) {
        String thumb1 = tc.getThumbprintSha1();
        for (TrustedCert cert : sortedData) {
            String thumb2 = cert.getThumbprintSha1();
            if (thumb1.equals(thumb2))
                return true;
        }

        return false;
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
            ascending = !ascending;
        }

        // always sort in ascending order if the user select a new column
        if(column != columnToSort){
            ascending = true;
        }
        // save the column index
        columnToSort = column;

        TrustedCert[] sorted = rawdata.toArray(new TrustedCert[0]);
        Arrays.sort(sorted, new TrustedCertTableSorter.ColumnSorter(columnToSort, ascending));
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
        try {
            switch (col) {
                case CERT_TABLE_CERT_NAME_COLUMN_INDEX:
                    return sortedData[row].getName();

                case CERT_TABLE_ISSUER_NAME_COLUMN_INDEX:
                    return CertUtils.extractFirstIssuerNameFromCertificate(sortedData[row].getCertificate());

                case CERT_TABLE_CERT_EXPIRATION_DATE_COLUMN_INDEX:
                    SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(sortedData[row].getCertificate().getNotAfter());
                    return sdf.format(cal.getTime());

                case CERT_TABLE_THUMBPRINT_COLUMN_INDEX:
                    return CertUtils.getCertificateFingerprint(sortedData[row].getCertificate(), "SHA1", "rawhex");

                case CERT_TABLE_CERT_USAGE_COLUMN_INDEX:
                    return sortedData[row].getUsageDescription();

                case CERT_TABLE_SUBJECT_DN_COLUMN_INDEX:
                    return sortedData[row].getSubjectDn();

                default:
                    throw new IllegalArgumentException("Bad Column");
            }
        } catch (CertificateException e) {
            logger.warning("Invalid certificate: " + e.getMessage());
            return null;
        } catch (NoSuchAlgorithmException e) {
            logger.warning("Invalid fingerprint algorithm: " + e.getMessage()); // can't happen
            return null;
        }
    }

    /**
     * A class for determining the order of two objects by comparing their values.
     */
    public class ColumnSorter implements Comparator<TrustedCert> {
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
        public int compare(TrustedCert a, TrustedCert b) {

            Object elementA = new Object();
            Object elementB = new Object();

            switch (column) {
                case CERT_TABLE_CERT_NAME_COLUMN_INDEX:
                    elementA = a.getName();
                    elementB = b.getName();
                    break;

                case CERT_TABLE_ISSUER_NAME_COLUMN_INDEX:
                    elementA = a.getCertificate().getIssuerDN().getName();
                    elementB = b.getCertificate().getIssuerDN().getName();
                    break;

                case CERT_TABLE_CERT_EXPIRATION_DATE_COLUMN_INDEX:
                    elementA = a.getCertificate().getNotAfter().getTime();
                    elementB = b.getCertificate().getNotAfter().getTime();
                    break;

                case CERT_TABLE_THUMBPRINT_COLUMN_INDEX:
                    elementA = a.getThumbprintSha1();
                    elementB = b.getThumbprintSha1();
                    break;

                case CERT_TABLE_CERT_USAGE_COLUMN_INDEX:
                    elementA = a.getUsageDescription();
                    elementB = b.getUsageDescription();
                    break;

                case CERT_TABLE_SUBJECT_DN_COLUMN_INDEX:
                    elementA = a.getSubjectDn();
                    elementB = b.getSubjectDn();
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
                    if (elementA instanceof Long && elementB instanceof Long) {
                        return ((Long) elementA).longValue() > ((Long) elementB).longValue()?1:0;
                    } else if(elementA instanceof String && elementB instanceof String) {
                        return ((String)elementA).compareToIgnoreCase((String)elementB);
                    } else {
                        // add code here to support other types
                        return 0;
                    }
                } else {
                     if (elementA instanceof Long && elementB instanceof Long) {
                        return ((Long) elementB).longValue() > ((Long) elementA).longValue()?1:0;
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


