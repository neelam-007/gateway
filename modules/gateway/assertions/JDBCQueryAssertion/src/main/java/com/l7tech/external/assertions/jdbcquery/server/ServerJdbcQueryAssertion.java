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
    private final static String XML_RESULT_TAG_OPEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\">";
    private final static String XML_RESULT_TAG_CLOSE = "</L7j:jdbcQueryResult>";
    private final static String XML_RESULT_COL_OPEN = "<L7j:col ";
    private final static String XML_RESULT_COL_CLOSE = "</L7j:col>";
    private final static String XML_RESULT_ROW_OPEN = "<L7j:row>";
    private final static String XML_RESULT_ROW_CLOSE = "</L7j:row>";
    private final static String XML_CDATA_TAG_OPEN = "<![CDATA[";
    private final static String XML_CDATA_TAG_CLOSE = "]]>";
    private final static String XML_NULL_VALUE = XML_CDATA_TAG_OPEN + "NULL" + XML_CDATA_TAG_CLOSE;//are special entry to null values

    public ServerJdbcQueryAssertion(JdbcQueryAssertion assertion, ApplicationContext context) throws PolicyAssertionException {
        super(assertion);

        if (context == null) throw new IllegalStateException("Application context cannot be null.");

        variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        final StringBuffer xmlResult = new StringBuffer(XML_RESULT_TAG_OPEN);
        final List<Object> preparedStmtParams = new ArrayList<Object>();
        try {
            final String plainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), preparedStmtParams, context, assertion.getVariablesUsed(), assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(), getAudit());
            applyNullValue(assertion.getNullPattern(),preparedStmtParams);

            final String connName = ExpandVariables.process(assertion.getConnectionName(), context.getVariableMap(variablesUsed, getAudit()), getAudit());
            // Get result by querying.  The result could be a ResultSet object, an integer (updated rows), or a string (a warning message).
            final Object result = jdbcQueryingManager.performJdbcQuery(connName, plainQuery, assertion.getSchema(),assertion.getMaxRecords(), preparedStmtParams);

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
            } else if (result instanceof Map) {
                int affectedRows = setContextVariables((Map<String,List<Object>>) result, context);
                if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                    return AssertionStatus.FAILED;
                }
                if(assertion.isGenerateXmlResult()){
                    buildXmlResultString((Map<String,List<Object>>) result, xmlResult);
                }
            } else if (result instanceof List) {
                List<SqlRowSet> listOfRowSet = (List<SqlRowSet>) result;
                int affectedRows = 0;
                if (listOfRowSet.size() == 1) {
                    affectedRows = setContextVariables(listOfRowSet.get(0), context, EMPTY_STRING);
                    if(assertion.isGenerateXmlResult()){
                        buildXmlResultString(0, listOfRowSet.get(0), xmlResult);
                    }
                } else {
                    int resultCount = 1;
                    for (SqlRowSet rowSet : listOfRowSet) {
                        String resultCountVariable = JdbcQueryAssertion.VARIABLE_RESULTSET + (resultCount++);
                        affectedRows += setContextVariables(rowSet, context, resultCountVariable);
                        if(assertion.isGenerateXmlResult()){
                            buildXmlResultString(resultCount-1, rowSet, xmlResult);
                        }
                    }
                    if (resultCount > 1) {
                        context.setVariable(getVariablePrefix(context) + "." + JdbcQueryAssertion.MULTIPLE_VARIABLE_COUNT, affectedRows);
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
        if (assertion.isGenerateXmlResult()) {
            xmlResult.append(XML_RESULT_TAG_CLOSE);
            context.setVariable(getVariablePrefix(context) + assertion.VARIABLE_XML_RESULT, xmlResult.toString());
        }
        return AssertionStatus.NONE;
    }

    static protected void applyNullValue(final String nullPattern, List<Object> preparedStmtParams) {
        if(nullPattern == null)
            return;

        for (int i = 0; i < preparedStmtParams.size(); i++) {
            Object o = preparedStmtParams.get(i);
            if(o.equals(nullPattern))
                preparedStmtParams.set(i,null);
        }
    }

    /**
     * process stored procedure results
     */
    void buildXmlResultString(int resultSetNumber, final SqlRowSet resultSet, final StringBuffer xmlResult) {
        int maxRecords = assertion.getMaxRecords();
        int row = 0;
        resultSet.first();
        StringBuffer records = new StringBuffer();
        while (resultSet.next() && row < maxRecords) {
            records.append(XML_RESULT_ROW_OPEN);
            SqlRowSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String columnName = metaData.getColumnName(i);
                Object value = resultSet.getObject(columnName);
                String colType = EMPTY_STRING;
                if (value != null) {
                    if (value instanceof byte[]) {
                        colType = "type=\"java.lang.byte[]\"";
                        StringBuilder sb = new StringBuilder();
                        for (byte b : (byte[]) value) {
                            sb.append(String.format("%02X ", b));
                        }
                        value = sb.toString();
                    } else {
                        colType = "type=\"" + value.getClass().getName() + "\"";
                    }
                }
                records.append(XML_RESULT_COL_OPEN + " name=\"" + columnName + "\" " + colType + ">");
                if (value != null) {
                    records.append(handleSpecialXmlChar(value));
                } else {
                    records.append(XML_NULL_VALUE);
                }
                records.append(XML_RESULT_COL_CLOSE);
            }
            records.append(XML_RESULT_ROW_CLOSE);
        }
        if (resultSetNumber > 0) {
            xmlResult.append("<resultSet" + resultSetNumber + ">");
            xmlResult.append(records);
            xmlResult.append("</resultSet" + resultSetNumber + ">");
        } else {
            xmlResult.append(records);
        }
    }

    /**
     * process standard SQL select results
     */
    void buildXmlResultString(Map<String, List<Object>> resultSet, final StringBuffer xmlResult) {
        int row = 0;
        //try to check how many rows we need
        for (String columnName : resultSet.keySet()) {
            if (resultSet.get(columnName) != null) {
                row = resultSet.get(columnName).toArray().length;
                break;
            }
        }
        StringBuffer records = new StringBuffer();
        for (int i = 0; i < row; i++) {
            records.append(XML_RESULT_ROW_OPEN);
            for (String columnName : resultSet.keySet()) {
                List list = resultSet.get(columnName);
                Object value = null;
                if (list != null && i < list.size()) {
                    value = resultSet.get(columnName).get(i);
                }
                String colType = EMPTY_STRING;
                if (value != null) {
                    if (value instanceof byte[]) {
                        colType = "type=\"java.lang.byte[]\"";
                        StringBuilder sb = new StringBuilder();
                        for (byte b : (byte[]) value) {
                            sb.append(String.format("%02X ", b));
                        }
                        value = sb.toString();
                    } else {
                        colType = "type=\"" + value.getClass().getName() + "\"";
                    }
                }
                records.append(XML_RESULT_COL_OPEN + " name=\"" + columnName + "\" " + colType + ">");
                if (value != null) {
                    records.append(handleSpecialXmlChar(value));
                } else {
                    records.append(XML_NULL_VALUE);
                }
                records.append(XML_RESULT_COL_CLOSE);
            }
            records.append(XML_RESULT_ROW_CLOSE);
        }
        xmlResult.append(records);
    }

    Object handleSpecialXmlChar(final Object inputObj) {
        if (inputObj instanceof String) {
            String inputStr = inputObj.toString();
            if (!inputStr.startsWith(XML_CDATA_TAG_OPEN) && (inputStr.indexOf('>') >= 0 || inputStr.indexOf('<') >= 0 || inputStr.indexOf('&') >= 0)) {
                StringBuffer sb = new StringBuffer(XML_CDATA_TAG_OPEN);
                sb.append(inputStr);
                sb.append(XML_CDATA_TAG_CLOSE);
                return sb.toString();
            } else {
                return inputStr;
            }
        } else {
            return inputObj;
        }
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

    /**
     * build newNameMapping from SqlRowSet type resultSet
     */
    Map<String, String> getNewMapping(SqlRowSet resultSet) {
        Map<String, String> namingMap = assertion.getNamingMap();
        Map<String, String> newNamingMap = new TreeMap<String, String>();

        // Get mappings of column names and context variable names
        SqlRowSetMetaData metaData = resultSet.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            String columnName = metaData.getColumnName(i);
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

    /**
     * To make sure we don't break any calls to the old signature
     */
    int setContextVariables(Map<String,List<Object>> resultSet, PolicyEnforcementContext context) throws SQLException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        Map<String, String> newNamingMap = getNewMapping(resultSet);

        // Assign the results to context variables
        String varPrefix = getVariablePrefix(context);
        int row = 0;
        for (String columnName : resultSet.keySet()) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, varPrefix  + "." + newNamingMap.get(columnName.toLowerCase()) + resultSet.get(columnName).toArray());
            }
            if (resultSet.get(columnName) != null) {
                row = resultSet.get(columnName).size();
                context.setVariable(varPrefix + "." + newNamingMap.get(columnName.toLowerCase()), resultSet.get(columnName).toArray());
            } else {
                logger.log(Level.FINER, columnName + " result list was null");
            }
        }
        context.setVariable(varPrefix + "." + JdbcQueryAssertion.VARIABLE_COUNT, row);
        return row;
    }

    int setContextVariables(SqlRowSet resultSet, PolicyEnforcementContext context, final String resultSetPrefix) throws SQLException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        Map<String, String> newNamingMap = getNewMapping(resultSet);

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
                logger.log(Level.FINER, varPrefix + resultSetPrefix + "." + newNamingMap.get(column.toLowerCase()) + results.get(column).toArray());
            }
            context.setVariable(varPrefix + resultSetPrefix + "." + newNamingMap.get(column.toLowerCase()), results.get(column).toArray());
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
