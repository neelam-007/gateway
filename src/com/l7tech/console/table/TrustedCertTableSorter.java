package com.l7tech.console.table;

import com.l7tech.console.panels.CertManagerWindow;
import com.l7tech.common.security.TrustedCert;

import javax.swing.table.DefaultTableModel;
import java.util.logging.Logger;
import java.util.Vector;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Calendar;
import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;

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
    public static final int CERT_TABLE_CERT_USAGE_COLUMN_INDEX = 3;

    static Logger logger = Logger.getLogger(TrustedCertTableSorter.class.getName());
    private boolean ascending = true;
    private int columnToSort = 1;
    private Vector rawdata = new Vector();
    private Object[] sortedData = null;

    /**
     * Constructor taking <CODE>DefaultTableModel</CODE> as the input parameter.
     *
     * @param model  A table model.
     */
    public TrustedCertTableSorter(DefaultTableModel model) {
        setRealModel(model);
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
     * Update the data of a row.
     * @param row  The row index.
     * @param data The new data to be stored.
     */
    public void updateData(int row, Object data) {

        TrustedCert tc = null;

        if (data instanceof TrustedCert) {
            TrustedCert newtc = (TrustedCert) data;

            for (int i = 0; i < rawdata.size(); i++) {
                tc = (TrustedCert) rawdata.elementAt(i);
                if (tc != null && tc.getOid() == newtc.getOid()) {
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

        X509Certificate cert = null;
        try {
            cert = ((TrustedCert) sortedData[row]).getCertificate();
        } catch (CertificateException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        switch (col) {
            case CERT_TABLE_CERT_NAME_COLUMN_INDEX:
                return ((TrustedCert) sortedData[row]).getName();

            case CERT_TABLE_ISSUER_NAME_COLUMN_INDEX:
                return cert.getIssuerDN().getName();

            case CERT_TABLE_CERT_EXPIRATION_DATE_COLUMN_INDEX:
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                Calendar cal = Calendar.getInstance();
                cal.setTime(cert.getNotAfter());
                return sdf.format(cal.getTime());

            case CERT_TABLE_CERT_USAGE_COLUMN_INDEX:
                return ((TrustedCert) sortedData[row]).getUsageDescription();

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
                case CERT_TABLE_CERT_NAME_COLUMN_INDEX:
                    elementA = ((TrustedCert) a).getName();
                    elementB = ((TrustedCert) b).getName();
                    break;

                case CERT_TABLE_ISSUER_NAME_COLUMN_INDEX:
                    try {
                        elementA = ((TrustedCert) a).getCertificate().getIssuerDN().getName();
                        elementB = ((TrustedCert) b).getCertificate().getIssuerDN().getName();
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (CertificateException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    break;

                case CERT_TABLE_CERT_EXPIRATION_DATE_COLUMN_INDEX:
                    try {
                        elementA = new Long(((TrustedCert) a).getCertificate().getNotAfter().getTime());
                        elementB = new Long(((TrustedCert) b).getCertificate().getNotAfter().getTime());
                    } catch (IOException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    } catch (CertificateException e) {
                        e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                    }
                    break;

                case CERT_TABLE_CERT_USAGE_COLUMN_INDEX:
                    elementA = ((TrustedCert) a).getUsageDescription();
                    elementB = ((TrustedCert) b).getUsageDescription();
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
                    if (elementA instanceof Long) {
                        return ((Long) elementA).longValue() > ((Long) elementB).longValue()?1:0;
                    } else if(elementA instanceof String) {
                        return ((String)elementA).compareToIgnoreCase((String)elementB);
                    } else {
                        // add code here to support other types
                        return 0;
                    }
                } else {
                     if (elementA instanceof Long) {
                        return ((Long) elementB).longValue() > ((Long) elementA).longValue()?1:0;
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


