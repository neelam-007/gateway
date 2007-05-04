package com.l7tech.server.config;

/**
 * User: megery
 * Date: Apr 26, 2007
 * Time: 3:59:23 PM
 */
public abstract class UnixSpecificFunctions extends OSSpecificFunctions {
    public UnixSpecificFunctions(String OSName) {
        super(OSName);
    }

    public UnixSpecificFunctions(String OSName, String partitionName) {
        super(OSName, partitionName);
    }

    public boolean isUnix() {
        return true;
    }

    public boolean isWindows() {
        return false;
    }
}
