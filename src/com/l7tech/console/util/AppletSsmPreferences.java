/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.util;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Preferences management for the Applet.  Currently just a stub -- makes no attempt to preserve prefs
 * between sessions.
 */
public class AppletSsmPreferences extends AbstractSsmPreferences {

    public void updateSystemProperties() {
        // Takes no action for applet
    }

    public void store() throws IOException {
        // Takes no action for applet
    }

    public String getHomePath() {
        throw new UnsupportedOperationException("No HomePath for applet");
    }

    public void importSsgCert(X509Certificate cert, String hostname) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
        // Takes no action for applet
    }

    public boolean isStatusBarBarVisible() {
        return false; // no status bar on applet
    }

}
