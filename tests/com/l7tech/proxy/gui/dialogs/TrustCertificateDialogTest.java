/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.proxy.gui.dialogs;

import com.l7tech.proxy.gui.Gui;
import com.l7tech.proxy.datamodel.SsgManagerStub;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.Ssg;
import java.util.logging.Logger;

import java.security.cert.X509Certificate;

/**
 *
 * @author mike
 * @version 1.0
 */
public class TrustCertificateDialogTest {
    private static final Logger log = Logger.getLogger(TrustCertificateDialog.class.getName());

    public static void main(String[] args) throws Exception {
        Gui.setInstance(Gui.createGui(null, new SsgManagerStub()));
        X509Certificate cert = SsgKeyStoreManager.getServerCert(new Ssg(1));
        TrustCertificateDialog tcd = new TrustCertificateDialog(cert, "Blah!", "foofoo");
        tcd.show();
        log.info("Trusted = " + tcd.isTrusted());
        System.exit(0);
    }
}
