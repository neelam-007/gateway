/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.client.gui.dialogs;

import com.l7tech.client.gui.dialogs.TrustCertificateDialog;
import com.l7tech.client.gui.Gui;

import java.security.cert.X509Certificate;
import java.util.logging.Logger;

/**
 *
 * @author mike
 * @version 1.0
 */
public class TrustCertificateDialogTest {
    private static final Logger log = Logger.getLogger(TrustCertificateDialog.class.getName());

    public static void main(String[] args) throws Exception {
        Gui.setInstance( Gui.createGui(new Gui.GuiParams(null/*new SsgManagerStub()*/, 0)));
        X509Certificate cert = null;//TestDocuments.getDotNetServerCertificate();
        TrustCertificateDialog tcd = new TrustCertificateDialog(cert, "Blah!", "The authenticity of this Gateway server certificate could not be verified using the current username and password.  Do you want to trust the Gateway data.l7tech.com using this server certificate?  If you don't know, click Cancel.");
        tcd.setVisible(true);
        log.info("Trusted = " + tcd.isTrusted());
        System.exit(0);
    }
}
