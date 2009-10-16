package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.jdbcconnection.JdbcQueryingManager;
import com.l7tech.gateway.common.audit.AssertionMessages;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.*;
import java.util.regex.Matcher;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Server side implementation of the JdbcQueryAssertion.
 *
 * @see com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion
 */
public class ServerJdbcQueryAssertion extends AbstractServerAssertion<JdbcQueryAssertion> {
    private static final Logger logger = Logger.getLogger(ServerJdbcQueryAssertion.class.getName());

    private final JdbcQueryAssertion assertion;
    private final Auditor auditor;
    private final String[] variablesUsed;
    private final JdbcQueryingManager jdbcQueryingManager;

    public ServerJdbcQueryAssertion(JdbcQueryAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        if (context == null) throw new IllegalStateException("Application context cannot be null.");
        if (assertion == null) throw new IllegalStateException("JDBC Query Assertion cannot be null.");

        this.assertion = assertion;
        auditor = new Auditor(this, context, logger);
        variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = (JdbcQueryingManager) context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        List<Object> preparedStmtParams = new ArrayList<Object>();
        String plainQuery = getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), preparedStmtParams, context);

        // Get result by quering.  The result could be a ResultSet object, an integer (updated rows), or a string (a warning message).
        Object result = jdbcQueryingManager.performJdbcQuery(assertion.getConnectionName(), plainQuery, assertion.getMaxRecords(), preparedStmtParams);

        // Analyze the result type and perform a corresponding action.
        if (result instanceof String) {
            auditor.logAndAudit(AssertionMessages.MCM_QUERYING_FAILURE_ASSERTION_FAILED, (String)result);
            return AssertionStatus.FAILED;
        } else if (result instanceof Integer) {
            int num = (Integer)result;
            if (num == 0 && assertion.isAssertionFailureEnabled()) {
                auditor.logAndAudit(AssertionMessages.MCM_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                return AssertionStatus.FAILED;
            } else {
                context.setVariable(getVaraiblePrefix(context) + "." + JdbcQueryAssertion.VARIABLE_COUNT, result);
            }
        } else if (result instanceof ResultSet) {
            try {
                int affectedRows = setContextVariables((ResultSet)result, context);

                if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
                    auditor.logAndAudit(AssertionMessages.MCM_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                    return AssertionStatus.FAILED;
                }
            } catch (SQLException e) {
                auditor.logAndAudit(AssertionMessages.MCM_QUERYING_FAILURE_ASSERTION_FAILED, e.getMessage());
                return AssertionStatus.FAILED;
            }
        } else {
            throw new IllegalStateException("Invalid returned result type, " + result.getClass().getSimpleName());
        }

        return AssertionStatus.NONE;
    }

    private String getQueryStatementWithoutContextVariables(String query, List<Object> params, PolicyEnforcementContext context) {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        if ( Syntax.getReferencedNames(query).length == 0) {
            // There are no context variables in the query text. Just return it immediately.
            return query;
        }

        // Get values of these context variables and then store these values into the parameter list, params.
        // params will be used in the PreparedStatement to set up values.
        String[] vars = Syntax.getReferencedNames(query);
        for (String var: vars) {
            String value = ExpandVariables.process("${" + var + "}", context.getVariableMap(vars, auditor), auditor);
            params.add(value);
        }

        // Replace all context variables with a question mark, ?
        Matcher matcher = Syntax.regexPattern.matcher(query);
        query = matcher.replaceAll("?");

        return query;
    }

    private int setContextVariables(ResultSet resultSet, PolicyEnforcementContext context) throws SQLException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        Map<String, String> namingMap = assertion.getNamingMap();
        Map<String, String> newNamingMap = new TreeMap<String, String>();

        // Get mappings of column names and context varaible names
        ResultSetMetaData metaData = resultSet.getMetaData();
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

    private String getVaraiblePrefix(PolicyEnforcementContext context) {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        String prefix = assertion.getVariablePrefix();
        prefix = ExpandVariables.process(prefix, context.getVariableMap(variablesUsed, auditor), auditor);

        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX;
        }

        return prefix;
    }
}
