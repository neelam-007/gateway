package com.l7tech.external.assertions.cassandra.server;

import com.ca.datasources.cassandra.CassandraUtil;
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

import static com.l7tech.server.jdbc.JdbcQueryUtils.getQueryStatementWithoutContextVariables;

/**
 * Created with IntelliJ IDEA.
 * User: joe
 * Date: 1/30/14
 * Time: 5:03 PM
 *
 */
public class ServerCassandraQueryAssertion extends AbstractServerAssertion<CassandraQueryAssertion> {

    private final String[] variablesUsed;
    private CassandraQueryAssertion assertion = null;
    private final CassandraConnectionManager connectionManager;

    public ServerCassandraQueryAssertion(final CassandraQueryAssertion assertion, ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
        this.assertion = assertion;
        this.connectionManager = applicationContext.getBean("cassandraConnectionManager", CassandraConnectionManager.class);
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        CassandraConnectionHolder cassandraConnection = connectionManager.getConnection(assertion.getConnectionName());

        if ((cassandraConnection == null) || (cassandraConnection.getSession() == null)){
            logAndAudit(AssertionMessages.CASSANDRA_QUERYING_FAILURE_ASSERTION_FAILED, "Error retrieving Cassandra Connection, Cassandra Session is null");
            return AssertionStatus.FAILED;
        }

        Session session = cassandraConnection.getSession();
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
            if(preparedStatement == null ) {
                preparedStatement = session.prepare(plainQuery);
                stmtMap.put(plainQuery, preparedStatement);//add prepared statement to the connection holder
            }

            //construct BoundStatement
            BoundStatement boundStatement = new BoundStatement(preparedStatement);

            List<ColumnDefinitions.Definition> cdlist = boundStatement.preparedStatement().getVariables().asList();
            List<Object> convertedStmtParams = new ArrayList<>();
            for(int i = 0; i < cdlist.size(); i++){
                convertedStmtParams.add(CassandraUtil.javaType2CassandraDataType(cdlist.get(i), preparedStmtParams.get(i)));
            }

            boundStatement.bind(convertedStmtParams.toArray());

            ResultSetFuture result = cassandraConnection.getSession().executeAsync(boundStatement);

            if(isSelectQuery) {
                resultSize = retriveResults(result.getUninterruptibly(), context);
            }

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


        if(resultSize < 1 && assertion.isFailIfNoResults()){
            logAndAudit(AssertionMessages.CASSANDRA_NO_QUERY_RESULT_ASSERTION_FAILED, cassandraConnection.getCassandraConnectionEntity().getName());
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
    }

    private int retriveResults(ResultSet rows, PolicyEnforcementContext context) {
        int resultSize = 0;
         Map<String,List<Object>> resultMap = new HashMap<>();

        Iterator<Row> resultSetIterator = rows.iterator();
        // Get resultSet into map
        while(resultSetIterator.hasNext()){
            Row row = resultSetIterator.next();
            for(ColumnDefinitions.Definition definition: row.getColumnDefinitions()){
                List<Object> col  = resultMap.get(definition.getName());

                if (col == null){
                    col = new ArrayList();
                    resultMap.put(definition.getName(),col);
                }

                Object o = CassandraUtil.cassandraDataType2JavaType(definition, row);
                col.add(o);
            }
            resultSize++;
        }

        //Get results map into context variable
        String prefix = assertion.getPrefix();
        Map<String, String> namingMap = assertion.getNamingMap();

        for(String key: resultMap.keySet()){
            String columnName = namingMap.containsKey(key.toLowerCase()) ? namingMap.get(key) : key;
            if (resultMap.get(key) != null) {
                context.setVariable(prefix + "." + columnName, resultMap.get(key).toArray());
            }

        }
        //set query result count
        context.setVariable(prefix + CassandraQueryAssertion.QUERYRESULT_COUNT, resultSize);
        return resultSize;
    }

    /**
     * build newNameMapping from map type resultSet
     */
    Map<String, String> getNewMapping(Map<String, List<Object>> resultSet) {
        Map<String, String> namingMap = assertion.getNamingMap();
        Map<String, String> newNamingMap = new TreeMap<String, String>();

        // Get mappings of column names and context variable names
        for (String columnName : resultSet.keySet()) {
            boolean found = false;
            for (final Map.Entry e : namingMap.entrySet()) {
                String key = e.getKey().toString();
                String value = e.getValue().toString();
                if (key.equalsIgnoreCase(columnName)) {
                    found = true;
                    newNamingMap.put(columnName.toLowerCase(), value);
                    break;
                }
            }
            if (!found) {
                newNamingMap.put(columnName.toLowerCase(), columnName);
            }
        }
        return newNamingMap;
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
