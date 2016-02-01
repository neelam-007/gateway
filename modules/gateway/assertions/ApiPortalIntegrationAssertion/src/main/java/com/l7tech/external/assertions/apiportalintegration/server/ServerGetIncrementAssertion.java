package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationApi;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationEntity;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationJson;
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

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import javax.xml.bind.JAXBException;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;

/**
 * Server side implementation of the GetIncrementAssertion.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.GetIncrementAssertion
 */
public class ServerGetIncrementAssertion extends AbstractServerAssertion<GetIncrementAssertion> {
    private static final Logger logger = Logger.getLogger(ServerGetIncrementAssertion.class.getName());

    private final String[] variablesUsed;

    // todo make configurable? cluster prop? use default jdbc?
    private int queryTimeout = 300;
    private int maxRecords = 1000000;

    private static final String ENTITY_TYPE_APPLICATION = "APPLICATION";

    private final JdbcQueryingManager jdbcQueryingManager;
    private final JdbcConnectionManager jdbcConnectionManager;

    public ServerGetIncrementAssertion(GetIncrementAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
        jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
    }

    /*
     * For tests.
     */
    ServerGetIncrementAssertion(GetIncrementAssertion assertion, JdbcQueryingManager jdbcQueryingManager, JdbcConnectionManager jdbcConnectionManager) throws PolicyAssertionException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        this.jdbcQueryingManager = jdbcQueryingManager;
        this.jdbcConnectionManager = jdbcConnectionManager;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());

            Object entityType = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_TYPE);
            Object jdbcConnectionName = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_JDBC_CONNECTION);
            Object sinceStr = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_SINCE);
            Object nodeId = vars.get(assertion.getVariablePrefix() + "." + GetIncrementAssertion.SUFFIX_NODE_ID);

            // validate inputs
            if (entityType == null) {
                throw new PolicyAssertionException(assertion, "Assertion must supply an entity type");
            }
            if (!entityType.equals(ENTITY_TYPE_APPLICATION)) {
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
            String jsonStr = getJsonMessage(jdbcConnectionName.toString(), since, nodeId.toString());

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

    String getJsonMessage(final String connName, final Object since, final String nodeId) throws IOException {
        ApplicationJson appJsonObj = new ApplicationJson();
        final long incrementStart = System.currentTimeMillis();
        appJsonObj.setIncrementStart(incrementStart);
        appJsonObj.setEntityType(ENTITY_TYPE_APPLICATION);

        Map<String, List> results;

        if (since != null) {
            // get deleted IDs
            results = (Map<String, List>) queryJdbc(connName, "SELECT ENTITY_UUID FROM DELETED_ENTITY WHERE TYPE = 'APPLICATION' AND DELETED_TS > ? AND DELETED_TS <= ?", CollectionUtils.list(since, incrementStart));
            List<String> deletedIds = results.get("entity_uuid");
            if (deletedIds == null || deletedIds.isEmpty()) {
                // do not include deleted list in json response
                appJsonObj.setDeletedIds(null);
            } else {
                appJsonObj.setDeletedIds(deletedIds);
            }

            // get new or updated or last sync error apps
            results = (Map<String, List>) queryJdbc(connName,
                    "SELECT a.UUID, a.NAME, a.API_KEY, a.KEY_SECRET, a.STATUS, a.ORGANIZATION_UUID, o.NAME as ORGANIZATION_NAME, a.OAUTH_CALLBACK_URL, a.OAUTH_SCOPE, a.OAUTH_TYPE, ax.API_UUID \n" +
                            "FROM APPLICATION a  \n" +
                            "\tJOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID \n" +
                            "\tJOIN APPLICATION_API_XREF ax on ax.APPLICATION_UUID = a.UUID\n" +
                            "\tLEFT JOIN APPLICATION_TENANT_GATEWAY t on t.APPLICATION_UUID = a.UUID \n" +
                            "WHERE a.API_KEY IS NOT NULL AND a.STATUS IN ('ENABLED','DISABLED') AND ( (a.MODIFY_TS > ? and a.MODIFY_TS <=  ? ) OR (a.MODIFY_TS =0 and a.CREATE_TS > ? and  a.CREATE_TS <=  ?) OR (t.TENANT_GATEWAY_UUID = ? AND t.SYNC_LOG IS NOT NULL))", CollectionUtils.list(since, incrementStart, since, incrementStart, nodeId));

        } else {
            // bulk, get everything
            results = (Map<String, List>) queryJdbc(connName,
                    "SELECT a.UUID, a.NAME, a.API_KEY, a.KEY_SECRET, a.STATUS, a.ORGANIZATION_UUID, o.NAME as ORGANIZATION_NAME, a.OAUTH_CALLBACK_URL, a.OAUTH_SCOPE, a.OAUTH_TYPE, ax.API_UUID \n" +
                            "FROM APPLICATION a  \n" +
                            "\tJOIN ORGANIZATION o on a.ORGANIZATION_UUID = o.UUID \n" +
                            "\tJOIN APPLICATION_API_XREF ax on ax.APPLICATION_UUID = a.UUID\n" +
                            "WHERE a.API_KEY IS NOT NULL AND a.STATUS IN ('ENABLED','DISABLED')", Collections.EMPTY_LIST);

            // do not include deleted list in json response
            appJsonObj.setDeletedIds(null);
        }

        appJsonObj.setNewOrUpdatedEntities(buildApplicationEntityList(results));

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

    private List buildApplicationEntityList(Map<String, List> results) {

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
                appEntity.setStatus((String) results.get("status").get(i));
                appEntity.setOrganizationId((String) results.get("organization_uuid").get(i));
                appEntity.setOrganizationName((String) results.get("organization_name").get(i));
                appEntity.setLabel((String) results.get("name").get(i));
                appEntity.setOauthCallbackUrl((String) results.get("oauth_callback_url").get(i));
                appEntity.setOauthScope((String) results.get("oauth_scope").get(i));
                appEntity.setOauthType((String) results.get("oauth_type").get(i));
                appMap.put(appId, appEntity);
            }

            ApplicationApi api = new ApplicationApi();
            api.setId((String) results.get("api_uuid").get(i));
            appEntity.getApis().add(api);
        }
        return CollectionUtils.toListFromCollection(appMap.values());
    }

    Object queryJdbc(String connName, String queryString, @NotNull List<Object> preparedStmtParams) {
        return jdbcQueryingManager.performJdbcQuery(connName, queryString, null, maxRecords, queryTimeout, preparedStmtParams);
    }
}
