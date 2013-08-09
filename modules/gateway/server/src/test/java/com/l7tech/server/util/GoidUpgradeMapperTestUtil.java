package com.l7tech.server.util;

/**
 * This is used to help in unit tests that expect goid prefixes to be available.
 *
 * @author Victor Kazakov
 */
public class GoidUpgradeMapperTestUtil {
    public static void addPrefix(String tableName, long prefix) {
        GoidUpgradeMapper.addPrefix(tableName, prefix);
    }
}
