package com.l7tech.gateway.config.manager.db;

import java.util.Map;
import java.util.Set;

/**
 *
 * User: megery
 * Time: 12:56:07 PM
 */
public abstract class DbVersionChecker implements Comparable{
    public abstract boolean doCheck(Map<String, Set<String>> tableData);
    public abstract String getVersion();

    public int compareTo(Object o) {
        if (o instanceof DbVersionChecker) {
            DbVersionChecker versionChecker2 = (DbVersionChecker) o;
            String thisVersion = this.getVersion();
            String thatVersion = versionChecker2.getVersion();
            return thisVersion.compareTo(thatVersion);
        }
        throw new ClassCastException("Expected " + DbVersionChecker.class +
            " received " + o.getClass());
    }
}
