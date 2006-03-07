package com.l7tech.server.config.db;

import java.util.Hashtable;
import java.util.Set;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 2:38:35 PM
 */
public class DbVersion35Checker extends DbVersionChecker {

    public static final String SERVICE_RESOLUTION_TABLE = "service_resolution";
    public static final String DIGESTED_COLUMN = "digested";

    public boolean doCheck(Hashtable tableData) {
        boolean passed = false;

        if (tableData == null) {
            return passed;
        }

        Set serviceResTable = (Set) tableData.get(SERVICE_RESOLUTION_TABLE);
        if (serviceResTable == null) {
            return passed;
        }

        passed = serviceResTable.contains(DIGESTED_COLUMN);

        return passed;
    }

    public String getVersion() {
        return "3.5";
    }
}
