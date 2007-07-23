package com.l7tech.server.config.db;

import java.util.Set;
import java.util.Hashtable;

/**
 * Version checker for version 4.2
 */
public class DbVersion42Checker extends DbVersionChecker {
    public static final String REVOCATION_POLICY_FILE_TABLE = "revocation_check_policy";

    public boolean doCheck(Hashtable<String, Set> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set data = tableData.get(REVOCATION_POLICY_FILE_TABLE);

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
