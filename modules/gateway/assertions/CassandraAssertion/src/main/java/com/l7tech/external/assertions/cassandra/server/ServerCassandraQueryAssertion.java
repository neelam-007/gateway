package com.l7tech.external.assertions.cassandra.server;

import com.ca.datasources.cassandra.CassandraQueryManager;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.server.cassandra.CassandraConnectionHolder;
import com.l7tech.server.cassandra.CassandraConnectionManager;
import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.l7tech.external.assertions.cassandra.CassandraQueryAssertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static com.l7tech.server.jdbc.JdbcQueryUtils.getQueryStatementWithoutContextVariables;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class ServerCassandraQueryAssertion extends AbstractServerAssertion<CassandraQueryAssertion> {
    private final static String XML_RESULT_TAG_OPEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:cassandraQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/cassandra-query-result\">";
    private final static String XML_RESULT_TAG_CLOSE = "</L7j:cassandraQueryResult>";


    private final String[] variablesUsed;
    private CassandraQueryAssertion assertion = null;
    private final CassandraConnectionManager connectionManager;
    private final CassandraQueryManager cassandraQueryManager;

    public ServerCassandraQueryAssertion(final CassandraQueryAssertion assertion, ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
        this.assertion = assertion;
        this.connectionManager = applicationContext.getBean("cassandraConnectionManager", CassandraConnectionManager.class);
        this.cassandraQueryManager = applicationContext.getBean("cassandraQueryManager", CassandraQueryManager.class);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        CassandraConnectionHolder cassandraConnection = connectionManager.getConnection(assertion.getConnectionName());

        if ((cassandraConnection == null) || (cassandraConnection.getSession() == null)){
            logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "Error retrieving Cassandra Connection, Cassandra Session is null");
            return AssertionStatus.FAILED;
        }

        //extract parameters from the query only the first time it should be used afterwords until someone changes the assertion
        final Pair<String, List<Object>> pair;
        if (context instanceof AuditLookupPolicyEnforcementContext || context instanceof AuditSinkPolicyEnforcementContext) {
            pair = getQueryStatementWithoutContextVariables(assertion.getQueryDocument(), context, variablesUsed, false, Collections.EMPTY_LIST, getAudit());
        } else {
            pair = getQueryStatementWithoutContextVariables(assertion.getQueryDocument(),context, variablesUsed, false, getAudit());
        }

        final String plainQuery = pair.left;
        final  List<Object> preparedStmtParams = pair.right;
        boolean isSelectQuery = plainQuery.toLowerCase().startsWith("select");
        Map<String, PreparedStatement> stmtMap = cassandraConnection.getPreparedStatementMap();
        PreparedStatement preparedStatement = stmtMap.get(plainQuery);

        int resultSize = 0;
        try {
            Session session = cassandraConnection.getSession(); //get the session

            if(preparedStatement == null ) {
                preparedStatement = cassandraQueryManager.buildPreparedStatement(session, plainQuery);
                if(preparedStatement == null) {
                    logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "PreparedStatement is null.");
                    return AssertionStatus.FAILED;
                }
                stmtMap.put(plainQuery, preparedStatement);//add prepared statement to the connection holder
            }

            BoundStatement boundStatement = cassandraQueryManager.buildBoundStatement(preparedStatement, preparedStmtParams);
            boundStatement.setFetchSize(assertion.getFetchSize());
            Map<String, List<Object>> resultMap =  new TreeMap<>();

            resultSize = cassandraQueryManager.executeStatement(session, boundStatement, resultMap, assertion.getMaxRecords(), assertion.getQueryTimeout());

            //Get results map into context variable
            String prefix = assertion.getPrefix();
            Map<String, String> namingMap = assertion.getNamingMap();
            //map results to the appropriate context variables
            for(String key: resultMap.keySet()){
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

        }  catch (NoHostAvailableException nhe) {
            for (Map.Entry<InetSocketAddress, Throwable> entry: nhe.getErrors().entrySet()) {
                InetSocketAddress inetAddress = entry.getKey();
                Throwable throwable = entry.getValue();
                logAndAudit(AssertionMessages.CASSANDRA_CONNECTION_CANNOT_CONNECT, new String[] {cassandraConnection.getCassandraConnectionEntity().getName(),  "Host not available. Host Address: " + inetAddress.getAddress()}, ExceptionUtils.getDebugException(throwable));
            }
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, new String[] {e.getMessage()}, ExceptionUtils.getDebugException(e));
            return AssertionStatus.FAILED;
        }

        if(isSelectQuery && resultSize < 1 && assertion.isFailIfNoResults()){
            logAndAudit(AssertionMessages.CASSANDRA_NO_QUERY_RESULT_ASSERTION_FAILED, cassandraConnection.getCassandraConnectionEntity().getName());
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
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
