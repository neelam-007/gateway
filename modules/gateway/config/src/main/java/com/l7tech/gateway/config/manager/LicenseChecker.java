package com.l7tech.gateway.config.manager;

import com.l7tech.gateway.common.InvalidLicenseException;

import java.sql.Connection;

/**
 * User: Mike
 * Date: Mar 6, 2007
 * Time: 9:15:21 PM
 */
public interface LicenseChecker {
    void checkLicense(Connection connection, String currentVersion, String productName, String productVersionMajor, String productVersionMinor) throws InvalidLicenseException;
}
