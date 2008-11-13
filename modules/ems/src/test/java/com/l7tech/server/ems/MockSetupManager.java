package com.l7tech.server.ems;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

/**
 * Mock SetupManager for tests
 */
public class MockSetupManager implements SetupManager {

    @Override
    public boolean isSetupPerformed() throws SetupException {
        return true;
    }

    @Override
    public void performInitialSetup(String licenseXml, String initialAdminUsername, String initialAdminPassword) throws SetupException {
        throw new SetupException("Mock setup not supported");
    }

    @Override
    public void deleteLicense() {
    }

    @Override
    public void configureListener(String ipaddress, int port) throws SetupException {
    }

    @Override
    public String saveSsl(PrivateKey key, X509Certificate[] certificateChain) throws SetupException {
        return "SSL";
    }

    @Override
    public String generateSsl(String hostname) throws SetupException {
        return "SSL";
    }

    @Override
    public void setSslAlias(String alias) throws SetupException {
    }
}
