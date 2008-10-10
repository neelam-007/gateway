/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gui;

import org.junit.Ignore;

import javax.swing.*;
import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
@Ignore("Not a unit test")
public class TrustCertificateDialogTest {
    private static final Logger log = Logger.getLogger(TrustCertificateDialog.class.getName());

    public static void main(String[] args) throws Exception {
        X509Certificate cert = null;//TestDocuments.getDotNetServerCertificate();
        TrustCertificateDialog tcd = new TrustCertificateDialog(new JFrame(), cert, "Blah!", "The authenticity of this Gateway server certificate could not be verified using the current username and password.  Do you want to trust the Gateway data.l7tech.com using this server certificate?  If you don't know, click Cancel.");
        tcd.setVisible(true);
        log.info("Trusted = " + tcd.isTrusted());
        System.exit(0);
    }
}
