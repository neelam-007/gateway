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

    /**
     * 1. Get all the APIs that are directly added to applications or indirectly added through API groups.
     * 2. Filter results that have an API key, by tenant ID, and an application status of enabled, disabled, or pending approval.
     * 3. Filter results that their application was modified, created, or had sync errors since the last sync.
     * 4. Filter results that their organization was modified
     * 5. Filter results that their API group was modified or created.
     * 6. Remove duplicate APIs
     */
    static final String SELECT_ENTITIES_SQL =
        "SELECT %s ,r.LATEST_REQ " +
        "FROM APPLICATION_API_API_GROUP_XREF aaagx " +
        "JOIN (SELECT * FROM APPLICATION " +
            "WHERE API_KEY IS NOT NULL AND TENANT_ID = '%s' AND STATUS IN ('ENABLED','DISABLED','EDIT_APPLICATION_PENDING_APPROVAL')) a ON aaagx.APPLICATION_UUID = a.UUID AND aaagx.TENANT_ID = a.TENANT_ID " +
        "JOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID AND a.TENANT_ID = o.TENANT_ID " +
        "LEFT JOIN API_GROUP ag ON ag.UUID = aaagx.API_GROUP_UUID AND ag.TENANT_ID = aaagx.TENANT_ID " +
        "LEFT JOIN APPLICATION_TENANT_GATEWAY t on t.APPLICATION_UUID = a.UUID AND t.TENANT_ID = a.TENANT_ID " +
        "LEFT JOIN (SELECT ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) as LATEST_REQ, TENANT_ID FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS, TENANT_ID) r ON a.UUID = r.ENTITY_UUID AND a.TENANT_ID = r.TENANT_ID " +
        "WHERE aaagx.API_UUID IS NOT NULL AND ((a.MODIFY_TS > ? and a.MODIFY_TS <= ?) OR (a.MODIFY_TS = 0 and a.CREATE_TS > ? and a.CREATE_TS <= ?) " +
            "OR (o.MODIFY_TS > ? and o.MODIFY_TS <= ?) OR (t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL) " +
            "OR (aaagx.API_GROUP_UUID IS NOT NULL AND ag.MODIFY_TS > ? AND ag.MODIFY_TS <= ?) " +
            "OR (aaagx.API_GROUP_UUID IS NOT NULL AND ag.CREATE_TS = ag.MODIFY_TS AND ag.CREATE_TS > ? AND ag.CREATE_TS <= ?)) ";
    static final String SELECT_ENTITIES_SQL_WITH_API_PLANS =
        "SELECT %s ,r.LATEST_REQ " +
        "FROM APPLICATION_API_API_PLAN_XREF aaapx " +
        "JOIN (SELECT * FROM APPLICATION " +
            "WHERE API_KEY IS NOT NULL AND TENANT_ID = '%s' AND STATUS IN ('ENABLED','DISABLED','EDIT_APPLICATION_PENDING_APPROVAL')) a ON aaapx.APPLICATION_UUID = a.UUID AND aaapx.TENANT_ID = a.TENANT_ID " +
        "JOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID AND a.TENANT_ID = o.TENANT_ID " +
        "LEFT JOIN APPLICATION_TENANT_GATEWAY t on t.APPLICATION_UUID = a.UUID AND t.TENANT_ID = a.TENANT_ID " +
        "LEFT JOIN (SELECT ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) as LATEST_REQ, TENANT_ID FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS, TENANT_ID) r ON a.UUID = r.ENTITY_UUID AND a.TENANT_ID = r.TENANT_ID " +
        "WHERE aaapx.API_UUID IS NOT NULL AND ((a.MODIFY_TS > ? and a.MODIFY_TS <= ?) OR (a.MODIFY_TS = 0 and a.CREATE_TS > ? and a.CREATE_TS <= ?) " +
            "OR (o.MODIFY_TS > ? and o.MODIFY_TS <= ?) OR (t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL)) ";

    static final String SELECT_DELETED_ENTITIES_SQL="SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = '%s' AND DELETED_TS > ? AND DELETED_TS <= ? AND TENANT_ID='%s'";

    /**
     * 1. Get a list of app UUIDs that does not have direct API association and indirect API association through API group
     * 2. Filter results by application created or modified time or API group created or modfiied time, or had sync errors during previous sync
     */
    static final String SELECT_APP_WITH_NO_API_SQL =
        "SELECT a.UUID FROM APPLICATION a " +
        "LEFT JOIN APPLICATION_API_API_GROUP_XREF aaagx ON aaagx.APPLICATION_UUID = a.UUID AND aaagx.TENANT_ID = a.TENANT_ID " +
        "LEFT JOIN API_GROUP ag ON ag.UUID = aaagx.API_GROUP_UUID AND ag.TENANT_ID = aaagx.TENANT_ID " +
        "LEFT JOIN APPLICATION_TENANT_GATEWAY t ON t.APPLICATION_UUID = a.UUID AND t.TENANT_ID = a.TENANT_ID " +
        "WHERE a.TENANT_ID = ? AND a.UUID NOT IN (SELECT APPLICATION_UUID " +
                                "FROM APPLICATION_API_API_GROUP_XREF " +
                                "WHERE API_UUID IS NOT NULL AND TENANT_ID = ?) " +
            "AND ((t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL) " +
            "OR (a.MODIFY_TS > ? and a.MODIFY_TS <=  ?) " +
            "OR (a.MODIFY_TS = 0 and a.CREATE_TS > ? and a.CREATE_TS <= ?) " +
            //api groups modified during sync window
            "OR (ag.UUID IS NOT NULL AND ag.MODIFY_TS > ? AND ag.MODIFY_TS <= ?)  " +
            //api groups created during sync window
            "OR (ag.UUID IS NOT NULL AND ag.CREATE_TS = ag.MODIFY_TS AND ag.CREATE_TS > ? AND ag.CREATE_TS <= ?))";

    static final String API_PLAN_SETTING_ENABLE_STATUS =
            "SELECT value FROM SETTING WHERE name='FEATURE_FLAG_API_PLANS' AND tenant_id = '%s'";
    static final String BULK_SYNC_TRUE = "true";
    static final String BULK_SYNC_FALSE = "false";
    static final String ENTITY_TYPE_APPLICATION = "APPLICATION";
    static final String ENTITY_TYPE_API = "API";

    public ServerIncrementalSyncCommon() {
    }

    public static final String getSyncDeletedEntities(String type, String tenantId) {
        return String.format(SELECT_DELETED_ENTITIES_SQL, type, tenantId);
    }

    public static final String getSyncUpdatedAppEntities(List<String> columns, String tenantId, boolean withApiPlans) {
        String columnStr = Joiner.on(",").join(columns);
        return String.format(withApiPlans ? SELECT_ENTITIES_SQL_WITH_API_PLANS : SELECT_ENTITIES_SQL, columnStr, tenantId);
    }

    public static int getQueryTimeout() {
        return queryTimeout;
    }

    public static int getMaxRecords() {
        return maxRecords;
    }
}