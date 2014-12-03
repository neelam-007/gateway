package com.l7tech.policy.bundle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;

/**
 * Result from running a dry run installation of a bundle
 */
public class PolicyBundleDryRunResult implements Serializable {

    public enum DryRunItem { SERVICES, POLICIES, CERTIFICATES, ENCAPSULATED_ASSERTION, JDBC_CONNECTIONS, ASSERTIONS }

    public static class UnknownBundleIdException extends Exception {
        public UnknownBundleIdException(String message) {
            super(message);
        }
    }

    @NotNull
    private final Map<String, Map<DryRunItem, List<String>>> conflictsForItemMap = new HashMap<>();
    @Nullable
    private final Map<String, List<MigrationDryRunResult>> migrationDryRunResultsMap;

    public PolicyBundleDryRunResult(@NotNull final Map<String, Map<DryRunItem, List<String>>> bundleToConflicts,
                                    @Nullable final Map<String, List<MigrationDryRunResult>> migrationDryRunResultsMap) {
        // do we really need to copy the map?  it's probably safe to work directly with original map
        for (Map.Entry<String, Map<DryRunItem, List<String>>> entry : bundleToConflicts.entrySet()) {
            final Map<DryRunItem, List<String>> itemsForBundle = entry.getValue();
            final Map<DryRunItem, List<String>> copiedItems = new HashMap<>();
            for (Map.Entry<DryRunItem, List<String>> bundleItemEntry : itemsForBundle.entrySet()) {
                copiedItems.put(bundleItemEntry.getKey(), new ArrayList<>(bundleItemEntry.getValue()));
            }

            conflictsForItemMap.put(entry.getKey(), copiedItems);
        }

        this.migrationDryRunResultsMap = migrationDryRunResultsMap;
    }

    @NotNull
    public List<MigrationDryRunResult> getMigrationDryRunResults(String bundleId) {
        if (migrationDryRunResultsMap == null) {
            return new ArrayList<>();
        }
        return migrationDryRunResultsMap.get(bundleId);
    }

    /**
     * Does the bundle with bundleId contain any conflicts?
     *
     * This is for convenience to not have to check each item individually.
     *
     * @param bundleId bundleId to check for
     * @return true if any conflicts, false otherwise.
     */
    public boolean anyConflictsForBundle(String bundleId) {
        final Map<DryRunItem, List<String>> itemMap = conflictsForItemMap.get(bundleId);
        if (itemMap != null) {
            for (Map.Entry<DryRunItem, List<String>> entry : itemMap.entrySet()) {
                if (!entry.getValue().isEmpty()) {
                    return true;
                }
            }
        }

        return migrationDryRunResultsMap != null && migrationDryRunResultsMap.containsKey(bundleId);
    }

    /**
     * Get the list of conflicting values for the given BundleItem. What the values are depends on the item.
     * Services - URL's with conflict
     * Policies - names with conflict
     * Folder - names with local conflict - given the installation directory
     *
     * @param bundleId id of bundle
     * @param dryRunItem item from bundle to get conflicts for
     * @return list of conflict values
     */
    public List<String> getConflictsForItem(String bundleId, DryRunItem dryRunItem) throws UnknownBundleIdException {
        if (!conflictsForItemMap.containsKey(bundleId)) {
            throw new UnknownBundleIdException("Unknown bundle id #{" + bundleId + "}");
        }

        final Map<DryRunItem, List<String>> itemMap = conflictsForItemMap.get(bundleId);
        if (!itemMap.containsKey(dryRunItem)) {
            return Collections.emptyList();
        }

        return Collections.unmodifiableList(itemMap.get(dryRunItem));
    }
}