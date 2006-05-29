package com.l7tech.server.config.db;

import java.util.Hashtable;
import java.util.Set;

/**
 *
 * User: megery
 * Time: 12:56:07 PM
 */
public abstract class DbVersionChecker implements Comparable{
    public abstract boolean doCheck(Hashtable<String, Set> tableData);
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
