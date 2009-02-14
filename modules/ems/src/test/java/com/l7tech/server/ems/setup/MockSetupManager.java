package com.l7tech.server.ems.setup;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * Mock SetupManager for tests
 */
public class MockSetupManager implements SetupManager {
    private static final String uuid = UUID.nameUUIDFromBytes(MockSetupManager.class.getName().getBytes()).toString();

    @Override
    public String getEsmId() {
        return uuid;
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
    public String generateSsl(String hostname, RsaKeySize rsaKeySize) throws SetupException {
        return "SSL";
    }

    @Override
    public void setSslAlias(String alias) throws SetupException {
    }

    @Override
    public void setSessionTimeout(int sessionTimeout) throws SetupException {
    }
}
