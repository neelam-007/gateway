package com.l7tech.external.assertions.apiportalintegration.server;

import com.google.common.base.Function;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.l7tech.external.assertions.apiportalintegration.IncrementPostBackAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.resource.ApplicationJson;
import com.l7tech.external.assertions.apiportalintegration.server.resource.PortalSyncPostbackJson;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.jdbc.JdbcConnectionPoolManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.logging.Logger;

/**
 * Server side implementation of the IncrementPostBackAssertion.
 * This assertion should be included in PSSG policy to handle postback message
 * from TSSG.
 *
 * @see com.l7tech.external.assertions.apiportalintegration.IncrementPostBackAssertion
 */
public class ServerIncrementPostBackAssertion extends AbstractServerAssertion<IncrementPostBackAssertion> {
    private static final Logger logger = Logger.getLogger(ServerIncrementPostBackAssertion.class.getName());

    private final String[] variablesUsed;

    protected final static String SYNC_STATUS_PARTIAL = PortalSyncPostbackJson.SYNC_STATUS_PARTIAL;
    protected final static String SYNC_STATUS_ERROR = PortalSyncPostbackJson.SYNC_STATUS_ERROR;
    protected final static String ERROR_ENTITY_ID_LABEL = PortalSyncPostbackJson.ERROR_ENTITY_ID_LABEL;
    protected final static String ERROR_ENTITY_MSG_LABEL = PortalSyncPostbackJson.ERROR_ENTITY_MSG_LABEL;


    private final JdbcQueryingManager jdbcQueryingManager;
    private PlatformTransactionManager transactionManager;
    private final JdbcConnectionPoolManager jdbcConnectionPoolManager;
    private DataSource dataSource;

    public ServerIncrementPostBackAssertion(IncrementPostBackAssertion assertion, ApplicationContext context) throws PolicyAssertionException, JAXBException {
        super(assertion);
        this.variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
        jdbcConnectionPoolManager = context.getBean("jdbcConnectionPoolManager", JdbcConnectionPoolManager.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            Map<String, Object> vars = context.getVariableMap(this.variablesUsed, getAudit());
            Object jsonPayload = vars.get(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JSON);
            final Object jdbcConnectionName = vars.get(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_JDBC_CONNECTION);
            final Object nodeId = vars.get(assertion.getVariablePrefix() + "." + IncrementPostBackAssertion.SUFFIX_NODE_ID);
            //validate inputs
            if (jsonPayload == null || !(jsonPayload instanceof String) || ((String) jsonPayload).isEmpty()) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a sync postback message");
            }
            //validate inputs
            if (!(nodeId instanceof String) || Strings.isNullOrEmpty((String) nodeId)) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a node id");
            }
            if (!(jdbcConnectionName instanceof String) || Strings.isNullOrEmpty((String) jdbcConnectionName)) {
                throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
            }
            try {
                dataSource = jdbcConnectionPoolManager.getDataSource(jdbcConnectionName.toString());
                if (dataSource == null) throw new FindException();
                // This update/insert is not using HibernateTransactionManager.  Instead using the transactionManager bean,
                // use DataSourceTransactionManager to create transaction for the following sql statements.
                transactionManager = transactionManager == null ? new DataSourceTransactionManager(dataSource) : transactionManager;
            } catch (FindException e) {
                logAndAudit(AssertionMessages.EXCEPTION_WARNING, "Could not find JDBC connection: " + jdbcConnectionName);
                return AssertionStatus.FAILED;
            }
            JsonFactory jsonFactory = new JsonFactory();
            JsonParser jsonParser = jsonFactory.createJsonParser((String) jsonPayload);
            ObjectMapper mapper = new ObjectMapper();
            JsonToken current = jsonParser.nextToken();
            if (current != JsonToken.START_OBJECT) {
                throw new IOException("Invalid JSON input");
            }
            PortalSyncPostbackJson postback = mapper.readValue(jsonParser, PortalSyncPostbackJson.class);
            postback.validate();
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
            TransactionStatus status = transactionManager.getTransaction(def);
            if (postback.getEntityType().equalsIgnoreCase(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION)) {
                try {
                    handleApplicationSyncPostback(jdbcConnectionName.toString(), postback, nodeId.toString());
                    transactionManager.commit(status);
                } catch (PolicyAssertionException e) {
                    transactionManager.rollback(status);
                    throw e;
                }
            } else {
                throw new PolicyAssertionException(assertion, "Not supported entity type:" + postback.getEntityType());
            }
        } catch (Exception ex) {
            final String errorMsg = "Error handling sync postBack message";
            logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO,
                    new String[]{errorMsg + ": " + ExceptionUtils.getMessage(ex)}, ExceptionUtils.getDebugException(ex));
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }


    /**
     * handle application sync postback message
     */
    void handleApplicationSyncPostback(String jdbcConnectionName, PortalSyncPostbackJson postback, String nodeId) throws PolicyAssertionException {
        final String TABLE_NAME = "APPLICATION_TENANT_GATEWAY";
        final String UUID_FIELD_NAME = "APPLICATION_UUID";

        final String TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME = "APP_SYNC_TIME";
        final String TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME = "APP_SYNC_LOG";
        postback.setIncrementStart(postback.getBulkSync().equalsIgnoreCase(ApplicationJson.BULK_SYNC_TRUE) ? 0 : postback.getIncrementStart());

        //update last sync time column of TENANT_GATEWAY
        updateTenantSyncStatus(jdbcConnectionName, nodeId, postback, TENANT_GATEWAY_SYNC_TIME_COLUMN_NAME, TENANT_GATEWAY_SYNC_LOG_COLUMN_NAME);
        List<String> errorEntityIds;
        if (postback.getIncrementStatus().equalsIgnoreCase(SYNC_STATUS_PARTIAL)) {
            errorEntityIds = Lists.transform(postback.getEntityErrors(), new Function<Map<String, String>, String>() {
                @Override
                public String apply(Map<String, String> stringMap) {
                    return stringMap.get(PortalSyncPostbackJson.ERROR_ENTITY_ID_LABEL);
                }
            });
            // if we have partial status, update the error sync entities
            updateErrorEntities(jdbcConnectionName, nodeId, postback.getEntityErrors(), postback.getIncrementEnd(), TABLE_NAME, UUID_FIELD_NAME);
        } else {
            errorEntityIds = Collections.emptyList();
        }
        updateSyncSuccessEntities(jdbcConnectionName, postback, nodeId, errorEntityIds);
    }
    /*
     * update postback status for successfully sync entities
     */
    private void updateSyncSuccessEntities(String jdbcConnectionName, PortalSyncPostbackJson postback, String nodeId, List<String> errorEntityIds) throws PolicyAssertionException {
        //get entities that are sync and are not in the errorEntityIds list
        EntityIds entityIds = new EntityIds(getApplicationSyncEntitiesIds(jdbcConnectionName, nodeId, errorEntityIds, postback.getIncrementStart(), postback.getIncrementEnd()));
        if (entityIds.getEntityIds().size() == 0) {  //if no sync status of entity needs to be updated, exit
            return;
        }
        String log = (postback.getIncrementStatus().equalsIgnoreCase(SYNC_STATUS_ERROR)) ? postback.getErrorMessage() : null;

        //get entities that need to be updated in APPLICATION_TENANT_GATEWAY
        String columnName = "application_uuid";
        String applicationSyncQuery = String.format(
                "SELECT  %s FROM APPLICATION_TENANT_GATEWAY WHERE TENANT_GATEWAY_UUID=? and APPLICATION_UUID IN ( %s )", columnName.toUpperCase(), entityIds.getSqlString());
        List<Object> params = Lists.<Object>newArrayList(nodeId);
        params.addAll(entityIds.getEntityIds());
        EntityIds entitiesToUpdate = new EntityIds(queryEntityInfo(jdbcConnectionName, applicationSyncQuery, params, columnName));
        if (entitiesToUpdate.getEntityIds().size() > 0) {
            params = Lists.<Object>newArrayList(log, BigInteger.valueOf(postback.getIncrementEnd()), nodeId);
            params.addAll(entitiesToUpdate.getEntityIds());
            queryJdbc(jdbcConnectionName,
                    String.format(
                            "UPDATE APPLICATION_TENANT_GATEWAY SET SYNC_LOG=? , SYNC_TIME=? WHERE TENANT_GATEWAY_UUID=? AND APPLICATION_UUID IN (%s)", entitiesToUpdate.getSqlString()), params
            );
        }
        //get entities that need to be inserted to APPLICATION_TENANT_GATEWAY
        Collection<String> entitiesToAdd = CollectionUtils.subtract(entityIds.getEntityIds(), entitiesToUpdate.getEntityIds());
        for (String addEntityId : entitiesToAdd) {
            appEntityInsert(jdbcConnectionName, nodeId, addEntityId, postback.getIncrementEnd(), log);
        }
    }
    /*
     * insert sync status to APPLICATION_TENANT_GATEWAY
     */
    private void appEntityInsert(String jdbcConnectionName, String nodeId, String addEntityId, long syncTime, String log) throws PolicyAssertionException {
        List<Object> params = Lists.<Object>newArrayList(nodeId, addEntityId, BigInteger.valueOf(syncTime), log);
        Object result = queryJdbc(jdbcConnectionName, "INSERT INTO APPLICATION_TENANT_GATEWAY (UUID, TENANT_GATEWAY_UUID , APPLICATION_UUID , SYNC_TIME, SYNC_LOG ) VALUES  ( UUID() , ?, ? , ?, ?)", params);
        if (!(result instanceof Integer) || ((Integer) result).intValue() != 1) {
            throw new PolicyAssertionException(assertion, String.format("Failed to insert sync status of entity (%s) of node(%s) to  APPLICATION_TENANT_GATEWAY.  ", addEntityId, nodeId));
        }
    }

    /*
     * update sync status of the tssg that is identitied by the input nodeId
     */
    private void updateTenantSyncStatus(String jdbcConnectionName, String nodeId, PortalSyncPostbackJson postback, String syncTimeColumn, String syncLogColumn) throws PolicyAssertionException {
        String query = String.format("UPDATE TENANT_GATEWAY SET %s=? , %s=? WHERE UUID=?", syncTimeColumn, syncLogColumn);
        Object result = queryJdbc(jdbcConnectionName, query, Lists.<Object>newArrayList(BigInteger.valueOf(postback.getIncrementEnd()), postback.getSyncLog(), nodeId));
        if (!(result instanceof Integer) || ((Integer) result).intValue() != 1) {
            throw new PolicyAssertionException(assertion, String.format("Failed to update the sync status of the node with nodeId, '%s', in TENANT_GATEWAY.  ", nodeId));
        }
    }

    /*
     * get entities that are modified within the sync time range,  deleted within the sync time range or sync-ed with errors before
     */
    List<String> getApplicationSyncEntitiesIds(String jdbcConnectionName, String nodeId, List<String> errorEntityIds, long incrementStart, long incrementEnd)
            throws PolicyAssertionException {
        Set<String> entityIds = Sets.newHashSet();
        //get deleted entities
        String columeName = "entity_uuid";
        String sqlQuery = ServerIncrementalSyncCommon.getSyncDeletedEntities(ServerIncrementalSyncCommon.ENTITY_TYPE_APPLICATION);
        entityIds.addAll(queryEntityInfo(jdbcConnectionName, sqlQuery, Lists.<Object>newArrayList(BigInteger.valueOf(incrementStart), BigInteger.valueOf(incrementEnd)), columeName));
        // get new or updated or last sync error apps
        columeName = "uuid";
        sqlQuery = ServerIncrementalSyncCommon.getSyncUpdatedAppEntities(Lists.newArrayList("a." + columeName.toUpperCase()));
        entityIds.addAll(queryEntityInfo(jdbcConnectionName, sqlQuery,
                Lists.<Object>newArrayList(incrementStart, incrementEnd, incrementStart, incrementEnd, incrementStart, incrementEnd, nodeId), columeName));
        //subtract the new error sync entities
        return Lists.newArrayList(CollectionUtils.subtract(entityIds, errorEntityIds));
    }

    /*
     *run the input query and get the value that is identified by input columneName
     */
    private List<String> queryEntityInfo(String jdbcConnectionName, String sqlQuery, List<Object> params, String columnName) throws PolicyAssertionException {
        Map<String, List> mapResult = (Map<String, List>) queryJdbc(jdbcConnectionName, sqlQuery, params);
        if (mapResult.size() > 0) {
            return Lists.newArrayList(Iterables.transform(mapResult.get(columnName), new Function() {
                @Override
                public String apply(Object o) {
                    return ((String) o);
                }
            }));
        } else {
            return Lists.newArrayList();
        }
    }

    /**
     * insert or update error sync entity messages to the target table
     */
    List<String> updateErrorEntities(String jdbcConnectionName, String nodeId, List<Map<String, String>> errors, long end, String tableName, String uuidColumnName)
            throws PolicyAssertionException {
        final List<String> errorEntityIds = Lists.newArrayList();
        String id;
        String msg;
        Object results;
        String updateSql = String.format("UPDATE %s SET  SYNC_TIME=? , SYNC_LOG=? WHERE TENANT_GATEWAY_UUID=? AND %s=? ", tableName, uuidColumnName);
        String insertSql = String.format(
                "INSERT INTO %s (UUID, TENANT_GATEWAY_UUID , SYNC_LOG ,SYNC_TIME, %s  ) VALUES  ( UUID() , ?, ? ,?, ? )", tableName, uuidColumnName);

        for (Map<String, String> m : errors) {
            id = m.get(ERROR_ENTITY_ID_LABEL);
            msg = m.get(ERROR_ENTITY_MSG_LABEL);
            // if the record exists, update it.  Otherwise, insert it
            results = queryJdbc(jdbcConnectionName, updateSql, Lists.<Object>newArrayList(end, msg, nodeId, id));
            errorEntityIds.add(id);
            if (results instanceof Integer && ((Integer) results).intValue() == 0) {
                queryJdbc(jdbcConnectionName, insertSql, Lists.<Object>newArrayList(nodeId, msg, end, id));
            }
        }
        return errorEntityIds;
    }


    Object queryJdbc(String connName, String queryString, @NotNull List<Object> preparedStmtParams) throws PolicyAssertionException {
        final Object result = jdbcQueryingManager.performJdbcQuery(connName, dataSource, queryString, null, ServerIncrementalSyncCommon.getMaxRecords(), ServerIncrementalSyncCommon.getQueryTimeout(), preparedStmtParams);
        //if the results type is String, according to JdbcQueryingManager javadoc, it should be an error message
        if (result instanceof String) {
            throw new PolicyAssertionException(assertion,
                    String.format("Problem executing sql string, '%s'.  Error message: %s", queryString, (String) result));
        }
        return result;
    }

    private class EntityIds {
        final private List<String> entityIds;
        final private String sqlString;

        public EntityIds(List<String> entityIds) {
            this.entityIds = entityIds;
            sqlString = StringUtils.repeat("?", " , ", entityIds.size());
        }

        public List<String> getEntityIds() {
            return entityIds;
        }

        public String getSqlString() {
            return sqlString;
        }
    }
}