package com.l7tech.server.config.db;

import java.util.Set;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;

import com.l7tech.common.BuildInfo;

/**
 * DB Checker that always passes the current version.
 *
 * <p>This is meant to always succeed, useful if there are no DB changes for 
 * the current version.</p>
 */
public class DbVersionBeHappyChecker extends DbVersionChecker {

    public boolean doCheck(Hashtable<String, Set> tableData) {
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
