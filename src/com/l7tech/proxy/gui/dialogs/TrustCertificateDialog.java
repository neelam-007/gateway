/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.CertificatePanel;
import com.l7tech.common.gui.widgets.WrappingLabel;
import com.l7tech.proxy.gui.Gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Shows user a server certificate and asks if they wish to trust it.
 * @author mike
 * @version 1.0
 */
public class TrustCertificateDialog extends JDialog {
    private boolean trusted = false;

    public TrustCertificateDialog(X509Certificate cert, String title, String mess) {
        super(Gui.getInstance().getFrame(), title, true);
        Container c = this.getContentPane();
        c.setLayout(new GridBagLayout());
        c.add(new WrappingLabel(mess),
              new GridBagConstraints(0, 0, GridBagConstraints.REMAINDER, 1, 0.0, 0.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(5, 5, 5, 5), 0, 0));
        Component cpan = null;
        try {
            cpan = new CertificatePanel(cert);
        } catch (CertificateEncodingException e) {
            cpan = new JLabel("Can't display certificate -- could not decode it.");
        } catch (NoSuchAlgorithmException e) {
            cpan = new JLabel("Can't display certificate -- it uses unknown algorithm.");
        }
        c.add(cpan,
              new GridBagConstraints(0, 1, GridBagConstraints.REMAINDER, 1, 1000.0, 1000.0,
                                     GridBagConstraints.CENTER,
                                     GridBagConstraints.BOTH,
                                     new Insets(5, 5, 5, 5), 0, 0));
        c.add(Box.createGlue(),
              new GridBagConstraints(0, 2, 1, 1, 1.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.HORIZONTAL,
                                     new Insets(0, 0, 0, 0), 0, 0));

        c.add(getTrustButton(),
              new GridBagConstraints(1, 2, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 5, 5, 0), 0, 0));
        c.add(getRejectButton(),
              new GridBagConstraints(2, 2, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 5, 5, 0), 0, 0));
        c.add(getCancelButton(),
              new GridBagConstraints(3, 2, 1, 1, 0.0, 0.0,
                                     GridBagConstraints.EAST,
                                     GridBagConstraints.NONE,
                                     new Insets(5, 5, 5, 5), 0, 0));
        this.setSize(550, 360);
        Utilities.centerOnScreen(this);
    }

    private Action getCancelAction() {
        return new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                TrustCertificateDialog.this.hide();
                TrustCertificateDialog.this.dispose();
            }
        };
    }

    private JButton getCancelButton() {
        JButton cb = new JButton("Cancel");
        cb.addActionListener(getCancelAction());
        Utilities.runActionOnEscapeKey(getRootPane(), getCancelAction());
        return cb;
    }

    private JButton getRejectButton() {
        JButton cb = new JButton("Reject");
        cb.addActionListener(getCancelAction());
        return cb;
    }

    private JButton getTrustButton() {
        JButton cb = new JButton("Trust");
        cb.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                trusted = true;
                TrustCertificateDialog.this.hide();
                TrustCertificateDialog.this.dispose();
            }
        });
        return cb;
    }

    public boolean isTrusted() {
        return trusted;
    }
}
