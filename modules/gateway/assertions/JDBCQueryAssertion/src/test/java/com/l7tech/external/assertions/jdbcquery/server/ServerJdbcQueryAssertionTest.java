package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.audit.AuditLookupPolicyEnforcementContext;
import com.l7tech.server.audit.AuditSinkPolicyEnforcementContext;
import com.l7tech.server.jdbc.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.sql.*;
import java.util.*;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author ghuang
 * @author rraquepo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerJdbcQueryAssertionTest {
    @Autowired
    private ApplicationContext appCtx;

    private PolicyEnforcementContext peCtx;

    private JdbcQueryAssertion assertion;
    
    private JdbcConnectionManager connectionManager;

    private Config config;

    @Mock
    private JdbcConnectionManager jdbcConnectionManager;
    @Mock
    private JdbcConnection jdbcConnection;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        // Get the policy enforcement context
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        final String connectionName = "mockDb";

        //create assertion
        assertion = new JdbcQueryAssertion();
        assertion.setConnectionName(connectionName);
        //test can change this as needed. A sql query is always needed.
        assertion.setSqlQuery("select * from mytable");

        final JdbcConnection connection = new JdbcConnection();
        connection.setName(connectionName);
        connection.setDriverClass("com.mysql.jdbc.Driver");

        connectionManager = new JdbcConnectionManagerStub(connection);
        config = (Config) appCtx.getBean("serverConfig");

        appCtx = Mockito.spy(appCtx);
        Mockito.doReturn(jdbcConnectionManager).when(appCtx).getBean(Matchers.eq("jdbcConnectionManager"), Matchers.eq(JdbcConnectionManager.class));
        when(jdbcConnectionManager.getJdbcConnection(Matchers.eq(connectionName))).thenReturn(jdbcConnection);
        when(jdbcConnection.getDriverClass()).thenReturn("com.mysql.jdbc.Driver");
    }

    /**
     * Test getting a plain query statement and the prepared statement parameters.
     *
     * @throws PolicyAssertionException
     */
    @Test
    public void testPlainQueryStatementAndPreparedStatementParameters() throws PolicyAssertionException {

        final String department = "Production";
        final String[] employee_info = new String[] {"John", "Male", "45"};

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_info", employee_info);

        String query = "SELECT * FROM employees " +
            "WHERE employee_department = ${var_dept} " +
            "AND name = ${var_info[0]} " +
            "ADN sex = ${var_info[1]} " +
            "AND age = ${var_info[2]}";
        assertion.setSqlQuery(query);

        final String expectedPlainQuery = "SELECT * FROM employees WHERE employee_department = ? AND name = ? ADN sex = ? AND age = ?";
        final Pair<String,List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
        final String actualPlainQuery = pair.left;
        final List<Object> params = pair.right;

        // Check the query statement
        assertEquals("Correct plain query produced", expectedPlainQuery, actualPlainQuery);

        // Check the prepared statement parameters
        assertEquals("Employee Department found", department, params.get(0));
        assertEquals("Employee Name found", employee_info[0], params.get(1));
        assertEquals("Employee Sex found", employee_info[1], params.get(2));
        assertEquals("Employee Age found", employee_info[2], params.get(3));
    }

    @Test
    public void shouldReturnConstructedQueryWhenMultiValueParametersUsed() throws Exception {

        final String department = "Production";
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        employeeIds.add(3);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", employeeIds);

        String query = "select * from employees where id in (${var_ids}) and department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(true);

        String expectedPlainQuery = "select * from employees where id in (?, ?, ?) and department = ?";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
        final String actualPlainQuery = pair.left;
        final List<Object> params = pair.right;

        assertEquals(expectedPlainQuery, actualPlainQuery);
        assertEquals(employeeIds.get(0), params.get(0));
        assertEquals(employeeIds.get(1), params.get(1));
        assertEquals(employeeIds.get(2), params.get(2));
        assertEquals(department, params.get(3));
    }


    @Test
     public void shouldTestMultivaluedVariableExpansionWhenAllowMultivalueTurnedOff() throws Exception {
        final String department = "Production";
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        employeeIds.add(3);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", employeeIds);

        String query = "select * from employees where id in (${var_ids}) and department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(false);

        String expectedPlainQuery = "select * from employees where id in (?) and department = ?";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
        final String actualPlainQuery = pair.left;
        final List<Object> params = pair.right;

        assertEquals(expectedPlainQuery, actualPlainQuery);
        assertEquals("1, 2, 3", params.get(0));
        assertEquals(department, params.get(1));
     }

    @Test
    public void shouldReturnConstructedQueryWhenTheSameParameterUsedTwiceAndMultiValueParametersUsed() throws Exception {

        final String department = "Production";
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        employeeIds.add(3);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", employeeIds);

        String query = "select * from employees e1, employees e2 where e1.id in (${var_ids}) and e2.id in (${var_ids}) and e1.department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(true);

        String expectedPlainQuery = "select * from employees e1, employees e2 where e1.id in (?, ?, ?) and e2.id in (?, ?, ?) and e1.department = ?";
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
        final String actualPlainQuery = pair.left;
        final List<Object> params = pair.right;

        assertEquals(expectedPlainQuery, actualPlainQuery);
        assertEquals(employeeIds.get(0), params.get(0));
        assertEquals(employeeIds.get(1), params.get(1));
        assertEquals(employeeIds.get(2), params.get(2));
        assertEquals(employeeIds.get(0), params.get(3));
        assertEquals(employeeIds.get(1), params.get(4));
        assertEquals(employeeIds.get(2), params.get(5));
        assertEquals(department, params.get(6));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowExceptionWhenVariableValueNotFound() throws Exception {
        final List<Object> employeeIds = new ArrayList<Object>();
        employeeIds.add(1);
        employeeIds.add(2);
        assertion.setAllowMultiValuedVariables(true);

        peCtx.setVariable("var_dept", "department");
        peCtx.setVariable("var_ids", "1");

        String query = "select * from employees where id=${var_ids[3]} and department = ${var_dept}";
        assertion.setSqlQuery(query);
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
    }

    @Test
    public void shouldReturnEmptyVaslueWhenVariableIndexOutOfBounds() throws Exception {
        final List<Object> employeeIds = new ArrayList<Object>();
        final String department = "Production";
        employeeIds.add(1);
        employeeIds.add(2);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", "1");

        String query = "select * from employees where id=${var_ids[3]} and department = ${var_dept}";
        assertion.setSqlQuery(query);
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
        final String actualQuery = pair.left;
        final List<Object> params = pair.right;

        assertEquals("select * from employees where id=? and department = ?", actualQuery);
        assertEquals("", params.get(0));
        assertEquals(department, params.get(1));
        
    }
    
    @Test
    public void shouldReturnUnalteredQueryStringWhenNoContextVariablesPresent() throws Exception {
        String expectedQuery = "select * from employees where id=1 and department = 'Production'";
        assertion.setSqlQuery(expectedQuery);
        final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(expectedQuery, peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
        final String actualQuery = pair.left;
        final List<Object> params = pair.right;

        assertEquals(expectedQuery, actualQuery);
        assertTrue(params.size() == 0);

    }

    /**
     * Test querying results and context variable naming and creation.
     *
     * @throws PolicyAssertionException
     * @throws SQLException
     * @throws NoSuchVariableException
     */
    @Test
    public void testQueryResultContextVariables() throws PolicyAssertionException, SQLException, NoSuchVariableException {
        Map<String, String> namingMap = new TreeMap<String, String>();
        String originalDeptColumnName = MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[1];
        namingMap.put(originalDeptColumnName, "dept");

        assertion.setNamingMap(namingMap);
        assertion.setMaxRecords(MockJdbcDatabaseManager.MOCK_MAX_ROWS);

        JdbcQueryingManager jdbcQueryingManager = (JdbcQueryingManager) appCtx.getBean("jdbcQueryingManager");
        SqlRowSet resultSet = jdbcQueryingManager.getMockSqlRowSet();

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, appCtx);
        int numOfRecords = serverAssertion.setContextVariables(resultSet, peCtx,"");

        // Check the number of returned records by the query
        assertEquals("The number of returned numOfRecords matched", MockJdbcDatabaseManager.MOCK_MAX_ROWS, numOfRecords);

        // Check if the context variable 'department' has been renamed to 'dept'.
        Object deptCtxVar = peCtx.getVariable("jdbcQuery.dept");
        assertNotNull("The context variable successfully renamed from '" + originalDeptColumnName + "' to 'dept'", deptCtxVar);

        // Check context variables creation
        assertArrayEquals("All names matched", MockJdbcDatabaseManager.MOCK_NAMES, (Object[])peCtx.getVariable("jdbcQuery.name"));
        assertArrayEquals("All departments matched", MockJdbcDatabaseManager.MOCK_DEPTS, (Object[])peCtx.getVariable("jdbcQuery.dept"));
        assertArrayEquals("All sexes matched", MockJdbcDatabaseManager.MOCK_SEXES, (Object[])peCtx.getVariable("jdbcQuery.sex"));
        assertArrayEquals("All ages matched", MockJdbcDatabaseManager.MOCK_AGES, (Object[])peCtx.getVariable("jdbcQuery.age"));
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize( XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize( XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    //really test from ServerJdbcQueryAssertion
    //by @author rraquepo

    private ServerPolicyFactory serverPolicyFactory;
    @Mock
    private JdbcQueryingManager jdbcQueryingManager;

    @BugNumber(12457)
    @Test
    public void testInvalidDriverClass() throws Exception {
        MockitoAnnotations.initMocks(this);

        final String connectionName = "TestConnection";
        final JdbcConnection connection = new JdbcConnection();
        connection.setName(connectionName);
        connection.setDriverClass("test.driver.class");

        final JdbcConnectionManager cm = new JdbcConnectionManagerStub(connection);
        JdbcConnectionPoolManager cpm = new JdbcConnectionPoolManager(cm);
        cpm.afterPropertiesSet();
        final JdbcQueryingManager qm = new JdbcQueryingManagerImpl(cpm, cm, config, new TimeSource());

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", qm);
            put("jdbcConnectionManager", cm);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);

        assertion.setConnectionName(connectionName);
        assertion.setSqlQuery("select * from mytable");
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FAILED, result);

    }

    @BugNumber(12457)
    @Test
    public void testDriverClassNotFound() throws Exception {
        MockitoAnnotations.initMocks(this);
        final String connectionName = "TestConnection";
        final JdbcConnection connection = new JdbcConnection();
        connection.setName(connectionName);
        connection.setDriverClass("test.driver.class");

        final JdbcConnectionManager cm = new JdbcConnectionManagerStub();
        JdbcConnectionPoolManager cpm = new JdbcConnectionPoolManager(cm);
        cpm.afterPropertiesSet();
        final JdbcQueryingManager qm = new JdbcQueryingManagerImpl(cpm, cm, config, new TimeSource());

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", qm);
            put("jdbcConnectionManager", cm);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);

        assertion.setConnectionName(connectionName);
        assertion.setSqlQuery("select * from mytable");
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus result = sass.checkRequest(peCtx);
        assertEquals(AssertionStatus.FAILED, result);

    }

    @BugNumber(12512)
    @Test
    public void testXmlResult() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockResults());
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        assertion.setSqlQuery("SELECT * FROM myTest");
        assertion.setGenerateXmlResult(true);
        assertion.setConnectionName("mockDb");

        //test/touch SELECT query related code
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        String xmlResult = peCtx.getVariable("jdbcQuery.xmlResult").toString();
        assertNotNull(xmlResult);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\"><L7j:row><L7j:col  name=\"id\" type=\"java.lang.Integer\">1</L7j:col><L7j:col  name=\"name\" type=\"java.lang.String\">name1</L7j:col><L7j:col  name=\"value\" type=\"java.lang.String\">value1</L7j:col></L7j:row></L7j:jdbcQueryResult>";
        assertEquals(expected, xmlResult);
        Object[] idObj = (Object[]) peCtx.getVariable("jdbcQuery.id");
        Object[] nameObj = (Object[]) peCtx.getVariable("jdbcQuery.name");
        Object[] valueObj = (Object[]) peCtx.getVariable("jdbcQuery.value");
        assertEquals(1, idObj[0]);
        assertEquals("name1", nameObj[0]);
        assertEquals("value1", valueObj[0]);

        //test/touch SELECT query related code, with special character result
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockSpecialResults());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        xmlResult = peCtx.getVariable("jdbcQuery.xmlResult").toString();
        assertNotNull(xmlResult);
        expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\"><L7j:row><L7j:col  name=\"id\" type=\"java.lang.Integer\">2</L7j:col><L7j:col  name=\"name\" type=\"java.lang.String\"><![CDATA[name2]]></L7j:col><L7j:col  name=\"value\" type=\"java.lang.String\"><![CDATA[value2&'<\">]]></L7j:col></L7j:row></L7j:jdbcQueryResult>";
        assertEquals(expected, xmlResult);
        idObj = (Object[]) peCtx.getVariable("jdbcQuery.id");
        nameObj = (Object[]) peCtx.getVariable("jdbcQuery.name");
        valueObj = (Object[]) peCtx.getVariable("jdbcQuery.value");
        assertEquals(2, idObj[0]);
        assertEquals("<![CDATA[name2]]>", nameObj[0]);
        assertEquals("value2&'<\">", valueObj[0]);

        //test/touch Stored Procedure related code
        assertion.setSqlQuery("CALL mockStoredProcedure()");
        assertion.setMaxRecords(12);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockSqlRowResults());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        xmlResult = peCtx.getVariable("jdbcQuery.xmlResult").toString();
        assertNotNull(xmlResult);
        expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\"><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">John</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Production</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">45</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Darcy</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Sales</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">26</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Alice</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Admin</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">F</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">22</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Mary</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Production</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">F</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">32</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Oliver</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Marketing</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">50</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Bob</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Development</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">40</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Ahmad</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Development</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">38</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Amy</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Admin</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">F</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">19</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Carmen</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Sales</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">F</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">25</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">David</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Sales</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">28</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Carlo</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Marketing</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">37</L7j:col></L7j:row><L7j:row><L7j:col  name=\"name\" type=\"java.lang.String\">Kan</L7j:col><L7j:col  name=\"department\" type=\"java.lang.String\">Production</L7j:col><L7j:col  name=\"sex\" type=\"java.lang.String\">M</L7j:col><L7j:col  name=\"age\" type=\"java.lang.String\">29</L7j:col></L7j:row></L7j:jdbcQueryResult>";
        assertEquals(expected, xmlResult);
        // Check context variables creation
        assertArrayEquals("All names matched", MockJdbcDatabaseManager.MOCK_NAMES, (Object[])peCtx.getVariable("jdbcQuery.name"));
        assertArrayEquals("All departments matched", MockJdbcDatabaseManager.MOCK_DEPTS, (Object[])peCtx.getVariable("jdbcQuery.department"));
        assertArrayEquals("All sexes matched", MockJdbcDatabaseManager.MOCK_SEXES, (Object[])peCtx.getVariable("jdbcQuery.sex"));
        assertArrayEquals("All ages matched", MockJdbcDatabaseManager.MOCK_AGES, (Object[])peCtx.getVariable("jdbcQuery.age"));

        //generateXmlResult is false
        assertion.setGenerateXmlResult(false);
        sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");//fresh context
        sass.checkRequest(peCtx);
        try {
            peCtx.getVariable("jdbcQuery.xmlResult");
            fail("Should have failed");
        } catch (NoSuchVariableException e) {
            //expected
        }
    }

    private Object getMockResults() {
        Map<String, List<Object>> row1 = new HashMap<String, List<Object>>();
        List<Object> list1 = new ArrayList<Object>();
        List<Object> list2 = new ArrayList<Object>();
        List<Object> list3 = new ArrayList<Object>();
        list1.add(1);
        list2.add("name1");
        list3.add("value1");
        row1.put("id", list1);
        row1.put("name", list2);
        row1.put("value", list3);
        return row1;
    }

    private Object getMockSpecialResults() {
        Map<String, List<Object>> row1 = new HashMap<String, List<Object>>();
        List<Object> list1 = new ArrayList<Object>();
        List<Object> list2 = new ArrayList<Object>();
        List<Object> list3 = new ArrayList<Object>();
        list1.add(2);
        list2.add("<![CDATA[name2]]>");
        list3.add("value2&'<\">");
        row1.put("id", list1);
        row1.put("name", list2);
        row1.put("value", list3);
        return row1;
    }

    private Object getMockSqlRowResults() {
        MockJdbcDatabaseManager dbm = new MockJdbcDatabaseManager();
        List<SqlRowSet> list = new ArrayList<SqlRowSet>();
        list.add(dbm.getMockSqlRowSet());
        return list;
    }

    @BugNumber(12795)
    @Test
    public void testNewNamingMapCaseInsensetive() throws Exception {
        Map<String, String> namingMap = new TreeMap<String, String>();
        namingMap.put(MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[0].toLowerCase(), "name2");
        namingMap.put(MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[1].toUpperCase(), "dept2");
        namingMap.put("sEx", "sex2");
        namingMap.put("AgE", "age2");

        assertion.setNamingMap(namingMap);
        assertion.setMaxRecords(MockJdbcDatabaseManager.MOCK_MAX_ROWS);

        JdbcQueryingManager jdbcQueryingManager = (JdbcQueryingManager) appCtx.getBean("jdbcQueryingManager");
        SqlRowSet resultSet = jdbcQueryingManager.getMockSqlRowSet();

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, appCtx);
        int numOfRecords = serverAssertion.setContextVariables(resultSet, peCtx, "");

        // Check the number of returned records by the query
        assertEquals("The number of returned numOfRecords matched", MockJdbcDatabaseManager.MOCK_MAX_ROWS, numOfRecords);

        // Check if the context variable 'department' has been renamed to 'dept2'.
        Object nameCtxVar = peCtx.getVariable("jdbcQuery.name2");
        Object deptCtxVar = peCtx.getVariable("jdbcQuery.dept2");
        Object sexCtxVar = peCtx.getVariable("jdbcQuery.sex2");
        Object ageCtxVar = peCtx.getVariable("jdbcQuery.age2");
        assertNotNull("The context variable successfully renamed from '" + MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[0] + "' to 'name2'", nameCtxVar);
        assertNotNull("The context variable successfully renamed from '" + MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[1] + "' to 'dept2'", deptCtxVar);
        assertNotNull("The context variable successfully renamed from '" + MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[2] + "' to 'sex2'", sexCtxVar);
        assertNotNull("The context variable successfully renamed from '" + MockJdbcDatabaseManager.MOCK_COLUMN_NAMES[3] + "' to 'age2'", ageCtxVar);

        // Check context variables creation
        assertArrayEquals("All names matched", MockJdbcDatabaseManager.MOCK_NAMES, (Object[]) peCtx.getVariable("jdbcQuery.NAME2"));
        assertArrayEquals("All departments matched", MockJdbcDatabaseManager.MOCK_DEPTS, (Object[]) peCtx.getVariable("jdbcQuery.DEPT2"));
        assertArrayEquals("All sexes matched", MockJdbcDatabaseManager.MOCK_SEXES, (Object[]) peCtx.getVariable("jdbcQuery.SEX2"));
        assertArrayEquals("All ages matched", MockJdbcDatabaseManager.MOCK_AGES, (Object[]) peCtx.getVariable("jdbcQuery.AGE2"));
    }

    /**
     * Test XML Generation on a null value
    */
    @BugNumber(12888)
    @Test
    public void testNullResult() throws Exception {
        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockResults());
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        assertion.setSqlQuery("SELECT * FROM myTest");
        assertion.setGenerateXmlResult(true);
        assertion.setConnectionName("mockDb");

        //test/touch SELECT query related code, with null result
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithNullResults());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        sass.checkRequest(peCtx);
        String xmlResult = peCtx.getVariable("jdbcQuery.xmlResult").toString();
        assertNotNull(xmlResult);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\"><L7j:row><L7j:col  name=\"id\" type=\"java.lang.Integer\">2</L7j:col><L7j:col  name=\"testByte\" type=\"java.lang.byte[]\">63 68 61 72 64 </L7j:col><L7j:col  name=\"name\" type=\"java.lang.String\"><![CDATA[name2]]></L7j:col><L7j:col  name=\"testField\" ><![CDATA[NULL]]></L7j:col><L7j:col  name=\"value\" type=\"java.lang.String\"><![CDATA[value2&'<\">]]></L7j:col></L7j:row></L7j:jdbcQueryResult>";
        assertEquals(expected, xmlResult);
        assertTrue(xmlResult.indexOf("type=\"java.lang.byte[]\"")>0);
        assertTrue(xmlResult.indexOf("<![CDATA[NULL]]>")>0);
        Object[] idObj = (Object[]) peCtx.getVariable("jdbcQuery.id");
        Object[] nameObj = (Object[]) peCtx.getVariable("jdbcQuery.name");
        Object[] valueObj = (Object[]) peCtx.getVariable("jdbcQuery.value");
        assertEquals(2, idObj[0]);
        assertEquals("<![CDATA[name2]]>", nameObj[0]);
        assertEquals("value2&'<\">", valueObj[0]);
        try {
            Object[] testFieldObj = (Object[]) peCtx.getVariable("jdbcQuery.testField");
            fail("Should have failed");
        } catch (NoSuchVariableException e) {
            //expected
        }
    }

    private Object getMockWithNullResults() {
        Map<String, List<Object>> row1 = new HashMap<String, List<Object>>();
        List<Object> list1 = new ArrayList<Object>();
        List<Object> list2 = new ArrayList<Object>();
        List<Object> list3 = new ArrayList<Object>();
        List<Object> list4 = new ArrayList<Object>();
        list1.add(2);
        list2.add("<![CDATA[name2]]>");
        list3.add("value2&'<\">");
        list4.add(new byte[]{0x63, 0x68, 0x61, 0x72, 0x64});
        row1.put("id", list1);
        row1.put("name", list2);
        row1.put("value", list3);
        row1.put("testField", null);
        row1.put("testByte", list4);
        return row1;
    }

    @BugId("SSG-5937")
    @Test
    public void nullValueSubstitution() throws Exception {

        // single value variable NO null value substitution
        {
            assertion.setConvertVariablesToStrings(true);
            assertion.setSqlQuery("SELECT FUNC('a',${policy.nullvalue},'b') as output");
            final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
            final List<Object> params = pair.right;

            assertEquals("", params.get(0));
        }

        // single value variable WITH null value substitution
        {
            assertion.setConvertVariablesToStrings(false);
            final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
            final List<Object> params = pair.right;
            assertEquals(null, params.get(0));
        }

        // multi-value variable WITH null value substitution
        {
            peCtx.setVariable("args", new Object[]{"one","two",null});
            assertion.setSqlQuery("SELECT FUNC(${args}) as output");
            assertion.setConvertVariablesToStrings(false);
            final Pair<String, List<Object>> pair = JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), peCtx, assertion.getVariablesUsed(), assertion.isConvertVariablesToStrings(), new TestAudit());
            final List<Object> params = pair.right;
            assertEquals("one", params.get(0));
            assertEquals("two", params.get(1));
            assertEquals(null, params.get(2));
        }
    }

    @BugId("SSG-6572")
    @Test
    public void testValidLiteralQueryTimeoutValue() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithNullResults());
        assertion.setQueryTimeout("60");

        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, applicationContext);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(makeContext("<xml />", "<xml />"));
        assertEquals(AssertionStatus.NONE, assertionStatus);

        verify(jdbcQueryingManager).performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), eq(60), anyList());
    }

    @BugId("SSG-6572")
    @Test
    public void testUpgradeDefaultValue() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithNullResults());

        // default value for assertion is null
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, applicationContext);
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(makeContext("<xml />", "<xml />"));
        assertEquals(AssertionStatus.NONE, assertionStatus);

        // validate null was interpreted as 0
        verify(jdbcQueryingManager).performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), eq(0), anyList());
    }

    @BugId("SSG-6572")
    @Test
    public void testValidVariableQueryTimeoutValue() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithNullResults());
        assertion.setQueryTimeout("${myvar}");

        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, applicationContext);
        final PolicyEnforcementContext context = makeContext("<xml />", "<xml />");
        context.setVariable("myvar", "100");
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);

        verify(jdbcQueryingManager).performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), eq(100), anyList());
    }

    @BugId("SSG-6572")
    @Test
    public void testInvalidQueryTimeoutValue() throws Exception {
        assertion.setQueryTimeout("invalid");
        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, appCtx);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(makeContext("<xml />", "<xml />"));
        assertEquals(AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining("\"Perform JDBC Query\" assertion failed due to: Invalid resolved value for query timeout: invalid"));
    }

    @BugId("SSG-6572")
    @Test
    public void testInValidVariableQueryTimeoutValue() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithNullResults());
        assertion.setQueryTimeout("${myvar}");

        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, applicationContext);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));

        final PolicyEnforcementContext context = makeContext("<xml />", "<xml />");
        context.setVariable("myvar", "invalid");
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining("\"Perform JDBC Query\" assertion failed due to: Invalid resolved value for query timeout: invalid"));
    }

    /**
     * Validate that the resolveAsObject property is ignored for normal message traffic processing usages of the JDBC Query
     * assertion. The distinction on the type of usage is made based on the subclass of PolicyEnforcementContext used.
     *
     */
    @Test
    public void testResolveAsObjectIgnoredForMessageTraffic() throws Exception {

        final PolicyEnforcementContext context = makeContext("<xml />", "<xml />");
        verifyResolveAsObjectPropUsage(context, true);
   }

    /**
     * Validate that the hidden resolveAsObject property applies when running a look up policy
     */
    @Test
    public void testResolveAsObjectNotIgnoredForLookupExternalAudits() throws Exception {
        final PolicyEnforcementContext context = makeContext("<xml />", "<xml />");
        // This allows the processing of resolveAsObject property
        final AuditLookupPolicyEnforcementContext actualContext = new AuditLookupPolicyEnforcementContext(null, context);
        verifyResolveAsObjectPropUsage(actualContext, false);
    }

    /**
     * Validate that the hidden resolveAsObject property applies when running the audit sink policy
     */
    @Test
    public void testResolveAsObjectNotIgnoredForPutExternalAudits() throws Exception {
        final PolicyEnforcementContext context = makeContext("<xml />", "<xml />");
        // This allows the processing of resolveAsObject property
        final AuditSinkPolicyEnforcementContext actualContext = new AuditSinkPolicyEnforcementContext(null, context, context);
        verifyResolveAsObjectPropUsage(actualContext, false);
    }

    @BugId("SSG-6716")
    @Test
    public void testInvalidConnectionName() throws Exception {
        JdbcQueryAssertion assertionToUse = assertion.clone();
        assertionToUse.setConnectionName("invalid");
        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertionToUse, appCtx);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(makeContext("<xml />", "<xml />"));
        assertEquals(AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining("\"Perform JDBC Query\" assertion failed due to: Could not find JDBC connection: invalid"));
    }

    @BugId("SSG-6716")
    @Test
    public void testNotSchemaApplicableConnectionName() throws Exception {
        JdbcQueryAssertion assertionToUse = assertion.clone();
        assertionToUse.setSchema("mySchema");
        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertionToUse, appCtx);
        final TestAudit testAudit = new TestAudit();
        ApplicationContexts.inject(serverAssertion, Collections.singletonMap("auditFactory", testAudit.factory()));
        final AssertionStatus assertionStatus = serverAssertion.checkRequest(makeContext("<xml />", "<xml />"));
        assertEquals(AssertionStatus.FAILED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresentContaining("\"Perform JDBC Query\" assertion failed due to: Schema value given but JDBC connection does not support it. Connection name: mockDb"));
    }

    /**
     * Validate that the resolveAsObject property is used and ignored when appropriate given the correct PolicyEnforcementContext subclass.
     *
     * External audits work by setting the resolveAsObject on the assertion bean to force some objects to not be converted
     * to Strings. External audits as-is presently will not work without this.
     *
     * If the usage of the JDBC Query assertion is not related to external audits then the 'resolveAsObject' property must
     * always be ignored to avoid a situation where a policy author discovers it and starts to write policies which depend
     * on it.
     *
     * @param context The PolicyEnforcementContext to use with the server assertion. If the correct Audit subclass is
     *                used then the resolveAsObject property is not ignored.
     * @param ignoreResolveAsObject true if resolveAsObject should be ignored e.g. all parameters should have been
     *                              converted to a string. False if the raw object type should have been preserved.
     */
    private void verifyResolveAsObjectPropUsage(final PolicyEnforcementContext context,
                                                final boolean ignoreResolveAsObject) throws Exception {
        MockitoAnnotations.initMocks(this);

        final Date time = new GregorianCalendar(2013, 3, 5, 11, 46, 0).getTime();
        assertion.setSqlQuery("select * from mytable where one = ${one} and two = ${two} and three = ${three}");
        final String one = "one";
        context.setVariable("one", one);
        context.setVariable("two", time);
        final Message msgVar = new Message(XmlUtil.parse("<xml />"));
        context.setVariable("three", msgVar);

        //Tell the assertion to resolve both non string variables as strings
        assertion.setResolveAsObjectList(Arrays.asList("two", "three"));

        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithNullResults());

        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", ConfigFactory.getCachedConfig());
        }}));

        ServerJdbcQueryAssertion serverAssertion = new ServerJdbcQueryAssertion(assertion, applicationContext);

        final AssertionStatus assertionStatus = serverAssertion.checkRequest(context);
        assertEquals(AssertionStatus.NONE, assertionStatus);

        // This fails if all objects were not stringified
        verify(jdbcQueryingManager).performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(),
                argThat(new IsListOfVariousTypes(new Functions.Binary<Boolean, Integer, Object>() {
                    @Override
                    public Boolean call(Integer i, Object o) {
                        if (!ignoreResolveAsObject) {
                            switch (i) {
                                case 0:
                                    return o instanceof String;
                                case 1:
                                    return o instanceof Date;
                                case 2:
                                    return o instanceof Message;
                                default:
                                    throw new RuntimeException("Invalid test construction");
                            }
                        } else {
                            return o instanceof String;
                        }
                    }
                })));
    }

    private static class IsListOfVariousTypes extends ArgumentMatcher<List<Object>> {

        private final Functions.Binary<Boolean, Integer, Object> matchFunction;

        IsListOfVariousTypes(Functions.Binary<Boolean, Integer, Object> matchFunction) {
            this.matchFunction = matchFunction;
        }

        @Override
        public boolean matches(Object inObj) {
            List list = (List) inObj;
            for (int i = 0, listSize = list.size(); i < listSize; i++) {
                Object o = list.get(i);
                if (!matchFunction.call(i, o)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Tests blob results from a function or a stored procedure.
     * @throws Exception
     */
    @Test
    public void testBlobResults() throws Exception {
        final byte[] blobBytes = new byte[40 * 1024];
        RandomUtil.nextBytes(blobBytes);

        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final Map propertiesMap = new HashMap<String, String>();
        final MockConfig mockConfig = new MockConfig(propertiesMap);
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", mockConfig);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        assertion.setGenerateXmlResult(true);
        assertion.setConnectionName("mockDb");

        //test blob result
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockStoredProcedureBlobResults(blobBytes));
        assertion.setSqlQuery("func myTest");
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(status, AssertionStatus.NONE);
        Object[] bytesObj = (Object[]) peCtx.getVariable("jdbcQuery.bytes");
        Assert.assertEquals(byte[].class, ((Object[]) peCtx.getVariable("jdbcQuery.bytes"))[0].getClass());
        assertArrayEquals(blobBytes, (byte[]) bytesObj[0]);

        // set blob size limit
        propertiesMap.put(ServerConfigParams.PARAM_JDBC_QUERY_MAX_BLOB_SIZE_OUT, Long.toString(blobBytes.length-2));
        assertion.setSqlQuery("func myTest");
        sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        status = sass.checkRequest(peCtx);
        assertEquals(status, AssertionStatus.FAILED);
    }

    private Object getMockStoredProcedureBlobResults(byte[] blobBytes) throws SQLException {

        List<Map<String, Object>> row = new ArrayList<Map<String, Object>>();
        Map<String, Object> column = new HashMap<String, Object>();
        Blob blob = new SerialBlob(blobBytes);
        column.put("bytes", blob);
        row.add(column);

        List<SqlRowSet> results = new ArrayList<>();
        results.add(new ResultSetWrappingSqlRowSet(new MockResultSet(row)));
        return results;
    }

    private Object getMockStoredProcedureByteArrayResults(byte[] blobBytes) throws SQLException {

        List<Map<String, Object>> row = new ArrayList<Map<String, Object>>();
        Map<String, Object> column = new HashMap<String, Object>();
        column.put("bytes", blobBytes);
        row.add(column);

        List<SqlRowSet> results = new ArrayList<>();
        results.add(new ResultSetWrappingSqlRowSet(new MockResultSet(row)));
        return results;
    }

    /**
     * Test that the generated XML result is the same for byte[] or Blob results from a function / stored procedure
     * @throws Exception
     */
    @Test
    public void testBlobByteArrayResults() throws Exception {
        final byte[] blobBytes = new byte[]{0x63, 0x68, 0x61, 0x72, 0x64};

        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final Map propertiesMap = new HashMap<String, String>();
        final MockConfig mockConfig = new MockConfig(propertiesMap);
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", mockConfig);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        assertion.setGenerateXmlResult(true);
        assertion.setConnectionName("mockDb");

        //get blob result
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockStoredProcedureBlobResults(blobBytes));
        assertion.setSqlQuery("func myTest");
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(status, AssertionStatus.NONE);
        String blobXmlResult = peCtx.getVariable("jdbcQuery.xmlResult").toString();
        assertNotNull(blobXmlResult);
        Object[] bytesObj = (Object[]) peCtx.getVariable("jdbcQuery.bytes");
        Assert.assertEquals(byte[].class, ((Object[]) peCtx.getVariable("jdbcQuery.bytes"))[0].getClass());
        assertArrayEquals(blobBytes, (byte[]) bytesObj[0]);

        // get byte array result
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockStoredProcedureByteArrayResults(blobBytes));
        assertion.setSqlQuery("func myTest");
        assertion.setVariablePrefix("jdbcQuery1");
        sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        status = sass.checkRequest(peCtx);
        assertEquals(status, AssertionStatus.NONE);
        String byteArrayXmlResult = peCtx.getVariable("jdbcQuery1.xmlResult").toString();
        assertNotNull(byteArrayXmlResult);
        bytesObj = (Object[]) peCtx.getVariable("jdbcQuery1.bytes");
        Assert.assertEquals(byte[].class, ((Object[]) peCtx.getVariable("jdbcQuery1.bytes"))[0].getClass());
        assertArrayEquals(blobBytes, (byte[]) bytesObj[0]);

        // xml should look the same
        assertEquals(blobXmlResult, byteArrayXmlResult);
    }

    /**
     * Tests blob results from a function or a stored procedure.
     * @throws Exception
     */
    @Test
    public void testClobResults() throws Exception {
        final String clobString = RandomStringUtils.randomAlphanumeric(40 * 1024);

        MockitoAnnotations.initMocks(this);
        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyString(), anyInt(), anyInt(), anyList())).thenReturn(getMockWithClobResults(clobString));
        final Map propertiesMap = new HashMap<String, String>();
        final MockConfig mockConfig = new MockConfig(propertiesMap);
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
            put("serverConfig", mockConfig);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        assertion.setSqlQuery("SELECT * FROM myTest");
        assertion.setGenerateXmlResult(true);
        assertion.setConnectionName("mockDb");

        // get clob result
        ServerJdbcQueryAssertion sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        AssertionStatus status = sass.checkRequest(peCtx);
        assertEquals(status, AssertionStatus.NONE);
        String xmlResult = peCtx.getVariable("jdbcQuery.xmlResult").toString();
        assertNotNull(xmlResult);
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><L7j:jdbcQueryResult xmlns:L7j=\"http://ns.l7tech.com/2012/08/jdbc-query-result\"><L7j:row><L7j:col  name=\"clob\" >" + clobString + "</L7j:col></L7j:row></L7j:jdbcQueryResult>";
        assertEquals(expected, xmlResult);
        Object[] bytesObj = (Object[]) peCtx.getVariable("jdbcQuery.clob");
        Assert.assertEquals(String.class, ((Object[]) peCtx.getVariable("jdbcQuery.clob"))[0].getClass());
        assertEquals(clobString, bytesObj[0]);

        //limit clob size
        propertiesMap.put(ServerConfigParams.PARAM_JDBC_QUERY_MAX_CLOB_SIZE_OUT, Long.toString(clobString.length()-2));
        assertion.setSqlQuery("func myTest");
        sass = (ServerJdbcQueryAssertion) serverPolicyFactory.compilePolicy(assertion, false);
        status = sass.checkRequest(peCtx);
        assertEquals(status, AssertionStatus.FAILED);

    }

    private Object getMockWithClobResults(String clobString) throws SQLException {

        List<Map<String, Object>> row = new ArrayList<Map<String, Object>>();
        Map<String, Object> column = new HashMap<String, Object>();
        Clob clob = new SerialClob(clobString.toCharArray());
        column.put("clob", clob);
        row.add(column);

        List<SqlRowSet> results = new ArrayList<>();
        results.add(new ResultSetWrappingSqlRowSet(new MockResultSet(row)));
        return results;
    }

}