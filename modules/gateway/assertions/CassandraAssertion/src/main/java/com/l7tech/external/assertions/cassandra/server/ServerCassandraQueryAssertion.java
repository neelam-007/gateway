package com.l7tech.external.assertions.cassandra.server;

import com.ca.datasources.cassandra.connection.CassandraConnectionHolder;
import com.ca.datasources.cassandra.connection.CassandraConnectionManager;
import com.datastax.driver.core.*;
import com.datastax.driver.core.exceptions.NoHostAvailableException;
import com.l7tech.external.assertions.cassandra.CassandraQueryAssertion;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.DateTimeConfigUtils;
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
    private BoundStatement boundStatement = null;
    private CassandraQueryAssertion assertion = null;
    private DateTimeConfigUtils dateTimeConfigUtils = null;
    private final CassandraConnectionManager connectionManager;

    public ServerCassandraQueryAssertion(final CassandraQueryAssertion assertion, ApplicationContext applicationContext) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
        this.assertion = assertion;
        this.connectionManager = applicationContext.getBean("cassandraConnectionManager", CassandraConnectionManager.class);

        dateTimeConfigUtils = new DateTimeConfigUtils();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        CassandraConnectionHolder cassandraConnection = connectionManager.getConnection(assertion.getConnectionName());

        if ((cassandraConnection == null) || (cassandraConnection.getSession() == null)){
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Error retrieving Cassandra Connection, Cassandra Session is null");
            return AssertionStatus.FAILED;
        }

        //Resolve context variable references within query document
        Map<String, Object> vars = context.getVariableMap(Syntax.getReferencedNames(assertion.getQueryDocument()), getAudit());

        //TODO: extract parameters from the query
        final Pair<String, List<Object>> pair;
        if (context instanceof AuditLookupPolicyEnforcementContext || context instanceof AuditSinkPolicyEnforcementContext) {
            pair = getQueryStatementWithoutContextVariables(assertion.getQueryDocument(),
                    context, assertion.getVariablesUsed(), true, Collections.EMPTY_LIST, getAudit());
        } else {
            pair = getQueryStatementWithoutContextVariables(assertion.getQueryDocument(),context, assertion.getVariablesUsed(), true, getAudit());
        }
        final String plainQuery = pair.left;
        final List<Object> preparedStmtParams = pair.right;

//        String queryDocument = ExpandVariables.process(assertion.getQueryDocument(), vars, getAudit(), true);

        final boolean isSelectQuery = plainQuery.toLowerCase().startsWith("select");
        int resultSize = 0;
        try {
            if(null == boundStatement){
                try {
                    PreparedStatement preparedStatement = cassandraConnection.getSession().prepare(plainQuery);
                    boundStatement = new BoundStatement(preparedStatement);
                } catch (Exception e){
                    getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
                    return AssertionStatus.FAILED;
                }

            }

            boundStatement.bind(preparedStmtParams.toArray());

            ResultSetFuture result = cassandraConnection.getSession().executeAsync(boundStatement);


            if(isSelectQuery) {
                retriveResults(result.getUninterruptibly(), context);
            }

        }  catch (NoHostAvailableException e) {
            for (Map.Entry<InetSocketAddress, Throwable> entry: e.getErrors().entrySet()) {
                InetSocketAddress inetAddress = entry.getKey();
                Throwable throwable = entry.getValue();
                getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Host Address: " + inetAddress.getAddress() + " Throwable: " + throwable.getMessage());
            }
            return AssertionStatus.FAILED;
        } catch (Exception e) {
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, e.getMessage());
            return AssertionStatus.FAILED;
        }



        if(resultSize < 1 && assertion.isFailIfNoResults()){
            getAudit().logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Failing assertion due to no result returned.");
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private void retriveResults(ResultSet rows, PolicyEnforcementContext context) {
        int resultSize = 0;
         Map<String,List<Object>> resultMap = new HashMap<String,List<Object>>();

        Iterator<Row> resultSetIterator = rows.iterator();
        // Get resultSet into map
        while(resultSetIterator.hasNext()){
            Row row = resultSetIterator.next();
            for(ColumnDefinitions.Definition definition: row.getColumnDefinitions()){
                List<Object> col  = resultMap.get(definition.getName());

                if (col == null){
                    col = new ArrayList<Object>();
                    resultMap.put(definition.getName(),col);
                }

                Object o = mapCassandraDataTypeToJavaType(definition, row);
                col.add(o);
            }
            resultSize++;
        }

        //Get results map into context variable
        String prefix = assertion.getPrefix();

        for(String key: resultMap.keySet()){
            if (resultMap.get(key) != null) {
                context.setVariable(prefix + "." + key, resultMap.get(key).toArray());
            }

        }
        context.setVariable(prefix + ".queryresult.count", resultSize);

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

    private Object mapCassandraDataTypeToJavaType(ColumnDefinitions.Definition definition, Row row){
       Object o = null;
       String columnName = definition.getName();
       DataType.Name name= definition.getType().getName();
       switch (name) {
           case ASCII:
           case TEXT:
           case VARCHAR:
               o =  row.getString(columnName);
               break;
           case BIGINT:
           case COUNTER:
               o = row.getLong(columnName);
               break;
           case BLOB:
           case CUSTOM:
               o = row.getBytes(columnName);
               break;
           case BOOLEAN:
               o = row.getBool(columnName);
               break;
           case DECIMAL:
               o = row.getDecimal(columnName);
               break;
           case DOUBLE:
               o = row.getDouble(columnName);
               break;
           case FLOAT:
               o = row.getFloat(columnName);
               break;
           case INET:
               o = row.getInet(columnName);
               break;
           case INT:
               o = row.getInt(columnName);
               break;
           case LIST:
               o = row.getList(columnName, Object.class);
               break;
           case MAP:
               o =row.getMap(columnName, Object.class, Object.class);
               break;
           case SET:
               o = row.getSet(columnName, Object.class);
               break;
           case TIMESTAMP:
               o = row.getDate(columnName);
               break;
           case UUID:
           case TIMEUUID:
               o = row.getUUID(columnName);
               break;
           case VARINT:
               o = row.getVarint(columnName);
               break;
       }
       return o;
    }

}
