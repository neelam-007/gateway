package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.gateway.common.jdbc.JdbcConnection;
import com.l7tech.gateway.common.jdbc.JdbcUtil;
import com.l7tech.message.Message;
import com.l7tech.server.jdbc.JdbcCallHelper;
import com.l7tech.server.jdbc.JdbcConnectionManager;
import com.l7tech.server.jdbc.JdbcQueryingManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

/**
 * Test Stored Procedure processing utility
 *
 * @author rraquepo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class JdbcCallHelperTest {

    @Mock
    private AbstractDataSource dataSource;
    @Mock
    private Connection conn;
    @Mock
    private DatabaseMetaData databaseMetaData;
    @Mock
    private SimpleJdbcCall simpleJdbcCall;
    @Mock
    private JdbcConnectionManager jdbcConnectionManager;
    @Mock
    private JdbcConnection jdbcConnection;
    @Autowired
    private ApplicationContext appCtx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        myMocks();
    }

    private void myMocks() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");//pretend we are using a MySQL database

        when(simpleJdbcCall.execute(any(SqlParameterSource.class))).thenReturn(getDummyResults());
        when(simpleJdbcCall.getJdbcTemplate()).thenReturn(new JdbcTemplate(dataSource));

        appCtx = Mockito.spy(appCtx);
        Mockito.doReturn(jdbcConnectionManager).when(appCtx).getBean(Matchers.eq("jdbcConnectionManager"), Matchers.eq(JdbcConnectionManager.class));
        when(jdbcConnectionManager.getJdbcConnection(Matchers.eq("MySQL"))).thenReturn(jdbcConnection);
        when(jdbcConnection.getDriverClass()).thenReturn("my.driver.class");
    }

    private Map<String, Object> getDummyResults() {
        Map<String, Object> results = new HashMap<String, Object>();
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        Map<String, Object> row1 = new HashMap<String, Object>();
        row1.put("out1", "result 1");
        row1.put("out2", "result 2");
        row1.put("out3", new Long(999));
        row1.put("result", "function result");
        rows.add(row1);
        results.put("#result-set-", rows);
        results.put("outparam", "output param1");
        return results;
    }

    private Map<String, Object> getDummyOutParameterResults() {
        Map<String, Object> results = new HashMap<String, Object>();
        results.put("outparam", "output param1");
        return results;
    }

    private Map<String, Object> getDummyEmptyResult() {
        Map<String, Object> results = new HashMap<String, Object>();
        return results;
    }

    /**
     * Test our stored procedure util, although a bit limited since all the db related stuff are mocked
     *
     * @throws Exception
     */
    @Test
    public void testHelper() throws Exception {
        String query = "CALL GetSamples";
        final ArrayList<String> inParameterNames = new ArrayList<String>();
        final JdbcCallHelper jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

        List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[0]);
        SqlRowSet rowSet = results.get(0);
        rowSet.next();//results sets
        Object obj = rowSet.getObject("out1");
        assertNotNull(obj);
        assertEquals("result 1", obj);
        obj = rowSet.getObject("out2");
        assertNotNull(obj);
        assertEquals("result 2", obj);
        obj = rowSet.getObject("out3");
        assertNotNull(obj);
        assertEquals(999L, obj);

        //test non existing  parameter
        obj = rowSet.getObject("dummy field");
        assertNull(obj);

        query = "FUNC sampleFunc";
        results = jdbcHelper.queryForRowSet(query, new Object[0]);
        rowSet = results.get(0);
        rowSet.next();
        obj = rowSet.getObject("result");
        assertNotNull(obj);
        assertEquals("function result", obj);

        when(simpleJdbcCall.execute(any(SqlParameterSource.class))).thenReturn(getDummyOutParameterResults());
        query = "CALL GetSampleOut    ()";
        results = jdbcHelper.queryForRowSet(query, new Object[0]);
        rowSet = results.get(0);
        rowSet.next();//out parameter result
        obj = rowSet.getObject("outparam");
        assertNotNull(obj);
        assertEquals("output param1", obj);

        when(simpleJdbcCall.execute(any(SqlParameterSource.class))).thenReturn(getDummyEmptyResult());
        query = "CALL GetSamples3()";//no results
        results = jdbcHelper.queryForRowSet(query, new Object[0]);
        assertEquals(0, results.size());
    }

    /**
     * Test if we can properly extract the parameters from the query
     */
    @Test
    public void testParameters() {
        //by the time the helper class was called, all context variables has been replaced by ?
        String query = "CALL GetSample ('value1','value2',99,?,?)";
        final ArrayList<String> inParameterNames = new ArrayList<String>();
        final JdbcCallHelper jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

        List<String> parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "value1");
        assertEquals(parameter.get(1), "value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        query = "CALL GetSample 'value1','value2',99,?,?";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "value1");
        assertEquals(parameter.get(1), "value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        //make sure we can still retrieve parenthesis if they are inside a string parameter
        query = "CALL GetSample ('value1(','value2)',99,?,?)";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "value1(");
        assertEquals(parameter.get(1), "value2)");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        query = "CALL GetSample ('(value1',')value2',99,?,?)";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "(value1");
        assertEquals(parameter.get(1), ")value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        //make sure comma's inside the string was not splitted
        query = "CALL GetSample \"value1,\",\",value2\",99,?,?,\",\"";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 6);
        assertEquals(parameter.get(0), "value1,");
        assertEquals(parameter.get(1), ",value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");
        assertEquals(parameter.get(5), ",");

        try {
            query = "CALL GetSample 'value1',";
            jdbcHelper.getParametersFromQuery(query);
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
    }

    /**
     * Test if we can properly set the required context variables
     */
    @Test
    public void testContextVariables() throws Exception {
        PolicyEnforcementContext peCtx = makeContext("<myrequest/>", "<myresponse/>");
        String query = "CALL GetSamples ()";
        final ArrayList<String> inParameterNames = new ArrayList<String>();
        final JdbcCallHelper jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

        List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[0]);
        JdbcQueryingManagerStub jdbcQueryingManager = (JdbcQueryingManagerStub) appCtx.getBean("jdbcQueryingManager");
        jdbcQueryingManager.setMockResults(results);

        JdbcQueryAssertion assertion = new JdbcQueryAssertion();
        assertion.setConnectionName("MySQL");
        assertion.setSqlQuery("select * from mytable");
        ServerJdbcQueryAssertion fixture = new ServerJdbcQueryAssertion(assertion, appCtx);
        fixture.checkRequest(peCtx);

        //test multiple result set
        assertNotNull(peCtx.getVariable("jdbcQuery.resultSet1.out1"));
        assertNotNull(peCtx.getVariable("jdbcQuery.resultSet1.out2"));
        assertNotNull(peCtx.getVariable("jdbcQuery.resultSet1.out3"));
        assertNotNull(peCtx.getVariable("jdbcQuery.resultSet1.queryresult.count"));
        assertEquals(((Object[]) peCtx.getVariable("jdbcQuery.resultSet1.out1"))[0], "result 1");

        assertNotNull(peCtx.getVariable("jdbcQuery.resultSet2.outparam"));
        assertNotNull(peCtx.getVariable("jdbcQuery.resultSet2.queryresult.count"));
        assertEquals(((Object[]) peCtx.getVariable("jdbcQuery.resultSet2.outparam"))[0], "output param1");

        //test single result set
        when(simpleJdbcCall.execute(any(SqlParameterSource.class))).thenReturn(getDummyOutParameterResults());
        results = jdbcHelper.queryForRowSet(query, new Object[0]);
        jdbcQueryingManager.setMockResults(results);
        peCtx = makeContext("<myrequest/>", "<myresponse/>");
        fixture = new ServerJdbcQueryAssertion(assertion, appCtx);
        fixture.checkRequest(peCtx);
        assertNotNull(peCtx.getVariable("jdbcQuery.outparam"));
        assertEquals(((Object[]) peCtx.getVariable("jdbcQuery.outparam"))[0], "output param1");
    }

    @BugNumber(12255)
    @Test
    public void testParameterCount() throws Exception {
        ArrayList<String> inParameterNames = new ArrayList<String>(Arrays.asList("inparam"));
        JdbcCallHelper jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

        try {
            //expecting a parameter, but passed empty argument
            String query = "CALL GetSamples (?)";
            jdbcHelper.queryForRowSet(query, new Object[0]);
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
        try {
            //not expecting a parameter, but an argument was passed
            String query = "CALL GetSamples ";
            jdbcHelper.queryForRowSet(query, new Object[]{"value1"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
        try {
            //parameter vs argument passed does not match, expected 1 vs 2
            String query = "CALL GetSamples (?,?)";
            jdbcHelper.queryForRowSet(query, new Object[]{"value1", "value2"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
        try {
            // Note this test covers the condition when the assertion's configuration does not match the database's
            // configuration for the proc / function e.g. the db was updated.
            inParameterNames = new ArrayList<String>(Arrays.asList("inparam", "inparam"));
            jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

            //parameter vs argument passed does not match, expected 2 vs 1
            String query = "CALL GetSamples (?)";
            jdbcHelper.queryForRowSet(query, new Object[]{"value1"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
    }

    @BugNumber(12575)
    @Test
    public void testInvalidCharacter() throws Exception {
        ArrayList<String> inParameterNames = new ArrayList<String>(Arrays.asList("inparam"));
        JdbcCallHelper jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

        try {
            String query = "CALL GetSamples (?)";
            jdbcHelper.queryForRowSet(query, new Object[]{});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        //additional check for invalid quotes
        try {
            String query = "CALL GetSamples '?";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        try {
            String query = "CALL GetSamples ?'";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        try {
            String query = "CALL GetSamples \"?";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        try {
            String query = "CALL GetSamples ?\"";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        //with proper closing quotes
        try {
            String query = "CALL GetSamples \"?\"";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
        } catch (Exception e) {
            fail("should not fail");
        }

        try {
            String query = "CALL GetSamples '?'";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
        } catch (Exception e) {
            fail("should not fail");
        }

        //not properly closed )

        try {
            String query = "CALL GetSample ('value1','value2',99,?,?";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        try {
            String query = "CALL GetSample 'value1','value2',99,?,?)";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        try {
            String query = "CALL GetSample ('value1',('value2',99,?,?)";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        try {
            String query = "CALL GetSample ('value1','value2',99,?),?)";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        //empty param in between commas or commas at the end of the query
        try {
            String query = "CALL GetSample 'value1','value2',99,?,?,";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }

        //empty param ,
        try {
            String query = "CALL GetSample 'value1','value2',,99,?,?";
            jdbcHelper.queryForRowSet(query, new Object[]{"?"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
    }

    /**
     * Test how we can handle various query variations
     */
    @Test
    public void testGetQueryName(){
        String name = JdbcUtil.getName("CALL GetSamples1 (?)");
        assertEquals("GetSamples1",name);
        name = JdbcUtil.getName("CALL GetSamples2");
        assertEquals("GetSamples2",name);
        name = JdbcUtil.getName("CALL GetSamples3     ?");
        assertEquals("GetSamples3",name);
        name = JdbcUtil.getName("CALL GetSamples4");
        assertEquals("GetSamples4",name);
        name = JdbcUtil.getName("CALL GetSamples5()");
        assertEquals("GetSamples5",name);
        name = JdbcUtil.getName("CALL GetSamples6(        )");
        assertEquals("GetSamples6",name);
        name = JdbcUtil.getName("CALL GetSamples7           (        )");
        assertEquals("GetSamples7",name);
    }

    /**
     * Simulate result from Oracle/DB2, fields are upper case, make sure we can retrieve result in a case-insensitive way
     *
     * @throws Exception
     */
    @BugNumber(12888)
    @Test
    public void testOracleDB2Result() throws Exception {
        String query = "CALL GetSamples";
        final ArrayList<String> inParameterNames = new ArrayList<String>();
        final JdbcCallHelper jdbcHelper = new JdbcCallHelper(simpleJdbcCall, inParameterNames);

        List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[0]);
        SqlRowSet rowSet = results.get(0);
        rowSet.next();//results sets
        Object obj = rowSet.getObject("Out1");//Out1 should also match out1
        assertNotNull(obj);
        assertEquals("result 1", obj);
        obj = rowSet.getObject("OUT2");//OUT2 should also match out2
        assertNotNull(obj);
        assertEquals("result 2", obj);
        obj = rowSet.getObject("OUT3");//OUT3 should also match out3
        assertNotNull(obj);
        assertEquals(999L, obj);

        //test non existing  parameter
        obj = rowSet.getObject("dummy field");
        assertNull(obj);
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

}
