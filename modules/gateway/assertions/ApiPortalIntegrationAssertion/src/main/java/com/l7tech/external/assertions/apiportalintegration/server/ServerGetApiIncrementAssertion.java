package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.GetApiIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the GetApiIncrementAssertion.
 *
 * @see GetApiIncrementAssertion
 */
public class ServerGetApiIncrementAssertion extends AbstractServerAssertion<GetApiIncrementAssertion> {
  private static final Logger logger = Logger.getLogger(ServerGetApiIncrementAssertion.class.getName());

  private final String[] variablesUsed;
  private final JdbcQueryingManager jdbcQueryingManager;
  private final JdbcConnectionManager jdbcConnectionManager;
  protected Object jdbcConnectionName = null;

  public ServerGetApiIncrementAssertion(GetApiIncrementAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
    super(assertion);
    this.variablesUsed = assertion.getVariablesUsed();
    jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
    jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
  }

  public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
    try {
      Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

      Object entityType = vars.get(assertion.getVariablePrefix() + "." + GetApiIncrementAssertion.SUFFIX_TYPE);
      jdbcConnectionName = vars.get(assertion.getVariablePrefix() + "." + GetApiIncrementAssertion.SUFFIX_JDBC_CONNECTION);
      Object nodeId = vars.get(assertion.getVariablePrefix() + "." + GetApiIncrementAssertion.SUFFIX_NODE_ID);
      Object tenantId = vars.get(assertion.getVariablePrefix() + "." + GetApiIncrementAssertion.SUFFIX_TENANT_ID);

      // validate inputs
      if (entityType == null) {
        throw new PolicyAssertionException(assertion, "Assertion must supply an entity type");
      }
      if (!entityType.equals(ServerIncrementalSyncCommon.ENTITY_TYPE_API)) {
        throw new PolicyAssertionException(assertion, "Not supported entity type: " + entityType);
      }

      // validate inputs
      if (nodeId == null) {
        throw new PolicyAssertionException(assertion, "Assertion must supply a node id");
      }

      if (jdbcConnectionName == null) {
        throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
      }

      if (tenantId == null) {
        throw new PolicyAssertionException(assertion, "Assertion must supply a valid tenant prefix");
      }

      // validate that the connection exists.
      final JdbcConnection connection;
      connection = jdbcConnectionManager.getJdbcConnection(jdbcConnectionName.toString());
      if (connection == null) {
        logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Could not find JDBC connection: " + jdbcConnectionName);
        return AssertionStatus.FAILED;
      }

      // create result
      String jsonStr = getJsonMessage(nodeId.toString(), tenantId.toString());

      // save result
      context.setVariable(assertion.getVariablePrefix() + '.' + GetApiIncrementAssertion.SUFFIX_JSON, jsonStr);


    } catch (Exception ex) {
      final String errorMsg = "Error Retrieving Api V2 for Sync";
      logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
          new String[]{errorMsg + ": " + ExceptionUtils.getMessage(ex)}, ExceptionUtils.getDebugException(ex));
      return AssertionStatus.FAILED;
    }
    return AssertionStatus.NONE;
  }

  /**
   * get all apis based on tenant and node id and then make json from it
   * @param nodeId
   * @param tenantId
   * @return Json string
   * @throws IOException
   */
  String getJsonMessage(final String nodeId, final String tenantId) throws IOException {
    ApiBulkJson apiBulkJson = new ApiBulkJson();
    apiBulkJson.setApis(getApis(nodeId, tenantId));

    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    String jsonInString;
    try {
      jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiBulkJson);
    } catch (IOException ioe) {
      throw new IOException("Unable to write json string: " + ExceptionUtils.getMessage(ioe), ioe);
    }

    return jsonInString;
  }

  /**
   * Return list of apis in v2 format
   * @param nodeId
   * @param tenantId
   * @return
   */
  private List<ApiEntity> getApis(String nodeId, String tenantId) {
    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
           "SELECT \n" +
            "    a.UUID AS UUID,\n" +
            "    a.NAME AS NAME,\n" +
            "    a.PORTAL_STATUS AS PORTAL_STATUS,\n" +
            "    atg.API_LOCATION_URL AS API_LOCATION_URL,\n" +
            "    a.PUBLISHED_BY_PORTAL AS PUBLISHED_BY_PORTAL,\n" +
            "    a.SSG_URL AS SSG_URL,\n" +
            "    a.MODIFY_TS AS MODIFY_TS\n" +
            "FROM\n" +
            "   API a LEFT JOIN API_TENANT_GATEWAY atg on atg.API_UUID = a.UUID and atg.TENANT_ID = a.TENANT_ID \n" +
            "WHERE a.TENANT_ID = ? and atg.TENANT_GATEWAY_UUID = ? and a.type != 'ADVANCED'",
        CollectionUtils.list(tenantId, nodeId));

    if (results.isEmpty()) {
      return CollectionUtils.list();
    }

    logger.log(Level.INFO, "Fetched " + results.size() + " V2 api from database");
    Map<String, ApiEntity> apiV2EntityMap = buildApiEntities(results);

    if (apiV2EntityMap.size() > 0) {
      String apiUuids = "'" + StringUtils.join(apiV2EntityMap.keySet(), "','") + "'";
      buildCustomFields(apiV2EntityMap, apiUuids, tenantId);
      buildPolicyEntities(apiV2EntityMap, apiUuids, tenantId);
    }

    return CollectionUtils.toListFromCollection(apiV2EntityMap.values());
  }

  /**
   * Populate Map of Api v2 from database result
   * @param results
   * @return
   */
  private Map<String, ApiEntity> buildApiEntities(Map<String, List> results) {
    Map<String, ApiEntity> apiV2EntityMap = new HashMap<>();
    int size = results.get("uuid").size();
    ApiEntity apiEntity;
    for (int i = 0; i < size; i++) {
      apiEntity = new ApiEntity();
      String uuid = (String) results.get("uuid").get(i);
      apiEntity.setUuid(uuid);
      apiEntity.setName((String) results.get("name").get(i));
      apiEntity.setServiceEnabled(isServiceEnabled((String) results.get("portal_status").get(i)));
      apiEntity.setPortalPublished((Boolean) results.get("published_by_portal").get(i));
      apiEntity.setApiLocationUrl((String) results.get("api_location_url").get(i));
      apiEntity.setSsgUrl((String) results.get("ssg_url").get(i));
      apiEntity.setPortalModifyTS((Long) results.get("modify_ts").get(i));
      apiV2EntityMap.put(uuid, apiEntity);
    }

    logger.log(Level.FINE, "Build api v2 map");

    return apiV2EntityMap;
  }

  /**
   * Fetch custom fields of given api uuids and set it to api v2 map
   * @param apiV2EntityMap
   * @param apiUuids
   */
  private void buildCustomFields(Map<String, ApiEntity> apiV2EntityMap, String apiUuids, String tenantId) {
    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
           "SELECT\n" +
            "   NAME,\n" +
            "   ENTITY_UUID,\n" +
            "   VALUE\n" +
            "FROM CUSTOM_FIELD_VALUE_VIEW \n" +
            "WHERE ENTITY_UUID in (" + apiUuids + ") AND TENANT_ID = ?", CollectionUtils.list(tenantId));
    logger.log(Level.FINE, "Fetched " + results.size() + " Custom Fields from database");

    if (results.size() > 0) {
      int size = results.get("name").size();
      ApiEntity apiEntity = null;
      CustomFieldValueEntity customFieldValueEntity;
      for (int i = 0; i < size; i++) {
        customFieldValueEntity = new CustomFieldValueEntity();
        String apiUuid = (String) results.get("entity_uuid").get(i);
        customFieldValueEntity.setValue((String) results.get("value").get(i));
        customFieldValueEntity.setName((String) results.get("name").get(i));
        apiEntity = apiV2EntityMap.get(apiUuid);
        apiEntity.getCustomFieldValueEntities().add(customFieldValueEntity);
      }
      logger.log(Level.FINE, "Linked custom fields with their respective api v2 objects");
    }
  }

  /**
   * Fetch Policy Entities of given api uuids and set it to api v2 map
   * @param apiV2EntityMap
   * @param apiUuids
   */
  private void buildPolicyEntities(Map<String, ApiEntity> apiV2EntityMap, String apiUuids, String tenantId) {

    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
           "SELECT\n" +
            "   UUID,\n" +
            "   API_UUID,\n" +
            "   POLICY_ENTITY_UUID\n" +
            "FROM API_POLICY_ENTITY_XREF \n" +
            "WHERE API_UUID in (" + apiUuids + ") AND TENANT_ID = ?" +
               " ORDER BY ORDINAL ASC", CollectionUtils.list(tenantId));
    logger.log(Level.FINE, "Fetched " + results.size() + " Policy entities from database");

    if (results.size() > 0) {
      int size = results.get("uuid").size();
      PolicyEntity policyEntity;
      Map<String, PolicyEntity> policyEntityMap = new HashMap<>();
      ApiEntity apiEntity;
      for (int i = 0; i < size; i++) {
        policyEntity = new PolicyEntity();
        policyEntity.setPolicyEntityUuid((String) results.get("policy_entity_uuid").get(i));
        policyEntityMap.put((String) results.get("uuid").get(i), policyEntity);

        apiEntity = apiV2EntityMap.get((String) results.get("api_uuid").get(i));
        apiEntity.getPolicyEntities().add(policyEntity);
      }

      logger.log(Level.FINE, "Linked policy entities with their respective api v2 object");

      buildPolicyTemplateArguments(policyEntityMap, apiUuids, tenantId);
    }
  }

  /**
   * Fetch Policy template Arguments of given api uuids and set it to policy entities map
   * @param policyEntityMap
   * @param apiUuids
   */
  private void buildPolicyTemplateArguments(Map<String, PolicyEntity> policyEntityMap, String apiUuids, String tenantId) {
    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
           "SELECT\n" +
            "   API_POLICY_ENTITY_XREF_UUID,\n" +
            "   NAME,\n" +
            "   VALUE\n" +
            "FROM API_POLICY_TEMPLATE_XREF_VIEW\n" +
            "WHERE API_UUID in (" + apiUuids + ") AND TENANT_ID = ?", CollectionUtils.list(tenantId));
    logger.log(Level.FINE, "Fetched " + results.size() + " Policy template Arguments from database");

    if (results.size() > 0) {
      int size = results.get("name").size();
      PolicyTemplateArgument templateArgument;
      PolicyEntity policyEntity;
      for (int i = 0; i < size; i++) {
        templateArgument = new PolicyTemplateArgument();
        templateArgument.setName((String) results.get("name").get(i));
        templateArgument.setValue((String) results.get("value").get(i));
        policyEntity = policyEntityMap.get((String) results.get("api_policy_entity_xref_uuid").get(i));
        policyEntity.getPolicyTemplateArguments().add(templateArgument);
      }
      logger.log(Level.FINE, "Linked policy template arguments with their respective policy entities");
    }
  }

  private boolean isServiceEnabled(String status) {
    return (status.equals("ENABLED") || status.equals("DEPRECATED"));
  }

  Object queryJdbc(String connName, String queryString, @NotNull List<Object> preparedStmtParams) {
    return jdbcQueryingManager.performJdbcQuery(connName, queryString, null, ServerIncrementalSyncCommon.getMaxRecords(), ServerIncrementalSyncCommon.getQueryTimeout(), preparedStmtParams);
  }
}
