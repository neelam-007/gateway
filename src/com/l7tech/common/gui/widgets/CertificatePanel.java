/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.gui.util.TableUtil;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.DSAParams;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.math.BigInteger;

/**
 * Panel that displays the fields of an X509Certificate.
 * User: mike
 * Date: Aug 18, 2003
 * Time: 4:05:46 PM
 */
public class CertificatePanel extends JPanel {
    private X509Certificate cert;
    private AbstractTableModel certificateTableModel;
    private JTable certificateTable;
    private JScrollPane tableScrollPane;

    /**
     * Create a new CertificatePanel
     */
    public CertificatePanel() {
        initComponents();
    }

    public CertificatePanel(X509Certificate cert)
            throws CertificateEncodingException, NoSuchAlgorithmException
    {
        this();
        this.cert = cert;
        loadCertificateInfo();
    }

    /**
     * This method is called from within the constructor to
     * initialize the dialog.
     */
    private void initComponents() {
        certificateTable = new JTable();

        tableScrollPane = new JScrollPane(certificateTable);
        add(tableScrollPane,
          new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
            GridBagConstraints.CENTER,
            GridBagConstraints.NONE,
            new Insets(15, 15, 0, 15), 0, 0));
    }

    /**
     * create the table model with certificate fields
     *
     * @return the <code>AbstractTableModel</code> for the
     * user's certificate
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.cert.CertificateEncodingException
     */
    private AbstractTableModel getCertificateTableModel()
      throws NoSuchAlgorithmException, CertificateEncodingException {
        if (certificateTableModel != null) {
            return certificateTableModel;
        }

        certificateTableModel = new AbstractTableModel() {
            String[] cols = {"Certificate Field", "Value"};
            ArrayList data = getCertProperties();

            public String getColumnName(int col) {
                return cols[col];
            }

            public int getColumnCount() {
                return cols.length;
            }

            public int getRowCount() {
                return data.size();
            }

            public Object getValueAt(int row, int col) {
                return ((String[])data.get(row))[col];
            }
        };

        Dimension size = new Dimension(500,
                              (certificateTableModel.getRowCount() + 1) * certificateTable.getRowHeight() + 1);
        tableScrollPane.setPreferredSize(size);
        tableScrollPane.setMinimumSize(size);

        return certificateTableModel;
    }

    /** Returns a properties instance filled out with info about the certificate. */
    private ArrayList getCertProperties()
      throws CertificateEncodingException, NoSuchAlgorithmException {
        ArrayList l = new ArrayList();
        if (cert == null) return l;

        // l.add(new String[]{"Revocation date", new Date().toString()});
        l.add(new String[]{"Creation date", nullNa(cert.getNotBefore())});
        l.add(new String[]{"Expiry date", nullNa(cert.getNotAfter())});
        l.add(new String[]{"Issued to", nullNa(cert.getSubjectDN())});
        l.add(new String[]{"Serial number", nullNa(cert.getSerialNumber())});
        l.add(new String[]{"Issuer", nullNa(cert.getIssuerDN())});

        l.add(new String[]{"SHA-1 fingerprint", getCertificateFingerprint(cert, "SHA1")});
        l.add(new String[]{"MD5 fingerprint", getCertificateFingerprint(cert, "MD5")});

        PublicKey publicKey = cert.getPublicKey();
        l.add(new String[]{"Key type", nullNa(publicKey.getAlgorithm())});

        if (publicKey != null && publicKey instanceof RSAPublicKey) {
            RSAPublicKey rsaKey = (RSAPublicKey) publicKey;
            System.out.println(rsaKey.toString());
            String modulus = rsaKey.getModulus().toString(16);
            l.add(new String[]{"RSA strength", (modulus.length() * 4) + " bits"});
            l.add(new String[]{"RSA modulus", modulus});
            l.add(new String[]{"RSA public exponent", rsaKey.getPublicExponent().toString(16)});
        } else if (publicKey != null && publicKey instanceof DSAPublicKey) {
            DSAPublicKey dsaKey = (DSAPublicKey) publicKey;
            DSAParams params = dsaKey.getParams();
            l.add(new String[]{"DSA prime (P)", params.getP().toString(16)});
            l.add(new String[]{"DSA subprime (P)", params.getQ().toString(16)});
            l.add(new String[]{"DSA base (P)", params.getG().toString(16)});
        }

        return l;
    }

    /**
     * load certificate info and updates the data and status of the
     * form elements
     */
    private void loadCertificateInfo() throws CertificateEncodingException, NoSuchAlgorithmException {
            AbstractTableModel
              certificateTableModel = getCertificateTableModel();
            certificateTable.setModel(certificateTableModel);
        TableUtil.adjustColumnWidth(certificateTable, 0);
    }


    /**
     * The method creates the fingerprint and returns it in a
     * String to the caller.
     *
     * @param cert      the certificate
     * @param algorithm the alghorithm (MD5 or SHA1)
     * @return the certificate fingerprint as a String
     * @exception java.security.NoSuchAlgorithmException
     *                      if the algorithm is not available.
     * @exception java.security.cert.CertificateEncodingException
     *                      thrown whenever an error occurs while attempting to
     *                      encode a certificate.
     */
    private String getCertificateFingerprint(X509Certificate cert, String algorithm)
      throws NoSuchAlgorithmException, CertificateEncodingException {
        if (cert == null) {
            throw new NullPointerException("cert");
        }
        StringBuffer buff = new StringBuffer();
        byte[] fingers = cert.getEncoded();

        MessageDigest md = MessageDigest.getInstance(algorithm);
        md.update(fingers);
        byte[] digest = md.digest();
        // the algorithm
        buff.append(algorithm + ":");

        for (int i = 0; i < digest.length; i++) {
            if (i != 0) buff.append(":");
            int b = digest[i] & 0xff;
            String hex = Integer.toHexString(b);
            if (hex.length() == 1) buff.append("0");
            buff.append(hex.toUpperCase());
        }
        return buff.toString();
    }


    /** Update the certificate information. */
    public void setCertificate(X509Certificate newCert) throws CertificateEncodingException, NoSuchAlgorithmException {
        cert = newCert;
        loadCertificateInfo();
    }

    /** Convert a null object into "N/A", otherwise toString */
    private String nullNa(Object o) {
        return o == null ? "N/A" : o.toString();
    }
}
