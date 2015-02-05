package com.l7tech.server.cassandra;

import com.ca.datasources.cassandra.CassandraQueryManager;
import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Session;
import com.l7tech.gateway.common.cassandra.CassandraConnection;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.rbac.RbacServices;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import scala.actors.threadpool.Arrays;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CassandraConnectionManagerAdminImplTest {

    @Mock
    CassandraConnectionEntityManager mockCassandraEntityManager;
    @Mock
    RbacServices mockRbackServices;
    @Mock
    CassandraConnectionManager mockCassandraConnectionManager;
    @Mock
    CassandraQueryManager mockCassandraQueryManager;
    @Mock
    Session mockSession;
    @Mock
    Cluster mockCluster;

    CassandraConnectionManagerAdminImpl fixture;
    private CassandraConnection connOne;
    private CassandraConnection connTwo;
    private User currentUser;

    @Before
    public void setUp() throws Exception {
        connOne = new CassandraConnection();
        connOne.setGoid(new Goid(-100,-100));
        connOne.setName("one");
        connOne.setContactPoints("myhost1");
        connOne.setKeyspaceName("keyspace1");
        connTwo = new CassandraConnection();
        connTwo.setGoid(new Goid(-100, -200));
        connTwo.setName("two");
        connTwo.setContactPoints("myhost2");
        connTwo.setKeyspaceName("keyspace2");
        currentUser = new InternalUser("user");
        when(mockCassandraEntityManager.findAll()).thenReturn(Collections.unmodifiableCollection(Arrays.asList(new CassandraConnection[]{connOne, connTwo})));
        when(mockCassandraEntityManager.findByUniqueName("one")).thenReturn(connOne);

        fixture = spy(new CassandraConnectionManagerAdminImpl(mockCassandraEntityManager, mockRbackServices, mockCassandraConnectionManager, mockCassandraQueryManager));
        doNothing().when(fixture).checkLicense();
    }

    @Test
    public void testGetCassandraSingleConnection() throws Exception {
        CassandraConnection connection = fixture.getCassandraConnection("one");
        assertEquals(connOne, connection);
    }

    @Test
    public void testGetAllCassandraConnections() throws Exception {
        List<CassandraConnection> actual = fixture.getAllCassandraConnections();
        assertArrayEquals(new CassandraConnection[]{connOne, connTwo}, actual.toArray());
    }

    @Test
    public void testGetAllCassandraConnectionNames() throws Exception {
       List<String> expected = Arrays.asList(new String[]{"one", "two"});
       when(fixture.getCurrentUser()).thenReturn(currentUser);
       when(mockRbackServices.isPermittedForEntity(eq(currentUser), any(Entity.class), eq(OperationType.READ), isNull(String.class))).thenReturn(true);
       List<String> actual = fixture.getAllCassandraConnectionNames();
       verify(mockCassandraEntityManager, times(1)).findAll();
       assertEquals(expected, actual);
    }

    @Test
    public void testGetOnlyPermittedCassandraConnectionNames() throws Exception {
        List<String> expected = Arrays.asList(new String[]{"two"});
        when(fixture.getCurrentUser()).thenReturn(currentUser);
        when(mockRbackServices.isPermittedForEntity(eq(currentUser), eq(connTwo), eq(OperationType.READ), isNull(String.class))).thenReturn(true);
        List<String> actual = fixture.getAllCassandraConnectionNames();
        verify(mockCassandraEntityManager, times(1)).findAll();
        assertEquals(expected, actual);
    }

    @Test
    public void testUserNotAuthorizedToGetCassandraConnectionNames() throws Exception {
        when(fixture.getCurrentUser()).thenReturn(new InternalUser("otherUser"));
        when(mockRbackServices.isPermittedForEntity(eq(currentUser), any(Entity.class), eq(OperationType.READ), isNull(String.class))).thenReturn(true);
        List<String> actual = fixture.getAllCassandraConnectionNames();
        verify(mockCassandraEntityManager, times(1)).findAll();
        assertTrue(actual.size() == 0);
    }

    @Test
    public void testSaveCassandraConnection() throws Exception {
        CassandraConnection connThree = new CassandraConnection();
        connThree.setName("three");
        fixture.saveCassandraConnection(connThree);
        verify(mockCassandraEntityManager, never()).update(connOne);
        verify(mockCassandraConnectionManager, never()).updateConnection(connOne);
        verify(mockCassandraEntityManager, times(1)).save(connThree);

    }

    @Test
    public void testUpdateCassandraConnection() throws Exception {
        connOne.setUsername("test");
        fixture.saveCassandraConnection(connOne);
        verify(mockCassandraEntityManager, times(1)).update(connOne);
        verify(mockCassandraConnectionManager, times(1)).updateConnection(connOne);
        verify(mockCassandraEntityManager, never()).save(any(CassandraConnection.class));
    }


    @Test
    public void testDeleteCassandraConnection() throws Exception {
        CassandraConnection deleted = new CassandraConnection();
        fixture.deleteCassandraConnection(deleted);
        verify(mockCassandraEntityManager, times(1)).delete(deleted);
        verify(mockCassandraConnectionManager, times(1)).removeConnection(deleted);
    }

    @Test
    public void testTestCassandraConnection() throws Exception {
        fixture.testCassandraConnection(connOne);
        verify(mockCassandraConnectionManager, times(1)).testConnection(connOne);
    }

    @Test
    public void testTestCassandraQuery() throws Exception {
        CassandraConnectionHolder mockConnectionHolder = new CassandraConnectionHolderImpl(connOne, mockCluster, mockSession);
        when(mockCassandraConnectionManager.getConnection("one")).thenReturn(mockConnectionHolder);
        final String query = "select * from table";
        fixture.testCassandraQuery("one", query, 0);
        verify(mockCassandraConnectionManager, times(1)).getConnection("one");
        verify(mockCassandraQueryManager, times(1)).testQuery(mockSession, query, 0);
    }
}