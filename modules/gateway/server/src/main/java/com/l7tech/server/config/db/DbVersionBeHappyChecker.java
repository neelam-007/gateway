package com.l7tech.server.config.db;

import com.l7tech.util.BuildInfo;
import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.Set;

/**
 * DB Checker that passes the current version if the previous checker is successful.
 *
 * <p>This is meant to always succeed, useful if there are no DB changes for 
 * the current version.</p>
 */
public class DbVersionBeHappyChecker extends DbVersionChecker {

    //- PUBLIC

    public DbVersionBeHappyChecker( final DbVersionChecker previousChecker ) {
        this.previousChecker = previousChecker;
    }

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean result = true;

        if ( previousChecker != null ) {
            result = previousChecker.doCheck( tableData );
        }

        return result;
    }

    /**
     * Generate version number for this build.
     *
     * @return The current version number.
     */
    public String getVersion() {
        String currentVersion = BuildInfo.getProductVersionMajor() + "." + BuildInfo.getProductVersionMinor();
        String subminor = BuildInfo.getProductVersionSubMinor();
        if (StringUtils.isNotEmpty(subminor)) 
            currentVersion += "." + subminor;
        return currentVersion;
    }

    //- PRIVATE

    private final DbVersionChecker previousChecker;
}
