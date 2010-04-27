package com.l7tech.console.util;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 *
 */
public class MockSsmPreferences extends AbstractSsmPreferences {
    @Override
    public void updateSystemProperties() {
    }

    @Override
    public void store() throws IOException {
    }

    @Override
    public String getHomePath() {
        return null;
    }

    @Override
    public void importSsgCert( final X509Certificate cert, final String hostname ) throws KeyStoreException, NoSuchAlgorithmException, IOException, CertificateException {
    }
}
