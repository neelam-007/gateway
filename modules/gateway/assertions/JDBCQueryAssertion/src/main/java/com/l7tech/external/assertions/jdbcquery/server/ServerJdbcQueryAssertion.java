package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;

import java.io.*;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.*;
import java.util.logging.Level;

import static com.l7tech.server.jdbc.JdbcQueryUtils.getQueryStatementWithoutContextVariables;

/**
 * Server side implementation of the JdbcQueryAssertion.
 *
 * @see com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion
 */
public class ServerJdbcQueryAssertion extends AbstractServerAssertion<JdbcQueryAssertion> {
    private final String[] variablesUsed;
    private final JdbcQueryingManager jdbcQueryingManager;
    private final JdbcConnectionManager jdbcConnectionManager;
    private final Config config;
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
        jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
        config = context.getBean("serverConfig", Config.class);

        if (assertion.getConnectionName() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
        }
        if (assertion.getSqlQuery() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a sql statement");
        }

        if (!Syntax.isAnyVariableReferenced(assertion.getConnectionName()) && (assertion.getSchema() == null || !Syntax.isAnyVariableReferenced(assertion.getSchema()))) {
            jdbcQueryingManager.registerQueryForPossibleCaching(assertion.getConnectionName(), assertion.getSqlQuery(), assertion.getSchema());
        }
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        final StringBuilder xmlResult = new StringBuilder(XML_RESULT_TAG_OPEN);
        try {
            final Pair<String, List<Object>> pair;
            if (context instanceof AuditLookupPolicyEnforcementContext || context instanceof AuditSinkPolicyEnforcementContext) {
                pair = getQueryStatementWithoutContextVariables(assertion.getSqlQuery(),
                        context, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), assertion.getResolveAsObjectList(), getAudit());
            } else {
                pair = getQueryStatementWithoutContextVariables(assertion.getSqlQuery(),
                        context, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), getAudit());
            }
            final String plainQuery = pair.left;
            final List<Object> preparedStmtParams = pair.right;

            final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
            final String connName = ExpandVariables.process(assertion.getConnectionName(), variableMap, getAudit());
            final String schema = assertion.getSchema() != null ? ExpandVariables.process(assertion.getSchema(), variableMap, getAudit()) : null;
            // Get result by querying.  The result could be a ResultSet object, an integer (updated rows), or a string (a warning message).
            final String queryTimeoutString = (assertion.getQueryTimeout() != null) ? assertion.getQueryTimeout() : "0";
            final String resolvedQueryTimeout = ExpandVariables.process(queryTimeoutString, variableMap, getAudit());
            if (!ValidationUtils.isValidInteger(resolvedQueryTimeout, false, 0, Integer.MAX_VALUE)) {
                logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, "Invalid resolved value for query timeout: " + resolvedQueryTimeout);
                return AssertionStatus.FAILED;
            }
            final int queryTimeout = Integer.parseInt(resolvedQueryTimeout);

            //validate that the connection exists.
            final JdbcConnection connection;
            try {
                connection = jdbcConnectionManager.getJdbcConnection(connName);
                if (connection == null) throw new FindException();
            } catch (FindException e) {
                logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, "Could not find JDBC connection: " + connName);
                return AssertionStatus.FAILED;
            }
            //Validate the if the schema is not null then the connection is able to use that schema.
            final String driverClass = connection.getDriverClass();
            if (schema != null && !(driverClass.contains("oracle") || driverClass.contains("sqlserver"))) {
                logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, "Schema value given but JDBC connection does not support it. Connection name: " + connName);
                return AssertionStatus.FAILED;
            }

            final Object result = jdbcQueryingManager.performJdbcQuery(connName, plainQuery, schema, assertion.getMaxRecords(), queryTimeout, preparedStmtParams);

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
                int affectedRows = setContextVariables((Map<String, List<Object>>) result, context);
                if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
                    logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
                    return AssertionStatus.FAILED;
                }
                if (assertion.isGenerateXmlResult()) {
                    buildXmlResultString((Map<String, List<Object>>) result, xmlResult);
                }
            } else if (result instanceof List) {
                List<SqlRowSet> listOfRowSet = (List<SqlRowSet>) result;
                int affectedRows = 0;
                if (listOfRowSet.size() == 1) {
                    affectedRows = setContextVariables(listOfRowSet.get(0), context, EMPTY_STRING);
                    if (assertion.isGenerateXmlResult()) {
                        buildXmlResultString(0, listOfRowSet.get(0), xmlResult);
                    }
                } else {
                    int resultCount = 1;
                    for (SqlRowSet rowSet : listOfRowSet) {
                        String resultCountVariable = JdbcQueryAssertion.VARIABLE_RESULTSET + (resultCount++);
                        affectedRows += setContextVariables(rowSet, context, resultCountVariable);
                        if (assertion.isGenerateXmlResult()) {
                            buildXmlResultString(resultCount - 1, rowSet, xmlResult);
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

    /**
     * process stored procedure results
     */
    void buildXmlResultString(int resultSetNumber, final SqlRowSet resultSet, final StringBuilder xmlResult) throws SQLException {
        int maxRecords = assertion.getMaxRecords();
        int row = 0;
        resultSet.first();
        final StringBuilder records = new StringBuilder();
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
                        value = getReadableHexString((byte[]) value);
                    } else if (value instanceof Clob) {
                        value = getClobStringValue((Clob) value);
                    } else if (value instanceof Blob) {
                        colType = "type=\"java.lang.byte[]\"";
                        value = getReadableHexString(getBlobValue((Blob) value));
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
     * Converts byte array to readable hexidecimal string. Ie: "12 34 56 78 9A BC DE F0"
     * @param byteArray
     * @return
     */
    static String getReadableHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(String.format("%02X ", b));
        }
        return  sb.toString();
    }

    /**
     * process standard SQL select results
     */
    void buildXmlResultString(Map<String, List<Object>> resultSet, final StringBuilder xmlResult) {
        int row = 0;
        //try to check how many rows we need
        for (String columnName : resultSet.keySet()) {
            if (resultSet.get(columnName) != null) {
                row = resultSet.get(columnName).toArray().length;
                break;
            }
        }
        StringBuilder records = new StringBuilder();
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
                StringBuilder sb = new StringBuilder(XML_CDATA_TAG_OPEN);
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
    int setContextVariables(Map<String, List<Object>> resultSet, PolicyEnforcementContext context) throws SQLException {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        Map<String, String> newNamingMap = getNewMapping(resultSet);

        // Assign the results to context variables
        String varPrefix = getVariablePrefix(context);
        int row = 0;
        for (String columnName : resultSet.keySet()) {
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, varPrefix + "." + newNamingMap.get(columnName.toLowerCase()) + resultSet.get(columnName).toArray());
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
                final List<Object> rows = results.get(columnName);
                final Object value = resultSet.getObject(columnName);
                //TODO - what other types may not be directly applicable as-is?
                if (value instanceof Clob) {
                    rows.add(getClobStringValue((Clob) value));
                } else if (value instanceof Blob) {
                    rows.add(getBlobValue((Blob) value));
                } else {
                    rows.add(value);
                }
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

    //- PRIVATE

    private String getVariablePrefix(PolicyEnforcementContext context) {
        if (context == null) throw new IllegalStateException("Policy Enforcement Context cannot be null.");

        String prefix = assertion.getVariablePrefix();
        prefix = ExpandVariables.process(prefix, context.getVariableMap(variablesUsed, getAudit()), getAudit());

        if (prefix == null || prefix.trim().isEmpty()) {
            prefix = JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX;
        }

        return prefix;
    }

    /**
     * This may be called twice during a single call to checkRequest for the same clob value if xml output is configured.
     * //todo caching based on some object identity
     *
     * @throws SQLException any problems reading the clob's stream or if the stream limit is exceeded
     */
    @NotNull
    private String getClobStringValue(final Clob clob) throws SQLException {
        Reader reader = null;
        StringWriter writer = null;
        final long maxClobSize = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_CLOB_SIZE_OUT, 10485760L);
        try {
            reader = clob.getCharacterStream();
            writer = new StringWriter(8192);
            IOUtils.copyStream(reader, writer, new Functions.UnaryVoidThrows<Long, IOException>() {
                @Override
                public void call(Long totalRead) throws IOException {
                    if (totalRead > maxClobSize) {
                        throw new IOException("CLOB value has exceeded maximum allowed size of " + maxClobSize + " bytes");
                    }
                }
            });
            // todo intern to help against duplicate calls? decide when caching is implemented.
            return writer.toString();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading CLOB: '" + ExceptionUtils.getMessage(ioe) + "'.", ExceptionUtils.getDebugException(ioe));
            throw new SQLException(ExceptionUtils.getMessage(ioe));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error reading CLOB: '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw e;
        } finally {
            ResourceUtils.closeQuietly(reader);
            ResourceUtils.closeQuietly(writer);
        }
    }

    /**
     * This may be called twice during a single call to checkRequest for the same clob value if xml output is configured.
     * //todo caching based on some object identity
     *
     * @throws SQLException any problems reading the blob's stream or if the stream limit is exceeded
     */
    @NotNull
    private byte[] getBlobValue(final Blob blob) throws SQLException {
        InputStream inputStream = null;
        ByteArrayOutputStream byteOutput = null;
        final long maxBlobSize = config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L);
        try {
            inputStream = blob.getBinaryStream();
            byteOutput = new ByteArrayOutputStream();
            IOUtils.copyStream(inputStream, byteOutput, new Functions.UnaryVoidThrows<Long, IOException>() {
                @Override
                public void call(Long totalRead) throws IOException {
                    if (totalRead > maxBlobSize) {
                        throw new IOException("BLOB value has exceeded maximum allowed size of " + maxBlobSize + " bytes");
                    }
                }
            });
            return byteOutput.toByteArray();
        } catch (IOException ioe) {
            logger.log(Level.WARNING, "Error reading BLOB: '" + ExceptionUtils.getMessage(ioe) + "'.", ExceptionUtils.getDebugException(ioe));
            throw new SQLException(ExceptionUtils.getMessage(ioe));
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Error reading BLOB: '" + ExceptionUtils.getMessage(e) + "'.", ExceptionUtils.getDebugException(e));
            throw e;
        } finally {
            ResourceUtils.closeQuietly(inputStream);
            ResourceUtils.closeQuietly(byteOutput);
        }
    }

}
