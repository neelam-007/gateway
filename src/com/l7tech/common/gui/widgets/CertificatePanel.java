/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Panel that displays the fields of an X509Certificate.
 * User: mike
 * Date: Aug 18, 2003
 * Time: 4:05:46 PM
 */
public class CertificatePanel extends JPanel {
    private X509Certificate cert;
    private JPanel cp;
    private boolean certBorderEnabled = true;

    /**
     * Create a new CertificatePanel
     */
    public CertificatePanel() {
        setLayout(new GridBagLayout());
        setBackground(Color.white);
        cp = new JPanel(new GridBagLayout());
        cp.setBackground(Color.white);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.NORTHWEST;
        c.fill = GridBagConstraints.BOTH;
        add(cp, c);
        setCertBorderEnabled(certBorderEnabled);
    }

    public CertificatePanel(X509Certificate cert)
            throws CertificateEncodingException, NoSuchAlgorithmException
    {
        this();
        this.cert = cert;
        loadCertificateInfo();
    }

    public void setCertBorderEnabled(boolean certBorderEnabled) {
        this.certBorderEnabled = certBorderEnabled;
        if (certBorderEnabled) {
            Color color = new JLabel().getForeground();
            Border thin = BorderFactory.createLineBorder(color, 2);
            Border thick = BorderFactory.createLineBorder(color, 6);
            Border thickSpace = BorderFactory.createCompoundBorder(thick, BorderFactory.createEmptyBorder(4, 4, 4, 4));
            Border thinSpace = BorderFactory.createCompoundBorder(thin, BorderFactory.createEmptyBorder(4, 4, 4, 4));
            cp.setBorder(BorderFactory.createCompoundBorder(thickSpace, thinSpace));
        } else
            cp.setBorder(BorderFactory.createEmptyBorder());
    }

    public boolean getCertBorderEnabled() {
        return certBorderEnabled;
    }

    private void loadCertificateInfo() throws CertificateEncodingException, NoSuchAlgorithmException {
        ArrayList props = getCertProperties();
        int y = 0;
        cp.removeAll();
        for (Iterator i = props.iterator(); i.hasNext();) {
            String[] pair = (String[]) i.next();
            JLabel key = new JLabel(pair[0] + ": ");
            JLabel value = new JLabel(pair[1]);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = y++;
            c.anchor = GridBagConstraints.WEST;
            c.fill = GridBagConstraints.NONE;
            cp.add(key, c);
            c.gridx = 1;
            cp.add(value, c);
        }
        cp.add(Box.createGlue(), new GridBagConstraints(0, y++, 1, 1, 1.0, 1.0,
                                                        GridBagConstraints.NORTHWEST,
                                                        GridBagConstraints.BOTH,
                                                        new Insets(0, 0, 0, 0), 0, 0));
        invalidate();
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
            String modulus = rsaKey.getModulus().toString(16);
            l.add(new String[]{"RSA strength", (modulus.length() * 4) + " bits"});
            //l.add(new String[]{"RSA modulus", modulus});
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
