package com.l7tech.server;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.objectmodel.UpdateException;

/**
 * 
 */
public interface UpdatableLicenseManager extends LicenseManager {

    /**
     * Get the currently-installed valid license, or null if no valid license is installed or present in the database.
     * If a license is present in the database last time we checked but was not installed because it was invalid,
     * this method throws a LicenseException explaining the problem.
     *
     * @return the currently installed valid license, or null if no license is installed or present in the database.
     * @throws com.l7tech.gateway.common.InvalidLicenseException if a license is present in the database but was not installed because it was invalid.
     */
    License getCurrentLicense() throws InvalidLicenseException;

    /**
     * Validate and install a new license, both in memory and to the database.  If the new license appears to be
     * valid it will be sent to the database and also immediately made live in this license manager.
     * <p>
     * If a new license is successfully installed, it replaces any previous license.  The previous license XML is
     * not saved anywhere and will be lost unless the administrator saved it up somewhere.
     * <p>
     * If the new license is not valid, any current license is left in place.
     *
     * @param newLicenseXml   the new license to install.  Must be a valid license file signed by a trusted issuer.
     * @throws InvalidLicenseException  if the license was not valid.
     * @throws com.l7tech.objectmodel.UpdateException   if the database change could not be recorded (old license restored)
     */
    void installNewLicense(String newLicenseXml) throws InvalidLicenseException, UpdateException;
}
