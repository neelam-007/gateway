package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;
import com.l7tech.util.RandomUtil;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.*;

import static com.l7tech.external.assertions.jdbcquery.server.ContextFreeServerJdbcQueryAssertionTest.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

// TODO: some testing for logAndAudit() is needed
// TODO: variableMapping: test with null, empty, test that mappings work...
public class ContextFreeServerJdbcQueryAssertionTest {

    static final String[] NO_VARIABLES = new String[]{};
    static final String CONNECTION_NAME = "connectionName";
    static final String SCHEMA_WITH_SPACES = "with spaces";
    static final String SCHEMA_NAME = "schemaName";
    static final String SQL_QUERY = "select nothing form nowhere";
    static final String DRIVER_CLASS_MYSQL = "mysql";
    static final String DRIVER_CLASS_ORACLE = "oracle";
    static final int MAX_RECORDS = 10;
    static final String QUERY_TIMEOUT_STRING = "10";
    static final int QUERY_TIMEOUT_INT = 10;
    static final String COLUMN_NAME_BLOB = "blob";
    final static String XML_RESULT_TAG_OPEN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\">";
    final static String XML_RESULT_TAG_CLOSE = "</L7j:jdbcQueryResult>";

    static final byte[] BLOB_CONTENT = new byte[8192];
    static {
        SecureRandom seeder = new SecureRandom();
        byte[] generatedSeed = seeder.generateSeed(128);
        System.out.println("Creating random BLOB with SecureRandom seeded with " + Arrays.toString(generatedSeed));
        new SecureRandom(generatedSeed).nextBytes(BLOB_CONTENT);
    }

    @Test
    public void shouldThrowIllegalArgumentExceptionWhenNoVariablesUsed() throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder().build();
        try {
            ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, null);
        } catch (IllegalArgumentException e) {
            assertTrue("Unexpected exception message: " + e.getMessage(),
                    e.getMessage().contains("@NotNull") && e.getMessage().contains("value"));
            return;
        }

        fail("IllegalStateException expected but not thrown");
    }

    @Test
    public void shouldThrowIllegalStateExceptionWhenApplicationContextIsNull() throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder().withVariables(NO_VARIABLES).build();
        try {
            ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, null);
        } catch (IllegalStateException e) {
            return;
        }

        fail("IllegalStateException expected but not thrown");
    }

    @Test
    public void shouldThrowPolicyAssertionExceptionWhenNoConnectionName() {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder().withVariables(NO_VARIABLES).build();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        try {
            ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        } catch (PolicyAssertionException e) {
            assertEquals("Unexpected exception message", "Assertion must supply a connection name", e.getMessage());
            return;
        }

        fail("PolicyAssertionException expected but not thrown");
    }

    @Test
    public void shouldThrowPolicyAssertionExceptionWhenNoSqlQuery() {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withConnectionName(CONNECTION_NAME)
                .build();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        try {
            ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        } catch (PolicyAssertionException e) {
            assertEquals("Unexpected exception message", "Assertion must supply a sql statement", e.getMessage());
            return;
        }

        fail("PolicyAssertionException expected but not thrown");
    }

    @Test
    public void shouldThrowPolicyAssertionExceptionSchemaContainsSpaces() {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withConnectionName(CONNECTION_NAME).withSchema(SCHEMA_WITH_SPACES)
                .withSqlQuery(SQL_QUERY).build();
        ApplicationContext applicationContext = mock(ApplicationContext.class);
        try {
            ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        } catch (PolicyAssertionException e) {
            assertEquals("JDBC Query assertion schema must not contain spaces: " + SCHEMA_WITH_SPACES, e.getMessage());
            return;
        }

        fail("PolicyAssertionException expected but not thrown");
    }

    @Test
    public void shouldRegisterForCachingWhenConnectionNameDoesNotReferenceVariablesAndSchemaIsNull()
    throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withConnectionName(CONNECTION_NAME).withSqlQuery(SQL_QUERY)
                .build();

        JdbcQueryingManager jdbcQueryingManager = mock(JdbcQueryingManager.class);
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager)
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        verify(jdbcQueryingManager).registerQueryForPossibleCaching(CONNECTION_NAME, SQL_QUERY, null);
    }

    @Test
    public void shouldRegisterForCachingWhenNeitherConnectionNameNorSchemaReferenceVariables()
    throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY)
                .build();

        JdbcQueryingManager jdbcQueryingManager = mock(JdbcQueryingManager.class);
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager)
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        verify(jdbcQueryingManager).registerQueryForPossibleCaching(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME);
    }

    @Test
    public void shouldNotRegisterForCachingWhenConnectionNameReferencesVariables() throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName("${connectionName}")
                .withSqlQuery(SQL_QUERY)
                .build();

        JdbcQueryingManager jdbcQueryingManager = mock(JdbcQueryingManager.class);
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager)
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        verify(jdbcQueryingManager, never()).registerQueryForPossibleCaching(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME);
    }

    @Test
    public void shouldNotRegisterForCachingWhenSchemaNameReferencesVariables() throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema("${schema}").withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY)
                .build();

        JdbcQueryingManager jdbcQueryingManager = mock(JdbcQueryingManager.class);
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager)
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        verify(jdbcQueryingManager, never()).registerQueryForPossibleCaching(CONNECTION_NAME, SQL_QUERY, "${schema}");
    }

    @Test
    public void shouldNotRegisterForCachingWhenConnectionNameDoesReferencesVariablesAndSchemaIsNull()
    throws PolicyAssertionException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withConnectionName(CONNECTION_NAME).withSqlQuery(SQL_QUERY)
                .build();

        JdbcQueryingManager jdbcQueryingManager = mock(JdbcQueryingManager.class);
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager)
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        verify(jdbcQueryingManager, never()).registerQueryForPossibleCaching(CONNECTION_NAME, SQL_QUERY, "${schema}");
    }

    @Test
    public void shouldThrowWhenPolicyEnforcementContextIsNull() throws PolicyAssertionException, IOException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withConnectionName(CONNECTION_NAME).withSqlQuery(SQL_QUERY)
                .build();

        JdbcQueryingManager jdbcQueryingManager = mock(JdbcQueryingManager.class);
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager)
                .build();

        try {
            ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
            assertion.checkRequest(null);
        } catch (IllegalStateException e) {
            assertEquals("Right exception type throw, but with unexpected message",
                    "Policy Enforcement Context cannot be null.", e.getMessage());
        }
    }

    @Test
    public void shouldCallResolveAsObjectListWhenGivenAuditLookupPolicyEnforcementContext()
    throws PolicyAssertionException, IOException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilderSpecific()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX)
                .withGenerateXmlResult(true)
                .withSaveResultsAsContextVariables(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        assertion.checkRequest(mock(AuditLookupPolicyEnforcementContext.class));
        verify(clientAssertion, times(1)).getResolveAsObjectList();
    }

    @Test
    public void shouldCallResolveAsObjectListWhenGivenAuditSinkPolicyEnforcementContext()
    throws PolicyAssertionException, IOException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        assertion.checkRequest(mock(AuditSinkPolicyEnforcementContext.class));
        verify(clientAssertion, times(1)).getResolveAsObjectList();
    }

    @Test
    public void shouldNotCallResolveAsObjectListWhenGivenNeither() throws PolicyAssertionException, IOException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        assertion.checkRequest(mock(PolicyEnforcementContext.class));
        verify(clientAssertion, never()).getResolveAsObjectList();
    }

    @Test
    public void shouldFailIfQueryReturnsString() throws PolicyAssertionException, IOException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenReturn("My result is a string")
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        AssertionStatus assertionStatus = assertion.checkRequest(new PolicyEnforcementContextBuilder().build());

        assertEquals("Unexpected assertion status returned: " + assertionStatus, AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void shouldFailIfResultIsIntegerZeroAndAssertionFailureEnabled()
    throws PolicyAssertionException, IOException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .withAssertionFailureEnabled(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenReturn(0)
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        AssertionStatus assertionStatus = assertion.checkRequest(new PolicyEnforcementContextBuilder().build());
        assertEquals("Unexpected assertion status returned: " + assertionStatus, AssertionStatus.FAILED, assertionStatus);
    }

    @Test
    public void shouldSetContextVariableIfResultIsIntegerZeroAndAssertionFailureDisabled()
    throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .withAssertionFailureEnabled(false)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenReturn(0)
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        AssertionStatus assertionStatus = assertion.checkRequest(policyEnforcementContext);
        assertEquals("Unexpected assertion status returned: " + assertionStatus, AssertionStatus.NONE, assertionStatus);

        Integer variableCountContextVar =  (Integer) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + "." + JdbcQueryAssertion.VARIABLE_COUNT);
        assertEquals("Unexpected context variable value", new Integer(0), variableCountContextVar);
    }

    @Test
    public void shouldSetContextVariableIfResultIsNonZeroIntegerAndAssertionFailureEnabled()
            throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .withAssertionFailureEnabled(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenReturn(101)
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        AssertionStatus assertionStatus = assertion.checkRequest(policyEnforcementContext);
        assertEquals("Unexpected assertion status returned: " + assertionStatus, AssertionStatus.NONE, assertionStatus);

        int variableCountContextVar =  (Integer) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + "." + JdbcQueryAssertion.VARIABLE_COUNT);
        assertEquals("Unexpected context variable value", 101, variableCountContextVar);
    }

    // TODO: we should add some cases for when the QueryingManager.performJdbcQuery() result is a Map

    @Test
    public void shouldOnlySetNumberOfRowsInContextWhenResultIsListAndSaveResultAsContextVariablesDisabled()
    throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns("some_column", "other_column")
                                        .addRow(new Object(), new Object())
                                        .addRow(new Object(), new Object())
                                        .addRow(new Object(), new Object()) // 3 rows
                                        .build())
                                .build())
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        assertion.checkRequest(policyEnforcementContext);

        int rows = (Integer) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + "." + JdbcQueryAssertion.VARIABLE_COUNT);
        assertEquals("Unexpected number of rows", 3, rows);
        assertEquals("Unexpected number of context variables", 1, policyEnforcementContext.getAllVariables().size());
    }

    @Test
    public void shouldPutRawBlobContentInContextWhenRequested()
            throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withSaveResultsAsContextVariables(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        assertion.checkRequest(policyEnforcementContext);

        Object[] blobContextVar = (Object[]) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + "." + COLUMN_NAME_BLOB);
        byte[] blobContent = (byte[]) blobContextVar[0];
        assertArrayEquals("Unexpected blob content", BLOB_CONTENT, blobContent);
    }

    @Test
    public void shouldPutXmlBlobContentInContextWhenRequested()
            throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        assertion.checkRequest(policyEnforcementContext);

        String blobXml = (String) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + JdbcQueryAssertion.VARIABLE_XML_RESULT);
        assertEquals("Unexpected blob content",
                XML_RESULT_TAG_OPEN + JdbcUtil.XML_RESULT_ROW_OPEN
                        + JdbcUtil.XML_RESULT_COL_OPEN + " name=\"" + COLUMN_NAME_BLOB + "\" "
                        + "type=\"java.lang.byte[]\"" + ">" + ServerJdbcQueryAssertion.getReadableHexString(BLOB_CONTENT)
                        + JdbcUtil.XML_RESULT_COL_CLOSE
                        + JdbcUtil.XML_RESULT_ROW_CLOSE
                        + XML_RESULT_TAG_CLOSE,
                blobXml);
    }

    @Test
    public void shouldPutBothXmlAndRawBlobContentInContextWhenRequested()
            throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .withSaveResultsAsContextVariables(true)
                .build();

        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);
        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        AssertionStatus status = assertion.checkRequest(policyEnforcementContext);
        assertEquals("Unexpected assertion status", AssertionStatus.NONE, status);

        String blobXml = (String) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + JdbcQueryAssertion.VARIABLE_XML_RESULT);
        assertEquals("Unexpected blob content",
                XML_RESULT_TAG_OPEN + JdbcUtil.XML_RESULT_ROW_OPEN
                        + JdbcUtil.XML_RESULT_COL_OPEN + " name=\"" + COLUMN_NAME_BLOB + "\" "
                        + "type=\"java.lang.byte[]\"" + ">" + ServerJdbcQueryAssertion.getReadableHexString(BLOB_CONTENT)
                        + JdbcUtil.XML_RESULT_COL_CLOSE
                        + JdbcUtil.XML_RESULT_ROW_CLOSE
                        + XML_RESULT_TAG_CLOSE,
                blobXml);

        Object[] blobContextVar = (Object[]) policyEnforcementContext.getVariable(
                JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX + "." + COLUMN_NAME_BLOB);
        byte[] blobContent = (byte[]) blobContextVar[0];
        assertArrayEquals("Unexpected blob content", BLOB_CONTENT, blobContent);
    }

    @Test
    public void shouldHitDatabaseOnlyOnceWhenReadingOracleBlob() throws PolicyAssertionException, IOException, NoSuchVariableException {
        JdbcQueryAssertion clientAssertion = new JdbcQueryAssertionBuilder()
                .withVariables(NO_VARIABLES).withSchema(SCHEMA_NAME).withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY).withMaxRecords(MAX_RECORDS).withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withSaveResultsAsContextVariables(true)
                .withNamingMap(CollectionUtils.MapBuilder.<String, String>builder()
                        .put(COLUMN_NAME_BLOB, COLUMN_NAME_BLOB)
                        .map())
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX).withGenerateXmlResult(true)
                .build();

        JdbcQueryingManager jdbcQueryingManager;
        ApplicationContext applicationContext = new ApplicationContextBuilder()
                .withJdbcQueryingManager(jdbcQueryingManager = new JdbcQueryingManagerBuilder()
                        .<SqlRowSet>whenPerformJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                                QUERY_TIMEOUT_INT, new ArrayList<>())
                        .thenLazyReturn(new ListBuilder<SqlRowSet>()
                                .add(new SqlRowSetBuilder()
                                        .withColumns(COLUMN_NAME_BLOB)
                                        .addRow(new BlobBuilder().withBinaryStreamContaining(BLOB_CONTENT).build())
                                        .build()))
                        .build())
                .withJdbcConnectionManager(new JdbcConnectionManagerBuilder()
                        .withConnection(CONNECTION_NAME, new JdbcConnectionBuilder()
                                .withDriverClass(DRIVER_CLASS_ORACLE)
                                .build())
                        .build())
                .withConfig(new ConfigBuilder()
                        .withLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, 10485760L)
                        .build())
                .build();

        ServerJdbcQueryAssertion assertion = new ServerJdbcQueryAssertion(clientAssertion, applicationContext);

        PolicyEnforcementContext policyEnforcementContext = new PolicyEnforcementContextBuilder().build();
        AssertionStatus assertionStatus = assertion.checkRequest(policyEnforcementContext);

        // let's make sure we went to the DB only once
        verify(jdbcQueryingManager, times(1)).performJdbcQuery(CONNECTION_NAME, SQL_QUERY, SCHEMA_NAME, MAX_RECORDS,
                QUERY_TIMEOUT_INT, new ArrayList<>());

        assertEquals("Unexpected assertion status returned: " + assertionStatus, AssertionStatus.NONE, assertionStatus);
    }
}

class ApplicationContextBuilder {

    private final ApplicationContext applicationContext;

    ApplicationContextBuilder() {
        applicationContext = mock(ApplicationContext.class);
    }

    ApplicationContextBuilder withJdbcQueryingManager(JdbcQueryingManager jdbcQueryingManager) {
        when(applicationContext.getBean("jdbcQueryingManager", JdbcQueryingManager.class))
                .thenReturn(jdbcQueryingManager);
        return this;
    }

    ApplicationContextBuilder withJdbcConnectionManager(JdbcConnectionManager jdbcConnectionManager) {
        when(applicationContext.getBean("jdbcConnectionManager", JdbcConnectionManager.class))
                .thenReturn(jdbcConnectionManager);
        return this;
    }

    ApplicationContextBuilder withConfig(Config config) {
        when(applicationContext.getBean("serverConfig", Config.class)).thenReturn(config);
        return this;
    }

    ApplicationContext build() {
        return applicationContext;
    }
}


/* *****************************************************************
 * ******************      AUXILIARY CLASSES      ******************
 * *****************************************************************/

class JdbcQueryAssertionBuilderSpecific extends JdbcQueryAssertionBuilder {

    JdbcQueryAssertionBuilder withBasics() {
        return this
                .withVariables(NO_VARIABLES)
                .withSchema(SCHEMA_NAME)
                .withConnectionName(CONNECTION_NAME)
                .withSqlQuery(SQL_QUERY)
                .withMaxRecords(MAX_RECORDS)
                .withQueryTimeout(QUERY_TIMEOUT_STRING)
                .withVariablePrefix(JdbcQueryAssertion.DEFAULT_VARIABLE_PREFIX);
    }
}

class JdbcConnectionManagerBuilder {
    private final JdbcConnectionManager jdbcConnectionManager;

    JdbcConnectionManagerBuilder() {
        jdbcConnectionManager = mock(JdbcConnectionManager.class);
    }

    JdbcConnectionManagerBuilder withConnection(String connectionName, JdbcConnection connection) {
        try {
            when(jdbcConnectionManager.getJdbcConnection(connectionName)).thenReturn(connection);
        } catch (FindException e) {
            fail("Unexpected FindException: " + e.getMessage());
        }
        return this;
    }

    JdbcConnectionManager build() {
        return jdbcConnectionManager;
    }
}

class JdbcConnectionBuilder {
    private final JdbcConnection jdbcConnection;

    JdbcConnectionBuilder() {
        jdbcConnection = mock(JdbcConnection.class);
    }

    JdbcConnectionBuilder withDriverClass(String driverClass) {
        when(jdbcConnection.getDriverClass()).thenReturn(driverClass);
        return this;
    }

    JdbcConnection build() {
        return jdbcConnection;
    }
}

class BlobBuilder {
    private final Blob blob;

    BlobBuilder() {
        blob = mock(Blob.class);
    }

    /**
     * Creates a new binary stream containing the content passed in every time it is called.
     * This is to imitate the behaviour of the DataDirect 5.1.4 Oracle drivers,
     * which return a sql result containing a blob object whose InputStream can be read only once.
     * To read again, you have to go to the database again.
     * In order to emulate the process of 'getting a new blob' when the database re-read,
     * we have this method that will <b>ALWAYS</b> return a fresh InputStream that will throw an exception
     * if you try to read from it after it's closed.
     * This is not quite the same as ensuring that a fresh copy is returned ONLY when a new database read happens,
     * but since this little class should only be used from
     * {@link ContextFreeServerJdbcQueryAssertionTest},
     * and is only called form code that set up the mock response to reading form the database, it will work,
     * <b>in this context</b>.
     * Re-use at your own risk.
     * @param content
     * @return
     */
    BlobBuilder withBinaryStreamContaining(final byte[] content) {
        try {
            when(blob.getBinaryStream()).thenReturn(new MockInputStream(content));
            return this;
        } catch (SQLException e) {
            throw new RuntimeException("Unexpected SQLException: " + e.getMessage());
        }
    }

    Blob build() {
        return blob;
    }
}

class MockInputStream extends ByteArrayInputStream {

    private boolean closed;

    MockInputStream(byte[] bytes) {
        super(bytes);
        closed = false;
    }

    @Override
    public int read(byte[] bytes) throws IOException {
        if (!closed) {
            return super.read(bytes);
        }
        throw new IOException("[l7tech][Oracle JDBC Driver][Oracle]ORA-22922: nonexistent LOB value");
    }

    @Override
    public void close() throws IOException {
        closed = true;
        super.close();
    }
}

class ConfigBuilder {
    private final Config config;

    ConfigBuilder() {
        config = mock(Config.class);
    }

    ConfigBuilder withLongProperty(String name, long value) {
        when(config.getLongProperty(eq(name), anyLong())).thenReturn(value);
        return this;
    }

    Config build() {
        return config;
    }
}
