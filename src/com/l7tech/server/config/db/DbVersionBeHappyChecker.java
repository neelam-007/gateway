package com.l7tech.server.config.db;

import com.l7tech.common.BuildInfo;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * DB Checker that always passes the current version.
 *
 * <p>This is meant to always succeed, useful if there are no DB changes for 
 * the current version.</p>
 */
public class DbVersionBeHappyChecker extends DbVersionChecker {

    public boolean doCheck(Map<String, Set<String>> tableData) {
        return true;
    }

    public String getVersion() {
        String currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
        String subminor = BuildInfo.getProductVersionSubMinor();
        if (StringUtils.isNotEmpty(subminor)) 
            currentVersion += "." + subminor;
        return currentVersion;
    }
}
