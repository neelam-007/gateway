package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.security.password.SecurePasswordManager;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.util.Config;
import com.l7tech.util.TimeUnit;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.net.ssl.TrustManager;
import java.security.SecureRandom;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkQueueExecutorManagerTest {

    @Mock
    private Config config;
    @Mock
    private SecurePasswordManager securePasswordManager;
    @Mock
    private TrustManager trustManager;
    @Mock
    private SecureRandom secureRandom;

    private WorkQueueExecutorManager workQueueExecutorManager;
    private WorkQueue wq1;
    private WorkQueue wq2;
    private WorkQueue wq3;

    @Before
    public void setup() throws Exception {
        when(config.getTimeUnitProperty("cassandra.maxConnectionCacheAge", 0L)).thenReturn(0L);
        when(config.getTimeUnitProperty("cassandra.maxConnectionCacheIdleTime", TimeUnit.MINUTES.toMillis(30))).thenReturn(TimeUnit.MINUTES.toMillis(30));
        when(config.getIntProperty("cassandra.maxConnectionCacheSize", 20)).thenReturn(20);

        wq1 = new WorkQueue();
        wq1.setGoid(getGoid());
        wq1.setName("wq1");
        wq1.setMaxQueueSize(10);
        wq1.setThreadPoolMax(5);
        wq1.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);

        wq2 = new WorkQueue();
        wq2.setGoid(getGoid());
        wq2.setName("wq2");
        wq2.setMaxQueueSize(20);
        wq2.setThreadPoolMax(10);
        wq2.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);

        wq3 = new WorkQueue();
        wq3.setGoid(getGoid());
        wq3.setName("wq3");
        wq3.setMaxQueueSize(30);
        wq3.setThreadPoolMax(15);
        wq3.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);

        WorkQueueEntityManager mockWorkQueueEntityManager = mock(WorkQueueEntityManager.class);
        Mockito.doReturn(wq1).when(mockWorkQueueEntityManager).getWorkQueueEntity(wq1.getGoid());
        Mockito.doReturn(wq2).when(mockWorkQueueEntityManager).getWorkQueueEntity(wq2.getGoid());
        Mockito.doReturn(wq3).when(mockWorkQueueEntityManager).getWorkQueueEntity(wq3.getGoid());
        Mockito.doReturn(wq1).when(mockWorkQueueEntityManager).findByPrimaryKey(wq1.getGoid());
        Mockito.doReturn(wq2).when(mockWorkQueueEntityManager).findByPrimaryKey(wq2.getGoid());
        Mockito.doReturn(wq3).when(mockWorkQueueEntityManager).findByPrimaryKey(wq3.getGoid());

        workQueueExecutorManager = new WorkQueueExecutorManagerImpl(mockWorkQueueEntityManager);
    }


    @After
    public void after() throws Exception {
        workQueueExecutorManager = null;
    }

    @Test
    public void testAddWorkQueueExecutor() throws Exception {
        workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        Assert.assertEquals("Number of work queue executors does not match.", 1, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
        ThreadPoolExecutor result = workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        // Using remaining capacity because the queue should be empty.
        Assert.assertSame("Work queue not added with the same settings.", wq1.getMaxQueueSize(), result.getQueue().remainingCapacity());
        Assert.assertSame("Work queue not added with the same settings.", wq1.getThreadPoolMax(), result.getMaximumPoolSize());
        Assert.assertTrue("Work queue not added with the same settings.", compareRejectPolicy(wq1.getRejectPolicy(), result.getRejectedExecutionHandler()));

        workQueueExecutorManager.getWorkQueueExecutor(wq2.getGoid());
        Assert.assertEquals("Number of work queue executors does not match.", 2, workQueueExecutorManager.getNumberOfWorkQueueExecutors());

        workQueueExecutorManager.getWorkQueueExecutor(wq3.getGoid());
        Assert.assertEquals("Number of work queue executors does not match.", 3, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
    }

    @Test
    public void testRemoveWorkQueueExecutor() throws Exception {
        workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        workQueueExecutorManager.getWorkQueueExecutor(wq2.getGoid());
        Assert.assertEquals("Number of work queue executors does not match.", 2, workQueueExecutorManager.getNumberOfWorkQueueExecutors());

        workQueueExecutorManager.removeWorkQueueExecutor(wq1);
        Assert.assertEquals("Number of work queue executors does not match.", 1, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
        workQueueExecutorManager.removeWorkQueueExecutor(wq2);
        Assert.assertEquals("Number of work queue executors does not match.", 0, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
        // Remove non-existing connection
        workQueueExecutorManager.removeWorkQueueExecutor(wq3);
        Assert.assertEquals("Number of work queue executors does not match.", 0, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
    }

    @Test
    public void testUpdateConnectionWithNewSettings() throws Exception {
        workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        workQueueExecutorManager.getWorkQueueExecutor(wq2.getGoid());
        Assert.assertEquals("Number of work queue executors does not match.", 2, workQueueExecutorManager.getNumberOfWorkQueueExecutors());

        // Update wq1
        WorkQueue updatedWq = new WorkQueue();
        updatedWq.setGoid(wq1.getGoid());
        updatedWq.setName(wq1.getName());
        updatedWq.setMaxQueueSize(7);
        updatedWq.setThreadPoolMax(3);
        updatedWq.setRejectPolicy(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY);

        workQueueExecutorManager.updateWorkQueueExecutor(updatedWq, wq1);
        Assert.assertEquals("Number of work queue executors does not match.", 2, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
        ThreadPoolExecutor result = workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        // Using remaining capacity because the queue should be empty.
        Assert.assertSame("Work queue not added with the same settings.", updatedWq.getMaxQueueSize(), result.getQueue().remainingCapacity());
        Assert.assertSame("Work queue not added with the same settings.", updatedWq.getThreadPoolMax(), result.getMaximumPoolSize());
        Assert.assertTrue("Work queue not added with the same settings.", compareRejectPolicy(updatedWq.getRejectPolicy(), result.getRejectedExecutionHandler()));
        // wq2 has not changed
        result = workQueueExecutorManager.getWorkQueueExecutor(wq2.getGoid());
        Assert.assertSame("Work queue not added with the same settings.", wq2.getMaxQueueSize(), result.getQueue().remainingCapacity());
        Assert.assertSame("Work queue not added with the same settings.", wq2.getThreadPoolMax(), result.getMaximumPoolSize());
        Assert.assertTrue("Work queue not added with the same settings.", compareRejectPolicy(wq2.getRejectPolicy(), result.getRejectedExecutionHandler()));
    }

    @Test
    public void testUpdateWorkQueueWithDifferentName() throws Exception {
        workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        workQueueExecutorManager.getWorkQueueExecutor(wq2.getGoid());
        Assert.assertEquals("Number of work queue executors does not match.", 2, workQueueExecutorManager.getNumberOfWorkQueueExecutors());

        // Update wq1
        WorkQueue updatedWq = new WorkQueue();
        updatedWq.setGoid(wq1.getGoid());
        updatedWq.setName("Name change");
        updatedWq.setMaxQueueSize(wq1.getMaxQueueSize());
        updatedWq.setThreadPoolMax(wq1.getThreadPoolMax());
        updatedWq.setRejectPolicy(wq1.getRejectPolicy());

        // Name change should have no effect
        workQueueExecutorManager.updateWorkQueueExecutor(updatedWq, wq1);
        Assert.assertEquals("Number of work queue executors does not match.", 2, workQueueExecutorManager.getNumberOfWorkQueueExecutors());
        ThreadPoolExecutor result = workQueueExecutorManager.getWorkQueueExecutor(wq1.getGoid());
        // Using remaining capacity because the queue should be empty.
        Assert.assertSame("Work queue not added with the same settings.", updatedWq.getMaxQueueSize(), result.getQueue().remainingCapacity());
        Assert.assertSame("Work queue not added with the same settings.", updatedWq.getThreadPoolMax(), result.getMaximumPoolSize());
        Assert.assertTrue("Work queue not added with the same settings.", compareRejectPolicy(updatedWq.getRejectPolicy(), result.getRejectedExecutionHandler()));

        // wq2 has not changed
        result = workQueueExecutorManager.getWorkQueueExecutor(wq2.getGoid());
        Assert.assertSame("Work queue not added with the same settings.", wq2.getMaxQueueSize(), result.getQueue().remainingCapacity());
        Assert.assertSame("Work queue not added with the same settings.", wq2.getThreadPoolMax(), result.getMaximumPoolSize());
        Assert.assertTrue("Work queue not added with the same settings.", compareRejectPolicy(wq2.getRejectPolicy(), result.getRejectedExecutionHandler()));
    }

    private Goid getGoid() {
        ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator =
                new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }

    private boolean compareRejectPolicy(String rejectPolicyName, RejectedExecutionHandler handler) {
        if ((WorkQueue.REJECT_POLICY_WAIT_FOR_ROOM.equals(rejectPolicyName) && handler instanceof WorkQueueExecutorManagerImpl.CallerBlocksPolicy) ||
                (WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY.equals(rejectPolicyName) && handler instanceof ThreadPoolExecutor.AbortPolicy)) {
            return true;
        }
        return false;
    }

}
