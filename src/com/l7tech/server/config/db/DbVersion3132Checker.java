package com.l7tech.server.config.db;

import java.util.Hashtable;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Nov 28, 2005
 * Time: 12:55:36 PM
 * To change this template use File | Settings | File Templates.
 */
public class DbVersion3132Checker extends DbVersionChecker {

    public static final String COUNTERS_TABLE= "counters";
    public static final String AUDIT_MESSAGE_TABLE = "audit_message";

    private String realVersion = "3.2";

    public boolean doCheck(Hashtable<String, Set> tableData) {
        boolean passed = false;
        if (tableData == null) {
            return passed;
        }

        Set counterTable = tableData.get(COUNTERS_TABLE);
        Set auditTable = tableData.get(AUDIT_MESSAGE_TABLE);

        if (auditTable == null) {
            passed = false;
        } else {
            passed = true;
            if (counterTable == null) {
                realVersion = "3.1";
            }
            else {
                realVersion = "3.2";
            }
        }
        return passed;
    }

    public String getVersion() {
        return realVersion;
    }
}
