package com.l7tech.server.workqueue;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.gateway.common.audit.LoggingAudit;

import com.l7tech.gateway.common.workqueue.WorkQueue;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.*;

import java.util.concurrent.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class WorkQueueExecutorManagerImpl implements WorkQueueExecutorManager {
    private static final Logger logger = Logger.getLogger(WorkQueueExecutorManagerImpl.class.getName());
    private final ConcurrentHashMap<Goid, ThreadPoolExecutor> workQueueExecutorMap = new ConcurrentHashMap<>();
    private final WorkQueueEntityManager workQueueEntityManager;
    private final static Audit auditor = new LoggingAudit(logger);
    protected final Lock lock = new ReentrantLock();

    class CallerBlocksPolicy implements RejectedExecutionHandler {
        private WorkQueue entity;

        CallerBlocksPolicy(WorkQueue entity) {
            this.entity = entity;
        }

        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            if (!executor.isShutdown()) {
                BlockingQueue<Runnable> queue = executor.getQueue();
                try {
                    queue.put(r);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RejectedExecutionException("Waiting to be added to work queue " + entity.getName() + " was interrupted: ", e);
                }
            } else {
                throw new RejectedExecutionException("Executor for work queue " + entity.getName() + " has been shut down.");
            }
        }
    }

    public WorkQueueExecutorManagerImpl(WorkQueueEntityManager workQueueEntityManager) {
        this.workQueueEntityManager = workQueueEntityManager;
    }

    @Override
    public ThreadPoolExecutor getWorkQueueExecutor(Goid id) {
        lock.lock();
        try {
            ThreadPoolExecutor threadPoolExecutor = workQueueExecutorMap.get(id);
            if (threadPoolExecutor == null) {
                // First time being used, create a work queue and add it to the thread pool executor
                WorkQueue entity;
                try {
                    entity = workQueueEntityManager.getWorkQueueEntity(id);
                } catch (FindException e) {
                    auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_NOT_AVAIL, new String[]{id.toString(),
                            "Unable to find work queue:" + id.toString()}, ExceptionUtils.getDebugException(e));
                    return null;
                }

                if (entity == null) {
                    auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_NOT_AVAIL, id.toString(), "Work queue does not exist.");
                    return null;
                }

                threadPoolExecutor = createWorkQueueExecutor(entity);
                if (threadPoolExecutor != null) {
                    workQueueExecutorMap.put(id, threadPoolExecutor);
                }
            }
            return threadPoolExecutor;
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void removeWorkQueueExecutor(WorkQueue entity) {
        lock.lock();
        try {
            ThreadPoolExecutor threadPoolExecutor = workQueueExecutorMap.get(entity.getGoid());
            if (threadPoolExecutor != null) {
                threadPoolExecutor.shutdown();
                workQueueExecutorMap.remove(entity.getGoid());
                auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_FINE, "Removed work queue executor " + entity.getName());
            } else {
                auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_FINE,
                        "Work queue executor " + entity.getName() + " does not exist.");
            }
        } finally {
            lock.unlock();
        }
    }

    @Override
    public void updateWorkQueueExecutor(final WorkQueue newEntity, WorkQueue oldEntity) throws UpdateException {
        lock.lock();
        try {
            ThreadPoolExecutor threadPoolExecutor = workQueueExecutorMap.get(oldEntity.getGoid());

            if (threadPoolExecutor != null) {
                if (newEntity.getMaxQueueSize() != oldEntity.getMaxQueueSize()) {
                    threadPoolExecutor.shutdown();
                    workQueueExecutorMap.remove(oldEntity.getGoid());
                    final ThreadPoolExecutor workQueueExecutor = createWorkQueueExecutor(newEntity);
                    if (workQueueExecutor == null) {
                        auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_FINE,
                                "Work queue executor " + newEntity.getName() + " cannot be created after work queue size change.");
                    } else {
                        auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_FINE,
                                "Work queue executor restarted due to work queue " + newEntity.getName() + " size changed.");
                    }
                } else {
                    threadPoolExecutor.setMaximumPoolSize(newEntity.getThreadPoolMax());
                    threadPoolExecutor.setCorePoolSize(getCalculatedCorePoolSize(newEntity));

                    // Reject policy changed
                    if (!newEntity.getRejectPolicy().equals(oldEntity.getRejectPolicy())) {
                        if (newEntity.getRejectPolicy().equals(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY)) {
                            threadPoolExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
                        } else {
                            threadPoolExecutor.setRejectedExecutionHandler(new CallerBlocksPolicy(newEntity));
                        }
                    }
                    auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_FINE,
                            "Work queue executor " + newEntity.getName() + " updated with new properties.");
                }
            }
        } finally {
            lock.unlock();
        }
    }

    protected ThreadPoolExecutor createWorkQueueExecutor(WorkQueue entity) {
        lock.lock();
        try {
            RejectedExecutionHandler rejectHandler;
            if (entity.getRejectPolicy().equals(WorkQueue.REJECT_POLICY_FAIL_IMMEDIATELY)) {
                rejectHandler = new ThreadPoolExecutor.AbortPolicy();
            } else {
                rejectHandler = new CallerBlocksPolicy(entity);
            }

            BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(entity.getMaxQueueSize(), true);
            ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(getCalculatedCorePoolSize(entity),
                    entity.getThreadPoolMax(), 5L * 60L, TimeUnit.SECONDS, queue, rejectHandler);

            threadPoolExecutor.allowCoreThreadTimeOut(true);
            workQueueExecutorMap.put(entity.getGoid(), threadPoolExecutor);
            auditor.logAndAudit(AssertionMessages.WORK_QUEUE_EXECUTOR_FINE, "Created work queue executor: " + entity.getName());
            return threadPoolExecutor;
        } finally {
            lock.unlock();
        }
    }

    private int getCalculatedCorePoolSize(WorkQueue entity) {
        int maxThreadPool = entity.getThreadPoolMax();
        if (maxThreadPool <= 10) {
            return maxThreadPool;
        } else {
            return (int) Math.ceil(maxThreadPool * 0.75);
        }
    }
}
