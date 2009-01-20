package com.l7tech.gateway.config.manager.db;

import com.l7tech.gateway.config.manager.db.DbVersionChecker;

import java.util.Set;
import java.util.Map;

/**
 * User: megery
 * Date: Nov 12, 2008
 */
public class DbVersion465Checker extends DbVersionChecker {
    private static final String EMAIL_LISTENER_TABLE_NAME = "email_listener";
    private static final String EMAIL_LISTENER_PROPERTIES_COLUMN = "properties";
    private static final String SHARED_KEYS_TABLE_NAME="shared_keys";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;

        if (tableData != null) {
            passed = tableData.containsKey(EMAIL_LISTENER_TABLE_NAME);

            //check if password expiry column is in internal_user table
            if (passed) {
                Set<String> data = tableData.get(EMAIL_LISTENER_TABLE_NAME);
                if (data != null) {
                    passed = data.contains(EMAIL_LISTENER_PROPERTIES_COLUMN.toLowerCase());
                }
            }
        }

        return passed;
    }

    public String getVersion() {
        return "4.6.5";
    }

}
