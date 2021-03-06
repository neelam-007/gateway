package com.l7tech.external.assertions.cassandra.server;

import com.ca.datasources.cassandra.CassandraQueryManager;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cassandra.CassandraConnectionHolder;
import com.l7tech.server.cassandra.CassandraConnectionManager;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.l7tech.external.assertions.cassandra.CassandraQueryAssertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Config;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.logging.Logger;

import static com.l7tech.server.jdbc.JdbcQueryUtils.getQueryStatementWithoutContextVariables;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class ServerCassandraQueryAssertion extends AbstractServerAssertion<CassandraQueryAssertion> {
    private final static Logger logger = Logger.getLogger(ServerCassandraQueryAssertion.class.getName());
    private final static String XML_RESULT_TAG_OPEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:cassandraQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/cassandra-query-result\">";
    private final static String XML_RESULT_TAG_CLOSE = "</L7j:cassandraQueryResult>";
    public static final long DEFAULT_BLOB_MAX_SIZE = 10485760L;


    private final String[] variablesUsed;
    private CassandraQueryAssertion assertion = null;
    private final CassandraConnectionManager connectionManager;
    private final CassandraQueryManager cassandraQueryManager;
    private final Config config;

    public ServerCassandraQueryAssertion(final CassandraQueryAssertion assertion, ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
        this.assertion = assertion;
        this.connectionManager = applicationContext.getBean("cassandraConnectionManager", CassandraConnectionManager.class);
        this.cassandraQueryManager = applicationContext.getBean("cassandraQueryManager", CassandraQueryManager.class);
        this.config = applicationContext.getBean("serverConfig", Config.class);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        CassandraConnectionHolder cassandraConnection = null;
        try {
            cassandraConnection = connectionManager.getConnection(assertion.getConnectionName());

            if ((cassandraConnection == null) || (cassandraConnection.getSession() == null)) {
                logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "Error retrieving Cassandra Connection, Cassandra Session is null");
                return AssertionStatus.FAILED;
            }

            //extract parameters from the query only the first time it should be used afterwords until someone changes the assertion
            final Pair<String, List<Object>> pair = getQueryStatementWithoutContextVariables(
                    assertion.getQueryDocument(), context, variablesUsed, false, Collections.EMPTY_LIST, getAudit());

            final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
            final String queryTimeoutString = StringUtils.isNotBlank(assertion.getQueryTimeout()) ? assertion.getQueryTimeout() : "0";
            final String resolvedQueryTimeout = ExpandVariables.process(queryTimeoutString, variableMap, getAudit());
            if (!ValidationUtils.isValidLong(resolvedQueryTimeout, false, 0, Long.MAX_VALUE)) {
                logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "Invalid resolved value for query timeout: " + resolvedQueryTimeout);
                return AssertionStatus.FAILED;
            }
            final long queryTimeout = Long.parseLong(resolvedQueryTimeout);

            final String plainQuery = pair.left;
            final List<Object> preparedStmtParams = pair.right;
            boolean isSelectQuery = plainQuery.toLowerCase().startsWith("select");
            Map<String, PreparedStatement> stmtMap = cassandraConnection.getPreparedStatementMap();
            PreparedStatement preparedStatement = stmtMap.get(plainQuery);

            int resultSize = 0;
            try {
                Session session = cassandraConnection.getSession(); //get the session

                if (preparedStatement == null) {
                    preparedStatement = cassandraQueryManager.buildPreparedStatement(session, plainQuery);
                    if (preparedStatement == null) {
                        logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "PreparedStatement is null.");
                        return AssertionStatus.FAILED;
                    }
                    stmtMap.put(plainQuery, preparedStatement);//add prepared statement to the connection holder
                }


                if (!validateParameters(preparedStmtParams)) {
                    logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "Context Variable Type is not convertible to a Cassandra Data Type");
                    return AssertionStatus.FALSIFIED;
                }
                BoundStatement boundStatement = cassandraQueryManager.buildBoundStatement(preparedStatement, preparedStmtParams);
                boundStatement.setFetchSize(assertion.getFetchSize());
                Map<String, List<Object>> resultMap = new TreeMap<>();

                final long maxBlobSize = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, DEFAULT_BLOB_MAX_SIZE);
                resultSize = cassandraQueryManager.executeStatement(session, boundStatement, resultMap, assertion.getMaxRecords(), maxBlobSize, queryTimeout);

                //Get results map into context variable
                String prefix = assertion.getPrefix();
                Map<String, String> namingMap = assertion.getNamingMap();
                //map results to the appropriate context variables
                for (String key : resultMap.keySet()) {
                    String columnName = namingMap.containsKey(key.toLowerCase()) ? namingMap.get(key) : key;
                    if (resultMap.get(key) != null) {
                        context.setVariable(prefix + "." + columnName, resultMap.get(key).toArray());
                    }

                }
                if (assertion.isGenerateXmlResult()) {
                    final StringBuilder xmlResult = new StringBuilder(XML_RESULT_TAG_OPEN);
                    JdbcUtil.buildXmlResultString(resultMap, xmlResult);
                    xmlResult.append(XML_RESULT_TAG_CLOSE);
                    context.setVariable(prefix + CassandraQueryAssertion.VARIABLE_XML_RESULT, xmlResult.toString());
                }
                //set query result count
                context.setVariable(prefix + CassandraQueryAssertion.QUERYRESULT_COUNT, resultSize);

            } catch (NoHostAvailableException nhe) {
                for (Map.Entry<InetSocketAddress, Throwable> entry : nhe.getErrors().entrySet()) {
                    InetSocketAddress inetAddress = entry.getKey();
                    Throwable throwable = entry.getValue();
                    logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[]{cassandraConnection.getCassandraConnectionEntity().getName(), "Host not available. Host Address: " + inetAddress.getAddress()}, ExceptionUtils.getDebugException(throwable));
                }
                return AssertionStatus.FAILED;
            } catch (Exception e) {
                logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, new String[]{e.getMessage()}, ExceptionUtils.getDebugException(e));
                return AssertionStatus.FAILED;
            }

            if (isSelectQuery && resultSize < 1 && assertion.isFailIfNoResults()) {
                logAndAudit(AssertionMessages.CASSANDRA_NO_QUERY_RESULT_ASSERTION_FAILED, cassandraConnection.getCassandraConnectionEntity().getName());
                return AssertionStatus.FALSIFIED;
            }

            return AssertionStatus.NONE;
        } finally {
          int maxConnectionCacheSize = config.getIntProperty(ServerConfigParams.PARAM_CASSANDRA_MAX_CONNECTION_CACHE_SIZE, CassandraConnectionManager.DEFAULT_CONNECTION_CACHE_SIZE);
          if(cassandraConnection != null && maxConnectionCacheSize == 0) {
              connectionManager.removeConnection(cassandraConnection.getCassandraConnectionEntity());
          }
        }
    }

    private boolean validateParameters(List<Object> preparedStmtParams) {

        for (Object o : preparedStmtParams) {
            Class clazz = o.getClass();
            if (   clazz.isAssignableFrom(String.class)
                || clazz.isAssignableFrom(Integer.class)
                || clazz.isAssignableFrom(Long.class)
                || clazz.isAssignableFrom(Date.class)
                || clazz.isAssignableFrom(Boolean.class)
                || clazz.isAssignableFrom(BigDecimal.class)
                || clazz.isAssignableFrom(Double.class)
                || clazz.isAssignableFrom(BigInteger.class))
            {
                continue;
            } else {
                logger.fine("Cannot convert Gateway object (" + o.toString() + ") to Cassandra datatype.");
                return false;
            }
        }
        return true;
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     *
     * DELETEME if not required.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
    }
}
