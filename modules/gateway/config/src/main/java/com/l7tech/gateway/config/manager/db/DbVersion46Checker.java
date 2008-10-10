package com.l7tech.gateway.config.manager.db;

import java.util.Set;
import java.util.Map;

/**
 * version 4.6 checker
 * <p>
 * Note that this checker is identical to the DbVersion44Checker except for getVersion().
 */
public class DbVersion46Checker extends DbVersionChecker {
    private static final String EMAIL_LISTENER_TABLE_NAME = "email_listener";
    private static final String PASSWORD_HISTORY_TABLE_NAME = "password_history";
    private static final String LOGON_INFO_TABLE_NAME = "logon_info";
    private static final String INTERNAL_USER_TABLE_NAME = "internal_user";
    private static final String PASSWORD_EXPIRY_COLUMN_NAME = "password_expiry";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;

        if (tableData != null) {
            passed = tableData.containsKey(EMAIL_LISTENER_TABLE_NAME) &&
                    tableData.containsKey(PASSWORD_HISTORY_TABLE_NAME) &&
                    tableData.containsKey(LOGON_INFO_TABLE_NAME);

            //check if password expiry column is in internal_user table
            if (passed) {
                Set<String> data = tableData.get(INTERNAL_USER_TABLE_NAME);
                if (data != null) {
                    passed = data.contains(PASSWORD_EXPIRY_COLUMN_NAME.toLowerCase());
                }
            }
        }

        return passed;
    }

    public String getVersion() {
        return "4.6.0";
    }
}