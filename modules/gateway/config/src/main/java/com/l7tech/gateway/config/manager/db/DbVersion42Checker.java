package com.l7tech.gateway.config.manager.db;

import java.util.Map;
import java.util.Set;

/**
 * Version checker for version 4.2
 */
public class DbVersion42Checker extends DbVersionChecker {
    public static final String REVOCATION_POLICY_FILE_TABLE = "revocation_check_policy";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set<String> data = tableData.get(REVOCATION_POLICY_FILE_TABLE);

            if (data != null) {
                passed = true;
            }
        }
        return passed;
    }

    public String getVersion() {
        return "4.2.0";
    }
}
