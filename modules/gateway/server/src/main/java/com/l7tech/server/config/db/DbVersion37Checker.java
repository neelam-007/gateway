package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 * Checks the database to see if it looks like a version 3.7 database.
 */
public class DbVersion37Checker extends DbVersionChecker {

    public static final String SERVICE_DOCUMENTS_TABLE = "service_documents";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set<String> data = tableData.get(SERVICE_DOCUMENTS_TABLE);

            if (data != null) {
                passed = true;
            }
        }
        return passed;
    }

    public String getVersion() {
        return "3.7.0";
    }
}
