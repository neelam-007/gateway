package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 * User: megery
 * Date: Feb 20, 2006
 * Time: 2:38:35 PM
 */
public class DbVersion35Checker extends DbVersionChecker {

    public static final String SERVICE_RESOLUTION_TABLE = "service_resolution";
    public static final String DIGESTED_COLUMN = "digested";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        if (tableData == null) {
            return false;
        }

        Set<String> serviceResTable = tableData.get(SERVICE_RESOLUTION_TABLE);
        return serviceResTable != null && serviceResTable.contains(DIGESTED_COLUMN);

    }

    public String getVersion() {
        return "3.5";
    }
}
