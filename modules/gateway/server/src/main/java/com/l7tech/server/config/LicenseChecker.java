package com.l7tech.server.config;

import com.l7tech.gateway.common.InvalidLicenseException;

import java.sql.Connection;

/**
 * Created by IntelliJ IDEA.
 * User: Mike
 * Date: Mar 6, 2007
 * Time: 9:15:21 PM
 * To change this template use File | Settings | File Templates.
 */
public interface LicenseChecker {
    void checkLicense(Connection connection, String currentVersion, String productName, String productVersionMajor, String productVersionMinor) throws InvalidLicenseException;
}
