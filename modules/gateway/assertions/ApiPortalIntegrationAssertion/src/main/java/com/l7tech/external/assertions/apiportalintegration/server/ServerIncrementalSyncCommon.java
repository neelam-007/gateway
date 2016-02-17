package com.l7tech.external.assertions.apiportalintegration.server;

import com.google.common.base.Joiner;

import java.util.List;

/**
 * This class provides some common constants that are used by multiple
 * modules for processing incremental sync
 */
public class ServerIncrementalSyncCommon {
    // todo make configurable? cluster prop? use default jdbc?
    private static final int queryTimeout = 300;
    private static final int maxRecords = 1000000;
    final static String SELECT_ENTITIES_SQL="SELECT %s FROM APPLICATION a  \n" +
            "\tJOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID \n" +
            "\tJOIN APPLICATION_API_XREF ax on ax.APPLICATION_UUID = a.UUID\n" +
            "\tLEFT JOIN APPLICATION_TENANT_GATEWAY t on t.APPLICATION_UUID = a.UUID \n" +
            "WHERE a.API_KEY IS NOT NULL AND a.STATUS IN ('ENABLED','DISABLED') AND ( (a.MODIFY_TS > ? and a.MODIFY_TS <=  ? ) OR (a.MODIFY_TS =0 and a.CREATE_TS > ? and  a.CREATE_TS <=  ?) OR ( o.MODIFY_TS > ? and  o.MODIFY_TS <=  ?) OR (t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL))";
    final static String SELECT_DELETED_ENTITIES_SQL="SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = '%s' AND DELETED_TS > ? AND DELETED_TS <= ?";
    static final String BULK_SYNC_TRUE = "true";
    static final String BULK_SYNC_FALSE = "false";
    static final String ENTITY_TYPE_APPLICATION = "APPLICATION";

    public ServerIncrementalSyncCommon() {
    }

    public final static String getSyncDeletedEntities(String type) {
        return String.format(SELECT_DELETED_ENTITIES_SQL, type);
    }

    public final static String getSyncUpdatedAppEntities(List<String> columns) {
        String columnStr = Joiner.on(",").join(columns);
        return String.format(SELECT_ENTITIES_SQL, columnStr
        );
    }

    public static int getQueryTimeout() {
        return queryTimeout;
    }

    public static int getMaxRecords() {
        return maxRecords;
    }
}