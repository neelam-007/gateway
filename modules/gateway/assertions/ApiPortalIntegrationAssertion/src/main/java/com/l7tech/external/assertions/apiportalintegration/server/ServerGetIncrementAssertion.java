package com.l7tech.external.assertions.apiportalintegration.server;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationApi;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationEntity;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationJson;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationMag;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Server side implementation of the GetIncrementAssertion.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion
 */
public class ServerGetIncrementAssertion extends AbstractServerAssertion<GetIncrementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerGetIncrementAssertion.class.getName());

    /**
     * 1. Get all the APIs that are directly added to applications or indirectly added through API groups.
     * 2. Filter results that have an API key, by tenant ID, and an application status of enabled, disabled, or pending approval.
     * 3. Remove duplicate APIs
     */
    private static final String BULK_SYNC_SELECT =
        "SELECT DISTINCT(aaagx.API_UUID), a.UUID, concat(a.NAME,'-',o.NAME) AS NAME, a.API_KEY, a.KEY_SECRET, " +
            "coalesce (r.PREVIOUS_STATE, a.STATUS) AS STATUS, a.ORGANIZATION_UUID, o.NAME AS ORGANIZATION_NAME, " +
            "a.OAUTH_CALLBACK_URL, a.OAUTH_SCOPE, a.OAUTH_TYPE, a.MAG_SCOPE, a.MAG_MASTER_KEY, a.CREATED_BY, a.MODIFIED_BY, r.LATEST_REQ " +
            "FROM APPLICATION_API_API_GROUP_XREF aaagx " +
            "JOIN (SELECT * FROM APPLICATION " +
            "WHERE API_KEY IS NOT NULL AND TENANT_ID ='%s' AND STATUS IN ('ENABLED','DISABLED','EDIT_APPLICATION_PENDING_APPROVAL')) a ON aaagx.APPLICATION_UUID = a.UUID AND aaagx.TENANT_ID = a.TENANT_ID " +
            "JOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID AND a.TENANT_ID = o.TENANT_ID " +
            "LEFT JOIN API_GROUP ag ON ag.UUID = aaagx.API_GROUP_UUID AND ag.TENANT_ID = aaagx.TENANT_ID " +
            "LEFT JOIN (SELECT ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) AS LATEST_REQ, TENANT_ID " +
            "FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS, TENANT_ID) r ON a.UUID = r.ENTITY_UUID AND a.TENANT_ID = r.TENANT_ID " +
            "WHERE aaagx.API_UUID IS NOT NULL ";
    private static final String BULK_SYNC_SELECT_WITH_API_PLANS =
        "SELECT DISTINCT(aaapx.API_UUID), aaapx.API_PLAN_UUID, a.UUID, concat(a.NAME,'-',o.NAME) AS NAME, a.API_KEY, a.KEY_SECRET, " +
            "coalesce (r.PREVIOUS_STATE, a.STATUS) AS STATUS, a.ORGANIZATION_UUID, o.NAME AS ORGANIZATION_NAME, " +
            "a.OAUTH_CALLBACK_URL, a.OAUTH_SCOPE, a.OAUTH_TYPE, a.MAG_SCOPE, a.MAG_MASTER_KEY, a.CREATED_BY, a.MODIFIED_BY, r.LATEST_REQ " +
            "FROM APPLICATION_API_API_PLAN_XREF aaapx " +
            "JOIN (SELECT * FROM APPLICATION " +
            "WHERE API_KEY IS NOT NULL AND TENANT_ID ='%s' AND STATUS IN ('ENABLED','DISABLED','EDIT_APPLICATION_PENDING_APPROVAL')) a ON aaapx.APPLICATION_UUID = a.UUID AND aaapx.TENANT_ID = a.TENANT_ID " +
            "JOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID AND a.TENANT_ID = o.TENANT_ID " +
            "LEFT JOIN (SELECT ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) AS LATEST_REQ, TENANT_ID " +
            "FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS, TENANT_ID) r ON a.UUID = r.ENTITY_UUID AND a.TENANT_ID = r.TENANT_ID " +
            "WHERE aaapx.API_UUID IS NOT NULL";

  private static final String STATUS_ENABLED = "ENABLED";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_SUSPEND = "suspend";

    private static final String APPLICATION_MAG_SCOPE_MSSO = "msso";
    private static final String APPLICATION_SCOPE_OOB =   "oob";
    private static final String APPLICATION_MAG_SCOPE_OPENID =   "openid";

    public static final String FIELD_ENTITY_UUID = "entity_uuid";
    public static final String FIELD_SYSTEM_PROPERTY_NAME = "system_property_name";
    public static final String FIELD_VALUE = "value";


    private final String[] variablesUsed;
    private final JdbcQueryingManager jdbcQueryingManager;
    private final JdbcConnectionManager jdbcConnectionManager;

    public ServerGetIncrementAssertion(GetIncrementAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
        jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

            Object entityType = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_TYPE);
            Object jdbcConnectionName = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_JDBC_CONNECTION);
            Object sinceStr = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_SINCE);
            Object nodeId = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_NODE_ID);
            Object tenantId = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_TENANT_ID);

            // validate inputs
            if (entityType == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply an entity type");
            }
            if (!entityType.equals(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION)) {
                throw new PolicyAssertionException(assertion, "Not supported entity type: " + entityType);
            }

            // validate inputs
            if (nodeId == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a node id");
            }

            Long since = null;
            if (sinceStr != null && sinceStr instanceof String && !((String) sinceStr).isEmpty()) {
                since = Long.parseLong((String) sinceStr);
            }

            if (jdbcConnectionName == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
            }

            if (tenantId == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a valid tenant prefix");
            }

            // validate that the connection exists.
            final JdbcConnection connection;
            try {
                connection = jdbcConnectionManager.getJdbcConnection(jdbcConnectionName.toString());
                if (connection == null) throw new FindException();
            } catch (FindException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Could not find JDBC connection: " + jdbcConnectionName);
                return AssertionStatus.FAILED;
            }

            // create result
            String jsonStr = getJsonMessage(jdbcConnectionName.toString(), since, nodeId.toString(), tenantId.toString());

            // save result
            context.setVariable(assertion.getVariablePrefix() + '.' + GetIncrementAssertion.SUFFIX_JSON, jsonStr);


        } catch (Exception ex) {
            final String errorMsg = "Error Retrieving Application Increment";
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{errorMsg + ": " + ExceptionUtils.getMessage(ex)}, ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    String getJsonMessage(final String connName, final Object since, final String nodeId, final String tenantId) throws IOException {
        ApplicationJson appJsonObj = new ApplicationJson();
        final long incrementStart = System.currentTimeMillis();
        appJsonObj.setIncrementStart(incrementStart);
        appJsonObj.setEntityType(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        boolean isApiPlansEnabled = isApiPlanEnabled(connName, tenantId);
        appJsonObj.setApiPlansEnabled(isApiPlansEnabled);

        Map<String, List> results;

        if (since != null) {
            appJsonObj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
            Set deletedAppIds = getDeletedAppIds(connName, since, nodeId, tenantId, isApiPlansEnabled, incrementStart);
            if (deletedAppIds.isEmpty()) {
                appJsonObj.setDeletedIds(new ArrayList<>());
            } else {
                appJsonObj.setDeletedIds(new ArrayList<>(deletedAppIds));
            }
            results = getAppsUpdatedWithinIncrement(connName, since, incrementStart, nodeId, tenantId,
                    isApiPlansEnabled);
        } else {
            appJsonObj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
            // bulk, get everything
            results = (Map<String, List>) queryJdbc(connName, String.format(isApiPlansEnabled ? BULK_SYNC_SELECT_WITH_API_PLANS : BULK_SYNC_SELECT, tenantId), Collections.emptyList());

            // do not include deleted list in json response
            appJsonObj.setDeletedIds(null);
        }

        appJsonObj.setNewOrUpdatedEntities(buildApplicationEntityList(results, connName, isApiPlansEnabled));

        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        String jsonInString;
        try {
            jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(appJsonObj);
        } catch (IOException ioe) {
            throw new IOException("Unable to write json string: " + ExceptionUtils.getMessage(ioe), ioe);
        }

        return jsonInString;
    }

    private Map<String, List> getAppsUpdatedWithinIncrement(String connName, Object since, long incrementStart,
                                                            String nodeId, String tenantId, boolean appsWithApiPlans) {
        LinkedList<String> columnsToSelect = getColumnsToSelectForAppsLookupQuery(appsWithApiPlans);
        List valuesForQueryArguments = getValuesForAppsLookupQuery(since, nodeId, appsWithApiPlans, incrementStart);
        // get new or updated or last sync error apps
        return (Map<String, List>) queryJdbc(
                connName,
                ServerIncrementalSyncCommon.getSyncUpdatedAppEntities(columnsToSelect, tenantId, appsWithApiPlans),
                valuesForQueryArguments);
    }

    private boolean isApiPlanEnabled(final String connName, final String tenantId)  {
        boolean isEnabled = false;
        Map<String, List> isApiPlanEnabledMap =  (Map<String, List>)  queryJdbc(connName,
                String.format(ServerIncrementalSyncCommon.API_PLAN_SETTING_ENABLE_STATUS, tenantId),
                new ArrayList<>());
        //Only one key returned
        Set<String> isEnabledMap = isApiPlanEnabledMap.keySet();
        if(isEnabledMap.isEmpty()) {
            //default as off
            return false;
        }
        final String SETTING_COL_NAME =  isEnabledMap.iterator().next();
        if(isApiPlanEnabledMap.containsKey(SETTING_COL_NAME) && !isApiPlanEnabledMap.get(SETTING_COL_NAME).isEmpty()) {
            isEnabled = Boolean.parseBoolean(isApiPlanEnabledMap.get(SETTING_COL_NAME).get(0).toString());
        }
        return isEnabled;
    }

    @NotNull
    private List getValuesForAppsLookupQuery(Object since, String nodeId, boolean appsWithApiPlans, long incrementStart) {
        List valuesForQueryArguments = Lists.newArrayList(since, incrementStart, since, incrementStart, since, incrementStart, nodeId);
        if (!appsWithApiPlans) {
            valuesForQueryArguments.addAll(CollectionUtils.list(since, incrementStart, since, incrementStart));
        }
        return valuesForQueryArguments;
    }

    @NotNull
    private LinkedList<String> getColumnsToSelectForAppsLookupQuery(boolean appsWithApiPlans) {
        LinkedList<String> columnsToSelect = new LinkedList<>(Lists.newArrayList("a.UUID", "concat(a.NAME, '-', o.NAME) as NAME", "a.API_KEY", "a.KEY_SECRET", "coalesce(r.PREVIOUS_STATE, a.STATUS) as STATUS", "a.ORGANIZATION_UUID", "o.NAME as ORGANIZATION_NAME", "a.OAUTH_CALLBACK_URL", "a.OAUTH_SCOPE", "a.OAUTH_TYPE", "a.MAG_SCOPE", "a.MAG_MASTER_KEY", "a.CREATED_BY", "a.MODIFIED_BY"));

        if (appsWithApiPlans) {
            columnsToSelect.add(0, "DISTINCT aaapx.API_UUID");
            columnsToSelect.add(1, "aaapx.API_PLAN_UUID");
        } else {
            columnsToSelect.add(0, "DISTINCT aaagx.API_UUID");
        }
        return columnsToSelect;
    }

    /**
     * Get deleted IDs - there are two parts to this:
     * 1. Include applications that are actually deleted by the user
     * 2. Include applications that have empty API groups (disabled API can be removed from a group even if it's used by an app) and no directly associated APIs
     * A new feature was introduced and the side effect of it is that an app is now possible to have zero API associated to it, which caused sync to break.
     * The fix is to treat apps with no API as "deleted" in the payload so that OTK will remove it from their db.
     * @param connName
     * @param since
     * @param nodeId
     * @param tenantId
     * @param appsWithApiPlans
     * @param incrementStart
     * @return
     */
    @NotNull
    private Set getDeletedAppIds(String connName, Object since, String nodeId, String tenantId, boolean
            appsWithApiPlans, long incrementStart) {
        Map<String, List> results;// Get deleted apps
        results = (Map<String, List>) queryJdbc(connName, ServerIncrementalSyncCommon.getSyncDeletedEntities(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION, tenantId), CollectionUtils.list(since, incrementStart));
        List<String> deletedIds = results.get("entity_uuid");

        // Merge the app uuids
        Set mergedAppUuids = new HashSet<String>();
        if (deletedIds != null && !deletedIds.isEmpty()) {
            mergedAppUuids.addAll(deletedIds);
        }

        //unnecessary query when API Plans is enabled as app -> API association is defined directly via the
        //APPLICATION_API_API_GROUP_XREF table (API_UUID)
        if(!appsWithApiPlans) {
            // Get apps that are not associated to any API, either directly or indirectly
            results = (Map<String, List>) queryJdbc(connName, ServerIncrementalSyncCommon.SELECT_APP_WITH_NO_API_SQL, CollectionUtils.list(tenantId, tenantId, nodeId, since, incrementStart, since, incrementStart, since, incrementStart, since, incrementStart));
            List<String> appIdsWithNoApi = results.get("uuid");

            if (appIdsWithNoApi != null && !appIdsWithNoApi.isEmpty()) {
                mergedAppUuids.addAll(appIdsWithNoApi);
            }
        }
        return mergedAppUuids;
    }

    private List buildApplicationEntityList(Map<String, List> results, String connName, boolean appsWithApiPlans) throws UnsupportedEncodingException {

        if (results.isEmpty()) {
            return CollectionUtils.list();
        }

        Map<String, ApplicationEntity> appMap = new HashMap<>();
        int elements = results.get("uuid").size();
        for (int i = 0; i < elements; i++) {
            String appId = (String) results.get("uuid").get(i);
            ApplicationEntity appEntity = appMap.get(appId);
            if (appEntity == null) {
                appEntity = new ApplicationEntity();
                appEntity.setId(appId);
                appEntity.setKey((String) results.get("api_key").get(i));
                appEntity.setSecret((String) results.get("key_secret").get(i));
                // Mapping the status, "ENABLED" -> "active", "DISABLED" -> "suspend"
                final String status = (String) results.get("status").get(i);
                appEntity.setStatus(status.equalsIgnoreCase(STATUS_ENABLED) ? STATUS_ACTIVE : STATUS_SUSPEND);
                appEntity.setOrganizationId((String) results.get("organization_uuid").get(i));
                appEntity.setOrganizationName((String) results.get("organization_name").get(i));
                appEntity.setLabel((String) results.get("name").get(i));
                appEntity.setOauthCallbackUrl((String) results.get("oauth_callback_url").get(i));
                String oauthScope = (String) results.get("oauth_scope").get(i);
                appEntity.setOauthScope(buildScope(oauthScope));
                appEntity.setOauthType((String) results.get("oauth_type").get(i));
                ApplicationMag mag = new ApplicationMag();
                mag.setScope(buildMagScope(oauthScope, (String) results.get("mag_scope").get(i)));
                mag.setRedirectUri(appEntity.getOauthCallbackUrl());
                mag.getMasterKeys().add(ImmutableMap.<String, String>builder()
                        .put("masterKey", (String) results.get("mag_master_key").get(i))
                        .put("environment", "all") // For now environment is hard-coded, in the future it will come from the database.
                        .build());
                appEntity.setMag(mag);
                appEntity.setCustom("");
                appEntity.setCreatedBy((String) results.get("created_by").get(i));
                appEntity.setModifiedBy((String) results.get("modified_by").get(i));
                appMap.put(appId, appEntity);
            }

            ApplicationApi api = new ApplicationApi();
            api.setId((String) results.get("api_uuid").get(i));
            if (appsWithApiPlans) {
                api.setPlanId((String) results.get("api_plan_uuid").get(i));
            }
            appEntity.getApis().add(api);
        }
        appendCustomFields(appMap, connName);
        return CollectionUtils.toListFromCollection(appMap.values());
    }


    private void appendCustomFields(Map<String, ApplicationEntity> values, String connName) {
        final String UUID_PARAM = "{{UUID_LIST}}";
        final String CF_QUERY = "SELECT ENTITY_UUID, SYSTEM_PROPERTY_NAME, VALUE \n" +
                "from CUSTOM_FIELD cf inner join CUSTOM_FIELD_VALUE cfv on cf.UUID = cfv.CUSTOM_FIELD_UUID \n" +
                "where ENTITY_UUID in (" + UUID_PARAM + ") AND cf.STATUS='ENABLED'";

        String app_uuids = "'"+StringUtils.join(((HashMap) values).keySet(), "','")+"'";

        if (!app_uuids.isEmpty()) {

            Map<String, List> results = (Map<String, List>) queryJdbc(connName, CF_QUERY.replace(UUID_PARAM, app_uuids), Collections.emptyList());

            if(results.size()>0) {
                int elements = results.get(FIELD_ENTITY_UUID).size();

                for (int i = 0; i < elements; i++) {
                    String entUuid = (String) results.get(FIELD_ENTITY_UUID).get(i);

                    ApplicationEntity applicationEntity = values.get(entUuid);
                    applicationEntity.getCustomFields().put((String) results.get(FIELD_SYSTEM_PROPERTY_NAME).get(i), (String) results.get(FIELD_VALUE).get(i));
                }
            }
        }

    }

    static String buildMagScope(String oauthScope, String mag_scope) {
        List<String> scopeList = new ArrayList();
        if(oauthScope!=null){
            scopeList.addAll(Arrays.asList(oauthScope.split("\\s")));
        }
        if(mag_scope!=null){
            scopeList.addAll(Arrays.asList(mag_scope.split("\\s")));
        }

        if(!scopeList.contains(APPLICATION_MAG_SCOPE_OPENID)){
            scopeList.add(APPLICATION_MAG_SCOPE_OPENID);
        }
        if(!scopeList.contains(APPLICATION_MAG_SCOPE_MSSO)){
            scopeList.add(APPLICATION_MAG_SCOPE_MSSO);
        }
        return buildScope(Joiner.on(" ").join(scopeList));
    }

    static String buildScope(String oauthScope) {
        if(oauthScope==null){
            return null;
        } else if(oauthScope.trim().equals(APPLICATION_SCOPE_OOB)){
            return APPLICATION_SCOPE_OOB;
        }else{
            Set<String> scopeSet = new HashSet();
            scopeSet.addAll(Arrays.asList(oauthScope.split("\\s")));

            if(Functions.exists(scopeSet, new Functions.Unary<Boolean, String>() {
                @Override
                public Boolean call(String s) {
                    return s.equals(APPLICATION_SCOPE_OOB);
                }})){
                scopeSet.removeIf(new Predicate<String>() {
                    @Override
                    public boolean test(String s) {
                        return s.equals(APPLICATION_SCOPE_OOB);
                    }
                });
            }
            return Joiner.on(" ").join(scopeSet);
        }
    }

    Object queryJdbc(String connName, String queryString, @NotNull List<Object> preparedStmtParams) {
        return jdbcQueryingManager.performJdbcQuery(connName, queryString, null, ServerIncrementalSyncCommon.getMaxRecords(), ServerIncrementalSyncCommon.getQueryTimeout(), preparedStmtParams);
    }
}
