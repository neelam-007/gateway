/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.gui.widgets;

import com.l7tech.common.util.CertUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
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
        ArrayList props = CertUtils.getCertProperties(cert);
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


    /** Update the certificate information. */
    public void setCertificate(X509Certificate newCert) throws CertificateEncodingException, NoSuchAlgorithmException {
        cert = newCert;
        loadCertificateInfo();
    }

}
