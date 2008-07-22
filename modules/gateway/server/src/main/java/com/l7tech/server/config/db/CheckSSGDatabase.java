package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Nov 28, 2005
 * Time: 1:26:06 PM
 * To change this template use File | Settings | File Templates.
 */
public class CheckSSGDatabase extends DbVersionChecker {
    private static final String PUBLISHEDSERVICE_TABLE = "published_service";

    public boolean doCheck(Map<String, Set<String>> tableData){
        boolean passed = false;
        Set columns = tableData.keySet();
        passed = columns.contains(PUBLISHEDSERVICE_TABLE);
        return passed;
    }

    public String getVersion() {
        return "";
    }
}
