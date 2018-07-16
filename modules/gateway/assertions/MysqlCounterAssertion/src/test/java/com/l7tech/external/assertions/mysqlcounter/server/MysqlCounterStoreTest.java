package com.l7tech.external.assertions.mysqlcounter.server;

import com.ca.apim.gateway.extension.sharedstate.counter.exception.CounterLimitReachedException;
import com.l7tech.server.PlatformTransactionManagerStub;
import com.l7tech.server.extension.provider.sharedstate.SharedCounterConfigConstants;
import com.l7tech.util.TestTimeSource;
import org.hibernate.SessionFactory;
import org.hibernate.classic.Session;
import org.hibernate.jdbc.Work;
import org.junit.Before;
import org.junit.Test;
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
import java.util.Properties;

import static com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest.MONTH;
import static com.ca.apim.gateway.extension.sharedstate.counter.CounterFieldOfInterest.SEC;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Test the MysqlCounterAssertion.
 */
public class MysqlCounterStoreTest {
    public static final String MY_COUNTER_NAME = "myCounterName";
    public static final String INVALID_COUNTER_NAME = "invalidCounterName";
    Session session;
    SessionFactory sessionFactory;
    Connection connection;
    PreparedStatement preparedStatement;
    ResultSet resultSet;

    MysqlCounterStore counterStore;
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
        long now = (System.currentTimeMillis() / 1000L) * 1000L + 10;
        timeSource = new TestTimeSource(now, 10L);
        MysqlCounterStore.timeSource = timeSource;

        session = mock(Session.class);
        sessionFactory = mock(SessionFactory.class);
        connection = mock(Connection.class);
        preparedStatement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        template = new HibernateTemplate();
        template.setSessionFactory(sessionFactory);
        counterStore = new MysqlCounterStore(stubTransactionManager);

        ReflectionTestUtils.setField(counterStore, "hibernateTemplate", template);
    }

    @Test
    public void givenCounterNameIsNotExist_whenQueryByGetInterface_NullIsReturned() throws SQLException {
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
        String hibernateTemplate = "hibernateTemplate";
        ReflectionTestUtils.setField(counterStore, hibernateTemplate, counterCreatorTemplate);
        assertEquals(null, counterStore.query(INVALID_COUNTER_NAME));
    }

    @Test
    public void givenReadSynchronously_whenCreateAndGetCounter_expectTwoDBCalls() throws SQLException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L).thenReturn(2L);

        doAnswer(invocationOnMock -> {
            Work work = (Work) invocationOnMock.getArguments()[0];
            work.execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));
        Properties properties = createProperties(true, false);

        long counterValue = counterStore.get(MY_COUNTER_NAME, properties, SEC);
        assertEquals(1, counterValue);
        counterValue = counterStore.get(MY_COUNTER_NAME, properties, SEC);
        assertEquals(2, counterValue);
        verify(connection, times(2)).prepareStatement(any(String.class));
    }


    @Test
    public void givenReadAsychronously_whenCreateAndGetCounter_expectOneDBCall() throws SQLException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(1)).thenReturn(1L).thenReturn(2L);

        doAnswer(invocationOnMock -> {
            Work work = (Work) invocationOnMock.getArguments()[0];
            work.execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));

        Properties properties = createProperties(false, false);

        long counterValue = counterStore.get(MY_COUNTER_NAME, properties, SEC);
        assertEquals(1, counterValue);
        counterValue = counterStore.get(MY_COUNTER_NAME, properties, SEC);
        assertEquals(1, counterValue);
        verify(connection, times(1)).prepareStatement(any(String.class));
    }

    @Test
    public void givenReadSynchronouslyIncrementAsynchronously_whenCreateAndIncrementCounter_expectThreeDBCall() throws SQLException {

        long timestamp = Calendar.getInstance().getTimeInMillis();

        setupDBCallMocks(timestamp);

        createCounterInManager();

        Properties properties = createProperties(true, false);

        long counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1);
        assertEquals(2, counterValue);
        counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1);
        assertEquals(3, counterValue);
        verify(connection, times(3)).prepareStatement(any(String.class));
    }


    @Test
    public void givenReadAsynchronouslyIncrementAsynchronously_whenCreateAndIncrementCounter_expectTwoDBCalls() throws SQLException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        setupDBCallMocks(timestamp);

        createCounterInManager();

        Properties properties = createProperties(false, false);

        long counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1);
        assertEquals(2, counterValue);
        counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1);
        assertEquals(3, counterValue);
        verify(connection, times(2)).prepareStatement(any(String.class));
    }

    @Test
    public void givenReadSynchronouslyIncrementSynchronously_whenCreateAndIncrementCounter_expectFiveDBCalls() throws SQLException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        setupDBCallMocks(timestamp);

        createCounterInManager();

        Properties properties = createProperties(true, true);

        long counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1);
        assertEquals(2, counterValue);
        counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1);
        assertEquals(3, counterValue);
        verify(connection, times(5)).prepareStatement(any(String.class));
    }

    @Test
    public void givenReadSynchronouslyIncrementAsynchronously_whenCreateAndIncrementCounterWithLimit_expectThreeDBCalls()
            throws SQLException, CounterLimitReachedException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        setupDBCallMocks(timestamp);

        createCounterInManager();

        Properties properties = this.createProperties(true, false);

        long counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1, 3);
        assertEquals(2, counterValue);
        counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1, 3);
        assertEquals(3, counterValue);
        verify(connection, times(3)).prepareStatement(any(String.class));
    }

    @Test
    public void givenReadAsynchronouslyIncrementAsynchronously_whenCreateAndIncrementCounterWithLimit_expectTwoDBCalls()
            throws SQLException, CounterLimitReachedException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        setupDBCallMocks(timestamp);

        createCounterInManager();

        Properties properties = this.createProperties(false, false);

        long counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1, 3);
        assertEquals(2, counterValue);
        counterValue = counterStore.updateAndGet(MY_COUNTER_NAME, properties, MONTH, timestamp, 1, 3);
        assertEquals(3, counterValue);
        verify(connection, times(2)).prepareStatement(any(String.class));
    }

    @Test
    public void givenAsyncIncrementOnlyWithinLimitAndReturnValueIsCalled_whenAnIncrementWouldExceedTheCounter_TheCounterShouldNotBeIncremented() throws SQLException, CounterLimitReachedException {
        long timestamp = Calendar.getInstance().getTimeInMillis();

        setupDBCallMocks(timestamp);

        createCounterInManager();

        Properties properties = this.createProperties(false, false);

        boolean thrown = false;

        long counterValue = counterStore.asyncIncrementOnlyWithinLimitAndReturnValue(false, MY_COUNTER_NAME, timestamp, MONTH, 2L, 1);
        assertEquals(2, counterValue);
        try {
            counterStore.asyncIncrementOnlyWithinLimitAndReturnValue(false, MY_COUNTER_NAME, timestamp, MONTH, 2L, 1);
        } catch (CounterLimitReachedException e) {
            thrown = true;
        }
        assertTrue(thrown);
        counterValue = counterStore.get(MY_COUNTER_NAME, properties, MONTH);
        assertEquals(2, counterValue);
    }

    private Properties createProperties(boolean readSync, boolean writeSync) {
        Properties properties = new Properties();
        properties.setProperty(SharedCounterConfigConstants.CounterOperations.KEY_READ_SYNC, String.valueOf(readSync));
        properties.setProperty(SharedCounterConfigConstants.CounterOperations.KEY_WRITE_SYNC, String.valueOf(writeSync));
        return properties;
    }

    private void createCounterInManager() {
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
        String hibernateTemplate = "hibernateTemplate";
        ReflectionTestUtils.setField(counterStore, hibernateTemplate, counterCreatorTemplate);
        counterStore.get(MY_COUNTER_NAME);
        ReflectionTestUtils.setField(counterStore, hibernateTemplate, template);
    }

    private void setupDBCallMocks(long timestamp) throws SQLException {
        when(sessionFactory.openSession()).thenReturn(session);
        when(connection.prepareStatement(any(String.class))).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true).thenReturn(true);
        when(resultSet.getLong(5)).thenReturn(0L).thenReturn(1L).thenReturn(2L);
        when(resultSet.getLong(6)).thenReturn(timestamp).thenReturn(timestamp);

        doAnswer(invocationOnMock -> {
            Work work = (Work) invocationOnMock.getArguments()[0];
            work.execute(connection);
            return null;
        }).when(session).doWork(any(Work.class));
    }
}
