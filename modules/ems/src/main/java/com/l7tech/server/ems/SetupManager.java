package com.l7tech.server.ems;

import org.springframework.transaction.annotation.Transactional;

/**
 * Encapsulates behavior for initial setup of a new EMS instance.
 */
public interface SetupManager {

    /**
     * Check if this EMS instance has already had initial setup performed.
     * This returns true if any of the following are true:
     * <ul>
     * <li>A valid license is currently installed.</li>
     * <li>At least one internal user currently exists.</li>
     * </ul>
     * @return true if initial setup has been performed per the above.
     * @throws com.l7tech.server.ems.SetupException if there is a problem checking whether any internal users exist
     */
    @Transactional(propagation= org.springframework.transaction.annotation.Propagation.SUPPORTS, readOnly=true)
    boolean isSetupPerformed() throws SetupException;

    /**
     * Perform initial setup of this EMS instance.
     * This sets a license and creates the initial administrator user in a single transaction.
     *
     * @param licenseXml  XML license file to install.  Required.
     * @param initialAdminUsername  username for initial administrator user.  Required.
     * @param initialAdminPassword  password for iniital administrator user.  Required.
     * @throws com.l7tech.server.ems.SetupException if this EMS instance has already been set up.
     */
    @Transactional(propagation= org.springframework.transaction.annotation.Propagation.REQUIRED, rollbackFor=Throwable.class)
    void performInitialSetup(String licenseXml, String initialAdminUsername, String initialAdminPassword) throws SetupException;
}
