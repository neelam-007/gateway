package com.l7tech.gateway.config.manager.db;

import java.util.Map;
import java.util.Set;

/**
 * Checker to ensure database is an SSG DB
 */
public class CheckSSGDatabase extends DbVersionChecker {
    private static final String PUBLISHEDSERVICE_TABLE = "published_service";

    public boolean doCheck(Map<String, Set<String>> tableData){
        Set columns = tableData.keySet();
        return columns.contains(PUBLISHEDSERVICE_TABLE);
    }

    public String getVersion() {
        return "";
    }
}
