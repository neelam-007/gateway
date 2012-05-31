package com.l7tech.external.assertions.jdbcquery.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.message.Message;
import com.l7tech.server.jdbc.JdbcCallHelper;
import com.l7tech.server.jdbc.JdbcQueryingManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test Stored Procedure processing utility
 *
 * @author rraquepo
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class JdbcCallHelperTest {

    private JdbcCallHelper jdbcHelper;
    @Mock
    private AbstractDataSource dataSource;
    @Mock
    private Connection conn;
    @Mock
    private ResultSet resultSet1;
    @Mock
    private ResultSet resultSet2;
    @Mock
    private DatabaseMetaData databaseMetaData;
    @Mock
    private SimpleJdbcCall simpleJdbcCall;
    @Autowired
    private ApplicationContext appCtx;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        jdbcHelper = new JdbcCallHelper(simpleJdbcCall);
        myMocks();
    }

    private void myMocks() throws Exception {
        when(dataSource.getConnection()).thenReturn(conn);
        when(conn.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDatabaseProductName()).thenReturn("MySQL");//pretend we are using a MySQL database

        when(resultSet1.getString("PROCEDURE_CAT")).thenReturn("test");
        when(resultSet1.getString("PROCEDURE_SCHEM")).thenReturn("test");
        when(resultSet1.getString("PROCEDURE_NAME")).thenReturn("getsamples");
        when(resultSet1.next()).thenReturn(true).thenReturn(false);

        when(databaseMetaData.getProcedures(anyString(), anyString(), anyString())).thenReturn(resultSet1);
        when(databaseMetaData.getProcedureColumns(anyString(), anyString(), anyString(), anyString())).thenReturn(resultSet1);
        when(simpleJdbcCall.execute(any(SqlParameterSource.class))).thenReturn(getDummyResults());
        when(simpleJdbcCall.getJdbcTemplate()).thenReturn(new JdbcTemplate(dataSource));
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
        query = "CALL GetSampleOut";
        results = jdbcHelper.queryForRowSet(query, new Object[0]);
        rowSet = results.get(0);
        rowSet.next();//out parameter result
        obj = rowSet.getObject("outparam");
        assertNotNull(obj);
        assertEquals("output param1", obj);

        when(simpleJdbcCall.execute(any(SqlParameterSource.class))).thenReturn(getDummyEmptyResult());
        query = "CALL GetSamples3";//no results
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

        //we are really not strict in requiring ( or ), a missing open or close parenthesis should be okay
        query = "CALL GetSample ('value1','value2',99,?,?";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "value1");
        assertEquals(parameter.get(1), "value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        query = "CALL GetSample 'value1','value2',99,?,?)";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "value1");
        assertEquals(parameter.get(1), "value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        //make sure we can still retrieve parenthesis if they are inside a string parameter
        query = "CALL GetSample (('value1(','value2)',99,?,?))";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "value1(");
        assertEquals(parameter.get(1), "value2)");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");

        query = "CALL GetSample (('(value1',')value2',99,?,?))";
        parameter = jdbcHelper.getParametersFromQuery(query);
        assertEquals(parameter.size(), 5);
        assertEquals(parameter.get(0), "(value1");
        assertEquals(parameter.get(1), ")value2");
        assertEquals(parameter.get(2), "99");
        assertEquals(parameter.get(3), "?");
        assertEquals(parameter.get(4), "?");
    }

    /**
     * Test if we can properly set the required context variables
     */
    @Test
    public void testContextVariables() throws Exception {
        PolicyEnforcementContext peCtx = makeContext("<myrequest/>", "<myresponse/>");
        String query = "CALL GetSamples ()";
        List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[0]);
        JdbcQueryingManagerStub jdbcQueryingManager = (JdbcQueryingManagerStub) appCtx.getBean("jdbcQueryingManager");
        jdbcQueryingManager.setMockResults(results);

        JdbcQueryAssertion assertion = new JdbcQueryAssertion();
        assertion.setConnectionName("MySQL");
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
        when(resultSet2.getString("COLUMN_NAME")).thenReturn("inparam");
        when(resultSet2.getInt("COLUMN_TYPE")).thenReturn(1);
        when(resultSet2.next()).thenReturn(true).thenReturn(false);
        when(databaseMetaData.getProcedureColumns(anyString(), anyString(), anyString(), anyString())).thenReturn(resultSet2);
        try {
            //expecting a parameter, but passed empty argument
            when(resultSet2.next()).thenReturn(true).thenReturn(false);
            String query = "CALL GetSamples (?)";
            List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[0]);
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
        try {
            //not expecting a parameter, but an argument was passed
            String query = "CALL GetSamples ";
            when(resultSet2.next()).thenReturn(true).thenReturn(false);
            List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[]{"value1"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
        try {
            //parameter vs argument passed does not match, expected 1 vs 2
            String query = "CALL GetSamples (?,?)";
            when(resultSet2.next()).thenReturn(true).thenReturn(false);
            List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[]{"value1", "value2"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
        try {
            //parameter vs argument passed does not match, expected 2 vs 1
            String query = "CALL GetSamples (?)";
            when(resultSet2.next()).thenReturn(true).thenReturn(true).thenReturn(false);//this will mock a 2 required parameter
            List<SqlRowSet> results = jdbcHelper.queryForRowSet(query, new Object[]{"value1"});
            fail("should have failed");
        } catch (Exception e) {
            //exception expected
        }
    }

    private PolicyEnforcementContext makeContext(String req, String res) {
        Message request = new Message();
        request.initialize(XmlUtil.stringAsDocument(req));
        Message response = new Message();
        response.initialize(XmlUtil.stringAsDocument(res));
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

}
