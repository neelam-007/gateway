package com.l7tech.gateway.config.manager.db;

import java.util.Set;
import java.util.Map;

/**
 * Version checker for 5.0
 */
public class DBVersion50Checker extends DbVersionChecker {
    
    public static final String PUBLISHED_SERVICE_ALIAS_TABLE = "published_service_alias";

    public boolean doCheck( final Map<String, Set<String>> tableData ) {
        boolean passed = false;

        if ( tableData != null ) {
            passed = tableData.get(PUBLISHED_SERVICE_ALIAS_TABLE) != null;
        }

        return passed;
    }

    public String getVersion() {
        return "5.0";
    }
}
