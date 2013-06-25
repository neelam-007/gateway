package com.l7tech.policy.bundle;

import java.io.Serializable;
import java.util.List;

/**
 * Result from running a dry run installation of a bundle
 */
public interface PolicyBundleDryRunResult extends Serializable {

    enum DryRunItem { SERVICES, POLICIES, CERTIFICATES, JDBC_CONNECTIONS, ASSERTIONS}

    /**
     * Does the bundle with bundleId contain any conflicts?
     *
     * This is for convenience to not have to check each item individually.
     *
     * @param bundleId bundleId to check for
     * @return true if any conflicts, false otherwise.
     */
    boolean anyConflictsForBundle(String bundleId) throws UnknownBundleIdException;

    /**
     * Get the list of conflicting values for the given BundleItem. What the values are depends on the
     * item.
     * Services - URL's with conflict
     * Policies - names with conflict
     * Folder - names with local conflict - given the installation directory
     *
     *
     *
     *
     * @param bundleId id of bundle
     * @param dryRunItem item from bundle to get conflicts for
     * @return list of conflict values
     */
    List<String> getConflictsForItem(String bundleId, final DryRunItem dryRunItem) throws UnknownBundleIdException;

    public static class UnknownBundleIdException extends Exception{
        public UnknownBundleIdException(String message) {
            super(message);
        }
    }
}