package com.l7tech.server.sla;

import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.server.PlatformTransactionManagerStub;
import com.l7tech.util.TestTimeSource;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.jdbc.Work;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;


/**
 * Unit tests for the CounterManagerImpl. Specifically these tests cover the new functionality of the
 * reads from the database being synchronous or asynchronous.
 */
@RunWith(MockitoJUnitRunner.class)
public class CounterManagerImplTest {

    Session session;
    SessionFactory sessionFactory;
    Connection connection;
    PreparedStatement preparedStatement;
    ResultSet resultSet;

    CounterManager counterManager;
    HibernateTemplate template;

    TestTimeSource timeSource;

    /**
     * The set up method that is run before each test case. This method is responsible for the set up
     * of test data and objects so that the tests are running appropriately under the right circumstances/situation.
     */
    @Before
    public void setUp() {
        PlatformTransactionManagerStub stubTransactionManager = new PlatformTransactionManagerStub();

        // Ensure sure test doesn't start immediately before a time period rollover
        long now = (System.currentTimeMillis() / 1000L ) * 1000L + 10;
        timeSource = new TestTimeSource( now, 10L );
        CounterManagerImpl.timeSource = timeSource;

        session = mock(Session.class);
        sessionFactory = mock(SessionFactory.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        template = new HibernateTemplate();
        template.setSessionFactory(sessionFactory);
        counterManager = new CounterManagerImpl(stubTransactionManager);

        ReflectionTestUtils.setField(counterManager, "hibernateTemplate", template);
    }

    /**
     * Test that will check the get counter value read method if the reads are synchronous. It is expected that
     * after two invocations, that the first invocation will get the value from the database and the second invocation
     * will also hit the database causing in two calls to the database.
     *
     * @throws SQLException exception on error
     */
    @Test
    public void testGetCounterValueReadSynchronous() throws SQLException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L).thenReturn(2L);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Work work = (Work) invocationOnMock.getArguments()[0];
                work.execute(connection);
                return null;
            }

        }).when(session).doWork(any(Work.class));

        long counterValue = counterManager.getCounterValue(true, "myCounterName", 1);
        assertEquals(1, counterValue);
        counterValue = counterManager.getCounterValue(true, "myCounterName", 1);
        assertEquals(2, counterValue);
        verify(connection, times(2)).prepareStatement(any(String.class));
    }

    /**
     * Test that will check the get counter value read method if the reads are asynchronous. It is expected that
     * after two invocations, that the first invocation will get the value from the database and the second invocation
     * will get the information from the cache causing in one call to the database.
     *
     * @throws SQLException exception on error
     */
    @Test
    public void testGetCounterValueReadAsynchronous() throws SQLException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L).thenReturn(2L);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Work work = (Work) invocationOnMock.getArguments()[0];
                work.execute(connection);
                return null;
            }

        }).when(session).doWork(any(Work.class));

        long counterValue = counterManager.getCounterValue(false, "myCounterName", 1);
        assertEquals(1, counterValue);
        counterValue = counterManager.getCounterValue(false, "myCounterName", 1);
        assertEquals(1, counterValue);
        verify(connection, times(1)).prepareStatement(any(String.class));
    }

    /**
     * Test that will check the asynch increment and return value method if the reads are asynchronous. It is expected that
     * after two invocations, that the first invocation will get the value from the database and the second invocation
     * will get the information from the cache causing in one call to the database.
     *
     * @throws SQLException exception on error
     */
    @Test
    public void testAsynchIncrementAndReturnValueReadSynchronous() throws SQLException, ObjectModelException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(5)).thenReturn(1L).thenReturn(2L);
        when(resultSet.getLong(6)).thenReturn(timestamp).thenReturn(timestamp);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Work work = (Work) invocationOnMock.getArguments()[0];
                work.execute(connection);
                return null;
            }

        }).when(session).doWork(any(Work.class));

        createCounterInManager();

        long counterValue = ((CounterManagerImpl)counterManager).asyncIncrementAndReturnValue(true, "myCounterName", timestamp, 5, 1);
        assertEquals(2, counterValue);
        counterValue = ((CounterManagerImpl)counterManager).asyncIncrementAndReturnValue(true, "myCounterName", timestamp, 5, 1);
        assertEquals(3, counterValue);
        verify(connection, times(2)).prepareStatement(any(String.class));
    }

    /**
     * Test that will check the get asynch increment and return value method if the reads are asynchronous. It is expected that
     * after two invocations, that the first invocation will get the value from the database and the second invocation
     * will get the information from the cache causing in one call to the database.
     *
     * @throws SQLException exception on error
     */
    @Test
    public void testAsynchIncrementAndReturnValueReadAsynchronous() throws SQLException, ObjectModelException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(5)).thenReturn(1L).thenReturn(2L);
        when(resultSet.getLong(6)).thenReturn(timestamp).thenReturn(timestamp);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Work work = (Work) invocationOnMock.getArguments()[0];
                work.execute(connection);
                return null;
            }

        }).when(session).doWork(any(Work.class));

        createCounterInManager();

        long counterValue = ((CounterManagerImpl)counterManager).asyncIncrementAndReturnValue(false, "myCounterName", timestamp, 5, 1);
        assertEquals(2, counterValue);
        counterValue = ((CounterManagerImpl)counterManager).asyncIncrementAndReturnValue(false, "myCounterName", timestamp, 5, 1);
        assertEquals(3, counterValue);
        verify(connection, times(1)).prepareStatement(any(String.class));
    }

    /**
     * Test that will check the get asynch increment and return value method if the reads are asynchronous. It is expected that
     * after two invocations, that the first invocation will get the value from the database and the second invocation
     * will get the information from the cache causing in one call to the database.
     *
     * @throws SQLException exception on error
     */
    @Test
    public void testAsyncIncrementOnlyWithinLimitAndReturnValueReadSynchronous() throws CounterManager.LimitAlreadyReachedException, SQLException, ObjectModelException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(5)).thenReturn(1L).thenReturn(2L);
        when(resultSet.getLong(6)).thenReturn(timestamp).thenReturn(timestamp);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Work work = (Work) invocationOnMock.getArguments()[0];
                work.execute(connection);
                return null;
            }

        }).when(session).doWork(any(Work.class));

        createCounterInManager();

        long counterValue = ((CounterManagerImpl)counterManager).asyncIncrementOnlyWithinLimitAndReturnValue(true, "myCounterName", timestamp, 5, 3, 1);
        assertEquals(2, counterValue);
        counterValue = ((CounterManagerImpl)counterManager).asyncIncrementOnlyWithinLimitAndReturnValue(true, "myCounterName", timestamp, 5, 3, 1);
        assertEquals(3, counterValue);
        verify(connection, times(2)).prepareStatement(any(String.class));

    }

    /**
     * Test that will check the asynch increment only within limit and return value method if the reads are asynchronous. It is expected that
     * after two invocations, that the first invocation will get the value from the database and the second invocation
     * will get the information from the cache causing in one call to the database.
     *
     * @throws SQLException exception on error
     */
    @Test
    public void testAsyncIncrementOnlyWithinLimitAndReturnValueReadAsynchronous() throws CounterManager.LimitAlreadyReachedException, ObjectModelException, SQLException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(5)).thenReturn(1L).thenReturn(2L);
        when(resultSet.getLong(6)).thenReturn(timestamp).thenReturn(timestamp);

        doAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                Work work = (Work) invocationOnMock.getArguments()[0];
                work.execute(connection);
                return null;
            }

        }).when(session).doWork(any(Work.class));

        createCounterInManager();

        long counterValue = ((CounterManagerImpl)counterManager).asyncIncrementOnlyWithinLimitAndReturnValue(false, "myCounterName", timestamp, 5, 3, 1);
        assertEquals(2, counterValue);
        counterValue = ((CounterManagerImpl)counterManager).asyncIncrementOnlyWithinLimitAndReturnValue(false, "myCounterName", timestamp, 5, 3, 1);
        assertEquals(3, counterValue);
        verify(connection, times(1)).prepareStatement(any(String.class));
    }

    /**
     * Helper method which will create counter in the counter manager.
     *
     * @throws ObjectModelException exception on error
     */
    private void createCounterInManager() throws ObjectModelException {

        //Modify so we can create a counter in the QUEUE
        HibernateTemplate counterCreatorTemplate = new HibernateTemplate() {
            @Override
            public List executeFind(HibernateCallback<?> action) throws DataAccessException {
                return new ArrayList<>();
            }

            @Override
            public Serializable save(Object entity) throws DataAccessException {
                return null;
            }
        };

        counterCreatorTemplate.setSessionFactory(sessionFactory);
        ReflectionTestUtils.setField(counterManager, "hibernateTemplate", counterCreatorTemplate);
        counterManager.ensureCounterExists("myCounterName");
        ReflectionTestUtils.setField(counterManager, "hibernateTemplate", template);
    }

}