package com.l7tech.gateway.config.manager;

import java.sql.Connection;

/**
 * User: Mike
 * Date: Mar 6, 2007
 * Time: 9:16:14 PM
 */
public class NoopLicenseChecker implements LicenseChecker {
    public void checkLicense(Connection connection, String currentVersion, String productName, String productVersionMajor, String productVersionMinor) {
    }
}
