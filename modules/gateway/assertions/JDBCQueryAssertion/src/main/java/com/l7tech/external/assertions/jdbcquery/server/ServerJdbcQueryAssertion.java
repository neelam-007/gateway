package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.io.IOException;
import java.util.*;
import java.sql.SQLException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Server side implementation of the JdbcQueryAssertion.
 *
 * @see com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion
 */
public class ServerJdbcQueryAssertion extends AbstractServerAssertion<JdbcQueryAssertion> {
    private final String[] variablesUsed;
    private final JdbcQueryingManager jdbcQueryingManager;

    public ServerJdbcQueryAssertion(JdbcQueryAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        if (context == null) throw new IllegalStateException("Application context cannot be null.");

        variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        final List<Object> preparedStmtParams = new ArrayList<Object>();
        try {
            final String plainQuery = getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), preparedStmtParams, context);

            // Get result by querying.  The result could be a ResultSet object, an integer (updated rows), or a string (a warning message).
            final String connName =  ExpandVariables.process(assertion.getConnectionName(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            final Object result = jdbcQueryingManager.performJdbcQuery(connName, plainQuery, assertion.getMaxRecords(), preparedStmtParams);

            // Analyze the result type and perform a corresponding action.
            if (result instanceof String) {
                logAndAudit( AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, (String) result );
                return AssertionStatus.FAILED;
            } else if (result instanceof Integer) {
                int num = (Integer)result;
                if (num == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit( AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName() );
                    return AssertionStatus.FAILED;
                } else {
                    context.setVariable(getVaraiblePrefix(context) + "." + JdbcQueryAssertion.VARIABLE_COUNT, result);
                }
            } else if (result instanceof SqlRowSet) {
                int affectedRows = setContextVariables((SqlRowSet)result, context);
                if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit( AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName() );
                    return AssertionStatus.FAILED;
                }
            } else {
                throw new IllegalStateException("Invalid returned result type, " + result.getClass().getSimpleName());
            }

        } catch(SQLException e){
            logAndAudit( AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, e.getMessage() );
            return AssertionStatus.FAILED;
        } catch(VariableNameSyntaxException e) {
            logAndAudit( AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, e.getMessage() );
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    String getQueryStatementWithoutContextVariables(String query, List<Object> params, PolicyEnforcementContext context) {
        //TODO: we may not need to check the context here because it is checked in checkRequest
        //if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        if ( Syntax.getReferencedNames(query).length == 0) {
            // There are no context variables in the query text. Just return it immediately.
            return query;
        }
        // Get values of these context variables and then store these values into the parameter list, params.
        // params will be used in the PreparedStatement to set up values.
        final String[] varsWithIndex = Syntax.getReferencedNamesIndexedVarsNotOmitted(query);
        final String[] varsWithoutIndex = assertion.getVariablesUsed();
        final Map<String, Pattern> varPatternMap = new HashMap<String, Pattern>();
        for (final String varWithIndex: varsWithIndex) {
            if(assertion.isAllowMultiValuedVariables()) {
                List<Object> varValues = ExpandVariables.processNoFormat("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, getAudit()), getAudit(), true);
                //when parameters are multi-value, make each value as a separate parameter of the parameterized query separated by comma.
                StringBuilder sb = new StringBuilder();
                Iterator<Object> iter = varValues.iterator();
                while(iter.hasNext()){
                    Object value = iter.next();
                    params.add(value);
                    sb.append("?").append(iter.hasNext()?", ":"");
                }

                final Pattern searchPattern = getSearchPattern(varWithIndex, varPatternMap);
                Matcher matcher = searchPattern.matcher(query);
                query = matcher.replaceFirst(sb.toString());
            }
            else {
                String value = ExpandVariables.process("${" + varWithIndex + "}", context.getVariableMap(varsWithoutIndex, getAudit()), getAudit());
                params.add(value);
            }
        }

        if(!assertion.isAllowMultiValuedVariables()) {
            // Replace all context variables with a question mark, ?
            Matcher matcher = Syntax.regexPattern.matcher(query);
            query = matcher.replaceAll("?");
        }

        return query;
    }

    int setContextVariables(SqlRowSet resultSet, PolicyEnforcementContext context) throws SQLException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        Map<String, String> namingMap = assertion.getNamingMap();
        Map<String, String> newNamingMap = new TreeMap<String, String>();

        // Get mappings of column names and context varaible names
        SqlRowSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
            if (namingMap.containsKey(columnName)) {
                newNamingMap.put(columnName, namingMap.get(columnName));
            } else {
                newNamingMap.put(columnName, columnName);
            }
        }

        // Get results
        Map<String, List<Object>> results = new HashMap<String, List<Object>>(newNamingMap.size());
        for (String column: newNamingMap.keySet()) results.put(column, new ArrayList<Object>());

        int maxRecords = assertion.getMaxRecords();
        int row = 0;
        while (resultSet.next() && row < maxRecords) {
            for (String columnName: newNamingMap.keySet()) {
                results.get(columnName).add(resultSet.getObject(columnName));
            }
            row++;
        }

        // Assign the results to context variables
        String varPrefix = getVaraiblePrefix(context);
        for (String column: results.keySet()) {
            context.setVariable(varPrefix + "." + newNamingMap.get(column), results.get(column).toArray());
        }
        context.setVariable(varPrefix + "." + JdbcQueryAssertion.VARIABLE_COUNT, row);

        return row;
    }

    private Pattern getSearchPattern(String varWithIndex, Map<String, Pattern> varPatternMap) {
        if(varPatternMap.containsKey(varWithIndex)) {
            return varPatternMap.get(varWithIndex);
        }

        final Pattern searchPattern = Pattern.compile("${" + varWithIndex + "}", Pattern.LITERAL);
        varPatternMap.put(varWithIndex, searchPattern);

        return searchPattern;
    }

    private String getVaraiblePrefix(PolicyEnforcementContext context) {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        String prefix = assertion.getVariablePrefix();
        prefix = ExpandVariables.process(prefix, context.getVariableMap(variablesUsed, getAudit()), getAudit());

        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX;
        }

        return prefix;
    }
}
