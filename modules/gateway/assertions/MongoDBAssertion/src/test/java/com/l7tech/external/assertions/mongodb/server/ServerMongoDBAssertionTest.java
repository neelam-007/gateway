package com.l7tech.external.assertions.mongodb.server;

import com.l7tech.external.assertions.mongodb.*;
import com.l7tech.external.assertions.mongodb.entity.MongoDBConnectionEntity;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.mongodb.*;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.junit.Assert.assertEquals;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Test the MongoDBAssertion.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MongoDBConnectionManager.class)
public class ServerMongoDBAssertionTest {

    @Mock
    MongoDBAssertion mockMongoDBAssertion;
    @Mock
    MongoDBConnection mockMongoDBConnection;
    @Mock
    PolicyEnforcementContext mockPolicyEnforcementContext;
    @Mock
    DBCollection mockDBCollection;
    @Mock
    DB mockDB;
    @Mock
    MongoClient mockMongoClient;
    @Mock
    MongoDBConnectionEntity mockMockDBConnectionEntity;
    @Mock
    DBCursor mockDBCursor;
    @Mock
    WriteResult mockWriteResult;
    @Mock
    DBObject mockDBObject;


    ServerMongoDBAssertion serverMongoDBAssertion;
    MongoDBConnectionManager mockMongoDBConnectionManager;
    private static Goid aMongoConnectionGoid = new Goid(1, 0);
    private static Goid connectionGoid = new Goid(2, 0);

    String queryDocument = "{\"name\" : \"tactical\" }";
    String updateDocument = "{$set: {port: 686}}";

    @Before
    public void setup() throws Exception {

        mockMongoDBConnectionManager = Mockito.mock(MongoDBConnectionManager.class);
        PowerMockito.mockStatic(MongoDBConnectionManager.class);

        when(MongoDBConnectionManager.getInstance()).thenReturn(mockMongoDBConnectionManager);        
        when(mockMongoDBConnectionManager.getConnection(Mockito.any(Goid.class))).thenReturn(mockMongoDBConnection);

        when(mockMongoDBAssertion.getVariablesUsed()).thenReturn(new String[]{"variableName"});
        when(mockMongoDBConnection.getMongoClient()).thenReturn(mockMongoClient);
        when(mockMongoDBConnection.getMongoDBConnectionEntity()).thenReturn(mockMockDBConnectionEntity);

        when(mockMongoClient.getDB(anyString())).thenReturn(mockDB);
        when(mockDB.getCollection(anyString())).thenReturn(mockDBCollection);

        when(mockMongoDBAssertion.getConnectionGoid()).thenReturn(connectionGoid);
        when(mockMongoDBAssertion.getQueryDocument()).thenReturn(queryDocument);
        when(mockMongoDBAssertion.getProjectionDocument()).thenReturn("");
        when(mockMongoDBAssertion.getUpdateDocument()).thenReturn(updateDocument);
        when(mockMongoDBAssertion.getWriteConcern()).thenReturn(String.valueOf(MongoDBWriteConcern.ACKNOWLEDGED));

        serverMongoDBAssertion = new ServerMongoDBAssertion(mockMongoDBAssertion);
    }

    @Test
    public void testAssertionFailsWhenMongoClientIsNull() throws Exception {
        when(mockMongoDBConnection.getMongoClient()).thenReturn(null);

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        assertEquals(status, AssertionStatus.FAILED);
    }

    @Test
    public void testAssertionFailsWhenIsFailIfNoResultsIsTrue() throws Exception {

        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.FIND));
        when(mockMongoDBAssertion.isFailIfNoResults()).thenReturn(true);
        when(mockDBCollection.find(any(DBObject.class), any(DBObject.class))).thenReturn(mockDBCursor);
        when(mockDBCursor.hasNext()).thenReturn(false);

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        assertEquals(status, AssertionStatus.FAILED);
    }

    @Test
    public void testAssertionSuccessfulForFindOperation() throws Exception {
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.FIND));
        when(mockDBCollection.find(any(DBObject.class), any(DBObject.class))).thenReturn(mockDBCursor);
        when(mockDBCursor.hasNext()).thenReturn(false);
        when(mockMongoDBAssertion.isFailIfNoResults()).thenReturn(false);

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).find(any(DBObject.class), any(DBObject.class));
        assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testAssertionSuccessfulForInsertOperation() throws Exception {
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.INSERT));
        when(mockDBCollection.insert(any(DBObject.class), any(WriteConcern.class))).thenReturn(mockWriteResult);

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).insert(any(DBObject.class), any(WriteConcern.class));
        verify(mockDBCollection, times(0)).remove(any(DBObject.class));
        verify(mockDBCollection, times(0)).update(any(DBObject.class), any(DBObject.class), any(Boolean.class), any(Boolean.class), any(WriteConcern.class));
        assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testAssertionSuccessfulForUpdateOperation() throws Exception {
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.UPDATE));
        when(mockDBCollection.update(any(DBObject.class), any(DBObject.class), any(Boolean.class), any(Boolean.class), any(WriteConcern.class))).thenReturn(mockWriteResult);

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).update(any(DBObject.class), any(DBObject.class), any(Boolean.class), any(Boolean.class), any(WriteConcern.class));
        verify(mockDBCollection, times(0)).remove(any(DBObject.class));
        verify(mockDBCollection, times(0)).insert(any(DBObject.class), any(WriteConcern.class));
        assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testAssertionSuccessfulForDeleteOperation() throws Exception {
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.DELETE));
        when(mockDBCollection.remove(any(DBObject.class))).thenReturn(mockWriteResult);

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).remove(any(DBObject.class));
        verify(mockDBCollection, times(0)).update(any(DBObject.class), any(DBObject.class), any(Boolean.class), any(Boolean.class), any(WriteConcern.class));
        verify(mockDBCollection, times(0)).insert(any(DBObject.class), any(WriteConcern.class));
        assertEquals(status, AssertionStatus.NONE);
    }

    @Test
    public void testAssertionFailsWhenResultHasError() throws Exception {
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.DELETE));
        when(mockDBCollection.remove(any(DBObject.class))).thenReturn(mockWriteResult);
        when(mockWriteResult.getError()).thenReturn("Not Null");

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).remove(any(DBObject.class));
        assertEquals(status, AssertionStatus.FAILED);
    }

    @Test
    public void testAssertionFailsWhenConnectionNameIsNull() throws Exception {
        when(mockMongoDBAssertion.getConnectionGoid()).thenReturn(null);
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.DELETE));

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).remove(any(DBObject.class));
        assertEquals(status, AssertionStatus.FAILED);
    }

    @Test
    public void testAssertionFailsWhenDBNameIsNull() throws Exception {
        when(mockMockDBConnectionEntity.getDatabaseName()).thenReturn(null);
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.DELETE));

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).remove(any(DBObject.class));
        assertEquals(status, AssertionStatus.FAILED);
    }

    @Test
    public void testAssertionFailsWhenPortIsNull() throws Exception {
        when(mockMockDBConnectionEntity.getPort()).thenReturn(null);
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.DELETE));

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).remove(any(DBObject.class));
        assertEquals(status, AssertionStatus.FAILED);
    }

    @Test
    public void testAssertionFailsWhenServernameIsNull() throws Exception {
        when(mockMockDBConnectionEntity.getUri()).thenReturn(null);
        when(mockMongoDBAssertion.getOperation()).thenReturn(String.valueOf(MongoDBOperation.DELETE));

        AssertionStatus status = serverMongoDBAssertion.checkRequest(mockPolicyEnforcementContext);

        verify(mockDBCollection).remove(any(DBObject.class));
        assertEquals(status, AssertionStatus.FAILED);
    }
}
