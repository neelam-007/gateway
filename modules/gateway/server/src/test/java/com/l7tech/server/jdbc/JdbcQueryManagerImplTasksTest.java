package com.l7tech.server.jdbc;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.event.system.Stopped;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
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
    private Field jdbcMetadataRetrievalThreadPoolField;
    private Field configField;

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

    @BugId("SSG-6812")
    @Test
    public void cacheRefreshIntervalMinMaxTest() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder().map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "-1");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, null, null));
        Mockito.verify(downloadMetaDataTimer, Mockito.times(1)).schedule(Matchers.<TimerTask>any(TimerTask.class), Matchers.anyLong(), Matchers.eq(600000L));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "0");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, null, null));
        Mockito.verifyNoMoreInteractions(downloadMetaDataTimer);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, "1");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, null, null));
        Mockito.verify(downloadMetaDataTimer, Mockito.times(1)).schedule(Matchers.<TimerTask>any(TimerTask.class), Matchers.anyLong(), Matchers.eq(1L));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, String.valueOf(Long.MAX_VALUE));
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_REFRESH_INTERVAL, null, null));
        Mockito.verify(downloadMetaDataTimer, Mockito.times(1)).schedule(Matchers.<TimerTask>any(TimerTask.class), Matchers.anyLong(), Matchers.eq(Long.MAX_VALUE));
    }

    @BugId("SSG-6812")
    @Test
    public void cacheCleanUpIntervalMinMaxTest() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder().map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "-1");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, null, null));
        Mockito.verify(cleanUpTimer, Mockito.times(1)).schedule(Matchers.<TimerTask>any(TimerTask.class), Matchers.anyLong(), Matchers.eq(60000L));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "0");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, null, null));
        Mockito.verifyNoMoreInteractions(cleanUpTimer);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, "1");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, null, null));
        Mockito.verify(cleanUpTimer, Mockito.times(1)).schedule(Matchers.<TimerTask>any(TimerTask.class), Matchers.anyLong(), Matchers.eq(1L));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, String.valueOf(Long.MAX_VALUE));
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_CLEANUP_REFRESH_INTERVAL, null, null));
        Mockito.verify(cleanUpTimer, Mockito.times(1)).schedule(Matchers.<TimerTask>any(TimerTask.class), Matchers.anyLong(), Matchers.eq(Long.MAX_VALUE));
    }

    @BugId("SSG-6812")
    @Test
    public void minCacheConcurrencyMinMaxTest() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder().map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);
        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "1");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, null, null));
        Assert.assertEquals(1, ((ThreadPool) jdbcMetadataRetrievalThreadPoolField.get(jdbcQueryingManagerImpl)).getCorePoolSize());

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "0");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, null, null));
        Assert.assertEquals(10, ((ThreadPool) jdbcMetadataRetrievalThreadPoolField.get(jdbcQueryingManagerImpl)).getCorePoolSize());

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "200");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, null, null));
        Assert.assertEquals(200, ((ThreadPool) jdbcMetadataRetrievalThreadPoolField.get(jdbcQueryingManagerImpl)).getCorePoolSize());

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, "201");
        jdbcQueryingManagerImpl.propertyChange(new PropertyChangeEvent(this, ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_MIN_CACHE_CONCURRENCY, null, null));
        Assert.assertEquals(10, ((ThreadPool) jdbcMetadataRetrievalThreadPoolField.get(jdbcQueryingManagerImpl)).getCorePoolSize());
    }

    @BugId("SSG-6812")
    @Test
    public void cacheKeyNoUsageExpirationMinMaxTest() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder().map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);
        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));
        Config config = (Config) configField.get(jdbcQueryingManagerImpl);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, "-1");
        Assert.assertEquals(2678400L, config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, 2678400));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, "0");
        Assert.assertEquals(0L, config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, 2678400));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, String.valueOf(Long.MAX_VALUE));
        Assert.assertEquals(Long.MAX_VALUE, config.getLongProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_NO_USAGE_EXPIRATION, 2678400));
    }

    @BugId("SSG-6812")
    @Test
    public void cacheTaskStatementTimeoutMinMaxTest() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder().map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);
        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));
        Config config = (Config) configField.get(jdbcQueryingManagerImpl);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, "-1");
        Assert.assertEquals(120, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, 120));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, "0");
        Assert.assertEquals(0, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, 120));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, String.valueOf(Integer.MAX_VALUE));
        Assert.assertEquals(Integer.MAX_VALUE, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_CACHE_TASK_MAX_STMT_TIME_OUT, 120));
    }

    @BugId("SSG-6812")
    @Test
    public void maxGatewayStatementTimeoutMinMaxTest() throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> properties = CollectionUtils.MapBuilder.<String, String>builder().map();
        MockConfig mockConfig = new MockConfig(properties);
        jdbcQueryingManagerImpl = createJdbcQueryingManagerImpl(mockConfig);
        jdbcQueryingManagerImpl.onApplicationEvent(new ReadyForMessages(this, null, null));
        Config config = (Config) configField.get(jdbcQueryingManagerImpl);

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, "0");
        Assert.assertEquals(300, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, 300));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, "1");
        Assert.assertEquals(1, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, 300));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, "60");
        Assert.assertEquals(60, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, 300));

        properties.put(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, String.valueOf(Integer.MAX_VALUE));
        Assert.assertEquals(Integer.MAX_VALUE, config.getIntProperty(ServerConfigParams.PARAM_JDBC_QUERY_MANAGER_MAX_GATEWAY_STMT_TIME_OUT, 300));
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

        jdbcMetadataRetrievalThreadPoolField = JdbcQueryingManagerImpl.class.getDeclaredField("jdbcMetadataRetrievalThreadPool");
        jdbcMetadataRetrievalThreadPoolField.setAccessible(true);

        currentCacheTaskField = JdbcQueryingManagerImpl.class.getDeclaredField("currentCacheTask");
        currentCacheTaskField.setAccessible(true);

        currentCleanUpTaskField = JdbcQueryingManagerImpl.class.getDeclaredField("currentCleanUpTask");
        currentCleanUpTaskField.setAccessible(true);

        configField = JdbcQueryingManagerImpl.class.getDeclaredField("config");
        configField.setAccessible(true);

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
