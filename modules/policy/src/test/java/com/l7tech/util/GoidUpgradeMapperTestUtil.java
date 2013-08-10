package com.l7tech.util;

/**
 * This is used to help in unit tests that expect goid prefixes to be available.
 *
 * @author Victor Kazakov
 */
public class GoidUpgradeMapperTestUtil {
    /**
     * Add a mapping to the GOID upgrade map.
     *
     * @param tableName the table name, ie "trusted_cert".  Required.
     * @param prefix the assigned GOID upgraded prefix for this upgraded table,
     *               which must not be in the reserved range between 0 and 15 inclusive.
     */
    public static void addPrefix(String tableName, long prefix) {
        GoidUpgradeMapper.addPrefix(tableName, prefix);
    }

    /**
     * Clear all prefixes from the GOID upgrade map, for testing purposes.
     */
    public static void clearAllPrefixes() {
        GoidUpgradeMapper.clearAllPrefixes();
    }
}
