package com.l7tech.server.licensing;

import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.licensing.*;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jamie Williams - wilja33 - jamie.williams2@ca.com
 */
public interface UpdatableCompositeLicenseManager extends CompositeLicenseManager {

    /**
     * Get the composite license of currently-installed licenses, or null if no licenses are
     * installed or present in the database.
     *
     * @return the currently installed valid license, or null if no license is installed or present in the database.
     */
    @Transactional(readOnly=true)
    public CompositeLicense getCurrentCompositeLicense();

    /**
     * Creates a FeatureLicense representing all the information contained in a Gateway license document, represented
     * by the given LicenseDocument.
     *
     * @param licenseDocument the LicenseDocument from which to create the FeatureLicense
     * @return the created FeatureLicense
     * @throws InvalidLicenseException if the license document is poorly formed, is missing information, or otherwise
     * does not conform to the expectations of a feature license document.
     */
    public FeatureLicense createFeatureLicense(LicenseDocument licenseDocument) throws InvalidLicenseException;

    /**
     * Check the validity (i.e. applicability to current environment) of the license.
     *
     * @param license the FeatureLicense to validate
     * @throws InvalidLicenseException if the FeatureLicense is not valid, detailing the reason
     */
    public void validateLicense(FeatureLicense license) throws InvalidLicenseException;

    /**
     * Installs the specified FeatureLicense under the control of the LicenseManager.
     *
     * @param license the FeatureLicense to install
     * @throws LicenseInstallationException if an error was encountered in the installation attempt
     */
    public void installLicense(FeatureLicense license) throws LicenseInstallationException;

    /**
     * Uninstalls the specified FeatureLicense from the control of the LicenseManager.
     *
     * @param license the FeatureLicense to uninstall
     * @throws LicenseRemovalException if an error was encountered in the uninstallation attempt
     */
    public void uninstallLicense(FeatureLicense license) throws LicenseRemovalException;
}
