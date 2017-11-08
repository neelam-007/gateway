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

    // Query to get all APIs added directly to the application and indirectly added through API groups
    private static final String BULK_SYNC_SELECT =
            "            SELECT a3.UUID, concat(a3.NAME,'-',o.NAME) as NAME, a3.API_KEY, a3.KEY_SECRET, \n" +
            "                   coalesce (r.PREVIOUS_STATE, a3.STATUS) as STATUS, a3.ORGANIZATION_UUID, o.NAME as ORGANIZATION_NAME, \n" +
            "                   a3.OAUTH_CALLBACK_URL, a3.OAUTH_SCOPE, a3.OAUTH_TYPE, a3.MAG_SCOPE, a3.MAG_MASTER_KEY, \n" +
            "                   a3.API_UUID, a3.CREATED_BY, a3.MODIFIED_BY, r.LATEST_REQ \n" +
            "            FROM (SELECT a.UUID, a.NAME, a.API_KEY, a.KEY_SECFRET, a.STATUS, a.ORGANIZATION_UUID, \n" +
            "                         a.OAUTH_CALLBACK_URL, a.OAUTH_SCOPE, a.OAUTH_TYPE, a.MAG_SCOPE, a.MAG_MASTER_KEY, \n" +
            "                         ax.API_UUID, a.CREATED_BY, a.MODIFIED_BY, a.TENANT_ID \n" +
            "                  FROM APPLICATION a \n" +
            "                  JOIN APPLICATION_API_XREF ax on ax.APPLICATION_UUID = a.UUID \n" +
            "                  UNION \n" +
            "                  SELECT a2.UUID, a2.NAME, a2.API_KEY, a2.KEY_SECRET, a2.STATUS, a2.ORGANIZATION_UUID, \n" +
            "                         a2.OAUTH_CALLBACK_URL, a2.OAUTH_SCOPE, a2.OAUTH_TYPE, a2.MAG_SCOPE, a2.MAG_MASTER_KEY, \n" +
            "                         agax.API_UUID, a2.CREATED_BY, a2.MODIFIED_BY, a2.TENANT_ID \n" +
            "                  FROM APPLICATION_API_GROUP_XREF aagx \n" +
            "                  JOIN APPLICATION a2 ON a2.UUID = aagx.APPLICATION_UUID \n" +
            "                  JOIN  API_GROUP_API_XREF agax ON agax.API_GROUP_UUID = aagx.API_GROUP_UUID \n" +
            "                  JOIN ORGANIZATION_API_VIEW oav ON oav.UUID = agax.API_UUID \n" +
            "                  WHERE (oav.ACCESS_STATUS = 'PUBLIC' OR (oav.ACCESS_STATUS = 'PRIVATE' AND oav.ORGANIZATION_UUID = a2.ORGANIZATION_UUID))) a3 \n" +
            "            JOIN ORGANIZATION o on a3.ORGANIZATION_UUID = o.UUID \n" +
            "            LEFT JOIN (select ENTITY_UUID, PREVIOUS_STATE, max(CREATE_TS) as LATEST_REQ FROM REQUEST GROUP BY ENTITY_UUID, PREVIOUS_STATE, CREATE_TS) r ON a3.UUID = r.ENTITY_UUID \n" +
            "            WHERE a3.API_KEY IS NOT NULL AND a3.TENANT_ID='%s' AND a3.STATUS IN ('ENABLED', 'DISABLED', 'EDIT_APPLICATION_PENDING_APPROVAL')";

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

        Map<String, List> results;

        if (since != null) {
            appJsonObj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_FALSE);
            // get deleted IDs
            results = (Map<String, List>) queryJdbc(connName, ServerIncrementalSyncCommon.getSyncDeletedEntities(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION, tenantId), CollectionUtils.list(since, incrementStart));
            List<String> deletedIds = results.get("entity_uuid");
            if (deletedIds == null || deletedIds.isEmpty()) {
                appJsonObj.setDeletedIds(new ArrayList<String>());
            } else {
                appJsonObj.setDeletedIds(deletedIds);
            }

            // get new or updated or last sync error apps
            results = (Map<String, List>) queryJdbc(
                    connName,
                    ServerIncrementalSyncCommon.getSyncUpdatedAppEntities(
                            Lists.newArrayList("a3.UUID", "concat(a3.NAME, '-', o.NAME) as NAME", "a3.API_KEY", "a3.KEY_SECRET", "coalesce(r.PREVIOUS_STATE, a3.STATUS) as STATUS", "a3.ORGANIZATION_UUID", "o.NAME as ORGANIZATION_NAME", "a3.OAUTH_CALLBACK_URL", "a3.OAUTH_SCOPE", "a3.OAUTH_TYPE", "a3.MAG_SCOPE", "a3.MAG_MASTER_KEY", "a3.API_UUID", "a3.CREATED_BY", "a3.MODIFIED_BY"),
                            tenantId),
                    CollectionUtils.list(since, incrementStart, since, incrementStart, since, incrementStart, since, incrementStart, nodeId));
        } else {
            appJsonObj.setBulkSync(ServerIncrementalSyncCommon.BULK_SYNC_TRUE);
            // bulk, get everything
            results = (Map<String, List>) queryJdbc(connName, String.format(BULK_SYNC_SELECT, tenantId), Collections.EMPTY_LIST);

            // do not include deleted list in json response
            appJsonObj.setDeletedIds(null);
        }

        appJsonObj.setNewOrUpdatedEntities(buildApplicationEntityList(results, connName));

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

    private List buildApplicationEntityList(Map<String, List> results, String connName) throws UnsupportedEncodingException {

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

      Map<String, List> results = (Map<String, List>) queryJdbc(connName, CF_QUERY.replace(UUID_PARAM, app_uuids), Collections.EMPTY_LIST);

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
