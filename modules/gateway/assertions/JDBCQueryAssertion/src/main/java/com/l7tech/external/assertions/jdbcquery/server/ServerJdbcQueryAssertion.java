package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
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
    private final static String XML_RESULT_TAG_OPEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
            "<L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\">";
    private final static String XML_RESULT_TAG_CLOSE = "</L7j:jdbcQueryResult>";

    private final String[] variablesUsed;
    private final JdbcQueryingManager jdbcQueryingManager;
    private final JdbcConnectionManager jdbcConnectionManager;
    private final Config config;
    private final Map<String, JdbcConnection> jdbcConnectionCache;

    public ServerJdbcQueryAssertion(JdbcQueryAssertion assertion, ApplicationContext context)
            throws PolicyAssertionException {
        super(assertion);

        if (context == null) {
            throw new IllegalStateException("Application context cannot be null.");
        }

        variablesUsed = assertion.getVariablesUsed();
        jdbcQueryingManager = context.getBean("jdbcQueryingManager", JdbcQueryingManager.class);
        jdbcConnectionManager = context.getBean("jdbcConnectionManager", JdbcConnectionManager.class);
        config = validated(context.getBean("serverConfig", Config.class));
        jdbcConnectionCache = new HashMap<>();

        if (assertion.getConnectionName() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a connection name");
        }
        if (assertion.getSqlQuery() == null) {
            throw new PolicyAssertionException(assertion, "Assertion must supply a sql statement");
        }
        if (assertion.getSchema() != null && !Syntax.isAnyVariableReferenced(assertion.getSchema()) && assertion.getSchema().matches(".*\\s.*")) {
            throw new PolicyAssertionException(assertion, "JDBC Query assertion schema must not contain spaces: " + assertion.getSchema());
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
            final Object result = performQuery(context);

            // Analyze the result type and perform a corresponding action.
            if (result instanceof String) {
                logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, (String) result);
                return AssertionStatus.FAILED;

            } else if (result instanceof Integer) {
                processInteger(context, result);

            } else if (result instanceof Map) {
                processMap(context, xmlResult, result);

            } else if (result instanceof List) {
                processList(context, xmlResult, result);

            } else {
                throw new IllegalStateException("Invalid returned result type, " + result.getClass().getSimpleName());
            }

        } catch (FindException | SchemaNotSupportedException | QueryTimeoutIsNotValidIntegerException
                | NoQueryResultAssertionFailedException e) {
            return AssertionStatus.FAILED;

        } catch (SQLException | VariableNameSyntaxException e) {
            logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, e.getMessage());
            return AssertionStatus.FAILED;
        }

        if (assertion.isGenerateXmlResult()) {
            xmlResult.append(XML_RESULT_TAG_CLOSE);
            context.setVariable(getVariablePrefix(context) + JdbcQueryAssertion.VARIABLE_XML_RESULT, xmlResult.toString());
        }
        return AssertionStatus.NONE;
    }

    private Object performQuery(PolicyEnforcementContext context)
            throws QueryTimeoutIsNotValidIntegerException, PolicyAssertionException, SchemaNotSupportedException, FindException {
        final Pair<String, List<Object>> pair;
        if (context instanceof AuditLookupPolicyEnforcementContext
                || context instanceof AuditSinkPolicyEnforcementContext) {
            pair = getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), context,
                    assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(),
                    assertion.getResolveAsObjectList(), getAudit());
        } else {
            pair = getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), context,
                    assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), getAudit());
        }

        final Map<String, Object> variableMap = context.getVariableMap(variablesUsed, getAudit());
        final String connName = getConnectionName(variableMap);
        final String query = pair.left;
        final String schema = getSchema(connName, variableMap);
        int maxRecords = assertion.getMaxRecords();
        int queryTimeout = getQueryTimeout(variableMap);
        List<Object> params = pair.right;
        return jdbcQueryingManager.performJdbcQuery(connName, query, schema, maxRecords, queryTimeout, params);
    }

    private void processInteger(PolicyEnforcementContext context, Object result)
            throws NoQueryResultAssertionFailedException {
        int num = (Integer) result;
        if (num == 0 && assertion.isAssertionFailureEnabled()) {
            logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
            throw new NoQueryResultAssertionFailedException();
        } else {
            context.setVariable(getVariablePrefix(context) + "." + JdbcQueryAssertion.VARIABLE_COUNT, result);
        }
    }

    private void processMap(PolicyEnforcementContext context, StringBuilder xmlResult, Object result)
            throws SQLException, NoQueryResultAssertionFailedException {
        @SuppressWarnings("unchecked")
        Map<String, List<Object>> map = (Map) result;

        int affectedRows = setContextVariables(map, context);
        if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
            logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
            throw new NoQueryResultAssertionFailedException();
        }
        if (assertion.isGenerateXmlResult()) {
            JdbcUtil.buildXmlResultString(map, xmlResult);
        }
    }

    private void processList(PolicyEnforcementContext context, StringBuilder xmlResult, Object result)
            throws SQLException, NoQueryResultAssertionFailedException {
        @SuppressWarnings("unchecked")
        final List<SqlRowSet> list = (List) result;

        int affectedRows = 0;
        int resultSetCount = 0;
        final int totalResultSets = list.size();
        BlobContainer blobs = new BlobContainer();
        for (SqlRowSet rowSet : list) {
            affectedRows += setContextVariables(rowSet, context, resultSetCount, totalResultSets, blobs);
            if (assertion.isGenerateXmlResult()) {
                buildXmlResultString(blobs, resultSetCount, rowSet, xmlResult);
            }
            resultSetCount++;
        }

        if (totalResultSets > 1) {
            String variablePrefix = getVariablePrefix(context);
            context.setVariable(variablePrefix + "." + JdbcQueryAssertion.MULTIPLE_VARIABLE_COUNT, affectedRows);
            context.setVariable(variablePrefix + "." + JdbcQueryAssertion.MULTIPLE_RESULTSET_COUNT, totalResultSets);
        }

        if (affectedRows == 0 && assertion.isAssertionFailureEnabled()) {
            logAndAudit(AssertionMessages.JDBC_NO_QUERY_RESULT_ASSERTION_FAILED, assertion.getConnectionName());
            throw new NoQueryResultAssertionFailedException();
        }
    }

    private String getConnectionName(Map<String, Object> variableMap) {
        return ExpandVariables.process(assertion.getConnectionName(), variableMap, getAudit());
    }

    private String getSchema(String connectionName, Map<String, Object> variableMap)
            throws PolicyAssertionException, SchemaNotSupportedException, FindException {
        String driverClass = getConnectionDriverClass(connectionName);
        final String schema = assertion.getSchema() != null ? ExpandVariables.process(assertion.getSchema(), variableMap, getAudit()) : null;
        if (schema != null && schema.matches(".*\\s.*")) {
            throw new PolicyAssertionException(assertion, "JDBC Query assertion schema must not contain spaces: " + schema);
        }
        if (schema != null && !(driverClass.contains("oracle") || driverClass.contains("sqlserver"))) {
            logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, "Schema value given but JDBC connection does not support it. Connection name: " + connectionName);
            throw new SchemaNotSupportedException();
        }
        return schema;
    }

    private String getConnectionDriverClass(String connectionName) throws FindException {
        try {
            JdbcConnection connection = getJdbcConnectionFromCache(connectionName);
            return connection.getDriverClass();

        } catch (FindException e) {
            logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED,
                    "Could not find JDBC connection: " + connectionName);
            throw e;
        }
    }

    private JdbcConnection getJdbcConnectionFromCache(String connectionName) throws FindException {
        synchronized (jdbcConnectionCache) {
            if (jdbcConnectionCache.containsKey(connectionName)) {
                return jdbcConnectionCache.get(connectionName);
            }

            final JdbcConnection connection = jdbcConnectionManager.getJdbcConnection(connectionName);
            if (connection == null) {
                throw new FindException();
            }

            jdbcConnectionCache.put(connectionName, connection);
            return connection;
        }
    }

    private int getQueryTimeout(final Map<String, Object> variableMap) throws QueryTimeoutIsNotValidIntegerException {
        final String queryTimeoutString = (assertion.getQueryTimeout() != null) ? assertion.getQueryTimeout() : "0";
        final String resolvedQueryTimeout = ExpandVariables.process(queryTimeoutString, variableMap, getAudit());
        if (!ValidationUtils.isValidInteger(resolvedQueryTimeout, false, 0, Integer.MAX_VALUE)) {
            logAndAudit(AssertionMessages.JDBC_QUERYING_FAILURE_ASSERTION_FAILED, "Invalid resolved value for query timeout: " + resolvedQueryTimeout);
            throw new QueryTimeoutIsNotValidIntegerException();
        }
        return Integer.parseInt(resolvedQueryTimeout);
    }

    /**
     * process stored procedure results
     */
    void buildXmlResultString(BlobContainer blobs, int resultSetNumber, final SqlRowSet resultSet,
                              final StringBuilder xmlResult)
            throws SQLException {
        int row = 0;
        resultSet.first();
        final StringBuilder records = new StringBuilder();

        while (resultSet.next() && row < assertion.getMaxRecords()) {
            records.append(JdbcUtil.XML_RESULT_ROW_OPEN);
            SqlRowSetMetaData metaData = resultSet.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                processRow(blobs, resultSet, records, metaData.getColumnName(i), row);
            }
            records.append(JdbcUtil.XML_RESULT_ROW_CLOSE);
        }
        if (resultSetNumber > 0) {
            xmlResult.append("<resultSet" + resultSetNumber + ">");
            xmlResult.append(records);
            xmlResult.append("</resultSet" + resultSetNumber + ">");
        } else {
            xmlResult.append(records);
        }
    }

    private void processRow(BlobContainer blobs, SqlRowSet resultSet, StringBuilder records, String columnName, int row)
            throws SQLException {
        Object value = resultSet.getObject(columnName);
        String colType = JdbcUtil.EMPTY_STRING;
        if (value != null) {
            if (value instanceof byte[]) {
                colType = "type=\"java.lang.byte[]\"";
                value = getReadableHexString((byte[]) value);
            } else if (value instanceof Clob) {
                value = new String(blobs.get(row).get(columnName)).intern();
            } else if (value instanceof Blob) {
                colType = "type=\"java.lang.byte[]\"";
                value = getReadableHexString(blobs.get(row).get(columnName));
            } else {
                colType = "type=\"" + value.getClass().getName() + "\"";
            }
        }
        records.append(JdbcUtil.XML_RESULT_COL_OPEN + " name=\"" + columnName + "\" " + colType + ">");
        if (value != null) {
            records.append(JdbcUtil.handleSpecialXmlChar(value));
        } else {
            records.append(JdbcUtil.XML_NULL_VALUE);
        }
        records.append(JdbcUtil.XML_RESULT_COL_CLOSE);
    }

    /**
     * Converts byte array to readable hexidecimal string. Ie: "12 34 56 78 9A BC DE F0"
     * @param byteArray the array to convert
     * @return a string in readable hex
     */
    static String getReadableHexString(byte[] byteArray) {
        StringBuilder sb = new StringBuilder();
        for (byte b : byteArray) {
            sb.append(String.format("%02X ", b));
        }
        return  sb.toString();
    }

    /**
     * build newNameMapping from map type resultSet
     */
    Map<String, String> getNewMapping(Map<String, List<Object>> resultSet) {
        Map<String, String> namingMap = assertion.getNamingMap();
        Map<String, String> newNamingMap = new TreeMap<>();

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
        Map<String, String> newNamingMap = new TreeMap<>();

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
        if (context == null) {
            throw new IllegalStateException("Policy Enforcement Context cannot be null.");
        }

        final boolean saveResults = assertion.isSaveResultsAsContextVariables();
        final Map<String, String> newNamingMap = saveResults ? getNewMapping(resultSet) : null;
        final String varPrefix = getVariablePrefix(context);
        int row = 0;
        for (String columnName : resultSet.keySet()) {
            String varName = saveResults ? varPrefix + "." + newNamingMap.get(columnName.toLowerCase()) : null;
            if (saveResults && logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, varName + Arrays.toString(resultSet.get(columnName).toArray()));
            }
            if (resultSet.get(columnName) != null) {
                row = resultSet.get(columnName).size();
                if (saveResults){
                    context.setVariable(varName, resultSet.get(columnName).toArray());
                }
            } else {
                logger.log(Level.FINER, columnName + " result list was null");
            }
        }
        context.setVariable(varPrefix + "." + JdbcQueryAssertion.VARIABLE_COUNT, row);
        return row;
    }

    int setContextVariables(SqlRowSet resultSet, PolicyEnforcementContext context, int resultSetCount,
                            int totalResultSets, BlobContainer blobs) throws SQLException {
        if (context == null) {
            throw new IllegalStateException("Policy Enforcement Context cannot be null.");
        }

        final boolean saveResults = assertion.isSaveResultsAsContextVariables();
        final Map<String, String> newNamingMap = getNewMapping(resultSet);
        final Map<String, List<Object>> results = saveResults ? getResults(newNamingMap) : null;

        int maxRecords = assertion.getMaxRecords();
        int row = 0;
        resultSet.first();
        while (resultSet.next() && row < maxRecords) {
            populateBlobs(row, resultSet, newNamingMap, blobs);
            if (saveResults) {
                populateResults(row, resultSet, newNamingMap, results, blobs);
            }
            row++;
        }

        // Assign the results to context variables
        String varPrefix = getVariablePrefix(context);
        final String resultSetPrefix = getResultVariablePrefix(resultSetCount, totalResultSets);
        if (saveResults) {
            saveResults(context, newNamingMap, results, varPrefix, resultSetPrefix);
        }
        context.setVariable(varPrefix + resultSetPrefix + "." + JdbcQueryAssertion.VARIABLE_COUNT, row);

        return row;
    }

    private void saveResults(PolicyEnforcementContext context, Map<String, String> newNamingMap,
                             Map<String, List<Object>> results, String varPrefix, String resultSetPrefix) {
        for (String column : results.keySet()) {
            String varName = varPrefix + resultSetPrefix + "." + newNamingMap.get(column.toLowerCase());
            if (logger.isLoggable(Level.FINER)) {
                logger.log(Level.FINER, varName + Arrays.toString(results.get(column).toArray()));
            }
            context.setVariable(varName, results.get(column).toArray());
        }
    }

    private void populateBlobs(int rowNumber, SqlRowSet resultSet, Map<String, String> newNamingMap,
                               BlobContainer blobs)
            throws SQLException {
        for (String oldColumnName : newNamingMap.keySet()) {
            final Object value = resultSet.getObject(oldColumnName);
            final String newColumnName = newNamingMap.get(oldColumnName);
            // TODO - what other types may not be directly applicable as-is?
            if (value instanceof Clob) {
                String clob = getClobStringValue((Clob) value);
                if (!blobs.containsKey(rowNumber)) {
                    blobs.put(rowNumber, new HashMap<String, byte[]>());
                }
                blobs.get(rowNumber).put(newColumnName, clob.getBytes());
            } else if (value instanceof Blob) {
                byte[] blob = getBlobValue((Blob) value);
                if (!blobs.containsKey(rowNumber)) {
                    blobs.put(rowNumber, new HashMap<String, byte[]>());
                }
                blobs.get(rowNumber).put(newColumnName, blob);
            }
        }
    }

    private void populateResults(int rowNumber, SqlRowSet resultSet, Map<String, String> newNamingMap,
                                       Map<String, List<Object>> results, BlobContainer blobs)
            throws SQLException {
        for (String oldColumnName : newNamingMap.keySet()) {
            String newColumnName = newNamingMap.get(oldColumnName);
            final List<Object> rows = results.get(oldColumnName);
            final Object value = resultSet.getObject(oldColumnName);
            // TODO - what other types may not be directly applicable as-is?
            if (value instanceof Clob) {
                rows.add(new String(blobs.get(rowNumber).get(newColumnName)).intern());
            } else if (value instanceof Blob) {
                rows.add(blobs.get(rowNumber).get(newColumnName));
            } else {
                rows.add(value);
            }
        }
    }

    private Map<String, List<Object>> getResults(Map<String, String> newNamingMap) {
        Map<String, List<Object>> results;
        results = new HashMap<>(newNamingMap.size());
        for (String column : newNamingMap.keySet()) {
            results.put(column, new ArrayList<>());
        }
        return results;
    }

    //- PRIVATE

    private String getResultVariablePrefix(int i, int size) {
        if (size == 1) {
            return JdbcUtil.EMPTY_STRING;
        }
        return JdbcQueryAssertion.VARIABLE_RESULTSET + (i+1);
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
                    if (maxClobSize > 0 && totalRead > maxClobSize) {
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
                    if (maxBlobSize > 0 && totalRead > maxBlobSize) {
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

    private Config validated(final Config config) {
        final ValidatedConfig vc = new ValidatedConfig(config, logger);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MAX_CLOB_SIZE_OUT, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MAX_CLOB_SIZE_OUT, Long.MAX_VALUE);

        vc.setMinimumValue(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 0);
        vc.setMaximumValue(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, Long.MAX_VALUE);

        return vc;
    }

    private static class QueryTimeoutIsNotValidIntegerException extends Exception {}
    private static class SchemaNotSupportedException extends Exception {}
    private static class NoQueryResultAssertionFailedException extends Exception {}

    static class BlobContainer extends HashMap<Integer, Map<String, byte[]>> {

    }

}
