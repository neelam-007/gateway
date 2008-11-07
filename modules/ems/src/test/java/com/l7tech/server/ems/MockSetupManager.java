package com.l7tech.server.ems;

/**
 * Mock SetupManager for tests
 */
public class MockSetupManager implements SetupManager {

    public boolean isSetupPerformed() throws SetupException {
        return true;
    }

    public void performInitialSetup(String licenseXml, String initialAdminUsername, String initialAdminPassword) throws SetupException {
        throw new SetupException("Mock setup not supported");
    }

    public void deleteLicense() {
    }
}
