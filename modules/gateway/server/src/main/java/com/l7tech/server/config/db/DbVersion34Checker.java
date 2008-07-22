package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 *  Checks the database metainfo to see if it looks like part of a 3.4 database (based on the latest ssg.sql for 3.4).
 *  User: megery
 */
public class DbVersion34Checker extends DbVersionChecker {
    public static final String CLIENT_CERT_TABLE = "client_cert";
    public static final String TRUSTED_CERT_TABLE = "trusted_cert";

    public static final String SKI_COLUMN ="ski";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        if (tableData == null) {
            return false;
        }

        boolean passed = false;
        Set<String> clientCertColumns = tableData.get(CLIENT_CERT_TABLE);
        Set<String> trustedCertColumns= tableData.get(TRUSTED_CERT_TABLE);

        if (clientCertColumns != null && trustedCertColumns != null) {
            passed = clientCertColumns.contains(SKI_COLUMN) && trustedCertColumns.contains(SKI_COLUMN);
        }
        return passed;
    }

    public String getVersion() {
        return "3.4";
    }
}
