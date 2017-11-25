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
     * 1. Get all the APIs that are directly added to applications.
     * 2. Get all the APIs that are associated to applications through API groups.  These APIs should either be public or belong to the same org as the application.
     * 3. Union 1 & 2.
     * 4. Filter results that have an API key and an application status of enabled, disabled, or pending approval.
     * 5. Filter results that their application was modified, created, or had sync errors since the last sync.
     * 6. Filter results that their API group was modified or created.
     * 7. Filter results by tenant ID
     * 8. Remove duplicate APIs
     */
    final static String SELECT_ENTITIES_SQL =
        "SELECT %s ,r.LATEST_REQ " +
        "FROM (SELECT a.UUID, a.NAME, a.API_KEY, a.KEY_SECRET, a.STATUS, a.ORGANIZATION_UUID, " +
                    "a.OAUTH_CALLBACK_URL, a.OAUTH_SCOPE, a.OAUTH_TYPE, a.MAG_SCOPE, a.MAG_MASTER_KEY, " +
                    "ax.API_UUID, a.CREATED_BY, a.MODIFIED_BY, a.MODIFY_TS, a.CREATE_TS, null AS API_GROUP_UUID, null AS API_GROUP_CREATED_TS, null AS API_GROUP_MODIFIED_TS, a.TENANT_ID " +
                    "FROM APPLICATION a " +
                    "JOIN APPLICATION_API_XREF ax on ax.APPLICATION_UUID = a.UUID " +
               "UNION " +
               "SELECT a2.UUID, a2.NAME, a2.API_KEY, a2.KEY_SECRET, a2.STATUS, a2.ORGANIZATION_UUID, " +
                    "a2.OAUTH_CALLBACK_URL, a2.OAUTH_SCOPE, a2.OAUTH_TYPE, a2.MAG_SCOPE, a2.MAG_MASTER_KEY, " +
                    "agax.API_UUID, a2.CREATED_BY, a2.MODIFIED_BY, a2.MODIFY_TS, a2.CREATE_TS, ag.UUID AS API_GROUP_UUID, ag.CREATE_TS AS API_GROUP_CREATED_TS, ag.MODIFY_TS AS API_GROUP_MODIFIED_TS, a2.TENANT_ID " +
               "FROM APPLICATION_API_GROUP_XREF aagx " +
               "JOIN APPLICATION a2 ON a2.UUID = aagx.APPLICATION_UUID " +
               "JOIN  API_GROUP_API_XREF agax ON agax.API_GROUP_UUID = aagx.API_GROUP_UUID " +
               "JOIN API_GROUP ag ON ag.UUID = agax.API_GROUP_UUID " +
               "JOIN ORGANIZATION_ALL_API_VIEW oav ON oav.UUID = agax.API_UUID " +
               "WHERE (oav.ACCESS_STATUS = 'PUBLIC' OR (oav.ACCESS_STATUS = 'PRIVATE' AND oav.ORGANIZATION_UUID = a2.ORGANIZATION_UUID))) a3 " +
        "JOIN ORGANIZATION o on a3.ORGANIZATION_UUID = o.UUID " +
        "LEFT JOIN APPLICATION_TENANT_GATEWAY t on t.APPLICATION_UUID = a3.UUID " +
        "LEFT JOIN (SELECT ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) as LATEST_REQ FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS) r ON a3.UUID = r.ENTITY_UUID " +
        "WHERE a3.API_KEY IS NOT NULL AND a3.STATUS IN ('ENABLED','DISABLED','EDIT_APPLICATION_PENDING_APPROVAL') " +
            "AND ((a3.MODIFY_TS > ? and a3.MODIFY_TS <= ?) OR (a3.MODIFY_TS = 0 and a3.CREATE_TS > ? and a3.CREATE_TS <= ?) " +
            "OR ( o.MODIFY_TS > ? and o.MODIFY_TS <= ?) OR (t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL) " +
            "OR (a3.API_GROUP_UUID IS NOT NULL AND a3.API_GROUP_MODIFIED_TS > ? AND a3.API_GROUP_MODIFIED_TS <= ?) " +
            "OR (a3.API_GROUP_UUID IS NOT NULL AND a3.API_GROUP_CREATED_TS = a3.API_GROUP_MODIFIED_TS AND a3.API_GROUP_CREATED_TS > ? AND a3.API_GROUP_CREATED_TS <= ?)) " +
            "AND a3.TENANT_ID = '%s' ";

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