package com.l7tech.server.config.db;

import java.util.Set;
import java.util.Map;

/**
 * Version checker for version 4.3
 */
public class DbVersion43Checker extends DbVersionChecker {
    public static final String CONNECTOR_TABLE = "connector";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set<String> data = tableData.get(CONNECTOR_TABLE);

            if (data != null) {
                passed = true;
            }
        }
        return passed;
    }

    public String getVersion() {
        return "4.3.0";
    }
}
