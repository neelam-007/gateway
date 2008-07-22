package com.l7tech.server.config;

import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Mar 6, 2007
 * Time: 9:16:14 PM
 * To change this template use File | Settings | File Templates.
 */
public class NoopLicenseChecker implements LicenseChecker {

    public void checkLicense(Connection connection, String currentVersion, String productName, String productVersionMajor, String productVersionMinor) {
    }
}
