package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.GetApiIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.*;
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
  private Object jdbcConnectionName = null;

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
      try {
        connection = jdbcConnectionManager.getJdbcConnection(jdbcConnectionName.toString());
        if (connection == null) throw new FindException();
      } catch (FindException e) {
        logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Could not find JDBC connection: " + jdbcConnectionName);
        return AssertionStatus.FAILED;
      }

      // create result
      String jsonStr = getJsonMessage(nodeId.toString(), tenantId.toString());

      // save result
      context.setVariable(assertion.getVariablePrefix() + '.' + GetApiIncrementAssertion.SUFFIX_JSON, jsonStr);


    } catch (Exception ex) {
      final String errorMsg = "Error Retrieving Application Increment";
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
    ApiV2BulkJson apiV2BulkJson = new ApiV2BulkJson();
    apiV2BulkJson.setApis(getApis(nodeId, tenantId));

    ObjectMapper mapper = new ObjectMapper();
    mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    String jsonInString;
    try {
      jsonInString = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(apiV2BulkJson);
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
  private List<ApiV2Entity> getApis(String nodeId, String tenantId) {
    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
        "SELECT \n" +
            "    a.`UUID` AS `UUID`,\n" +
            "    a.`NAME` AS `NAME`,\n" +
            "    a.`PORTAL_STATUS` AS `PORTAL_STATUS`,\n" +
            "    atg.`API_LOCATION_URL` AS `API_LOCATION_URL`,\n" +
            "    a.`PUBLISHED_BY_PORTAL` AS `PUBLISHED_BY_PORTAL`,\n" +
            "    a.`SSG_URL` AS `SSG_URL`\n" +
            "FROM\n" +
            "   `API` a LEFT JOIN `API_TENANT_GATEWAY` atg on atg.`API_UUID` = a.`UUID`\n" +
            "WHERE a.`TENANT_ID` = ? and atg.`TENANT_GATEWAY_UUID` = ?",
              CollectionUtils.list(tenantId, nodeId));

    if (results.isEmpty()) {
      return CollectionUtils.list();
    }

    logger.log(Level.INFO, "Fetched " + results.size() + " V2 api from database");
    Map<String, ApiV2Entity> apiV2EntityMap = buildApiEntities(results);

    String apiUuids = "'" + StringUtils.join(apiV2EntityMap.keySet(), "','") + "'";
    if (!apiUuids.isEmpty()) {
      buildCustomFields(apiV2EntityMap, apiUuids);
      buildPolicyEntities(apiV2EntityMap, apiUuids);
    }

    return CollectionUtils.toListFromCollection(apiV2EntityMap.values());
  }

  /**
   * Populate Map of Api v2 from database result
   * @param results
   * @return
   */
  private Map<String, ApiV2Entity> buildApiEntities(Map<String, List> results) {
    Map<String, ApiV2Entity> apiV2EntityMap = new HashMap<>();
    int elements = results.get("uuid").size();
    ApiV2Entity apiV2Entity = null;
    for (int i = 0; i < elements; i++) {
      apiV2Entity = new ApiV2Entity();
      String uuid = (String) results.get("uuid").get(i);
      apiV2Entity.setUuid(uuid);
      apiV2Entity.setName((String) results.get("name").get(i));
      apiV2Entity.setServiceEnabled(isServiceEnabled((String) results.get("portal_status").get(i)));
      apiV2Entity.setPublishedByPortal((Boolean) results.get("published_by_portal").get(i));
      apiV2Entity.setApiLocationUrl((String) results.get("api_location_url").get(i));
      apiV2Entity.setSsgUrl((String) results.get("ssg_url").get(i));
      apiV2EntityMap.put(uuid, apiV2Entity);
    }

    logger.log(Level.INFO, "Build api v2 map");

    return apiV2EntityMap;
  }

  /**
   * Fetch custom fields of given api uuids and set it to api v2 map
   * @param apiV2EntityMap
   * @param apiUuids
   */
  private void buildCustomFields(Map<String, ApiV2Entity> apiV2EntityMap, String apiUuids) {
    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
        "SELECT\n" +
                  "   `NAME`,\n" +
                  "   `ENTITY_UUID`,\n" +
                  "   `VALUE`\n" +
                  "FROM `CUSTOM_FIELD_VALUE_VIEW` \n" +
                  "WHERE `ENTITY_UUID` in (" + apiUuids + ")", Collections.EMPTY_LIST);

    logger.log(Level.INFO, "Fetched " + results.size() + " Custom Fields from database");

    if (results.size() > 0) {
      int elements = results.get("name").size();
      ApiV2Entity apiV2Entity = null;
      CustomFieldValueEntity customFieldValueEntity;
      for (int i = 0; i < elements; i++) {
        customFieldValueEntity = new CustomFieldValueEntity();
        String apiUuid = (String) results.get("entity_uuid").get(i);
        customFieldValueEntity.setValue((String) results.get("value").get(i));
        customFieldValueEntity.setName((String) results.get("name").get(i));
        apiV2Entity = apiV2EntityMap.get(apiUuid);
        apiV2Entity.getCustomFieldValueEntities().add(customFieldValueEntity);
      }
      logger.log(Level.INFO, "Linked custom fields with their respective api v2 objects");
    }
  }

  /**
   * Fetch Policy Entities of given api uuids and set it to api v2 map
   * @param apiV2EntityMap
   * @param apiUuids
   */
  private void buildPolicyEntities(Map<String, ApiV2Entity> apiV2EntityMap, String apiUuids) {

    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
        "SELECT\n" +
                  "   `UUID`,\n" +
                  "   `API_UUID`,\n" +
                  "   `POLICY_ENTITY_UUID`\n" +
                  "FROM `API_POLICY_ENTITY_XREF` \n" +
                  "WHERE `API_UUID` in (" + apiUuids + ")", Collections.EMPTY_LIST);

    logger.log(Level.INFO, "Fetched " + results.size() + " Policy entities from database");

    if (results.size() > 0) {
      int elements = results.get("uuid").size();
      PolicyEntity policyEntity;
      Map<String, PolicyEntity> policyEntityMap = new HashMap<>();
      ApiV2Entity apiV2Entity;
      for (int i = 0; i < elements; i++) {
        policyEntity = new PolicyEntity();
        policyEntity.setPolicyEntityUuid((String) results.get("policy_entity_uuid").get(i));
        policyEntityMap.put((String) results.get("uuid").get(i), policyEntity);

        apiV2Entity = apiV2EntityMap.get((String) results.get("api_uuid").get(i));
        apiV2Entity.getPolicyEntities().add(policyEntity);
      }

      logger.log(Level.INFO, "Linked policy entities with their respective api v2 object");

      buildPolicyTemplateArguments(policyEntityMap, apiUuids);
    }
  }

  /**
   * Fetch Policy template Arguments of given api uuids and set it to policy entities map
   * @param policyEntityMap
   * @param apiUuids
   */
  private void buildPolicyTemplateArguments(Map<String, PolicyEntity> policyEntityMap, String apiUuids) {
    Map<String, List> results = (Map<String, List>) queryJdbc(jdbcConnectionName.toString(),
        "SELECT\n" +
                  "   `API_POLICY_ENTITY_XREF_UUID`,\n" +
                  "   `NAME`,\n" +
                  "   `VALUE`\n" +
                  "FROM `API_POLICY_TEMPLATE_XREF_VIEW`\n" +
                  "WHERE `API_UUID` in (" + apiUuids + ")", Collections.EMPTY_LIST);

    logger.log(Level.INFO, "Fetched " + results.size() + " Policy template Arguments from database");

    if (results.size() > 0) {
      int elements = results.get("name").size();
      PolicyTemplateArgument templateArgument;
      PolicyEntity  policyEntity;
      for (int i = 0; i < elements; i++) {
        templateArgument = new PolicyTemplateArgument();
        templateArgument.setName((String) results.get("name").get(i));
        templateArgument.setValue((String) results.get("value").get(i));
        policyEntity = policyEntityMap.get((String) results.get("api_policy_entity_xref_uuid").get(i));
        policyEntity.getPolicyTemplateArguments().add(templateArgument);
      }
      logger.log(Level.INFO, "Linked policy template arguments with their respective policy entities");
    }
  }

  private boolean isServiceEnabled(String status) {
    return (status.equals("ENABLED") || status.equals("DEPRECATED"));
  }

  Object queryJdbc(String connName, String queryString, @NotNull List<Object> preparedStmtParams) {
    return jdbcQueryingManager.performJdbcQuery(connName, queryString, null, ServerIncrementalSyncCommon.getMaxRecords(), ServerIncrementalSyncCommon.getQueryTimeout(), preparedStmtParams);
  }
}
