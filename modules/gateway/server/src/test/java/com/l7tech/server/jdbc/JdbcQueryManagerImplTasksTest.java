package com.l7tech.server.jdbc;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.MockConfig;
import com.l7tech.util.TimeSource;
import org.hamcrest.Description;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.beans.PropertyChangeEvent;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This was created: 3/25/13 as 4:54 PM
 *
 * @author Victor Kazakov
 */
@RunWith(MockitoJUnitRunner.class)
public class JdbcQueryManagerImplTasksTest {

    @Mock
    JdbcConnectionPoolManager jdbcConnectionPoolManager;
    @Mock
    JdbcConnectionManager jdbcConnectionManager;

    private JdbcQueryingManagerImpl jdbcQueryingManagerImpl;

    @Mock
    ManagedTimer downloadMetaDataTimer;
    @Mock
    ManagedTimer cleanUpTimer;

    @Mock
    TimerTask currentCacheTask;
    @Mock
    TimerTask currentCleanUpTask;

    private Field currentCacheTaskField;
    private Field currentCleanUpTaskField;

    @Before
    public void beforeTest() {
    }

    @Test
    public void testDoStart() throws NoSuchFieldException, IllegalAccessException {
        MockConfig mockConfig = new MockConfig(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "true")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456").map());
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));

        Mockito.verify(downloadMetaDataTimer).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheTask");
            }
        }), Matchers.anyLong(), Matchers.eq(123L));

        Mockito.verify(cleanUpTimer).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheCleanUpTask");
            }
        }), Matchers.anyLong(), Matchers.eq(456L));
    }

    @Test
    public void testDoStartMetadataCacheTaskDisabled() throws NoSuchFieldException, IllegalAccessException {
        MockConfig mockConfig = new MockConfig(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "false")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456").map());
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));

        Mockito.verify(downloadMetaDataTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        Mockito.verify(cleanUpTimer).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheCleanUpTask");
            }
        }), Matchers.anyLong(), Matchers.eq(456L));
    }

    @Test
    public void testDoStop() throws NoSuchFieldException, IllegalAccessException {
        MockConfig mockConfig = new MockConfig(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "true")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456").map());
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        Assert.assertNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());

        jdbcQueryingManagerImpl.onApplicationEvent(new Stopped(this, null, null));

        Assert.assertNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());

        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));

        spyCacheTasks(jdbcQueryingManagerImpl);

        jdbcQueryingManagerImpl.onApplicationEvent(new Stopped(this, null, null));

        Mockito.verify(currentCacheTask).cancel();
        Mockito.verify(currentCleanUpTask).cancel();

        Assert.assertNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());

    }

    @Test
    public void propertyChangeTestCacheRefreshInterval() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "true")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "789").map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, null, null));

        //verify that the download task was started
        Mockito.verify(downloadMetaDataTimer).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheTask");
            }
        }), Matchers.anyLong(), Matchers.eq(123L));

        //verify the the cleanup task was not started
        Mockito.verify(cleanUpTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        spyCacheTasks(jdbcQueryingManagerImpl);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "999");
        //trigger a cache refresh interval event
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, null, null));

        //the cache task should have been started 1 times
        Mockito.verify(downloadMetaDataTimer, Mockito.times(1)).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheTask");
            }
        }), Matchers.anyLong(), Matchers.eq(999L));

        //verify the the cleanup task was not started
        Mockito.verify(cleanUpTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        //the cache task should have been stop before it was started
        Mockito.verify(currentCacheTask).cancel();

        //the cache task should be set and the clean up task should never have been set.
        Assert.assertNotNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());
    }

    @Test
    public void propertyChangeTestCacheMetaDataTaskEnableFalse() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "false")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "789").map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, null, null));

        //nothing should happen here since it is not currently running.

        //verify that the download task was not started
        Mockito.verify(downloadMetaDataTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());
        //verify the the cleanup task was not started
        Mockito.verify(cleanUpTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        //start both the download and clean up tasks.
        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "true");
        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));
        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "false");

        spyCacheTasks(jdbcQueryingManagerImpl);

        //stop the cache task
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, null, null));

        //the cache task should have been started 1 times
        Mockito.verify(downloadMetaDataTimer, Mockito.times(1)).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheTask");
            }
        }), Matchers.anyLong(), Matchers.eq(123L));

        //the clean up task should have been started only once
        Mockito.verify(cleanUpTimer, Mockito.times(1)).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheCleanUpTask");
            }
        }), Matchers.anyLong(), Matchers.eq(456L));

        //the cache task should have been stop before it was started
        Mockito.verify(currentCacheTask).cancel();
        //the clean up task should never have been stopped
        Mockito.verify(currentCleanUpTask, Mockito.times(0)).cancel();

        //The cache task should be stopped and null. the clean up task should be set..
        Assert.assertNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNotNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());
    }

    @Test
    public void propertyChangeTestCacheMetaDataTaskEnableTrue() throws NoSuchFieldException, IllegalAccessException {
        MockConfig mockConfig = new MockConfig(CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "true")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "789").map());
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, null, null));

        //verify that the download task was started
        Mockito.verify(downloadMetaDataTimer).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheTask");
            }
        }), Matchers.anyLong(), Matchers.eq(123L));

        //verify the the cleanup task was not started
        Mockito.verify(cleanUpTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        //start both the download and clean up tasks.
        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));

        spyCacheTasks(jdbcQueryingManagerImpl);

        //start the cache task
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, null, null));

        //the cache task should have been started 3 times
        Mockito.verify(downloadMetaDataTimer, Mockito.times(3)).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheTask");
            }
        }), Matchers.anyLong(), Matchers.eq(123L));

        //the clean up task should have been started only once
        Mockito.verify(cleanUpTimer, Mockito.times(1)).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheCleanUpTask");
            }
        }), Matchers.anyLong(), Matchers.eq(456L));

        //the cache task should have been stop before it was started
        Mockito.verify(currentCacheTask).cancel();
        //the clean up task should never have been stopped
        Mockito.verify(currentCleanUpTask, Mockito.times(0)).cancel();

        //both tasks should be set.
        Assert.assertNotNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNotNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());
    }

    @Test
    public void propertyChangeTestCacheCleanUpInterval() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder()
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_METADATA_TASK_ENABLED, "true")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "123")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "456")
                .put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "789").map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, null, null));

        //verify that the cleanup task was started
        Mockito.verify(cleanUpTimer).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheCleanUpTask");
            }
        }), Matchers.anyLong(), Matchers.eq(456L));

        //verify the the cache task was not started
        Mockito.verify(downloadMetaDataTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        spyCacheTasks(jdbcQueryingManagerImpl);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "888");
        //trigger a cache cleanup refresh interval event
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, null, null));

        //the cache task should have been started 1 times
        Mockito.verify(cleanUpTimer, Mockito.times(1)).schedule(Matchers.argThat(new Matcher<TimerTask>() {
            @Override
            public boolean matches(Object task) {
                return task.getClass().getSimpleName().equals("MetaDataCacheCleanUpTask");
            }
        }), Matchers.anyLong(), Matchers.eq(888L));

        //verify the the cache task was not started
        Mockito.verify(downloadMetaDataTimer, Mockito.times(0)).schedule(Matchers.any(TimerTask.class), Matchers.anyLong(), Matchers.anyLong());

        //the cleanup task should have been stop before it was started
        Mockito.verify(currentCleanUpTask).cancel();

        //the clean up task should be set and the cache task should never have been set.
        Assert.assertNotNull(((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManagerImpl)).get());
        Assert.assertNull(((AtomicReference) currentCacheTaskField.get(jdbcQueryingManagerImpl)).get());
    }

    @SuppressWarnings("unchecked")
    private void spyCacheTasks(JdbcQueryingManagerImpl jdbcQueryingManager) throws IllegalAccessException {
        currentCacheTask = null;
        currentCleanUpTask = null;
        if (((AtomicReference) currentCacheTaskField.get(jdbcQueryingManager)).get() != null) {
            currentCacheTask = Mockito.spy((TimerTask) ((AtomicReference) currentCacheTaskField.get(jdbcQueryingManager)).get());
        }
        if (((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManager)).get() != null) {
            currentCleanUpTask = Mockito.spy((TimerTask) ((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManager)).get());
        }
        ((AtomicReference) currentCacheTaskField.get(jdbcQueryingManager)).set(currentCacheTask);
        ((AtomicReference) currentCleanUpTaskField.get(jdbcQueryingManager)).set(currentCleanUpTask);
    }

    private JdbcQueryingManagerImpl createJdbcQueryingManagerImpl(MockConfig mockConfig) throws NoSuchFieldException, IllegalAccessException {
        JdbcQueryingManagerImpl jdbcQueryingManager = new JdbcQueryingManagerImpl(jdbcConnectionPoolManager, jdbcConnectionManager, mockConfig, new TimeSource());

        Field downloadMetaDataTimerField = JdbcQueryingManagerImpl.class.getDeclaredField("downloadMetaDataTimer");
        downloadMetaDataTimerField.setAccessible(true);
        downloadMetaDataTimerField.set(jdbcQueryingManager, downloadMetaDataTimer);

        Field cleanUpTimerField = JdbcQueryingManagerImpl.class.getDeclaredField("cleanUpTimer");
        cleanUpTimerField.setAccessible(true);
        cleanUpTimerField.set(jdbcQueryingManager, cleanUpTimer);

        currentCacheTaskField = JdbcQueryingManagerImpl.class.getDeclaredField("currentCacheTask");
        currentCacheTaskField.setAccessible(true);

        currentCleanUpTaskField = JdbcQueryingManagerImpl.class.getDeclaredField("currentCleanUpTask");
        currentCleanUpTaskField.setAccessible(true);

        return jdbcQueryingManager;
    }

    private abstract class Matcher<T> implements org.hamcrest.Matcher<T> {

        @Override
        public void describeMismatch(Object o, Description description) {
        }

        @Override
        public void _dont_implement_Matcher___instead_extend_BaseMatcher_() {
        }

        @Override
        public void describeTo(Description description) {
        }
    }
}
