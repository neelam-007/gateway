package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 * User: megery
 * Date: Nov 9, 2006
 * Time: 4:08:56 PM
 */
public class DbVersion365Checker extends DbVersionChecker{
    public static final String AUDIT_MSG_TABLE_NAME = "audit_message";
    public static final String TRUSTED_CERT_TABLE_NAME = "trusted_cert";

    public static final String AUTH_TYPE_COLUMN = "authenticationType";
    private static final String VERIFY_HOSTNAME_COLUMN = "verify_hostname";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passedAuditCheck = false;
        boolean passedTrustedCertCheck = false;

        if (tableData != null) {
            Set<String> data = tableData.get(AUDIT_MSG_TABLE_NAME);
            if (data != null) {
                passedAuditCheck =  data.contains(AUTH_TYPE_COLUMN) ||
                                    data.contains(AUTH_TYPE_COLUMN.toLowerCase());
            }

            data = tableData.get(TRUSTED_CERT_TABLE_NAME);
            if (data != null) {
                passedTrustedCertCheck =    data.contains(VERIFY_HOSTNAME_COLUMN) ||
                                            data.contains(VERIFY_HOSTNAME_COLUMN);
            }
        }
        return passedAuditCheck && passedTrustedCertCheck;
    }

    public String getVersion() {
        return "3.6.5";
    }
}
