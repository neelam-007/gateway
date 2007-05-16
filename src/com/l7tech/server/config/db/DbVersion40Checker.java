package com.l7tech.server.config.db;

import java.util.Set;
import java.util.Hashtable;

/**
 * User: megery
 * Date: May 16, 2007
 * Time: 12:23:41 PM
 */
public class DbVersion40Checker extends DbVersionChecker {
    public static final String KEYSTORE_FILE_TABLE = "keystore_file";

    public boolean doCheck(Hashtable<String, Set> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set data = tableData.get(KEYSTORE_FILE_TABLE);

            if (data != null) {
                passed = true;
            }
        }
        return passed;
    }

    public String getVersion() {
        return "4.0";
    }
}
