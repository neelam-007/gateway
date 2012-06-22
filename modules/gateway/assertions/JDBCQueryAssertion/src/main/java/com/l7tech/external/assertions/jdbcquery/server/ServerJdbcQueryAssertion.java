package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.jdbc.JdbcQueryUtils;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

/**
 * Server side implementation of the JdbcQueryAssertion.
 *
 * @see com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion
 */
public class ServerJdbcQueryAssertion extends AbstractServerAssertion<JdbcQueryAssertion> {
    private final String[] variablesUsed;
    private final JdbcQueryingManager jdbcQueryingManager;
    private final static String EMPTY_STRING = "";

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
            final String plainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), preparedStmtParams, context, assertion.getVariablesUsed(), assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(), getAudit());

            final String connName = ExpandVariables.process(assertion.getConnectionName(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            // Get result by querying.  The result could be a ResultSet object, an integer (updated rows), or a string (a warning message).
            final Object result = jdbcQueryingManager.performJdbcQuery(connName, plainQuery, assertion.getMaxRecords(), preparedStmtParams);

            // Analyze the result type and perform a corresponding action.
            if (result instanceof String) {
                logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, (String) result);
                return AssertionStatus.FAILED;
            } else if (result instanceof Integer) {
                int num = (Integer) result;
                if (num == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                    return AssertionStatus.FAILED;
                } else {
                    context.setVariable(getVariablePrefix(context) + "." + JdbcQueryAssertion.VARIABLE_COUNT, result);
                }
            } else if (result instanceof SqlRowSet) {
                int affectedRows = setContextVariables((SqlRowSet) result, context);
                if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                    return AssertionStatus.FAILED;
                }
            } else if (result instanceof List) {
                List<SqlRowSet> listOfRowSet = (List<SqlRowSet>) result;
                int affectedRows = 0;
                if (listOfRowSet.size() == 1) {
                    affectedRows = setContextVariables(listOfRowSet.get(0), context, EMPTY_STRING);
                } else {
                    int resultCount = 1;
                    for (SqlRowSet rowSet : listOfRowSet) {
                        String resultCountVariable = JdbcQueryAssertion.VARIABLE_RESULTSET + (resultCount++);
                        affectedRows += setContextVariables(rowSet, context, resultCountVariable);
                    }
                    if (resultCount > 1) {
                        context.setVariable(getVariablePrefix(context) + "." + JdbcQueryAssertion.MULTIPLE_VARIABLE_COUNT, resultCount);
                        context.setVariable(getVariablePrefix(context) + "." + JdbcQueryAssertion.MULTIPLE_RESULTSET_COUNT, listOfRowSet.size());
                    }
                }
                if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                    return AssertionStatus.FAILED;
                }
            } else {
                throw new IllegalStateException("Invalid returned result type, " + result.getClass().getSimpleName());
            }

        } catch (SQLException e) {
            logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, e.getMessage());
            return AssertionStatus.FAILED;
        } catch (VariableNameSyntaxException e) {
            logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, e.getMessage());
            return AssertionStatus.FAILED;
        }
        return AssertionStatus.NONE;
    }

    /**
     * To make sure we don't break any calls to the old signature
     */
    int setContextVariables(SqlRowSet resultSet, PolicyEnforcementContext context) throws SQLException {
        return setContextVariables(resultSet, context, EMPTY_STRING);
    }

    int setContextVariables(SqlRowSet resultSet, PolicyEnforcementContext context, final String resultSetPrefix) throws SQLException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        Map<String, String> namingMap = assertion.getNamingMap();
        Map<String, String> newNamingMap = new TreeMap<String, String>();

        // Get mappings of column names and context variable names
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
        for (String column : newNamingMap.keySet()) results.put(column, new ArrayList<Object>());

        int maxRecords = assertion.getMaxRecords();
        int row = 0;
        while (resultSet.next() && row < maxRecords) {
            for (String columnName : newNamingMap.keySet()) {
                results.get(columnName).add(resultSet.getObject(columnName));
            }
            row++;
        }

        // Assign the results to context variables
        String varPrefix = getVariablePrefix(context);
        for (String column : results.keySet()) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, varPrefix + resultSetPrefix + "." + newNamingMap.get(column) + results.get(column).toArray());
            }
            context.setVariable(varPrefix + resultSetPrefix + "." + newNamingMap.get(column), results.get(column).toArray());
        }
        context.setVariable(varPrefix + resultSetPrefix + "." + JdbcQueryAssertion.VARIABLE_COUNT, row);

        return row;
    }

    private String getVariablePrefix(PolicyEnforcementContext context) {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        String prefix = assertion.getVariablePrefix();
        prefix = ExpandVariables.process(prefix, context.getVariableMap(variablesUsed, getAudit()), getAudit());

        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX;
        }

        return prefix;
    }
}
