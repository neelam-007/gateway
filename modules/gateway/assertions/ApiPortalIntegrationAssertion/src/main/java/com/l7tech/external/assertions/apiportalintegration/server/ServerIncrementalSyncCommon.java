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
    final static String SELECT_ENTITIES_SQL="SELECT %s , r.LATEST_REQ FROM APPLICATION a " +
            "JOIN (SELECT UUID, NAME, MODIFY_TS FROM ORGANIZATION GROUP BY UUID, NAME, MODIFY_TS ) o on a.ORGANIZATION_UUID = o.UUID " +
            "JOIN (SELECT API_UUID, APPLICATION_UUID FROM APPLICATION_API_XREF GROUP BY API_UUID, APPLICATION_UUID) ax on ax.APPLICATION_UUID = a.UUID " +
            "LEFT JOIN (SELECT APPLICATION_UUID, TENANT_GATEWAY_UUID, SYNC_LOG FROM APPLICATION_TENANT_GATEWAY GROUP BY APPLICATION_UUID, TENANT_GATEWAY_UUID, SYNC_LOG) t on t.APPLICATION_UUID = a.UUID " +
            "LEFT JOIN (select ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) as LATEST_REQ FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS) r ON a.UUID = r.ENTITY_UUID " +
            "WHERE a.API_KEY IS NOT NULL AND a.STATUS IN ('ENABLED','DISABLED','EDIT_APPLICATION_PENDING_APPROVAL') AND ( (a.MODIFY_TS > ? and a.MODIFY_TS <=  ? ) " +
            "OR (a.MODIFY_TS =0 and a.CREATE_TS > ? and  a.CREATE_TS <=  ?) OR ( o.MODIFY_TS > ? and  o.MODIFY_TS <=  ?) OR (t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL)) AND a.TENANT_ID = '%s'";

    final static String SELECT_DELETED_ENTITIES_SQL="SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = '%s' AND DELETED_TS > ? AND DELETED_TS <= ? AND TENANT_ID='%s'";
    static final String BULK_SYNC_TRUE = "true";
    static final String BULK_SYNC_FALSE = "false";
    static final String ENTITY_TYPE_APPLICATION = "APPLICATION";
    static final String ENTITY_TYPE_API = "API";

    public ServerIncrementalSyncCommon() {
    }

    public final static String getSyncDeletedEntities(String type, String tenantId) {
        return String.format(SELECT_DELETED_ENTITIES_SQL, type, tenantId);
    }

    public final static String getSyncUpdatedAppEntities(List<String> columns, String tenantId ) {
        String columnStr = Joiner.on(",").join(columns);
        return String.format(SELECT_ENTITIES_SQL, columnStr, tenantId);
    }

    public static int getQueryTimeout() {
        return queryTimeout;
    }

    public static int getMaxRecords() {
        return maxRecords;
    }
}