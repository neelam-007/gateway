package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 * Checks the database metainfo to see if it looks like part of a 4.0 database (based on the latest ssg.sql for 4.0).
 * User: megery
 */
public class DbVersion36Checker extends DbVersionChecker {

    public static final String PUBLISHED_SERVICE_TABLE = "published_service";
    public static final String HTTP_METHODS_COLUMN = "http_methods";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set<String> data = tableData.get(PUBLISHED_SERVICE_TABLE);

            passed = (data != null && data.contains(HTTP_METHODS_COLUMN));
        }
        return passed;
    }

    public String getVersion() {
        return "3.6";
    }
}
