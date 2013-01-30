package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.gateway.common.jdbc.JdbcAdmin;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.TestLicenseManager;
import com.l7tech.server.jdbc.*;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.ServerPolicyFactory;
import com.l7tech.server.util.MockInjector;
import com.l7tech.server.util.SimpleSingletonBeanFactory;
import com.l7tech.test.BugId;
import com.l7tech.test.BugNumber;
import com.l7tech.util.CollectionUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.SQLException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
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

    @Before
    public void setUp() throws Exception {
        // Get the policy enforcement context
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        //create assertion
        assertion = new JdbcQueryAssertion();

        final JdbcConnection connection = new JdbcConnection();
        connection.setName("mockDb");
        connection.setDriverClass("com.mysql.jdbc.Driver");

        connectionManager = new JdbcConnectionManagerStub(connection);
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

        List<Object> params = new ArrayList<Object>();
        String query = "SELECT * FROM employees " +
            "WHERE employee_department = ${var_dept} " +
            "AND name = ${var_info[0]} " +
            "ADN sex = ${var_info[1]} " +
            "AND age = ${var_info[2]}";
        assertion.setSqlQuery(query);

        String expectedPlainQuery = "SELECT * FROM employees WHERE employee_department = ? AND name = ? ADN sex = ? AND age = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit() );

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

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id in (${var_ids}) and department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(true);

        String expectedPlainQuery = "select * from employees where id in (?, ?, ?) and department = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
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

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id in (${var_ids}) and department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(false);

        String expectedPlainQuery = "select * from employees where id in (?) and department = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
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

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees e1, employees e2 where e1.id in (${var_ids}) and e2.id in (${var_ids}) and e1.department = ${var_dept}";
        assertion.setSqlQuery(query);
        assertion.setAllowMultiValuedVariables(true);

        String expectedPlainQuery = "select * from employees e1, employees e2 where e1.id in (?, ?, ?) and e2.id in (?, ?, ?) and e1.department = ?";
        String actualPlainQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
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

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id=${var_ids[3]} and department = ${var_dept}";
        assertion.setSqlQuery(query);
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
    }

    @Test
    public void shouldReturnEmptyVaslueWhenVariableIndexOutOfBounds() throws Exception {
        final List<Object> employeeIds = new ArrayList<Object>();
        final String department = "Production";
        employeeIds.add(1);
        employeeIds.add(2);

        peCtx.setVariable("var_dept", department);
        peCtx.setVariable("var_ids", "1");

        List<Object> params = new ArrayList<Object>();
        String query = "select * from employees where id=${var_ids[3]} and department = ${var_dept}";
        assertion.setSqlQuery(query);
        String actualQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(query, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals("select * from employees where id=? and department = ?", actualQuery);
        assertEquals("", params.get(0));
        assertEquals(department, params.get(1));
        
    }
    
    @Test
    public void shouldReturnUnalteredQueryStringWhenNoContextVariablesPresent() throws Exception {
        List<Object> params = new ArrayList<Object>();
        String expectedQuery = "select * from employees where id=1 and department = 'Production'";
        assertion.setSqlQuery(expectedQuery);
        String actualQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(expectedQuery, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertEquals(expectedQuery, actualQuery);
        assertTrue(params.size() == 0);

    }

    @Test
    public void resolveContextVariablesAsObjects() throws Exception {
        Object varValue = new byte[]{0x01,0x02};
        peCtx.setVariable("var", varValue);
        peCtx.setVariable("var1", varValue);

        List<Object> params = new ArrayList<Object>();
        String expectedQuery = "select * from employees where id=${var} and department = ${var1}";
        assertion.setSqlQuery(expectedQuery);
        assertion.setResolveAsObjectList(CollectionUtils.list("var"));
        String actualQuery = JdbcQueryUtils.getQueryStatementWithoutContextVariables(expectedQuery, params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        assertTrue(params.size() == 2);
        assertEquals(params.get(0), varValue);
        assertEquals(params.get(1), varValue.toString());
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
        final JdbcQueryingManager qm = new JdbcQueryingManagerImpl(cpm);

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", qm);
            put("jdbcConnectionManager", cm);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);

        assertion.setConnectionName(connectionName);
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
        final JdbcQueryingManager qm = new JdbcQueryingManagerImpl(cpm);

        final AssertionRegistry assertionRegistry = new AssertionRegistry();
        assertionRegistry.afterPropertiesSet();
        serverPolicyFactory = new ServerPolicyFactory(new TestLicenseManager(), new MockInjector());
        final GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", qm);
            put("jdbcConnectionManager", cm);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);

        assertion.setConnectionName(connectionName);
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
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyInt(), anyList())).thenReturn(getMockResults());
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
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
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyInt(), anyList())).thenReturn(getMockSpecialResults());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
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
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyInt(), anyList())).thenReturn(getMockSqlRowResults());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
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
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyInt(), anyList())).thenReturn(getMockResults());
        GenericApplicationContext applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
        }}));
        serverPolicyFactory.setApplicationContext(applicationContext);
        assertion.setSqlQuery("SELECT * FROM myTest");
        assertion.setGenerateXmlResult(true);
        assertion.setConnectionName("mockDb");

        //test/touch SELECT query related code, with null result
        when(jdbcQueryingManager.performJdbcQuery(anyString(), anyString(), anyInt(), anyList())).thenReturn(getMockWithNullResults());
        applicationContext = new GenericApplicationContext(new SimpleSingletonBeanFactory(new HashMap<String, Object>() {{
            put("assertionRegistry", assertionRegistry);
            put("policyFactory", serverPolicyFactory);
            put("jdbcQueryingManager", jdbcQueryingManager);
            put("jdbcConnectionManager", connectionManager);
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
        peCtx.setVariable("nullval", "NULL");
        List<Object> params = new ArrayList<Object>();
        assertion.setSqlQuery("SELECT FUNC('a',${nullval},'b') as output");
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        ServerJdbcQueryAssertion.applyNullValue(assertion.getNullPattern(),params);
        assertEquals("NULL", params.get(0));

        // single value variable WITH null value substitution
        params.clear();
        assertion.setNullPattern("NULL");
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        ServerJdbcQueryAssertion.applyNullValue(assertion.getNullPattern(),params);
        assertEquals(null, params.get(0));

        // multi-value variable WITH null value substitution
        params.clear();
        peCtx.setVariable("args", new String[]{"one","two","NULL"});
        assertion.setSqlQuery("SELECT FUNC(${args}) as output");
        assertion.setAllowMultiValuedVariables(true);
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        ServerJdbcQueryAssertion.applyNullValue(assertion.getNullPattern(),params);
        assertEquals("one", params.get(0));
        assertEquals("two", params.get(1));
        assertEquals(null, params.get(2));

        // multi-value variable NO null value substitution
        params.clear();
        assertion.setNullPattern("null");
        JdbcQueryUtils.getQueryStatementWithoutContextVariables(assertion.getSqlQuery(), params, peCtx,assertion.getVariablesUsed(),assertion.isAllowMultiValuedVariables(),assertion.getResolveAsObjectList(),new TestAudit());
        ServerJdbcQueryAssertion.applyNullValue(assertion.getNullPattern(),params);
        assertEquals("one", params.get(0));
        assertEquals("two", params.get(1));
        assertEquals("NULL", params.get(2));


    }


}