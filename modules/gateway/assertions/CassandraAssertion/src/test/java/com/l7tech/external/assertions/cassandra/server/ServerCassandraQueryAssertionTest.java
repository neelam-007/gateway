package com.l7tech.external.assertions.cassandra.server;

import static org.junit.Assert.*;

import com.ca.datasources.cassandra.CassandraQueryManager;
import com.ca.datasources.cassandra.CassandraQueryManagerStub;
import com.l7tech.server.cassandra.CassandraConnectionHolder;
import com.l7tech.server.cassandra.CassandraConnectionManager;
import com.datastax.driver.core.*;
import com.l7tech.external.assertions.cassandra.CassandraQueryAssertion;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.cassandra.CassandraConnectionManagerStub;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test the CassandraAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerCassandraQueryAssertionTest {

    @Autowired
    ApplicationContext applicationContext;
    //Class under test
    ServerCassandraQueryAssertion fixture;

    String queryDocument = "Select * from oauth_token";
    PolicyEnforcementContext mockPolicyEnforcementContext;
    CassandraConnectionHolder mockCassandraConnectionHolder;
    Session mockSession;
    ResultSet mockResultSet;
    ResultSetFuture mockResultSetFuture;
    Iterator mockResultSetIterator;
    Iterator mockColumnDefinitionsIterator;
    Row mockRow;
    ColumnDefinitions mockColumnDefinitions;
    PreparedStatement mockPreparedStatement;
    BoundStatement mockBoundStatement;

    @Before
    public void setup() throws Exception {
        CassandraConnectionManagerStub cassandraConnectionManager = (CassandraConnectionManagerStub)applicationContext.getBean("cassandraConnectionManager", CassandraConnectionManager.class);
        CassandraQueryManagerStub cassandraQueryManager = (CassandraQueryManagerStub)applicationContext.getBean("cassandraQueryManager", CassandraQueryManager.class);
        mockPolicyEnforcementContext = mock(PolicyEnforcementContext.class);
        mockCassandraConnectionHolder = mock(CassandraConnectionHolder.class);
        mockResultSetFuture = mock(ResultSetFuture.class);
        mockResultSet = mock(ResultSet.class);
        mockSession = mock(Session.class);
        mockResultSetIterator = mock(Iterator.class);
        mockColumnDefinitionsIterator = mock(Iterator.class);
        mockRow = mock(Row.class);
        mockColumnDefinitions = mock(ColumnDefinitions.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockBoundStatement = mock(BoundStatement.class);
        cassandraConnectionManager.setMockConnection(mockCassandraConnectionHolder);
        cassandraQueryManager.setPreparedStatement(mockPreparedStatement);
        cassandraQueryManager.setBoundStatement(mockBoundStatement);
        when(mockCassandraConnectionHolder.getSession()).thenReturn(mockSession);
        when(mockResultSetFuture.getUninterruptibly()).thenReturn(mockResultSet);
    }

    @Test
    public void testPreparedStatementQueryExecution() throws Exception {
        CassandraQueryAssertion cassandraAssertion = new CassandraQueryAssertion();
        cassandraAssertion.setConnectionName("testConnection");
        cassandraAssertion.setPrefix(CassandraQueryAssertion.DEFAULT_QUERY_PREFIX);
        cassandraAssertion.setQueryDocument(queryDocument);

        when(mockSession.executeAsync(mockBoundStatement)).thenReturn(mockResultSetFuture);
        when(mockSession.prepare(queryDocument)).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.getConsistencyLevel()).thenReturn(ConsistencyLevel.LOCAL_QUORUM);
        when(mockPreparedStatement.getSerialConsistencyLevel()).thenReturn(ConsistencyLevel.LOCAL_SERIAL);
        when(mockPreparedStatement.isTracing()).thenReturn(false);
        PreparedId mockPreparedId = new MockPreparedId(null, mockColumnDefinitions, mockColumnDefinitions, new int[0] , ProtocolVersion.V3);
        when(mockPreparedStatement.getPreparedId()).thenReturn(mockPreparedId);
        when(mockPreparedStatement.getVariables()).thenReturn(mockColumnDefinitions);
        when(mockColumnDefinitions.size()).thenReturn(1);
        when(mockResultSet.iterator()).thenReturn(mockResultSetIterator);
        when(mockResultSetIterator.hasNext()).thenReturn(true,false);
        when(mockResultSetIterator.next()).thenReturn(mockRow);
        when(mockRow.getColumnDefinitions()).thenReturn(mockColumnDefinitions);
        when(mockColumnDefinitions.iterator()).thenReturn(mockColumnDefinitionsIterator);
        when(mockColumnDefinitionsIterator.hasNext()).thenReturn(true,false);
        when(mockColumnDefinitionsIterator.next()).thenReturn(
                new MockDefinition("oauth", "oauth_token", "otk_token_id", DataType.text()));
        when(mockRow.getString("otk_token_id")).thenReturn("bbc3cf8d-63d0-4a21-b57e-2ae9e0646b9e");

        fixture = new ServerCassandraQueryAssertion(cassandraAssertion, applicationContext);

        AssertionStatus status = fixture.checkRequest(mockPolicyEnforcementContext);

        assertEquals(AssertionStatus.NONE, status);
        verify(mockPolicyEnforcementContext, times(1)).setVariable(CassandraQueryAssertion.DEFAULT_QUERY_PREFIX + ".queryresult.count", 1);
        verify(mockPolicyEnforcementContext, times(1)).setVariable(CassandraQueryAssertion.DEFAULT_QUERY_PREFIX + ".otk_token_id", new Object[]{"bbc3cf8d-63d0-4a21-b57e-2ae9e0646b9e"});
    }

    @Test
    public void testPreparedStatementQueryExecutionWithBindVariables() throws Exception {
        CassandraQueryAssertion cassandraAssertion = new CassandraQueryAssertion();
        cassandraAssertion.setConnectionName("testConnection");
        cassandraAssertion.setPrefix(CassandraQueryAssertion.DEFAULT_QUERY_PREFIX);
        String query = "Select * from oauth_token where otk_token_id = ${myVar}";
        cassandraAssertion.setQueryDocument(query);
        Map<String, Object> varMap = new HashMap();
        varMap.put("myvar", "bbc3cf8d-63d0-4a21-b57e-2ae9e0646b9e");
        when(mockPolicyEnforcementContext.getVariableMap(eq(new String[]{"myVar"}), any(Audit.class))).thenReturn(varMap);
        when(mockSession.executeAsync(mockBoundStatement)).thenReturn(mockResultSetFuture);
        when(mockSession.prepare("Select * from oauth_token where otk_token_id = ?")).thenReturn(mockPreparedStatement);
        PreparedId mockPreparedId = new MockPreparedId(null, mockColumnDefinitions, mockColumnDefinitions, new int[0] , ProtocolVersion.V3);
        when(mockPreparedStatement.getPreparedId()).thenReturn(mockPreparedId);
        when(mockPreparedStatement.getConsistencyLevel()).thenReturn(ConsistencyLevel.LOCAL_QUORUM);
        when(mockPreparedStatement.getSerialConsistencyLevel()).thenReturn(ConsistencyLevel.LOCAL_SERIAL);
        when(mockPreparedStatement.isTracing()).thenReturn(false);

        when(mockPreparedStatement.getVariables()).thenReturn(mockColumnDefinitions);
        when(mockColumnDefinitions.size()).thenReturn(1);
        when(mockColumnDefinitions.getType(1)).thenReturn(DataType.varchar());
        when(mockResultSet.iterator()).thenReturn(mockResultSetIterator);
        when(mockResultSetIterator.hasNext()).thenReturn(true,false);
        when(mockResultSetIterator.next()).thenReturn(mockRow);
        when(mockRow.getColumnDefinitions()).thenReturn(mockColumnDefinitions);
        when(mockColumnDefinitions.iterator()).thenReturn(mockColumnDefinitionsIterator);
        when(mockColumnDefinitionsIterator.hasNext()).thenReturn(true,false);
        when(mockColumnDefinitionsIterator.next()).thenReturn(
                new MockDefinition("oauth", "oauth_token", "otk_token_id", DataType.text()));
        when(mockRow.getString("otk_token_id")).thenReturn("bbc3cf8d-63d0-4a21-b57e-2ae9e0646b9e");


        fixture = new ServerCassandraQueryAssertion(cassandraAssertion, applicationContext);
        AssertionStatus status = fixture.checkRequest(mockPolicyEnforcementContext);

        assertEquals(AssertionStatus.NONE, status);
        verify(mockPolicyEnforcementContext, times(1)).setVariable(CassandraQueryAssertion.DEFAULT_QUERY_PREFIX + ".queryresult.count", 1);
        verify(mockPolicyEnforcementContext, times(1)).setVariable(CassandraQueryAssertion.DEFAULT_QUERY_PREFIX + ".otk_token_id", new Object[]{"bbc3cf8d-63d0-4a21-b57e-2ae9e0646b9e"});
    }


}
